package net.nutchi.sharelocation.listener;

import lombok.RequiredArgsConstructor;
import net.nutchi.sharelocation.ShareLocation;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

@RequiredArgsConstructor
public class InventoryListener implements Listener {
    private final ShareLocation plugin;

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        plugin.getGui().onInventoryClick(event);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        plugin.getGui().onInventoryClose(event);
    }
}
