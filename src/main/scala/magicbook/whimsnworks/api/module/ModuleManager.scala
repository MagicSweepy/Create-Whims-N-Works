package magicbook.whimsnworks.api.module

import com.simibubi.create.foundation.data.CreateRegistrate
import magicbook.whimsnworks.CW2Mod
import magicbook.whimsnworks.api.util.DistLogger
import net.neoforged.fml.ModList

import java.io.{File, IOException}
import java.net.{JarURLConnection, URL}
import scala.collection.mutable

/** Step 1: Scanning classpath `magicbook.whimsnworks.module` package
  *         for all annotated classes with [[[discover]].
  * Step 2: Check [[ModModule.requiredMods]] against NF mod list, skip
  *         modules which missing dependencies.
  * Step 3: Check [[ModModule.requiredModules]] and iterative fixed-point
  *         for all modules whose dependencies cannot be satisfied.
  * Step 4: Do DFS ordering each module initialises after its declared
  *         dependencies
  * Step 5: Call [[ModModule.onRegister()]] then [[ModModule.onInit()]]
  *         in dependency order.
  */
object ModuleManager {

  private val logger = DistLogger(s"${CW2Mod.NAME} | Module Manager")

  private val modules: mutable.ListBuffer[ModModule] = mutable.ListBuffer.empty
  private val enabledModules: mutable.LinkedHashMap[String, ModModule] = mutable.LinkedHashMap.empty

  private var discovered = false
  
  /** Manually register a module.
    * 
    * @see [[discover]] for automatically version.
    */
  def register(module: ModModule): Unit = {
    if (modules.exists(_.moduleId == module.moduleId)) {
      throw new IllegalStateException(s"Module '${module.moduleId}' is already registered")
    }
    modules += module
    logger.debug(s"Registered module: ${module.moduleId}")
  }

  def init(registrate: CreateRegistrate): Unit = {
    if (!discovered) scanModules()
    if (modules.isEmpty) {
      logger.warn("Cannot found any modules registered, it's that correct?")
      return
    }
    
    val mods = modules.filter { mod =>
      val modMissing = mod.requiredMods.filterNot(ModList.get().isLoaded)
      if (modMissing.nonEmpty) {
        logger.info(s"Skipping module '${mod.moduleId}', missing mod(s): ${modMissing.mkString(", ")}")
        false
      } else {
        true
      }
    }

    if (mods.isEmpty) {
      logger.warn("Cannot found any modules pass dependencies checking")
      return
    }

    val moduleResolved: mutable.LinkedHashMap[String, ModModule] 
      = mutable.LinkedHashMap.from(mods.map(m => m.moduleId -> m))
    
    var changed = true
    while (changed) {
      changed = false
      val toRemove = moduleResolved.filter { case (_, module) =>
        module.requiredModules.exists(depId => !moduleResolved.contains(depId))
      }
      if (toRemove.nonEmpty) {
        toRemove.foreach { case (_, module) =>
          logger.info(s"Skipping module '${module.moduleId}', missing required module(s): " + 
              module.requiredModules.filterNot(moduleResolved.contains).mkString(", "))
        }
        moduleResolved --= toRemove.keys
        changed = true
      }
    }
    
    val moduleSorted = sortModules(moduleResolved.values.toSeq)
    
    enabledModules.clear()
    for (module <- moduleSorted) {
      logger.info(s"Loading module: ${module.moduleId}")
      module.onRegister(registrate)
      module.onInit()
      enabledModules(module.moduleId) = module
    }

    logger.info(s"Module loading complete. Loaded ${enabledModules.size} / ${modules.size} module(s).")
  }

  def initClient(): Unit = {
    for (module <- enabledModules.values) {
      module.onClientInit()
    }
  }

  def isModuleEnabled(id: String): Boolean = enabledModules.contains(id)
  
  /** Scan the given package for [[discover]] annotated classes and register these modules. 
    * 
    * Will called automatically by [[init]], or can be invoked explicitly to control timing.
    *
    * @param pkg The dot-separated package name to scan.
    */
  private def scanModules(pkg: String = "magicbook.whimsnworks.module"): Unit = {
    if (discovered) return
    discovered = true

    logger.info(s"Scanning package '$pkg' for @discover...")

    val classes = scanPackage(pkg)
    var count = 0

    for (clazz <- classes) {
      if (hasDiscoverAnnotation(clazz)) {
        if (classOf[ModModule].isAssignableFrom(clazz)) {
          try {
            register(getSingleton(clazz))
            count += 1
          } catch {
            case e: Exception => logger.error(s"Failed to instantiate module: ${clazz.getName}", e)
          }
        } else {
          logger.warn(s"Class ${clazz.getName} has @discover but doesn't extend ModModule — skipping")
        }
      }
    }

    logger.info(s"Auto-discovered $count module(s) in package '$pkg'")
  }

  private val discoverAnnotationName: String = classOf[discover].getName
  
  private def hasDiscoverAnnotation(clazz: Class[?]): Boolean
    = clazz.getDeclaredAnnotations.exists(_.annotationType().getName == discoverAnnotationName)

  /** Topological sorting all modules via DFS.
    * 
    * @param modules All prepare modules to be sorted.
    */
  private def sortModules(modules: Seq[ModModule]): Seq[ModModule] = {
    val moduleIdx   = modules.map(module => module.moduleId -> module).toMap
    val visited = mutable.LinkedHashSet.empty[String]
    val result  = mutable.ListBuffer.empty[ModModule]

    def visit(module: ModModule, path: List[String]): Unit = {
      if (visited.contains(module.moduleId)) return
      if (path.contains(module.moduleId)) {
        val cycle = (module.moduleId :: path).reverse
          .dropWhile(_ != module.moduleId)
          .mkString(" -> ")
        throw new IllegalStateException(s"Circular module dependency detected: $cycle")
      }
      
      for (depId <- module.requiredModules) {
        moduleIdx.get(depId).foreach(visit(_, module.moduleId :: path))
      }
      visited += module.moduleId
      result += module
    }

    for (module <- modules) visit(module, Nil)
    result.toSeq
  }

  /** Reflect the singleton instance from a class that is expected to be a Scala `object`
    * because it has `MODULE$` field.
    */
  private def getSingleton(clazz: Class[?]): ModModule = {
    try {
      val field = clazz.getDeclaredField("MODULE$")
      field.setAccessible(true)
      field.get(null).asInstanceOf[ModModule]
    } catch {
      case _: NoSuchFieldException => clazz.getDeclaredConstructor().newInstance().asInstanceOf[ModModule]
    }
  }
  
  /** Find all classes in the given dot-separated package.
    *
    * Handles directory-based classpath (dev) and Jar-based (production).
    */
  private def scanPackage(pkg: String): Seq[Class[?]] = {
    val classLoader = Thread.currentThread().getContextClassLoader
    val path        = pkg.replace('.', '/')
    val result      = mutable.LinkedHashSet.empty[Class[?]]

    try {
      val resources = classLoader.getResources(path)
      while (resources.hasMoreElements) {
        try result ++= listClasses(resources.nextElement(), pkg)
        catch { case e: IOException => logger.warn(s"Failed to scan classpath entry", e) }
      }
    } catch {
      case _: IOException => // :)
    }

    result.toSeq
  }

  private def listClasses(url: URL, pkg: String): Seq[Class[?]] = {
    url.getProtocol match {
      case "file" =>
        val dir = new File(url.toURI)
        if (dir.isDirectory) scanDir(dir, pkg) else Seq.empty
      case "jar"  => scanJar(url, pkg)
      case _      =>
        logger.debug(s"Unsupported protocol '${url.getProtocol}', skipping: $url")
        Seq.empty
    }
  }

  private def scanDir(dir: File, pkg: String): Seq[Class[?]] = {
    val result = mutable.ListBuffer.empty[Class[?]]
    val prefix = pkg + "."
    val files  = dir.listFiles()
    if (files != null) {
      for (file <- files) {
        val name = file.getName
        if (name.endsWith(".class")) {
          val className = prefix + name.substring(0, name.length - 6)
          loadClass(className).foreach(result += _)
        }
      }
    }
    result.toSeq
  }

  private def scanJar(jarUrl: URL, pkg: String): Seq[Class[?]] = {
    val result = mutable.ListBuffer.empty[Class[?]]
    val prefix = pkg.replace('.', '/') + "/"

    val connection = jarUrl.openConnection().asInstanceOf[JarURLConnection]
    connection.setUseCaches(false)
    val jarFile = connection.getJarFile
    try {
      val entries = jarFile.entries()
      while (entries.hasMoreElements) {
        val name = entries.nextElement().getName
        if (name.startsWith(prefix) && name.endsWith(".class")) {
          val className = name.substring(0, name.length - 6).replace('/', '.')
          loadClass(className).foreach(result += _)
        }
      }
    } finally {
      jarFile.close()
    }
    result.toSeq
  }

  private def loadClass(name: String): Option[Class[?]] = {
    try {
      Some(Class.forName(name, false, Thread.currentThread().getContextClassLoader))
    } catch {
      case _: ClassNotFoundException =>
        logger.warn(s"Class not found: $name")
        None
      case e: Exception =>
        logger.warn(s"Failed to load class: $name", e)
        None
    }
  }
}
