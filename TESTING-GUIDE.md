# How to test Diagonal Doors 0.4.0-alpha

You do not need to compile anything. Use the supplied **diagonaldoors-0.4.0-alpha.jar**.

## Quick retest after the 0.3.0 feedback

1. Close Minecraft, remove `diagonaldoors-0.3.0-alpha.jar`, and install only `diagonaldoors-0.4.0-alpha.jar`.
2. Reopen your existing disposable test world. You do not need to create another one.
3. Break and replace the double doors so the new automatic hinge-pairing logic runs. For a pair, place matching wooden doors on diagonal neighbors, looking the same direction; the second placement should put both hinges at the outer ends. Try starting at either end.
4. Open a door, then click near its hinge, center, and free/handle end to close it. Try both halves and both sides. You should no longer have to aim near the hinge.
5. Close the pair and inspect the meeting edges and handles. There should be no central gap, and the handles should be mirrored toward the meeting edges.
6. Look at the panels and windows: their main texture now stays at vanilla width, with extra length confined to narrow frame strips at the ends. Tell me whether that looks closer to what you intended.

The rest of this guide remains available for a full retest.

## 1. Install the new build

1. Close Minecraft completely.
2. Open the folder for the Minecraft instance you actually play. Your launcher's **Open Folder** or **Open Instance Folder** button is usually the easiest way to find it.
3. Confirm that instance uses **Minecraft 1.21.1** and **NeoForge 21.1.25**. This is not a Fabric or ordinary Forge mod.
4. Open its `mods` folder. Remove any older **Diagonal Doors** JAR from that folder, including `diagonaldoors-0.3.0-alpha.jar`. Keep only one version of this mod.
5. Copy **diagonaldoors-0.4.0-alpha.jar** into `mods`. Do not put the source ZIP or a `-sources.jar` into that folder.
6. For a first test, leave Fuzs's mods out, or use a separate test instance. You will add them for the compatibility test below.
7. Launch the instance. Check the Mods screen for **Diagonal Doors 0.4.0-alpha**.

If Minecraft crashes before the menu, stop here and send the instance's `logs/latest.log` and, if generated, the newest file in `crash-reports`. You do not need to change Java or Gradle to use the compiled mod.

## 2. Make a disposable test world

1. Choose **Singleplayer → Create New World**.
2. Call it `Diagonal Doors Test`. Select **Creative**, **Superflat**, and enable **Allow Commands** (called cheats in some menus).
3. Create the world. Find a flat patch with plenty of empty space above it.
4. Open the creative inventory and search for **Cherry Door**. Choose Minecraft's ordinary **Cherry Door**, not the separate item named **Diagonal Cherry Door**. Also take an ordinary Oak Door, stone blocks, and redstone blocks.

Use a fresh world for this alpha. Keep your existing survival world untouched during testing.

## 3. Test the new Shift placement

1. Hold the ordinary **Cherry Door**.
2. Without holding Shift, right-click the top of the ground. **Expected:** an ordinary straight vanilla cherry door appears.
3. Move a few blocks away. Hold your **Sneak** key (Shift by default), then right-click the top of the ground while still holding it. Release Shift afterward. **Expected:** a diagonal cherry door appears, using the same ordinary cherry-door item.
4. Stand back and inspect both doors. Each should have a bottom and top half, with the diagonal one sitting at 45 degrees to the block grid.
5. Repeat with an ordinary Oak Door. Normal placement should stay straight; Shift placement should be diagonal.

If you have rebound Sneak, use your Sneak key instead of Shift. Release Sneak before testing opening and closing, so you do not accidentally try to place another door.

## 4. Open, close, and walk against a door

1. Empty your hand or switch to a harmless item. Right-click the **lower half** of a diagonal door. **Expected:** it opens, both halves stay together, and one edge remains fixed as its hinge.
2. Right-click the **upper half**. **Expected:** the same door closes.
3. Repeat a few times and try from both sides.
4. With the door closed, walk directly into the visible panel. **Expected:** it blocks you along that diagonal panel.
5. Open it, then walk through the space it vacated. **Expected:** you can pass through the cleared space, but the visible open panel still blocks you if you walk into it.
6. Aim at the far end of the open panel, including the part extending into the next block space. **Expected:** it can be highlighted and right-clicked there.
7. Look at both halves from the front and back. Look for missing purple/black textures, wrong handle placement, a seam between halves, or a door that seems to slide rather than pivot.

Leave at least two empty blocks around the door while doing these checks. The open leaf extends into a neighboring cell; it can overlap a nearby wall if you build inside its swing space.

## 5. Try the other orientations and hinges

1. Place an ordinary oak door while sneaking and facing north. Press **F3** on Java Edition to see which direction you face.
2. Place three more, facing east, south, and west, leaving several blocks between them.
3. Open and close each one. Check that its panel, highlight, and collision all move together.
4. To try the other hinge, place two more doors facing the same direction, but click near opposite ends of the ground block's diagonal. The hinge is chosen near the end you click. Press F3 while aiming at a door to inspect its `hinge: left` or `hinge: right` property if you are unsure.
5. Repeat the open/close and walking checks on both hinges.

You do not need to test all 11 woods immediately. Start with cherry and oak; if they behave well, repeat with the remaining woods, especially bamboo's different-looking texture.

## 6. Build and test a diagonal double door

Both leaves need to use the **same wood** and occupy **diagonally adjacent ground blocks**. For the easiest setup, face the **same direction** for both placements; the new build also supports opposite facings on the same diagonal. Ordinary side-by-side blocks will not make this diagonal pair.

For a reproducible layout in your new test world, enter these commands one at a time in chat:

```mcfunction
/fill -8 64 -8 8 64 8 minecraft:stone
/setblock 0 64 0 minecraft:gold_block
/setblock 1 64 -1 minecraft:diamond_block
/tp @s 0.5 65 3.5 180 30
```

This creates a small platform, marks the two door positions, and faces you north. These commands replace blocks near those coordinates, so use them only in your disposable test world.

1. Hold an ordinary Cherry Door and stay facing generally **north**.
2. Hold Sneak and place the first door on the **gold block**. You do not need to select its hinge carefully.
3. Still facing north, hold Sneak and place the second cherry door on the **diamond block**. The gold-block door should now have `hinge: left`, and the diamond-block door `hinge: right`. You can inspect these properties using F3 while aiming at each door. Placing the second door corrects the first hinge if necessary.
4. Release Sneak. Right-click either door. **Expected:** both doors open together. Click the other one. **Expected:** both close together. Try upper and lower halves.
5. Place a redstone block immediately beside either door. **Expected:** both open. Remove it. **Expected:** both close.
6. Power both doors with separate redstone blocks, then remove only one supply. **Expected:** the pair stays open. Remove the other supply. **Expected:** both close.
7. Break one leaf and operate the remaining door. It should still work by itself.

If the pair fails, note the targeted block's `facing`, `hinge`, `open`, and `powered` properties shown by F3. A screenshot of those properties is particularly useful.

## 7. Check item counts and breaking

1. In your disposable world, use `/gamemode survival`. Give yourself a small stack of ordinary cherry doors first if needed: `/give @s minecraft:cherry_door 4`.
2. Note the count in your selected hotbar slot. Hold Sneak and place one door. **Expected:** the stack decreases by exactly one, and both halves appear.
3. Try placing under a solid block with only one block of height available. **Expected:** placement fails, no half-door remains, and your item count stays the same.
4. Break a successfully placed diagonal door from the top half, then try another from the bottom half. **Expected:** each complete door drops exactly **one diagonal door item**, with no orphaned half.
5. You can craft the dropped diagonal item back into an ordinary vanilla door using the retained one-item conversion recipe. Separate diagonal items always place diagonally; Sneak is needed when placing the ordinary vanilla item.
6. Return to Creative with `/gamemode creative`. Placing should no longer consume an item. Creative breaking should not drop a door.

## 8. Add Fuzs's mods and repeat a short test

1. Save and quit, then close Minecraft.
2. Add the **NeoForge / Minecraft 1.21.1** versions of Diagonal Windows, Diagonal Walls, Diagonal Fences, and Puzzles Lib to the same test instance. The exact combination tested automatically was Windows **21.1.1**, Walls **21.1.2**, Fences **21.1.1**, and Puzzles Lib **21.1.65**. Diagonal Blocks is embedded in those mods; no extra download was needed for this combination.
3. Relaunch and reopen your test world.
4. Repeat the cherry Shift-placement, open/close, walking, and double-door checks.
5. Build some diagonal windows, walls, and fences near your doors using those mods' normal placement behavior. Inspect gaps, intersections, lighting, and collision. Do not assume their connection system automatically reshapes itself around the doors; visual joins still need checking.
6. Save, quit to the title screen, and reopen the world. **Expected:** door positions, hinges, and open/closed states survive reloading.

## 9. Tell me what happened

Send a short result like this:

```text
Build: 0.4.0-alpha
Minecraft / NeoForge: 1.21.1 / 21.1.25
Other mods: [names, or none]
Normal placement: pass / problem
Sneak placement: pass / problem
Opening and collision: pass / problem
Double doors and redstone: pass / problem
Appearance: pass / problem
Steps to reproduce a problem: ...
```

For a visual issue, include a screenshot or short video and the wood type, facing, hinge, and whether the door was open. For a crash, include `latest.log` and the crash report. Multiplayer, simultaneous clicks by two players, and a dedicated server remain separate tests; install this same JAR on both client and server when you try them.
