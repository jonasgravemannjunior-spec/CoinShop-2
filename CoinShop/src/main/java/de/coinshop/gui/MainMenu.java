package de.coinshop.gui;

import de.coinshop.CoinShopPlugin;
import de.coinshop.ItemUtil;
import org.bukkit.Material;

/** Das Hauptmenü: Auswahl zwischen Server-Shop und Spieler-Shop. */
public final class MainMenu extends Menu {

    public MainMenu(CoinShopPlugin plugin) {
        super(plugin, 3, plugin.messages().get("gui.main.title"));
        var msg = plugin.messages();
        int hour = plugin.getConfig().getInt("server-shop.reset-hour", 12);

        // Links: Server-Shop
        set(11, ItemUtil.icon(Material.ENDER_CHEST, msg.get("gui.main.server-name"),
                        msg.getList("gui.main.server-lore", "hour", hour)),
                (player, click) -> new ServerShopMenu(plugin, player).open(player));

        // Mitte: Kontostand (nur Anzeige)
        // wird unten beim Öffnen mit dem aktuellen Spieler gesetzt

        // Rechts: Spieler-Shop
        set(15, ItemUtil.icon(Material.EMERALD, msg.get("gui.main.player-name"),
                        msg.getList("gui.main.player-lore")),
                (player, click) -> new PlayerShopMenu(plugin, player, 0).open(player));

        // Unten: Schließen
        set(22, ItemUtil.icon(Material.BARRIER, msg.get("gui.close-name"), java.util.List.of()),
                (player, click) -> player.closeInventory());

        fillEmpty();
    }

    @Override
    public void open(org.bukkit.entity.Player player) {
        // Der Kontostand hängt vom Spieler ab, deshalb erst beim Öffnen einsetzen
        set(13, balanceIcon(player), null);
        super.open(player);
    }
}
