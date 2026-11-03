import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** Explicit schema maintenance; the game runtime never creates or writes command definitions. */
public class CommandTable {
    static Properties read(String path) throws Exception {
        Properties p=new Properties();
        try(var r=Files.newBufferedReader(Path.of(path),StandardCharsets.UTF_8)){p.load(r);}
        return p;
    }
    static String id(String value) {
        if(value==null||!value.matches("[A-Za-z0-9_]+"))throw new IllegalArgumentException("Invalid database name");
        return "`"+value+"`";
    }
    public static void main(String[] args) throws Exception {
        Properties config=read(args[1]);
        String source=id(config.getProperty("database.static.database")), target=id(config.getProperty("database.lito.database"));
        boolean apply=args[0].equals("apply");
        Properties credential=apply?read(args[2]):config;
        String url=config.getProperty("database.jdbc.base.url")+config.getProperty("database.lito.database")+config.getProperty("database.jdbc.options");
        String user=apply?credential.getProperty("Login",credential.getProperty("Login_LOGIN")):credential.getProperty("database.user");
        String password=apply?credential.getProperty("Password",credential.getProperty("Password_LOGIN","")):credential.getProperty("database.password");
        Class.forName("com.mysql.cj.jdbc.Driver");
        try(Connection c=DriverManager.getConnection(url,user,password);Statement s=c.createStatement()) {
            if(!apply)c.setReadOnly(true);
            s.setQueryTimeout(15);
            if(apply) {
                // Preserve existing operator rows; copy only the three implemented commands.
                String ddl=Files.readString(Path.of(args[3]),StandardCharsets.UTF_8);
                for(String sql:ddl.replaceAll("(?m)^--.*$", "").split(";"))
                    if(!sql.isBlank())s.executeUpdate(sql.replace("`380_lito`",target).replace("`380_db`",source));
                System.out.println("Applied LiTo command table; source commands and accounts untouched.");
            }
            for(String database:List.of(source,target)) {
                try(ResultSet r=s.executeQuery("SELECT name,access_level,class_name,note,system FROM "+database+".commands WHERE name IN ('item','spawn','de') ORDER BY name")) {
                    while(r.next())System.out.println(database+" | "+r.getString(1)+" | "+r.getInt(2)+" | "+r.getString(3)+" | "+r.getString(4)+" | "+r.getInt(5));
                }catch(SQLException e){if(apply)throw e;System.out.println(database+" commands unavailable: SQLState="+e.getSQLState()+" code="+e.getErrorCode());}
            }
        }
    }
}
