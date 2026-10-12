package de.coinshop;

import de.coinshop.Database.Offer;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Der Server-Shop: 10 zufällige Angebote, feste Preise, täglicher Wechsel um 12:00 Uhr.
 */
public final class ServerShopManager {

    private static final String META_LAST_RESET = "server_shop_last_reset";

    private final CoinShopPlugin plugin;
    private final Random random = new Random();

    public ServerShopManager(CoinShopPlugin plugin) {
        this.plugin = plugin;
    }

    public List<Offer> getOffers() {
        return plugin.database().getOffers();
    }

    // ------------------------------------------------------------------
    //  Zeitplan
    // ------------------------------------------------------------------

    private ZoneId zone() {
        try {
            return ZoneId.of(plugin.getConfig().getString("server-shop.timezone", "Europe/Berlin"));
        } catch (Exception e) {
            return ZoneId.of("Europe/Berlin");
        }
    }

    private int resetHour() {
        int hour = plugin.getConfig().getInt("server-shop.reset-hour", 12);
        return Math.max(0, Math.min(23, hour));
    }

    /** Der letzte Zeitpunkt (heute oder gestern 12:00 Uhr), an dem der Shop gewechselt haben sollte. */
    private ZonedDateTime lastScheduledReset() {
        ZonedDateTime now = ZonedDateTime.now(zone());
        ZonedDateTime reset = now.withHour(resetHour()).withMinute(0).withSecond(0).withNano(0);
        if (reset.isAfter(now)) {
            reset = reset.minusDays(1);
        }
        return reset;
    }

    /** Wie lange es noch bis zum nächsten Wechsel dauert. */
    public Duration timeUntilReset() {
        ZonedDateTime next = lastScheduledReset().plusDays(1);
        return Duration.between(ZonedDateTime.now(zone()), next);
    }

    /** Wird alle 30 Sekunden aufgerufen: wechselt die Angebote, wenn 12:00 Uhr überschritten wurde. */
    public void checkReset() {
        try {
            long lastDone = plugin.database().getMetaLong(META_LAST_RESET, 0L);
            long shouldHave = lastScheduledReset().toInstant().toEpochMilli();
            if (lastDone < shouldHave || plugin.database().getOffers().isEmpty()) {
                rotate();
            }
        } catch (RuntimeException e) {
            plugin.getLogger().severe("Server-Shop-Wechsel fehlgeschlagen: " + e.getMessage());
        }
    }

    /** Erzwingt einen Wechsel (/coinsadmin resetshop). */
    public void forceRotate() {
        rotate();
    }

    // ------------------------------------------------------------------
    //  Neue Angebote auslosen
    // ------------------------------------------------------------------

    private void rotate() {
        ConfigurationSection pool = plugin.getConfig().getConfigurationSection("server-shop.pool");
        List<Material> candidates = new ArrayList<>();
        if (pool != null) {
            for (String name : pool.getKeys(false)) {
                Material material = Material.matchMaterial(name);
                if (material == null || material.isAir() || !material.isItem()) {
                    plugin.getLogger().warning("Server-Shop: '" + name + "' ist kein gültiges Item und wird ignoriert.");
                    continue;
                }
                candidates.add(material);
            }
        }
        Collections.shuffle(candidates, random);

        int count = Math.min(plugin.getConfig().getInt("server-shop.offers", 10), candidates.size());
        List<Offer> offers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Material material = candidates.get(i);
            ConfigurationSection entry = pool.getConfigurationSection(material.name());
            if (entry == null) {
                entry = pool.getConfigurationSection(material.name().toLowerCase());
            }
            int min = entry == null ? 1 : Math.max(1, entry.getInt("min", 1));
            int max = entry == null ? 1 : Math.max(min, entry.getInt("max", min));
            long pricePerItem = entry == null ? 1 : Math.max(1, entry.getLong("price", 1));

            // Zufällige Menge, aber nie mehr als in einen Stapel passt
            int amount = min + random.nextInt(max - min + 1);
            amount = Math.max(1, Math.min(amount, material.getMaxStackSize()));

            offers.add(new Offer(i, material, amount, amount * pricePerItem));
        }

        plugin.database().replaceOffers(offers);
        plugin.database().setMetaLong(META_LAST_RESET, System.currentTimeMillis());
        plugin.getLogger().info("Server-Shop: " + offers.size() + " neue Angebote ausgelost.");
    }

    // ------------------------------------------------------------------
    //  Kaufen
    // ------------------------------------------------------------------

    /** Ist dieses Angebot noch genau so aktuell? (Falls der Shop gewechselt hat, während das Menü offen war.) */
    private boolean isCurrent(Offer offer) {
        for (Offer current : plugin.database().getOffers()) {
            if (current.equals(offer)) {
                return true;
            }
        }
        return false;
    }

    public void buy(Player player, Offer offer) {
        Messages msg = plugin.messages();
        Database db = plugin.database();
        UUID uuid = player.getUniqueId();

        if (!isCurrent(offer)) {
            msg.send(player, "server-offer-outdated");
            return;
        }

        int limit = plugin.getConfig().getInt("server-shop.purchase-limit-per-offer", 0);
        if (limit > 0 && db.getPurchases(uuid, offer.slot()) >= limit) {
            msg.send(player, "server-limit-reached", "limit", limit);
            return;
        }

        ItemStack stack = new ItemStack(offer.material(), offer.amount());
        if (!ItemUtil.canFit(player.getInventory(), stack)) {
            msg.send(player, "inventory-full");
            return;
        }

        // Bezahlen und Kauf zählen: alles oder nichts
        db.begin();
        try {
            if (!db.takeCoins(uuid, offer.price())) {
                db.rollback();
                msg.send(player, "not-enough");
                return;
            }
            db.addPurchase(uuid, offer.slot());
            db.commit();
        } catch (RuntimeException e) {
            db.rollback();
            plugin.getLogger().severe("Fehler beim Kauf im Server-Shop: " + e.getMessage());
            msg.send(player, "error");
            return;
        }

        player.getInventory().addItem(stack);
        msg.send(player, "server-bought", "amount", offer.amount(),
                "item", ItemUtil.name(offer.material()), "coins", offer.price());
    }
}
