package local.lito381.server;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import local.lito381.server.ModernEquipmentOverrideStore.Fingerprint;

/** Fresh source-owned shop facts, not purchased inventory or a cached purchase grant. */
final class ModernShopCatalog {
    static final int MAX_ROWS = 256;
    enum Support { HEALING, UNIMPLEMENTED, UNSUPPORTED_TEMPLATE, TIMED, AMMO, MATERIAL }
    /** Selected raw projectile row. Retained even when unsupported; never defaulted. */
    record Arrow(long small,long large,long rawArrowSprite,long consume) {
        Arrow {if(small<0||large<0||rawArrowSprite< -1||consume<0)throw bad();}
    }

    /** Exact selected etcitem columns. NULL executor means unknown, never a default implementation. */
    record Etc(String executor,String itemType,String useType,String material,
               long stackable,long maxChargeCount,long minLevel,long maxLevel,long blessing,long trade,
               long cantDelete,long delayGroup,long delayMillis,long delayEffect,long foodVolume,
               long saveAtOnce,long limitClass) {
        Etc {
            if (executor!=null && !validText(executor,256) || !validText(itemType,64)
                    || !validText(useType,64) || !validText(material,64)
                    || !flag(stackable) || maxChargeCount<0 || minLevel<0 || maxLevel<0
                    || maxLevel!=0 && maxLevel<minLevel || blessing<0 || blessing>255
                    || !flag(trade) || !flag(cantDelete) || delayGroup<0 || delayMillis<0
                    || delayEffect<0 || foodVolume<0 || !flag(saveAtOnce) || limitClass<0 || limitClass>127) throw bad();
        }
        @Override public String toString() { return "ModernShopEtc[redacted]"; }
    }
    record Template(ModernCharacterData.ItemKind kind,String nameToken,long inventoryGraphic,
                    long groundGraphic,long weight,Etc etc,Arrow arrow) {
        Template(ModernCharacterData.ItemKind kind,String nameToken,long inventoryGraphic,long groundGraphic,long weight,Etc etc) {
            this(kind,nameToken,inventoryGraphic,groundGraphic,weight,etc,null);
        }
        Template {
            Objects.requireNonNull(kind);
            if (!validText(nameToken,256) || inventoryGraphic<0 || groundGraphic<0 || weight<0
                    || (kind==ModernCharacterData.ItemKind.ETC)!=(etc!=null)||arrow!=null&&kind!=ModernCharacterData.ItemKind.ETC) throw bad();
        }
    }
    record Healing(long min,long max,int effect,long delayGroup,long delayMillis) {
        Healing { if(min<1 || max<min || effect<1 || delayGroup<0 || delayMillis<1 || delayMillis>10_000)throw bad(); }
    }
    /** A qualified native tuple, never an arbitrary executor or client-granted buff. */
    record Timed(ModernTimedEffects.Kind kind) {
        Timed { Objects.requireNonNull(kind); }
        boolean eligible(int characterClass) {
            return characterClass>=0 && characterClass<=6 && (kind.classMask()&(1<<characterClass))!=0;
        }
    }
    record Offer(long itemId,int sortOrder,long buyPrice,long sellPrice,String nameToken,
                 long inventoryGraphic,long groundGraphic,long weight,Support support,Healing healing,Template evidence,Timed timed,ModernAmmo.Facts ammo) {
        Offer(long itemId,int sortOrder,long buyPrice,long sellPrice,String nameToken,long inventoryGraphic,long groundGraphic,long weight,
              Support support,Healing healing,Template evidence,Timed timed) {
            this(itemId,sortOrder,buyPrice,sellPrice,nameToken,inventoryGraphic,groundGraphic,weight,support,healing,evidence,timed,null);
        }
        Offer(long itemId,int sortOrder,long buyPrice,long sellPrice,String nameToken,
              long inventoryGraphic,long groundGraphic,long weight,Support support,Healing healing,Template evidence) {
            this(itemId,sortOrder,buyPrice,sellPrice,nameToken,inventoryGraphic,groundGraphic,weight,support,healing,evidence,null);
        }
        Offer {
            Objects.requireNonNull(evidence);Objects.requireNonNull(support);
            if(itemId<1 || sortOrder<0 || sortOrder>65535 || buyPrice<1 || sellPrice<1
                    || !evidence.nameToken().equals(nameToken) || evidence.inventoryGraphic()!=inventoryGraphic
                    || evidence.groundGraphic()!=groundGraphic || evidence.weight()!=weight)throw bad();
            Healing qualified=qualify(itemId,evidence);
            Timed qualifiedTimed=qualifyTimed(itemId,evidence);
            var qualifiedAmmo=qualifyAmmo(itemId,evidence);
            Support expected=qualified!=null?Support.HEALING:qualifiedTimed!=null?Support.TIMED:qualifiedAmmo!=null?Support.AMMO:ModernSpellMaterials.qualify(itemId,evidence)!=null?Support.MATERIAL:
                    isHealingId(itemId)||timedKind(itemId)!=null||itemId==ModernSlow.MATERIAL_ID||evidence.arrow()!=null?Support.UNSUPPORTED_TEMPLATE:Support.UNIMPLEMENTED;
            if(support!=expected || !Objects.equals(healing,qualified) || !Objects.equals(timed,qualifiedTimed)||!Objects.equals(ammo,qualifiedAmmo))throw bad();
        }
        static Offer fromSource(long itemId,int sortOrder,long buyPrice,long sellPrice,Template template) {
            Objects.requireNonNull(template);Healing healing=qualify(itemId,template);Timed timed=qualifyTimed(itemId,template);var ammo=qualifyAmmo(itemId,template);
            return new Offer(itemId,sortOrder,buyPrice,sellPrice,template.nameToken(),template.inventoryGraphic(),
                    template.groundGraphic(),template.weight(),healing!=null?Support.HEALING:timed!=null?Support.TIMED:ammo!=null?Support.AMMO:ModernSpellMaterials.qualify(itemId,template)!=null?Support.MATERIAL:
                    isHealingId(itemId)||timedKind(itemId)!=null||itemId==ModernSlow.MATERIAL_ID||template.arrow()!=null?Support.UNSUPPORTED_TEMPLATE:Support.UNIMPLEMENTED,healing,template,timed,ammo);
        }
        ModernSpellMaterials.Facts material() { return ModernSpellMaterials.qualify(itemId,evidence); }
        Template template() { return evidence; }
    }
    record Snapshot(int sourceId,String canonicalAccount,ModernApplicationProtocol.CharacterRow character,
                    Fingerprint token,List<Offer> offers) {
        Snapshot(int sourceId,String canonicalAccount,ModernApplicationProtocol.CharacterRow character,List<Offer> offers) {
            this(sourceId,canonicalAccount,character,fingerprint(sourceId,canonicalAccount,character,offers),offers);
        }
        Snapshot {
            validateOwner(sourceId,canonicalAccount,character);validateOffers(offers);offers=List.copyOf(offers);
            if(!fingerprint(sourceId,canonicalAccount,character,offers).equals(token))throw bad();
        }
        @Override public String toString() { return "ModernShopSnapshot[redacted]"; }
    }
    private ModernShopCatalog() {}
    private static boolean isHealingId(long id) { return id==40010 || id==40011 || id==40012; }
    private static ModernAmmo.Facts qualifyAmmo(long id,Template template) {
        var kind=ModernAmmo.Kind.find(id);var a=template.arrow();var e=template.etc();
        if(kind==null||a==null||e==null||!template.nameToken().equals(id==40744?"$93":"$773")
                ||template.inventoryGraphic()!=96||template.groundGraphic()!=66||!"0".equals(e.executor())
                ||!e.itemType().equals("arrow")||!e.useType().equals("arrow")||!e.material().equals(kind.sourceMaterial())
                ||e.stackable()!=1||e.maxChargeCount()!=0||e.minLevel()!=0||e.maxLevel()!=0||e.blessing()!=0
                ||e.trade()!=0||e.cantDelete()!=0||e.delayGroup()!=0||e.delayMillis()!=0||e.delayEffect()!=0
                ||e.foodVolume()!=0||e.saveAtOnce()!=0||e.limitClass()!=127||a.small()<1||a.large()<1
                ||a.consume()!=1||a.rawArrowSprite()!=-1&&a.rawArrowSprite()!=66)return null;
        return new ModernAmmo.Facts(id,a.small(),a.large(),(int)a.rawArrowSprite(),true,kind.material(),0);
    }

    /**
     * Current retained hp.UserAddHp.set_set consumes one item for true/1.
     * Its optional currency operands default to zero; we accept explicit zero or
     * the complete four-argument form, never malformed parser-default fallbacks.
     * These three exact base tuples are the bounded implemented feature. Native
     * potion modifiers/debuffs are not silently inferred from this catalog.
     */
    private static Healing qualify(long id,Template template) {
        if(!isHealingId(id) || template.kind()!=ModernCharacterData.ItemKind.ETC)return null;
        Etc e=template.etc();long min=id==40010?10:id==40011?30:60,max=id==40010?20:id==40011?50:80;
        int effect=id==40010?189:id==40011?194:197;long delay=id==40010?500:1000;
        String name=id==40010?"$26":id==40011?"$235":"$238";
        long inventory=id==40010?80:id==40011?78:81,ground=id==40010?45:id==40011?43:46;
        if(!template.nameToken().equals(name) || template.inventoryGraphic()!=inventory || template.groundGraphic()!=ground
                || e.executor()==null || !e.itemType().equals("potion") || !e.useType().equals("hpr")
                || !e.material().equals("glass") || e.stackable()!=1 || e.maxChargeCount()!=0
                || e.minLevel()!=0 || e.maxLevel()!=0 || e.blessing()!=0 || e.trade()!=0 || e.cantDelete()!=0
                || e.delayGroup()!=2 || e.delayMillis()!=delay || e.delayEffect()!=0 || e.foodVolume()!=0
                || e.limitClass()!=127)return null;
        String[] fields=e.executor().split(" +",-1);
        if(fields.length!=5 && fields.length!=7 || !fields[0].equals("hp.UserAddHp")
                || !fields[1].equals(Long.toString(min)) || !fields[2].equals(Long.toString(max))
                || !fields[3].equals(Integer.toString(effect))
                || !(fields[4].equalsIgnoreCase("true") || fields[4].equals("1"))
                || fields.length==7 && (!fields[5].equals("0") || !fields[6].equals("0")))return null;
        return new Healing(min,max,effect,e.delayGroup(),e.delayMillis());
    }
    private static ModernTimedEffects.Kind timedKind(long id) {
        for(var kind:ModernTimedEffects.Kind.values())if(kind.itemId()==id)return kind;
        return null;
    }
    /**
     * Native StatusHaste/Brave/ElfBrave use seconds, effect, consume, optional
     * currency operands. With consume=true those operands are NOT charged.
     * Only complete numeric forms qualify; native parser defaulting, old class
     * operands, free-use executors and unknown extra operands do not.
     * Wisdom has only seconds/effect and always consumes one source item.
     */
    private static Timed qualifyTimed(long id,Template template) {
        var kind=timedKind(id);
        if(kind==null || template.kind()!=ModernCharacterData.ItemKind.ETC)return null;
        String name;long inventory,ground;String executor;
        switch(kind) {
            case HASTE -> {name="$234";inventory=77;ground=42;executor="brave.StatusHaste";}
            case BRAVE -> {name="$943";inventory=79;ground=44;executor="brave.StatusBrave";}
            case ELF_BRAVE -> {name="$110";inventory=130;ground=81;executor="brave.StatusElfBrave";}
            case WISDOM -> {name="$944";inventory=1529;ground=3836;executor="brave.StatusWisdomPotion";}
            default -> {return null;}
        }
        Etc e=template.etc();
        if(!template.nameToken().equals(name) || template.inventoryGraphic()!=inventory || template.groundGraphic()!=ground
                || e.executor()==null || !e.itemType().equals("potion") || !e.useType().equals("power")
                || !e.material().equals(kind==ModernTimedEffects.Kind.ELF_BRAVE?"vegetation":"glass")
                || e.stackable()!=1 || e.maxChargeCount()!=0 || e.minLevel()!=0 || e.maxLevel()!=0
                || e.blessing()!=0 || e.trade()!=0 || e.cantDelete()!=0 || e.delayGroup()!=0
                || e.delayMillis()!=0 || e.delayEffect()!=0 || e.foodVolume()!=0 || e.saveAtOnce()!=0 || e.limitClass()!=kind.classMask())return null;
        String[] fields=e.executor().split(" ",-1);
        boolean wisdom=kind==ModernTimedEffects.Kind.WISDOM;
        if((wisdom?fields.length!=3:fields.length!=4&&fields.length!=6) || !fields[0].equals(executor))return null;
        Integer seconds=nonnegativeInt(fields[1]),effect=nonnegativeInt(fields[2]);
        if(seconds==null || effect==null || seconds.longValue()*1000!=kind.durationMillis() || effect!=kind.effect())return null;
        if(!wisdom && (!(fields[3].equalsIgnoreCase("true") || fields[3].equals("1"))
                || fields.length==6 && (nonnegativeInt(fields[4])==null || nonnegativeInt(fields[5])==null)))return null;
        return new Timed(kind);
    }
    private static Integer nonnegativeInt(String value) {
        if(!value.matches("\\+?[0-9]+"))return null;
        try{return Integer.valueOf(value);}catch(NumberFormatException invalid){return null;}
    }
    private static void validateOwner(int source,String account,ModernApplicationProtocol.CharacterRow row) {
        if(source<1 || !validText(account,64) || row==null || row.objectId()<1 || !validText(row.name(),256)
                || row.level()<1 || row.characterClass()<0 || row.characterClass()>65535 || row.sex()<0 || row.sex()>1)throw bad();
    }
    private static void validateOffers(List<Offer> offers) {
        if(offers==null || offers.size()>MAX_ROWS)throw bad();int previous=-1;var ids=new HashSet<Long>();
        for(Offer offer:offers) {
            if(offer==null || offer.sortOrder()<=previous || !ids.add(offer.itemId()))throw bad();
            previous=offer.sortOrder();
        }
    }
    private static Fingerprint fingerprint(int source,String account,ModernApplicationProtocol.CharacterRow row,List<Offer> offers) {
        validateOwner(source,account,row);validateOffers(offers);
        try {
            var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);
            out.write("LITO-SHOP-CATALOG\0".getBytes(StandardCharsets.US_ASCII));out.writeShort(1);
            out.writeInt(source);text(out,account);out.writeLong(row.objectId());text(out,row.name());
            out.writeInt(row.characterClass());out.writeByte(row.sex());out.writeLong(row.level());out.writeShort(offers.size());
            for(Offer offer:offers) {
                out.writeLong(offer.itemId());out.writeInt(offer.sortOrder());out.writeLong(offer.buyPrice());out.writeLong(offer.sellPrice());
                Template t=offer.evidence();out.writeByte(t.kind().ordinal());text(out,t.nameToken());
                out.writeLong(t.inventoryGraphic());out.writeLong(t.groundGraphic());out.writeLong(t.weight());
                out.writeByte(offer.support().ordinal());out.writeBoolean(offer.healing()!=null);
                if(offer.healing()!=null) {Healing h=offer.healing();out.writeLong(h.min());out.writeLong(h.max());out.writeInt(h.effect());out.writeLong(h.delayGroup());out.writeLong(h.delayMillis());}
                // Existing non-timed catalogs retain their canonical byte stream.
                // TIMED was not a valid support ordinal in that stream before v10.
                if(offer.timed()!=null) {var k=offer.timed().kind();out.writeByte(k.ordinal());out.writeLong(k.durationMillis());out.writeInt(k.effect());out.writeInt(k.classMask());}
                if(t.arrow()!=null) {out.write("LITO-ARROW\0".getBytes(StandardCharsets.US_ASCII));var a=t.arrow();out.writeLong(a.small());out.writeLong(a.large());out.writeLong(a.rawArrowSprite());out.writeLong(a.consume());}
                out.writeBoolean(t.etc()!=null);
                if(t.etc()!=null) {
                    Etc e=t.etc();out.writeBoolean(e.executor()!=null);if(e.executor()!=null)text(out,e.executor());
                    text(out,e.itemType());text(out,e.useType());text(out,e.material());
                    for(long n:new long[]{e.stackable(),e.maxChargeCount(),e.minLevel(),e.maxLevel(),e.blessing(),e.trade(),e.cantDelete(),
                            e.delayGroup(),e.delayMillis(),e.delayEffect(),e.foodVolume(),e.saveAtOnce(),e.limitClass()})out.writeLong(n);
                }
            }
            out.flush();return new Fingerprint(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
        } catch(java.io.IOException | java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException("shop fingerprint unavailable"); }
    }
    private static void text(DataOutputStream out,String value)throws java.io.IOException {byte[] bytes=value.getBytes(StandardCharsets.UTF_8);out.writeShort(bytes.length);out.write(bytes);}
    static boolean validText(String text,int maximum) {
        return text!=null && !text.codePoints().allMatch(c->Character.isWhitespace(c)||Character.isSpaceChar(c))
                && text.codePoints().noneMatch(Character::isISOControl) && StandardCharsets.UTF_8.newEncoder().canEncode(text)
                && text.getBytes(StandardCharsets.UTF_8).length<=maximum;
    }
    private static boolean flag(long value){return value==0 || value==1;}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid modern shop source value");}
}
