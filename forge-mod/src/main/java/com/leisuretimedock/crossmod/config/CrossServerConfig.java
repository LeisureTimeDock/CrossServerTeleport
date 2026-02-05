package com.leisuretimedock.crossmod.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class CrossServerConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SERVER_LIST;
    static {
        BUILDER.comment("Cross Server Config").push("servers");
        SERVER_LIST = BUILDER
                .comment("Server list in format: <server_name>: <translate_key>")
                .defineList("serverList",
                        Arrays.asList(
                                "lobby: ltd.mod.client.menu.button.1",
                                "survival: ltd.mod.client.menu.button.2"
                        ),
                        obj -> obj instanceof String str && checkSyntax(str)
                );

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
    public static boolean checkSyntax(@NotNull String input) {
        return CrossServerConfigManager.SYNTAX.matcher(input).matches();
    }
}
