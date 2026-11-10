package local.lito381.server;
import java.sql.*;import java.util.*;
/** Immutable static Source INT healing data; shared readonly startup transaction, no actor ownership. */
final class NativeSourceHealingFormula {
 record Growth(Map<Integer,Integer> magic,Map<Integer,Integer> healing){Growth{magic=Map.copyOf(magic);healing=Map.copyOf(healing);}int magicBonus(int intelligence){if(intelligence<1)return -2;return magic.getOrDefault(Math.min(intelligence,127),0);}int bonus(int intelligence){if(intelligence<1)return -2;int key=Math.min(intelligence,127);var value=healing.get(key);return value!=null?value:Math.min(magicBonus(intelligence),10);}}
 private static volatile Growth current;
 static Growth read(Connection c)throws SQLException{if(c==null||!c.isReadOnly()||c.getAutoCommit())throw new SQLException("Source healing growth requires existing readonly transaction");var magic=new HashMap<Integer,Integer>();var healing=new HashMap<Integer,Integer>();try(var statement=c.createStatement()){statement.setMaxRows(4097);try(var rs=statement.executeQuery("SELECT * FROM `角色_智力成長`")){var meta=rs.getMetaData();boolean bonus=false;for(int col=1;col<=meta.getColumnCount();col++)if("治癒骰加成".equals(meta.getColumnLabel(col)))bonus=true;int count=0;while(rs.next()){if(++count>4096)throw new SQLException("Source INT growth exceeds bounded static rows");int id=rs.getInt("id"),value=rs.getInt("魔法加成");if(id<1||id>127)continue;magic.put(id,value);healing.put(id,bonus?rs.getInt("治癒骰加成"):Math.min(value,10));}}}return new Growth(magic,healing);}
 static void install(Growth growth){current=Objects.requireNonNull(growth);}
 static boolean enabled(){return NativeSourceHealingCast.current()!=null;}
 static int bonus(int intelligence){var data=current;if(data==null)throw new IllegalStateException("Source healing growth not loaded");return data.bonus(intelligence);}
 static int add(int left,int right){return enabled()?left+right:Math.addExact(left,right);}
 static int multiply(int left,int right){return enabled()?left*right:Math.multiplyExact(left,right);}
 static int lawful(int rolled,long lawful){double revision=1.0;if(lawful>0)revision+=lawful/32768.0;return(int)(rolled*revision);}
 static int adjusted(int healing,boolean pollute,boolean dirty,boolean erosion){int damage=-healing;if(pollute)damage>>=1;if(dirty)damage>>=1;if(erosion)damage*=-1;return -damage;}
 static int single(int healing,boolean water,boolean pollute,boolean dirty,boolean erosion){int damage=-healing;if(water)damage<<=1;if(pollute)damage>>=1;if(dirty)damage>>=1;if(erosion)damage*=-1;return -damage;}
 private NativeSourceHealingFormula(){}
}
