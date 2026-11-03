// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

final class NativeDeSpriteCatalog {
    static final String QUERY = "SELECT * FROM `spr_action`";

    private NativeDeSpriteCatalog() {
    }

    static int calculate(int n, int n2) {
        return (int)((double)(n * 40) * (24.0 / (double)n2));
    }

    static Catalog empty() {
        return new Catalog(Map.of(), 0L, 0L, false);
    }

    static Catalog read(Connection connection, Catalog catalog, Reporter reporter) {
        Object object;
        boolean bl;
        long l;
        long l2;
        HashMap<Integer, Mutable> hashMap;
        block26: {
            hashMap = new HashMap<Integer, Mutable>();
            catalog.sprites().forEach((n, sprite) -> hashMap.put((Integer)n, new Mutable((Sprite)sprite)));
            l2 = 0L;
            l = 0L;
            bl = false;
            try {
                object = connection.prepareStatement(QUERY);
                try {
                    ResultSet resultSet = object.executeQuery();
                    block22: while (true) {
                        while (resultSet.next()) {
                            ++l2;
                            int n2 = -1;
                            try {
                                n2 = resultSet.getInt("spr_id");
                                Mutable mutable = hashMap.computeIfAbsent(n2, n -> new Mutable());
                                int n3 = resultSet.getInt("act_id");
                                int n4 = Math.max(0, resultSet.getInt("framecount"));
                                int n5 = Math.max(0, resultSet.getInt("framerate"));
                                int n6 = NativeDeSpriteCatalog.calculate(n4, n5);
                                switch (n3) {
                                    case 0: 
                                    case 4: 
                                    case 11: 
                                    case 20: 
                                    case 24: 
                                    case 40: 
                                    case 46: 
                                    case 50: 
                                    case 54: 
                                    case 58: 
                                    case 62: 
                                    case 83: {
                                        mutable.move.put(n3, n6);
                                        break;
                                    }
                                    case 1: 
                                    case 5: 
                                    case 12: 
                                    case 21: 
                                    case 25: 
                                    case 41: 
                                    case 47: 
                                    case 51: 
                                    case 55: 
                                    case 59: 
                                    case 63: 
                                    case 84: {
                                        mutable.attack.put(n3, n6);
                                        break;
                                    }
                                    case 2: {
                                        mutable.damage = n6;
                                        break;
                                    }
                                    case 18: {
                                        mutable.dir = n6;
                                        break;
                                    }
                                    case 19: {
                                        mutable.noDir = n6;
                                        break;
                                    }
                                    case 30: {
                                        mutable.dir30 = n6;
                                        break;
                                    }
                                }
                                continue block22;
                            }
                            catch (Exception exception) {
                                ++l;
                                reporter.row(n2, exception);
                            }
                        }
                        break block26;
                        {
                            continue block22;
                            break;
                        }
                        break;
                    }
                    finally {
                        if (resultSet != null) {
                            resultSet.close();
                        }
                    }
                }
                finally {
                    if (object != null) {
                        object.close();
                    }
                }
            }
            catch (SQLException sQLException) {
                bl = true;
                reporter.sql(sQLException);
            }
        }
        object = new HashMap();
        hashMap.forEach((arg_0, arg_1) -> NativeDeSpriteCatalog.lambda$read$2((HashMap)object, arg_0, arg_1));
        return new Catalog((Map<Integer, Sprite>)object, l2, l, bl);
    }

    private static /* synthetic */ void lambda$read$2(HashMap hashMap, Integer n, Mutable mutable) {
        hashMap.put(n, mutable.snapshot());
    }

    record Catalog(Map<Integer, Sprite> sprites, long rows, long skipped, boolean sqlFailure) {
        Catalog {
            map = Map.copyOf(map);
        }

        int exactAttack(int n, int n2) {
            Sprite sprite = this.sprites.get(n);
            return sprite == null ? 0 : sprite.attack().getOrDefault(n2, sprite.attack().getOrDefault(1, 0));
        }

        int attack(int n, int n2) {
            int n3 = this.exactAttack(n, n2);
            return n3 < 100 ? 900 : n3;
        }

        int exactMove(int n, int n2) {
            Sprite sprite = this.sprites.get(n);
            return sprite == null ? 0 : sprite.move().getOrDefault(n2, sprite.move().getOrDefault(0, 0));
        }

        int move(int n, int n2) {
            int n3 = this.exactMove(n, n2);
            return n3 < 100 ? 640 : n3;
        }

        int dirSpell(int n) {
            Sprite sprite = this.sprites.get(n);
            return sprite == null ? 0 : sprite.dirSpell();
        }

        int noDirSpell(int n) {
            Sprite sprite = this.sprites.get(n);
            return sprite == null ? 0 : sprite.noDirSpell();
        }

        int dirSpell30(int n) {
            Sprite sprite = this.sprites.get(n);
            return sprite == null ? 0 : sprite.dirSpell30();
        }

        int damage(int n) {
            Sprite sprite = this.sprites.get(n);
            int n2 = sprite == null ? 0 : sprite.damage();
            return n2 < 100 ? 300 : n2;
        }

        boolean chainsword(int n) {
            Sprite sprite = this.sprites.get(n);
            return sprite != null && sprite.move().containsKey(83);
        }

        boolean tripleArrow(int n) {
            Sprite sprite = this.sprites.get(n);
            return sprite != null && sprite.attack().containsKey(21);
        }

        long legacyMove(int n) {
            return 200L;
        }

        long legacyAttack(int n) {
            return 200L;
        }

        long legacySkill(int n) {
            return 200L;
        }
    }

    private static final class Mutable {
        final Map<Integer, Integer> move = new HashMap<Integer, Integer>();
        final Map<Integer, Integer> attack = new HashMap<Integer, Integer>();
        int damage;
        int dir;
        int noDir;
        int dir30;

        Mutable() {
        }

        Mutable(Sprite sprite) {
            this.move.putAll(sprite.move());
            this.attack.putAll(sprite.attack());
            this.damage = sprite.damage();
            this.dir = sprite.dirSpell();
            this.noDir = sprite.noDirSpell();
            this.dir30 = sprite.dirSpell30();
        }

        Sprite snapshot() {
            return new Sprite(this.move, this.attack, this.damage, this.dir, this.noDir, this.dir30);
        }
    }

    static interface Reporter {
        public void row(int var1, Exception var2);

        public void sql(SQLException var1);
    }

    record Sprite(Map<Integer, Integer> move, Map<Integer, Integer> attack, int damage, int dirSpell, int noDirSpell, int dirSpell30) {
        Sprite {
            map = Map.copyOf(map);
            map2 = Map.copyOf(map2);
        }
    }
}
