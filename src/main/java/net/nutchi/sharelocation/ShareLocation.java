package net.nutchi.sharelocation;

import lombok.Getter;
import net.nutchi.sharelocation.listener.InventoryListener;
import net.nutchi.sharelocation.listener.PlayerListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

@Getter
public final class ShareLocation extends JavaPlugin {
    private Gui gui;
    private final Storage storage = new Storage(this);

    @Override
    public void onEnable() {
        register();

        if (storage.connect()) {
            storage.init();
            Inventory inventory = storage.loadInventory();
            if (inventory != null) {
                gui = new Gui(this, inventory);
                return;
            }
        }

        getServer().getPluginManager().disablePlugin(this);
    }

    private void register() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new InventoryListener(this), this);
        pm.registerEvents(new PlayerListener(this), this);
    }

    @Override
    public void onDisable() {
        storage.shutdown();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player) {
            gui.open((Player) sender);
        }

        return true;
    }
}
