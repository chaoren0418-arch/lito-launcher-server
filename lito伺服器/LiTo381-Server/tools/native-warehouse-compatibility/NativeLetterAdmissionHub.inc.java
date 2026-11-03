    private record NativeLetterSource(String identity, long sqlCharacterObjectId) {}
    private final Map<NativeLetterSource, NativeLetterPermit> nativeLetterSenders = new LinkedHashMap<>();
    private final Map<NativeLetterSource, Map<Long, NativeLetterReceipt>> nativeLetterReceipts = new LinkedHashMap<>();
    private final Map<NativeLetterSource, Map<Long, NativeLetterSenderDebt>> nativeLetterSenderDebts = new LinkedHashMap<>();

    synchronized NativeLetterPermit beginNativeLetterDelivery(ClientSession sender,
            RuntimeInventory.Item paper, long sqlPaperObjectId, ClientSession recipient) {
        TradeBinding senderBinding=nativeTradeBinding(sender);
        if (senderBinding==null || !sender.nativeLetterSenderReady(paper)) return null;
        NativeLetterSource senderSource=nativeLetterSource(sender);
        if (senderSource==null || nativeLetterSenders.containsKey(senderSource)
                || !nativeLetterSenderDebts.getOrDefault(senderSource,Map.of()).isEmpty()) return null;
        TradeBinding recipientBinding=recipient==null ? null : nativeTradeBinding(recipient);
        NativeLetterSource recipientSource=recipient==null ? null : nativeLetterSource(recipient);
        if (recipient!=null && (recipientBinding==null || recipientSource==null
                || !recipient.nativeLetterRecipientReady()
                || !sameNativeClanSource(senderSource.identity(),recipientSource.identity()))) return null;
        RuntimeInventory.RemovalLease removal=sender.nativeLetterInventory().reserveLetterPaper(paper.objectId());
        if (removal==null) return null;
        RuntimeInventory.SlotLease slot=null;
        if (recipient!=null) {
            int pending=nativeLetterReceipts.getOrDefault(recipientSource,Map.of()).size();
            int released=recipient==sender && paper.quantity()==1 ? 1 : 0;
            slot=recipient.nativeLetterInventory().reserveLetterSlot(released,pending);
            if (slot==null) { removal.close(); return null; }
        }
        NativeLetterPermit permit=new NativeLetterPermit(sender,senderBinding,senderSource,
                recipient,recipientBinding,recipientSource,sqlPaperObjectId,removal,slot);
        nativeLetterSenders.put(senderSource,permit);
        return permit;
    }

    private NativeLetterSource nativeLetterSource(ClientSession session) {
        long id=session.nativeLetterSelectedObjectId();
        return id<=0 ? null : new NativeLetterSource(key(session.sourceCharacterIdentity()),id);
    }

    final class NativeLetterPermit implements AutoCloseable {
        private final ClientSession sender,recipient;
        private final TradeBinding senderBinding,recipientBinding;
        private final NativeLetterSource senderSource,recipientSource;
        private final String senderContext,recipientContext;
        private final Object senderVersion,recipientVersion;
        private final long sqlPaperObjectId;
        private final RuntimeInventory.RemovalLease removal;
        private final RuntimeInventory.SlotLease slot;
        private boolean completed,closed;
        private NativeLetterPermit(ClientSession s,TradeBinding sb,NativeLetterSource ss,
                ClientSession r,TradeBinding rb,NativeLetterSource rs,long paper,
                RuntimeInventory.RemovalLease removal,RuntimeInventory.SlotLease slot) {
            sender=s;senderBinding=sb;senderSource=ss;recipient=r;recipientBinding=rb;recipientSource=rs;
            senderContext=s.nativeLetterContext();senderVersion=s.nativeClanSelectionVersion();
            recipientContext=r==null ? "" : r.nativeLetterContext();
            recipientVersion=r==null ? null : r.nativeClanSelectionVersion();
            sqlPaperObjectId=paper;this.removal=removal;this.slot=slot;
        }
        boolean senderCurrent() { synchronized(GameHub.this) {
            return !closed && tradeBindingCurrent(sender,senderBinding) && senderSameContext();
        }}
        boolean recipientCurrent() { synchronized(GameHub.this) {
            return !closed && recipient!=null && tradeBindingCurrent(recipient,recipientBinding) && recipientSameContext();
        }}
        boolean senderSameContext() {
            return senderVersion==sender.nativeClanSelectionVersion()
                    && senderContext.equals(sender.nativeLetterContext()) && sender.transportOpen();
        }
        boolean recipientSameContext() {
            return recipient!=null && recipientVersion==recipient.nativeClanSelectionVersion()
                    && recipientContext.equals(recipient.nativeLetterContext()) && recipient.transportOpen();
        }
        RuntimeInventory.RemovalLease removal() { return removal; }
        RuntimeInventory.SlotLease slot() { return slot; }
        long recipientSqlObjectId() { return recipientSource==null ? 0 : recipientSource.sqlCharacterObjectId(); }
        NativeLetterReceipt committed(RepositoryLetter letter) {
            synchronized(GameHub.this) {
                if (completed || closed) throw new IllegalStateException("letter permit already completed");
                completed=true;
                NativeLetterSenderDebt debt=new NativeLetterSenderDebt(senderSource,sqlPaperObjectId);
                nativeLetterSenderDebts.computeIfAbsent(senderSource,k->new LinkedHashMap<>()).put(sqlPaperObjectId,debt);
                if (recipientSource==null) return null;
                NativeLetterReceipt receipt=new NativeLetterReceipt(recipientSource,letter);
                Map<Long,NativeLetterReceipt> receipts=nativeLetterReceipts.computeIfAbsent(recipientSource,k->new LinkedHashMap<>());
                NativeLetterReceipt existing=receipts.putIfAbsent(letter.itemObjectId(),receipt);
                return existing==null ? receipt : existing;
            }
        }
        void senderInstalled() { synchronized(GameHub.this) {
            Map<Long,NativeLetterSenderDebt> debts=nativeLetterSenderDebts.get(senderSource);
            if (debts!=null) debts.remove(sqlPaperObjectId);
        }}
        @Override public void close() { synchronized(GameHub.this) {
            if (closed) return;
            closed=true;nativeLetterSenders.remove(senderSource,this);
            removal.close();if(slot!=null)slot.close();
        }}
    }

    final class NativeLetterReceipt {
        private final NativeLetterSource source;
        private final RepositoryLetter committed;
        private NativeLetterReceipt(NativeLetterSource source,RepositoryLetter letter) { this.source=source;committed=letter; }
        RepositoryLetter committed() { return committed; }
    }
    final class NativeLetterSenderDebt {
        private final NativeLetterSource source;
        private final long sqlPaperObjectId;
        private NativeLetterSenderDebt(NativeLetterSource source,long id) { this.source=source;sqlPaperObjectId=id; }
        long sqlPaperObjectId() { return sqlPaperObjectId; }
    }

    synchronized NativeLetterReconcile nativeLetterReconcile(ClientSession session) {
        TradeBinding binding=nativeTradeBinding(session);NativeLetterSource source=nativeLetterSource(session);
        if (binding==null || source==null || !session.nativeLetterRecipientReady()) return null;
        return new NativeLetterReconcile(session,binding,source);
    }
    final class NativeLetterReconcile {
        private final ClientSession session;
        private final TradeBinding binding;
        private final NativeLetterSource source;
        private final String context;
        private final Object version;
        private final List<NativeLetterReceipt> receipts;
        private final List<NativeLetterSenderDebt> debts;
        private NativeLetterReconcile(ClientSession session,TradeBinding binding,NativeLetterSource source) {
            this.session=session;this.binding=binding;this.source=source;
            context=session.nativeLetterContext();version=session.nativeClanSelectionVersion();
            receipts=List.copyOf(nativeLetterReceipts.getOrDefault(source,Map.of()).values());
            debts=List.copyOf(nativeLetterSenderDebts.getOrDefault(source,Map.of()).values());
        }
        List<NativeLetterReceipt> receipts() { return receipts; }
        List<NativeLetterSenderDebt> debts() { return debts; }
        boolean current() { synchronized(GameHub.this) { return tradeBindingCurrent(session,binding) && sameContext(); }}
        boolean sameContext() { return version==session.nativeClanSelectionVersion() && context.equals(session.nativeLetterContext()) && session.transportOpen(); }
        void installed(NativeLetterReceipt receipt) { synchronized(GameHub.this) {
            if (receipt.source.equals(source)) nativeLetterReceipts.getOrDefault(source,new LinkedHashMap<>()).remove(receipt.committed.itemObjectId(),receipt);
        }}
        void installed(NativeLetterSenderDebt debt) { synchronized(GameHub.this) {
            if (debt.source.equals(source)) nativeLetterSenderDebts.getOrDefault(source,new LinkedHashMap<>()).remove(debt.sqlPaperObjectId,debt);
        }}
    }
    synchronized void nativeLetterReceiptInstalled(NativeLetterReceipt receipt) {
        if(receipt!=null) nativeLetterReceipts.getOrDefault(receipt.source,new LinkedHashMap<>()).remove(receipt.committed.itemObjectId(),receipt);
    }
    synchronized boolean nativeLetterReconcilePending(ClientSession session) {
        NativeLetterSource source=nativeLetterSource(session);
        return source!=null && (!nativeLetterReceipts.getOrDefault(source,Map.of()).isEmpty()
                || !nativeLetterSenderDebts.getOrDefault(source,Map.of()).isEmpty()
                || nativeLetterSenders.values().stream().anyMatch(p->source.equals(p.senderSource)||source.equals(p.recipientSource)));
    }
