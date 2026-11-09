package com.grizmtech.towny.core;

import java.util.UUID;

/**
 * Paper 1.21.4 uyumlu yüksek performanslı Chunk anahtarlama ve koordinat yardımcı sınıfı.
 * 64-bit bit-packing ile O(1) bellek erişimi sağlar:
 * long chunkKey = ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
 */
public final class ChunkKey {

    private ChunkKey() {}

    public static long pack(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    public static int getX(long chunkKey) {
        return (int) (chunkKey >> 32);
    }

    public static int getZ(long chunkKey) {
        return (int) chunkKey;
    }

    public record ChunkCoord(UUID worldUuid, int x, int z) {
        public long toPackedKey() {
            return pack(x, z);
        }

        public static ChunkCoord from(UUID worldUuid, long key) {
            return new ChunkCoord(worldUuid, getX(key), getZ(key));
        }

        public static ChunkCoord fromLocation(org.bukkit.Location location) {
            return new ChunkCoord(location.getWorld().getUID(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
        }
    }
}
