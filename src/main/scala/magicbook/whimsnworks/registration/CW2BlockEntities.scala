package magicbook.whimsnworks.registration

import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.content.kinetics.simpleRelays.SimpleKineticBlockEntity
import com.tterrag.registrate.util.entry.BlockEntityEntry
import magicbook.whimsnworks.CW2Mod

object CW2BlockEntities {
  val FLANGED_COGWHEEL: BlockEntityEntry[KineticBlockEntity]
    = CW2Mod.REGISTRATE.blockEntity[KineticBlockEntity]("flanged_cogwheel", (t, p, s) => KineticBlockEntity(t, p, s))
    .validBlocks(CW2Blocks.flangedCogwheels: _*)
    .validBlocks(CW2Blocks.largeFlangedCogwheels: _*)
    .register()

  val ENCASED_FLANGED_COGWHEEL: BlockEntityEntry[SimpleKineticBlockEntity]
    = CW2Mod.REGISTRATE.blockEntity[SimpleKineticBlockEntity]("encased_flanged_cogwheel",
        (t, p, s) => SimpleKineticBlockEntity(t, p, s))
      .validBlocks(CW2Blocks.encasedFlangedCogwheels: _*)
      .validBlocks(CW2Blocks.encasedLargeFlangedCogwheels: _*)
      .register()

  def register(): Unit = {}
}
