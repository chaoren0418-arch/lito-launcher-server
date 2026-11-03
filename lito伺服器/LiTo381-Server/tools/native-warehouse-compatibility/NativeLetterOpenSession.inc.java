    private void useSqlPhysicalLetter(RuntimeInventory.Item item) throws IOException {
        GameHub.NativeLetterReconcile admission = hub.nativeLetterReconcile(this);
        if (admission == null || !admission.current()
                || item == null || !item.equals(inventory.item(item.objectId()))) return;
        RepositoryLetter expected = sqlLetterSnapshot.letters().stream()
                .filter(letter -> letter.itemObjectId() == item.objectId())
                .findFirst().orElse(null);
        if (expected == null) return;
        CharacterSummary character = selectedCharacterSummary();
        if (character == null) return;
        AccountRepository source = selectedSourceAccounts();
        if (!admission.current()) return;
        RepositoryLetter letter = null;
        boolean transitioned = item.definition().effect() == InventoryCatalog.Effect.LETTER_CLOSED;
        try {
            if (transitioned) {
                RepositoryLetterMutation mutation = source.openLetter(character.objectId(), expected);
                if (mutation.status() != RepositoryLetterMutationStatus.SUCCESS
                        && mutation.status() != RepositoryLetterMutationStatus.ALREADY_OPEN) return;
                letter = mutation.letter();
            } else {
                // An opened letter still requires current physical ownership and
                // an authoritative source read; a removed source body is not read
                // back from a stale session cache.
                RepositoryLetterSnapshot snapshot = source.letters(character.objectId());
                if (!snapshot.authoritative()) return;
                for (RepositoryLetter row : snapshot.letters()) {
                    if (row.itemObjectId() == item.objectId()) { letter = row; break; }
                }
            }
        } catch (Exception failure) {
            throw new IOException("cannot read SQL physical letter object=" + item.objectId(), failure);
        }
        if (letter == null || !letter.opened() || letter.itemObjectId() != item.objectId()
                || !admission.current()) return;
        synchronized (this) {
            if (!admission.sameContext() || !transportOpen()
                    || !item.equals(inventory.item(item.objectId()))) return;
            RuntimeInventory next = inventory.copy();
            if (!next.replaceDefinition(item.objectId(), InventoryCatalog.require(InventoryCatalog.LETTER_OPEN))) return;
            inventory = next;
            reconcileSqlLetter(letter);
            saveSqlLetterEconomyBestEffort();
            sendEncrypted(Protocol.readMail(item.objectId(),
                            InventoryCatalog.LETTER_OPEN_INVENTORY_GFX,
                            letter.code(), letter.sender(), letter.receiver(), letter.subject(),
                            letter.content(), letter.templateId(), letter.date()),
                    "S_READ_MAIL SQL object=" + item.objectId()
                            + " opened=" + letter.opened() + " transitioned=" + transitioned);
        }
    }

