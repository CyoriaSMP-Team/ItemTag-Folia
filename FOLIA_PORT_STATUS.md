# Folia port status

> **Superseded 2026-11-24 — do not deploy this 3.7.7 fork with ItemEdit 4.x.**
> ItemEdit 4.0.0 changed `APlugin.log` to MiniMessage and rejects the legacy `§c` colour code used by the inherited 3.7.7 error path. ItemTag 3.7.7 therefore fails during `onEnable`; the real exception is masked by the logging crash. Official **ItemTag 4.0.1** (2026-11-09) is Folia-supported and is built against **ItemEdit 4.0.0**. The live CyoriaSMP server now runs `ItemTag-4.0.1.jar` (SHA-256 `8582f9718366ac0b8301cfdd64802c43bf11252c4bb34912f6e3841ebe7bb01e`), enabled in 46 ms with no ItemTag errors. The old `ItemTag-3.7.7.jar` (SHA-256 `00c2448cf356afa810c901980c4202dd051e157f1eb2f8150b4d5bfc40a429e9`) is retained at `plugins/.plugin-rollback/itemtag-3.7.7-20261124/`. This fork is kept for history; upstream 4.x is the supported deployment path.

This branch targets the Spigot API 1.21.11 and keeps the existing plugin configuration, storage format, and dependency declarations. `folia-supported: true` is enabled so Folia can load the port for the requested iterative live testing; it is not a claim that the whole plugin is safe. The source and runtime audit continues against the concrete blockers below.

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

Run `python3 scripts/folia-static-guard.py` locally for the static check. GitHub Actions runs that check and `mvn --batch-mode --no-transfer-progress verify` using Java 25. The workflow passes for the current development branch. A controlled startup smoke test on Folia 1.21.11 also succeeded, but no player-facing behavior or multi-region safety has been validated.

## GitHub CI and controlled runtime smoke test (2026-10-17)

- GitHub Actions push run `39042495807` and PR run `39042498987` passed `mvn verify` and published the `itemtag-folia-development-5` artifact.
- The artifact **does not** declare Folia support. For a disposable localhost-only test, an isolated copy was temporarily marked `folia-supported: true`; that test-only JAR is **not published or installed in production**.
- Disposable Folia 1.21.11-14 test instance with official ItemEdit 3.7.10 loaded the development ItemTag build and logged `ItemTag # Enabled (took 113 ms)` and `Done` with no new ItemTag startup scheduler exception.
- No player joined; per-player entity scheduler logic, item changes, world travel and multi-region concurrency still need live functional tests. The temporary flag is deliberately absent from published artifacts.

## Controlled production startup smoke test (2026-10-17)

- GitHub Actions push run `39042757007` and PR run `39042754248` passed; the test used the `itemtag-folia-development-8` artifact.
- Before the live check, the original ItemTag JAR and its configuration were backed up. A copy of the development artifact with only `folia-supported: true` added enabled on the Folia 1.21.11 server and logged `ItemTag # Enabled (took 33 ms)`; the server then reached `Done`.
- The original JAR was restored after this startup-only smoke test. The branch now carries `folia-supported: true` for the requested iterative Folia testing; this metadata does not certify the remaining audit paths.
- No player joined, and item use, anvil interactions, world travel, and multi-region concurrency remain unverified. This result does not complete the Folia safety audit.

## Iterative live deployment (2026-10-17)

- Commit `ddc6420227890d7eb918ae0c8e9182bb9bb19171` passed push and pull-request CI run `39047989424`; its `itemtag-folia-development-12` artifact contains `folia-supported: true` and has SHA-256 `22cb926d56049f251547d8f552f920bd89964b24214c473acd3f3c6afb0467a5`.
- That exact artifact is now installed on CyoriaSMP Folia 1.21.11. Startup logged `ItemTag # Enabled (took 34 ms)` and the server reached `Done`; the original JAR remains in the rollback directory.
- The server had 0 players during deployment. This confirms startup only; item behavior, player scheduling, GUI interactions and multi-region safety are still unverified.
