from pathlib import Path
import json,hashlib
R=Path(__file__).resolve().parent;W=R.parent/'water-life-material'
t=(W/'WaterLifeMaterialProbe.java').read_text('utf-8').replace('WaterLifeMaterialProbe','NegativeMaterialProbe')
t=t.replace('/** Real player-target skill170 packets and effects, independently derived Source cost. */','/** Actual selected roots with existing deterministic roll arguments; one native packet case per skill/phase/configuration. */')
t=t.replace('static final int[] SKILLS={170};','static final int[] SKILLS={29,152,153};static int selectedSkill,selectedMode;')
t=t.replace('return BattleSpellCatalog.waterLifeSpell(skill);','return skill==153?BattleSpellCatalog.eraseMagicSpell(skill):BattleSpellCatalog.entangleSpell(skill);')
start=t.index(' static void cast(');end=t.index(' static void setup(',start)
t=t[:start]+''' static void cast(ClientSession pc,int skill,Object row)throws Exception {
  byte[] packet=new Protocol.Writer().c(Protocol.C_USE_SPELL).c((skill-1)>>3).c((skill-1)&7).d(targetId).h((int)get(pc,"positionX")+1).h((int)get(pc,"positionY")).bytes();
  if(selectedMode==12)call(pc,"handle",new Class[]{byte[].class},packet);
  else call(pc,skill==153?"useEraseMagicSpell":"useEntangleSpell",new Class[]{byte[].class,row.getClass(),int.class},packet,row,selectedMode==1?100:1);
 }
 static void probability(int skill)throws Exception{
  var f=CombatDatabase.class.getDeclaredField("current");f.setAccessible(true);var data=(CombatDatabase.Snapshot)f.get(null);
  var rows=new HashMap<>(data.controls().probabilities());rows.put(skill,new SourceSkillControls.Probability(skill,50,50,50,0,0,0,0,false,false,0,0,0));
  f.set(null,copy(data,Map.of("controls",copy(data.controls(),Map.of("probabilities",rows)))));
 }
 static boolean active(ClientSession pc,int skill)throws Exception{
  return skill==153?((BattleEraseMagicStatus)get(pc,"eraseMagicStatus")).active(System.nanoTime()):((BattleTimedStatus)get(pc,"timedStatus")).speed(System.nanoTime()).type()==BattleTimedStatus.SLOW;
 }
'''+t[end:]
t=t.replace('Map.of("type",2,"originalIntelligence",10)','Map.of("type",selectedSkill==29?3:2,"originalIntelligence",10)')
t=t.replace('((BattleWaterLifeStatus)get(p,"waterLifeStatus")).cancel();','((BattleTimedStatus)get(p,"timedStatus")).clear();((BattleEraseMagicStatus)get(p,"eraseMagicStatus")).clear();')
t=t.replace('set(pc,"learnedSkills",new HashSet<>(Set.of(2,170)));targetId=peer.playerObjectId();','''set(pc,"learnedSkills",new HashSet<>(Set.of(2,29,152,153)));targetId=peer.playerObjectId();
  if(!world){var arena=ArenaMapCatalog.forMap(hub.battleMapId((long)get(pc,"currentRoomId"),pc));var point=arena.spawns().get(0);set(pc,"positionX",point.x());set(pc,"positionY",point.y());}
  set(peer,"positionX",(int)get(pc,"positionX")+1);set(peer,"positionY",(int)get(pc,"positionY"));
''')
start=t.index(' static void exercise(');end=t.index(' public static void main(',start)
t=t[:start]+''' static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int mode)throws Exception {
  setup(pc,peer,hub,world);int skill=selectedSkill;Object row=spell(skill);probability(skill);
  if(mode==3)set(pc,"playerMp",0);if(mode==5)targetId=0x7feeeeeeL;if(mode==6)set(peer,"playerHp",0);
  if(mode==7)set(peer,"positionX",(int)get(pc,"positionX")+40);if(mode==8)set(peer,"invisible",true);
  if(mode==9)targetId=pc.playerObjectId();if(mode==10)((BattleTimedStatus)get(peer,"timedStatus")).activateCounterMagic(System.nanoTime(),60);
  if(mode==11)((BattleTimedStatus)get(peer,"timedStatus")).activateHaste(System.nanoTime(),60,43);
  int item=Math.toIntExact((long)call(row,"itemConsumeId",new Class[]{})),count=(int)call(row,"itemConsumeCount",new Class[]{});if(mode!=2)NativeDeItemPublication.grant(pc,item,count+3);
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   if(mode==4){NativeCastMaterialProbe.cast(pc,2);long ready=((Map<Integer,Long>)get(pc,"spellReadyNanos")).get(2);while(System.nanoTime()-ready<=0)Thread.sleep(1);need(!pc.nativeCastTimelineState().timeline.canReserveSkillAction(),"genuine prior Light window");}
   int mp=(int)get(pc,"playerMp"),hp=(int)get(pc,"playerHp"),bytes=wire.raw.size();boolean accepted=mode==0||mode==1||mode>=10||mode==4&&before;
   cast(pc,skill,row);
   need((int)get(pc,"playerMp")==Math.max(0,Math.min(1000,mp-(accepted?cost(pc,skill,row):0)))&&(int)get(pc,"playerHp")==hp,"Source resources mode="+mode+" skill="+skill+" world="+world);
   need(pc.nativeDeInventory(0).countCatalog(item)==(mode==2?0:count+3-(accepted?count:0)),"material once after valid target and resources");
   need(((Map<?,?>)get(pc,"spellReadyNanos")).containsKey(skill)==accepted,"reuse follows accepted material");
   if(mode!=12){boolean applied=accepted&&mode!=1&&(skill==153||mode!=10&&mode!=11);need(active(peer,skill)==applied&&!active(pc,skill),"actual target status follows Source roll/counter/haste rules mode="+mode+" skill="+skill);}
   if(mode==4){need((wire.raw.size()>bytes)==before,"shared Source time window");if(before)gaps++;}
   var self=collect(wire);var peers=collect(other);
   if(mode==2)need(self.equals(List.of(HexFormat.of().formatHex(Protocol.messageCode(299))))&&peers.isEmpty(),"missing material299");
   if(mode==0||mode==1||mode==10||mode==11)goldens.add("skill="+skill+" world="+world+" mode="+mode+" hp="+get(pc,"playerHp")+" mp="+get(pc,"playerMp")+" self="+self+" peer="+peers);
   need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0&&SourceActionPairProbe.state(pc).queue.isEmpty(),"unlocked output and drained receipts");
  }cases++;
 }
'''+t[end:]
t=t.replace('for(int mode=0;mode<8;mode++){','for(int skill:SKILLS)for(int mode=0;mode<13;mode++){selectedSkill=skill;selectedMode=mode;')
t=t.replace('app+"-"+mode','app+"-"+skill+"-"+mode')
(R/'NegativeMaterialProbe.java').write_text(t,encoding='utf-8')
t=(W/'test.py').read_text('utf-8').replace('WaterLifeMaterialProbe','NegativeMaterialProbe').replace("v[mode]['cases']==64","v[mode]['cases']==312").replace("(8 if mode=='before' else 0)","(24 if mode=='before' else 0)")
(R/'test.py').write_text(t,encoding='utf-8')
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
(R/'test-adaptation.json').write_text(json.dumps(dict(inputs=[dict(path=str(p),sha256=sha(p))for p in[W/'WaterLifeMaterialProbe.java',W/'test.py',Path(__file__)]],scope='3 skills, two phases, persistence flags/application variants; actual original roll-fixture root and native packet per variant. NPC and companion behavior not yet qualified.'),indent=2),encoding='utf-8')
