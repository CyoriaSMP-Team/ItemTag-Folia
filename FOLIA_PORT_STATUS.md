# Folia port status

This branch targets the Spigot API 1.21.11 and keeps the existing plugin configuration, storage format, and dependency declarations. The `folia-supported` metadata declaration has been removed. This repository has not completed a whole-plugin Folia audit, and this build must not be advertised as Folia compatible yet.

## Changes in this port

- Equipment inventory polling is now one repeating entity-scheduler callback per tracked player. It reads that player's equipment only on the player's entity scheduler; it no longer takes a cross-player polling snapshot or reads online players in a periodic/global task.
- Deferred equipment slot checks, delayed player actions, respawn and potion-effect work, and delayed player inventory callbacks use the entity scheduler on Folia. `TaskScheduler` detects the Folia scheduler API reflectively and falls back to Bukkit's scheduler on Paper, avoiding a compile-time Folia API dependency.
- The equipment listener's cross-region player/task registries use concurrent collections. Per-player equipment snapshots are still only accessed by that player's event and entity-scheduler callbacks.
- Added a static guard for known unsafe scheduler calls and a GitHub Actions Maven verification job on Java 25.

## Remaining audit blockers

- This is not a full region-thread-safety audit. Other mutable registries and caches across activity, trigger, action, command, and configuration code still use ordinary `HashMap`/`HashSet` instances. Their access paths have not been proven to remain on one owning region, especially for asynchronous plugin events and actions affecting multiple players.
- ItemTag depends on ItemEdit, and its listeners, storage APIs, permission/cooldown APIs, PlaceholderAPI, Vault, NBT API, and optional hooks are outside this port's source audit. Their Folia safety and scheduler choices remain unverified.
- Command and GUI paths, event handlers that touch multiple entities or blocks, custom events dispatched by ItemTag, and lifecycle/reload behavior need review and live Folia testing. The static guard only protects the specific scheduler patterns listed in the script; it does not prove thread ownership or detect all unsafe Bukkit access.
- `TaskScheduler`'s reflective Folia API bridge and entity task cancellation behavior need validation against Folia 1.21.11 at runtime. The local Home Server build was intentionally not run.
- The listener tracks players when their join event is observed. Reload/lifecycle behavior with already-online players and server/plugin manager reloads needs a Folia integration test.

## Validation

Run `python3 scripts/folia-static-guard.py` locally for the static check. GitHub Actions runs that check and `mvn --batch-mode --no-transfer-progress verify` using Java 25. This port has not yet been validated by that remote workflow or by enabling it on a Folia server.

## GitHub CI and controlled runtime smoke test (2026-10-17)

- GitHub Actions push run `39042495807` and PR run `39042498987` passed `mvn verify` and published the `itemtag-folia-development-5` artifact.
- The artifact **does not** declare Folia support. For a disposable localhost-only test, an isolated copy was temporarily marked `folia-supported: true`; that test-only JAR is **not published or installed in production**.
- Disposable Folia 1.21.11-14 test instance with official ItemEdit 3.7.10 loaded the development ItemTag build and logged `ItemTag # Enabled (took 113 ms)` and `Done` with no new ItemTag startup scheduler exception.
- No player joined; per-player entity scheduler logic, item changes, world travel and multi-region concurrency still need live functional tests. The temporary flag is deliberately absent from published artifacts.
