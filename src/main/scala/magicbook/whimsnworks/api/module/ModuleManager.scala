package magicbook.whimsnworks.api.module

import com.simibubi.create.foundation.data.CreateRegistrate
import magicbook.whimsnworks.CW2Mod
import magicbook.whimsnworks.api.util.DistLogger
import net.neoforged.fml.ModList

import java.io.{File, IOException}
import java.net.{JarURLConnection, URL}
import java.nio.file.{Files, Paths}
import scala.annotation.tailrec
import scala.collection.mutable
import scala.jdk.CollectionConverters._

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

  private var modules: Vector[ModModule] = Vector.empty
  private var enabledModules: Map[String, ModModule] = Map.empty
  private var discovered = false

  /** Manually register a module.
    *
    * @see [[discover]] for automatically version.
    */
  def register(module: ModModule): Unit = {
    if (modules.exists(_.moduleId == module.moduleId)) {
      throw new IllegalStateException(s"Module '${module.moduleId}' is already registered")
    }
    modules = modules :+ module
    logger.debug(s"Registered module: ${module.moduleId}")
  }

  def init(registrate: CreateRegistrate): Unit = {
    if (!discovered) scanModules()
    if (modules.isEmpty) {
      logger.warn("Cannot found any modules registered, it's that correct?")
      return
    }
    val filteredMods = filterByModDependencies(modules.toList)
    if (filteredMods.isEmpty) {
      logger.warn("Cannot found any modules pass dependencies checking")
      return
    }

    val resolved = resolveDependencies(filteredMods)
    val sorted   = sortModules(resolved.values.toList)
    initializeAll(sorted, registrate)

    logger.info(s"Module loading complete. Loaded ${enabledModules.size} / ${modules.size} module(s).")
  }

  def initClient(): Unit = enabledModules.values.foreach(_.onClientInit())

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

    val discoveredModules = discoverFrom(scanPackage(pkg))
    discoveredModules.foreach(register)

    logger.info(s"Auto-discovered ${discoveredModules.size} module(s) in package '$pkg'")
  }

  private def discoverFrom(classes: Seq[Class[?]]): Seq[ModModule] =
    classes.filter(hasDiscoverAnnotation)
      .flatMap { cls =>
        if (classOf[ModModule].isAssignableFrom(cls)) {
          try Some(getSingleton(cls))
          catch { case e: Exception =>
            logger.error(s"Failed to instantiate module: ${cls.getName}", e)
            None
          }
        } else {
          logger.warn(s"Class ${cls.getName} has @discover but doesn't extend ModModule — skipping")
          None
        }
      }

  private def filterByModDependencies(modules: List[ModModule]): List[ModModule] =
    modules.filter { mod =>
      val modMissing = mod.requiredMods.filterNot(ModList.get().isLoaded)
      if (modMissing.nonEmpty) {
        logger.info(s"Skipping module '${mod.moduleId}', missing mod(s): ${modMissing.mkString(", ")}")
        false
      } else {
        true
      }
    }

  @tailrec
  private def resolveDependencies(resolvedModules: List[ModModule]): Map[String, ModModule] = {
    val idx          = resolvedModules.map(module => module.moduleId -> module).toMap
    val (keep, drop) = idx.partition { case (_, module) =>
      module.requiredModules.forall(idx.contains)
    }

    if (drop.isEmpty) {
      keep
    } else {
      drop.foreach { case (_, module) =>
        logger.info(s"Skipping module '${module.moduleId}', missing required module(s): " +
          module.requiredModules.filterNot(idx.contains).mkString(", "))
      }
      resolveDependencies(keep.values.toList)
    }
  }

  /** Topological sorting all modules via DFS.
    *
    * @param modules All prepare modules to be sorted.
    */
  private def sortModules(modules: List[ModModule]): List[ModModule] = {
    val moduleIdx = modules.map(module => module.moduleId -> module).toMap

    def visit(module: ModModule,
              path: List[String],
              visited: Set[String],
              result: List[ModModule]): (Set[String], List[ModModule]) = {
      if (visited.contains(module.moduleId)) {
        (visited, result)
      } else if (path.contains(module.moduleId)) {
        val cycle = (module.moduleId :: path).reverse
          .dropWhile(_ != module.moduleId)
          .mkString(" -> ")
        throw new IllegalStateException(s"Circular module dependency detected: $cycle")
      } else {
        val (v, r) = module.requiredModules.foldLeft((visited, result)) {
          case ((vs, rs), depId) =>
            moduleIdx.get(depId).fold((vs, rs))(visit(_, module.moduleId :: path, vs, rs))
        }
        (v + module.moduleId, module :: r)
      }
    }

    modules.foldLeft((Set.empty[String], List.empty[ModModule])) {
      case ((vs, rs), m) => visit(m, Nil, vs, rs)
    }._2.reverse
  }

  private def initializeAll(sortedModules: List[ModModule], registrate: CreateRegistrate): Unit = {
    enabledModules = Map.empty
    for (module <- sortedModules) {
      logger.info(s"Loading module: ${module.moduleId}")
      module.onRegister(registrate)
      module.onInit()
      enabledModules = enabledModules + (module.moduleId -> module)
    }
  }

  private val discoverAnnotationName: String = classOf[discover].getName

  private def hasDiscoverAnnotation(clazz: Class[?]): Boolean
    = clazz.getDeclaredAnnotations.exists(_.annotationType().getName == discoverAnnotationName)

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

  private def listClasses(url: URL, pkg: String): Seq[Class[?]] =
    url.getProtocol match {
      case "file" =>
        val dir = new File(url.toURI)
        if (dir.isDirectory) scanDir(dir, pkg) else Seq.empty
      case "jar"   => scanJar(url, pkg)
      case "union" => scanUnion(url, pkg)
      case _       =>
        logger.debug(s"Unsupported protocol '${url.getProtocol}', skipping: $url")
        Seq.empty
    }

  private def scanDir(dir: File, pkg: String): Seq[Class[?]] = {
    val prefix = pkg + "."
    val files  = dir.listFiles()
    if (files == null) {
      Seq.empty
    } else {
      files.filter(_.getName.endsWith(".class"))
        .flatMap { file =>
          val className = prefix + file.getName.substring(0, file.getName.length - 6)
          loadClass(className)
        }
        .toSeq
    }
  }

  private def scanJar(jarUrl: URL, pkg: String): Seq[Class[?]] = {
    val prefix     = pkg.replace('.', '/') + "/"
    val connection = jarUrl.openConnection().asInstanceOf[JarURLConnection]
    connection.setUseCaches(false)
    val jarFile = connection.getJarFile

    try {
      val buffer  = mutable.ListBuffer.empty[Class[?]]
      val entries = jarFile.entries()
      while (entries.hasMoreElements) {
        val name = entries.nextElement().getName
        if (name.startsWith(prefix) && name.endsWith(".class")) {
          val className = name.substring(0, name.length - 6).replace('/', '.')
          loadClass(className).foreach(buffer += _)
        }
      }
      buffer.toSeq
    } finally {
      jarFile.close()
    }
  }

  private def scanUnion(url: URL, pkg: String): Seq[Class[?]] = {
    try {
      val path = Paths.get(url.toURI)
      if (Files.isDirectory(path)) {
        Files.list(path).iterator().asScala
          .filter(p => p.getFileName.toString.endsWith(".class"))
          .flatMap { p =>
            val name = p.getFileName.toString
            val className = pkg + "." + name.substring(0, name.length - 6)
            loadClass(className)
          }
          .toSeq
      } else {
        Seq.empty
      }
    } catch {
      case _: Exception => Seq.empty
    }
  }

  private def loadClass(name: String): Option[Class[?]] = {
    try {
      Some(Class.forName(name, false, Thread.currentThread().getContextClassLoader))
    } catch {
      case _: ClassNotFoundException =>
        logger.warn(s"Class not found: $name")
        None
      case e: Exception              =>
        logger.warn(s"Failed to load class: $name", e)
        None
    }
  }
}
