package com.bundles.util;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.text.LanguageMap;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.gui.GuiUtils;

import java.util.*;

/**
 * @author JimiIT92
 */
public class BundleTooltipUtil {

    private static List<ItemStack> CACHED_ITEM_STACKS;
    private static List<ItemStack> CACHED_TOOLTIP_ITEM_STACKS;
    private static HashMap<ItemStack, Map.Entry<Integer, Integer>> CACHED_TOOLTIP_POSITIONS;

    /**
     * Draw the Bundle Tooltip
     *
     * @param event Render Tooltip Event
     */
    public static void drawBundleTooltip(final RenderTooltipEvent.Pre event)
    {
        final ItemStack stack = event.getStack();
        MatrixStack mStack = event.getMatrixStack();
        List<? extends ITextProperties> textLines = event.getLines();
        int mouseX = event.getX();
        int mouseY = event.getY();
        int screenWidth = event.getScreenWidth();
        int screenHeight = event.getScreenHeight();
        int maxTextWidth = event.getMaxWidth();
        FontRenderer font = event.getFontRenderer();
        int backgroundColor = GuiUtils.DEFAULT_BACKGROUND_COLOR;
        int borderColorStart = GuiUtils.DEFAULT_BORDER_COLOR_START;
        int borderColorEnd = GuiUtils.DEFAULT_BORDER_COLOR_END;
        List<ItemStack> bundleItems = BundleItemUtils.getItemsFromBundle(stack);
        boolean useCached = CACHED_ITEM_STACKS != null && bundleItems.size() == CACHED_ITEM_STACKS.size();
        if(useCached) {
            for (int i = 0; i < bundleItems.size(); i++) {
                if(!ItemStack.areItemStacksEqual(bundleItems.get(i), CACHED_ITEM_STACKS.get(i))) {
                    useCached = false;
                    break;
                }
            }
        }
        if(!useCached) {
            CACHED_ITEM_STACKS = bundleItems;
            CACHED_TOOLTIP_POSITIONS = new HashMap<>();
            List<ItemStack> tooltipItems = new ArrayList<>();
            Random random = new Random();
            bundleItems.forEach(x -> {
                for (int i = 0; i < x.getCount(); i++) {
                    ItemStack tooltipStack = x.copy();
                    tooltipStack.setCount(1);
                    tooltipStack.setDisplayName(new StringTextComponent("stack_" + i));
                    tooltipItems.add(tooltipStack);
                    CACHED_TOOLTIP_POSITIONS.put(tooltipStack,
                            new AbstractMap.SimpleEntry<>(6 + random.nextInt(4) , 4 + random.nextInt(4)));
                }
            });
            Collections.shuffle(tooltipItems);
            CACHED_TOOLTIP_ITEM_STACKS = tooltipItems;
        }

        if (!textLines.isEmpty())
        {
            RenderSystem.disableRescaleNormal();
            RenderSystem.disableDepthTest();
            int tooltipTextWidth = 0;

            for (ITextProperties textLine : textLines)
            {
                int textLineWidth = font.func_238414_a_(textLine);
                if (textLineWidth > tooltipTextWidth)
                    tooltipTextWidth = textLineWidth;
            }

            boolean needsWrap = false;

            int titleLinesCount = 1;
            int tooltipX = mouseX + 12;
            if (tooltipX + tooltipTextWidth + 4 > screenWidth)
            {
                tooltipX = mouseX - 16 - tooltipTextWidth;
                if (tooltipX < 4)
                {
                    if (mouseX > screenWidth / 2)
                        tooltipTextWidth = mouseX - 12 - 8;
                    else
                        tooltipTextWidth = screenWidth - 16 - mouseX;
                    needsWrap = true;
                }
            }

            if (maxTextWidth > 0 && tooltipTextWidth > maxTextWidth)
            {
                tooltipTextWidth = maxTextWidth;
                needsWrap = true;
            }

            if (needsWrap)
            {
                int wrappedTooltipWidth = 0;
                List<ITextProperties> wrappedTextLines = new ArrayList<>();
                for (int i = 0; i < textLines.size(); i++)
                {
                    ITextProperties textLine = textLines.get(i);
                    List<ITextProperties> wrappedLine = font.func_238420_b_().func_238362_b_(textLine, tooltipTextWidth, Style.field_240709_b_);
                    if (i == 0)
                        titleLinesCount = wrappedLine.size();

                    for (ITextProperties line : wrappedLine)
                    {
                        int lineWidth = font.func_238414_a_(line);
                        if (lineWidth > wrappedTooltipWidth)
                            wrappedTooltipWidth = lineWidth;
                        wrappedTextLines.add(line);
                    }
                }
                tooltipTextWidth = wrappedTooltipWidth;
                textLines = wrappedTextLines;

                if (mouseX > screenWidth / 2)
                    tooltipX = mouseX - 16 - tooltipTextWidth;
                else
                    tooltipX = mouseX + 12;
            }

            int tooltipY = mouseY - 12;
            int tooltipHeight = 8;

            if (textLines.size() > 1)
            {
                tooltipHeight += (textLines.size() - 1) * 10;
                if (textLines.size() > titleLinesCount)
                    tooltipHeight += 2; // gap between title lines and next lines
            }

            tooltipTextWidth += (CACHED_TOOLTIP_ITEM_STACKS.size() / 16) * 23;
            tooltipHeight += (CACHED_TOOLTIP_ITEM_STACKS.size() / 16) * 23;

            if (tooltipY < 4)
                tooltipY = 4;
            else if (tooltipY + tooltipHeight + 4 > screenHeight)
                tooltipY = screenHeight - tooltipHeight - 4;

            final int zLevel = 400;
            RenderTooltipEvent.Color colorEvent = new RenderTooltipEvent.Color(stack, textLines, mStack, tooltipX, tooltipY, font, backgroundColor, borderColorStart, borderColorEnd);
            MinecraftForge.EVENT_BUS.post(colorEvent);
            backgroundColor = colorEvent.getBackground();
            borderColorStart = colorEvent.getBorderStart();
            borderColorEnd = colorEvent.getBorderEnd();

            mStack.push();
            Matrix4f mat = mStack.getLast().getMatrix();
            GuiUtils.drawGradientRect(mat, zLevel, tooltipX - 3, tooltipY - 4, tooltipX + tooltipTextWidth + 3, tooltipY - 3, backgroundColor, backgroundColor);
            GuiUtils.drawGradientRect(mat, zLevel, tooltipX - 3, tooltipY + tooltipHeight + 3, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 4, backgroundColor, backgroundColor);
            GuiUtils.drawGradientRect(mat, zLevel, tooltipX - 3, tooltipY - 3, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 3, backgroundColor, backgroundColor);
            GuiUtils.drawGradientRect(mat, zLevel, tooltipX - 4, tooltipY - 3, tooltipX - 3, tooltipY + tooltipHeight + 3, backgroundColor, backgroundColor);
            GuiUtils.drawGradientRect(mat, zLevel, tooltipX + tooltipTextWidth + 3, tooltipY - 3, tooltipX + tooltipTextWidth + 4, tooltipY + tooltipHeight + 3, backgroundColor, backgroundColor);
            GuiUtils.drawGradientRect(mat, zLevel, tooltipX - 3, tooltipY - 3 + 1, tooltipX - 3 + 1, tooltipY + tooltipHeight + 3 - 1, borderColorStart, borderColorEnd);
            GuiUtils.drawGradientRect(mat, zLevel, tooltipX + tooltipTextWidth + 2, tooltipY - 3 + 1, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 3 - 1, borderColorStart, borderColorEnd);
            GuiUtils.drawGradientRect(mat, zLevel, tooltipX - 3, tooltipY - 3, tooltipX + tooltipTextWidth + 3, tooltipY - 3 + 1, borderColorStart, borderColorStart);
            GuiUtils.drawGradientRect(mat, zLevel, tooltipX - 3, tooltipY + tooltipHeight + 2, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 3, borderColorEnd, borderColorEnd);

            MinecraftForge.EVENT_BUS.post(new RenderTooltipEvent.PostBackground(stack, textLines, mStack, tooltipX, tooltipY, font, tooltipTextWidth, tooltipHeight));

            IRenderTypeBuffer.Impl renderType = IRenderTypeBuffer.getImpl(Tessellator.getInstance().getBuffer());
            mStack.translate(0.0D, 0.0D, zLevel);

            int tooltipTop = tooltipY;
            ITextProperties firstLine = textLines.get(0);
            if (firstLine != null)
                font.func_238416_a_(LanguageMap.getInstance().func_241870_a(firstLine), (float)tooltipX, (float)tooltipY, -1, true, mat, renderType, false, 0, 15728880);

            tooltipY += 10;
            float prevZLevel = Minecraft.getInstance().getItemRenderer().zLevel;

            Minecraft.getInstance().getItemRenderer().zLevel = zLevel + 1;
            for (int i = 0; i < CACHED_TOOLTIP_ITEM_STACKS.size(); i++) {
                ItemStack bundleItem = CACHED_TOOLTIP_ITEM_STACKS.get(i);
                Map.Entry<Integer, Integer> position = CACHED_TOOLTIP_POSITIONS.get(bundleItem);
                Minecraft.getInstance().getItemRenderer().renderItemIntoGUI(bundleItem, tooltipX + (i == 0 ? 0 : (i % 16) * position.getKey()),
                        tooltipY + (i == 0 ? 0 : (i % 4) * position.getValue()));
            }
            tooltipY += 40;
            Minecraft.getInstance().getItemRenderer().zLevel = prevZLevel;

            for (int lineNumber = 1; lineNumber < textLines.size(); ++lineNumber)
            {
                ITextProperties line = textLines.get(lineNumber);
                if (line != null)
                    font.func_238416_a_(LanguageMap.getInstance().func_241870_a(line), (float)tooltipX, (float)tooltipY, -1, true, mat, renderType, false, 0, 15728880);

                if (lineNumber + 1 == titleLinesCount)
                    tooltipY += 2;

                tooltipY += 10;
            }


            renderType.finish();
            mStack.pop();

            MinecraftForge.EVENT_BUS.post(new RenderTooltipEvent.PostText(stack, textLines, mStack, tooltipX, tooltipTop, font, tooltipTextWidth, tooltipHeight));

            RenderSystem.enableDepthTest();
            RenderSystem.enableRescaleNormal();
        }
    }
}
