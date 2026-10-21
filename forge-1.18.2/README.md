# Playtime + AFK Tracker 1.2.1 — Forge 1.18.2: GUI / No GUI

Zwei getrennte Builds für **Minecraft 1.18.2**, **Forge 40.3.11+ innerhalb
40.x** und **Java 17**. Zum Bauen wird außerdem Python 3.11+ benötigt.

| Variante | Server | Clients | Funktionen |
|---|---|---|---|
| **No GUI** | JAR auf einem dedizierten Forge-Server | Kein Tracker und kein Modloader nötig | Spielstunden im Tab und AFK in Tab/über dem Kopf |
| **GUI** | GUI-JAR auf dem Forge-Server | Dieselbe GUI-JAR und Forge auf jedem Client | Zusätzlich die Inventar-Leaderboards; auch im Einzelspieler nutzbar |

**Ein reiner Mojang-Vanilla-Server kann diese Mod-JARs nicht laden.** Die No-GUI-
Variante ist für einen Forge-Server mit unveränderten Clients gedacht. Die
Vanilla-Client-Verbindung ist über das Forge-DisplayTest-Protokoll freigegeben;
ein vollständiger Verbindungstest im Spiel steht noch aus. Andere Mods auf dem
Server dürfen ebenfalls keine Client-Mods voraussetzen.

## Korrektur in 1.2.1

Die GUI-Versionen 1.1.0 bis 1.2.0 erzeugten beim Lesen der Live-Streckenstatistik
neue `ResourceLocation`-Objekte. Die Custom-Stat-Registry von Minecraft 1.18.2
verlangt die registrierten Objektinstanzen; dadurch entstand eine
`NullPointerException` beim Öffnen des Leaderboards und beim Abmelden.
1.2.1 verwendet die registrierten `Stats`-Konstanten für alle 13 Streckenzähler.
Die Forge-No-GUI- und Fabric-Versionen waren von diesem Fehler nicht betroffen.
Bei GUI müssen Server und Clients auf dieselbe neue JAR aktualisiert werden.

## Installation

**No GUI:** `playtimetracker-1.18.2-forge-nogui-1.2.1.jar` aus
`build/nogui/libs/` ausschließlich in den `mods/`-Ordner des Servers legen.
Keine Client-Installation, keine eigenen Netzwerkpakete und keine Leaderboard-
Befehle. Die JAR enthält keine Client- oder Leaderboard-Klassen und keine
Inventarbuttons; der Build prüft diese Trennung automatisch.

**GUI:** `playtimetracker-1.18.2-forge-gui-1.2.1.jar` aus `build/gui/libs/`
auf dem Server und allen Clients installieren. Mit **FTB Library für Forge
1.18.2** erscheint ein Uhr-Button in der FTB-Inventarleiste. Ohne FTB Library gibt
es einen eigenen `L`-Button. FTB Teams und FTB Quests sind keine Abhängigkeiten.
`/playtime` und `/leaderboard` öffnen dieselbe Übersicht.

Beim Wechsel die bisherige Tracker-JAR entfernen und den Server neu starten.
Beide Varianten haben dieselbe Mod-ID und dürfen nicht gleichzeitig installiert
sein. Keine Fabric-JAR und kein zusätzliches AFK-Display-Datapack parallel nutzen.

## Leaderboards (nur GUI)

- Hauptfenster und Spieler-Details sind **176 × 166 GUI-Pixel** groß, mittig wie
  das normale Minecraft-Inventar. Die globale GUI-Skalierung von Minecraft gilt
  auch für diese Fenster. Hintergrundrahmen, Schrift, Grautöne und vertiefte Felder
  folgen der bestätigten Inventar-Vorschau.
- Sechs Item-Registerkarten oben: **Spielzeit, Tode, Mob-Kills, abgebaute Blöcke,
  zurückgelegte Strecke und Sprünge**. Tooltips nennen die jeweilige Kategorie.
  Jede Kategorie sortiert absteigend; Gleichstände nach Namen.
- Fünf Spieler pro Seite, mit Kopf, Rang und Statusmarkierung. Mausrad oder
  Pfeilbuttons wechseln die Seite. Lange Namen werden gekürzt; vollständiger Name,
  Status und Statistikwert stehen im Tooltip. Große Zähler werden kompakt angezeigt.
- Namenssuche und Filter für alle Spieler oder nur Online-Spieler.
- Spieler anklicken oder „Details“ wählen: gleich großes Detailfenster mit Kopf,
  Online-/AFK-Status und sechs vertieften Statistikfeldern. „Zurück“ kehrt zur
  vorherigen Kategorie, Suche und Seite zurück.
- Im geöffneten Fenster werden aktuelle Online-Werte etwa alle fünf Sekunden geladen.
  Bereits vorhandene Offline-Statistikdateien werden im Hintergrund alle 60 Sekunden
  neu gelesen. Beim ersten Öffnen kann kurz „Spielerdaten werden geladen“ erscheinen.
- Deutsch und Englisch werden über die Minecraft-Sprache ausgewählt.

Die Daten stammen aus den Vanilla-Statistiken der aktuellen Serverwelt; abgebaute
Blöcke umfassen auch Mod-Blöcke. Spielzeit zählt wie bisher die gesamte Vanilla-
Spielzeit inklusive AFK. Strecke summiert Bewegung und Transport, ohne Fallstrecke.
Es werden keine alten Zeiten erfunden und keine Statistikdateien verändert.
Für Offline-Spieler ohne bekannten Namen im Server-Profilcache erscheint zunächst
`Player` mit einer kurzen Kennung. Nach dem nächsten Login wird ihr Name bekannt.
Die Übersicht überträgt maximal 4096 Spieler; ein größerer Bestand wird im Fenster
gekennzeichnet. Sie enthält keine Positionen oder Inventare und benötigt keine
Internet-Abfrage von Spielernamen.

Die Spielzeit kommt aus der bestehenden Vanilla-Statistik `minecraft:play_time`.
Bei einer bestehenden 1.18.2-Welt zählt die dort gespeicherte Spielzeit weiter;
es ist kein Zurücksetzen oder Import erforderlich. Dies ist ein Mod-Port, kein
Verfahren zum Downgrade einer neueren Minecraft-Welt auf 1.18.2.

## Verhalten

- Tab-Liste: `Alex [12h]`, mit abgerundeten Gesamtstunden; Aktualisierung beim
  Beitritt und Respawn sowie spätestens alle 60 Sekunden.
- Nach 300 Sekunden unveränderter **Blockposition X/Y/Z**: `Alex [12h] [AFK]`
  im Tab und `[AFK]` über dem Kopf. Nur die Kamera zu drehen beendet AFK nicht;
  Bewegung innerhalb desselben Blocks zählt wie im Fabric-Original nicht.
- Nach einem Blockwechsel verschwindet AFK bei der nächsten Sekundenprüfung.
  Die Intervalle werden in Serverticks gemessen; bei niedrigen TPS dauern sie länger.
- Für die Tab-/AFK-Funktion ist keine Konfiguration nötig.
- Die Forge-Tab-Anzeige erhält vorhandene Namen/Formatierungen anderer früherer
  Event-Listener. Die Erweiterung verwendet das Forge-Event statt eines Mixins.

Für das AFK-Schild über dem Kopf wird wie bisher das Scoreboard-Team `ptt-afk`
verwendet. Minecraft erlaubt nur ein Scoreboard-Team pro Spieler: während AFK
greifen daher dessen Eigenschaften. Vorherige Teams werden beim Zurückkehren,
Ausloggen, Respawn und regulären Herunterfahren wiederhergestellt. Von anderen
Mods oder Admins inzwischen geänderte Teamzuordnungen werden nicht überschrieben.
Die Rückkehrzuordnungen werden zusätzlich mit der Welt in
`data/playtimetracker_afk.dat` gespeichert und beim Neustart bereinigt.

## Lokal bauen

Mit JDK 17 in diesem Verzeichnis:

```sh
./gradlew -Pvariant=nogui build
./gradlew -Pvariant=gui build
```

Ergebnisse: `build/nogui/libs/playtimetracker-1.18.2-forge-nogui-1.2.1.jar` und
`build/gui/libs/playtimetracker-1.18.2-forge-gui-1.2.1.jar`.
Ohne `-Pvariant` wird die GUI-Variante gebaut.
Die `-sources.jar` ist Quellcode und gehört nicht in `mods/`.
Gradle 8.8 und ForgeGradle 6.0.54 sind festgelegt; die reine Stunden-/AFK-Logik
und ihre Tests werden direkt mit dem Fabric-Projekt geteilt.

Der GUI-Regressionstest läuft zusätzlich in einem echten Forge-Testserver:

```sh
./gradlew -Pvariant=gui runGameTestServer
```

Vier GameTests prüfen leere Live-Zähler, alle 13 Bewegungsarten einschließlich
Summen über dem Integer-Limit, die Übereinstimmung mit gespeicherten Statistiken,
negative Zähler und den Abmeldepfad mit einem `ServerPlayer`. Sie laufen auch in
GitHub Actions. Testklassen und die leere Teststruktur gehören zu einem eigenen
Source-Set und werden nicht in die ausgelieferten JARs gepackt.

`build` prüft außerdem Runtime- und Sources-JARs und erzeugt `.sha256`-Dateien.
Die No-GUI-Prüfung verbietet Client-Klassen, Leaderboard-Code und GUI-Ressourcen.

Der Build führt die vorhandenen Logiktests und zusätzliche Tests für Anzeige,
Scoreboard-Teamrückkehr, gespeicherte Rückkehrzuordnungen, Statistikimport,
Sortierung, Suchfilter und Netzwerkpakete aus. Ein vollständiger Spieltest der
Oberfläche auf einem Forge-Server mit echten Clients steht noch aus.

## Kurzer Spieltest

1. Server starten, beitreten und `Name [Nh]` im Tab prüfen.
2. Fünf Minuten auf derselben Blockposition stehen: AFK in Tab und über dem Kopf.
3. Block wechseln: AFK verschwindet innerhalb ungefähr einer Sekunde bei 20 TPS.
4. Mit bestehendem Scoreboard-Team wiederholen; das Team muss danach wieder gelten.
5. AFK ausloggen, neu beitreten und Server neu starten: kein hängender AFK-Status.
6. Nur GUI: Uhr-Button neben den FTB-Buttons anklicken: Das Fenster muss genauso groß wie
   das Inventar sein. Die sechs Item-Tabs durchschalten, suchen, Seiten wechseln
   und einen Spieler für dessen Detailansicht auswählen. GUI-Skalierung ändern
   und lange Spielernamen sowie große Werte prüfen.
7. Nur GUI: `/playtime` ohne OP testen, Offline-Spieler prüfen und bei einem zweiten Client
   Online-/AFK-Änderungen beobachten.

8. No GUI: Mit einem unveränderten 1.18.2-Client ohne Forge und ohne Tracker
   beitreten, Tab-Stunden und AFK prüfen. Im Inventar darf kein Tracker-Button
   vorhanden sein. Anschließend mit einem Forge-Client ohne Tracker wiederholen.

Quellgrundlagen: [Forge-Build-Anleitung](https://docs.minecraftforge.net/en/1.18.x/gettingstarted/)
und [Forge-Sides](https://docs.minecraftforge.net/en/1.18.x/concepts/sides/).
Funktionsvorbild: [FTB Utilities Leaderboards](https://github.com/FTBTeam/FTB-Utilities/blob/master/src/main/java/com/feed_the_beast/ftbutilities/data/Leaderboard.java).
