package com.sanhiruzu.atelier.synthesis.world;

import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.core.ReagentShape;
import com.sanhiruzu.atelier.synthesis.storage.ReagentContainer;
import com.sanhiruzu.atelier.synthesis.storage.ReagentContainerSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReagentCabinetSavedData extends SavedData {
    private static final String DATA_NAME = "atelier_reagent_cabinets";
    private static final String CABINETS_KEY = "cabinets";
    private static final String POS_KEY = "pos";
    private static final String SNAPSHOT_KEY = "snapshot";
    private static final String ENTRIES_KEY = "entries";
    private static final String REAGENT_KEY = "reagent";
    private static final String AMOUNT_KEY = "amount";
    private static final String TIER_KEY = "tier";
    private static final String QUALITY_KEY = "quality";
    private static final String PURITY_KEY = "purity";
    private static final String INSTABILITY_KEY = "instability";
    private static final String CATEGORIES_KEY = "categories";
    private static final String ELEMENTS_KEY = "elements";
    private static final String TRAITS_KEY = "traits";
    private static final String SHAPE_KEY = "shape";
    private static final String SHAPE_ID_KEY = "id";
    private static final String SHAPE_CELLS_KEY = "cells";
    private static final String SHAPE_CELL_X_KEY = "x";
    private static final String SHAPE_CELL_Y_KEY = "y";
    private static final String SOURCE_HINTS_KEY = "source_hints";

    private final Map<BlockPos, ReagentContainerSnapshot> cabinets = new HashMap<>();

    private static ReagentCabinetSavedData create() {
        return new ReagentCabinetSavedData();
    }

    static ReagentCabinetSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        ReagentCabinetSavedData data = new ReagentCabinetSavedData();
        ListTag cabinetList = tag.getList(CABINETS_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < cabinetList.size(); i++) {
            CompoundTag cabinetTag = cabinetList.getCompound(i);
            BlockPos pos = BlockPos.of(cabinetTag.getLong(POS_KEY));
            ReagentContainerSnapshot snapshot = loadSnapshot(cabinetTag.getCompound(SNAPSHOT_KEY));
            if (!snapshot.entries().isEmpty()) {
                data.cabinets.put(pos, snapshot);
            }
        }
        return data;
    }

    public static ReagentCabinetSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(ReagentCabinetSavedData::create, ReagentCabinetSavedData::load, null),
                DATA_NAME
        );
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag cabinetList = new ListTag();
        for (Map.Entry<BlockPos, ReagentContainerSnapshot> entry : cabinets.entrySet()) {
            if (entry.getValue().entries().isEmpty()) {
                continue;
            }
            CompoundTag cabinetTag = new CompoundTag();
            cabinetTag.putLong(POS_KEY, entry.getKey().asLong());
            cabinetTag.put(SNAPSHOT_KEY, saveSnapshot(entry.getValue()));
            cabinetList.add(cabinetTag);
        }
        tag.put(CABINETS_KEY, cabinetList);
        return tag;
    }

    public ReagentContainer getContainer(BlockPos pos) {
        return cabinets.getOrDefault(pos, new ReagentContainerSnapshot(List.of())).toContainer();
    }

    public ReagentContainerSnapshot getSnapshot(BlockPos pos) {
        return cabinets.getOrDefault(pos, new ReagentContainerSnapshot(List.of()));
    }

    public Set<BlockPos> positions() {
        return Set.copyOf(cabinets.keySet());
    }

    public void putContainer(BlockPos pos, ReagentContainer container) {
        ReagentContainerSnapshot snapshot = ReagentContainerSnapshot.fromContainer(container);
        if (snapshot.entries().isEmpty()) {
            cabinets.remove(pos);
        } else {
            cabinets.put(pos.immutable(), snapshot);
        }
        setDirty();
    }

    public void clear(BlockPos pos) {
        if (cabinets.remove(pos) != null) {
            setDirty();
        }
    }

    static CompoundTag saveSnapshot(ReagentContainerSnapshot snapshot) {
        CompoundTag tag = new CompoundTag();
        ListTag entries = new ListTag();
        for (ReagentStack stack : snapshot.entries()) {
            entries.add(saveStack(stack));
        }
        tag.put(ENTRIES_KEY, entries);
        return tag;
    }

    static ReagentContainerSnapshot loadSnapshot(CompoundTag tag) {
        List<ReagentStack> entries = new ArrayList<>();
        ListTag entryTags = tag.getList(ENTRIES_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < entryTags.size(); i++) {
            ReagentStack stack = loadStack(entryTags.getCompound(i));
            if (stack != null) {
                entries.add(stack);
            }
        }
        return new ReagentContainerSnapshot(entries);
    }

    private static CompoundTag saveStack(ReagentStack stack) {
        CompoundTag tag = new CompoundTag();
        tag.putString(REAGENT_KEY, stack.reagentId());
        tag.putInt(AMOUNT_KEY, stack.amount());
        tag.putInt(TIER_KEY, stack.tier());
        tag.putInt(QUALITY_KEY, stack.quality());
        tag.putInt(PURITY_KEY, stack.purity());
        tag.putInt(INSTABILITY_KEY, stack.instability());
        tag.put(CATEGORIES_KEY, saveStringList(stack.categories().stream().sorted().toList()));

        CompoundTag elements = new CompoundTag();
        for (Map.Entry<String, Integer> element : stack.elements().entrySet()) {
            elements.putInt(element.getKey(), element.getValue());
        }
        tag.put(ELEMENTS_KEY, elements);
        tag.put(TRAITS_KEY, saveStringList(stack.traits()));
        tag.put(SHAPE_KEY, saveShape(stack.shape()));
        tag.put(SOURCE_HINTS_KEY, saveStringList(stack.sourceHints().stream().sorted().toList()));
        return tag;
    }

    private static ReagentStack loadStack(CompoundTag tag) {
        try {
            return new ReagentStack(
                    tag.getString(REAGENT_KEY),
                    new HashSet<>(loadStringList(tag, CATEGORIES_KEY)),
                    tag.getInt(AMOUNT_KEY),
                    tag.getInt(TIER_KEY),
                    tag.getInt(QUALITY_KEY),
                    tag.getInt(PURITY_KEY),
                    tag.getInt(INSTABILITY_KEY),
                    loadElements(tag.getCompound(ELEMENTS_KEY)),
                    loadStringList(tag, TRAITS_KEY),
                    loadShape(tag),
                    new HashSet<>(loadStringList(tag, SOURCE_HINTS_KEY))
            );
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static Map<String, Integer> loadElements(CompoundTag tag) {
        Map<String, Integer> elements = new HashMap<>();
        for (String key : tag.getAllKeys()) {
            int value = tag.getInt(key);
            if (!key.isBlank() && value > 0) {
                elements.put(key, value);
            }
        }
        return elements;
    }

    private static CompoundTag saveShape(ReagentShape shape) {
        CompoundTag tag = new CompoundTag();
        tag.putString(SHAPE_ID_KEY, shape.id());
        ListTag cells = new ListTag();
        for (ReagentShape.Cell cell : shape.cells()) {
            CompoundTag cellTag = new CompoundTag();
            cellTag.putInt(SHAPE_CELL_X_KEY, cell.x());
            cellTag.putInt(SHAPE_CELL_Y_KEY, cell.y());
            cells.add(cellTag);
        }
        tag.put(SHAPE_CELLS_KEY, cells);
        return tag;
    }

    private static ReagentShape loadShape(CompoundTag stackTag) {
        if (!stackTag.contains(SHAPE_KEY)) {
            return ReagentShape.SINGLE;
        }
        CompoundTag shapeTag = stackTag.getCompound(SHAPE_KEY);
        ListTag cellTags = shapeTag.getList(SHAPE_CELLS_KEY, Tag.TAG_COMPOUND);
        List<ReagentShape.Cell> cells = new ArrayList<>();
        for (int i = 0; i < cellTags.size(); i++) {
            CompoundTag cellTag = cellTags.getCompound(i);
            cells.add(new ReagentShape.Cell(cellTag.getInt(SHAPE_CELL_X_KEY), cellTag.getInt(SHAPE_CELL_Y_KEY)));
        }
        return new ReagentShape(shapeTag.getString(SHAPE_ID_KEY), cells);
    }

    private static ListTag saveStringList(List<String> values) {
        ListTag tag = new ListTag();
        for (String value : values) {
            tag.add(StringTag.valueOf(value));
        }
        return tag;
    }

    private static List<String> loadStringList(CompoundTag tag, String key) {
        List<String> values = new ArrayList<>();
        ListTag list = tag.getList(key, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            values.add(list.getString(i));
        }
        return values;
    }
}
