package com.sanhiruzu.atelier.space;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ChunkClassificationData {
    private static final int CHUNK_WIDTH = 16;
    private static final int CHUNK_HEIGHT = 384;
    private static final int BLOCKS_PER_LONG = 32;
    private static final int BITS_PER_BLOCK = 2;
    private static final long BITS_MASK = 0x3L;

    private static final Codec<long[]> LONG_ARRAY_CODEC = Codec.LONG.listOf().xmap(
            list -> list.stream().mapToLong(Long::longValue).toArray(),
            array -> Arrays.stream(array).boxed().toList()
    );

    public static final Codec<ChunkClassificationData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    LONG_ARRAY_CODEC.fieldOf("bitfield").forGetter(d -> d.bitfield),
                    ClassifiedRegion.CODEC.listOf().fieldOf("regions").forGetter(d -> d.regions),
                    Codec.BOOL.fieldOf("isDirty").forGetter(d -> d.isDirty)
            ).apply(instance, ChunkClassificationData::new)
    );

    private final long[] bitfield;
    private final List<ClassifiedRegion> regions;
    private boolean isDirty;

    public ChunkClassificationData() {
        int totalBlocks = CHUNK_WIDTH * CHUNK_WIDTH * CHUNK_HEIGHT;
        int longs = (totalBlocks * BITS_PER_BLOCK + 63) / 64;
        this.bitfield = new long[longs];
        this.regions = new ArrayList<>();
        this.isDirty = true;
    }

    private ChunkClassificationData(long[] bitfield, List<ClassifiedRegion> regions, boolean isDirty) {
        this.bitfield = bitfield;
        this.regions = new ArrayList<>(regions);
        this.isDirty = isDirty;
    }

    public ClassificationState getBlockState(int x, int y, int z) {
        if (!isValidCoord(x, y, z)) {
            return ClassificationState.SOLID;
        }
        int blockIndex = getBlockIndex(x, y, z);
        int longIndex = (blockIndex * BITS_PER_BLOCK) / 64;
        int bitOffset = (blockIndex * BITS_PER_BLOCK) % 64;

        long value = bitfield[longIndex];
        int bits = (int) ((value >> bitOffset) & BITS_MASK);
        return ClassificationState.fromBits(bits);
    }

    public void setBlockState(int x, int y, int z, ClassificationState state) {
        if (!isValidCoord(x, y, z)) {
            return;
        }
        int blockIndex = getBlockIndex(x, y, z);
        int longIndex = (blockIndex * BITS_PER_BLOCK) / 64;
        int bitOffset = (blockIndex * BITS_PER_BLOCK) % 64;

        long mask = ~(BITS_MASK << bitOffset);
        bitfield[longIndex] = (bitfield[longIndex] & mask) | ((long) state.getBits() << bitOffset);
    }

    private int getBlockIndex(int x, int y, int z) {
        return (y + 64) * CHUNK_WIDTH * CHUNK_WIDTH + z * CHUNK_WIDTH + x;
    }

    private boolean isValidCoord(int x, int y, int z) {
        return x >= 0 && x < CHUNK_WIDTH && y >= -64 && y < 320 && z >= 0 && z < CHUNK_WIDTH;
    }

    public void addRegion(ClassifiedRegion region) {
        regions.add(region);
    }

    public void clearRegions() {
        regions.clear();
    }

    public List<ClassifiedRegion> getRegions() {
        return new ArrayList<>(regions);
    }

    public boolean replaceRegionId(UUID eliminatedId, UUID survivingId, int newVolume, int newOpeningArea) {
        for (int i = 0; i < regions.size(); i++) {
            if (regions.get(i).getId().equals(eliminatedId)) {
                regions.set(i, new ClassifiedRegion(survivingId, newVolume, newOpeningArea));
                return true;
            }
        }
        return false;
    }

    public void setDirty(boolean dirty) {
        this.isDirty = dirty;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void serializeNBT(CompoundTag tag) {
        tag.put("bitfield", new LongArrayTag(bitfield));
        tag.putBoolean("isDirty", isDirty);

        CompoundTag regionsTag = new CompoundTag();
        for (int i = 0; i < regions.size(); i++) {
            ClassifiedRegion region = regions.get(i);
            CompoundTag regionTag = new CompoundTag();
            regionTag.putUUID("id", region.getId());
            regionTag.putInt("volume", region.getVolume());
            regionTag.putInt("openingArea", region.getOpeningArea());
            regionsTag.put(String.valueOf(i), regionTag);
        }
        tag.put("regions", regionsTag);
    }

    public void deserializeNBT(CompoundTag tag) {
        long[] savedBitfield = tag.getLongArray("bitfield");
        System.arraycopy(savedBitfield, 0, bitfield, 0, Math.min(savedBitfield.length, bitfield.length));

        this.isDirty = tag.getBoolean("isDirty");

        regions.clear();
        CompoundTag regionsTag = tag.getCompound("regions");
        for (String key : regionsTag.getAllKeys()) {
            CompoundTag regionTag = regionsTag.getCompound(key);
            ClassifiedRegion region = new ClassifiedRegion(
                    regionTag.getUUID("id"),
                    regionTag.getInt("volume"),
                    regionTag.getInt("openingArea")
            );
            regions.add(region);
        }
    }

}
