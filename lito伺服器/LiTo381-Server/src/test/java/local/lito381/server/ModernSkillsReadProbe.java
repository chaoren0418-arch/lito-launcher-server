package local.lito381.server;

import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/** Opt-in TEST CLASSPATH ONLY. Aggregates SELECT-only learned-skill qualification; never casts or writes. */
public final class ModernSkillsReadProbe {
    private ModernSkillsReadProbe() {}
    static boolean fireStormOptIn(String[] args) {
        if(args==null||args.length<1||args.length>2||args[0]==null||args[0].isEmpty()
                ||args.length==2&&!"--fire-storm".equals(args[1]))throw new IllegalArgumentException("explicit configuration and optional fire-storm mode required");
        return args.length==2;
    }
    /** Anonymous totals only; every support enum participates in conservation. */
    static final class Counts {
        long characters,skills,teleport,lightArrow,basicHeal,unimplemented,unsupported,empty,fireQualified,fireUnsupported;
        void add(List<ModernSkills.Skill> rows) {
            characters=Math.incrementExact(characters);skills=Math.addExact(skills,rows.size());if(rows.isEmpty())empty=Math.incrementExact(empty);
            for(var skill:rows)switch(skill.support()) {
                case TELEPORT -> teleport=Math.incrementExact(teleport);
                case LIGHT_ARROW -> lightArrow=Math.incrementExact(lightArrow);
                case BASIC_HEAL -> basicHeal=Math.incrementExact(basicHeal);
                case FIRE_STORM -> fireQualified=Math.incrementExact(fireQualified);
                case UNIMPLEMENTED -> unimplemented=Math.incrementExact(unimplemented);
                case UNSUPPORTED_TEMPLATE -> {unsupported=Math.incrementExact(unsupported);if(skill.skillId()==70)fireUnsupported=Math.incrementExact(fireUnsupported);}
            }
        }
        void validate(){if(characters==0||Math.addExact(Math.addExact(Math.addExact(teleport,lightArrow),Math.addExact(basicHeal,fireQualified)),Math.addExact(unimplemented,unsupported))!=skills)
            throw new IllegalStateException("skill aggregate mismatch");}
        String json(boolean fireStorm){
            return ",\"characters\":"+characters+",\"skills\":"+skills+",\"teleport_qualified\":"+teleport+",\"light_arrow_qualified\":"+lightArrow
                    +",\"unimplemented\":"+unimplemented+",\"unsupported_template\":"+unsupported+",\"empty_skill_characters\":"+empty
                    +(basicHeal>0||fireStorm?",\"basic_heal_qualified\":"+basicHeal:"")
                    +(fireStorm?",\"fire_qualified\":"+fireQualified+",\"fire_unsupported_template\":"+fireUnsupported:"");
        }
    }
    public static void main(String[] args) {
        ModernDatabaseReadProbe.Settings settings=null;ModernServerConfig config=null;
        ModernBackendExecutor executor=null;char[] password=null;String stage="CONFIGURATION";
        Diagnostics diagnostics=new Diagnostics();
        boolean retired=true;int exit=2;String success=null;
        try {
            boolean fireStorm=fireStormOptIn(args);
            Path file=Path.of(args[0]);String account=System.getenv("LITO_MODERN_PROBE_ACCOUNT");
            String secret=System.getenv("LITO_MODERN_PROBE_PASSWORD");
            if(!ModernCharacterDataReadProbe.boundedText(account,64,false)
                    ||!ModernCharacterDataReadProbe.boundedText(secret,256,true))throw new IllegalArgumentException("explicit probe credentials required");
            password=secret.toCharArray();settings=ModernDatabaseReadProbe.load(file);
            config=ModernDatabaseReadProbe.modernConfig(settings,ModernCharacterDataReadProbe.catalogDatabase(file));
            final ModernServerConfig configured=config;
            executor=new ModernBackendExecutor(1,1);retired=false;
            Counts counts=new Counts();
            try(var gate=executor.openSession()) {
                for(var source:config.sources()) {
                    var audit=new ModernEquipmentReadProbe.Audit();
                    var dao=new ModernJdbcBackend(source.database(),config.dbConnectMillis(),config.dbNetworkMillis(),
                            (url,properties)->ModernEquipmentReadProbe.inspectOwned(observe(new com.mysql.cj.jdbc.Driver().connect(url,properties),
                                    source.catalogDatabase(),diagnostics,fireStorm),
                                    ModernBackendScope.current(),source.database().database(),source.catalogDatabase(),configured.dbNetworkMillis(),null,audit,null));
                    var verifier=ModernAccountAuthentication.repositories(Map.of(source.id(),
                            ModernAccountAuthentication.SourceBinding.direct(source.name(),dao::authenticate)),ignored->false);
                    stage="AUTHENTICATION";var principal=ModernEquipmentRepositoryReadProbe.authenticate(gate,verifier,account,password);
                    stage="ROSTER";var roster=gate.call(5000,scope->dao.characters(principal.accountForSource(source.id())));
                    var repository=ModernSkillsRepository.jdbc(Map.of(source.id(),new ModernSkillsRepository.Source(dao,source.catalogDatabase(),fireStorm)));
                    for(var expected:roster) {
                        stage="SKILL_QUALIFICATION";
                        var snapshot=gate.call(5000,scope->repository.character(principal,source.id(),expected.objectId())).orElseThrow();
                        if(snapshot.sourceId()!=source.id()||!snapshot.character().equals(expected))throw new IllegalStateException("skill source changed");
                        counts.add(snapshot.skills());
                    }
                    stage="ROSTER_RECHECK";
                    if(!gate.call(5000,scope->dao.characters(principal.accountForSource(source.id()))).equals(roster))
                        throw new IllegalStateException("skill roster changed");
                }
            }
            counts.validate();
            if(fireStorm)diagnostics.requireCompletedExtras();
            stage="RETIREMENT";executor.close();retired=executor.awaitTermination(5000);
            if(!retired)throw new IllegalStateException("worker not retired");
            success="{\"phase\":\"production-skills-read\",\"status\":\"OK\",\"sources\":"+config.sources().size()
                    +counts.json(fireStorm)+(fireStorm?diagnostics.extrasJson():"")
                    +",\"source_database_verified\":true,\"atomic_generation\":false,\"cast_executed\":false,\"database_written\":false}";
            exit=0;
        }catch(Exception failure) {
            String reason=failure instanceof ModernBackendExecutor.BackendException backend?backend.failure().name():"INVALID_RESULT_OR_SETUP";
            System.out.println("{\"phase\":\"production-skills-read\",\"status\":\"FAILED\",\"stage\":\""+stage+"\",\"reason\":\""+reason+"\""+diagnostics.json()+"}");
        }finally {
            if(executor!=null)try{executor.close();retired=executor.awaitTermination(5000);}catch(Exception failure){retired=false;}
            if(retired) {
                try {try{if(config!=null)config.close();}finally{if(settings!=null)settings.close();}}
                catch(Exception failure){exit=2;System.out.println("{\"phase\":\"production-skills-read\",\"status\":\"FAILED\",\"reason\":\"OWNER_CLOSE_FAILED\"}");}
            }else{exit=2;System.out.println("{\"phase\":\"production-skills-read\",\"status\":\"FAILED\",\"reason\":\"WORKER_NOT_RETIRED\"}");}
            if(password!=null)Arrays.fill(password,'\0');
        }
        if(exit==0)System.out.println(success);else System.exit(exit);
    }

    /* TEST-ONLY diagnostics. Never retain/print SQL, bound parameters, row values,
       exception messages, account names, character/item identifiers, database names or addresses.
       The only emitted catalog identifier is the public skill ID in 1..220.
       Every reflected driver failure is rethrown as the ORIGINAL instance so the
       production optional-column guard retains its vendor/state/cleanup rules. */
    enum Query { NONE, ROUTE, AUTH, ROSTER, EFFECT_COLUMN, SKILLS, FIRE_EXTRAS, UNKNOWN }
    private enum Step { NONE, PREPARE, SETUP, EXECUTE, NEXT, COLUMN, METADATA, VALUE, CLEANUP }
    private enum Column {
        NONE, UNKNOWN, OBJID, CHAR_NAME, CHAR_NAME_BYTES, CHARACTER_TYPE, CHARACTER_SEX, LEVEL,
        LEARNED_OWNER, LEARNED_ID, TEMPLATE_ID, SKILL_LEVEL, SKILL_NUMBER, NAME_TOKEN, NAME_TOKEN_BYTES,
        MP_COST, HP_COST, ITEM_ID, ITEM_COUNT, REUSE_MILLIS, TARGET, TARGET_BYTES, SPELL_TYPE,
        ACTION_ID, CASTGFX, EFFECT_SKILL_ID, CATALOG_NAME, CATALOG_NAME_BYTES,
        BUFF_DURATION,TARGET_TO,DAMAGE_VALUE,DAMAGE_DICE,DAMAGE_DICE_COUNT,PROBABILITY_VALUE,PROBABILITY_DICE,
        ATTR,LAWFUL,MAXIMUM_RANGE,AREA,THROUGH_FLAG,CASTGFX2,TEMPLATE_ACTION,SYSMSG_HAPPEN,SYSMSG_STOP,SYSMSG_FAIL,
        EXTENSION_KIND,IDS_NULL,COUNTS_NULL;
        static Column known(String name) {
            if(name==null)return UNKNOWN;
            for(Column candidate:values())
                if(candidate!=NONE&&candidate!=UNKNOWN&&candidate.name().equalsIgnoreCase(name))return candidate;
            return UNKNOWN;
        }
    }
    private enum NumericType { UNKNOWN, EXACT_INTEGER, EXACT_DECIMAL, BOOLEAN, APPROXIMATE, TEXT, OTHER }
    private enum TextShape { UNKNOWN, NULL, EMPTY, BLANK, INVALID_UNICODE, CONTROL, OVERSIZED, VALID }
    static final class Diagnostics {
        private Query query=Query.NONE;private Step step=Step.NONE;private Column column=Column.NONE;
        private NumericType numericType=NumericType.UNKNOWN;private int rows;
        private String sqlState="NONE";private int vendor;private boolean sqlFailure;
        private TextShape textShape=TextShape.UNKNOWN;private int publicSkillId;
        private long extrasPrepared,extrasCompleted,extrasRows;private boolean extrasEnded;
        synchronized void prepare(Query next) {
            query=next;step=Step.PREPARE;column=Column.NONE;numericType=NumericType.UNKNOWN;rows=0;
            sqlState="NONE";vendor=0;sqlFailure=false;
            textShape=TextShape.UNKNOWN;publicSkillId=0;
            extrasEnded=false;if(next==Query.FIRE_EXTRAS)extrasPrepared=Math.incrementExact(extrasPrepared);
        }
        synchronized void at(Step next) {if(!sqlFailure)step=next;}
        synchronized void column(Column next,Step nextStep) {
            if(!sqlFailure){column=next;step=nextStep;numericType=NumericType.UNKNOWN;}
        }
        synchronized void row() {
            if(rows<ModernSkillsJdbc.MAX_RAW_ROWS+1)rows++;
            if(query==Query.SKILLS){textShape=TextShape.UNKNOWN;publicSkillId=0;}
            if(query==Query.FIRE_EXTRAS)extrasRows=Math.incrementExact(extrasRows);
        }
        synchronized void endRows(){if(query==Query.FIRE_EXTRAS&&!extrasEnded){extrasEnded=true;extrasCompleted=Math.incrementExact(extrasCompleted);}}
        synchronized void requireCompletedExtras(){if(extrasPrepared!=extrasCompleted)throw new IllegalStateException("fire extension audit incomplete");}
        synchronized String extrasJson(){return ",\"fire_storm_opt_in\":true,\"fire_extras_queries\":"+extrasPrepared+",\"fire_extras_completed\":"+extrasCompleted+",\"fire_extras_rows\":"+extrasRows;}
        synchronized void type(int type) {
            numericType=switch(type) {
                case Types.TINYINT,Types.SMALLINT,Types.INTEGER,Types.BIGINT -> NumericType.EXACT_INTEGER;
                case Types.DECIMAL,Types.NUMERIC -> NumericType.EXACT_DECIMAL;
                case Types.BIT,Types.BOOLEAN -> NumericType.BOOLEAN;
                case Types.FLOAT,Types.REAL,Types.DOUBLE -> NumericType.APPROXIMATE;
                case Types.CHAR,Types.VARCHAR,Types.LONGVARCHAR,Types.NCHAR,Types.NVARCHAR,Types.LONGNVARCHAR -> NumericType.TEXT;
                default -> NumericType.OTHER;
            };
        }
        synchronized void skill(Object value) {
            if(query!=Query.SKILLS)return;
            publicSkillId=0;
            if(value instanceof BigDecimal number)try {
                long id=number.longValueExact();if(id>=1&&id<=ModernSkills.MAX_SKILLS)publicSkillId=(int)id;
            }catch(ArithmeticException invalid){/* Invalid values are never emitted. */}
        }
        synchronized void text(String value) {
            if(query!=Query.SKILLS)return;
            if(value==null){textShape=TextShape.NULL;return;}
            if(value.isEmpty()){textShape=TextShape.EMPTY;return;}
            boolean blank=true;
            for(int i=0;i<value.length();) {
                char c=value.charAt(i);
                if(Character.isSurrogate(c)&&(!Character.isHighSurrogate(c)||i+1>=value.length()
                        ||!Character.isLowSurrogate(value.charAt(i+1)))){textShape=TextShape.INVALID_UNICODE;return;}
                int point=value.codePointAt(i);i+=Character.charCount(point);
                if(Character.isISOControl(point)){textShape=TextShape.CONTROL;return;}
                if(!Character.isWhitespace(point)&&!Character.isSpaceChar(point))blank=false;
            }
            textShape=value.getBytes(StandardCharsets.UTF_8).length>256?TextShape.OVERSIZED:blank?TextShape.BLANK:TextShape.VALID;
        }
        synchronized void failed(Throwable failure,Step failureStep) {
            if(!sqlFailure&&failure instanceof SQLException sql) {
                sqlFailure=true;step=failureStep;vendor=sql.getErrorCode();
                String value=sql.getSQLState();sqlState=value!=null&&value.matches("[A-Za-z0-9]{5}")?value:"UNKNOWN";
            }
        }
        synchronized String json() {
            return ",\"jdbc_query\":\""+query+"\",\"jdbc_step\":\""+step+"\",\"jdbc_column\":\""+column
                    +"\",\"jdbc_numeric_type\":\""+numericType+"\",\"jdbc_rows_scanned\":"+rows
                    +",\"jdbc_name_token_shape\":\""+textShape+"\",\"jdbc_public_skill_id\":"+publicSkillId
                    +",\"jdbc_sqlstate\":\""+sqlState+"\",\"jdbc_vendor\":"+vendor;
        }
    }
    static Query classify(String sql,String catalog,boolean fireStorm) {
        if("SELECT DATABASE()".equals(sql))return Query.ROUTE;
        if(ModernJdbcBackend.AUTH_SQL.equals(sql))return Query.AUTH;
        if(ModernJdbcBackend.CHARACTERS_SQL.equals(sql))return Query.ROSTER;
        if(("SELECT `effect_skill_id` FROM `"+catalog+"`.`skills` LIMIT 0").equals(sql))return Query.EFFECT_COLUMN;
        if(ModernSkillsJdbc.sql(catalog,true,fireStorm).equals(sql)||ModernSkillsJdbc.sql(catalog,false,fireStorm).equals(sql))return Query.SKILLS;
        if(fireStorm&&ModernSkillsJdbc.fireExtrasSql(catalog).equals(sql))return Query.FIRE_EXTRAS;
        return Query.UNKNOWN;
    }
    static Connection observe(Connection connection,String catalog,Diagnostics diagnostics,boolean fireStorm) {
        if(connection==null)return null;
        return (Connection)Proxy.newProxyInstance(ModernSkillsReadProbe.class.getClassLoader(),new Class<?>[]{Connection.class},(proxy,method,args)->{
            boolean prepare=method.getName().equals("prepareStatement")&&args!=null&&args.length>0&&args[0] instanceof String;
            if(prepare){var query=classify((String)args[0],catalog,fireStorm);diagnostics.prepare(query);
                // Opt-in audit permits only the exact read statements used by
                // this route. No execute/write/DDL or SQL text is disclosed.
                if(fireStorm&&query==Query.UNKNOWN)throw new SQLException("unqualified probe statement");
            }
            if(fireStorm&&(method.getName().equals("createStatement")||method.getName().equals("prepareCall")||method.getName().equals("commit")))throw new SQLException("unqualified probe operation");
            if(fireStorm&&method.getName().equals("setReadOnly")&&!Boolean.TRUE.equals(args[0]))throw new SQLException("probe must remain read only");
            Object result=invoke(connection,method,args,diagnostics,prepare?Step.PREPARE:Step.CLEANUP);
            return prepare&&result instanceof PreparedStatement statement?observe(statement,diagnostics,fireStorm):result;
        });
    }
    private static PreparedStatement observe(PreparedStatement statement,Diagnostics diagnostics,boolean fireStorm) {
        return (PreparedStatement)Proxy.newProxyInstance(ModernSkillsReadProbe.class.getClassLoader(),new Class<?>[]{PreparedStatement.class},(proxy,method,args)->{
            String name=method.getName();Step step=name.equals("executeQuery")?Step.EXECUTE:name.equals("close")?Step.CLEANUP:Step.SETUP;
            if(fireStorm&&(name.equals("execute")||name.equals("executeUpdate")||name.equals("executeLargeUpdate")||name.equals("addBatch")||name.equals("executeBatch")||name.equals("executeLargeBatch")
                    ||name.equals("executeQuery")&&args!=null&&args.length!=0))throw new SQLException("probe permits prepared SELECT execution only");
            if(step!=Step.CLEANUP)diagnostics.at(step);
            Object result=invoke(statement,method,args,diagnostics,step);
            return result instanceof ResultSet rows?observe(rows,diagnostics):result;
        });
    }
    private static ResultSet observe(ResultSet rows,Diagnostics diagnostics) {
        Map<Integer,Column> columns=new HashMap<>();
        return (ResultSet)Proxy.newProxyInstance(ModernSkillsReadProbe.class.getClassLoader(),new Class<?>[]{ResultSet.class},(proxy,method,args)->{
            String name=method.getName();Step step=name.equals("close")?Step.CLEANUP:Step.VALUE;
            if(name.equals("next")){step=Step.NEXT;diagnostics.at(step);}
            else if(name.equals("findColumn")){step=Step.COLUMN;diagnostics.column(Column.known((String)args[0]),step);}
            else if(name.equals("getMetaData")){step=Step.METADATA;diagnostics.at(step);}
            else if(args!=null&&args.length>0&&name.startsWith("get")) {
                Column column=args[0] instanceof String label?Column.known(label)
                        :args[0] instanceof Integer index?columns.getOrDefault(index,Column.UNKNOWN):Column.UNKNOWN;
                // Keep the exact numeric type just observed for getBigDecimal(index).
                if(!(name.equals("getBigDecimal")&&args[0] instanceof Integer))diagnostics.column(column,step);
                else diagnostics.at(step);
            }
            Object result=invoke(rows,method,args,diagnostics,step);
            if(name.equals("getString")&&args!=null&&args.length==1&&"name_token".equals(args[0]))
                diagnostics.text((String)result);
            if(name.equals("getBigDecimal")&&args!=null&&args.length==1&&args[0] instanceof Integer index
                    &&columns.get(index)==Column.TEMPLATE_ID)diagnostics.skill(result);
            if(name.equals("next")&&Boolean.TRUE.equals(result))diagnostics.row();
            if(name.equals("next")&&Boolean.FALSE.equals(result))diagnostics.endRows();
            if(name.equals("findColumn")&&result instanceof Integer index)columns.put(index,Column.known((String)args[0]));
            if(result instanceof ResultSetMetaData metadata)return observe(metadata,diagnostics);
            return result;
        });
    }
    private static ResultSetMetaData observe(ResultSetMetaData metadata,Diagnostics diagnostics) {
        return (ResultSetMetaData)Proxy.newProxyInstance(ModernSkillsReadProbe.class.getClassLoader(),new Class<?>[]{ResultSetMetaData.class},(proxy,method,args)->{
            Object result=invoke(metadata,method,args,diagnostics,Step.METADATA);
            if(method.getName().equals("getColumnType")&&result instanceof Integer type)diagnostics.type(type);
            return result;
        });
    }
    private static Object invoke(Object target,Method method,Object[] args,Diagnostics diagnostics,Step step) throws Throwable {
        try{return method.invoke(target,args);}
        catch(InvocationTargetException failure){Throwable original=failure.getCause();diagnostics.failed(original,step);throw original;}
    }
}
