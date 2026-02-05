package com.leisuretimedock.crossmod.client.overlay;

import com.leisuretimedock.crossmod.client.ClientPingHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.ArrayList;
import java.util.List;

public class PingOverlayManager implements IGuiOverlay {
    public static final PingOverlayManager INSTANCE = new PingOverlayManager();
    private static boolean showOverlay = false;
    private static final Minecraft mc = Minecraft.getInstance();
    public static boolean isShowOverlay() {
        return  !showOverlay || mc.player == null || mc.level == null;
    }
    // 配置参数
    private static final int MARGIN = 5;
    private static final int PADDING = 2;
    private static final int BACKGROUND_COLOR = 0x80000000;
    private static final int TEXT_COLOR = 0xFFFFFF;

    public static void setShow(boolean show) {
        showOverlay = show;
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int width, int height) {
        if (!showOverlay || mc.player == null || mc.level == null) {
            return;
        }

        // 获取所有要显示的内容
        List<String> allLines = getAllDisplayLines();
        if (allLines.isEmpty()) {
            return;
        }

        // 计算渲染位置
        Font font = mc.font;
        int maxWidth = getMaxLineWidth(font, allLines);
        int totalHeight = allLines.size() * font.lineHeight;

        // 自动调整位置以避免与其他调试信息重叠
        int x = width - MARGIN - maxWidth;
        int y = findSuitableYPosition(height, totalHeight);

        // 绘制背景
        drawBackground(guiGraphics, x, y, maxWidth, totalHeight, font);

        // 绘制文本
        drawTextLines(guiGraphics, font, allLines, x, y);
    }

    private List<String> getAllDisplayLines() {
        List<String> lines = new ArrayList<>();

        // 添加Ping信息
        String pingText = ClientPingHandler.getPingDisplayText();
        if (!pingText.isEmpty()) {
            lines.addAll(List.of(pingText.split("\n")));
        }

        // 添加统计信息
        String statsText = ClientPingHandler.getStatsDisplayText();
        if (!statsText.isEmpty()) {
            if (!lines.isEmpty()) lines.add(""); // 添加分隔空行
            lines.addAll(List.of(statsText.split("\n")));
        }

        return lines;
    }

    private int getMaxLineWidth(Font font, List<String> lines) {
        return lines.stream()
                .mapToInt(font::width)
                .max()
                .orElse(0);
    }

    private int findSuitableYPosition(int screenHeight, int totalHeight) {
        // 基础位置（从顶部开始）
        int baseY = 10;

        // 检查是否会与其他调试信息重叠
        // 这里可以根据需要添加更复杂的检测逻辑
        if (baseY + totalHeight > screenHeight / 2) {
            // 如果会重叠，则移动到屏幕下半部分
            return screenHeight - totalHeight - 30;
        }
        return baseY;
    }

    private void drawBackground(GuiGraphics guiGraphics, int x, int y, int width, int height, Font font) {
        guiGraphics.fill(
                x - PADDING, y - PADDING,
                x + width + PADDING, y + height + PADDING,
                BACKGROUND_COLOR);
    }

    private void drawTextLines(GuiGraphics guiGraphics, Font font, List<String> lines, int x, int y) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (!line.isEmpty()) {
                guiGraphics.drawString(font, line,
                        x,
                        y + i * font.lineHeight,
                        TEXT_COLOR);
            }
        }
    }
}