package de.coinshop;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.advancement.Advancement;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;

/**
 * Reagiert auf Ereignisse im Spiel:
 *  - Spieler tritt bei        -> Konto anlegen, Hinweise anzeigen
 *  - Erfolg geschafft         -> Coins als Belohnung
 *  - Chat-Nachricht           -> Preis-Eingabe beim Einstellen eines Items
 */
public final class PlayerListener implements Listener {

    private final CoinShopPlugin plugin;

    public PlayerListener(CoinShopPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Database db = plugin.database();
        db.ensurePlayer(player.getUniqueId(), player.getName());

        // Eine Sekunde warten, damit die Hinweise nicht in den Beitritts-Meldungen untergehen
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            Messages msg = plugin.messages();
            for (Database.SaleNotice notice : db.popSaleNotices(player.getUniqueId())) {
                msg.send(player, "listing-sold", "amount", notice.amount(),
                        "item", ItemUtil.name(notice.material()), "coins", notice.coins());
            }
            int claims = db.countClaims(player.getUniqueId());
            if (claims > 0) {
                msg.send(player, "claims-waiting", "count", claims);
            }
        }, 20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.playerShop().cancelAwaiting(event.getPlayer());
    }

    /** Coins für Erfolge. */
    @EventHandler
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        if (!plugin.getConfig().getBoolean("achievements.enabled", true)) {
            return;
        }
        Advancement advancement = event.getAdvancement();

        // Unsichtbare Fortschritte (z. B. Rezepte-Freischaltungen) zählen nicht
        if (advancement.getDisplay() == null) {
            return;
        }
        String path = advancement.getKey().getKey();      // z. B. "story/mine_stone"
        String fullKey = advancement.getKey().toString(); // z. B. "minecraft:story/mine_stone"
        if (path.startsWith("recipes/")) {
            return;
        }

        // Normale Belohnung, oder eine eigene aus "achievements.overrides"
        long reward = plugin.getConfig().getLong("achievements.default-reward", 10);
        ConfigurationSection overrides = plugin.getConfig().getConfigurationSection("achievements.overrides");
        if (overrides != null && overrides.contains(fullKey)) {
            reward = overrides.getLong(fullKey);
        }
        if (reward <= 0) {
            return;
        }

        Player player = event.getPlayer();
        plugin.database().ensurePlayer(player.getUniqueId(), player.getName());
        plugin.database().addCoins(player.getUniqueId(), reward);
        plugin.messages().send(player, "achievement-reward", "coins", reward);
    }

    /**
     * Fängt die Chat-Nachricht ab, wenn der Spieler gerade nach einem Preis gefragt wurde.
     * Diese Methode läuft NICHT im Haupt-Thread, deshalb übergeben wir die Arbeit mit runTask.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!plugin.playerShop().isAwaitingPrice(player)) {
            return;
        }
        event.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        Bukkit.getScheduler().runTask(plugin, () -> plugin.playerShop().handlePriceInput(player, text));
    }
}
