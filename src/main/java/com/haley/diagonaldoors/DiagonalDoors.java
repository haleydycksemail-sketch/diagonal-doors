package com.haley.diagonaldoors;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import java.util.LinkedHashMap;
import java.util.Map;

@Mod(DiagonalDoors.MOD_ID)
public final class DiagonalDoors {
    public static final String MOD_ID = "diagonaldoors";
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    private static final Map<String, DeferredItem<BlockItem>> DOOR_ITEMS = new LinkedHashMap<>();

    private static final Map<Block, DeferredItem<BlockItem>> VANILLA_DOORS = new LinkedHashMap<>();

    static {
        register("oak", Blocks.OAK_DOOR, BlockSetType.OAK);
        register("spruce", Blocks.SPRUCE_DOOR, BlockSetType.SPRUCE);
        register("birch", Blocks.BIRCH_DOOR, BlockSetType.BIRCH);
        register("jungle", Blocks.JUNGLE_DOOR, BlockSetType.JUNGLE);
        register("acacia", Blocks.ACACIA_DOOR, BlockSetType.ACACIA);
        register("dark_oak", Blocks.DARK_OAK_DOOR, BlockSetType.DARK_OAK);
        register("mangrove", Blocks.MANGROVE_DOOR, BlockSetType.MANGROVE);
        register("cherry", Blocks.CHERRY_DOOR, BlockSetType.CHERRY);
        register("bamboo", Blocks.BAMBOO_DOOR, BlockSetType.BAMBOO);
        register("crimson", Blocks.CRIMSON_DOOR, BlockSetType.CRIMSON);
        register("warped", Blocks.WARPED_DOOR, BlockSetType.WARPED);
    }

    private static void register(String wood, Block vanilla, BlockSetType type) {
        String id = "diagonal_" + wood + "_door";
        DeferredBlock<DoorBlock> block = BLOCKS.register(id, () -> new AngledDoorBlock(type, BlockBehaviour.Properties.ofFullCopy(vanilla).noOcclusion().dynamicShape()));
        DeferredItem<BlockItem> item = ITEMS.register(id, () -> new DoubleHighBlockItem(block.get(), new Item.Properties()));
        DOOR_ITEMS.put(id, item);
        VANILLA_DOORS.put(vanilla, item);
    }

    public static BlockItem diagonalItemFor(Block vanilla) {
        var item = VANILLA_DOORS.get(vanilla);
        return item == null ? null : item.get();
    }

    public DiagonalDoors(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(this::addCreativeTab);
    }

    private void addCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            DOOR_ITEMS.values().forEach(item -> event.accept(item.get()));
        }
    }
}
