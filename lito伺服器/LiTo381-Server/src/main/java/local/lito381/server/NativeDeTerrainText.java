// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.BufferedReader;
import java.io.LineNumberReader;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import local.lito381.server.NativeDeTerrainData;
import local.lito381.server.NativeDeTerrainFileReader;
import local.lito381.server.NativeDeTerrainScope;
import local.lito381.server.NativeDeTerrainTable;
import local.lito381.server.NativeDeTerrainV1;
import local.lito381.server.NativeDeWorldLog;

public class NativeDeTerrainText {
    private static final NativeDeWorldLog _log = NativeDeTerrainScope.Logs.getLogger(NativeDeTerrainText.class);
    private static final String _map_dir = "./maps/";

    private byte[][] read(int n, int n2, int n3, int n4) {
        byte[][] byArray = new byte[n3][n4];
        int n5 = n2 > 0 ? n2 : n;
        String string = _map_dir + n5 + ".txt";
        try {
            String string2;
            LineNumberReader lineNumberReader = new LineNumberReader(new BufferedReader(new NativeDeTerrainFileReader(string)));
            int n6 = 0;
            while ((string2 = lineNumberReader.readLine()) != null) {
                if (string2.trim().length() == 0 || string2.startsWith("#")) continue;
                int n7 = 0;
                StringTokenizer stringTokenizer = new StringTokenizer(string2, ",");
                while (stringTokenizer.hasMoreTokens()) {
                    String string3 = stringTokenizer.nextToken();
                    byte by = Byte.parseByte(string3);
                    try {
                        byArray[n7][n6] = by;
                    }
                    catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
                        _log.error("\u6307\u5b9a\u5730\u5716\u52a0\u8f09\u969c\u7919\u6578\u64da\u7570\u5e38\uff08\u9663\u5217\u8d8a\u754c\uff09: mapId={} fileMapId={} \u6a94\u6848={} mapids\u9810\u671f={}x{} \u5beb\u5165\u683c x={} y={}", (Object)n, (Object)n5, (Object)string, (Object)n3, (Object)n4, (Object)n7, (Object)n6, (Object)arrayIndexOutOfBoundsException);
                    }
                    ++n7;
                }
                ++n6;
            }
            lineNumberReader.close();
        }
        catch (Exception exception) {
            _log.error("\u6307\u5b9a\u5730\u5716\u52a0\u8f09\u969c\u7919\u6578\u64da\u7570\u5e38: mapId={} fileMapId={} \u6a94\u6848={} \u539f\u56e0={}", (Object)n, (Object)n5, (Object)string, (Object)exception.toString(), (Object)exception);
        }
        return byArray;
    }

    public Map<Integer, NativeDeTerrainV1> read() {
        HashMap<Integer, NativeDeTerrainV1> hashMap = new HashMap<Integer, NativeDeTerrainV1>();
        Map<Integer, NativeDeTerrainData> map = NativeDeTerrainTable.get().getMaps();
        for (Integer n : map.keySet()) {
            NativeDeTerrainData nativeDeTerrainData = map.get(n);
            int n2 = nativeDeTerrainData.mapId;
            int n3 = nativeDeTerrainData.endX - nativeDeTerrainData.startX + 1;
            int n4 = nativeDeTerrainData.endY - nativeDeTerrainData.startY + 1;
            NativeDeTerrainV1 nativeDeTerrainV1 = null;
            try {
                nativeDeTerrainV1 = new NativeDeTerrainV1((short)n2, this.read(n2, nativeDeTerrainData.copyMapId, n3, n4), nativeDeTerrainData.startX, nativeDeTerrainData.endX, nativeDeTerrainData.startY, nativeDeTerrainData.endY, nativeDeTerrainData.isUnderwater, nativeDeTerrainData.markable, nativeDeTerrainData.teleportable, nativeDeTerrainData.escapable, nativeDeTerrainData.isUseResurrection, nativeDeTerrainData.isUsePainwand, nativeDeTerrainData.isEnabledDeathPenalty, nativeDeTerrainData.isTakePets, nativeDeTerrainData.isRecallPets, nativeDeTerrainData.isUsableItem, nativeDeTerrainData.isUsableSkill, nativeDeTerrainData.isGuaji, nativeDeTerrainData.isUsableShop);
            }
            catch (Exception exception) {
                _log.error("\u5730\u5716\u8cc7\u6599\u751f\u6210\u6578\u64da\u8f09\u5165\u7570\u5e38: mapId={} \u539f\u56e0={}", (Object)n2, (Object)exception.toString(), (Object)exception);
            }
            if (nativeDeTerrainV1 == null) continue;
            hashMap.put(n2, nativeDeTerrainV1);
        }
        return hashMap;
    }

    public static byte[][] readOne(int n, int n2, int n3, int n4) {
        return new NativeDeTerrainText().read(n, n2, n3, n4);
    }
}
