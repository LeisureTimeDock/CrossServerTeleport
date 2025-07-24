package com.leisuretimedock.crossmod.client;

import com.leisuretimedock.crossmod.CrossTeleportMod;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class CrossServerGui extends Screen {

    private static final ResourceLocation CHANNEL_ID = new ResourceLocation(CrossTeleportMod.MOD_ID, "teleport");
    private static final ResourceLocation LOGO_TEXTURE = new ResourceLocation(CrossTeleportMod.MOD_ID, "textures/ltd_logo.png");

    public CrossServerGui() {
        super(new TextComponent("跨服菜单"));
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int centerY = height / 2;
        int buttonWidth = 150;
        int buttonHeight = 20;
        int spacing = 5;

        addRenderableWidget(new Button(centerX - buttonWidth / 2, centerY - buttonHeight - spacing,
                buttonWidth, buttonHeight, new TextComponent("🏰 主城"), btn -> {
            sendCustomPayload("connect:lobby");
            onClose();
        }));

        addRenderableWidget(new Button(centerX - buttonWidth / 2, centerY,
                buttonWidth, buttonHeight, new TextComponent("🌲 生存服"), btn -> {
            sendCustomPayload("connect:survival");
            onClose();
        }));
        // 添加 Checkbox 控件
        Checkbox overlayCheckbox = new Checkbox(centerX - buttonWidth / 2, centerY + buttonHeight + spacing + 5,
                150, 20, new TextComponent("显示传送提示"), !OverlayRenderer.isShowOverlay()) {
            @Override
            public void onPress() {
                super.onPress();
                OverlayRenderer.setShow(this.selected());
            }
        };
        addRenderableWidget(overlayCheckbox);
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
