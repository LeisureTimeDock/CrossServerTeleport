package com.leisuretimedock.crossmod.network;

import com.leisuretimedock.crossmod.CrossTeleportMod;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.concurrent.*;


@Slf4j
public final class PingRequestManager {
    // 配置常量
    private static final long DEFAULT_TIMEOUT_MS = 5000;
    private static final long CLEANUP_INTERVAL_MS = 60000;
    private static final int MAX_PING_HISTORY = 1024;
    @Getter
    private static final int MAX_BATCH_PINGS = 1024; // 单次批量最大Ping数
    @Getter
    private static final long MIN_PING_INTERVAL = 50; // 最小间隔时间(ms)
    @Getter
    private static final long PING_INTERVAL = 1000; // 系统间隔时间(ms)
    // 线程安全的存储结构
    private static final ConcurrentMap<UUID, PlayerPingData> playerData = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    // 添加定时任务调度器
    private static final ScheduledExecutorService pingScheduler = Executors.newSingleThreadScheduledExecutor();
    static {
        // 定期清理过期请求
        scheduler.scheduleAtFixedRate(
                PingRequestManager::cleanupExpiredRequests,
                CLEANUP_INTERVAL_MS, CLEANUP_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
        // Ping任务 (新增)
        pingScheduler.scheduleAtFixedRate(() -> {
            try {
                pingAllMonitoredPlayers();
            } catch (Exception e) {
                log.error("Ping任务执行失败", e);
            }
        }, 1000, PING_INTERVAL, TimeUnit.MILLISECONDS);
    }

    private PingRequestManager() {} // 防止实例化

    // ========== 公共API ==========

    /**
     * 监控玩家并开始收集ping数据
     */
    public static void monitor(ServerPlayer player) {
        Objects.requireNonNull(player, "Player cannot be null");
        playerData.putIfAbsent(player.getUUID(), new PlayerPingData());
    }

    /**
     * 停止监控玩家并清理数据
     */
    public static void unmonitor(ServerPlayer player) {
        Objects.requireNonNull(player, "Player cannot be null");
        playerData.remove(player.getUUID());
    }

    public static boolean isMonitored(UUID uuid) {
        return playerData.containsKey(uuid);
    }

    /**
     * 发起ping请求
     */
    public static void ping(ServerPlayer player) {
        Objects.requireNonNull(player, "Player cannot be null");

        PlayerPingData data = playerData.get(player.getUUID());
        if (data == null) return; // 玩家未被监控

        UUID requestId = UUID.randomUUID();
        long currentTime = System.currentTimeMillis();

        synchronized (data) {
            data.totalRequests++;
            data.activeRequests.put(requestId, currentTime);
        }

        NetworkHandler.sendPingRequest(player, requestId);
    }

    /**
     * 完成ping请求并计算延迟
     */
    public static void complete(ServerPlayer player, UUID requestId) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(requestId, "Request ID cannot be null");

        PlayerPingData data = playerData.get(player.getUUID());
        if (data == null) return;

        synchronized (data) {
            Long startTime = data.activeRequests.remove(requestId);
            if (startTime != null) {
                long ping = System.currentTimeMillis() - startTime;
                data.successfulRequests++;

                // 网络拥塞检测
                if (ping > DEFAULT_TIMEOUT_MS * 0.8) {
                    player.displayClientMessage(Component.translatable("ltd.mod.ping.warn.network_latency",  player.getUUID()),
                           true);
                }

                updatePingHistory(data, ping);
            }
        }
    }

    /**
     * 获取最新ping值
     */
    public static Optional<Long> getLatestPing(UUID playerId) {
        PlayerPingData data = playerData.get(playerId);
        return data != null && !data.pingHistory.isEmpty()
                ? Optional.of(data.pingHistory.getLast())
                : Optional.empty();
    }

    /**
     * 获取ping统计数据（包含丢包率）
     */
    public static PingStats getPingStats(UUID playerId) {
        PlayerPingData data = playerData.get(playerId);
        if (data == null || data.pingHistory.isEmpty()) {
            return PingStats.EMPTY;
        }

        // 计算基本统计数据
        LongSummaryStatistics stats = data.pingHistory.stream()
                .mapToLong(Long::longValue)
                .summaryStatistics();

        // 计算丢包率
        double packetLossRate = calculatePacketLossRate(data);

        return new PingStats(
                stats.getAverage(),
                stats.getMax(),
                stats.getMin(),
                (int) stats.getCount(),
                calculateAverageLatency(data.pingHistory),
                packetLossRate
        );
    }

    /**
     * 获取玩家的平均时延
     */
    public static double getAverageLatency(UUID playerId) {
        PlayerPingData data = playerData.get(playerId);
        return data != null ? calculateAverageLatency(data.pingHistory) : 0;
    }

    /**
     * 获取玩家的丢包率
     */
    public static double getPacketLossRate(UUID playerId) {
        PlayerPingData data = playerData.get(playerId);
        return data != null ? calculatePacketLossRate(data) : 0;
    }

    /**
     * 获取全局Ping统计数据（包含丢包率）
     */
    public static PingStats getGlobalPingStats() {
        List<Long> allPings = new ArrayList<>();
        int totalRequests = 0;
        int successfulRequests = 0;

        for (PlayerPingData data : playerData.values()) {
            synchronized (playerData) {
                allPings.addAll(data.pingHistory);
                totalRequests += data.totalRequests;
                successfulRequests += data.successfulRequests;
            }
        }

        if (allPings.isEmpty()) {
            return PingStats.EMPTY;
        }

        LongSummaryStatistics stats = allPings.stream()
                .mapToLong(Long::longValue)
                .summaryStatistics();

        // 计算全局丢包率
        double globalPacketLossRate = totalRequests > 0
                ? (1 - (double) successfulRequests / totalRequests) * 100
                : 0;

        return new PingStats(
                stats.getAverage(),
                stats.getMax(),
                stats.getMin(),
                (int) stats.getCount(),
                calculateAverageLatency(allPings),
                globalPacketLossRate
        );
    }

    /**
     * 获取所有玩家的最新ping值
     */
    public static Map<UUID, Long> getAllLatestPings() {
        Map<UUID, Long> results = new HashMap<>();

        playerData.forEach((uuid, data) -> {
            synchronized (PingRequestManager.class) {
                if (!data.pingHistory.isEmpty()) {
                    results.put(uuid, data.pingHistory.getLast());
                }
            }
        });

        return results;
    }
    /**
     * 获取指定玩家的最新ping数据
     * @param players 要查询的玩家集合
     * @return 包含玩家UUID和最新ping时间的Map
     */
    public static Map<UUID, Long> getLatestPingsForPlayers(Collection<ServerPlayer> players) {
        Map<UUID, Long> results = new HashMap<>();

        for (ServerPlayer player : players) {
            UUID uuid = player.getUUID();
            PlayerPingData data = playerData.get(uuid);

            if (data != null && !data.pingHistory.isEmpty()) {
                synchronized (data) {
                    results.put(uuid, data.pingHistory.getLast());
                }
            }
        }

        return results;
    }
    /**
     * 批量发送Ping请求
     * @param player 目标玩家
     * @param count 要发送的Ping包数量(1-10)
     * @param intervalMs 发送间隔(毫秒,最少100ms)
     * @return 是否成功开始发送
     */
    public static boolean sendMultiplePings(ServerPlayer player, int count, long intervalMs) {
        // 参数验证
        if (count < 1 || count > MAX_BATCH_PINGS || intervalMs < MIN_PING_INTERVAL) {
            return false;
        }

        PlayerPingData data = playerData.get(player.getUUID());
        if (data == null) return false;

        synchronized (data) {
            // 检查现有请求量
            if (data.activeRequests.size() > MAX_BATCH_PINGS || data.batchInProgress > 0) {
                return false;
            }

            data.batchInProgress = count;
        }

        // 开始发送
        for (int i = 0; i < count; i++) {
            final int attempt = i + 1;
            scheduler.schedule(() -> {
                synchronized (data) {
                    if (data.batchInProgress == 0) return; // 已取消

                    UUID requestId = UUID.randomUUID();
                    data.totalRequests++;
                    data.activeRequests.put(requestId, System.currentTimeMillis());
                    NetworkHandler.sendPingRequest(player, requestId);

                    if (attempt == count) {
                        data.batchInProgress = 0;
                        player.displayClientMessage(
                                Component.translatable("ltd.mod.ping.success.multiping.complete", count),
                                true);
                    }
                }
            }, i * intervalMs, TimeUnit.MILLISECONDS);
        }

        return true;
    }

    /**
     * 取消该玩家的所有进行中的Ping请求
     */
    public static void cancelPings(ServerPlayer player) {
        PlayerPingData data = playerData.get(player.getUUID());
        if (data != null) {
            synchronized (data) {
                data.activeRequests.clear();
                data.batchInProgress = 0;
            }
        }
    }

    // ========== 内部方法 ==========
    // 新增方法：Ping所有被监控玩家
    private static void pingAllMonitoredPlayers() {
        playerData.keySet().forEach(uuid -> {
            ServerPlayer player = CrossTeleportMod.CommonEvents.getPlayerByUUID(uuid);
            if (player != null && player.isAlive()) {
                ping(player);

                // 每10次Ping发送一次报告
                PlayerPingData data = playerData.get(uuid);
                if (data != null && data.totalRequests % 10 == 0) {
                    sendReportToClient(player);
                }
            }
        });
    }

    // 改进的发送报告方法
    private static void sendReportToClient(ServerPlayer player) {
        Map<UUID, Long> latestPings = new HashMap<>();
        Map<UUID, Double> averages = new HashMap<>();

        playerData.forEach((uuid, data) -> {
            synchronized (PingRequestManager.class) {
                if (!data.pingHistory.isEmpty()) {
                    latestPings.put(uuid, PingRequestManager.getLatestPing(uuid).orElse(-1L));
                    averages.put(uuid, PingRequestManager.calculateAverageLatency(data.pingHistory));
                }
            }
        });

        // 添加全局统计
        PingStats globalStats = getGlobalPingStats();
        NetworkHandler.sendPingReport(player, latestPings, averages, globalStats);
    }
    private static void updatePingHistory(PlayerPingData data, long ping) {
        data.addPing(ping);
    }

    private static void cleanupExpiredRequests() {
        long currentTime = System.currentTimeMillis();

        playerData.forEach((playerId, data) -> {
            synchronized (PingRequestManager.class) {
                data.activeRequests.entrySet().removeIf(entry ->
                        currentTime - entry.getValue() > DEFAULT_TIMEOUT_MS
                );
                // 将超时的请求计入丢包统计
                int expiredCount = data.activeRequests.size();
                if (expiredCount > 0) {
                    data.totalRequests += expiredCount;
                    data.activeRequests.clear();
                }
            }
        });
    }


    private static double calculateAverageLatency(Collection<Long> pingHistory) {
        return PlayerPingData.calculate(pingHistory);
    }

    private static double calculatePacketLossRate(PlayerPingData data) {
        synchronized (PingRequestManager.class) {
            if (data.totalRequests == 0) return 0;
            return (1 - (double) data.successfulRequests / data.totalRequests) * 100;
        }
    }

    // ========== 数据结构 ==========

    private static class PlayerPingData {
        final LinkedList<Long> pingHistory = new LinkedList<>();
        final Map<UUID, Long> activeRequests = new ConcurrentHashMap<>();
        volatile double currentLatency = 0;
        int totalRequests = 0;
        int successfulRequests = 0;
        int batchInProgress = 0;

        synchronized void addPing(long ping) {
            pingHistory.add(ping);
            if (pingHistory.size() > MAX_PING_HISTORY) {
                pingHistory.removeFirst();
            }
            currentLatency = calculateAverageLatency(pingHistory);
        }

        private double calculateAverageLatency(Collection<Long> pings) {
            return calculate(pings);
        }

        static double calculate(Collection<Long> pings) {
            if (pings.isEmpty()) return 0;

            double total = 0;
            double weightSum = 0;
            int i = 1;

            for (Long ping : pings) {
                double weight = 1.0 / i;
                total += ping * weight;
                weightSum += weight;
                i++;
            }

            return total / weightSum;
        }
    }

    public record PingStats(
            double average,
            long max,
            long min,
            int sampleCount,
            double averageLatency,
            double packetLossRate
    ) {
        public static final PingStats EMPTY = new PingStats(0, 0, 0, 0, 0, 0);
    }
}