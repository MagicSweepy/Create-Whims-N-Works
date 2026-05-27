package magicbook.whimsnworks.api.util

import net.createmod.catnip.platform.CatnipServices
import net.minecraft.client.Minecraft
import net.minecraft.world.item.{Item, ItemStack}
import net.neoforged.api.distmarker.{Dist, OnlyIn}

object ItemUtil {
    
    def is3d: Item => Boolean = {
        if (CatnipServices.PLATFORM.getEnv.isClient) {
            isGui3d
        } else {
            _ => false
        }
    }
    
    @OnlyIn(Dist.CLIENT)
    private def isGui3d: Item => Boolean = {
        val renderer = Minecraft.getInstance().getItemRenderer
        item => {
            val model = renderer.getModel(ItemStack(item), null, null, 0)
            model.isGui3d
        }
    }
}
