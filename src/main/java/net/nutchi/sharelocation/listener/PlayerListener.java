package net.nutchi.sharelocation.listener;

import lombok.RequiredArgsConstructor;
import net.nutchi.sharelocation.ShareLocation;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;

@RequiredArgsConstructor
public class PlayerListener implements Listener {
    private final ShareLocation plugin;

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        plugin.getGui().onPlayerDropItem(event);
    }
}
