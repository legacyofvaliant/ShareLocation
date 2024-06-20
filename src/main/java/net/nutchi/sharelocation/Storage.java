package net.nutchi.sharelocation;

import lombok.RequiredArgsConstructor;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.*;

@RequiredArgsConstructor
public class Storage {
    private final ShareLocation plugin;
    private Connection connection;

    public boolean connect() {
        try {
            File file = new File(plugin.getDataFolder(), "database.db");
            if (!file.exists()) {
                plugin.getDataFolder().mkdir();
                file.createNewFile();
            }

            String url = "jdbc:sqlite:" + file.getAbsolutePath().replace("\\", "/");
            connection = DriverManager.getConnection(url);

            return true;
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void shutdown() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void init() {
        String sql =
                "CREATE TABLE IF NOT EXISTS inventories (" +
                "id int(10) NOT NULL PRIMARY KEY," +
                "inventory text NOT NULL" +
                ")";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Nullable
    public Inventory loadInventory() {
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM inventories WHERE id = ?")) {
            statement.setInt(1, 1);

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return fromBase64(resultSet.getString("inventory"));
            } else {
                return Gui.createInventory(plugin);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void saveInventory(Inventory inventory) {
        try (PreparedStatement statement = connection.prepareStatement("INSERT OR REPLACE INTO inventories (id, inventory) VALUES (?, ?)")) {
            statement.setInt(1, 1);
            statement.setString(2, toBase64(inventory));

            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Nullable
    private String toBase64(Inventory inv) {
        try (
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                BukkitObjectOutputStream bukkitOutputStream = new BukkitObjectOutputStream(outputStream)
        ) {

            bukkitOutputStream.writeInt(inv.getSize());

            for (int i = 0; i < inv.getSize(); i++) {
                bukkitOutputStream.writeObject(inv.getItem(i));
            }

            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private @Nullable Inventory fromBase64(String data) {
        try (
                ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
                BukkitObjectInputStream bukkitInputStream = new BukkitObjectInputStream(inputStream)
        ) {
            Inventory inv = plugin.getServer().createInventory(null, bukkitInputStream.readInt());

            for (int i = 0; i < inv.getSize(); i++) {
                inv.setItem(i, (ItemStack) bukkitInputStream.readObject());
            }

            return inv;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}
