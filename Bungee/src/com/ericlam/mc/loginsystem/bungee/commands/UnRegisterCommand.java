package com.ericlam.mc.loginsystem.bungee.commands;

import com.ericlam.mc.bungee.hnmc.builders.MessageBuilder;
import com.ericlam.mc.bungee.hnmc.config.ConfigManager;
import com.ericlam.mc.loginsystem.bungee.exceptions.AuthException;
import com.ericlam.mc.loginsystem.bungee.managers.LoginManager;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class UnRegisterCommand extends FutureAuthCommandNode {

    public UnRegisterCommand(LoginManager loginManager, ConfigManager configManager) {
        super(loginManager, configManager, "unregister", "取消註冊", null, "unreg");
    }

    @Override
    public CompletableFuture<Boolean> executeOperation(ProxiedPlayer player, List<String> list) throws AuthException {
        if (loginManager.notLoggedIn(player.getUniqueId())){
            MessageBuilder.sendMessage(player, configManager.getMessage("not-logged-in"));
            return CompletableFuture.completedFuture(false);
        }
        return loginManager.unregister(player);
    }
}