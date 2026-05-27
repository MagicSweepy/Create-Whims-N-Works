package magicbook.whimsnworks

import com.simibubi.create.foundation.data.CreateRegistrate
import com.simibubi.create.foundation.item.{ItemDescription, KineticStats, TooltipModifier}
import magicbook.whimsnworks.CW2Mod.REGISTRATE
import magicbook.whimsnworks.api.module.ModuleManager
import magicbook.whimsnworks.api.util.DistLogger
import magicbook.whimsnworks.registration.{CW2BlockEntities, CW2Blocks, CW2CreativeModeTabs, CW2Registrate, CW2TransmissionSets}
import net.createmod.catnip.lang.FontHelper
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

  final val REGISTRATE: CreateRegistrate = CW2Registrate.create(ID)

  REGISTRATE.setTooltipModifierFactory(item => ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
    .andThen(TooltipModifier.mapNull(KineticStats.create(item))))

  /** Make the mod as [[ResourceLocation]] for register entries.
   *
   *  @param path The path in the given namespace (modid by default).
   *  @return     Returns the id in current namespace with given [[path]].
   */
  def id(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(ID, path)
}

@Mod(CW2Mod.ID)
class CW2Mod(eventBus: IEventBus) {
  REGISTRATE.registerEventListeners(eventBus)

  CW2Mod.LOGGER.debug("Starting to load ModuleManager...")
  ModuleManager.init(CW2Mod.REGISTRATE)
  if (FMLEnvironment.dist.isClient) {
      ModuleManager.initClient()
  }
  CW2Mod.LOGGER.debug("Finished ModuleManager load!")
  CW2CreativeModeTabs.register(eventBus)
}