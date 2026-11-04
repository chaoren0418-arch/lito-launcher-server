// DECOMPILED from base-0.6.71.jar, TODO review
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import local.lito381.server.NativeDeItemDefinitionsScope;
import local.lito381.server.NativeDeItemEquipProc;
import local.lito381.server.NativeDeTerrainTimer;
import local.lito381.server.NativeDeWorldLog;

public class NativeDeItemEquipProcTable {
    private static final NativeDeWorldLog _log = NativeDeItemDefinitionsScope.Logs.getLogger(NativeDeItemEquipProcTable.class);
    private final Map<Integer, NativeDeItemEquipProc> _procById;
    private final Map<Integer, List<NativeDeItemEquipProc>> _procsByItem;
    private static final int ENCHANT_ANY_LEVEL = -1;
    final NativeDeItemDefinitionsScope.Data nativeDefinitions = NativeDeItemDefinitionsScope.loadingData();

    private NativeDeItemEquipProcTable() {
        this._procById = new HashMap<Integer, NativeDeItemEquipProc>();
        this._procsByItem = new HashMap<Integer, List<NativeDeItemEquipProc>>();
        this.load();
    }

    public void reload() {
        NativeDeItemDefinitionsScope.requireLoading(this.nativeDefinitions);
        this._procById.clear();
        this._procsByItem.clear();
        this.load();
    }

    private void load() {
        NativeDeItemDefinitionsScope.requireLoading(this.nativeDefinitions);
        NativeDeTerrainTimer nativeDeTerrainTimer = new NativeDeTerrainTimer();
        int n2 = 0;
        try (Connection connection = NativeDeItemDefinitionsScope.Database.get().getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `\u88dd\u5099_armor_\u9b54\u6cd5` ORDER BY `item_id`, `enchant_level`, `proc_group`, `id`");
             ResultSet resultSet = preparedStatement.executeQuery();){
            while (resultSet.next()) {
                NativeDeItemEquipProc nativeDeItemEquipProc = NativeDeItemEquipProcTable.readProc(resultSet);
                this._procById.put(nativeDeItemEquipProc.getId(), nativeDeItemEquipProc);
                this._procsByItem.computeIfAbsent(nativeDeItemEquipProc.getItemId(), n -> new ArrayList()).add(nativeDeItemEquipProc);
                ++n2;
            }
        }
        catch (SQLException sQLException) {
            this.nativeDefinitions.log.error("\u8f09\u5165 \u88dd\u5099_armor_\u9b54\u6cd5 \u5931\u6557! \u8acb\u6aa2\u67e5\u8cc7\u6599\u8868!", sQLException);
        }
        this.nativeDefinitions.log.info("\u8f09\u5165 \u88dd\u5099_armor_\u9b54\u6cd5 ok: " + n2 + " (" + nativeDeTerrainTimer.get() + "ms)");
    }

    private static NativeDeItemEquipProc readProc(ResultSet resultSet) throws SQLException {
        return new NativeDeItemEquipProc(resultSet.getInt("id"), resultSet.getInt("item_id"), resultSet.getInt("enchant_level"), resultSet.getInt("proc_group"), resultSet.getInt("on_attack_chance"), resultSet.getInt("on_hurt_chance"), resultSet.getInt("flat_damage"), resultSet.getInt("rand_damage"), resultSet.getInt("heal_hp"), resultSet.getInt("heal_hp_rand"), resultSet.getInt("heal_mp"), resultSet.getInt("heal_mp_rand"), resultSet.getInt("heal_mode"), resultSet.getInt("trigger_skill"), resultSet.getInt("skill_time_sec"), resultSet.getInt("skill_target"), resultSet.getInt("gfx_id"), resultSet.getInt("gfx_mode"));
    }

    public NativeDeItemEquipProc getProcById(int n) {
        return this._procById.get(n);
    }

    public List<NativeDeItemEquipProc> getProcs(int n, int n2) {
        List<NativeDeItemEquipProc> list = this._procsByItem.get(n);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        ArrayList<NativeDeItemEquipProc> arrayList = new ArrayList<NativeDeItemEquipProc>();
        ArrayList<NativeDeItemEquipProc> arrayList2 = new ArrayList<NativeDeItemEquipProc>();
        for (NativeDeItemEquipProc nativeDeItemEquipProc : list) {
            if (nativeDeItemEquipProc.getEnchantLevel() == n2 && n2 >= 0) {
                arrayList.add(nativeDeItemEquipProc);
                continue;
            }
            if (nativeDeItemEquipProc.getEnchantLevel() != -1) continue;
            arrayList2.add(nativeDeItemEquipProc);
        }
        if (!arrayList.isEmpty()) {
            return List.copyOf(arrayList);
        }
        return List.copyOf(arrayList2);
    }

    @Deprecated
    public NativeDeItemEquipProc getProc(int n, int n2) {
        List<NativeDeItemEquipProc> list = this.getProcs(n, n2);
        return list.isEmpty() ? null : list.get(0);
    }

    public boolean hasProc(int n, int n2) {
        return !this.getProcs(n, n2).isEmpty();
    }

    public List<NativeDeItemEquipProc> getAllProcs(int n) {
        List<NativeDeItemEquipProc> list = this._procsByItem.get(n);
        if (list == null) {
            return Collections.emptyList();
        }
        return List.copyOf(list);
    }

    public int getTotalProcCount() {
        return this._procById.size();
    }

    public static NativeDeItemEquipProcTable getInstance() {
        return (NativeDeItemEquipProcTable)NativeDeItemDefinitionsScope.table(NativeDeItemEquipProcTable.class);
    }
}
