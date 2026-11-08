package com.grizmtech.perms.engine;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Derlenmiş oyuncu izin tablosu.
 * Öncelik sırası:
 * 1) Tam eşleşen izin düğümü (exactNodes: orn. "essentials.fly" veya "-essentials.fly")
 * 2) En uzun önek wildcard eşleşmesi (prefixWildcards: orn. "essentials.sethome.multiple." > "essentials.")
 * 3) Kök wildcard ("*" veya "-*")
 * 4) Tanımsız (null -> Bukkit varsayılan izin/OP kontrolüne düşer)
 */
public final class EffectivePermissions {

    public static final EffectivePermissions EMPTY = new EffectivePermissions(
            Collections.emptyMap(), Collections.emptyMap(), null
    );

    private final Map<String, Boolean> exactNodes;
    private final Map<String, Boolean> prefixWildcards;
    private final Boolean rootWildcard;

    public EffectivePermissions(Map<String, Boolean> exactNodes,
                                Map<String, Boolean> prefixWildcards,
                                Boolean rootWildcard) {
        this.exactNodes = Collections.unmodifiableMap(new HashMap<>(exactNodes));
        this.prefixWildcards = Collections.unmodifiableMap(new HashMap<>(prefixWildcards));
        this.rootWildcard = rootWildcard;
    }

    public static EffectivePermissions compile(List<String> orderedRawRules) {
        Map<String, Boolean> exact = new HashMap<>();
        Map<String, Boolean> wildcards = new HashMap<>();
        Boolean root = null;

        for (String raw : orderedRawRules) {
            if (raw == null) continue;
            String rule = raw.trim().toLowerCase();
            if (rule.isEmpty()) continue;

            boolean value = true;
            if (rule.startsWith("-")) {
                value = false;
                rule = rule.substring(1).trim();
            } else if (rule.startsWith("+")) {
                rule = rule.substring(1).trim();
            }
            if (rule.isEmpty()) continue;

            if (rule.equals("*")) {
                root = value;
            } else if (rule.endsWith(".*") && rule.length() > 2) {
                String prefix = rule.substring(0, rule.length() - 1); // örn: "essentials."
                wildcards.put(prefix, value);
                // Ayrıca üst kök düğümü de işaretle (örn: "essentials")
                String baseNode = prefix.substring(0, prefix.length() - 1);
                exact.put(baseNode, value);
            } else {
                exact.put(rule, value);
            }
        }

        return new EffectivePermissions(exact, wildcards, root);
    }

    /**
     * İzin durumunu döndürür: true (verildi), false (negasyon ile engellendi), null (tanımlanmamış).
     */
    public Boolean resolve(String permissionName) {
        if (permissionName == null || permissionName.isEmpty()) {
            return null;
        }
        String perm = permissionName.toLowerCase();

        Boolean exactVal = exactNodes.get(perm);
        if (exactVal != null) {
            return exactVal;
        }

        // En spesifik (en uzun) wildcard önekini bul
        String bestPrefix = null;
        Boolean bestValue = null;
        for (Map.Entry<String, Boolean> entry : prefixWildcards.entrySet()) {
            String prefix = entry.getKey();
            if (perm.startsWith(prefix)) {
                if (bestPrefix == null || prefix.length() > bestPrefix.length()) {
                    bestPrefix = prefix;
                    bestValue = entry.getValue();
                }
            }
        }
        if (bestValue != null) {
            return bestValue;
        }

        return rootWildcard;
    }

    public Map<String, Boolean> getExactNodes() {
        return exactNodes;
    }

    public Map<String, Boolean> getPrefixWildcards() {
        return prefixWildcards;
    }

    public Boolean getRootWildcard() {
        return rootWildcard;
    }
}

