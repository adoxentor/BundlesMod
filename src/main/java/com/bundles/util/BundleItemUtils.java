package com.bundles.util;

import com.bundles.init.BundleResources;
import com.bundles.item.BundleItem;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.util.Constants;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static net.minecraft.item.Items.SHULKER_BOX;

/**
 * Bundle Item Utilities
 *
 * @author JimiIT92
 */
public final class BundleItemUtils {

    /**
     * Check if the Item Stack is a Bundle Item Stack
     *
     * @param bundle Item Stack
     * @return True if is a Bundle Item Stack, False otherwise
     */
    public static boolean isBundle(ItemStack bundle) {
        return bundle.getItem() instanceof BundleItem || bundle.getItem() == SHULKER_BOX;
    }

    /**
     * Check if a Bundle is full
     *
     * @param bundle Bundle Item Stack
     * @return True if the Bundle is full, False otherwise
     */
    public static boolean isFull(ItemStack bundle) {
        if (bundle.getItem() == SHULKER_BOX) {
            return getItemsFromBundle(bundle).stream().filter(itemStack -> !itemStack.isEmpty()).count() > 27;
        }
        return getBundleItemsCount(bundle) >= bundle.getMaxDamage();
    }

    /**
     * Check if the Bundle is empty
     *
     * @param bundle Bundle Item Stack
     * @return True if the Bundle is empty, False otherwise
     */
    public static boolean isEmpty(ItemStack bundle) {
        return getBundleItemsCount(bundle) == 0;
    }

    /**
     * Check if an Item Stack can be added to a Bundle
     *
     * @param bundle Bundle Item Stack
     * @param stack  Item Stack to add
     * @return True if the Item Stack can be added to a Bundle, False otherwise
     */
    public static boolean canAddItemStackToBundle(ItemStack bundle, ItemStack stack) {
        if (bundle.getItem() == SHULKER_BOX) {
            return !isIgnored(stack)
                && (getItemsFromBundle(bundle).isEmpty()
                || getItemsFromBundle(bundle).stream().
                anyMatch(itemStack -> itemStack.isEmpty()
                    || (ItemStack.areItemStacksEqual(itemStack, stack) && itemStack.getCount() < itemStack.getMaxStackSize()))
            );
        }

        if (!isBundle(bundle) || isFull(bundle) || isIgnored(stack)) {
            return false;
        }
        ItemStack bundleItemStack = getItemStackFor(bundle, stack);
        return bundleItemStack.isEmpty() || stack.getMaxStackSize() == 1 || bundleItemStack.getCount() < getMaxStackSizeForBundle(stack);
    }

    /**
     * Check if an Item Stack is for a Container Block
     *
     * @param stack Item Stack
     * @return True if the Item Stack is for a Container Block, False otherwise
     */
    private static boolean isIgnored(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof BlockItem) {
            ITag<Block> blockTag = BlockTags.getCollection().get(BundleResources.BUNDLE_IGNORED_BLOCKS_TAG);
            return blockTag != null && blockTag.contains(((BlockItem) item).getBlock());
        }
        ITag<Item> itemTag = ItemTags.getCollection().get(BundleResources.BUNDLE_IGNORED_ITEMS_TAG);
        return itemTag != null && itemTag.contains(item);
    }

    /**
     * Add an Item Stack to a Bundle
     *
     * @param bundle Bundle Item Stack
     * @param stack  Item Stack to add
     */
    public static void addItemStackToBundle(ItemStack bundle, ItemStack stack) {
        if (bundle.getItem() == SHULKER_BOX) {
            CompoundNBT compoundnbt = bundle.getChildTag("BlockEntityTag");
            if (compoundnbt == null) {
                compoundnbt = new CompoundNBT();
            }
            if (!compoundnbt.contains("Items", 9)) {
                compoundnbt.put("Items", new ListNBT());
            }
            NonNullList<ItemStack> nonnulllist = NonNullList.withSize(27, ItemStack.EMPTY);
            ItemStackHelper.loadAllItems(compoundnbt, nonnulllist);
            for (int i = 0; i < 27; i++) {
                ItemStack itemStack = nonnulllist.get(i);
                if (ItemStack.areItemsEqual(itemStack, stack) && itemStack.getCount() < itemStack.getMaxStackSize()) {
                    int j = itemStack.getCount() + stack.getCount();
                    int maxSize = stack.getMaxStackSize();
                    if (j <= maxSize) {
                        stack.setCount(0);
                        itemStack.setCount(j);
                        break;
                    } else if (itemStack.getCount() < maxSize) {
                        stack.shrink(maxSize - itemStack.getCount());
                        itemStack.setCount(maxSize);
                    }
                }
            }
            if (!stack.isEmpty()) {
                for (int i = 0; i < 27; i++) {
                    ItemStack itemStack = nonnulllist.get(i);
                    if (itemStack.isEmpty()) {
                        nonnulllist.set(i, stack.copy());
                        stack.setCount(0);
                        break;
                    }
                }
            }

//            nonnulllist.add(stack);
            ItemStackHelper.saveAllItems(compoundnbt, nonnulllist, true);
            bundle.setTagInfo("BlockEntityTag", compoundnbt);
            return;
        }

        if (!isBundle(bundle) || isFull(bundle) || isBundle(stack)) {
            return;
        }
        ItemStack stackToAdd = stack.copy();
        int maxItemsToAdd = bundle.getMaxDamage() - getBundleItemsCount(bundle);
        stackToAdd.setCount(Math.min(getMaxStackSizeForBundleToInsert(stackToAdd), maxItemsToAdd));
        CompoundNBT bundleTag = bundle.getOrCreateTag();
        ListNBT items = bundleTag.getList(BundleResources.BUNDLE_ITEMS_LIST_NBT_RESOURCE_LOCATION, Constants.NBT.TAG_COMPOUND);
        CompoundNBT itemStackNbt = new CompoundNBT();
        ItemStack stackFromBundle = getItemStackFor(bundle, stackToAdd);
        int index = getItemStackIndex(bundle, stackFromBundle);
        if (!stackFromBundle.isEmpty() && stack.getMaxStackSize() > 1) {
            stackToAdd.setCount(Math.min(stackToAdd.getCount(), getMaxStackSizeForBundle(stack) - stackFromBundle.getCount()));
            stackFromBundle.setCount(stackFromBundle.getCount() + stackToAdd.getCount());
        }
        if (index != -1 && stack.getMaxStackSize() > 1) {
            stackFromBundle.write(itemStackNbt);
            items.remove(index);
            items.add(index, itemStackNbt);
        } else {
            stackToAdd.write(itemStackNbt);
            items.add(itemStackNbt);
        }
        bundleTag.put(BundleResources.BUNDLE_ITEMS_LIST_NBT_RESOURCE_LOCATION, items);
        bundle.setTag(bundleTag);
        stack.setCount(stack.getCount() - stackToAdd.getCount());
    }

    /**
     * Empty a Bundle
     *
     * @param bundle Bundle
     * @param player Player
     */
    public static void emptyBundle(ItemStack bundle, PlayerEntity player) {
        if (!isBundle(bundle) || isEmpty(bundle)) {
            return;
        }
        getItemsFromBundle(bundle).forEach(item -> {
            if (!player.addItemStackToInventory(item)) {
                if (!player.isCreative()) {
                    player.dropItem(item, true);
                }
            } else if (item.getCount() > 0) {
                if (!player.isCreative()) {
                    player.dropItem(item, true);
                }
            }
        });
        if (bundle.getItem() == SHULKER_BOX) {
            CompoundNBT compoundnbt = bundle.getChildTag("BlockEntityTag");
            if (compoundnbt == null) {
                compoundnbt = new CompoundNBT();
            }
            compoundnbt.put("Items", new ListNBT());
            bundle.setTagInfo("BlockEntityTag", compoundnbt);
            return;

        }
        CompoundNBT bundleTag = bundle.getOrCreateTag();
        ListNBT items = bundleTag.getList(BundleResources.BUNDLE_ITEMS_LIST_NBT_RESOURCE_LOCATION, Constants.NBT.TAG_COMPOUND);
        items.clear();
        bundleTag.put(BundleResources.BUNDLE_ITEMS_LIST_NBT_RESOURCE_LOCATION, items);
        bundle.setTag(bundleTag);
    }

    /**
     * Get how many Items are inside the Bundle
     *
     * @param bundle Bundle Item Stack
     * @return Bundle Items Count
     */
    public static int getBundleItemsCount(ItemStack bundle) {
        return Objects.requireNonNull(getItemsFromBundle(bundle)).stream().mapToInt(ItemStack::getCount).sum();
    }

    /**
     * Get the Item Stacks inside the Bundle
     *
     * @param bundle Bundle Item Stack
     * @return Bundle's Item Stacks
     */
    public static List<ItemStack> getItemsFromBundle(ItemStack bundle) {
        if (bundle.getItem() == SHULKER_BOX) {
            CompoundNBT compoundnbt = bundle.getChildTag("BlockEntityTag");
            if (compoundnbt == null) {
                return Collections.emptyList();
            }
            if (compoundnbt.contains("Items", 9)) {
                NonNullList<ItemStack> nonnulllist = NonNullList.withSize(27, ItemStack.EMPTY);
                ItemStackHelper.loadAllItems(compoundnbt, nonnulllist);
                return nonnulllist;
            }
            return Collections.emptyList();
        }
        if (!isBundle(bundle)) {
            return Collections.emptyList();
        }
        CompoundNBT bundleTag = bundle.getOrCreateTag();
        ListNBT items = bundleTag.getList(BundleResources.BUNDLE_ITEMS_LIST_NBT_RESOURCE_LOCATION, Constants.NBT.TAG_COMPOUND);
        return items.stream().map(x -> ItemStack.read((CompoundNBT) x)).collect(Collectors.toList());
    }

    /**
     * Get the Item Stack for an Item
     *
     * @param bundle Bundle Item Stack
     * @param stack  Item Stack
     * @return Item Stack for the Item or Empty Item Stack if not found
     */
    private static ItemStack getItemStackFor(ItemStack bundle, ItemStack stack) {
        return getItemsFromBundle(bundle).stream().filter(x -> ItemStack.areItemStacksEqual(x, stack)).findFirst().orElse(ItemStack.EMPTY);
    }

    /**
     * Get the Item Stack index inside the Bundle
     *
     * @param bundle Bundle Item Stack
     * @param stack  Item Stack to find
     * @return Item Stack index
     */
    private static int getItemStackIndex(ItemStack bundle, ItemStack stack) {
        List<ItemStack> items = getItemsFromBundle(bundle);
        return IntStream.range(0, items.size())
            .filter(i -> stack.equals(items.get(i), false))
            .findFirst().orElse(-1);
    }

    /**
     * Get the max stack size for an Item Stack
     * to be put inside a Bundle
     *
     * @param stack Item Stack
     * @return Max stack size for a Bundle
     */
    private static int getMaxStackSizeForBundleToInsert(ItemStack stack) {
        return Math.max(1, Math.min(stack.getCount(), stack.getMaxStackSize() / 2));
    }

    /**
     * Get the max stack size allowed inside
     * a Bundle for an Item
     *
     * @param stack Item Stack
     * @return Max Item Stack size inside the Bundle
     */
    private static int getMaxStackSizeForBundle(ItemStack stack) {
        return Math.max(1, stack.getMaxStackSize() / 2);
    }

    public static ItemStack removeFirstItemStack(ItemStack bundle, boolean reversed) {
        ItemStack stack = ItemStack.EMPTY;
        if (bundle.getItem() == SHULKER_BOX) {
            CompoundNBT compoundnbt = bundle.getChildTag("BlockEntityTag");
            if (compoundnbt == null) {
                compoundnbt = new CompoundNBT();
            }
            if (!compoundnbt.contains("Items", 9)) {
                compoundnbt.put("Items", new ListNBT());
            }
            NonNullList<ItemStack> nonnulllist = NonNullList.withSize(27, ItemStack.EMPTY);
            ItemStackHelper.loadAllItems(compoundnbt, nonnulllist);
            if (reversed) {
                for (int i = 26; i >= 0; i--) {
                    ItemStack itemStack = nonnulllist.get(i);
                    if (!itemStack.isEmpty()) {
                        nonnulllist.set(i, ItemStack.EMPTY);
                        stack = itemStack;
                        break;
                    }
                }
            } else {
                for (int i = 0; i < 27; i++) {
                    ItemStack itemStack = nonnulllist.get(i);
                    if (!itemStack.isEmpty()) {
                        nonnulllist.set(i, ItemStack.EMPTY);
                        stack = itemStack;
                        break;
                    }
                }
            }

//            nonnulllist.add(stack);
            ItemStackHelper.saveAllItems(compoundnbt, nonnulllist, true);
            bundle.setTagInfo("BlockEntityTag", compoundnbt);
            return stack;
        }

        if (!isBundle(bundle) || isEmpty(bundle)) {
            return stack;
        }
//        ItemStack stackToAdd = stack.copy();
        CompoundNBT bundleTag = bundle.getOrCreateTag();
        ListNBT items = bundleTag.getList(BundleResources.BUNDLE_ITEMS_LIST_NBT_RESOURCE_LOCATION, Constants.NBT.TAG_COMPOUND);
        CompoundNBT itemStackNbt = new CompoundNBT();
        List<ItemStack> itemsFromBundle = getItemsFromBundle(bundle);
        int index = reversed ? 0 : itemsFromBundle.size() - 1;
        stack = itemsFromBundle.get(index);
        items.remove(index);

        bundleTag.put(BundleResources.BUNDLE_ITEMS_LIST_NBT_RESOURCE_LOCATION, items);
        bundle.setTag(bundleTag);
        return stack;
    }
}
