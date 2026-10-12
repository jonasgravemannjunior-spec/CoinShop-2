package de.coinshop.gui;

import de.coinshop.CoinShopPlugin;
import de.coinshop.Database.Listing;
import de.coinshop.ItemUtil;
import de.coinshop.Messages;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/** "Meine Angebote": eigene Angebote ansehen/abbrechen und zur Abholung wechseln. */
public final class MyListingsMenu extends Menu {

    public MyListingsMenu(CoinShopPlugin plugin, Player viewer) {
        super(plugin, 5, plugin.messages().get("gui.mine.title"));
        Messages msg = plugin.messages();

        List<Listing> mine = plugin.database().getListingsOf(viewer.getUniqueId());
        if (mine.isEmpty()) {
            set(22, ItemUtil.icon(Material.BARRIER, msg.get("gui.mine.empty-name"), List.of()), null);
        }

        for (int i = 0; i < mine.size() && i < 36; i++) {
            Listing listing = mine.get(i);
            long timeLeft = listing.expires() - System.currentTimeMillis();
            ItemStack display = ItemUtil.withExtraLore(listing.item(),
                    msg.getList("gui.mine.listing-lore", "price", listing.price(),
                            "time", ItemUtil.formatDuration(timeLeft)));

            set(i, display, (player, click) -> {
                plugin.playerShop().cancel(player, listing.id());
                new MyListingsMenu(plugin, player).open(player);
            });
        }

        // Untere Reihe: Zurück | Abholung | Kontostand
        set(38, backButton(), (player, click) -> new PlayerShopMenu(plugin, player, 0).open(player));

        int claims = plugin.database().countClaims(viewer.getUniqueId());
        set(40, ItemUtil.icon(Material.ENDER_CHEST, msg.get("gui.mine.claims-name"),
                        msg.getList("gui.mine.claims-lore", "count", claims)),
                (player, click) -> new ClaimsMenu(plugin, player).open(player));

        set(42, balanceIcon(viewer), null);

        fillEmpty();
    }
}
