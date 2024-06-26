package it.feargames.tileculling.util;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedLevelChunkData;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainer.Strategy;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.craftbukkit.v1_20_R3.CraftChunk;
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R3.block.CraftBlockEntityState;
import org.bukkit.craftbukkit.v1_20_R3.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

public final class NMSUtils {

    private final Constructor<ClientboundBlockEntityDataPacket> BLOCK_ENTITY_DATA_PACKET_CONSTRUCTOR;
    private final BlockState AIR_BLOCK;
    private final PalettedContainer<BlockState> EMPTY_BLOCK_IDS;

    public NMSUtils() {
        try {
            BLOCK_ENTITY_DATA_PACKET_CONSTRUCTOR = ClientboundBlockEntityDataPacket.class.getDeclaredConstructor(
                    BlockPos.class, BlockEntityType.class, CompoundTag.class);
            BLOCK_ENTITY_DATA_PACKET_CONSTRUCTOR.setAccessible(true);

            AIR_BLOCK = Blocks.AIR.defaultBlockState();

            EMPTY_BLOCK_IDS = new PalettedContainer<>(Block.BLOCK_STATE_REGISTRY, AIR_BLOCK, Strategy.SECTION_STATES, null);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateBlockState(Player player, Location location, BlockData blockData) {
        CraftPlayer craftPlayer = (CraftPlayer) player;
        ServerPlayer handlePlayer = craftPlayer.getHandle();
        ServerPlayerConnection connection = handlePlayer.connection;

        if (connection == null) {
            return;
        }

        BlockPos blockPosition = new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        BlockState handleData = blockData == null ? AIR_BLOCK : ((CraftBlockData) blockData).getState();
        ClientboundBlockUpdatePacket blockChange = new ClientboundBlockUpdatePacket(blockPosition, handleData);
        connection.send(blockChange);
    }

    public void updateBlockData(Player player, Location location, org.bukkit.block.BlockState block) {
        CraftPlayer craftPlayer = (CraftPlayer) player;
        ServerPlayer handlePlayer = craftPlayer.getHandle();
        ServerPlayerConnection connection = handlePlayer.connection;

        if (connection == null) {
            return;
        }

        BlockEntityType<?> type;
        if (block instanceof Smoker) {
            type = BlockEntityType.SMOKER;
        } else if (block instanceof BlastFurnace) {
            type = BlockEntityType.BLAST_FURNACE;
        } else if (block instanceof Furnace) {
            type = BlockEntityType.FURNACE;
        } else if (block instanceof Chest) {
            type = block.getType() == Material.TRAPPED_CHEST ? BlockEntityType.TRAPPED_CHEST : BlockEntityType.CHEST; // FIXME
        } else if (block instanceof EnderChest) {
            type = BlockEntityType.ENDER_CHEST;
        } else if (block instanceof Jukebox) {
            type = BlockEntityType.JUKEBOX;
        } else if (block instanceof Dispenser) {
            type = BlockEntityType.DISPENSER;
        } else if (block instanceof Dropper) {
            type = BlockEntityType.DROPPER;
        } else if (block instanceof Sign) {
            type = BlockEntityType.SIGN;
        } else if (block instanceof CreatureSpawner) {
            type = BlockEntityType.MOB_SPAWNER;
        } else if (block instanceof BrewingStand) {
            type = BlockEntityType.BREWING_STAND;
        } else if (block instanceof EnchantingTable) {
            type = BlockEntityType.ENCHANTING_TABLE;
        } else if (block instanceof Beacon) {
            type = BlockEntityType.BEACON;
        } else if (block instanceof Skull) {
            type = BlockEntityType.SKULL;
        } else if (block instanceof DaylightDetector) {
            type = BlockEntityType.DAYLIGHT_DETECTOR;
        } else if (block instanceof Hopper) {
            type = BlockEntityType.HOPPER;
        } else if (block instanceof Comparator) {
            type = BlockEntityType.COMPARATOR;
        } else if (block instanceof Banner) {
            type = BlockEntityType.BANNER;
        } else if (block instanceof Structure) {
            type = BlockEntityType.STRUCTURE_BLOCK;
        } else if (block instanceof EndGateway) {
            type = BlockEntityType.END_GATEWAY;
        } else if (block instanceof CommandBlock) {
            type = BlockEntityType.COMMAND_BLOCK;
        } else if (block instanceof ShulkerBox) {
            type = BlockEntityType.SHULKER_BOX;
        } else if (block instanceof Bed) {
            type = BlockEntityType.BED;
        } else if (block instanceof Conduit) {
            type = BlockEntityType.CONDUIT;
        } else if (block instanceof Barrel) {
            type = BlockEntityType.BARREL;
        } else if (block instanceof Lectern) {
            type = BlockEntityType.LECTERN;
        } else if (block instanceof Bell) {
            type = BlockEntityType.BELL;
        } else if (block instanceof Jigsaw) {
            type = BlockEntityType.JIGSAW;
        } else if (block instanceof Campfire) {
            type = BlockEntityType.CAMPFIRE;
        } else if (block instanceof Beehive) {
            type = BlockEntityType.BEEHIVE;
        } else if (block instanceof SculkSensor) {
            type = BlockEntityType.SCULK_SENSOR;
        } else if (block instanceof SculkCatalyst) {
            type = BlockEntityType.SCULK_CATALYST;
        } else if (block instanceof SculkShrieker) {
            type = BlockEntityType.SCULK_SHRIEKER;
        } else if (block instanceof BrushableBlock) {
            type = BlockEntityType.BRUSHABLE_BLOCK;
        } else {
            return;
        }

        CraftBlockEntityState<?> craftBlockEntityState = (CraftBlockEntityState<?>) block;
        CompoundTag nbt = craftBlockEntityState.getSnapshotNBT();
        BlockPos blockPosition = new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        ClientboundBlockEntityDataPacket packet;

        try {
            packet = BLOCK_ENTITY_DATA_PACKET_CONSTRUCTOR.newInstance(blockPosition, type, nbt);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        connection.send(packet);
    }

    public void transformPacket(Player player, PacketContainer container, int chunkX, int chunkZ, Function<String, Boolean> tileEntityTypeFilter) {
        CraftWorld craftWorld = (CraftWorld) player.getWorld();
        ServerLevel vanillaWorld = craftWorld.getHandle();

        if (vanillaWorld.getMaxBuildHeight() != 320) {
            return;
        }

        WrappedLevelChunkData.ChunkData data = container.getLevelChunkData().read(0);

        List<WrappedLevelChunkData.BlockEntityInfo> blockTileEntities = data.getBlockEntityInfo();

        IntSet removedBlocks = null;
        for (Iterator<WrappedLevelChunkData.BlockEntityInfo> iterator = blockTileEntities.iterator(); iterator.hasNext();) {
            WrappedLevelChunkData.BlockEntityInfo tileEntity = iterator.next();

            String type = tileEntity.getTypeKey().getKey();
            if (!tileEntityTypeFilter.apply(type)) {
                continue;
            }

            iterator.remove();

            if (removedBlocks == null) {
                removedBlocks = new IntOpenHashSet();
            }

            short y = (short) tileEntity.getY();

            byte x = (byte) tileEntity.getSectionX();
            byte z = (byte) tileEntity.getSectionZ();

            // Y, X, Z
            int key = (y & 0xFFFF) | ((x & 0xF) << 16) | ((z & 0xF) << 20);
            removedBlocks.add(key);
        }

        if (removedBlocks == null) {
            return;
        }

        byte[] readerBuffer = data.getBuffer();
        FriendlyByteBuf reader = new FriendlyByteBuf(Unpooled.wrappedBuffer(readerBuffer));

        int bufferSize = 0;
        LevelChunkSection[] sections = new LevelChunkSection[vanillaWorld.getSectionsCount()];
        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            LevelChunkSection section = new LevelChunkSection(vanillaWorld.registryAccess().registryOrThrow(Registries.BIOME));
            section.read(reader);

            int yOffset = vanillaWorld.getSectionYFromSectionIndex(sectionIndex);
            for (byte y = 0; y < 16; y++) {
                for (byte z = 0; z < 16; z++) {
                    for (byte x = 0; x < 16; x++) {
                        int key = (((yOffset << 4) + y) & 0xFFFF) | ((x & 0xF) << 16) | ((z & 0xF) << 20);

                        if (!removedBlocks.contains(key)) {
                            continue;
                        }

                        section.setBlockState(x, y, z, AIR_BLOCK, false);
                    }
                }
            }

            sections[sectionIndex] = section;
            bufferSize += section.getSerializedSize();
        }

        byte[] writerBuffer = new byte[bufferSize];
        FriendlyByteBuf writer = new FriendlyByteBuf(Unpooled.wrappedBuffer(writerBuffer));
        writer.writerIndex(0);

        for (LevelChunkSection section : sections) {
            section.write(writer);
        }

        data.setBuffer(writerBuffer);

        container.getLevelChunkData().write(0, data);
    }

    public PalettedContainer<BlockState>[] getBlockIds(Chunk chunk) {
        CraftChunk craftChunk = (CraftChunk) chunk;
        ChunkAccess chunkAccess = craftChunk.getHandle(ChunkStatus.BIOMES);

        LevelChunkSection[] cs = chunkAccess.getSections();
        PalettedContainer[] sectionBlockIDs = new PalettedContainer[cs.length];

        for (int i = 0; i < cs.length; i++) {
            if (!cs[i].hasOnlyAir()) {
                sectionBlockIDs[i] = cs[i].getStates();
            } else {
                sectionBlockIDs[i] = EMPTY_BLOCK_IDS;
            }
        }

        return sectionBlockIDs;
    }
}
