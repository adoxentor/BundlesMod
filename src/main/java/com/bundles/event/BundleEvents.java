package com.bundles.event;

import com.bundles.init.BundleResources;
import com.bundles.network.message.BundleServerMessage;
import com.bundles.util.BundleItemUtils;
import com.bundles.util.BundleTooltipUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.screen.inventory.CreativeScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.CraftingResultSlot;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.lang.reflect.Field;

import static com.bundles.network.handler.BundleServerMessageHandler.processMessage;

/**
 * Bundle Events
 *
 * @author JimiIT92
 */
public final class BundleEvents {
    private static Slot oldSelectedSlot = null;
    private static boolean filling = false;

    /**
     * Handle mouse clicks on Containers
     * to determine if an Item Stack should
     * be put inside a Bundle
     *
     * @param event Mouse Released Event
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseReleased(final GuiScreenEvent.MouseReleasedEvent event) {
        oldSelectedSlot = null;
    }

    /**
     * Handle mouse clicks on Containers
     * to determine if an Item Stack should
     * be put inside a Bundle
     *
     * @param event Mouse Released Event
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseClicked1(final GuiScreenEvent.MouseClickedEvent event) {
        if (!event.isCanceled()
            && event.getGui() instanceof ContainerScreen<?>
            && event.getButton() == 1) {
            ContainerScreen<?> containerScreen = (ContainerScreen<?>) event.getGui();
            Slot slot = containerScreen.getSlotUnderMouse();
            PlayerEntity player = Minecraft.getInstance().player;
            oldSelectedSlot = slot;
            if (slot != null
                && !(slot instanceof CraftingResultSlot)
                && player != null
                && slot.canTakeStack(player)
                && slot.isEnabled()) {
                ItemStack draggedItemStack = player.inventory.getItemStack();
                ItemStack slotStack = slot.getStack();
                Container container = containerScreen.getContainer();
                if (container.canMergeSlot(draggedItemStack, slot)
                    && slot.isItemValid(draggedItemStack)
                    && slot.getHasStack()
                    && BundleItemUtils.isBundle(draggedItemStack)
                    && BundleItemUtils.canAddItemStackToBundle(draggedItemStack, slotStack)
                ) {
                    try {
                        filling = true;
                        Field slotIndexField = getSlotIndexField();
                        if (slotIndexField != null) {
                            slotIndexField.setAccessible(true);
                            int slotIndex = player.isCreative() && container instanceof CreativeScreen.CreativeContainer ?
                                (int) slotIndexField.get(slot)
                                : slot.slotNumber;
                            BundleServerMessage message = new BundleServerMessage(draggedItemStack, slotIndex, false, Screen.hasShiftDown());
                            processMessage(message, player);
                            BundleResources.NETWORK.sendToServer(message);
                            event.setResult(Event.Result.DENY);
                            event.setCanceled(true);
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                } else if (slot.canTakeStack(player) && slot.isEnabled()
                    && container.canMergeSlot(draggedItemStack, slot)
                    && slot.isItemValid(draggedItemStack)
                    && !slot.getHasStack()
                    && BundleItemUtils.isBundle(draggedItemStack)
                    && BundleItemUtils.getItemsFromBundle(draggedItemStack).size() > 0
                ) {
                    try {
                        filling = false;
                        Field slotIndexField = getSlotIndexField();
                        if (slotIndexField != null) {
                            slotIndexField.setAccessible(true);
                            int slotIndex = player.isCreative() && container instanceof CreativeScreen.CreativeContainer ?
                                (int) slotIndexField.get(slot)
                                : slot.slotNumber;
                            BundleResources.NETWORK.sendToServer(new BundleServerMessage(draggedItemStack, slotIndex, false, Screen.hasShiftDown()));
                            event.setResult(Event.Result.DENY);
                            event.setCanceled(true);
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
            oldSelectedSlot = slot;
        }
    }

    /**
     * Handle mouse clicks on Containers
     * to determine if an Item Stack should
     * be put inside a Bundle
     *
     * @param event Mouse Released Event
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseDrag(final GuiScreenEvent.MouseDragEvent event) {
        if (!event.isCanceled()
            && event.getGui() instanceof ContainerScreen<?>
            && event.getMouseButton() == 1) {
            ContainerScreen<?> containerScreen = (ContainerScreen<?>) event.getGui();
            Slot slot = containerScreen.getSlotUnderMouse();
            PlayerEntity player = Minecraft.getInstance().player;
            if (slot == oldSelectedSlot || slot == null)
                return;
            if (!(slot instanceof CraftingResultSlot)
                && player != null
                && slot.canTakeStack(player)
                && slot.isEnabled()) {
                ItemStack draggedItemStack = player.inventory.getItemStack();
                ItemStack slotStack = slot.getStack();
                Container container = containerScreen.getContainer();
                if ((filling && oldSelectedSlot != null)
                    && container.canMergeSlot(draggedItemStack, slot)
                    && slot.isItemValid(draggedItemStack)
                    && slot.getHasStack()
                    && BundleItemUtils.isBundle(draggedItemStack)
                    && BundleItemUtils.canAddItemStackToBundle(draggedItemStack, slotStack)
                ) {
                    try {
                        Field slotIndexField = getSlotIndexField();
                        if (slotIndexField != null) {
                            slotIndexField.setAccessible(true);
                            int slotIndex = player.isCreative() && container instanceof CreativeScreen.CreativeContainer ?
                                (int) slotIndexField.get(slot)
                                : slot.slotNumber;
                            BundleServerMessage message = new BundleServerMessage(draggedItemStack, slotIndex, false, Screen.hasShiftDown());
                            processMessage(message, player);
                            BundleResources.NETWORK.sendToServer(message);
                            event.setResult(Event.Result.DENY);
                            event.setCanceled(true);
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                } else if ((!filling && oldSelectedSlot != null) &&
                    slot.canTakeStack(player) && slot.isEnabled()
                    && container.canMergeSlot(draggedItemStack, slot)
                    && slot.isItemValid(draggedItemStack)
                    && !slot.getHasStack()
                    && BundleItemUtils.isBundle(draggedItemStack)
                    && BundleItemUtils.getItemsFromBundle(draggedItemStack).size() > 0
                ) {
                    try {
                        Field slotIndexField = getSlotIndexField();
                        if (slotIndexField != null) {
                            slotIndexField.setAccessible(true);
                            int slotIndex = player.isCreative() && container instanceof CreativeScreen.CreativeContainer ?
                                (int) slotIndexField.get(slot)
                                : slot.slotNumber;
                            BundleResources.NETWORK.sendToServer(new BundleServerMessage(draggedItemStack, slotIndex, false, Screen.hasShiftDown()));
                            event.setResult(Event.Result.DENY);
                            event.setCanceled(true);
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
            oldSelectedSlot = slot;

        }
    }

    /**
     * Handle mouse clicks on Containers
     * to determine if a Bundle should be cleared
     *
     * @param event Mouse Clicked Event
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseClick(final GuiScreenEvent.MouseClickedEvent event) {
        if (!event.isCanceled() && event.getGui() instanceof ContainerScreen<?>) {
            ContainerScreen<?> containerScreen = (ContainerScreen<?>) event.getGui();
            Slot slot = containerScreen.getSlotUnderMouse();
            if (slot != null && !(slot instanceof CraftingResultSlot)) {
                PlayerEntity player = Minecraft.getInstance().player;
                if (player != null) {
                    ItemStack slotStack = slot.getStack();
                    if (slot.canTakeStack(player) && slot.isEnabled()
                        && slot.getHasStack() && event.getButton() == 1
                        && BundleItemUtils.isBundle(slotStack)) {
                        try {
                            Field slotIndexField = getSlotIndexField();
                            if (slotIndexField != null) {
                                slotIndexField.setAccessible(true);
                                int slotIndex = player.isCreative() && containerScreen.getContainer() instanceof CreativeScreen.CreativeContainer ?
                                    (int) slotIndexField.get(slot)
                                    : slot.slotNumber;
                                BundleResources.NETWORK.sendToServer(new BundleServerMessage(slotStack, player.isCreative() ? slotIndex : slot.slotNumber, true, net.minecraft.client.gui.screen.Screen.hasShiftDown()));
                                event.setResult(Event.Result.DENY);
                                event.setCanceled(true);
                            }
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }


    /**
     * Get the Slot Index Field
     *
     * @return Slot Index Field
     */
    private static Field getSlotIndexField() {
        Field slotIndexField = null;
        try {
            slotIndexField = Slot.class.getDeclaredField("slotIndex");
        } catch (NoSuchFieldException e) {
            try {
                slotIndexField = Slot.class.getDeclaredField("field_75225_a");
            } catch (NoSuchFieldException ex) {
                ex.printStackTrace();
            }
        }
        return slotIndexField;
    }

    /**
     * Draw the Bundle Tooltip
     *
     * @param event Render Tooltip Event
     */
    @SubscribeEvent
    public static void onTooltipRender(final RenderTooltipEvent.Pre event) {
        if (BundleItemUtils.isBundle(event.getStack())) {
            event.setCanceled(true);
            BundleTooltipUtil.drawBundleTooltip(event);
        }
    }
}
