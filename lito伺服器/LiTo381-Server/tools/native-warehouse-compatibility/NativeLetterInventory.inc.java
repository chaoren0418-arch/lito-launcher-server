    // Shared only by copies of one loaded inventory. A rebuilt selection has
    // a different ledger and cannot spend an earlier selection's reservation.
    private static final class LetterLedger {
        int slots;
        final Map<Long,Integer> papers = new LinkedHashMap<>();
    }
    private LetterLedger letterLedger = new LetterLedger();
    static final class RemovalLease implements AutoCloseable {
        private final LetterLedger ledger;
        private final long objectId;
        private boolean active=true;
        private RemovalLease(LetterLedger ledger,long id) { this.ledger=ledger;objectId=id; }
        @Override public void close() { synchronized(ledger) {
            if(!active)return;active=false;
            ledger.papers.computeIfPresent(objectId,(k,v)->v==1?null:v-1);
        }}
    }
    static final class SlotLease implements AutoCloseable {
        private final LetterLedger ledger;
        private final int slots;
        private boolean active=true;
        private SlotLease(LetterLedger ledger,int slots) { this.ledger=ledger;this.slots=slots; }
        @Override public void close() { synchronized(ledger) {
            if(!active)return;active=false;ledger.slots-=slots;
        }}
    }
    RemovalLease reserveLetterPaper(long objectId) { synchronized(letterLedger) {
        Item paper=items.get(objectId);
        int held=letterLedger.papers.getOrDefault(objectId,0);
        if(paper==null || paper.definition().catalogId()!=InventoryCatalog.LETTER_PAPER
                || paper.quantity()<=held)return null;
        letterLedger.papers.put(objectId,held+1);
        return new RemovalLease(letterLedger,objectId);
    }}
    SlotLease reserveLetterSlot(int releasedSlots,int pending) { synchronized(letterLedger) {
        if(releasedSlots<0 || releasedSlots>1 || pending<0
                || items.size()+letterLedger.slots+pending+1-releasedSlots>MAX_SLOTS)return null;
        int slots=1-releasedSlots;letterLedger.slots+=slots;
        return new SlotLease(letterLedger,slots);
    }}
    boolean removeReservedLetterPaper(RemovalLease lease) { synchronized(letterLedger) {
        if(lease==null || lease.ledger!=letterLedger || !lease.active)return false;
        Item paper=items.get(lease.objectId);
        if(paper==null || paper.definition().catalogId()!=InventoryCatalog.LETTER_PAPER)return false;
        lease.close();return remove(lease.objectId,1);
    }}
    long addReservedLetter(InventoryCatalog.Definition definition,PersistentItem state,SlotLease lease) {
        synchronized(letterLedger) {
            if(lease==null || lease.ledger!=letterLedger || !lease.active)return 0;
            if(definition==null || state==null || state.quantity()!=1
                    || (definition.catalogId()!=InventoryCatalog.LETTER_CLOSED
                        && definition.catalogId()!=InventoryCatalog.LETTER_OPEN))return 0;
            letterLedger.slots-=lease.slots;
            long id=addDistinctState(definition,state);
            if(id!=0)lease.active=false;else letterLedger.slots+=lease.slots;
            return id;
        }
    }
    boolean reconcileLetterPaperQuantity(long objectId,int quantity) { synchronized(letterLedger) {
        Item paper=items.get(objectId);
        if(paper==null)return quantity==0;
        if(paper.definition().catalogId()!=InventoryCatalog.LETTER_PAPER || quantity<0
                || quantity>paper.quantity() || letterLedger.papers.containsKey(objectId))return false;
        if(quantity==0)items.remove(objectId);else items.put(objectId,paper.withQuantity(quantity));
        return true;
    }}
