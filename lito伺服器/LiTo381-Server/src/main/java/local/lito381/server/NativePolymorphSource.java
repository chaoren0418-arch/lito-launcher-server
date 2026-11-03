// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import local.lito381.server.BattlePolymorphCatalog;
import local.lito381.server.NativePolymorphWeights;

final class NativePolymorphSource {
    private static volatile Snapshot current;

    static Snapshot read(Connection connection) throws SQLException {
        int n;
        Object object;
        HashMap<Integer, Row> hashMap2;
        ArrayList<NativePolymorphWeights.Row> arrayList = new ArrayList<NativePolymorphWeights.Row>();
        try (Object object2 = connection.prepareStatement("SELECT * FROM `\u9053\u5177_\u8b8a\u5f62\u63a7\u5236\u9b54\u6756` WHERE `chance` > 0 ORDER BY `chance` ASC");){
            hashMap2 = object2.executeQuery();
            try {
                while (hashMap2.next()) {
                    arrayList.add(new NativePolymorphWeights.Row(hashMap2.getInt("poly_id"), hashMap2.getInt("chance")));
                }
            }
            finally {
                if (hashMap2 != null) {
                    hashMap2.close();
                }
            }
        }
        object2 = new HashSet();
        ((HashSet)object2).add(29);
        for (NativePolymorphWeights.Row hashMap3 : arrayList) {
            ((HashSet)object2).add(hashMap3.polyId());
        }
        hashMap2 = new HashMap<Integer, Row>();
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM polymorphs");){
            object = preparedStatement.executeQuery();
            try {
                while (object.next()) {
                    n = object.getInt("polyid");
                    if (!((HashSet)object2).contains(n)) continue;
                    hashMap2.put(n, new Row(object.getString("name"), n, object.getInt("minlevel"), object.getInt("weaponequip"), object.getInt("armorequip"), object.getBoolean("isSkillUse"), object.getInt("cause")));
                }
            }
            finally {
                if (object != null) {
                    object.close();
                }
            }
        }
        HashMap<Integer, BattlePolymorphCatalog.Form> hashMap = new HashMap<Integer, BattlePolymorphCatalog.Form>();
        object = ((HashSet)object2).iterator();
        while (object.hasNext()) {
            n = (Integer)object.next();
            Row row = (Row)hashMap2.get(n);
            if (row == null) {
                throw new SQLException("polymorphs: missing random/fallback polyid=" + n);
            }
            if ((row.cause & 1) == 0) {
                throw new SQLException("polymorphs: random polyid=" + n + " excludes item/magic cause; native no-change response requires qualification");
            }
            try {
                hashMap.put(n, new BattlePolymorphCatalog.Form(row.name, row.gfx, Math.max(1, row.level), row.weapons, row.armor, row.skills, row.cause, false, 0));
            }
            catch (RuntimeException runtimeException) {
                throw new SQLException("polymorphs: random/fallback polyid=" + n + " exceeds native metadata/animation contract", runtimeException);
            }
        }
        return new Snapshot(arrayList, hashMap);
    }

    static void publish(Snapshot snapshot) {
        current = Objects.requireNonNull(snapshot);
    }

    static BattlePolymorphCatalog.Form selectOrNull(int n) {
        Snapshot snapshot = current;
        if (snapshot == null) {
            return null;
        }
        return snapshot.forms.get(NativePolymorphWeights.select(snapshot.weights, n));
    }

    private NativePolymorphSource() {
    }

    private record Row(String name, int gfx, int level, int weapons, int armor, boolean skills, int cause) {
    }

    record Snapshot(List<NativePolymorphWeights.Row> weights, Map<Integer, BattlePolymorphCatalog.Form> forms) {
        Snapshot {
            list = List.copyOf(list);
            map = Map.copyOf(map);
        }
    }
}
