// DECOMPILED from base-0.6.71.jar, TODO review
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import local.lito381.server.NativeDeWeaponKernel;

final class NativeDeInitializerKernel {
    private static final int[][] _class_list = new int[][]{{0, 61, 138, 734, 2786, 6658, 6671}, {1, 48, 37, 1186, 2796, 6661, 6650}};

    private NativeDeInitializerKernel() {
    }

    static void start(Services services, Actor actor, Name name, boolean bl, String string) {
        NativeDeInitializerKernel.init(services, actor, name, bl, string);
        if (actor.getLevel() <= 0) {
            actor.setLevel(1);
        }
        if (actor.getInt() <= 0) {
            actor.setInt(10);
        }
    }

    static void companions(Services services, Actor actor, boolean bl) {
        NativeDeInitializerKernel.companions(services, actor, bl, true);
    }

    static void companions(Services services, Actor actor, boolean bl, boolean bl2) {
        if (bl) {
            NativeDeInitializerKernel.initDoll(services, actor);
        }
        if (bl2) {
            NativeDeInitializerKernel.initPet(services, actor);
        }
    }

    static void init(Services services, Actor actor, Name name) {
        NativeDeInitializerKernel.init(services, actor, name, actor.isMorphEnable(), null);
    }

    static void init(Services services, Actor actor, Name name, boolean bl) {
        NativeDeInitializerKernel.init(services, actor, name, bl, null);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void init(Services services, Actor actor, Name object, boolean bl, String string) {
        block48: {
            try {
                String string2;
                int n;
                int n2;
                int n3;
                Serializable serializable;
                Object object2;
                Setting setting = null;
                ArrayList<Integer> arrayList = null;
                if (string != null && !string.trim().isEmpty()) {
                    try {
                        object2 = string.split(",");
                        serializable = new ArrayList();
                        for (Object object3 : (String[])object2) {
                            if (((String)(object3 = ((String)object3).trim())).isEmpty()) continue;
                            try {
                                int n4 = Integer.parseInt((String)object3);
                                Setting setting2 = services.getSettingById(n4);
                                if (setting2 == null) continue;
                                ((ArrayList)serializable).add(setting2);
                            }
                            catch (NumberFormatException numberFormatException) {
                                // empty catch block
                            }
                        }
                        if (!((ArrayList)serializable).isEmpty()) {
                            setting = (Setting)((ArrayList)serializable).get(services.nextInt(((ArrayList)serializable).size()));
                            services.log().debug("DeInitializer: \u4f7f\u7528\u7cbe\u78ba\u6307\u5b9a\u6a21\u5f0f (Setting ID=" + setting.id + ", ClassId=" + setting.classId + ", WeaponType=" + setting.weaponType + ")");
                        } else {
                            arrayList = new ArrayList<Integer>();
                            for (Object object3 : (String[])object2) {
                                if (((String)(object3 = ((String)object3).trim())).isEmpty()) continue;
                                try {
                                    int n5 = Integer.parseInt((String)object3);
                                    arrayList.add(n5);
                                }
                                catch (NumberFormatException numberFormatException) {
                                    services.log().warn("DeInitializer: classRestrict \u683c\u5f0f\u932f\u8aa4 (\u90e8\u5206): " + (String)object3, numberFormatException);
                                }
                            }
                            if (!arrayList.isEmpty()) {
                                services.log().debug("DeInitializer: \u4f7f\u7528\u820a\u7248\u517c\u5bb9\u6a21\u5f0f (Class IDs: " + String.valueOf(arrayList) + ")");
                            }
                        }
                    }
                    catch (Exception exception) {
                        services.log().warn("DeInitializer: classRestrict \u89e3\u6790\u7570\u5e38: " + string, exception);
                    }
                }
                if (object == null && ((Name[])(object2 = services.getDeNameList())).length > 0) {
                    serializable = new ArrayList();
                    if (setting != null) {
                        for (Object object3 : (Name[])object2) {
                            if (((Name)object3).get_classId() != setting.classId) continue;
                            ((ArrayList)serializable).add(object3);
                        }
                    } else if (arrayList != null && !arrayList.isEmpty()) {
                        for (Object object3 : (Name[])object2) {
                            if (!arrayList.contains(((Name)object3).get_classId())) continue;
                            ((ArrayList)serializable).add(object3);
                        }
                    }
                    if (((ArrayList)serializable).isEmpty()) {
                        for (Object object3 : (Name[])object2) {
                            ((ArrayList)serializable).add(object3);
                        }
                    }
                    if (((ArrayList)serializable).isEmpty()) {
                        services.log().warn("DeInitializer: \u627e\u4e0d\u5230\u7b26\u5408\u689d\u4ef6\u7684\u5047\u4eba\u540d\u55ae");
                    } else {
                        Object object4 = (Name)((ArrayList)serializable).get(services.nextInt(((ArrayList)serializable).size()));
                        ArrayList<Name> arrayList2 = services.liveNames();
                        ReentrantLock reentrantLock = services.namesLock();
                        reentrantLock.lock();
                        try {
                            for (int i = 0; arrayList2.contains(object4) && i < 10; ++i) {
                                object4 = (Name)((ArrayList)serializable).get(services.nextInt(((ArrayList)serializable).size()));
                            }
                        }
                        finally {
                            reentrantLock.unlock();
                        }
                        object = (Name)object4;
                    }
                }
                if (object == null) {
                    services.log().warn("[\u5047\u4eba\u540d\u7a31\u9a57\u8b49][\u540d\u55ae\u9078\u53d6\u5931\u6557] \u627e\u4e0d\u5230 DeName\uff0cnpcObjectId={}\uff1b\u53ef\u80fd\u662f\u5047\u4eba\u540d\u55ae\u70ba\u7a7a\u3001\u8f09\u5165\u5931\u6557\u6216\u6c92\u6709\u53ef\u7528\u540d\u7a31", actor.getId());
                    actor.setInitFailed(true);
                    return;
                }
                if (((Name)object).get_name() == null || ((Name)object).get_name().trim().isEmpty()) {
                    services.log().warn("[\u5047\u4eba\u540d\u7a31\u9a57\u8b49][DB\u540d\u7a31\u7121\u6548] deobjid={}, denamebig5={}, npcObjectId={}", ((Name)object).get_deobjid(), ((Name)object).get_name() == null ? "<NULL>" : "[" + ((Name)object).get_name() + "]", actor.getId());
                    actor.setInitFailed(true);
                    return;
                }
                actor.set_deName((Name)object);
                object2 = services.liveNames();
                serializable = services.namesLock();
                ((ReentrantLock)serializable).lock();
                try {
                    if (!((ArrayList)object2).contains(object)) {
                        ((ArrayList)object2).add(object);
                    }
                }
                finally {
                    ((ReentrantLock)serializable).unlock();
                }
                actor.setNameId(((Name)object).get_name());
                if (actor.getNameId() == null || actor.getNameId().trim().isEmpty() || !((Name)object).get_name().equals(actor.getNameId())) {
                    services.log().error("[\u5047\u4eba\u540d\u7a31\u9a57\u8b49][\u5957\u7528\u6d41\u7a0b\u932f\u8aa4] deobjid={}, sourceName={}, npcNameId={}, npcObjectId={}", ((Name)object).get_deobjid(), "[" + ((Name)object).get_name() + "]", actor.getNameId() == null ? "<NULL>" : "[" + actor.getNameId() + "]", actor.getId());
                    actor.setInitFailed(true);
                    actor.removeFromDeNameList();
                    return;
                }
                int n6 = setting != null ? setting.classId : ((Name)object).get_classId();
                int n7 = ((Name)object).get_sex();
                if (n7 > 1) {
                    n7 = 1;
                }
                if (n6 > 6) {
                    n6 = 0;
                }
                int n8 = _class_list[n7][n6];
                Ability ability = services.getAbility(n6);
                if (ability != null) {
                    int n9 = services.nextInt(Math.max(1, ability.lvlMax - ability.lvlMin + 1)) + ability.lvlMin;
                    actor.setLevel(n9);
                    int n10 = services.nextInt(Math.max(1, ability.hpMax - ability.hpMin + 1)) + ability.hpMin;
                    n3 = services.nextInt(Math.max(1, ability.mpMax - ability.mpMin + 1)) + ability.mpMin;
                    actor.setMaxHp(n10);
                    actor.setCurrentHp(n10);
                    actor.setMaxMp(n3);
                    actor.setCurrentMp(n3);
                    n2 = Math.abs(ability.acMax - ability.acMin);
                    int n11 = ability.acMin - services.nextInt(n2 + 1);
                    int n12 = services.nextInt(Math.max(1, ability.lawfulMax - ability.lawfulMin + 1)) + ability.lawfulMin;
                    actor.setAc(n11);
                    actor.setLawful(n12);
                    actor.setStr(ability.strAdd + 10);
                    actor.setDex(ability.dexAdd + 10);
                    actor.setCon(ability.conAdd + 10);
                    actor.setInt(ability.intAdd + 10);
                    actor.setWis(ability.wisAdd + 10);
                    actor.setCha(ability.chaAdd + 10);
                    actor.addSp(ability.spAdd);
                    actor.addDmgup(ability.dmgAdd);
                    actor.addHitup(n9 / 3);
                }
                if (setting != null) {
                    n = setting.weaponType;
                    services.log().debug("DeInitializer: \u5f37\u5236\u9396\u5b9a\u6b66\u5668\u985e\u578b=" + n + " (\u4f86\u81ea Setting ID=" + setting.id + ")");
                } else {
                    n = ((Name)object).get_weaponType();
                    if (n == 0) {
                        n = NativeDeWeaponKernel.random(n6, services::nextBoolean);
                    }
                }
                int n13 = n8;
                if (bl && (n3 = services.getRandomPoly(n6, n)) > 0) {
                    n13 = n3;
                }
                actor.setClassId(n6);
                actor.setTempCharGfx(n13);
                actor.setGfxId(n13);
                NativeDeWeaponKernel.apply(actor, n);
                actor.setMoveSpeed(1);
                actor.setBraveSpeed(1);
                n3 = services.getAttackSpeed(actor.getTempCharGfx(), actor.getCurrentWeapon() + 1);
                n2 = services.getMoveSpeed(actor.getTempCharGfx(), actor.getCurrentWeapon());
                actor.setPassispeed(n2);
                actor.setAtkspeed(n3);
                if (((Name)object).get_clanid() != 0) {
                    actor.set_clanid(((Name)object).get_clanid());
                    Clan clan = services.clan(((Name)object).get_clanid());
                    if (clan != null) {
                        actor.set_clanname(clan.getClanName());
                        Emblem emblem = services.emblem(clan.getClanId());
                        actor.set_emblem(emblem);
                    }
                }
                if ((string2 = ((Name)object).getTitle()) == null || string2.trim().isEmpty()) {
                    actor.setTitle("");
                } else {
                    actor.setTitle(string2);
                }
                actor.broadcastPacketAll(services.visual(actor.getId(), actor.getCurrentWeapon()));
            }
            catch (Exception exception) {
                int n = object != null ? ((Name)object).get_deobjid() : 0;
                String string3 = object != null ? ((Name)object).get_name() : null;
                String string4 = actor != null ? actor.getNameId() : null;
                services.log().error("[\u5047\u4eba\u540d\u7a31\u9a57\u8b49][\u521d\u59cb\u5316\u4f8b\u5916] deobjid={}, sourceName={}, npcNameId={}, npcObjectId={}", n, string3 == null ? "<NULL>" : "[" + string3 + "]", string4 == null ? "<NULL>" : "[" + string4 + "]", actor != null ? actor.getId() : 0, exception);
                if (actor == null) break block48;
                actor.setInitFailed(true);
                actor.removeFromDeNameList();
            }
        }
    }

    static void initDoll(Services services, Actor actor) {
        try {
            if (!actor.getDolls().isEmpty()) {
                return;
            }
            int n = actor.getCurrentWeapon();
            if (n == 0) {
                return;
            }
            int n2 = actor.getClassId();
            int n3 = services.getRandomDoll(n2, n);
            if (n3 <= 0) {
                return;
            }
            int n4 = 71082;
            Template template = services.template(n4);
            if (template == null) {
                return;
            }
            services.doll(template, actor, n3);
        }
        catch (Exception exception) {
            services.log().error("\u5047\u4eba\u5a03\u5a03\u751f\u6210\u7570\u5e38", exception);
        }
    }

    static void initDoll(Services services, Actor actor, int n, int n2) {
        int n3 = services.getRandomDoll(n, n2);
        if (n3 <= 0) {
            return;
        }
        try {
            Template template = services.template(71082);
            if (template == null) {
                return;
            }
            services.doll(template, actor, n3);
        }
        catch (Exception exception) {
            services.log().error("\u5047\u4eba\u5a03\u5a03\u751f\u6210\u7570\u5e38", exception);
        }
    }

    static void initPet(Services services, Actor actor) {
        try {
            int n = actor.getClassId();
            int n2 = actor.getCurrentWeapon();
            PetInfo petInfo = services.getPetInfo(n, n2);
            if (petInfo == null || petInfo.npcId == 0 || petInfo.count <= 0) {
                return;
            }
            for (int i = 0; i < petInfo.count; ++i) {
                Template template = services.template(petInfo.npcId);
                if (template == null) continue;
                Pet pet = services.summon(template, actor);
                pet.setDePetType(petInfo.type);
                pet.setHomeX(pet.getX());
                pet.setHomeY(pet.getY());
                pet.setHeading(actor.getHeading());
            }
        }
        catch (Exception exception) {
            services.log().error("\u5047\u4eba\u5bf5\u7269 initPet \u7570\u5e38", exception);
        }
    }

    static interface Services {
        public Log log();

        public int nextInt(int var1);

        public boolean nextBoolean();

        public Setting getSettingById(int var1);

        public Name[] getDeNameList();

        public ArrayList<Name> liveNames();

        public ReentrantLock namesLock();

        public Ability getAbility(int var1);

        public int getRandomPoly(int var1, int var2);

        public int getRandomDoll(int var1, int var2);

        public PetInfo getPetInfo(int var1, int var2);

        public int getAttackSpeed(int var1, int var2);

        public int getMoveSpeed(int var1, int var2);

        public Clan clan(int var1);

        public Emblem emblem(int var1);

        public Template template(int var1);

        public void doll(Template var1, Actor var2, int var3);

        public Pet summon(Template var1, Actor var2);

        public Packet visual(int var1, int var2);
    }

    static interface Actor
    extends NativeDeWeaponKernel.Target {
        public boolean isMorphEnable();

        public int getId();

        public void setInitFailed(boolean var1);

        public void set_deName(Name var1);

        public void removeFromDeNameList();

        public void setNameId(String var1);

        public String getNameId();

        public void setLevel(int var1);

        public int getLevel();

        public void setMaxHp(int var1);

        public void setCurrentHp(int var1);

        public void setMaxMp(int var1);

        public void setCurrentMp(int var1);

        public void setAc(int var1);

        public void setLawful(int var1);

        public void setStr(int var1);

        public void setDex(int var1);

        public void setCon(int var1);

        public void setInt(int var1);

        public short getInt();

        public void setWis(int var1);

        public void setCha(int var1);

        public void addSp(int var1);

        public void addDmgup(int var1);

        public void addHitup(int var1);

        public void setClassId(int var1);

        public int getClassId();

        public void setTempCharGfx(int var1);

        public int getTempCharGfx();

        public void setGfxId(int var1);

        public void setCurrentWeapon(int var1);

        public int getCurrentWeapon();

        public void setStatus(int var1);

        public void set_ranged(int var1);

        public void setBowActId(int var1);

        public void setMoveSpeed(int var1);

        public void setBraveSpeed(int var1);

        public void setPassispeed(int var1);

        public void setAtkspeed(int var1);

        public void set_clanid(int var1);

        public void set_clanname(String var1);

        public void set_emblem(Emblem var1);

        public void setTitle(String var1);

        public void broadcastPacketAll(Packet var1);

        public Map<?, ?> getDolls();

        public int getHeading();

        @Override
        default public void weapon(int n) {
            this.setCurrentWeapon(n);
        }

        @Override
        default public void status(int n) {
            this.setStatus(n);
        }

        @Override
        default public void range(int n) {
            this.set_ranged(n);
        }

        @Override
        default public void bow(int n) {
            this.setBowActId(n);
        }
    }

    static class Name {
        final int id;
        final int classId;
        final int sex;
        final int clanId;
        final int weapon;
        final String name;
        final String title;
        final String shop;

        Name(int n, String string, int n2, int n3, int n4, int n5, String string2, String string3) {
            this.id = n;
            this.name = string;
            this.classId = n2;
            this.sex = n3;
            this.clanId = n4;
            this.weapon = n5;
            this.title = string2;
            this.shop = string3;
        }

        int get_deobjid() {
            return this.id;
        }

        String get_name() {
            return this.name;
        }

        int get_classId() {
            return this.classId;
        }

        int get_sex() {
            return this.sex;
        }

        int get_clanid() {
            return this.clanId;
        }

        int get_weaponType() {
            return this.weapon;
        }

        String getTitle() {
            return this.title;
        }

        String getDeShop() {
            return this.shop;
        }

        public void set_deobjid(int n) {
            throw new UnsupportedOperationException("shared source-name definition is immutable");
        }

        public void set_name(String string) {
            throw new UnsupportedOperationException("shared source-name definition is immutable");
        }

        public void set_classId(int n) {
            throw new UnsupportedOperationException("shared source-name definition is immutable");
        }

        public int get_type() {
            return this.get_classId();
        }

        public void set_type(int n) {
            throw new UnsupportedOperationException("shared source-name definition is immutable");
        }

        public void set_sex(int n) {
            throw new UnsupportedOperationException("shared source-name definition is immutable");
        }

        public void set_clanid(int n) {
            throw new UnsupportedOperationException("shared source-name definition is immutable");
        }

        public void set_weaponType(int n) {
            throw new UnsupportedOperationException("shared source-name definition is immutable");
        }

        public void setTitle(String string) {
            throw new UnsupportedOperationException("shared source-name definition is immutable");
        }

        public void setDeShop(String string) {
            throw new UnsupportedOperationException("shared source-name definition is immutable");
        }
    }

    record Setting(int id, int classId, int weaponType) {
    }

    static interface Log {
        public void debug(String var1);

        public void warn(String var1);

        public void warn(String var1, Throwable var2);

        public void warn(String var1, Object var2);

        public void warn(String var1, Object var2, Object var3, Object var4);

        public void error(String var1, Throwable var2);

        public void error(String var1, Object var2, Object var3, Object var4, Object var5);

        public void error(String var1, Object var2, Object var3, Object var4, Object var5, Object var6);
    }

    static final class Ability {
        final int lvlMin;
        final int lvlMax;
        final int lawfulMin;
        final int lawfulMax;
        final int hpMin;
        final int hpMax;
        final int mpMin;
        final int mpMax;
        final int acMin;
        final int acMax;
        final int strAdd;
        final int dexAdd;
        final int conAdd;
        final int intAdd;
        final int wisAdd;
        final int chaAdd;
        final int spAdd;
        final int dmgAdd;

        Ability(int ... nArray) {
            if (nArray.length != 18) {
                throw new IllegalArgumentException("18 explicit source ability values required");
            }
            this.lvlMin = nArray[0];
            this.lvlMax = nArray[1];
            this.lawfulMin = nArray[2];
            this.lawfulMax = nArray[3];
            this.hpMin = nArray[4];
            this.hpMax = nArray[5];
            this.mpMin = nArray[6];
            this.mpMax = nArray[7];
            this.acMin = nArray[8];
            this.acMax = nArray[9];
            this.strAdd = nArray[10];
            this.dexAdd = nArray[11];
            this.conAdd = nArray[12];
            this.intAdd = nArray[13];
            this.wisAdd = nArray[14];
            this.chaAdd = nArray[15];
            this.spAdd = nArray[16];
            this.dmgAdd = nArray[17];
        }
    }

    static interface Clan {
        public String getClanName();

        public int getClanId();
    }

    record Emblem(int id) {
    }

    static interface Packet {
    }

    record Template(int id) {
    }

    record PetInfo(int type, int npcId, int count) {
    }

    static interface Pet {
        public void setDePetType(int var1);

        public int getX();

        public int getY();

        public void setHomeX(int var1);

        public void setHomeY(int var1);

        public void setHeading(int var1);
    }
}
