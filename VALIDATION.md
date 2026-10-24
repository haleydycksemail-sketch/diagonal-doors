# Validation report — Diagonal Doors 0.3.0-alpha

Target: Minecraft **1.21.1**, NeoForge **21.1.25**, Java **21**, Gradle **8.14.3**.

## New placement behavior

Hold Sneak (Shift by default) while placing an ordinary vanilla wooden door to place the matching diagonal door. Without Sneak, it places the original vanilla door. All 11 vanilla wood types, including cherry, are covered. Iron and copper are not converted. Existing diagonal items and one-item conversion recipes remain available.

Placement delegates to the diagonal door's normal BlockItem placement using the original held stack and context. This preserves two-block checks, the selected hand, permissions, sounds, and survival/creative consumption. There is no separate network packet or client-only block replacement. The placement hook runs on both sides.

## Executed checks

- `./gradlew build runGameTestServer -Pcompat`: compilation and packaging succeeded. **Three JUnit geometry tests passed**, with zero failures or skips. **All ten required Minecraft GameTests passed** with Diagonal Windows 21.1.1, Diagonal Walls 21.1.2, Diagonal Fences 21.1.1, Puzzles Lib 21.1.65, and their embedded Diagonal Blocks library loaded.
- The two new GameTests exercise normal and sneaking placement for all 11 vanilla wooden door items, correct upper/lower blocks, consumption of exactly one vanilla door in survival, successful offhand placement, no consumption in creative, and failed obstructed placement without consuming an item or leaving an orphaned half.
- The eight retained GameTests cover all 11 wood registrations across four facings and two hinges; upper/lower synchronization; changed open collision; cherry door item placement and diagonal pairing; clicking upper/lower halves to operate both doors; redstone on either door/half; keeping shared power until both supplies disappear; support removal; one lower-half drop and no upper-half duplicate; vanilla wooden-door and axe tags; actual collision queried from adjacent cells; structure mirror hinge transforms; selecting an extended leaf through the Entity.pick mixin; and clearing shared power when a powered mate is removed.
- `./gradlew runClient -Pcompat -PmodelSmoke`: a real Minecraft client started successfully under Xvfb / Mesa software OpenGL, with the compatibility mods loaded. The current build logged **DIAGONAL_DOORS_MODEL_CHECK_PASSED: 704 states** and exited successfully. All states had six faces, present textures, and rendered X/Z bounds aligned with collision geometry. This also verifies that the new placement mixin loads on the client.
- Geometry unit tests exercise fixed hinges, 90-degree swings, continuous conservative collision coverage, collision bounds, and paired-leaf alignment across all facing/hinge/open combinations.

The compatibility mod downloads are pinned in `tools/compat-mods.lock.json` and SHA-512 verified. They are development dependencies and are not bundled in this mod.

## Remaining manual testing

Follow **TESTING-GUIDE.md** for installation and step-by-step in-game checks. No person has played this build in a Minecraft world during this task. Automated resource initialization is not a substitute for visually checking hinges, handles, texture alignment, lighting, movement through doorways, or connections to Fuzs blocks. Human mouse/keyboard Shift placement and multiplayer synchronization still require in-game acceptance testing, even though the underlying placement pipeline passes the server tests.

This remains an **alpha**. The collision is a conservative 64-segment approximation of a diagonal leaf. Opening extends into a neighboring cell; swing space is not reserved and a leaf can overlap nearby blocks. No custom integration with Fuzs's Diagonal Blocks conversion pipeline is claimed. Iron/copper doors and modded wood types are not implemented.

Minecraft's optional authentication/public-key lookups can report network errors in this cloud machine. Authenticated multiplayer connectivity has not been tested. Install this same mod version on both the client and server when testing multiplayer.
