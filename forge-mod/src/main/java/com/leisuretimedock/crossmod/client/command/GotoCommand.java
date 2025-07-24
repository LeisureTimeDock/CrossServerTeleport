package com.leisuretimedock.crossmod.client.command;

import com.leisuretimedock.crossmod.network.NetworkHandler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TranslatableComponent;

public class GotoCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> main = Commands.literal("goto")
                .then(Commands.argument("server", StringArgumentType.string())
                        .executes(ctx -> {
                            String server = StringArgumentType.getString(ctx, "server");
                            NetworkHandler.sendTeleportRequest(server);
                            ctx.getSource().sendSuccess(
                                    new TranslatableComponent("ltd.mod.client.request.goto",server), false);
                            return 1;
        }));
        dispatcher.register(main);
    }
}
