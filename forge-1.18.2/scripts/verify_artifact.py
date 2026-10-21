"""Verify the installable JAR and source archive without launching Minecraft."""
import argparse
import hashlib
import json
from pathlib import Path
import struct
import tomllib
from zipfile import ZipFile


def verify(jar_path, sources_path, variant, version):
    prefix = "dev/zoffmann/playtimetracker/forge/"
    with ZipFile(jar_path) as jar:
        names = jar.namelist()
        if len(names) != len(set(names)):
            raise ValueError("duplicate JAR entries")
        metadata = tomllib.loads(jar.read("META-INF/mods.toml").decode())
        mod = metadata["mods"][0]
        assert mod["modId"] == "playtimetracker" and mod["version"] == version
        deps = metadata["dependencies"]["playtimetracker"]
        assert any(d["modId"] == "minecraft" and d["versionRange"] == "[1.18.2]" for d in deps)
        assert any(d["modId"] == "forge" and d["versionRange"] == "[40.3.11,41)" for d in deps)
        assert prefix + "ServerTracking.class" in names
        assert prefix + "AfkTeams.class" in names and prefix + "TabDisplayManager.class" in names
        for name in names:
            assert not name.startswith(("/", "net/minecraft/", "net/minecraftforge/", "net/fabricmc/")), name
            assert ".." not in Path(name).parts, name
            assert not name.endswith(("Test.class", "fabric.mod.json", ".env", ".sqlite3", ".log")), name
            if name.endswith(".class"):
                data = jar.read(name)
                assert struct.unpack(">H", data[6:8])[0] == 61, name
                assert b"net/fabricmc/" not in data, name
                if variant == "nogui":
                    assert b"net/minecraft/client/" not in data, name
                    assert b"net/minecraftforge/client/" not in data, name
                    assert b"playtimetracker/forge/leaderboard/" not in data, name
        if variant == "nogui":
            assert prefix + "NoGuiPlaytimeTrackerMod.class" in names
            assert prefix + "PlaytimeTrackerMod.class" not in names
            assert not any(name.startswith((prefix + "client/", prefix + "leaderboard/", "assets/")) for name in names)
            entrypoint = jar.read(prefix + "NoGuiPlaytimeTrackerMod.class")
            # Forge 40.3.11 inlines IGNORESERVERONLY as its "OHNOES..." marker.
            assert b"OHNOES" in entrypoint and b"registerExtensionPoint" in entrypoint
            assert b"IExtensionPoint$DisplayTest" in entrypoint
            assert all(d["side"] == "SERVER" for d in deps)
        else:
            assert prefix + "NoGuiPlaytimeTrackerMod.class" not in names
            assert prefix + "client/LeaderboardScreen$MetricTab.class" in names
            assert prefix + "leaderboard/TrackerNetwork.class" in names
            assert all(d["side"] == "BOTH" for d in deps)
            sidebar = json.loads(jar.read("assets/playtimetracker/sidebar_buttons.json"))["leaderboards"]
            assert sidebar["click"] == "command:/playtime"
            de = json.loads(jar.read("assets/playtimetracker/lang/de_de.json"))
            en = json.loads(jar.read("assets/playtimetracker/lang/en_us.json"))
            assert de.keys() == en.keys()
        # The source JAR is deliberately separate from the reobfuscated runtime.
        assert b"getPlayerList" not in jar.read(prefix + "ServerTracking.class")
    with ZipFile(sources_path) as sources:
        names = sources.namelist()
        assert prefix + "ServerTracking.java" in names
        assert not any(name.endswith(".class") for name in names)
        if variant == "nogui":
            assert not any(name.startswith((prefix + "client/", prefix + "leaderboard/")) for name in names)
    for path in (jar_path, sources_path):
        digest = hashlib.sha256(path.read_bytes()).hexdigest()
        path.with_suffix(path.suffix + ".sha256").write_text(f"{digest}  {path.name}\n")
    print(f"Verified {variant}: Minecraft 1.18.2, Java 17, v{version}; checksums written.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", type=Path, required=True)
    parser.add_argument("--sources", type=Path, required=True)
    parser.add_argument("--variant", choices=("gui", "nogui"), required=True)
    parser.add_argument("--version", required=True)
    args = parser.parse_args()
    verify(args.jar, args.sources, args.variant, args.version)
