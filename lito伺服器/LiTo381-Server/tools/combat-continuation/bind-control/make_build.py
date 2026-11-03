from pathlib import Path
R=Path(__file__).resolve().parent
t=(R.parent/'triple-control/build.py').read_text('utf-8')
t=t.replace("R.parent/'invisibility-control/build/candidate-invisibility-control.jar'","R.parent/'triple-control/build/candidate-triple-control.jar'").replace('c7f2ad7ad7d527e5f2e6f86efbf3f42d48a4828e29543d9216d09ddec9f56729','2fad5a7454e0369600ee97c09dd35fb03d53199835a261dc0fe758b170db62ac').replace("R.parent/'invisibility-control/build/inputs/sources/CombatRules.java'","R.parent/'triple-control/build/inputs/sources/CombatRules.java'")
t=t.replace('TripleMethods','BindMethods').replace('CombatTripleArrow','CombatBindControl').replace("candidate=D/'candidate-triple-control.jar'","candidate=D/'candidate-bind-control.jar'")
a=t.index("parser=method(");z=t.index("(src/'CombatRules.java')",a)
t=t[:a]+'''parser=method((refs/'ConfigSkill.java').read_text('utf-8'),'private static boolean _getBoolean(')
(refs/'SourceBindReference.java').write_text('package local.lito381.server; import java.util.Properties; final class SourceBindReference {\\n'+parser+'\\nstatic boolean configured(Properties p){return _getBoolean(p,"SLAY_BREAK_NOT_TELEPORT",true); }\\n}',encoding='utf-8')
t=(orig/'CombatRules.java').read_text('utf-8');needle='"INVIS_CAST_BREAK",';assert t.count(needle)==1;t=t.replace(needle,'"SLAY_BREAK_NOT_TELEPORT",'+needle)
needle='if(key.equals("INVIS_CAST_BREAK")) {';assert t.count(needle)==1;t=t.replace(needle,\'''if(key.equals("SLAY_BREAK_NOT_TELEPORT")) {
                String raw=skill.getProperty(key);boolean value=true;
                if(raw==null||raw.isBlank())defaults.add(key);else value=Boolean.parseBoolean(raw.trim());
                values.put(key,value?1.0:0.0);
            }
            else \'''+needle)
''' + t[z:]
t=t.replace("for n in ['', '$Scope']","for n in ['']")
(R/'build.py').write_text(t,encoding='utf-8')
