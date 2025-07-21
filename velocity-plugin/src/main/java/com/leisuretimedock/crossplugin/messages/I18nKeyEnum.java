package com.leisuretimedock.crossplugin.messages;

import lombok.Getter;

@Getter
public enum I18nKeyEnum {
    NO_PERMISSION_TO_TRANS_THIS_SERVER("ltd.plugin.trans.failed.no_permission"),
    NO_PERMISSION_TO_USE_THIS_COMMAND("ltd.plugin.command.no_permission"),
    SERVER_NOT_FOUND("ltd.plugin.trans.failed.server_not_found"),
    ALREADY_ON_SERVER("ltd.plugin.trans.failed.already_on_server"),
    FAILED_TO_SEND_SERVER_LIST("ltd.plugin.send_server_list.failed"),
    FAILED_TO_RELOAD_CONFIG("ltd.plugin.reload.failed.error"),
    RELOAD_CONFIG_SUCCESSFUL("ltd.plugin.reload.successful"),
    COMMAND_HELP("ltd.plugin.help.command"),
    UNKNOWN_COMMAND("ltd.plugin.command.unknown_command"),
    ;

    final String key;
    I18nKeyEnum(String key) {
       this.key = key;
   }
}
