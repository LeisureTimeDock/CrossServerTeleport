package com.leisuretimedock.crossmod.client.overlay;

import com.leisuretimedock.crossmod.client.KeyBindingHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.awt.*;

public class CrossServerTipOverLay implements IGuiOverlay {
    public static final CrossServerTipOverLay INSTANCE = new CrossServerTipOverLay();
    private static boolean showOverlay = false;
    private static final Minecraft mc = Minecraft.getInstance();
    public static boolean isShowOverlay() {
        return  !showOverlay || mc.player == null || mc.level == null;
    }
    public static void setShow(boolean show) {
        showOverlay = show;
    }
    @Override
    public void render(ForgeGui forgeGui, GuiGraphics guiGraphics, float v, int i, int i1) {
        if ( !showOverlay || mc.player == null || mc.level == null) return;
        int x = 10;
        int y = 10;
        Font font = mc.font;
        ItemRenderer itemRenderer = mc.getItemRenderer();

        // 1. 原版钟物品
        ItemStack clockStack = new ItemStack(Items.CLOCK);
        PoseStack poseStack = new PoseStack();
        poseStack.translate(10, 10, 10);
        // 2. 渲染钟图标
        guiGraphics.renderItem(clockStack, x, y);
        guiGraphics.renderItemDecorations(font,clockStack, x, y);
        // 3. 绘制提示文字
        String text = Component.translatable("ltd.mod.client.overlay.tip", KeyBindingHandler.OPEN_GUI_KEY.getTranslatedKeyMessage()).getString();
        guiGraphics.drawString(font, text, x + 20, y + 6, 0xFFFFFF);

    }
}
