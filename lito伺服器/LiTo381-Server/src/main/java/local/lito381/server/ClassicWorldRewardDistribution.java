package local.lito381.server;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Pure retained {@code CalcExp.calcExpWithNpc} player/companion split. */
final class ClassicWorldRewardDistribution {
    static final double VISIBLE_PARTY_MEMBER_BONUS = 0.04D;
    static final double VISIBLE_CROWN_LEADER_BONUS = 0.059D;

    enum RecipientKind {
        PLAYER,
        PET
    }

    record Player(
            long objectId,
            int level,
            long hate,
            boolean inKillerParty,
            boolean visibleToKiller) {
        Player {
            if (objectId <= 0 || level < 1 || hate < 0) {
                throw new IllegalArgumentException("invalid classic WORLD reward player");
            }
        }

        long squaredLevel() {
            return Math.multiplyExact((long) level, level);
        }
    }

    record Companion(
            long objectId,
            long ownerObjectId,
            ClassicWorldMonsterPopulation.ContributorKind kind,
            long hate) {
        Companion {
            if (objectId <= 0 || ownerObjectId <= 0 || hate < 0
                    || (kind != ClassicWorldMonsterPopulation.ContributorKind.PET
                        && kind != ClassicWorldMonsterPopulation.ContributorKind.SUMMON)) {
                throw new IllegalArgumentException("invalid classic WORLD reward companion");
            }
        }

        boolean earnsExperience() {
            return kind == ClassicWorldMonsterPopulation.ContributorKind.PET;
        }
    }

    record Award(
            RecipientKind recipientKind,
            long objectId,
            long ownerObjectId,
            long baseExperience,
            int sourceLawfulShare,
            long hate,
            long totalHate,
            boolean partyAward) {
        Award {
            if (recipientKind == null || objectId <= 0 || ownerObjectId <= 0
                    || (recipientKind == RecipientKind.PLAYER
                        && objectId != ownerObjectId)
                    || baseExperience < 0 || hate < 0 || totalHate <= 0
                    || hate > totalHate
                    || (recipientKind == RecipientKind.PET
                        && sourceLawfulShare != 0)) {
                throw new IllegalArgumentException("invalid classic WORLD reward award");
            }
        }
    }

    private ClassicWorldRewardDistribution() {}

    static List<Award> distribute(
            long sourceExperience,
            int sourceLawful,
            long killerObjectId,
            boolean killerInParty,
            boolean visibleCrownPartyLeader,
            List<Player> players) {
        return distribute(sourceExperience, sourceLawful, killerObjectId,
                killerInParty, visibleCrownPartyLeader, players, List.of());
    }

    static List<Award> distribute(
            long sourceExperience,
            int sourceLawful,
            long killerObjectId,
            boolean killerInParty,
            boolean visibleCrownPartyLeader,
            List<Player> players,
            List<Companion> companions) {
        if (sourceExperience < 0 || killerObjectId <= 0
                || players == null || companions == null) {
            throw new IllegalArgumentException("invalid classic WORLD reward input");
        }
        List<Player> eligible = List.copyOf(players);
        List<Companion> eligibleCompanions = List.copyOf(companions);
        if (eligible.stream().map(Player::objectId).distinct().count() != eligible.size()
                || eligibleCompanions.stream().map(Companion::objectId).distinct().count()
                    != eligibleCompanions.size()) {
            throw new IllegalArgumentException("duplicate classic WORLD reward actor");
        }
        Map<Long, Player> playerById = new LinkedHashMap<>();
        for (Player player : eligible) playerById.put(player.objectId(), player);
        for (Companion companion : eligibleCompanions) {
            if (!playerById.containsKey(companion.ownerObjectId())
                    || playerById.containsKey(companion.objectId())) {
                throw new IllegalArgumentException("classic WORLD companion owner unavailable");
            }
        }
        Player killer = playerById.get(killerObjectId);
        if (killer == null || (killerInParty && !killer.inKillerParty())) {
            throw new IllegalArgumentException("classic WORLD reward killer unavailable");
        }

        long totalHate = eligible.stream().mapToLong(Player::hate)
                .reduce(0L, Math::addExact);
        totalHate = Math.addExact(totalHate, eligibleCompanions.stream()
                .mapToLong(Companion::hate).reduce(0L, Math::addExact));
        if (totalHate <= 0) return List.of();
        long totalLawfulHate = eligible.stream().mapToLong(Player::hate)
                .reduce(0L, Math::addExact);

        if (!killerInParty) {
            List<Award> direct = new ArrayList<>();
            for (Player player : eligible) {
                if (player.hate() <= 0) continue;
                direct.add(playerAward(player.objectId(),
                        proportionalShare(sourceExperience, player.hate(), totalHate),
                        totalLawfulHate == 0 ? 0 : Math.toIntExact(
                                signedProportionalShare(sourceLawful,
                                        player.hate(), totalLawfulHate)),
                        player.hate(), totalHate, false));
            }
            for (Companion companion : eligibleCompanions) {
                if (companion.hate() <= 0 || !companion.earnsExperience()) continue;
                direct.add(petAward(companion,
                        proportionalShare(sourceExperience, companion.hate(), totalHate),
                        totalHate, false));
            }
            direct.sort(AWARD_ORDER);
            return List.copyOf(direct);
        }

        long partyHate = 0;
        long partyLawfulHate = 0;
        for (Player player : eligible) {
            if (!player.inKillerParty()) continue;
            partyHate = Math.addExact(partyHate, player.hate());
            partyLawfulHate = Math.addExact(partyLawfulHate, player.hate());
        }
        for (Companion companion : eligibleCompanions) {
            if (playerById.get(companion.ownerObjectId()).inKillerParty()) {
                partyHate = Math.addExact(partyHate, companion.hate());
            }
        }

        List<Award> result = new ArrayList<>();
        for (Player player : eligible) {
            if (player.inKillerParty() || player.hate() <= 0) continue;
            result.add(playerAward(player.objectId(),
                    proportionalShare(sourceExperience, player.hate(), totalHate),
                    totalLawfulHate == 0 ? 0 : Math.toIntExact(
                            signedProportionalShare(sourceLawful,
                                    player.hate(), totalLawfulHate)),
                    player.hate(), totalHate, false));
        }
        for (Companion companion : eligibleCompanions) {
            Player owner = playerById.get(companion.ownerObjectId());
            if (owner.inKillerParty() || companion.hate() <= 0
                    || !companion.earnsExperience()) continue;
            result.add(petAward(companion,
                    proportionalShare(sourceExperience, companion.hate(), totalHate),
                    totalHate, false));
        }
        if (partyHate <= 0) {
            result.sort(AWARD_ORDER);
            return List.copyOf(result);
        }

        List<Player> partyRecipients = eligible.stream()
                .filter(Player::inKillerParty)
                .filter(player -> player.objectId() == killerObjectId
                        || player.visibleToKiller())
                .toList();
        long partyLevel = partyRecipients.stream().mapToLong(Player::squaredLevel)
                .reduce(0L, Math::addExact);
        if (partyLevel <= 0) {
            result.sort(AWARD_ORDER);
            return List.copyOf(result);
        }

        long partyExperience = proportionalShare(sourceExperience, partyHate, totalHate);
        long visiblePeers = partyRecipients.stream()
                .filter(player -> player.objectId() != killerObjectId)
                .filter(Player::visibleToKiller)
                .count();
        double bonus = 1.0D + visiblePeers * VISIBLE_PARTY_MEMBER_BONUS
                + (visibleCrownPartyLeader ? VISIBLE_CROWN_LEADER_BONUS : 0.0D);
        partyExperience = (long) (partyExperience * bonus);
        long partyLawful = totalLawfulHate == 0 ? 0
                : signedProportionalShare(
                        sourceLawful, partyLawfulHate, totalLawfulHate);

        for (Player recipient : partyRecipients) {
            double distance = recipient.squaredLevel() / (double) partyLevel;
            long memberExperience = (long) (partyExperience * distance);
            int memberLawful = Math.toIntExact((long) (partyLawful * distance));
            List<Companion> owned = eligibleCompanions.stream()
                    .filter(companion -> companion.ownerObjectId() == recipient.objectId())
                    .toList();
            long ownHate = recipient.hate();
            for (Companion companion : owned) {
                ownHate = Math.addExact(ownHate, companion.hate());
            }
            if (ownHate == 0) {
                result.add(playerAward(recipient.objectId(), memberExperience,
                        memberLawful, 0, totalHate, true));
                continue;
            }
            if (recipient.hate() > 0) {
                result.add(playerAward(recipient.objectId(), proportionalShare(
                                memberExperience, recipient.hate(), ownHate),
                        memberLawful, recipient.hate(), totalHate, true));
            }
            for (Companion companion : owned) {
                if (companion.hate() <= 0 || !companion.earnsExperience()) continue;
                result.add(petAward(companion, proportionalShare(
                                memberExperience, companion.hate(), ownHate),
                        totalHate, true));
            }
        }
        result.sort(AWARD_ORDER);
        return List.copyOf(result);
    }

    private static Award playerAward(long objectId, long experience, int lawful,
                                     long hate, long totalHate, boolean party) {
        return new Award(RecipientKind.PLAYER, objectId, objectId,
                experience, lawful, hate, totalHate, party);
    }

    private static Award petAward(Companion companion, long experience,
                                  long totalHate, boolean party) {
        return new Award(RecipientKind.PET, companion.objectId(),
                companion.ownerObjectId(), experience, 0,
                companion.hate(), totalHate, party);
    }

    private static final Comparator<Award> AWARD_ORDER =
            Comparator.comparingLong(Award::ownerObjectId)
                    .thenComparing(Award::recipientKind)
                    .thenComparingLong(Award::objectId);

    static long proportionalShare(long value, long part, long total) {
        if (value < 0 || part < 0 || total <= 0 || part > total) {
            throw new IllegalArgumentException("invalid proportional classic reward");
        }
        return Math.addExact(Math.multiplyExact(value / total, part),
                Math.multiplyExact(value % total, part) / total);
    }

    private static long signedProportionalShare(long value, long part, long total) {
        if (part < 0 || total <= 0 || part > total) {
            throw new IllegalArgumentException("invalid signed proportional classic reward");
        }
        return Math.addExact(Math.multiplyExact(value / total, part),
                Math.multiplyExact(value % total, part) / total);
    }
}
