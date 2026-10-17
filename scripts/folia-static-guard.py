#!/usr/bin/env python3
"""Static checks for known unsafe scheduler paths in ItemTag source."""
from pathlib import Path
import sys

root = Path(__file__).resolve().parents[1]
source = root / "src/main/java"
errors = []

for path in source.rglob("*.java"):
    text = path.read_text(encoding="utf-8")
    if path.name != "TaskScheduler.java":
        for token in ("runTaskLater(", "runTaskTimer(", "runTask(", "runTaskAsynchronously(", "BukkitRunnable"):
            if token in text:
                errors.append(f"{path.relative_to(root)}: direct legacy scheduler usage: {token}")

equipment = (source / "emanondev/itemtag/equipmentchange/EquipmentChangeListenerBase.java").read_text(encoding="utf-8")
if "Bukkit.getOnlinePlayers()" in equipment:
    errors.append("equipment listener must not scan all online players")
if "repeatAtEntity" not in equipment:
    errors.append("equipment polling must use entity-owned repeating tasks")

plugin_yml = (root / "src/main/resources/plugin.yml").read_text(encoding="utf-8")
if "folia-supported: true" in plugin_yml:
    errors.append("do not advertise Folia support until full audit is complete")

if errors:
    print("\n".join(errors), file=sys.stderr)
    sys.exit(1)
print("Folia static guard passed")
