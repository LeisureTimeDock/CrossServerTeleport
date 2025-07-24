package com.leisuretimedock.crossmod.client.overlay;

import java.util.Arrays;
import java.util.Optional;

public enum PluginCommand {
    OVERLAY_SHOW("overlay:show"),
    OVERLAY_HIDE("overlay:hide");

    public final String id;

    PluginCommand(String id) { this.id = id; }

    public static Optional<PluginCommand> fromId(String id) {
        return Arrays.stream(values()).filter(cmd -> cmd.id.equals(id)).findFirst();
    }
}
