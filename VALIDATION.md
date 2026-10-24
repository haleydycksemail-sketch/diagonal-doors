# Validation report — Diagonal Doors 0.4.0-alpha

Target: Minecraft **1.21.1**, NeoForge **21.1.25**, Java **21**, Gradle **8.14.3**.

## Changes prompted by in-game feedback

The player confirmed that Shift placement worked for every vanilla wooden door, but reported unreliable closing away from the hinge, gaps in double doors, unmirrored paired handles, and stretched-looking door artwork.

- **Closing:** Minecraft's server rejects ordinary block-use packets when the reported hit point lies more than one block from the owning block's center. Parts of an open diagonal leaf exceed that bound. The old tests covered picking and block callbacks but missed the packet handler. A new test invoked the actual handler and reproduced its `Rejecting UseItemOnPacket ... too far away from hit block` warning on the previous behavior. The fix narrowly extends that coordinate bound for a point on an actual diagonal leaf, after checking reach and ray occlusion. Vanilla block-use permission, border, game-mode, item, and block-distance checks still execute. Other blocks retain their original bound.
- **Meeting edges:** the hinge remains inset for swing clearance, but each free edge now reaches its shared grid corner. A paired doorway's free edges coincide exactly instead of both stopping short.
- **Pairing and handles:** placing the second matching door corrects both hinges to the outer ends, even if the first hinge was chosen incorrectly. Both placement orders and same/opposite facings on the same diagonal are supported. An already valid pair is not reassigned to a third leaf. The UV handle edge faces the free end on both faces in every model state.
- **Texture proportions:** the central 16-unit vanilla door artwork keeps its original scale. Extra length is supplied by narrow frame extensions at the ends instead of stretching panels, windows, and handles across the entire diagonal width. Extensions avoid sampling the handle pixels, preventing duplicated knobs.

Shift placement, vanilla placement without Sneak, conversion recipes, wood registrations, and existing block IDs remain available. Existing test worlds can be reopened. Break and replace old double doors to exercise the new placement-time hinge correction.

## Executed checks

- `./gradlew build runGameTestServer -Pcompat`: compiled and packaged the mod; **three JUnit tests passed** with zero failures/skips; **all thirteen required Minecraft GameTests passed**.
- The closing regression exercises **64 wire-encoded/decoded block-use packets through Minecraft's actual server handler**: four directions, two hinges, two vertical halves, and four positions along the leaf. Each click closes the door. It uses a real packet handler with outgoing packets suppressed, rather than FakePlayer's no-op packet override.
- The extended-hit validator rejects off-leaf points, hits behind an intervening solid block, and out-of-reach hits.
- A pairing regression exercises **16 layouts**: four directions, two placement orders, and same/opposite facings. Each starts with a deliberately wrong first hinge and checks correction plus opening/closing both leaves.
- The ten retained GameTests cover all eleven vanilla wooden door items under normal/sneaking placement; correct upper/lower blocks; survival consumption of one item; creative/offhand/obstructed placement; both halves' open state; collision queried from adjacent cells; selection of the extended leaf; paired redstone and shared power; support removal; structure mirror hinges; correct loot and vanilla tags; and removing a powered mate.
- The geometry unit tests cover stationary hinges and 90-degree swings, continuous conservative collision coverage, bounded swing geometry, and **exact coincidence of paired free edges in all four directions**.
- `./gradlew runClient -Pcompat -PmodelSmoke`: a real Minecraft client started under Xvfb / Mesa software OpenGL and completed successfully. The current models passed **704 states**, fourteen external quads per state, existing textures, rendered X/Z bounds matching collision, original 16-unit front/back artwork width, and handle UV direction toward the free edge. It logged `DIAGONAL_DOORS_MODEL_CHECK_PASSED: 704 states`.

The compatibility runs load Diagonal Windows **21.1.1**, Diagonal Walls **21.1.2**, Diagonal Fences **21.1.1**, Puzzles Lib **21.1.65**, and the selected embedded Diagonal Blocks library. These downloads are pinned and SHA-512 verified by `tools/compat-mods.lock.json`. Third-party mods are not bundled in the installable JAR. Development GameTests, anonymous test handlers, their structure template, and model diagnostics are excluded from that JAR.

## Manual retest still needed

Follow the quick retest at the top of **TESTING-GUIDE.md**. Test clicking along the whole open leaf, inspect paired meeting edges/handles, and judge the new end-extension appearance. The player's earlier in-game results apply to 0.3.0; human acceptance of the new 0.4.0 behavior is still pending.

This is an **alpha**. Collision remains a conservative 64-segment approximation, and swing space is not reserved: leaves can overlap nearby blocks. Co-loading Fuzs's mods is verified, but custom Diagonal Blocks conversion-pipeline integration and perfect visual joins are not claimed. Iron/copper doors and modded wood types are outside this version's scope. Dedicated multiplayer and simultaneous player interactions remain manual checks. The cloud machine's optional authentication/audio warnings do not prevent local server tests or client model initialization; neither authenticated multiplayer nor audio quality is claimed as tested.
