package com.leisuretimedock.crossmod.util;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

public class DebugUtils {
    public static void debugBuffer(FriendlyByteBuf buf) {
        int readable = buf.readableBytes();
        System.out.println("[Debug] Readable bytes: " + readable);

        if (readable <= 0) {
            System.out.println("[Debug] No extra bytes to inspect.");
            return;
        }

        // 保存当前位置
        int index = buf.readerIndex();

        // 读取并打印十六进制
        byte[] bytes = new byte[readable];
        buf.readBytes(bytes);
        String hex = HexFormat.of().formatHex(bytes);
        System.out.println("[Debug] Extra bytes (hex): " + hex);

        // 尝试以 UTF-8 解码（仅用于辅助分析）
        try {
            String utf8 = new String(bytes, StandardCharsets.UTF_8);
            System.out.println("[Debug] Interpreted as UTF-8 string:\n" + utf8);
        } catch (Exception e) {
            System.out.println("[Debug] Failed to interpret as UTF-8: " + e.getMessage());
        }

        // 还原读取位置，避免影响其他逻辑
        buf.readerIndex(index);
    }
    public static void debugFullBuffer(FriendlyByteBuf buf) {
        ByteBuf internal = buf.copy(); // 复制整个缓冲区（包括所有字节）
        int size = internal.readableBytes();
        byte[] data = new byte[size];
        internal.readBytes(data);

        System.out.println("[Debug] Full buffer size: " + size);
        System.out.println("[Debug] Hex dump:\n" + HexFormat.of().formatHex(data));

        try {
            String utf8 = new String(data, StandardCharsets.UTF_8);
            System.out.println("[Debug] UTF-8 decoded:\n" + utf8);
        } catch (Exception e) {
            System.out.println("[Debug] UTF-8 decode failed: " + e.getMessage());
        }

        internal.release(); // 手动释放 copy() 出来的 ByteBuf，防止泄漏
    }
}
