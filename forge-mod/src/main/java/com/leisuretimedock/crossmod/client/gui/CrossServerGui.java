package com.leisuretimedock.crossmod.client.gui;

import com.leisuretimedock.crossmod.CrossTeleportMod;
import com.leisuretimedock.crossmod.config.CrossServerConfigManager;
import com.leisuretimedock.crossmod.client.overlay.CrossServerTipOverLay;
import com.leisuretimedock.crossmod.client.overlay.PingOverlayManager;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class CrossServerGui extends Screen {
    public final static Component TITLE = Component.translatable("ltd.mod.client.menu");
    private static final ResourceLocation CHANNEL_ID = new ResourceLocation(CrossTeleportMod.MOD_ID, "teleport");
    private static final ResourceLocation LOGO_TEXTURE = new ResourceLocation(CrossTeleportMod.MOD_ID, "textures/ltd_logo.png");

    // 存储组件引用，以便在渲染时控制顺序
    private Checkbox enableCrCheckBox;
    private Checkbox enablePiCheckBox;
    private Button closeButton;
    private ServerSelectionList serverList;

    public CrossServerGui() {
        super(TITLE);
    }

    @Override
    protected void init() {
        // 先添加按钮和复选框（它们应该渲染在列表上方）
        initButtons();

        // 再添加列表（它应该渲染在下方）
        initServerList();
    }

    private void initButtons() {
        int centerX = width / 2;
        int bottomY = height - 60; // 从底部向上60像素

        // 添加 Checkbox 控件 - 显示传送提示
        enableCrCheckBox = new Checkbox(centerX - 150, bottomY,
                140, 20, Component.translatable("ltd.mod.client.menu.checkbox.show_trans_tip"),
                !CrossServerTipOverLay.isShowOverlay()) {
            @Override
            public void onPress() {
                super.onPress();
                CrossServerTipOverLay.setShow(this.selected());
            }
        };
        addRenderableWidget(enableCrCheckBox);

        // 添加 Checkbox 控件 - 显示ping统计
        enablePiCheckBox = new Checkbox(centerX + 10, bottomY,
                140, 20, Component.translatable("ltd.mod.client.menu.checkbox.show_ping_stat"),
                !PingOverlayManager.isShowOverlay()) {
            @Override
            public void onPress() {
                super.onPress();
                PingOverlayManager.setShow(this.selected());
            }
        };
        addRenderableWidget(enablePiCheckBox);

        // 添加关闭按钮
        closeButton = Button.builder(
                        Component.translatable("gui.done"),
                        button -> this.onClose()
                )
                .bounds(centerX - 50, bottomY + 30, 100, 20)
                .build();
        addRenderableWidget(closeButton);
    }

    private void initServerList() {
        int screenWidth = this.width;
        int screenHeight = this.height;


        // 创建服务器列表，但尺寸要避开按钮区域
        serverList = new ServerSelectionList(
                this,
                Minecraft.getInstance(),
                screenWidth,
                screenHeight,
                48, // X位置
                height - 64, // Y位置
                36, // 条目高度
               CrossServerConfigManager.INSTANCE.getServers()
        );

        // 设置列表属性
        serverList.setRenderBackground(true);
        serverList.setRenderTopAndBottom(true);


        addRenderableWidget(serverList);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {

        // 渲染标题
        guiGraphics.drawString(this.font, this.title.getString(), this.width / 2 - font.width(this.title.getString()) / 2, 20, 0xFFFFFF);

        // 重要：先渲染列表（在底层）
        if (serverList != null) {
            serverList.render(guiGraphics, mouseX, mouseY, partialTicks);
        }

        // 然后渲染按钮和复选框（在上层）
        // 手动调用按钮的render方法，确保它们在最上面
        if (enableCrCheckBox != null) {
            enableCrCheckBox.render(guiGraphics, mouseX, mouseY, partialTicks);
        }
        if (enablePiCheckBox != null) {
            enablePiCheckBox.render(guiGraphics, mouseX, mouseY, partialTicks);
        }
        if (closeButton != null) {
            closeButton.render(guiGraphics, mouseX, mouseY, partialTicks);
        }
        renderLogo(guiGraphics);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 先检查按钮点击
        if (enableCrCheckBox != null && enableCrCheckBox.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (enablePiCheckBox != null && enablePiCheckBox.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (closeButton != null && closeButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        // 再检查列表点击
        if (serverList != null && serverList.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    void sendCustomPayload(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer(256));
            buf.writeUtf(message);
            mc.getConnection().send(new ServerboundCustomPayloadPacket(CHANNEL_ID, buf));
        }
    }

    private void renderLogo(GuiGraphics guiGraphics) {
        int logoWidth = 64; // 缩小Logo，为列表腾出空间
        int logoHeight = 64;

        int x = (this.width - logoWidth - font.width(this.title.getString()) * 2) / 2;
        int y = -5; // 更靠近顶部
        guiGraphics.blit(LOGO_TEXTURE, x, y, 0, 0, logoWidth, logoHeight, logoWidth, logoHeight);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}