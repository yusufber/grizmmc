package com.grizmtech.perms.engine;

import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.PermissionAttachment;

import java.lang.reflect.Field;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CraftHumanEntity üzerindeki PermissibleBase alanını GrizmPermissible ile değiştirir.
 */
public final class PermissibleInjector {

    private static Field permFieldCache;
    private static Field attachmentsFieldCache;
    private static boolean lookupAttempted = false;

    private PermissibleInjector() {
    }

    @SuppressWarnings("unchecked")
    public static boolean inject(Player player, PermissionManager manager, Logger logger) {
        try {
            Field permField = resolvePermField(player.getClass());
            if (permField == null) {
                return false;
            }

            Object current = permField.get(player);
            if (current instanceof GrizmPermissible) {
                return true;
            }

            GrizmPermissible custom = new GrizmPermissible(player, manager);

            // Mevcut PermissionAttachment'ları koru
            if (current instanceof PermissibleBase oldBase) {
                Field attField = resolveAttachmentsField();
                if (attField != null) {
                    List<PermissionAttachment> oldAttachments = (List<PermissionAttachment>) attField.get(oldBase);
                    List<PermissionAttachment> newAttachments = (List<PermissionAttachment>) attField.get(custom);
                    if (oldAttachments != null && newAttachments != null) {
                        newAttachments.addAll(oldAttachments);
                    }
                }
            }

            permField.set(player, custom);
            return true;
        } catch (Throwable t) {
            logger.log(Level.WARNING, "[GrizmPerms] PermissibleBase enjeksiyon uyarısı (" + player.getName() + "): " + t.getMessage());
            return false;
        }
    }

    private static synchronized Field resolvePermField(Class<?> playerClass) {
        if (permFieldCache != null) {
            return permFieldCache;
        }
        if (lookupAttempted) {
            return null;
        }
        lookupAttempted = true;

        Class<?> current = playerClass;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (PermissibleBase.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    permFieldCache = field;
                    return permFieldCache;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static synchronized Field resolveAttachmentsField() {
        if (attachmentsFieldCache != null) {
            return attachmentsFieldCache;
        }
        try {
            Field f = PermissibleBase.class.getDeclaredField("attachments");
            f.setAccessible(true);
            attachmentsFieldCache = f;
        } catch (Throwable ignored) {
        }
        return attachmentsFieldCache;
    }
}

