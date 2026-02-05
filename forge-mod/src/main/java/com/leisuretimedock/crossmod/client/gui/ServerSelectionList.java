package com.leisuretimedock.crossmod.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class ServerSelectionList extends ObjectSelectionList<ServerSelectionList.ServerEntry> {
    private final CrossServerGui parentScreen;

    public ServerSelectionList(CrossServerGui parent, Minecraft mc, int width, int height, int y0, int y1,  int itemHeight, Map<String, String> servers) {
        super(mc, width, height, y0, y1, itemHeight);
        this.parentScreen = parent;

        // 添加服务器条目
        if (servers.isEmpty()) {
            this.addEntry(new ServerEntry(Component.translatable("ltd.mod.client.menu.button.no_servers"), null, parentScreen));
        } else {
            servers.forEach((server_name, translate_key) -> {
                this.addEntry(new ServerEntry(Component.translatable(translate_key), server_name, parentScreen));
            });
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            ServerEntry entry = this.getEntryAtPosition(mouseX, mouseY);
            if (entry != null && entry.serverId != null) {
                parentScreen.sendCustomPayload("connect:" + entry.serverId);
                parentScreen.onClose();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    public static class ServerEntry extends ObjectSelectionList.Entry<ServerEntry> {
        private final Component displayName;
        private final String serverId;
        private Button serverButton;

        public ServerEntry(Component displayName, String serverId, CrossServerGui parent) {
            this.displayName = displayName;
            this.serverId = serverId;

            if (serverId != null) {
                this.serverButton = Button.builder(displayName, button -> {
                    parent.sendCustomPayload("connect:" + serverId);
                    parent.onClose();
                }).build();
            }
        }

        @Override
        public void render(@NotNull GuiGraphics guiGraphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean isMouseOver, float partialTick) {

            if (serverButton != null) {
                // 更新按钮位置和大小
                serverButton.setX(left + 5);
                serverButton.setY(top + 2);
                serverButton.setWidth(width - 10);
                serverButton.setHeight(height - 4);

                // 渲染按钮
                serverButton.render(guiGraphics, mouseX, mouseY, partialTick);
            } else {
                // "无服务器"条目
                int textX = left + (width - Minecraft.getInstance().font.width(displayName)) / 2;
                int textY = top + (height - Minecraft.getInstance().font.lineHeight) / 2;
                guiGraphics.drawString(Minecraft.getInstance().font, displayName, textX, textY, 0xFFAAAAAA);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (serverButton != null) {
                return serverButton.mouseClicked(mouseX, mouseY, button);
            }
            return false;
        }

        @Override
        public @NotNull Component getNarration() {
            return displayName;
        }
    }
}
