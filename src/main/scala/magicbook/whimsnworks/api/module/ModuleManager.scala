package magicbook.whimsnworks.api.module

import com.google.gson.{Gson, JsonObject}
import com.simibubi.create.foundation.data.CreateRegistrate
import magicbook.whimsnworks.CW2Mod
import magicbook.whimsnworks.api.util.DistLogger
import net.neoforged.fml.ModList

import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import scala.annotation.tailrec
import scala.jdk.CollectionConverters.*

object ModuleManager {

  private val logger = DistLogger(s"${CW2Mod.NAME} | Module Manager")

  private var modules: Vector[ModModule] = Vector.empty
  private var enabledModules: Map[String, ModModule] = Map.empty

  def register(module: ModModule): Unit = {
    if (modules.exists(_.moduleId == module.moduleId)) {
      throw new IllegalStateException(s"Module '${module.moduleId}' is already registered")
    }
    modules = modules :+ module
    logger.debug(s"Registered module: ${module.moduleId}")
  }

  def init(registrate: CreateRegistrate): Unit = {
    loadModulesFromManifest()
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

  /** Load module class names from manifest on the classpath and register each one.
    */
  private def loadModulesFromManifest(): Unit = {
    val classLoader = Thread.currentThread().getContextClassLoader
    val resources   = classLoader.getResources("META-INF/whimsnworks-modules.json")
    val gson        = Gson()

    while (resources.hasMoreElements) {
      val url    = resources.nextElement()
      val reader = InputStreamReader(url.openStream(), StandardCharsets.UTF_8)
      try {
        val json       = gson.fromJson(reader, classOf[JsonObject])
        val moduleList = json.getAsJsonArray("modules")
        if (moduleList != null) {
          moduleList.iterator().asScala.foreach { element =>
            val className = element.getAsString
            try {
              val cls      = Class.forName(className, false, classLoader)
              val instance = getSingleton(cls)
              register(instance)
            } catch {
              case e: Exception => logger.error(s"Failed to load module class: $className", e)
            }
          }
        }
      } finally {
        reader.close()
      }
    }
  }

  /** Reflect the singleton instance from a Scala `object`.
    */
  private def getSingleton(clazz: Class[?]): ModModule = {
    def modFieldOf(c: Class[?]): Option[ModModule] =
      try {
        val field = c.getDeclaredField("MODULE$")
        field.setAccessible(true)
        Some(field.get(null).asInstanceOf[ModModule])
      } catch {
        case _: NoSuchFieldException => None
      }

    modFieldOf(clazz).orElse {
        try {
          val companion = Class.forName(clazz.getName + "$", false, clazz.getClassLoader)
          modFieldOf(companion)
        } catch {
          case _: ClassNotFoundException => None
        }
      }
      .getOrElse {
        clazz.getDeclaredConstructor().newInstance().asInstanceOf[ModModule]
      }
  }

  private def filterByModDependencies(modules: List[ModModule]): List[ModModule] =
    modules.filter { module =>
      val missingMod = module.requiredMods.filterNot(ModList.get().isLoaded)
      if (missingMod.nonEmpty) {
        logger.info(s"Skipping module '${module.moduleId}', missing mod(s): ${missingMod.mkString(", ")}")
        false
      } else {
        true
      }
    }

  @tailrec
  private def resolveDependencies(resolvedModules: List[ModModule]): Map[String, ModModule] = {
    val idx          = resolvedModules.map(module => module.moduleId -> module).toMap
    val (keep, drop) = idx.partition { case (_, module) => module.requiredModules.forall(idx.contains) }

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
}
