package local.lito381.server;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.lang.reflect.*;

public class LiveBuffReadCheck {
    public static void main(String[] args) throws Exception {
        Properties p=new Properties();
        try(var reader=Files.newBufferedReader(Path.of(args[0]),StandardCharsets.UTF_8)){p.load(reader);}
        Class.forName("com.mysql.cj.jdbc.Driver");
        String prefix=p.getProperty("database.jdbc.base.url"),options=p.getProperty("database.jdbc.options");
        List<String> sources=new ArrayList<>();
        try(Connection c=DriverManager.getConnection(prefix+p.getProperty("database.lito.database")+options,p.getProperty("database.user"),p.getProperty("database.password"))) {
            c.setReadOnly(true);
            try(Statement st=c.createStatement();ResultSet rs=st.executeQuery("SELECT character_database FROM lito_source_server WHERE enabled=1 ORDER BY source_id")){while(rs.next())sources.add(rs.getString(1));}
        }
        Method load=LegacyMySqlRepository.class.getDeclaredMethod("loadBuffSnapshot",Connection.class,long.class);load.setAccessible(true);
        for(String schema:sources) {
            try(Connection c=DriverManager.getConnection(prefix+schema+options,p.getProperty("database.user"),p.getProperty("database.password"))) {
                c.setReadOnly(true);List<Long> ids=new ArrayList<>();ids.add(0L);
                try(Statement st=c.createStatement();ResultSet rs=st.executeQuery("SELECT DISTINCT char_obj_id FROM character_buff ORDER BY char_obj_id LIMIT 20")){while(rs.next())ids.add(rs.getLong(1));}
                int checkedRows=0;
                for(long id:ids) {
                    RepositoryBuffSnapshot actual=(RepositoryBuffSnapshot)load.invoke(null,c,id);
                    boolean old=SourceBuffRead.versioned(c);List<RepositoryBuff> expected=new ArrayList<>();
                    String sql="SELECT skill_id,remaining_time,poly_id FROM character_buff WHERE char_obj_id=?"+(old?" AND snapshot_version=?":"")+" ORDER BY skill_id";
                    try(PreparedStatement st=c.prepareStatement(sql)) {
                        st.setLong(1,id);if(old)st.setLong(2,actual.completedVersion());
                        try(ResultSet rs=st.executeQuery()){while(rs.next())expected.add(new RepositoryBuff(rs.getInt(1),rs.getInt(2),rs.getInt(3)));}
                    }
                    if(!actual.authoritative() || !actual.buffs().equals(expected))throw new AssertionError("BUFF mismatch");
                    checkedRows+=expected.size();
                }
                System.out.println("PASS "+schema+" selected characters="+ids.size()+" BUFF rows="+checkedRows+" readOnly="+c.isReadOnly());
            }
        }
    }
}
