    /** Source spawnT keeps the old actor visible until replacement creation succeeds.
     * Fixed-position spawning allows a shared tile; this is not a death/reward path.
     * Caller owns the population across spawn and delete, as well as its scene lifetime.
     */
    synchronized Snapshot nativeCancellationSpawn(int mapId, long originalId,
            ClassicWorldMonsterCatalog.MonsterTemplate template) {
        Actor original = actors.get(originalId);
        if (original == null || !original.alive || !original.present
                || original.definition.mapId() != mapId || template == null
                || nextQuestDynamicObjectId <= 0
                || nextQuestDynamicObjectId > questDynamicObjectEnd
                || actors.containsKey(nextQuestDynamicObjectId)) return null;
        long objectId = nextQuestDynamicObjectId;
        var definition = new ClassicWorldMonsterCatalog.SpawnDefinition(
                mapId, original.definition.spawnId(), "cancellation-runtime-spawn", 1, 0,
                original.x, original.y, 0, 0, 0, 0, 0, 0, original.heading,
                0, 0, false, 30, false, 0, template);
        Actor replacement = new Actor(definition, 1, objectId,
                new Random(mix64(objectId)), true, List.of());
        replacement.x = original.x;
        replacement.y = original.y;
        replacement.homeX = original.x;
        replacement.homeY = original.y;
        replacement.heading = original.heading;
        Snapshot created = replacement.snapshot();
        // All construction/validation precedes population mutation.
        occupy(new Cell(mapId, replacement.x, replacement.y));
        actors.put(objectId, replacement);
        nextQuestDynamicObjectId++;
        return created;
    }

    /** Delete without attack, death rewards or corpse drops; reuse existing retirement.
     * The adapter still owns respawn scheduling and expiration publication.
     */
    synchronized Snapshot nativeCancellationDelete(int mapId, long objectId) {
        Actor actor = actors.get(objectId);
        if (actor == null || !actor.present || actor.definition.mapId() != mapId) return null;
        if (actor.alive) {
            actor.alive = false;
            actor.currentHp = 0;
            vacate(new Cell(mapId, actor.x, actor.y));
        }
        actor.releasedItems.clear();
        actor.digestSeconds.clear();
        digestActors.remove(objectId);
        return removeCorpse(mapId, objectId);
    }
