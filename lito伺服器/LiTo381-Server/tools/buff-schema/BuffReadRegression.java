package local.lito381.server;

import java.lang.reflect.*;
import java.sql.*;
import java.util.*;

/** Execute the actual repository reader against both schema contracts. */
public class BuffReadRegression {
    record Buff(int skill, int seconds, int poly, long version) {}
    static final class Fixture {
        boolean versioned, marker = true, missingColumn, denied;
        Long completed;
        int opened, closed;
        List<Buff> buffs = new ArrayList<>();
        Fixture(boolean v) { versioned = v; }
        Connection connection() {
            return proxy(Connection.class, (o,m,a) -> {
                if (m.getName().equals("prepareStatement")) {
                    String sql = (String)a[0];
                    if (!sql.startsWith("SELECT ")) throw new AssertionError("Non-read SQL: " + sql);
                    if (sql.contains("character_buff_snapshot") && (!versioned || !marker)) throw new SQLException("missing marker", "42S02");
                    Map<Integer,Long> params = new HashMap<>(); opened++;
                    return proxy(PreparedStatement.class, (p,n,b) -> {
                        switch(n.getName()) {
                            case "close": closed++; return null;
                            case "setLong": params.put((Integer)b[0], (Long)b[1]); return null;
                            case "executeQuery":
                                if (denied) throw new SQLException("access denied", "42000");
                                List<String> cols = new ArrayList<>(List.of("char_obj_id","skill_id","remaining_time","poly_id"));
                                if (missingColumn) cols.remove("poly_id");
                                if (versioned) cols.add("snapshot_version");
                                if (sql.contains("LIMIT 0")) return result(List.of(), cols);
                                if (sql.startsWith("SELECT completed_version")) return result(completed == null ? List.of() : List.of(Map.of("completed_version", completed)), List.of("completed_version"));
                                if (!params.containsKey(1) || params.get(1) != 17) throw new AssertionError("Character parameter");
                                if (versioned && (!sql.contains("snapshot_version=?") || !params.containsKey(2))) throw new AssertionError("Missing version filter");
                                if (!versioned && sql.contains("snapshot_version")) throw new SQLException("unknown version column", "42S22");
                                List<Map<String,Long>> rows = new ArrayList<>();
                                for (Buff buff : buffs) if (!versioned || buff.version == params.get(2)) rows.add(Map.of("skill_id",(long)buff.skill,"remaining_time",(long)buff.seconds,"poly_id",(long)buff.poly));
                                return result(rows, List.of("skill_id","remaining_time","poly_id"));
                            default: throw new UnsupportedOperationException(n.getName());
                        }
                    });
                }
                throw new UnsupportedOperationException(m.getName());
            });
        }
    }
    @SuppressWarnings("unchecked") static <T> T proxy(Class<T> type, InvocationHandler h) { return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},h); }
    static ResultSet result(List<Map<String,Long>> rows, List<String> cols) {
        int[] index={-1};
        return proxy(ResultSet.class,(o,m,a)-> {
            switch(m.getName()) {
                case "next": return ++index[0] < rows.size();
                case "close": return null;
                case "getMetaData": return proxy(ResultSetMetaData.class,(p,n,b)-> {
                    if(n.getName().equals("getColumnCount")) return cols.size();
                    if(n.getName().equals("getColumnLabel")) return cols.get((Integer)b[0]-1);
                    throw new UnsupportedOperationException(n.getName());
                });
                case "getInt": return rows.get(index[0]).get((String)a[0]).intValue();
                case "getLong": return rows.get(index[0]).get(a[0] instanceof Integer ? cols.get((Integer)a[0]-1) : (String)a[0]);
                default: throw new UnsupportedOperationException(m.getName());
            }
        });
    }
    static RepositoryBuffSnapshot load(Fixture f) throws Exception {
        Method m=LegacyMySqlRepository.class.getDeclaredMethod("loadBuffSnapshot",Connection.class,long.class);m.setAccessible(true);
        try { return (RepositoryBuffSnapshot)m.invoke(null,f.connection(),17L); }
        catch(InvocationTargetException e) { throw (Exception)e.getCause(); }
        finally { if(f.opened!=f.closed) throw new AssertionError("Statement leak: "+f.opened+"/"+f.closed); }
    }
    static void check(boolean value) { if(!value) throw new AssertionError(); }
    static void reject(Fixture f) throws Exception { try {load(f);throw new AssertionError("Invalid schema/data accepted");}catch(SQLException expected){} }
    public static void main(String[] args) throws Exception {
        boolean patched=args.length==0; int tests=0;
        Fixture old=new Fixture(true); old.completed=4L; old.buffs.addAll(List.of(new Buff(1,30,0,4),new Buff(2,70,0,5),new Buff(3,90,0,3)));
        var r=load(old);check(r.completedVersion()==4 && r.buffs().size()==1 && r.buffs().get(0).skillId()==1);tests++;
        old.completed=null;old.buffs.add(new Buff(4,60,0,0));r=load(old);check(r.completedVersion()==0 && r.buffs().size()==1 && r.buffs().get(0).skillId()==4);tests++;
        old.completed=8L;r=load(old);check(r.buffs().isEmpty() && r.completedVersion()==8);tests++;
        Fixture missing=new Fixture(true);missing.marker=false;reject(missing);tests++;
        Fixture denied=new Fixture(true);denied.denied=true;reject(denied);tests++;
        Fixture invalid=new Fixture(true);invalid.buffs.add(new Buff(0,30,0,0));reject(invalid);tests++;
        Fixture duplicate=new Fixture(true);duplicate.buffs.addAll(List.of(new Buff(1,30,0,0),new Buff(1,60,0,0)));reject(duplicate);tests++;
        Fixture negative=new Fixture(true);negative.completed=-1L;reject(negative);tests++;
        Fixture single=new Fixture(false);single.buffs.addAll(List.of(new Buff(2,-1,42,0),new Buff(1,60,0,0)));
        if(patched) {
            r=load(single);check(r.authoritative() && r.completedVersion()==0 && r.buffs().size()==2 && r.buffs().get(0).skillId()==1 && r.buffs().get(1).remainingSeconds()==-1);tests++;
            r=load(old);check(r.completedVersion()==8 && r.buffs().isEmpty());tests++;
            Fixture empty=new Fixture(false);check(load(empty).buffs().isEmpty());tests++;
            Fixture bad=new Fixture(false);bad.missingColumn=true;reject(bad);tests++;
            Fixture access=new Fixture(false);access.denied=true;reject(access);tests++;
            Fixture zero=new Fixture(false);zero.buffs.add(new Buff(1,0,0,0));reject(zero);tests++;
            Fixture dup=new Fixture(false);dup.buffs.addAll(List.of(new Buff(1,30,0,0),new Buff(1,60,0,0)));reject(dup);tests++;
            Fixture overflow=new Fixture(false);for(int i=1;i<=513;i++)overflow.buffs.add(new Buff(i,30,0,0));reject(overflow);tests++;
        } else { reject(single);tests++; }
        System.out.println("PASS actual repository " +(patched?"candidate":"original")+" cases="+tests+"; SELECT only; statements closed");
    }
}
