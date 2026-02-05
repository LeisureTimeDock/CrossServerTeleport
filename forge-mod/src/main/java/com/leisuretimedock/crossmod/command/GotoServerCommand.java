package com.leisuretimedock.crossmod.command;

import com.leisuretimedock.crossmod.network.NetworkHandler;
import com.leisuretimedock.crossmod.network.toClient.GotoServerPayload;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Collection;

public class GotoServerCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> main = Commands.literal("server")
                .requires(cs -> cs.hasPermission(2))
                .then(Commands.argument("players", EntityArgument.players())
                    .then(Commands.literal("goto")
                        .then(Commands.argument("server", StringArgumentType.string())
                            .executes(ctx -> {
                                String server = StringArgumentType.getString(ctx, "server");
                                Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "players");
                                players.forEach(p -> NetworkHandler.sendToPlayer(new GotoServerPayload(server), p));
                                ctx.getSource().sendSuccess(
                                        () -> Component.translatable("ltd.mod.client.request.goto",server), false);
                                return 1;
                            })
                        )
                    )
                )
                .then(Commands.literal("goto")
                        .then(Commands.argument("server", StringArgumentType.string())
                                .executes(ctx -> {
                                    CommandSourceStack source = ctx.getSource();
                                    ServerPlayer player = source.getPlayer();
                                    if (player != null) {
                                        String server = StringArgumentType.getString(ctx, "server");
                                        NetworkHandler.sendToPlayer(new GotoServerPayload(server), player);
                                        source.sendSuccess(
                                                () -> Component.translatable("ltd.mod.client.request.goto",server), false);
                                    }
                                    source.sendFailure(Component.literal("Request a player"));
                                    return 1;
                                })
                        )
                );
        dispatcher.register(main);
    }
}
