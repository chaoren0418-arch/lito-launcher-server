package local.lito381.server;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Modern Tournament carrying scope and retained weight rules. Pure facts only:
 * callers still own source/account, selection, equipment lease and room CAS.
 * Source weapons/armor and match-local supplies are distinct stacks. Source
 * ETC/Adena are not imported into the Tournament bag or counted a second time.
 * Integer quantities/weights are checked i64, never native int saturation.
 * Capacity intentionally retains the source's ordered binary64 calculation,
 * not a claim of exact decimal/rational arithmetic or an imported live buff.
 */
final class ModernInventoryWeight {
    static final int ATTACK_CAST_LIMIT = 197;
    static final int NORMAL_REGEN_LIMIT = 120;
    static final int MAX_UNITS = 256;
    private static final BigInteger THOUSAND = BigInteger.valueOf(1000);

    record Parameters(int characterClass, long strength, long constitution,
                      long originalStrength, long originalConstitution,
                      long equipmentWeightReductionPercent, double sourceRate) {
        Parameters {
            if (characterClass < 0 || characterClass > 6 || strength < 0 || constitution < 0
                    || originalStrength < 1 || originalConstitution < 1
                    || !Double.isFinite(sourceRate) || sourceRate <= 0) throw invalid();
        }
    }

    record Basis(ModernCharacterData.Snapshot source, long equipmentRevision,
                 long sourceGearWeight, double maximumWeight, double sourceRate,
                 long validUntilMillis,long retainedUntilMillis) {
        /** An externally supplied/legacy deadline has no separable provenance.
         * Keep it across every rebuild; only qualify() creates a derived lease. */
        Basis(ModernCharacterData.Snapshot source,long equipmentRevision,long sourceGearWeight,
                double maximumWeight,double sourceRate,long validUntilMillis) {
            this(source,equipmentRevision,sourceGearWeight,maximumWeight,sourceRate,validUntilMillis,validUntilMillis);
        }
        Basis {
            Objects.requireNonNull(source);
            if (equipmentRevision < 0 || sourceGearWeight < 0 || validUntilMillis < -1 || retainedUntilMillis < -1
                    ||retainedUntilMillis>=0&&(validUntilMillis<0||validUntilMillis>retainedUntilMillis)
                    || !positive(maximumWeight) || !positive(sourceRate)) throw invalid();
        }
        Basis retaining(long deadline) {
            if(deadline< -1)throw invalid();
            return new Basis(source,equipmentRevision,sourceGearWeight,maximumWeight,sourceRate,
                    earlier(validUntilMillis,deadline),earlier(retainedUntilMillis,deadline));
        }
        /** Reprice only from the same immutable inventory. The profile and all
         * source-gear leases are reevaluated; a separate stock/catalog cap is not. */
        Basis requalify(ModernCombatProfile profile) {
            if(!source.equals(profile.source())||profile.equipmentRevision()<equipmentRevision)throw invalid();
            return qualify(profile,sourceRate).retaining(retainedUntilMillis);
        }
        /** Query-start clock anchoring may tighten the same lifetime deadline.
         * It must be rechecked on every commit, not churn the shop CAS token. */
        boolean sameFacts(Basis other) {
            return other != null && source.equals(other.source)
                    && equipmentRevision == other.equipmentRevision
                    && sourceGearWeight == other.sourceGearWeight
                    && bits(maximumWeight) == bits(other.maximumWeight)
                    && bits(sourceRate) == bits(other.sourceRate);
        }
        @Override public String toString() { return "ModernWeightBasis[redacted]"; }
    }

    record Unit(long itemId, long unitWeight) {
        Unit { if (itemId < 1 || unitWeight < 0) throw invalid(); }
    }
    /** Zero quantity retains a consumed world's original, source-qualified unit. */
    record Carried(long itemId, long quantity, long unitWeight) {
        Carried { if (itemId < 1 || quantity < 0 || unitWeight < 0) throw invalid(); }
    }
    record View(long sourceGearWeight, long suppliesWeight, long totalWeight,
                double maximumWeight, int stage) {
        View {
            if (sourceGearWeight < 0 || suppliesWeight < 0 || totalWeight < 0
                    || Math.addExact(sourceGearWeight, suppliesWeight) != totalWeight) throw invalid();
            if (bits(maximumWeight) == 0) {
                if (sourceGearWeight != 0 || suppliesWeight != 0 || totalWeight != 0 || stage != 0) throw invalid();
            } else if (!positive(maximumWeight) || stage != weight240(totalWeight, maximumWeight)) throw invalid();
        }
        boolean applicable() { return bits(maximumWeight) != 0; }
        boolean permitsAttackCast() { return stage < ATTACK_CAST_LIMIT; }
        int hitPenalty() { return ModernInventoryWeight.hitPenalty(stage); }
        /** This predicate does not implement the still separate natural regen timer. */
        boolean permitsNaturalRegen() { return stage < NORMAL_REGEN_LIMIT; }
    }
    record Snapshot(View view, List<Unit> units) {
        Snapshot {
            Objects.requireNonNull(view); units = List.copyOf(units);
            if (units.size() > MAX_UNITS || !view.applicable() && !units.isEmpty()) throw invalid();
            long previous = 0;
            for (Unit unit : units) {
                if (unit.itemId() <= previous) throw invalid();
                previous = unit.itemId();
            }
        }
    }

    private ModernInventoryWeight() {}

    static Basis qualify(ModernCombatProfile profile, double rate) {
        Objects.requireNonNull(profile);
        var source = profile.source();
        var saved = source.persisted();
        var effective = profile.effective();
        var parameters = new Parameters(source.character().characterClass(), effective.strength(), effective.constitution(),
                saved.originalStrength(), saved.originalConstitution(),
                profile.equipmentEvaluation().total(ModernEquipmentFacts.Stat.WEIGHT_REDUCTION), rate);
        return qualify(source, profile.equipmentRevision(), profile.sourceLifetimes(), profile.evaluatedAtMillis(),
                profile.validUntilMillis(), parameters);
    }

    /** Public gear receipts already retain all qualified lifetime views and
     * effective modifier totals; no client-supplied carrying capacity is used. */
    static Basis qualify(ModernEquipmentAuthority.Snapshot equipment, double rate) {
        Objects.requireNonNull(equipment);
        var source = equipment.source();
        var saved = source.persisted();
        var parameters = new Parameters(source.character().characterClass(),
                Math.addExact(saved.strength(), equipment.modifiers().get(ModernEquipmentFacts.Stat.STR)),
                Math.addExact(saved.constitution(), equipment.modifiers().get(ModernEquipmentFacts.Stat.CON)),
                saved.originalStrength(), saved.originalConstitution(),
                equipment.modifiers().get(ModernEquipmentFacts.Stat.WEIGHT_REDUCTION), rate);
        return qualify(source, equipment.revision(), equipment.lifetimes(), equipment.evaluatedAtMillis(),
                equipment.nextStoneExpiry(), parameters);
    }

    private static Basis qualify(ModernCharacterData.Snapshot source, long revision,
                                 List<ModernItemLifetime.View> lifetimes, long now, long validUntil,
                                 Parameters parameters) {
        if (now < 0 || validUntil >= 0 && now >= validUntil
                || lifetimes.size() != source.inventory().size()) throw invalid();
        long raw = 0;
        for (int i = 0; i < source.inventory().size(); i++) {
            var item = source.inventory().get(i);
            var life = lifetimes.get(i);
            if (life.itemObjectId() != item.objectId()) throw invalid();
            if (item.template().kind() == ModernCharacterData.ItemKind.ETC) continue;
            if (life.state() == ModernItemLifetime.State.EXPIRED) {
                if (life.expiresAtMillis() > now) throw invalid();
                continue;
            }
            if (!life.usable()) throw invalid();
            if (life.state() == ModernItemLifetime.State.ACTIVE) {
                if (now >= life.expiresAtMillis()) throw invalid();
                validUntil = validUntil < 0 ? life.expiresAtMillis() : Math.min(validUntil, life.expiresAtMillis());
            }
            raw = Math.addExact(raw, itemWeight(item.quantity(), item.template().unitWeight()));
        }
        return new Basis(source, revision, raw, maximumWeight(parameters), parameters.sourceRate(), validUntil,-1);
    }

    private static long earlier(long a,long b){return a<0?b:b<0?a:Math.min(a,b);}

    static double maximumWeight(Parameters p) {
        Objects.requireNonNull(p);
        // New match has no imported weightUP/decrease-weight/doll/live buffs.
        // Keep native operation order; introducing those effects needs its own
        // authoritative timed state, not a guessed constant in this basis.
        double base = 150.0 * Math.floor(0.6 * p.strength() + 0.4 * p.constitution() + 1.0);
        double original = 0.04 * (originalStrengthReduction(p.characterClass(), p.originalStrength())
                + originalConstitutionReduction(p.characterClass(), p.originalConstitution()));
        double maximum = base * (1.0 + p.equipmentWeightReductionPercent() / 100.0 + original);
        maximum *= p.sourceRate();
        if (!positive(maximum)) throw invalid();
        return maximum;
    }

    static long itemWeight(long quantity, long unitWeight) {
        if (quantity < 0 || unitWeight < 0) throw invalid();
        if (quantity == 0 || unitWeight == 0) return 0;
        return Math.max(quotient(quantity, unitWeight), 1L);
    }
    private static long quotient(long quantity, long unitWeight) {
        try { return Math.multiplyExact(quantity, unitWeight) / 1000L; }
        catch (ArithmeticException overflow) {
            // A wide product can have a representable quotient. Reject only
            // the actual output overflow, not a harmless intermediate product.
            return BigInteger.valueOf(quantity).multiply(BigInteger.valueOf(unitWeight))
                    .divide(THOUSAND).longValueExact();
        }
    }
    static int weight240(long raw, double maximum) {
        if (raw < 0 || !positive(maximum)) throw invalid();
        if (raw > maximum) return 240;
        long stage = Math.round(raw * 240.0 / maximum);
        if (stage < 0 || stage > 240) throw invalid();
        return (int) stage;
    }
    /** Exact Tournament checkAddItem projection, NOT classic shop whole-order. */
    static boolean permitsAdd(View current, long quantity, long unitWeight) {
        if (current == null || !current.applicable() || quantity < 1 || unitWeight < 0) return false;
        try {
            long projected = Math.addExact(current.totalWeight(), Math.addExact(quotient(quantity, unitWeight), 1));
            return weight240(projected, current.maximumWeight()) < 240;
        } catch (ArithmeticException overflow) { return false; }
    }

    static Snapshot evaluate(Basis basis, List<Carried> carried) {
        Objects.requireNonNull(basis); Objects.requireNonNull(carried);
        if (carried.size() > MAX_UNITS) throw invalid();
        long raw = 0, previous = 0;
        var units = new ArrayList<Unit>(carried.size());
        for (Carried stack : carried) {
            if (stack.itemId() <= previous) throw invalid();
            previous = stack.itemId();
            raw = Math.addExact(raw, itemWeight(stack.quantity(), stack.unitWeight()));
            units.add(new Unit(stack.itemId(), stack.unitWeight()));
        }
        long total = Math.addExact(basis.sourceGearWeight(), raw);
        return new Snapshot(new View(basis.sourceGearWeight(), raw, total, basis.maximumWeight(),
                weight240(total, basis.maximumWeight())), units);
    }
    static Snapshot fromShop(Basis basis, List<ModernShopAuthority.Stack> stacks) {
        return evaluate(basis, stacks.stream().map(s ->
                new Carried(s.offer().itemId(), s.quantity(), s.offer().weight())).toList());
    }
    static Snapshot world(Basis basis, List<ModernItemProtocol.Stock> stocks, List<Unit> units) {
        if (stocks.size() != units.size()) throw invalid();
        var carried = new ArrayList<Carried>(stocks.size());
        for (int i = 0; i < stocks.size(); i++) {
            var stock = stocks.get(i); var unit = units.get(i);
            if (stock.itemId() != unit.itemId()) throw invalid();
            carried.add(new Carried(stock.itemId(), stock.remaining(), unit.unitWeight()));
        }
        return evaluate(basis, carried);
    }
    static Snapshot world(Basis basis,List<ModernItemProtocol.Stock> stocks,List<ModernAmmo.Stock> ammo,List<Unit> units) {
        if(ammo.isEmpty())return world(basis,stocks,units);
        var quantities=new java.util.TreeMap<Long,Long>();for(var s:stocks)if(quantities.put(s.itemId(),s.remaining())!=null)throw invalid();
        for(var s:ammo)if(quantities.put(s.itemId(),s.remaining())!=null)throw invalid();
        if(quantities.size()!=units.size()||units.size()>9)throw invalid();var carried=new ArrayList<Carried>();
        for(var unit:units){var q=quantities.remove(unit.itemId());if(q==null)throw invalid();carried.add(new Carried(unit.itemId(),q,unit.unitWeight()));}
        if(!quantities.isEmpty())throw invalid();return evaluate(basis,carried);
    }
    /** Material-aware candidate; the legacy overload still rejects new stock. */
    static Snapshot world(Basis basis,List<ModernItemProtocol.Stock> items,List<ModernAmmo.Stock> ammo,
            List<ModernSpellMaterials.Stock> materials,List<Unit> units) {
        if(materials.isEmpty())return world(basis,items,ammo,units);
        if(items.size()>ModernItemProtocol.MAX_TIMED_STOCKS||ammo.size()>ModernAmmo.MAX_STOCKS
                ||materials.size()>ModernSpellMaterials.MAX_STOCKS||units.size()>10)throw invalid();
        var quantities=new java.util.TreeMap<Long,Long>();
        for(var s:items)if(quantities.put(s.itemId(),s.remaining())!=null)throw invalid();
        for(var s:ammo)if(quantities.put(s.itemId(),s.remaining())!=null)throw invalid();
        for(var s:materials)if(quantities.put(s.itemId(),s.remaining())!=null)throw invalid();
        if(quantities.size()!=units.size())throw invalid();
        var carried=new ArrayList<Carried>();
        for(var u:units) {
            var quantity=quantities.remove(u.itemId());if(quantity==null)throw invalid();
            for(var material:materials)if(material.itemId()==u.itemId()&&material.facts().unitWeight()!=u.unitWeight())throw invalid();
            carried.add(new Carried(u.itemId(),quantity,u.unitWeight()));
        }
        if(!quantities.isEmpty())throw invalid();
        return evaluate(basis,carried);
    }
    static Snapshot notApplicable() { return new Snapshot(new View(0, 0, 0, 0.0, 0), List.of()); }
    static int hitPenalty(int stage) {
        if (stage < 0 || stage > 240) throw invalid();
        return stage >= 81 && stage <= 120 ? 1 : stage >= 121 && stage <= 160 ? 3
                : stage >= 161 && stage <= 200 ? 5 : 0;
    }
    static int originalStrengthReduction(int type, long value) {
        if (type < 0 || type > 6 || value < 1) throw invalid();
        return switch (type) {
            case 0 -> value <= 13 ? 0 : value <= 16 ? 1 : value <= 19 ? 2 : 3;
            case 1 -> 0;
            case 2 -> value <= 15 ? 0 : 2;
            case 3 -> value <= 9 ? 0 : 1;
            case 4 -> value <= 12 ? 0 : value <= 15 ? 2 : 3;
            case 5 -> value <= 15 ? 0 : 1;
            default -> value <= 17 ? 0 : 1;
        };
    }
    static int originalConstitutionReduction(int type, long value) {
        if (type < 0 || type > 6 || value < 1) throw invalid();
        return switch (type) {
            case 0 -> value <= 10 ? 0 : 1;
            case 1 -> value <= 14 ? 0 : 1;
            case 2 -> value <= 14 ? 0 : 2;
            case 3 -> value <= 12 ? 0 : value <= 14 ? 1 : 2;
            case 4 -> value <= 8 ? 0 : 1;
            case 5 -> 0;
            default -> value <= 16 ? 0 : value == 17 ? 1 : 2;
        };
    }
    private static boolean positive(double value) { return Double.isFinite(value) && value > 0; }
    private static long bits(double value) { return Double.doubleToRawLongBits(value); }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("invalid modern Tournament weight facts"); }
}
