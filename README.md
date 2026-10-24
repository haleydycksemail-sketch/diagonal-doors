# Diagonal Doors

A standalone NeoForge mod for **Minecraft 1.21.1**, built and tested against **NeoForge 21.1.25** and **Java 21**. Version **0.4.0-alpha** is an installable development build; hands-on world testing remains necessary before calling it a finished release.

**[Download the installable JAR](https://github.com/haleydycksemail-sketch/diagonal-doors/raw/refs/heads/main/releases/diagonaldoors-0.4.0-alpha.jar)**

## Install and use

Put `diagonaldoors-0.4.0-alpha.jar` in your instance's `mods` directory. Install it on both the client and server for multiplayer. Use Minecraft 1.21.1 and NeoForge 21.1.25. No Fuzs mod is required.

**Hold Sneak (Shift by default) while placing an ordinary vanilla wooden door to place its diagonal version.** Place without Sneak for a normal vanilla door. Release Sneak before opening or closing the placed door. The old one-item conversion recipes remain available; you can craft a diagonal door back into a vanilla door. The creative Building Blocks tab also contains all 11 variants: oak, spruce, birch, jungle, acacia, dark oak, mangrove, cherry, bamboo, crimson, and warped. Existing vanilla doors are not replaced automatically. Iron, copper, and modded wood types are outside this version's scope.

A closed door spans its cell diagonally. The click location selects the hinge near the closest diagonal endpoint. Right-click either half to open or close it. Opening rotates the leaf 90 degrees around its stationary hinge and extends it into an adjacent cell, so leave room for the swing. Collision and selection follow that leaf; the selection fix also covers the portion outside its owning cell. The central 16-unit vanilla texture keeps its normal scale; narrow frame strips at the ends supply the extra diagonal length. The free edges of a pair meet at their shared corner. Minecraft collision uses a continuous, conservative staircase of 64 cuboids, with less than 0.22 model-unit approximation error.

For double doors, use **the same wood** in two diagonally adjacent cells along the closed leaves. Face the same direction or opposite directions along the same diagonal. Placing the second door automatically sets both hinges to the outer ends, regardless of which end you placed first. Already-paired doors are not reassigned to a third leaf. For example, when both doors face north, a left-hinged door at `(x,y,z)` pairs with a right-hinged door at `(x+1,y,z-1)`. Click either door to operate both. Redstone at either half of either door controls the pair; it remains powered until both sides lose power. Cardinal side-by-side doors are not diagonal pairs.

## Build and test

The repository contains a Gradle **8.14.3** wrapper with distribution checksum verification. Install a full **JDK 21**, then run:

```sh
./gradlew build
./gradlew runGameTestServer
./gradlew runClient
```

On Windows, use `gradlew.bat`. JARs are written to `build/libs`; the `-sources.jar` is source code, not the installable mod. Development GameTests and model diagnostics are excluded from the installable JAR.

For the tested Fuzs combination:

```sh
python3 tools/fetch_compat_mods.py
./gradlew runGameTestServer -Pcompat
./gradlew runClient -Pcompat
```

The fetch script verifies SHA-512 hashes and installs only into the ignored development folder `run/compat-mods`. Third-party mods are not bundled or redistributed with this project. Exact versions and download hashes are in `tools/compat-mods.lock.json`:

- Diagonal Windows 21.1.1
- Diagonal Walls 21.1.2
- Diagonal Fences 21.1.1
- Puzzles Lib 21.1.65
- Diagonal Blocks 21.1.2, selected from the mods' embedded libraries by NeoForge

These mods coexist with Diagonal Doors. This project does not replace Fuzs's blocks or register doors in its Diagonal Blocks conversion pipeline. Visual joins between doors and those blocks still need hands-on inspection.

`runClient` enables development model validation. It checks every wood, facing, hinge, open/closed state, half, and power state against the actual baked models and textures. `-PmodelSmoke` exits the client after this check. A graphical display or Xvfb is required. Successful output contains `DIAGONAL_DOORS_MODEL_CHECK_PASSED: 704 states`.

For this Codex cloud snapshot, source `tools/cloud-env.sh` before Gradle commands. It selects the retained JDK and configures Java to use the current environment proxy without storing credentials. Each cloud task is already isolated: use the existing checkout instead of creating a worktree.

## Validation and remaining work

Start with **[TESTING-GUIDE.md](TESTING-GUIDE.md)** for step-by-step installation and in-game testing. See `VALIDATION.md` for executed automated checks and additional acceptance criteria. Model/collision bounds are verified automatically, but automated startup does not establish that the doors look and feel correct while playing. Test in a new creative world before using the alpha in an existing world.

The model generator is `tools/generate_assets.py`. It uses the same panel dimensions and transforms as `DoorGeometry.java`; if dimensions change, update both, regenerate assets, and rerun geometry, server, and client model checks.

## License

MIT. Vanilla door textures are referenced from the installed game and are not redistributed. Third-party compatibility mods retain their own licenses.
