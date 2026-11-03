package local.lito381.server;
import java.util.LinkedHashMap;
import java.util.Map;
/** Independent Source skill timers. A reveal is emitted only after the last lease ends. */
final class BattleInvisibilityStatus {
    private record Lease(long deadline) {}
    private record Snapshot(Map<Integer,Lease> leases) {}
    private final LinkedHashMap<Integer,Lease> leases=new LinkedHashMap<>();
    private volatile Object sourceInvisibilityLease;
    synchronized boolean active(){return !leases.isEmpty();}
    synchronized boolean hasSkill(int skill){return leases.containsKey(skill);}
    synchronized int skillId(){int latest=0;for(int skill:leases.keySet())latest=skill;return latest;}
    synchronized long expiresAtNanos(){var lease=leases.get(skillId());return lease==null?0L:lease.deadline();}
    synchronized void activate(int skill,long now,int duration){
        if(skill<=0||now<0||duration<0)throw new IllegalArgumentException("invalid invisibility lease");
        long deadline=duration==0?0L:Math.addExact(now,duration*1_000_000_000L);
        leases.remove(skill);leases.put(skill,new Lease(deadline));rotate();
    }
    private void rotate(){sourceInvisibilityLease=new Snapshot(Map.copyOf(leases));}
    synchronized boolean cancel(){if(!active())return false;leases.clear();rotate();return true;}
    /** Deferred accepted hits remove only the exact skill leases captured at admission. */
    synchronized boolean cancelCaptured(Object captured){
        if(!(captured instanceof Snapshot snapshot))return captured==sourceInvisibilityLease&&cancel();
        boolean changed=false;
        for(var old:snapshot.leases().entrySet())if(leases.get(old.getKey())==old.getValue()){leases.remove(old.getKey());changed=true;}
        if(changed)rotate();return changed;
    }
    synchronized int expire(long now){
        if(now<0)throw new IllegalArgumentException("negative nano time");int last=0;
        for(var it=leases.entrySet().iterator();it.hasNext();){var lease=it.next();long deadline=lease.getValue().deadline();if(deadline!=0&&now>=deadline){last=lease.getKey();it.remove();}}
        if(last!=0)rotate();
        return leases.isEmpty()?last:0;
    }
}
