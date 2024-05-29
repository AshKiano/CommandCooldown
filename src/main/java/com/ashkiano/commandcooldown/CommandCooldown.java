package com.ashkiano.commandcooldown;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CommandCooldown extends JavaPlugin implements CommandExecutor {

    private Map<String, Map<String, Long>> cooldowns = new HashMap<>();
    private FileConfiguration config;
    private File cooldownFile;
    private FileConfiguration cooldownConfig;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        config = this.getConfig();
        cooldownFile = new File(getDataFolder(), "cooldowns.yml");
        if (!cooldownFile.exists()) {
            try {
                cooldownFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        cooldownConfig = YamlConfiguration.loadConfiguration(cooldownFile);
        loadCooldowns();
        this.getCommand("cooldown").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(new CommandCooldownListener(this), this);
        Metrics metrics = new Metrics(this, 22055);
    }

    private void loadCooldowns() {
        for (String key : cooldownConfig.getKeys(false)) {
            String playerName = key;
            Map<String, Long> playerCooldowns = new HashMap<>();
            for (String command : cooldownConfig.getConfigurationSection(key).getKeys(false)) {
                playerCooldowns.put(command, cooldownConfig.getLong(key + "." + command));
            }
            cooldowns.put(playerName, playerCooldowns);
        }
    }

    private void saveCooldowns() {
        try {
            cooldownConfig.save(cooldownFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isOnCooldown(Player player, String command) {
        String playerName = player.getName();
        if (cooldowns.containsKey(playerName)) {
            Map<String, Long> playerCooldowns = cooldowns.get(playerName);
            if (playerCooldowns.containsKey(command)) {
                long cooldownTime = getCooldownTime(command);
                long timeLeft = (playerCooldowns.get(command) + cooldownTime) - System.currentTimeMillis();
                if (timeLeft > 0) {
                    String message = config.getString("messages.cooldown", "You must wait %time% seconds to use this command again.");
                    player.sendMessage(message.replace("%time%", String.valueOf(timeLeft / 1000)));
                    return true;
                } else {
                    playerCooldowns.remove(command);
                    if (playerCooldowns.isEmpty()) {
                        cooldowns.remove(playerName);
                        cooldownConfig.set(playerName, null);
                    }
                    saveCooldowns();
                }
            }
        }
        return false;
    }

    public void setCooldown(Player player, String command) {
        String playerName = player.getName();
        long currentTime = System.currentTimeMillis();
        cooldowns.computeIfAbsent(playerName, k -> new HashMap<>()).put(command, currentTime);
        cooldownConfig.set(playerName + "." + command, currentTime);
        saveCooldowns();
    }

    private long getCooldownTime(String command) {
        return config.getLong("cooldowns." + command, 0) * 1000;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length == 2 && args[0].matches("[a-zA-Z0-9]+") && args[1].matches("\\d+")) {
                String cmd = args[0];
                int cooldown = Integer.parseInt(args[1]);
                config.set("cooldowns." + cmd, cooldown);
                saveConfig();
                player.sendMessage("Cooldown time for " + cmd + " set to " + cooldown + " seconds.");
                return true;
            } else {
                player.sendMessage("Usage: /cooldown <command> <seconds>");
                return false;
            }
        }
        return false;
    }
}
