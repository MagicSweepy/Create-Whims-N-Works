package magicbook.whimsnworks.registration

import com.simibubi.create.AllCreativeModeTabs as CTCreativeModeTabs
import com.simibubi.create.foundation.data.CreateRegistrate
import magicbook.whimsnworks.CW2Mod
import magicbook.whimsnworks.api.util.ItemUtil
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.{BlockItem, CreativeModeTab, Item, ItemStack, Items}
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.loading.FMLEnvironment
import net.neoforged.fml.util.thread.EffectiveSide
import net.neoforged.neoforge.registries.{DeferredHolder, DeferredRegister}

import scala.collection.mutable
import scala.jdk.CollectionConverters.*

object CW2CreativeModeTabs {

  private val TAB_REGISTER: DeferredRegister[CreativeModeTab] = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CW2Mod.ID)

  val MAIN: DeferredHolder[CreativeModeTab, CreativeModeTab] = TAB_REGISTER.register("main", _ => CreativeModeTab.builder()
    .title(Component.translatable(s"itemGroup.${CW2Mod.ID}.main"))
    .withTabsBefore(CTCreativeModeTabs.BASE_CREATIVE_TAB.getKey)
    .icon(() => ItemStack(CW2Blocks.flangedCogwheels.head.asItem()))
    .displayItems(RegistrateDisplayItemsGenerator(true, MAIN))
    .build())
  
  def register(modEvent: IEventBus): Unit = {
      TAB_REGISTER.register(modEvent)
  } 
  
  private class RegistrateDisplayItemsGenerator(addItems: Boolean,
                                                tabFilter: DeferredHolder[CreativeModeTab, CreativeModeTab])
    extends CreativeModeTab.DisplayItemsGenerator {

      override def accept(params: CreativeModeTab.ItemDisplayParameters, output: CreativeModeTab.Output): Unit = {
        if (EffectiveSide.get().isServer) return
        if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) return

        val exclusionPredicate = makeExclusionPredicate
        val orderings = makeOrderings
        val stackFunc = makeStackFunc
        val visibilityFunc = makeVisibilityFunc

        val rawItems: Seq[Item] = {
          if (addItems) {
            val itemsNon3d = collectItems(item => exclusionPredicate(item) || !ItemUtil.is3d(item))
            val blocks     = collectBlocks(exclusionPredicate)
            val items3d    = collectItems(item => exclusionPredicate(item) || ItemUtil.is3d(item))
            itemsNon3d ++ blocks ++ items3d
          } else {
            collectBlocks(exclusionPredicate)
          }
        }

        val sortedItems = applyOrderings(rawItems, orderings)
        outputAll(output, sortedItems, stackFunc, visibilityFunc)
      }

      private def makeExclusionPredicate: Item => Boolean = {
          val exclusions = mutable.HashSet.empty[Item]
          item => exclusions.contains(item)
      }

      private def makeOrderings: Seq[ItemOrdering] = Seq.empty[ItemOrdering]

      private def makeStackFunc: Item => ItemStack = item => ItemStack(item)

      private def makeVisibilityFunc: Item => CreativeModeTab.TabVisibility 
        = _ => CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS

      private def collectBlocks(exclusionPredicate: Item => Boolean): Seq[Item] = {
        CW2Mod.REGISTRATE.getAll(Registries.BLOCK).asScala
          .filter(entry => CreateRegistrate.isInCreativeTab(entry, tabFilter))
          .map(_.get().asItem())
          .filter(item => item != Items.AIR && !exclusionPredicate(item))
          .toList
          .distinct
      }

      private def collectItems(exclusionPredicate: Item => Boolean): Seq[Item] = {
        CW2Mod.REGISTRATE.getAll(Registries.ITEM).asScala
          .filter(entry => CreateRegistrate.isInCreativeTab(entry, tabFilter))
          .map(_.get())
          .filter(item => !item.isInstanceOf[BlockItem] && !exclusionPredicate(item))
          .toList
          .distinct
      }

      private def applyOrderings(items: Seq[Item], orderings: Seq[ItemOrdering]): Seq[Item] = {
        orderings.foldLeft(items) { (currentItems, ordering) =>
          val anchorIdx = currentItems.indexOf(ordering.anchor)
          if (anchorIdx < 0) {
            currentItems
          } else {
            val withoutItem = currentItems.filterNot(_ == ordering.item)
            val newAnchorIdx = withoutItem.indexOf(ordering.anchor)
            val insertIdx = ordering.orderingType match {
              case OrderingType.After => newAnchorIdx + 1
              case _                  => newAnchorIdx
            }
            val (left, right) = withoutItem.splitAt(insertIdx)
            left ++ (ordering.item +: right)
          }
        }
      }

      private def outputAll(output: CreativeModeTab.Output,
                            items: Seq[Item],
                            stackFunc: Item => ItemStack,
                            visibilityFunc: Item => CreativeModeTab.TabVisibility): Unit = {
        items.foreach(item => output.accept(stackFunc(item), visibilityFunc(item)))
      }
  }

  sealed trait OrderingType
  private object OrderingType {
    case object Before extends OrderingType
    case object After extends OrderingType
  }

  case class ItemOrdering(item: Item, anchor: Item, orderingType: OrderingType)
  object ItemOrdering {
    def before(item: Item, anchor: Item): ItemOrdering =
      ItemOrdering(item, anchor, OrderingType.Before)

    def after(item: Item, anchor: Item): ItemOrdering =
      ItemOrdering(item, anchor, OrderingType.After)
  }
}
