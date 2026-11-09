package com.grizmtech.oneblock.managers;

import com.grizmtech.oneblock.GrizmOneBlock;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class StackerManager {

    private final GrizmOneBlock plugin;
    private final NamespacedKey stackKey;

    public StackerManager(GrizmOneBlock plugin) {
        this.plugin = plugin;
        this.stackKey = new NamespacedKey(plugin, "mob_stack_count");
    }

    public NamespacedKey getStackKey() {
        return stackKey;
    }

    public int getStack(LivingEntity entity) {
        if (entity == null || entity instanceof org.bukkit.entity.ArmorStand || entity.getType() == org.bukkit.entity.EntityType.ARMOR_STAND) return 1;
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        return pdc.getOrDefault(stackKey, PersistentDataType.INTEGER, 1);
    }

    public void setStack(LivingEntity entity, int count) {
        if (entity == null || entity instanceof org.bukkit.entity.ArmorStand || entity.getType() == org.bukkit.entity.EntityType.ARMOR_STAND) return;
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.set(stackKey, PersistentDataType.INTEGER, count);
        updateName(entity, count);
    }

    public void updateName(LivingEntity entity, int count) {
        if (count > 1) {
            String typeName = entity.getType().name().replace("_", " ").toLowerCase();
            entity.customName(Component.text(count + "x ").color(NamedTextColor.YELLOW)
                    .append(Component.text(typeName.substring(0, 1).toUpperCase() + typeName.substring(1)).color(NamedTextColor.WHITE)));
            entity.setCustomNameVisible(true);
        } else {
            entity.customName(null);
            entity.setCustomNameVisible(false);
        }
    }
}
