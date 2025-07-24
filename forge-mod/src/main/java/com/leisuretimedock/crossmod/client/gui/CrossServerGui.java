package com.leisuretimedock.crossmod.client.gui;

import com.leisuretimedock.crossmod.CrossTeleportMod;
import com.leisuretimedock.crossmod.client.overlay.CrossServerTipOverLay;
import com.leisuretimedock.crossmod.client.overlay.PingOverlayManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class CrossServerGui extends Screen {

    private static final ResourceLocation CHANNEL_ID = new ResourceLocation(CrossTeleportMod.MOD_ID, "teleport");
    private static final ResourceLocation LOGO_TEXTURE = new ResourceLocation(CrossTeleportMod.MOD_ID, "textures/ltd_logo.png");

    public CrossServerGui() {
        super(new TranslatableComponent("ltd.mod.client.menu"));
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int centerY = height / 2;
        int buttonWidth = 150;
        int buttonHeight = 20;
        int spacing = 5;

        addRenderableWidget(new Button(centerX - buttonWidth / 2, centerY - buttonHeight - spacing,
                buttonWidth, buttonHeight, new TranslatableComponent("ltd.mod.client.menu.button.1"), btn -> {
            sendCustomPayload("connect:lobby");
            onClose();
        }));

        addRenderableWidget(new Button(centerX - buttonWidth / 2, centerY,
                buttonWidth, buttonHeight,  new TranslatableComponent("ltd.mod.client.menu.button.2"), btn -> {
            sendCustomPayload("connect:survival");
            onClose();
        }));
        // 添加 Checkbox 控件
        Checkbox enableCrCheckBox = new Checkbox(centerX - buttonWidth / 2, centerY + buttonHeight + spacing + 5,
                150, 20, new TranslatableComponent("ltd.mod.client.menu.checkbox.show_trans_tip"), !CrossServerTipOverLay.isShowOverlay()) {
            @Override
            public void onPress() {
                super.onPress();
                CrossServerTipOverLay.setShow(this.selected());
            }
        };
        addRenderableWidget(enableCrCheckBox);
        // 添加 Checkbox 控件
        Checkbox enablePiCheckBox = new Checkbox(centerX - buttonWidth / 2, centerY + buttonHeight + spacing + 25,
                150, 20, new TranslatableComponent("ltd.mod.client.menu.checkbox.show_ping_stat"), !PingOverlayManager.isShowOverlay()) {
            @Override
            public void onPress() {
                super.onPress();
                PingOverlayManager.setShow(this.selected());
            }
        };
        addRenderableWidget(enablePiCheckBox);
    }

    private void sendCustomPayload(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer(256));
            buf.writeUtf(message);
            mc.getConnection().send(new ServerboundCustomPayloadPacket(CHANNEL_ID, buf));
        }
    }

    @Override
    public void render(@NotNull PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        // 背景
        this.renderBackground(poseStack);

        // Logo 渲染（缩放绘制）
        renderLogo(poseStack);

        // 渲染标题文字
        drawCenteredString(poseStack, this.font, this.title.getString(), this.width / 2 + 5, 10, 0xFFFFFF);

        // 渲染按钮等组件
        super.render(poseStack, mouseX, mouseY, partialTicks);
    }

    private void renderLogo(PoseStack poseStack) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, LOGO_TEXTURE);
        RenderSystem.enableDepthTest();


        int logoWidth = 100;  // 你可以改成 150、200 等
        int logoHeight = 100; // 保持比例缩放

        int x = (this.width - logoWidth) / 2;
        int y = 15;

        blit(poseStack, x, y, 0, 0, logoWidth, logoHeight, logoWidth, logoHeight);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
