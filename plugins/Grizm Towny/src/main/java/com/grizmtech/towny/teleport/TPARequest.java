package com.grizmtech.towny.teleport;

import java.util.UUID;

public class TPARequest {

    public enum Type {
        TPA,
        TPAHERE
    }

    private final UUID senderUuid;
    private final UUID targetUuid;
    private final Type type;
    private final long timestamp;

    public TPARequest(UUID senderUuid, UUID targetUuid, Type type) {
        this.senderUuid = senderUuid;
        this.targetUuid = targetUuid;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }

    public UUID getSenderUuid() { return senderUuid; }
    public UUID getTargetUuid() { return targetUuid; }
    public Type getType() { return type; }
    public long getTimestamp() { return timestamp; }

    public boolean isExpired(long timeoutSeconds) {
        return (System.currentTimeMillis() - timestamp) > (timeoutSeconds * 1000L);
    }
}
