package com.ericlam.mc.loginsystem.bungee.commands.admin;

import com.ericlam.mc.bungee.dnmc.config.YamlManager;
import com.ericlam.mc.bungee.dnmc.container.OfflinePlayer;
import com.ericlam.mc.loginsystem.bungee.exceptions.AuthException;
import com.ericlam.mc.loginsystem.bungee.managers.LoginManager;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class UnregisterCommand extends AdminAuthNode {
    UnregisterCommand(LoginManager loginManager, YamlManager configManager) {
        super(loginManager, configManager, "unregister", "取消註冊玩家", null, "unreg");
    }

    @Override
    public CompletableFuture<Boolean> execution(ProxiedPlayer player, List<String> list, OfflinePlayer target) throws AuthException {
        return loginManager.unregister(target).thenApply(bool -> {
            if (bool) loginManager.tryKick(target.getUniqueId(), "admin-kick.unregistered");
            return bool;
        });
    }
}
