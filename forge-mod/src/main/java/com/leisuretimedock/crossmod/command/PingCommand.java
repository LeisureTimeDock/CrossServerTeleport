package com.leisuretimedock.crossmod.command;

import com.leisuretimedock.crossmod.network.NetworkHandler;
import com.leisuretimedock.crossmod.network.PingRequestManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public class PingCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> networkping =
                Commands.literal("netping")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> executeFullReport(context.getSource()))

                        .then(Commands.literal("report")
                                .executes(context -> executeFullReport(context.getSource()))
                                .then(Commands.argument("players", EntityArgument.players())
                                        .executes(context -> executePlayerReport(
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "players")
                                        )))
                        )
                        .then(Commands.literal("stats")
                                .executes(context -> executeStatsReport(context.getSource())))

                        .then(Commands.literal("ping")
                                .then(Commands.argument("players", EntityArgument.players())
                                        .executes(context -> executePingPlayers(
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "players")
                                        ))
                                )
                                .executes(context -> executeSinglePing(context.getSource())))

                        .then(Commands.literal("monitor")
                                .then(Commands.argument("players", EntityArgument.players())
                                        .executes(context -> executeToggleMonitoring(
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "players"),
                                                true
                                        ))
                                )
                                .executes(context -> executeToggleMonitoring(context.getSource(), true)))

                        .then(Commands.literal("unmonitor")
                                .then(Commands.argument("players", EntityArgument.players())
                                        .executes(context -> executeToggleMonitoring(
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "players"),
                                                false
                                        ))
                                )
                                .executes(context -> executeToggleMonitoring(context.getSource(), false)))

                        .then(Commands.literal("multiping")
                                .then(Commands.argument("players", EntityArgument.players())
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, PingRequestManager.getMAX_BATCH_PINGS()))
                                                .then(Commands.argument("interval", IntegerArgumentType.integer((int) PingRequestManager.getMIN_PING_INTERVAL(), 5000))
                                                        .executes(ctx -> executeMultiPing(
                                                                ctx.getSource(),
                                                                EntityArgument.getPlayers(ctx, "players"),
                                                                IntegerArgumentType.getInteger(ctx, "count"),
                                                                IntegerArgumentType.getInteger(ctx, "interval")
                                                        ))
                                                )
                                        )
                                        .executes(ctx -> executeMultiPing(
                                                ctx.getSource(),
                                                Collections.singleton(ctx.getSource().getPlayerOrException()),
                                                5, // 默认次数
                                                1000 // 默认间隔(ms)
                                        ))
                                )
                        );

        dispatcher.register(networkping);
    }

    private static int executePlayerReport(CommandSourceStack source, Collection<ServerPlayer> players) throws CommandSyntaxException {
        if (players.isEmpty()) {
            source.sendSuccess(new TranslatableComponent("ltd.mod.ping.error.no_players"), false);
            return 0;
        }

        ServerPlayer requester = source.getPlayerOrException();
        Map<UUID, Long> results = PingRequestManager.getLatestPingsForPlayers(players);

        if (results.isEmpty()) {
            source.sendSuccess(new TranslatableComponent("ltd.mod.ping.info.no_data"), false);
            return Command.SINGLE_SUCCESS;
        }

        NetworkHandler.sendPingResults(requester, results);
        sendTextReport(requester, results);

        return Command.SINGLE_SUCCESS;
    }

    private static int executeFullReport(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Map<UUID, Long> results = PingRequestManager.getAllLatestPings();

        if (results.isEmpty()) {
            source.sendSuccess(new TranslatableComponent("ltd.mod.ping.info.no_data"), false);
            return Command.SINGLE_SUCCESS;
        }

        NetworkHandler.sendPingResults(player, results);
        sendTextReport(player, results);

        // 发送统计信息
        PingRequestManager.PingStats stats = PingRequestManager.getGlobalPingStats();
        NetworkHandler.sendPingStats(player, stats);

        return Command.SINGLE_SUCCESS;
    }

    private static int executeStatsReport(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PingRequestManager.PingStats stats = PingRequestManager.getGlobalPingStats();

        if (stats.sampleCount() == 0) {
            source.sendSuccess(new TranslatableComponent("ltd.mod.ping.info.no_data"), false);
            return Command.SINGLE_SUCCESS;
        }

        NetworkHandler.sendPingStats(player, stats);
        sendStatsTextReport(player, stats);

        return Command.SINGLE_SUCCESS;
    }

    private static int executeSinglePing(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if(!PingRequestManager.isMonitored(player.getUUID())) {
            source.sendFailure(new TranslatableComponent("ltd.mod.ping.error.not_monitored.self"));
            return -1;
        }
        PingRequestManager.ping(player);
        source.sendSuccess(new TranslatableComponent("ltd.mod.ping.success.ping_self"), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int executePingPlayers(CommandSourceStack source, Collection<ServerPlayer> players) throws CommandSyntaxException {
        if (players.isEmpty()) {
            source.sendSuccess(new TranslatableComponent("ltd.mod.ping.error.no_players"), false);
            return 0;
        }

        players.forEach(player -> {
            if(!PingRequestManager.isMonitored(player.getUUID())) {
                source.sendFailure(new TranslatableComponent("ltd.mod.ping.error.not_monitored.other",
                        player.getScoreboardName()));
            }
            else {
                PingRequestManager.ping(player);
                source.sendSuccess(new TranslatableComponent("ltd.mod.ping.success.ping_other",
                        player.getScoreboardName()), false);
            }
        });

        return Command.SINGLE_SUCCESS;
    }
    private static int executeMultiPing(CommandSourceStack source,
                                        Collection<ServerPlayer> players,
                                        int count,
                                        int interval) {
        if (players.isEmpty()) {
            source.sendSuccess(new TranslatableComponent("ltd.mod.ping.error.no_players"), false);
            return 0;
        }

        players.forEach(player -> {
            if (PingRequestManager.sendMultiplePings(player, count, interval)) {
                source.sendSuccess(
                        player.getScoreboardName().equals(source.getTextName()) ?
                        new TranslatableComponent("ltd.mod.ping.success.multiping.start.self", count, interval) :
                        new TranslatableComponent("ltd.mod.ping.success.multiping.start.other", player.getScoreboardName(), count, interval),
                false);
            } else {
                source.sendFailure(
                        new TranslatableComponent(
                                player.getScoreboardName().equals(source.getTextName()) ?
                                        "ltd.mod.ping.error.multiping.fail.self" :
                                        "ltd.mod.ping.error.multiping.fail.other",
                                player.getScoreboardName()
                        )
                );
            }
        });

        return Command.SINGLE_SUCCESS;
    }
    private static int executeToggleMonitoring(CommandSourceStack source, boolean monitor) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (monitor) {
            PingRequestManager.monitor(player);
            source.sendSuccess(new TranslatableComponent("ltd.mod.ping.success.monitor.self"), false);
        } else {
            PingRequestManager.unmonitor(player);
            source.sendSuccess(new TranslatableComponent("ltd.mod.ping.success.unmonitor.self"), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int executeToggleMonitoring(CommandSourceStack source, Collection<ServerPlayer> players, boolean monitor) throws CommandSyntaxException {
        if (players.isEmpty()) {
            source.sendSuccess(new TranslatableComponent("ltd.mod.ping.error.no_players"), false);
            return 0;
        }

        players.forEach(player -> {
            if (monitor) {
                PingRequestManager.monitor(player);
                source.sendFailure(new TranslatableComponent("ltd.mod.ping.error.not_monitored.other",
                        player.getScoreboardName()));
            } else {
                PingRequestManager.unmonitor(player);
                source.sendSuccess(new TranslatableComponent("ltd.mod.ping.success.ping_other",
                        player.getScoreboardName()), false);
            }
        });

        return players.size();
    }

    private static void sendTextReport(ServerPlayer player, Map<UUID, Long> results) {
        player.sendMessage(new TranslatableComponent("ltd.mod.ping.title.report").withStyle(ChatFormatting.GOLD),
                player.getUUID());

        results.forEach((uuid, ping) -> {
            player.sendMessage(
                    new TranslatableComponent(
                        "ltd.mod.ping.report.entry",
                            uuid.toString().substring(0, 8),
                            ping,
                            PingRequestManager.getAverageLatency(uuid),
                            PingRequestManager.getPacketLossRate(uuid)),
                            player.getUUID()
            );
        });
    }

    private static void sendStatsTextReport(ServerPlayer player, PingRequestManager.PingStats stats) {
        player.sendMessage(new TranslatableComponent("ltd.mod.ping.title.stats").withStyle(ChatFormatting.GOLD),
                player.getUUID());
        player.sendMessage(new TranslatableComponent(
                "ltd.mod.ping.stats.average", stats.average()), player.getUUID());
        player.sendMessage(new TranslatableComponent(
                "ltd.mod.ping.stats.max", stats.max()), player.getUUID());
        player.sendMessage(new TranslatableComponent(
                "ltd.mod.ping.stats.min", stats.max()), player.getUUID());
        player.sendMessage(new TranslatableComponent(
                "ltd.mod.ping.stats.avg_latency", stats.averageLatency()), player.getUUID());
        player.sendMessage(new TranslatableComponent(
                "ltd.mod.ping.stats.packet_loss", stats.packetLossRate()), player.getUUID());
        player.sendMessage(new TranslatableComponent(
                "ltd.mod.ping.stats.sample_count", stats.sampleCount()), player.getUUID());

    }
}