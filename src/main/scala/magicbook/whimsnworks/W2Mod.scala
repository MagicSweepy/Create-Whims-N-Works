package magicbook.whimsnworks

import net.minecraft.resources.ResourceLocation
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.common.Mod

object W2Mod {

    final val ID = "whimsnworks"

    /** Make the mod as [[ResourceLocation]] for register entries.
     *
     *  @param path The path in the given namespace (modid by default).
     *  @return     Returns the id in current namespace with given [[path]].
     */
    def id(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(ID, path)
}

@Mod(W2Mod.ID)
class W2Mod(eventBus: IEventBus) {

}