package local.lito381.server;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.LongSupplier;

/** Independent server-only candidate. No game wire extension or Native actor profile change.
 * Captures ONE source-qualified physical bundle and the current owner-bound gear revision.
 * Caller mints/invalidates the selection lease only after server-confirmed selection and
 * retains its operation in the actual BackendExecutor.Work until Work.close(). */
final class NativePlayerLoadoutAdapter {
    static final int MAX_PROJECTION_BYTES=2*1024*1024;
    record Captured(ModernRooms.Scope owner,ModernEquipmentOverrideStore.Fingerprint ownerBinding,
                    ModernPhysicalCombat.Source physical,ModernPhysicalCombat.Fighter fighter) {
        Captured {
            Objects.requireNonNull(owner);Objects.requireNonNull(ownerBinding);Objects.requireNonNull(physical);Objects.requireNonNull(fighter);
            var source=physical.input().equipment().source();
            if(source.sourceId()!=owner.sourceId()||source.character().objectId()!=owner.objectId()
                    ||!fighter.profile().source().equals(source))throw new IllegalArgumentException("loadout projection owner differs");
        }
        /** Numeric lifetime only; never a replacement for today's selection/match authority. */
        boolean currentAt(long now) {var p=fighter.profile();return now>=p.evaluatedAtMillis()&&(p.validUntilMillis()<0||now<p.validUntilMillis());}
        /** Offline evidence projection LPL1/schema1. Not an application request/result or
         * a self-sufficient serialized Physical.Source; typed physical() retains its full
         * qualified catalogs. Returned bytes are fresh and bounded. */
        byte[] encode() throws Exception {
            var p=fighter.profile();var e=p.effective();var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);
            out.writeInt(0x4c504c31);out.writeInt(0);out.writeShort(1);out.writeShort(fighter.supported()?1:0);
            out.writeInt(owner.sourceId());out.writeLong(owner.objectId());out.writeLong(owner.epoch());out.writeInt(owner.channelId());
            out.writeLong(p.equipmentRevision());out.writeLong(p.evaluatedAtMillis());out.writeLong(p.validUntilMillis());out.write(ownerBinding.bytes());
            byte[] baseline=ModernApplicationProtocol.encode(new ModernApplicationProtocol.CharacterDataResult(1,owner.sourceId(),owner.objectId(),owner.epoch(),0,p.source()),47);
            out.writeInt(baseline.length);out.write(baseline);
            for(long v:new long[]{e.maximumHp(),e.maximumMp(),e.initialHp(),e.initialMp(),e.armorClass(),e.strength(),e.dexterity(),e.constitution(),e.intelligence(),e.wisdom(),e.charisma()})out.writeLong(v);
            out.writeShort(ModernEquipmentFacts.Stat.values().length);
            for(var stat:ModernEquipmentFacts.Stat.values()){text(out,stat.name());out.writeLong(p.equipmentEvaluation().total(stat));}
            out.writeShort(p.equipped().size());
            for(var item:p.equipped()){out.writeLong(item.objectId());out.writeBoolean(item.equipped());var life=p.sourceLifetimes().stream().filter(x->x.itemObjectId()==item.objectId()).findFirst().orElseThrow();text(out,life.state().name());out.writeLong(life.expiresAtMillis());}
            var weapons=new TreeMap<>(physical.input().weapons());out.writeShort(weapons.size());
            for(var w:weapons.values()){for(long v:new long[]{w.objectId(),w.catalogId(),w.retainedType(),w.damageSmall(),w.damageLarge(),w.range(),w.material(),w.templateBlessing(),w.maximumDamageChance(),w.criticalChance()})out.writeLong(v);out.writeDouble(w.criticalMultiplier());out.writeLong(w.attributeKind());out.writeLong(w.attributeLevel());}
            text(out,p.weapon().state().name());out.writeLong(p.weapon().objectId());text(out,fighter.unsupportedReason().name());
            for(var rows:List.of(physical.growth().strength(),physical.growth().dexterity())){out.writeShort(rows.size());for(var row:rows){out.writeLong(row.id());out.writeLong(row.hit());out.writeLong(row.damage());}}
            out.flush();byte[] body=bytes.toByteArray();if(body.length>MAX_PROJECTION_BYTES-32)throw new IOException("loadout projection budget");ByteBuffer.wrap(body).putInt(4,body.length+32);
            var hash=MessageDigest.getInstance("SHA-256");hash.update("LITO-NATIVE-PLAYER-LOADOUT-PROJECTION\0".getBytes(StandardCharsets.US_ASCII));hash.update(body);
            return ByteBuffer.allocate(body.length+32).put(body).put(hash.digest()).array();
        }
        private static void text(DataOutputStream out,String value)throws IOException{byte[] b=value.getBytes(StandardCharsets.US_ASCII);if(b.length>128)throw new IOException("projection text bound");out.writeShort(b.length);out.write(b);}
        @Override public String toString(){return "NativePlayerLoadoutCaptured[redacted]";}
    }
    private final ModernEquipmentAuthority.PhysicalProvider provider;
    private final ModernEquipmentOverrideStore store;private final LongSupplier clock;
    NativePlayerLoadoutAdapter(ModernEquipmentAuthority.PhysicalProvider provider,ModernEquipmentOverrideStore store,LongSupplier clock){this.provider=Objects.requireNonNull(provider);this.store=Objects.requireNonNull(store);this.clock=Objects.requireNonNull(clock);}
    Captured capture(ModernAccountAuthentication.Principal principal,ModernApplicationProtocol.CharacterRow selected,
                     ModernRooms.Scope owner,Object ownerCapability,ModernEquipmentLease.Operation operation)throws Exception {
        Objects.requireNonNull(principal);Objects.requireNonNull(selected);Objects.requireNonNull(owner);Objects.requireNonNull(operation);
        var key=new TournamentPresence.CharacterKey(owner.sourceId(),owner.objectId());
        if(!principal.authorizesSource(owner.sourceId())||selected.objectId()!=owner.objectId()
                ||!operation.matchesKey(key)||!operation.matchesSelection(ownerCapability,owner.epoch())
                ||ModernBackendScope.current()!=operation.scope())throw new SecurityException("loadout selection differs");
        var scope=operation.scope();scope.checkActive();var loaded=provider.readForWorld(principal,owner.sourceId(),owner.objectId(),false);
        var physical=loaded.source();var source=physical.input().equipment().source();
        if(!physical.input().weaponsQualified()||source.sourceId()!=owner.sourceId()||!source.character().equals(selected))throw new IOException("loadout source changed");
        scope.checkActive();var binding=new ModernEquipmentReconciliation.ExpectedBinding(key,loaded.ownerBinding());
        var stored=store.read(key,scope);long now=clock.getAsLong();
        var equipment=physical.input().equipment();var lifetime=equipment.lifetimes();
        long observedAt=lifetime.queryStartedEpochMillis();
        if(now<observedAt)throw new IOException("loadout clock regressed after SQL observation");
        // A slow read must not silently turn an observed active selected weapon
        // into a new unarmed loadout when its deadline passes before qualification.
        // Use the same read bundle and current owner-bound CAS, without another SQL
        // query or trusting an offline projection. Already expired-at-read items
        // retain the existing source evaluator semantics.
        var atRead=ModernEquipmentReconciliation.reconcile(equipment,binding,stored,observedAt);
        for(var item:atRead.desiredLoadout().items()) {
            if(!atRead.overrides().getOrDefault(item.objectId(),item.equipped()))continue;
            var view=lifetime.view(item.objectId(),observedAt);
            if(view.state()==ModernItemLifetime.State.ACTIVE&&now>=view.expiresAtMillis())
                throw new IOException("loadout source lifetime elapsed after SQL observation");
        }
        var fighter=ModernPhysicalCombat.qualify(physical,binding,stored,now);
        var p=fighter.profile();operation.restrictCommit(p.evaluatedAtMillis(),p.validUntilMillis(),clock);
        if(!operation.matchesSelection(ownerCapability,owner.epoch())||!operation.tryCommit())throw new IOException("loadout retired before publication");
        scope.checkActive();return new Captured(owner,loaded.ownerBinding(),physical,fighter);
    }
}
