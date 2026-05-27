package magicbook.whimsnworks.registration

import net.minecraft.world.level.block.state.properties.WoodType

object CW2TransmissionSets {
  case class WoodInfo(name: String, woodType: WoodType)

  val woodTypes: Seq[WoodInfo] = Seq(
      WoodInfo("acacia", WoodType.ACACIA),
      WoodInfo("birch", WoodType.BIRCH),
      WoodInfo("bamboo", WoodType.BAMBOO),
      WoodInfo("cherry", WoodType.CHERRY),
      WoodInfo("crimson", WoodType.CRIMSON),
      WoodInfo("dark_oak", WoodType.DARK_OAK),
      WoodInfo("oak", WoodType.OAK),
      WoodInfo("jungle", WoodType.JUNGLE),
      WoodInfo("mangrove", WoodType.MANGROVE),
      WoodInfo("warped", WoodType.WARPED),
      WoodInfo("spruce", WoodType.SPRUCE))

  def register(): Unit = {}
}