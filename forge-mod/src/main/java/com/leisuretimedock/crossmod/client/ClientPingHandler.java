package com.leisuretimedock.crossmod.client;

import com.leisuretimedock.crossmod.network.PingRequestManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClientPingHandler {
    private static final long DATA_EXPIRE_TIME = 10000; // 10秒数据有效期
    private static final Map<UUID, PingData> pingData = new ConcurrentHashMap<>();
    private static PingRequestManager.PingStats lastStats;
    private static long lastStatsUpdateTime;

    public static void handlePingResults(Map<UUID, Long> pingResults, Map<UUID, Double> averages) {
        long now = System.currentTimeMillis();

        pingResults.forEach((uuid, ping) -> {
            PingData data = pingData.computeIfAbsent(uuid, k -> new PingData());
            data.update(ping, averages.getOrDefault(uuid, 0.0), now);
        });

        // 清理过期的数据(5秒未更新)
        long currentTime = System.currentTimeMillis();
        pingData.entrySet().removeIf(entry ->
                currentTime - entry.getValue().lastUpdateTime > 5000
        );
    }

    public static String getPingDisplayText() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return "";

        StringBuilder sb = new StringBuilder();

        // 显示自己的ping信息
        Player localPlayer = mc.player;
        if (localPlayer != null) {
            PingData selfData = pingData.get(localPlayer.getUUID());
            if (selfData != null) {
                sb.append("Ping: ").append(selfData.currentPing).append("ms");
                sb.append(" (Avg: ").append(String.format("%.1f", selfData.averageLatency)).append("ms)");
            }
        }

        // 显示其他玩家的ping信息(最多3个)
        int count = 0;
        for (Map.Entry<UUID, PingData> entry : pingData.entrySet()) {
            if (count >= 3) break;
            if (localPlayer != null && entry.getKey().equals(localPlayer.getUUID())) continue;

            Player player = mc.level.getPlayerByUUID(entry.getKey());
            if (player != null) {
                sb.append("\n");
                sb.append(player.getScoreboardName())
                        .append(": ").append(entry.getValue().currentPing).append("ms");
                count++;
            }
        }

        return sb.toString();
    }

    private static class PingData {
        long currentPing;
        double averageLatency;
        long lastUpdateTime;
        void update(long ping, double avgLatency, long timestamp) {
            this.currentPing = ping;
            this.averageLatency = avgLatency;
            this.lastUpdateTime = timestamp;
        }
    }


    // 更新统计数据处理方法
    public static void handlePingStats(PingRequestManager.PingStats stats) {
        lastStats = stats;
        lastStatsUpdateTime = System.currentTimeMillis();
    }

    // 获取要显示的统计文本
    public static String getStatsDisplayText() {
        if (lastStats == null || System.currentTimeMillis() - lastStatsUpdateTime > 10000) {
            return "网络统计: 数据过期";
        }

        return String.format("""
            网络延迟统计:
            平均: %.1fms | 最高: %dms
            最低: %dms | 时延: %.1fms
            丢包率: %.1f%% | 样本: %d
            """,
                lastStats.average(),
                lastStats.max(),
                lastStats.min(),
                lastStats.averageLatency(),
                lastStats.packetLossRate(),
                lastStats.sampleCount()
        );
    }

    // 合并Ping数据和统计数据的显示方法
    public static List<String> getCombinedDebugText() {
        List<String> lines = new ArrayList<>();

        // 添加Ping数据
        String pingText = getPingDisplayText();
        if (!pingText.isEmpty()) {
            lines.addAll(Arrays.asList(pingText.split("\n")));
        }

        // 添加统计信息
        String statsText = getStatsDisplayText();
        if (!statsText.isEmpty()) {
            lines.add(""); // 空行分隔
            lines.addAll(Arrays.asList(statsText.split("\n")));
        }

        return lines;
    }

    private static String resolvePlayerName(UUID uuid) {
        // 实现从UUID到玩家名的解析
        if (Minecraft.getInstance().level != null) {
            Player player = Minecraft.getInstance().level.getPlayerByUUID(uuid);
            if (player != null) {
                return player.getScoreboardName();
            }
        }
        return uuid.toString().substring(0, 8);
    }
    private static void cleanUpOldData(long currentTime) {
        pingData.entrySet().removeIf(entry ->
                currentTime - entry.getValue().lastUpdateTime > DATA_EXPIRE_TIME
        );
    }

}