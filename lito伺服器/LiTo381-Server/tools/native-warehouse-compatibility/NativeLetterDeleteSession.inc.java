    private void destroySqlPhysicalLetter(RuntimeInventory.Item existing) throws IOException {
        GameHub.NativeLetterReconcile admission = hub.nativeLetterReconcile(this);
        if (admission == null || !admission.current()
                || existing == null || !existing.equals(inventory.item(existing.objectId()))) return;
        RepositoryLetter expected = sqlLetterSnapshot.letters().stream()
                .filter(letter -> letter.itemObjectId() == existing.objectId())
                .findFirst().orElse(null);
        if (expected == null) return;
        CharacterSummary character = selectedCharacterSummary();
        if (character == null) return;
        AccountRepository source = selectedSourceAccounts();
        if (!admission.current()) return;
        RepositoryLetterMutation mutation;
        try {
            // Source route and physical owner are captured before I/O; the
            // original source deletion remains durable after context revocation.
            mutation = source.deleteLetter(character.objectId(), expected);
        } catch (Exception failure) {
            throw new IOException("cannot delete SQL physical letter object="
                    + existing.objectId(), failure);
        }
        if (mutation.status() != RepositoryLetterMutationStatus.SUCCESS) return;
        if (!admission.current()) return;
        synchronized (this) {
            if (!admission.sameContext() || !transportOpen()) return;
            RuntimeInventory.Item current = inventory.item(existing.objectId());
            if (current != null && !existing.equals(current)) return;
            RuntimeInventory previous = inventory;
            RuntimeInventory next = inventory.copy();
            if (current != null) next.removeEntire(existing.objectId());
            inventory = next;
            removeSqlLetter(existing.objectId());
            saveSqlLetterEconomyBestEffort();
            // Only the captured source's physical removal is published while
            // holding the transport monitor; no wider status computation here.
            sendInventoryDelta(previous, inventory, false);
        }
        System.out.println("[LiTo] C_DESTROY_ITEM SQL physical-letter object=" + existing.objectId());
    }

