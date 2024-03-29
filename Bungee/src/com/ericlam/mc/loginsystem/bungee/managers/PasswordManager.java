package com.ericlam.mc.loginsystem.bungee.managers;

import com.ericlam.mc.bungee.dnmc.SQLDataSource;
import com.ericlam.mc.bungee.dnmc.main.DragonNiteMC;
import com.ericlam.mc.loginsystem.bungee.exceptions.AccountNonExistException;
import com.ericlam.mc.loginsystem.bungee.exceptions.AlreadyRegisteredException;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PasswordManager {

    private final SQLDataSource sqlDataSource;
    private final Map<UUID, String> passwordMap = new ConcurrentHashMap<>();

    PasswordManager(Plugin plugin) {
        this.sqlDataSource = DragonNiteMC.getAPI().getSQLDataSource();
        ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
            try (Connection connection = sqlDataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "CREATE TABLE IF NOT EXISTS `LoginData` (UUID VARCHAR(40) NOT NULL PRIMARY KEY, Name TINYTEXT NOT NULL, Password LONGTEXT NOT NULL, IP TINYTEXT NOT NULL )")) {
                statement.execute();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    boolean matchPassword(UUID player, String password) throws AccountNonExistException {
        return getPasswordHash(player).map(s -> s.equals(hashing(password))).orElseThrow(AccountNonExistException::new);
    }

    static String hashing(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] pw = password.getBytes();
            byte[] hashed = digest.digest(pw);
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return password;
        }
    }

    boolean register(ProxiedPlayer player, final String password) {
        if (passwordMap.containsKey(player.getUniqueId())) throw new AlreadyRegisteredException();
        final String encoded = hashing(password);
        try (Connection connection = sqlDataSource.getConnection(); PreparedStatement statement = connection.prepareStatement("INSERT IGNORE INTO `LoginData` VALUES  (?,?,?,?)")) {
            statement.setString(1, player.getUniqueId().toString());
            statement.setString(2, player.getName());
            statement.setString(3, encoded);
            statement.setString(4, IPManager.getIP(player));
            int result = statement.executeUpdate();
            if (result == 0) throw new AlreadyRegisteredException();
            passwordMap.putIfAbsent(player.getUniqueId(), encoded);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    boolean editPassword(UUID uuid, String name, final String password) {
        if (!passwordMap.containsKey(uuid)) throw new AccountNonExistException();
        final String encoded = hashing(password);
        try (Connection connection = sqlDataSource.getConnection(); PreparedStatement statement = connection.prepareStatement("UPDATE `LoginData` SET `Name`=?, `Password`=? WHERE `UUID`=?")) {
            statement.setString(1, name);
            statement.setString(2, encoded);
            statement.setString(3, uuid.toString());
            int result = statement.executeUpdate();
            if (result == 0) throw new AccountNonExistException();
            this.passwordMap.put(uuid, encoded);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    boolean unregister(UUID uuid) {
        if (!passwordMap.containsKey(uuid)) throw new AccountNonExistException();
        try (Connection connection = sqlDataSource.getConnection(); PreparedStatement statement = connection.prepareStatement("DELETE FROM `LoginData` WHERE `UUID`=?")) {
            statement.setString(1, uuid.toString());
            int result = statement.executeUpdate();
            if (result == 0) throw new AccountNonExistException();
            passwordMap.remove(uuid);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    Optional<String> getPasswordHash(UUID uuid) {
        return Optional.ofNullable(passwordMap.get(uuid));
    }


    void getFromSQL(UUID uuid) {
        if (passwordMap.containsKey(uuid)) {
            return;
        }
        try (Connection connection = sqlDataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT `Password` FROM `LoginData` WHERE `UUID`=?")) {
            statement.setString(1, uuid.toString());
            ResultSet set = statement.executeQuery();
            if (set.next()) {
                String pwHash = set.getString("Password");
                this.passwordMap.put(uuid, pwHash);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
