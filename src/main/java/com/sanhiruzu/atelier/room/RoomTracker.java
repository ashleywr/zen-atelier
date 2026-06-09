package com.sanhiruzu.atelier.room;

import com.sanhiruzu.atelier.zone.StandardZone;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks rooms and their associated data for mods like Create Kaizen.
 * Stores custom metadata about rooms that need to persist.
 */
public class RoomTracker {
    private final Map<UUID, RoomData> trackedRooms = new HashMap<>();
    private final Map<BlockPos, UUID> positionToRoom = new HashMap<>();

    /**
     * Track a room at a specific position.
     */
    public void trackRoom(BlockPos pos, UUID roomId) {
        positionToRoom.put(pos, roomId);
    }

    /**
     * Get the room UUID at a position.
     */
    @Nullable
    public UUID getRoomAt(BlockPos pos) {
        return positionToRoom.get(pos);
    }

    /**
     * Get tracked data for a room.
     */
    @Nullable
    public RoomData getRoomData(UUID roomId) {
        return trackedRooms.computeIfAbsent(roomId, RoomData::new);
    }

    /**
     * Update or create room data.
     */
    public void setRoomData(UUID roomId, RoomData data) {
        trackedRooms.put(roomId, data);
    }

    /**
     * Check if a room is being tracked.
     */
    public boolean isTracking(UUID roomId) {
        return trackedRooms.containsKey(roomId);
    }

    /**
     * Stop tracking a room.
     */
    public void untrackRoom(UUID roomId) {
        trackedRooms.remove(roomId);
        positionToRoom.values().removeIf(id -> id.equals(roomId));
    }

    /**
     * Data associated with a tracked room.
     */
    public static class RoomData {
        private final UUID roomId;
        private final Map<String, Object> properties = new HashMap<>();
        private boolean sealed = true;
        private BoundingBox interiorBounds = null;

        public RoomData(UUID roomId) {
            this.roomId = roomId;
        }

        public UUID getRoomId() {
            return roomId;
        }

        /**
         * Set a custom property on the room.
         */
        public void setProperty(String key, Object value) {
            properties.put(key, value);
        }

        /**
         * Get a custom property from the room.
         */
        @Nullable
        public Object getProperty(String key) {
            return properties.get(key);
        }

        /**
         * Get a custom property with type safety.
         */
        @Nullable
        @SuppressWarnings("unchecked")
        public <T> T getProperty(String key, Class<T> type) {
            Object value = properties.get(key);
            if (type.isInstance(value)) {
                return (T) value;
            }
            return null;
        }

        /**
         * Check if a property exists.
         */
        public boolean hasProperty(String key) {
            return properties.containsKey(key);
        }

        /**
         * Remove a property.
         */
        public void removeProperty(String key) {
            properties.remove(key);
        }

        /**
         * Check if the room is properly sealed/enclosed.
         */
        public boolean isSealed() {
            return sealed;
        }

        /**
         * Set whether the room is sealed.
         */
        public void setSealed(boolean sealed) {
            this.sealed = sealed;
        }

        /**
         * Get the interior bounding box of the room.
         */
        @Nullable
        public BoundingBox getInteriorBounds() {
            return interiorBounds;
        }

        /**
         * Set the interior bounding box of the room.
         */
        public void setInteriorBounds(@Nullable BoundingBox bounds) {
            this.interiorBounds = bounds;
        }

        /**
         * Get the leak point (if any) in the room's enclosure.
         */
        @Nullable
        public BlockPos getLeakPoint() {
            return (BlockPos) getProperty("leak_point");
        }

        /**
         * Set the leak point in the room's enclosure.
         */
        public void setLeakPoint(@Nullable BlockPos pos) {
            if (pos != null) {
                setProperty("leak_point", pos);
            } else {
                removeProperty("leak_point");
            }
        }
    }
}
