package local.lito381.server;

import java.util.Objects;

/** Explicit participant namespace. A bot handle is never a source character objid. */
record ModernParticipantId(Kind kind,int sourceId,long id) implements Comparable<ModernParticipantId> {
    enum Kind { HUMAN, SERVER_BOT }
    ModernParticipantId {
        Objects.requireNonNull(kind);
        if(sourceId<=0 || id<=0)throw new IllegalArgumentException("invalid modern participant identity");
    }
    static ModernParticipantId human(TournamentPresence.CharacterKey key) {
        Objects.requireNonNull(key);return new ModernParticipantId(Kind.HUMAN,key.sourceId(),key.objectId());
    }
    static ModernParticipantId human(int sourceId,long objectId) {return new ModernParticipantId(Kind.HUMAN,sourceId,objectId);}
    static ModernParticipantId bot(int sourceId,long botId) {return new ModernParticipantId(Kind.SERVER_BOT,sourceId,botId);}
    boolean isHuman(){return kind==Kind.HUMAN;}
    TournamentPresence.CharacterKey humanKey() {
        if(!isHuman())throw new IllegalStateException("server bot is not a database character");
        return new TournamentPresence.CharacterKey(sourceId,id);
    }
    @Override public int compareTo(ModernParticipantId other) {
        int order=kind.compareTo(other.kind);
        if(order==0)order=Integer.compare(sourceId,other.sourceId);
        return order==0?Long.compare(id,other.id):order;
    }
    @Override public String toString(){return "ModernParticipantId["+kind+"]";}
}
