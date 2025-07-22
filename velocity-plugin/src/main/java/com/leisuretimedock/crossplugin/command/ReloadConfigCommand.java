package com.leisuretimedock.crossplugin.command;

import com.leisuretimedock.crossplugin.manager.ConfigManager;
import com.leisuretimedock.crossplugin.messages.I18n;
import com.leisuretimedock.crossplugin.messages.I18nKeyEnum;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class ReloadConfigCommand implements SimpleCommand {

    private final ConfigManager configManager;
    public static final String PERMISSION_RELOAD = "ltdcrossserver.reload";
    public static final String PERMISSION_HELP = "ltdcrossserver.help";
    public ReloadConfigCommand(ConfigManager configManager) {
        this.configManager = configManager;
    }
    public static List<String> SUGGESTIONS = List.of("reload", "help");

    @Override
    public void execute(SimpleCommand.Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();
        // ltdcrossserver
        if (args.length == 0) {
            source.sendMessage(I18n.translatable(I18nKeyEnum.COMMAND_HELP, NamedTextColor.YELLOW));
            return;
        }


        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "reload" -> handleReload(source);
            case "help" -> source.sendMessage(I18n.translatable(I18nKeyEnum.COMMAND_HELP, NamedTextColor.YELLOW));
            default -> source.sendMessage(I18n.translatable(I18nKeyEnum.UNKNOWN_COMMAND, NamedTextColor.YELLOW, Component.text(subCommand)));
        }



    }
    private void handleReload(CommandSource source) {
        // 控制台允许，玩家检查权限
        if (source instanceof Player player && !player.hasPermission(PERMISSION_RELOAD)) {
            source.sendMessage(I18n.translatable(I18nKeyEnum.NO_PERMISSION_TO_USE_THIS_COMMAND, NamedTextColor.RED, Component.text(PERMISSION_RELOAD)));
            return;
        }

        try {
            configManager.reload();
            source.sendMessage(I18n.translatable(I18nKeyEnum.RELOAD_CONFIG_SUCCESSFUL, NamedTextColor.GREEN));
        } catch (Exception e) {
            source.sendMessage(I18n.translatable(I18nKeyEnum.FAILED_TO_RELOAD_CONFIG, NamedTextColor.RED));
            log.error("Failed to reload config", e);
        }
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        return CompletableFuture.completedFuture(SUGGESTIONS);
    }

}

