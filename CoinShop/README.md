# CoinShop – Coins, Server-Shop und Spieler-Shop

Ein Plugin für **Paper 26.3** (Java 25).

## Was kann das Plugin?

| Befehl | Wirkung |
|---|---|
| `/shop` | Öffnet das Menü: **Server-Shop** oder **Spieler-Shop** |
| `/shop sell [Preis]` | Bietet das Item in deiner Hand im Spieler-Shop an (Alternative zum Menü) |
| `/coins [Spieler]` | Zeigt deinen Kontostand oder den eines anderen Spielers |
| `/pay [Spieler] [Betrag]` | Überweist Coins an einen anderen Spieler |
| `/baltop` | Rangliste der 10 reichsten Spieler |
| `/coinsadmin give\|take\|set [Spieler] [Betrag]` | Coins verwalten (nur Admins) |
| `/coinsadmin resetshop` | Server-Shop sofort neu auslosen (nur Admins) |
| `/coinsadmin reload` | `config.yml` und `messages.yml` neu laden (nur Admins) |

**Coins verdienen:** Für jeden Erfolg (Advancement) gibt es Coins (Standard: 10). Kein Startguthaben.
Die Sonnenblume ist das Symbol der Coins in allen Menüs.

**Server-Shop:** 10 zufällige Items mit zufälliger Menge zu festen Preisen. Jeden Tag um 12:00 Uhr
(Zeitzone Europe/Berlin) werden alle 10 komplett neu ausgelost. Pro Angebot darf jeder Spieler
standardmäßig 3x pro Tag kaufen (einstellbar).

**Spieler-Shop:** Spieler bieten Items zu einem frei gewählten Preis an. Der Verkäufer bekommt die Coins
sofort (auch wenn er offline ist), abzüglich Steuer.

**Schutz gegen "Zwischenlager"-Missbrauch:**
- Einstellgebühr (Standard 5 %, mindestens 1 Coin), die **nie zurückerstattet** wird
- Maximal 5 Angebote pro Spieler gleichzeitig
- Angebote laufen nach 48 Stunden ab (Item landet in der "Abholung")
- Mindest- und Höchstpreis, Verkaufssteuer (5 %), Sperrliste für Items

Hinweis: Wegen der Einstellgebühr braucht man mindestens 1 Coin, um etwas anzubieten. Das ist Absicht.
Alle Werte kannst du in `plugins/CoinShop/config.yml` ändern.

---

## So bekommst du die fertige Plugin-Datei (`CoinShop.jar`)

Der Server kann nur die fertige `.jar`-Datei benutzen, nicht den Quellcode. Es gibt zwei Wege.

### Weg A: Automatisch auf GitHub bauen lassen (nichts installieren)

1. Erstelle kostenlos ein Konto auf <https://github.com>.
2. Erstelle dort ein neues Repository (z. B. `CoinShop`).
3. Lade den **gesamten Inhalt** dieses Ordners hoch (auch den versteckten Ordner `.github`).
4. Öffne im Repository den Reiter **Actions**. Der Bau startet automatisch (dauert ca. 2–3 Minuten).
5. Klicke auf den fertigen Durchlauf. Unten bei **Artifacts** lädst du `CoinShop` herunter (eine ZIP mit der `CoinShop.jar`).
6. Wird der Durchlauf rot, öffne ihn und kopiere die Fehlermeldung. Damit kann ich den Fehler beheben.

### Weg B: Auf dem eigenen PC bauen

1. Installiere **Java 25** (z. B. Temurin: <https://adoptium.net>).
2. Öffne in diesem Ordner eine Eingabeaufforderung (Windows: `cmd`) und gib ein:
   - Windows: `gradlew.bat build`
   - Mac/Linux: `./gradlew build`
3. Die fertige Datei liegt danach in `build/libs/CoinShop.jar`.

---

## Installation auf dem Server (Serverminer)

1. Lade `CoinShop.jar` in den Ordner **`plugins`** deines Servers hoch (über das Datei-Menü im Serverminer-Panel).
2. Der Server muss **Paper** in Version **26.3** sein. Prüfe im Panel, ob dein Anbieter Paper 26.3 anbietet. Es ist bei Paper noch als experimentell/Beta gekennzeichnet.
3. Starte den Server neu. In der Konsole sollte stehen: `CoinShop wurde gestartet.`
4. Danach findest du die Einstellungen unter `plugins/CoinShop/` (`config.yml`, `messages.yml`).
5. Gib dir als Admin zum Testen Coins: `/coinsadmin give DeinName 500`

Falls schon andere Plugins `/pay`, `/baltop`, `/coins` oder `/shop` belegen (z. B. EssentialsX), gibt es Befehlskonflikte. Dann entweder das andere Plugin für diese Befehle abschalten oder mir Bescheid geben.

## Wichtig: Noch nicht getestet

Der Code wurde geschrieben, aber noch **nicht kompiliert und nicht auf einem echten Server getestet**.
Beim ersten Bau können Fehlermeldungen auftauchen (z. B. weil sich ein Paar API-Namen in Paper 26.3 unterscheidet).
Das ist normal und schnell behoben: Fehlermeldung kopieren und weitergeben.

## Projektaufbau (zum Nachlesen)

```
src/main/resources/
  plugin.yml          Steckbrief: Name, Befehle, Rechte
  config.yml          Einstellungen (Belohnungen, Preise, Item-Topf, Gebühren)
  messages.yml        Alle Texte (deutsch, änderbar)
src/main/java/de/coinshop/
  CoinShopPlugin.java       Start/Stopp des Plugins, verbindet alles
  Database.java             Speichert alles in einer SQLite-Datei (coinshop.db)
  Messages.java             Lädt und formatiert die Texte
  ItemUtil.java             Kleine Item-Helfer
  PlayerListener.java       Beitreten, Erfolge -> Coins, Chat-Eingabe des Preises
  CoinCommands.java         /coins /pay /baltop /coinsadmin
  ShopCommand.java          /shop
  ServerShopManager.java    Auslosung um 12:00 Uhr, Kauf im Server-Shop
  PlayerShopManager.java    Einstellen, Kaufen, Abbrechen, Ablaufen, Abholen
  gui/                      Alle Menüs (Haupt-, Server-Shop-, Spieler-Shop-, Meine-Angebote-, Abholungs-Menü)
```
