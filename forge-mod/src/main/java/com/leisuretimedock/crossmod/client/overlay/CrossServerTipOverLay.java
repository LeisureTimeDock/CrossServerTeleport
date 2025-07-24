package com.leisuretimedock.crossmod.client.overlay;

import com.leisuretimedock.crossmod.client.KeyBindingHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.client.gui.IIngameOverlay;

public class CrossServerTipOverLay implements IIngameOverlay {
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
    public void render(ForgeIngameGui forgeIngameGui, PoseStack poseStack, float v, int i, int i1) {
        if ( !showOverlay || mc.player == null || mc.level == null) return;
        int x = 10;
        int y = 10;
        Font font = mc.font;
        ItemRenderer itemRenderer = mc.getItemRenderer();

        // 1. 原版钟物品
        ItemStack clockStack = new ItemStack(Items.CLOCK);

        // 2. 渲染钟图标（含动画帧）
        itemRenderer.renderAndDecorateItem(clockStack, x, y);
        itemRenderer.renderGuiItemDecorations(mc.font, clockStack, x, y);

        // 3. 绘制提示文字
        String keyText = KeyBindingHandler.OPEN_GUI_KEY.getTranslatedKeyMessage().getString(); // 可动态从 KeyMapping 获取
        String text = "按 [" + keyText.toUpperCase() + "] 打开跨服传送菜单";
        GuiComponent.drawString(poseStack, font, text, x + 20, y + 6, 0xFFFFFF);

    }
}
