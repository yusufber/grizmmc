package com.grizmtech.towny.claim;

import com.grizmtech.towny.core.ChunkKey;

import java.util.UUID;

public class ClaimChunk {

    private final UUID worldUuid;
    private final int chunkX;
    private final int chunkZ;
    private final long chunkKey;
    private final UUID townUuid;
    private PlotType plotType;
    private UUID plotOwnerUuid;
    private double price;
    private long flagsBitset;
    private boolean isOutpost;

    public ClaimChunk(UUID worldUuid, int chunkX, int chunkZ, UUID townUuid,
                      PlotType plotType, UUID plotOwnerUuid, double price, long flagsBitset, boolean isOutpost) {
        this.worldUuid = worldUuid;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.chunkKey = ChunkKey.pack(chunkX, chunkZ);
        this.townUuid = townUuid;
        this.plotType = plotType != null ? plotType : PlotType.DEFAULT;
        this.plotOwnerUuid = plotOwnerUuid;
        this.price = price;
        this.flagsBitset = flagsBitset;
        this.isOutpost = isOutpost;

        // Varsayılan bayraklar ayarlanmamışsa tipiyle ilklendir
        if (this.flagsBitset == 0) {
            this.flagsBitset = getDefaultBitsetForType(this.plotType);
        }
    }

    public static long getDefaultBitsetForType(PlotType type) {
        long bitset = 0L;
        // RESIDENT varsayılan: Kırma (0), Koyma (1), Sandık (2), Kapı (3)
        bitset |= (1L << (PlotGroup.RESIDENT.getBaseBitShift() + PlotFlag.BLOCK_BREAK.getBitOffset()));
        bitset |= (1L << (PlotGroup.RESIDENT.getBaseBitShift() + PlotFlag.BLOCK_PLACE.getBitOffset()));
        bitset |= (1L << (PlotGroup.RESIDENT.getBaseBitShift() + PlotFlag.CHEST_ACCESS.getBitOffset()));
        bitset |= (1L << (PlotGroup.RESIDENT.getBaseBitShift() + PlotFlag.DOOR_USE.getBitOffset()));

        // ALLY varsayılan: Kapı (3)
        bitset |= (1L << (PlotGroup.ALLY.getBaseBitShift() + PlotFlag.DOOR_USE.getBitOffset()));

        if (type == PlotType.ARENA) {
            // Arenada herkes için PvP açık
            bitset |= (1L << (PlotGroup.RESIDENT.getBaseBitShift() + PlotFlag.PVP.getBitOffset()));
            bitset |= (1L << (PlotGroup.ALLY.getBaseBitShift() + PlotFlag.PVP.getBitOffset()));
            bitset |= (1L << (PlotGroup.OUTSIDER.getBaseBitShift() + PlotFlag.PVP.getBitOffset()));
        } else if (type == PlotType.MARKET) {
            // Pazarda herkes kapı ve sandık (dükkan) açabilsin
            bitset |= (1L << (PlotGroup.OUTSIDER.getBaseBitShift() + PlotFlag.DOOR_USE.getBitOffset()));
            bitset |= (1L << (PlotGroup.ALLY.getBaseBitShift() + PlotFlag.DOOR_USE.getBitOffset()));
        }
        return bitset;
    }

    public boolean hasFlag(PlotGroup group, PlotFlag flag) {
        int shift = group.getBaseBitShift() + flag.getBitOffset();
        return (flagsBitset & (1L << shift)) != 0;
    }

    public void setFlag(PlotGroup group, PlotFlag flag, boolean allowed) {
        int shift = group.getBaseBitShift() + flag.getBitOffset();
        if (allowed) {
            flagsBitset |= (1L << shift);
        } else {
            flagsBitset &= ~(1L << shift);
        }
    }

    public void toggleFlag(PlotGroup group, PlotFlag flag) {
        setFlag(group, flag, !hasFlag(group, flag));
    }

    public UUID getWorldUuid() {
        return worldUuid;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public long getChunkKey() {
        return chunkKey;
    }

    public UUID getTownUuid() {
        return townUuid;
    }

    public PlotType getPlotType() {
        return plotType;
    }

    public void setPlotType(PlotType plotType) {
        this.plotType = plotType;
        // Tip değiştiğinde temel bayrakları koruyup tipe özel bayrakları ekle
        if (plotType == PlotType.ARENA) {
            setFlag(PlotGroup.RESIDENT, PlotFlag.PVP, true);
            setFlag(PlotGroup.ALLY, PlotFlag.PVP, true);
            setFlag(PlotGroup.OUTSIDER, PlotFlag.PVP, true);
        }
    }

    public UUID getPlotOwnerUuid() {
        return plotOwnerUuid;
    }

    public void setPlotOwnerUuid(UUID plotOwnerUuid) {
        this.plotOwnerUuid = plotOwnerUuid;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = Math.max(0.0, price);
    }

    public boolean isForSale() {
        return price > 0.0;
    }

    public long getFlagsBitset() {
        return flagsBitset;
    }

    public void setFlagsBitset(long flagsBitset) {
        this.flagsBitset = flagsBitset;
    }

    public boolean isOutpost() {
        return isOutpost;
    }

    public void setOutpost(boolean outpost) {
        isOutpost = outpost;
    }
}
