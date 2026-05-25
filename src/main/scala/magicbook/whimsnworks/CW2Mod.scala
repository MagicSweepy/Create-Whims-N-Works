package magicbook.whimsnworks

import com.simibubi.create.foundation.data.CreateRegistrate
import magicbook.whimsnworks.api.module.ModuleManager
import magicbook.whimsnworks.api.util.DistLogger
import net.minecraft.resources.ResourceLocation
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.common.Mod
import net.neoforged.fml.loading.FMLEnvironment

/** Create: Whims'n'Works (short: CW^2^, seems like some complex or surface :D)
 *
 *  We use standard Scala format as the coding style by default, it means everything
 *  should be FP style but not OOP style. I think it will be funny for coding this mod :)
 *
 *  Additionally, Literary Programming is also be used for this mod. Although it is not
 *  means that we will write stories in ScalaDoc, we will provide clear ScalaDoc as much
 *  as possible, perhaps with some interesting descriptions?
 *
 *  All ScalaDoc is written for myself because the development of this mod is driven by my
 *  interest and exploration of Scala.
 */
object CW2Mod {

  final val ID = "whimsnworks"
  final val NAME = "Create: Whims'n'Works"
  
  final val LOGGER = DistLogger(NAME)

  final val REGISTRATE: CreateRegistrate = CreateRegistrate.create(ID)

  /** Make the mod as [[ResourceLocation]] for register entries.
   *
   *  @param path The path in the given namespace (modid by default).
   *  @return     Returns the id in current namespace with given [[path]].
   */
  def id(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(ID, path)
}

@Mod(CW2Mod.ID)
class CW2Mod(eventBus: IEventBus) {
  CW2Mod.LOGGER.debug("Starting to load ModuleManager...")
  ModuleManager.init(CW2Mod.REGISTRATE)
  if (FMLEnvironment.dist.isClient) {
      ModuleManager.initClient()
  }
  CW2Mod.LOGGER.debug("Finished ModuleManager load!")
}