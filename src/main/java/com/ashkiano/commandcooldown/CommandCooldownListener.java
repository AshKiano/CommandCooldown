package com.ashkiano.commandcooldown;


import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.entity.Player;

public class CommandCooldownListener implements Listener {

    private final CommandCooldown plugin;

    public CommandCooldownListener(CommandCooldown plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().split(" ")[0].substring(1);

        if (plugin.isOnCooldown(player, command)) {
            event.setCancelled(true);
        } else {
            plugin.setCooldown(player, command);
        }
    }
}