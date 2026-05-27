package magicbook.whimsnworks.registration

import com.simibubi.create.content.decoration.encasing.EncasingRegistry
import com.simibubi.create.foundation.data.{BlockStateGen, SharedProperties, TagGen}
import com.tterrag.registrate.util.entry.BlockEntry
import fr.iglee42.createcasing.casings.CasingSets
import magicbook.whimsnworks.CW2Mod
import magicbook.whimsnworks.content.block.{EncasedFlangedCogwheelBlock, FlangedCogwheelBlock}
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.material.MapColor

import scala.jdk.CollectionConverters.*

object CW2Blocks {

  private val woodTypes: Seq[String] = Seq("acacia", "birch", "bamboo", "cherry", "crimson", "dark_oak", "oak", "jungle",
    "mangrove", "warped", "spruce") // TODO: Merge with CW2Transmission Seq?
  
  val flangedCogwheels: Seq[BlockEntry[FlangedCogwheelBlock]] = woodTypes.map { wood =>
    CW2Mod.REGISTRATE.block[FlangedCogwheelBlock](s"${wood}_flanged_cogwheel", p => FlangedCogwheelBlock.small(p))
      .initialProperties(() => SharedProperties.wooden)
      .properties(p => p.sound(SoundType.WOOD).mapColor(MapColor.DIRT))
      .transform(com.simibubi.create.foundation.data.TagGen.axeOrPickaxe())
      .blockstate((c, p) => BlockStateGen.axisBlock(c, p,
        s => p.models().getExistingFile(CW2Mod.id(s"block/flanged_gear/${wood}_small_cogwheel"))))
      .item()
      .model((c, p) => p.withExistingParent(c.getName, CW2Mod.id(s"block/flanged_gear/${wood}_small_cogwheel")))
      .build()
      .register()
  }

  val largeFlangedCogwheels: Seq[BlockEntry[FlangedCogwheelBlock]] = woodTypes.map { wood =>
    CW2Mod.REGISTRATE.block[FlangedCogwheelBlock](s"${wood}_large_flanged_cogwheel", p => FlangedCogwheelBlock.large(p))
      .initialProperties(() => SharedProperties.wooden)
      .properties(p => p.sound(SoundType.WOOD).mapColor(MapColor.DIRT))
      .transform(TagGen.axeOrPickaxe())
      .blockstate((c, p) => BlockStateGen.axisBlock(c, p,
        s => p.models().getExistingFile(CW2Mod.id(s"block/flanged_gear/${wood}_large_cogwheel"))))
      .item()
      .model((c, p) => p.withExistingParent(c.getName, CW2Mod.id(s"block/flanged_gear/${wood}_large_cogwheel")))
      .build()
      .register()
  }
  
  private val smallFlangedByWood: Map[String, BlockEntry[FlangedCogwheelBlock]] = woodTypes.zip(flangedCogwheels).toMap
  private val largeFlangedByWood: Map[String, BlockEntry[FlangedCogwheelBlock]] = woodTypes.zip(largeFlangedCogwheels).toMap

  private val encasedFlangedCogwheelsBuilder = Seq.newBuilder[BlockEntry[EncasedFlangedCogwheelBlock]]
  private val encasedLargeFlangedCogwheelsBuilder = Seq.newBuilder[BlockEntry[EncasedFlangedCogwheelBlock]]

  for {
    casingSet <- CasingSets.getSets.asScala if casingSet.doesGenerateEncasedWoodenCogwheel
    woodInfo <- CW2TransmissionSets.woodTypes
  } {
    val setName = woodInfo.name + "_flanged"
    val woodName = woodInfo.name
    val casingName = casingSet.getName
    val casingSupplier = () => casingSet.getCasing

    smallFlangedByWood.get(woodName).foreach { entry =>
      val encasedSmall = CW2Mod.REGISTRATE.block[EncasedFlangedCogwheelBlock](s"${casingName}_encased_${setName}_cogwheel",
          p => EncasedFlangedCogwheelBlock(p, false, casingSupplier, () => entry.get()))
        .properties(p => p.mapColor(MapColor.PODZOL))
        .initialProperties(() => SharedProperties.stone)
        .properties(p => p.noOcclusion)
        .blockstate((c, p) => BlockStateGen.axisBlock(c, p,
          s => p.models().getExistingFile(CW2Mod.id(s"block/flanged_gear/${woodName}_encased_cogwheel"))))
        .item()
        .model((c, p) => p.withExistingParent(c.getName, CW2Mod.id(s"block/flanged_gear/${woodName}_encased_cogwheel")))
        .build()
        .transform(TagGen.axeOrPickaxe())
        .transform(EncasingRegistry.addVariantTo(() => entry.get()))
        .loot((p, lb) => p.dropOther(lb, entry.get()))
        .register()
      encasedFlangedCogwheelsBuilder += encasedSmall
    }

    largeFlangedByWood.get(woodName).foreach { entry =>
      val encasedLarge = CW2Mod.REGISTRATE.block[EncasedFlangedCogwheelBlock](s"${casingName}_encased_${setName}_large_cogwheel",
          p => EncasedFlangedCogwheelBlock(p, true, casingSupplier, () => entry.get()))
        .properties(p => p.mapColor(MapColor.PODZOL))
        .initialProperties(() => SharedProperties.stone)
        .properties(p => p.noOcclusion)
        .blockstate((c, p) => BlockStateGen.axisBlock(c, p,
          s => p.models().getExistingFile(CW2Mod.id(s"block/flanged_gear/${woodName}_encased_large_cogwheel"))))
        .item()
        .model((c, p) => p.withExistingParent(c.getName, CW2Mod.id(s"block/flanged_gear/${woodName}_encased_large_cogwheel")))
        .build()
        .transform(TagGen.axeOrPickaxe())
        .transform(EncasingRegistry.addVariantTo(() => entry.get()))
        .loot((p, lb) => p.dropOther(lb, entry.get()))
        .register()
      encasedLargeFlangedCogwheelsBuilder += encasedLarge
    }
  }

  val encasedFlangedCogwheels: Seq[BlockEntry[EncasedFlangedCogwheelBlock]] = encasedFlangedCogwheelsBuilder.result()
  val encasedLargeFlangedCogwheels: Seq[BlockEntry[EncasedFlangedCogwheelBlock]] = encasedLargeFlangedCogwheelsBuilder.result()

  def register(): Unit = {}
}
