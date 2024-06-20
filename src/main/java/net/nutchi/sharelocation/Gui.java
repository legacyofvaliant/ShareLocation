package net.nutchi.sharelocation;

import lombok.RequiredArgsConstructor;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class Gui {
    private final ShareLocation plugin;

    private final Inventory inventory;
    private final List<String> editModePlayers = new ArrayList<>();

    private static final int TP_ITEM_CUSTOM_MODEL_DATA = 7777;
    private static final Material TP_ITEM_TYPE = Material.ENDER_PEARL;
    private static final Material ADD_BUTTON_TYPE = Material.EMERALD_BLOCK;
    private static final Material EDIT_MODE_BUTTON_TYPE = Material.WRITABLE_BOOK;
    private static final int ADD_BUTTON_SLOT = 48;
    private static final int EDIT_MODE_BUTTON_SLOT = 50;
    private static final int LAST_AVAILABLE_SLOT = 44;
    private static final int INVENTORY_SIZE = 54;

    public static Inventory createInventory(JavaPlugin plugin) {
        Inventory inv = plugin.getServer().createInventory(null, INVENTORY_SIZE, "共有地点");

        ItemStack addButton = new ItemStack(ADD_BUTTON_TYPE);
        ItemMeta addButtonMeta = addButton.getItemMeta();
        addButtonMeta.setDisplayName("地点を追加する");
        addButton.setItemMeta(addButtonMeta);
        inv.setItem(ADD_BUTTON_SLOT, addButton);

        ItemStack editButton = new ItemStack(EDIT_MODE_BUTTON_TYPE);
        ItemMeta editButtonMeta = editButton.getItemMeta();
        editButtonMeta.setDisplayName("アイテムを編集する");
        editButton.setItemMeta(editButtonMeta);
        inv.setItem(EDIT_MODE_BUTTON_SLOT, editButton);

        return inv;
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack currentItem = event.getCurrentItem();

        boolean isMainGui = inventory.equals(event.getClickedInventory()) || inventory.equals(event.getInventory());
        boolean isTpButton = isMainGui && currentItem != null && currentItem.getType().equals(TP_ITEM_TYPE) && currentItem.getItemMeta() != null && currentItem.getItemMeta().hasCustomModelData() && currentItem.getItemMeta().getCustomModelData() == TP_ITEM_CUSTOM_MODEL_DATA;
        boolean isAddButton = isMainGui && currentItem != null && currentItem.getType().equals(ADD_BUTTON_TYPE) && event.getSlot() == ADD_BUTTON_SLOT;
        boolean isEditButton = isMainGui && currentItem != null && currentItem.getType().equals(EDIT_MODE_BUTTON_TYPE) && event.getSlot() == EDIT_MODE_BUTTON_SLOT;
        boolean onEditMode = isMainGui && editModePlayers.contains(player.getName());
        boolean unmovable = isMainGui && (!onEditMode && isTpButton || isAddButton || isEditButton);

        if (unmovable) {
            event.setCancelled(true);
        }

        if (!onEditMode && isTpButton) {
            getTeleportLocation(currentItem.getItemMeta()).ifPresent(player::teleport);
        } else if (isAddButton && player.hasPermission("sharelocation.add")) {
            openAddDialog(player);
        } else if (!onEditMode && isEditButton && player.hasPermission("sharelocation.edit")) {
            enterEditMode(player);
        } else if (onEditMode && isEditButton) {
            leaveEditMode(player);
        }
    }

    public void openAddDialog(Player player) {
        new AnvilGUI.Builder()
                .plugin(plugin)
                .title("地点名を入力")
                .text("地点名")
                .onClick((slot, stateSnapshot) -> {
                    if (slot == AnvilGUI.Slot.OUTPUT && !stateSnapshot.getText().isEmpty()) {
                        addLocation(stateSnapshot.getText(), player.getLocation());
                        return Collections.singletonList(AnvilGUI.ResponseAction.openInventory(inventory));
                    }
                    return Collections.emptyList();
                })
                .open(player);
    }

    private void enterEditMode(Player player) {
        editModePlayers.add(player.getName());

        ItemStack editButton = inventory.getItem(EDIT_MODE_BUTTON_SLOT);
        if (editButton != null && editButton.getItemMeta() != null) {
            ItemMeta meta = editButton.getItemMeta();
            meta.addEnchant(Enchantment.MENDING, 1, false);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.setLore(Stream.concat(Stream.of(ChatColor.GREEN + "編集中:"), editModePlayers.stream()).collect(Collectors.toList()));
            editButton.setItemMeta(meta);
        }
    }

    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(inventory)) {
            leaveEditMode((Player) event.getPlayer());
            saveInventory();
        }
    }

    private void leaveEditMode(Player player) {
        editModePlayers.remove(player.getName());

        ItemStack editButton = inventory.getItem(EDIT_MODE_BUTTON_SLOT);
        if (editButton != null && editButton.getItemMeta() != null) {
            ItemMeta meta = editButton.getItemMeta();
            if (meta.getLore() != null) {
                if (meta.getLore().size() == 2 && meta.getLore().get(1).equalsIgnoreCase(player.getName())) {
                    meta.setLore(null);
                    meta.getEnchants().forEach((e, l) -> meta.removeEnchant(e));
                } else {
                    meta.setLore(meta.getLore().stream().filter(l -> !l.equalsIgnoreCase(player.getName())).collect(Collectors.toList()));
                }
                editButton.setItemMeta(meta);
            }
        }
    }

    private void saveInventory() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> plugin.getStorage().saveInventory(inventory));
    }

    private void addLocation(String name, Location loc) {
        ItemStack tpItem = new ItemStack(TP_ITEM_TYPE);
        ItemMeta meta = tpItem.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList("world: " + Optional.ofNullable(loc.getWorld()).map(World::getName).orElse(""), "x: " + loc.getX(), "y: " + loc.getY(), "z: " + loc.getZ(), "yaw: " + loc.getYaw(), "pitch: " + loc.getPitch()));
        meta.setCustomModelData(TP_ITEM_CUSTOM_MODEL_DATA);
        tpItem.setItemMeta(meta);

        for (int i = 0; i <= LAST_AVAILABLE_SLOT; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, tpItem);
                return;
            }
        }
    }

    private Optional<Location> getTeleportLocation(ItemMeta itemMeta) {
        if (itemMeta.getLore() != null && itemMeta.getLore().size() == 6) {
            String world = itemMeta.getLore().get(0).split("world: ")[1];
            double x = Double.parseDouble(itemMeta.getLore().get(1).split("x: ")[1]);
            double y = Double.parseDouble(itemMeta.getLore().get(2).split("y: ")[1]);
            double z = Double.parseDouble(itemMeta.getLore().get(3).split("z: ")[1]);
            float yaw = Float.parseFloat(itemMeta.getLore().get(4).split("yaw: ")[1]);
            float pitch = Float.parseFloat(itemMeta.getLore().get(5).split("pitch: ")[1]);

            return Optional.ofNullable(plugin.getServer().getWorld(world)).map(w -> new Location(w, x, y, z, yaw, pitch));
        } else {
            return Optional.empty();
        }
    }

    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (
                event.getItemDrop().getItemStack().getType().equals(TP_ITEM_TYPE) &&
                event.getItemDrop().getItemStack().getItemMeta() != null &&
                event.getItemDrop().getItemStack().getItemMeta().hasCustomModelData() &&
                event.getItemDrop().getItemStack().getItemMeta().getCustomModelData() == TP_ITEM_CUSTOM_MODEL_DATA
        ) {
            event.getItemDrop().remove();
            event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_ITEM_BREAK, 1, 1);
        }
    }
}
