    RuntimeInventory nativeLetterInventory() { return inventory; }
    long nativeLetterSelectedObjectId() {
        CharacterSummary selected=selectedCharacterSummary();
        return selected==null ? 0 : selected.objectId();
    }
    String nativeLetterContext() {
        return nativeClanContext()+"\u0000"+nativeLetterSelectedObjectId()+"\u0000"+inventoryCharacter;
    }
    boolean nativeLetterRecipientReady() {
        return transportOpen() && sqlLetterSnapshot.authoritative()
                && inventoryCharacter.equalsIgnoreCase(stateCharacter());
    }
    boolean nativeLetterSenderReady(RuntimeInventory.Item paper) {
        return nativeLetterRecipientReady() && phase==Phase.BATTLE && playerHp>0 && !observer
                && paper!=null && paper.equals(inventory.item(paper.objectId()))
                && paper.definition().catalogId()==InventoryCatalog.LETTER_PAPER;
    }
    private void useSqlLetterPaper(RuntimeInventory.Item paper,
                                   Protocol.LetterComposeInventoryUse request,
                                   ClientSession target) throws Exception {
        CharacterSummary senderCharacter=selectedCharacterSummary();
        if(senderCharacter==null || target!=null && !target.playerName().equalsIgnoreCase(request.receiver()))return;
        AccountRepository source=selectedSourceAccounts();
        long sqlPaperObjectId=state.sourceInventoryBindings(stateCharacter()).stream()
                .filter(binding->binding.localObjectId()==paper.objectId())
                .mapToLong(GameStateStore.SourceInventoryBinding::sourceObjectId)
                .findFirst().orElse(paper.objectId());
        GameHub.NativeLetterPermit permit=hub.beginNativeLetterDelivery(this,paper,sqlPaperObjectId,target);
        if(permit==null) {
            sendEncrypted(Protocol.messageCode(942),"S_MESSAGE_CODE SQL-letter-runtime-admission-refused=942");
            return;
        }
        try(permit) {
            if(!permit.senderCurrent() || target!=null && !permit.recipientCurrent())return;
            String date=CLASSIC_BOARD_DATE.format(ZonedDateTime.now(CLASSIC_BOARD_ZONE));
            RepositoryLetterDelivery delivery=source.deliverLetter(senderCharacter.objectId(),
                    sqlPaperObjectId,request.receiver(),request.letterCode(),date,request.subject(),request.content());
            if(!delivery.delivered()) {
                if(permit.senderCurrent()) synchronized(this) {
                    if(permit.senderSameContext()) {
                        if(delivery.status()==RepositoryLetterDeliveryStatus.RECIPIENT_NOT_FOUND)
                            sendEncrypted(Protocol.messageCode(109,request.receiver()),"SQL-letter-recipient-not-found");
                        else if(delivery.status()==RepositoryLetterDeliveryStatus.RECIPIENT_FULL)
                            sendEncrypted(Protocol.messageCode(942),"SQL-letter-recipient-full");
                    }
                }
                return;
            }
            // Register obligations before touching runtime or writing packets.
            // SQL success is never compensated because a selected runtime moved.
            GameHub.NativeLetterReceipt receipt=permit.committed(delivery.letter());
            boolean removed=false;
            try { if(permit.senderCurrent()) synchronized(this) {
                if(permit.senderSameContext()) {
                    RuntimeInventory before=inventory,after=inventory.copy();
                    if(after.removeReservedLetterPaper(permit.removal())) {
                        inventory=after;removed=true;saveSqlLetterEconomyBestEffort();
                        if(inventoryBootstrapSent)sendInventoryDelta(before,after,false);
                    }
                }
            }} catch(IOException | RuntimeException deferred) {
                System.out.println("[LiTo] durable SQL-letter sender runtime/output deferred: "+deferred.getMessage());
            }
            if(removed)permit.senderInstalled();
            try {
                if(target!=null && (target!=this || removed))target.receiveReservedSqlLetter(permit,receipt,source);
            } catch(Exception deferred) {
                System.out.println("[LiTo] durable SQL-letter recipient runtime/output deferred: "+deferred.getMessage());
            }
            System.out.println("[LiTo] SQL physical-letter durable delivered object="+delivery.letter().itemObjectId());
        } finally {
            reconcileNativeLettersBestEffort();
            if(target!=null && target!=this)target.reconcileNativeLettersBestEffort();
        }
    }
    private void receiveReservedSqlLetter(GameHub.NativeLetterPermit permit,
                                          GameHub.NativeLetterReceipt receipt,AccountRepository source) throws Exception {
        if(receipt==null || !permit.recipientCurrent())return;
        RepositoryLetterSnapshot authoritative=source.letters(permit.recipientSqlObjectId());
        if(!authoritative.authoritative())return;
        RepositoryLetter letter=authoritative.letters().stream()
                .filter(row->row.itemObjectId()==receipt.committed().itemObjectId()).findFirst().orElse(null);
        if(letter==null || !permit.recipientCurrent())return;
        boolean installed=false;
        synchronized(this) {
            if(!permit.recipientSameContext())return;
            RuntimeInventory before=inventory,after=inventory.copy();
            if(addSqlLetterObject(after,letter,permit.slot())) {
                inventory=after;installSqlLetterCache(letter);installed=true;
                if(inventoryBootstrapSent)sendInventoryDelta(before,after,false);
                if(phase==Phase.BATTLE)sendLetterDeliveryNotice(letter.gameStateLetter());
            }
        }
        if(installed)hub.nativeLetterReceiptInstalled(receipt);
    }
    private void installSqlLetterCache(RepositoryLetter letter) {
        reconcileSqlLetter(letter);
        letterObjectBindings.put(letter.itemObjectId(),letter.itemObjectId());
        saveSqlLetterEconomyBestEffort();
    }
    private static boolean addSqlLetterObject(RuntimeInventory target,RepositoryLetter letter,
                                              RuntimeInventory.SlotLease slot) {
        InventoryCatalog.Definition definition=InventoryCatalog.require(letter.opened()
                ? InventoryCatalog.LETTER_OPEN : InventoryCatalog.LETTER_CLOSED);
        RuntimeInventory.PersistentItem state=new RuntimeInventory.PersistentItem(letter.itemObjectId(),
                1,0,0,true,definition.blessing(),true,false,false,null,
                definition.initialChargeCount(),0L,false,true);
        return target.addReservedLetter(definition,state,slot)==letter.itemObjectId();
    }
    private void reconcileNativeLettersBestEffort() {
        try { reconcileNativeLetters(); }
        catch(Exception failure) {
            System.out.println("[LiTo] SQL-letter runtime reconcile deferred: "+failure.getMessage());
        }
    }
    private void reconcileNativeLetters() throws Exception {
        GameHub.NativeLetterReconcile work=hub.nativeLetterReconcile(this);
        if(work==null || work.receipts().isEmpty() && work.debts().isEmpty())return;
        long characterId=nativeLetterSelectedObjectId();AccountRepository source=selectedSourceAccounts();
        // Reads outside the Hub and session transport monitors.
        RepositoryLetterSnapshot letters=work.receipts().isEmpty() ? null : source.letters(characterId);
        List<SourceInventoryItem> sourceItems=work.debts().isEmpty() ? null : source.sourceInventory(characterId);
        if(!work.current())return;
        List<GameHub.NativeLetterReceipt> installed=new ArrayList<>();
        List<GameHub.NativeLetterSenderDebt> settled=new ArrayList<>();
        synchronized(this) {
            if(!work.sameContext())return;
            if(sourceItems!=null) for(GameHub.NativeLetterSenderDebt debt:work.debts()) {
                long localId=state.sourceInventoryBindings(stateCharacter()).stream()
                        .filter(b->b.sourceObjectId()==debt.sqlPaperObjectId())
                        .mapToLong(GameStateStore.SourceInventoryBinding::localObjectId)
                        .findFirst().orElse(debt.sqlPaperObjectId());
                SourceInventoryItem row=sourceItems.stream().filter(i->i.sourceObjectId()==debt.sqlPaperObjectId()).findFirst().orElse(null);
                if(row!=null && row.catalogId()!=InventoryCatalog.LETTER_PAPER)continue;
                RuntimeInventory before=inventory,after=inventory.copy();
                if(after.reconcileLetterPaperQuantity(localId,row==null?0:row.quantity())) {
                    inventory=after;saveSqlLetterEconomyBestEffort();settled.add(debt);
                    if(inventoryBootstrapSent)sendInventoryDelta(before,after,false);
                }
            }
            if(letters!=null && letters.authoritative())for(GameHub.NativeLetterReceipt receipt:work.receipts()) {
                RepositoryLetter letter=letters.letters().stream().filter(r->r.itemObjectId()==receipt.committed().itemObjectId()).findFirst().orElse(null);
                if(letter==null) { installed.add(receipt);continue; }
                RuntimeInventory before=inventory,after=inventory.copy();
                RuntimeInventory.Item existing=after.item(letter.itemObjectId());
                if(existing!=null) {
                    long catalog=existing.definition().catalogId();
                    if(catalog!=InventoryCatalog.LETTER_OPEN && catalog!=InventoryCatalog.LETTER_CLOSED)continue;
                    if(!java.util.Objects.equals(letterObjectBindings.get(letter.itemObjectId()),letter.itemObjectId()))continue;
                    after.replaceDefinition(letter.itemObjectId(),InventoryCatalog.require(letter.opened()
                            ? InventoryCatalog.LETTER_OPEN : InventoryCatalog.LETTER_CLOSED));
                } else {
                    RuntimeInventory.SlotLease slot=after.reserveLetterSlot(0,0);
                    if(slot==null)continue;
                    try(slot) { if(!addSqlLetterObject(after,letter,slot))continue; }
                }
                inventory=after;installSqlLetterCache(letter);installed.add(receipt);
                if(inventoryBootstrapSent)sendInventoryDelta(before,after,false);
                if(existing==null && phase==Phase.BATTLE)sendLetterDeliveryNotice(letter.gameStateLetter());
            }
        }
        for(GameHub.NativeLetterReceipt receipt:installed)work.installed(receipt);
        for(GameHub.NativeLetterSenderDebt debt:settled)work.installed(debt);
    }
