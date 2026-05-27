package magicbook.whimsnworks.content.block

import com.kipti.bnb.registry.BnbShapes
import com.simibubi.create.content.decoration.encasing.EncasableBlock
import com.simibubi.create.content.kinetics.base.{KineticBlockEntity, RotatedPillarKineticBlock}
import com.simibubi.create.foundation.block.IBE
import magicbook.whimsnworks.registration.CW2BlockEntities
import net.minecraft.core.{BlockPos, Direction}
import net.minecraft.world.{InteractionHand, ItemInteractionResult}
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.{BlockGetter, Level, LevelReader}
import net.minecraft.world.level.block.state.{BlockBehaviour, BlockState}
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.{CollisionContext, VoxelShape}

class FlangedCogwheelBlock(properties: BlockBehaviour.Properties, val isLarge: Boolean)
  extends RotatedPillarKineticBlock(properties)
  with IBE[KineticBlockEntity]
  with EncasableBlock {

  override def useItemOn(stack: ItemStack, state: BlockState, level: Level, pos: BlockPos,
                         player: Player, hand: InteractionHand, hitResult: BlockHitResult): ItemInteractionResult = {
    val result = tryEncase(state, level, pos, stack, player, hand, hitResult)
    if (result.consumesAction) return result
    ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
  }

  override def getShape(state: BlockState, level: BlockGetter, pos: BlockPos,
                        context: CollisionContext): VoxelShape
    = (if (isLarge) BnbShapes.LARGE_GEAR else BnbShapes.SMALL_GEAR).get(state.getValue(RotatedPillarKineticBlock.AXIS))

  override def getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

  override def getRotationAxis(state: BlockState): Direction.Axis = state.getValue(RotatedPillarKineticBlock.AXIS)

  override def hasShaftTowards(world: LevelReader, pos: BlockPos, state: BlockState, face: Direction): Boolean
    = state.getValue(RotatedPillarKineticBlock.AXIS) == face.getAxis

  override def getBlockEntityClass: Class[KineticBlockEntity] = classOf[KineticBlockEntity]

  override def getBlockEntityType: BlockEntityType[? <: KineticBlockEntity] = CW2BlockEntities.FLANGED_COGWHEEL.get()
}

object FlangedCogwheelBlock {
  def small(properties: BlockBehaviour.Properties): FlangedCogwheelBlock = FlangedCogwheelBlock(properties, false)

  def large(properties: BlockBehaviour.Properties): FlangedCogwheelBlock = FlangedCogwheelBlock(properties, true)
}
