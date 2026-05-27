package magicbook.whimsnworks.content.block

import com.simibubi.create.content.kinetics.base.{KineticBlockEntity, RotatedPillarKineticBlock}
import com.simibubi.create.content.kinetics.simpleRelays.SimpleKineticBlockEntity
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogwheelBlock
import magicbook.whimsnworks.registration.CW2BlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.{BlockBehaviour, BlockState}
import net.minecraft.world.phys.{BlockHitResult, HitResult}

import scala.jdk.FunctionConverters.*

class EncasedFlangedCogwheelBlock(properties: BlockBehaviour.Properties,
                                  isLarge: Boolean,
                                  casing: () => Block,
                                  cogwheel: () => Block)
  extends EncasedCogwheelBlock(properties, isLarge, casing.asJava) {

  override def getBlockEntityType: BlockEntityType[? <: SimpleKineticBlockEntity]
    = CW2BlockEntities.ENCASED_FLANGED_COGWHEEL.get()

  override def onSneakWrenched(state: BlockState, context: UseOnContext): InteractionResult = {
    if (context.getLevel.isClientSide) return InteractionResult.SUCCESS
    context.getLevel.levelEvent(2001, context.getClickedPos, Block.getId(state))
    KineticBlockEntity.switchToBlockState(context.getLevel, context.getClickedPos, cogwheel.apply()
      .defaultBlockState.setValue(RotatedPillarKineticBlock.AXIS, state.getValue(RotatedPillarKineticBlock.AXIS)))
    InteractionResult.SUCCESS
  }

  override def getCloneItemStack(state: BlockState, target: HitResult, world: LevelReader, 
                                 pos: BlockPos, player: Player): ItemStack = {
    target match {
      case bhr: BlockHitResult =>
        if (bhr.getDirection.getAxis == getRotationAxis(state))
          ItemStack(cogwheel.apply())
        else
          getCasing.asItem.getDefaultInstance
      case _ => super.getCloneItemStack(state, target, world, pos, player)
    }
  }
}