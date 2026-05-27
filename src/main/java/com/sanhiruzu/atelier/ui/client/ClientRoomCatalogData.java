package com.sanhiruzu.atelier.ui.client;

import com.sanhiruzu.atelier.ui.network.RoomCatalogSyncPayload;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ClientRoomCatalogData {
    private static final List<RoomCatalogSyncPayload.Entry> ENTRIES = new ArrayList<>();

    private ClientRoomCatalogData() {
    }

    public static void update(List<RoomCatalogSyncPayload.Entry> entries) {
        ENTRIES.clear();
        ENTRIES.addAll(entries.stream()
                .sorted(Comparator.comparing(RoomCatalogSyncPayload.Entry::profileId))
                .toList());
    }

    public static List<RoomCatalogSyncPayload.Entry> all() {
        return List.copyOf(ENTRIES);
    }

    public static int discoveredCount() {
        int count = 0;
        for (RoomCatalogSyncPayload.Entry entry : ENTRIES) {
            if (ClientDiscoveryData.isDiscovered(entry.profileId())) {
                count++;
            }
        }
        return count;
    }

    public static void clear() {
        ENTRIES.clear();
    }
}
