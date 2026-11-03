from pathlib import Path
import json,hashlib
R=Path(__file__).resolve().parent;W=R.parent/'water-life-material'
t=(W/'WaterLifeMaterialProbe.java').read_text('utf-8').replace('WaterLifeMaterialProbe','ArmorMaterialProbe').replace('waterLifeSpell','armorBreakSpell').replace('170','112')
t=t.replace('.c((skill-1)&7).d(targetId).bytes()', '.c((skill-1)&7).bytes()')
t=t.replace('Map.of("type",2,"originalIntelligence",10)', 'Map.of("type",4,"originalIntelligence",10)')
t=t.replace('((BattleWaterLifeStatus)get(p,"waterLifeStatus")).cancel();','set(p,"classicWorldArmorBreakExpiresAtNanos",0L);hub.cancelArmorBreak((long)get(p,"currentRoomId"),p.playerObjectId());')
needle='set(pc,"learnedSkills",new HashSet<>(Set.of(2,112)));targetId=peer.playerObjectId();'
assert needle in t
t=t.replace(needle,needle+'''
  set(pc,"lastCombatTargetObjectId",peer.playerObjectId());
  if(!world){var arena=ArenaMapCatalog.forMap(hub.battleMapId((long)get(pc,"currentRoomId"),pc));var point=arena.spawns().get(0);set(pc,"positionX",point.x());set(pc,"positionY",point.y());}
  set(peer,"positionX",(int)get(pc,"positionX")+1);set(peer,"positionY",(int)get(pc,"positionY"));
''')
start=t.index(' static void exercise(');end=t.index(' public static void main(',start)
t=t[:start]+''' static boolean active(ClientSession target,GameHub hub,boolean world)throws Exception{
  return world?(boolean)call(target,"classicWorldArmorBreakActive",new Class[]{long.class},System.nanoTime()):hub.armorBreakActive((long)get(target,"currentRoomId"),target.playerObjectId(),System.nanoTime());
 }
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int mode)throws Exception {
  setup(pc,peer,hub,world);var row=BattleSpellCatalog.armorBreakSpell(112);
  if(mode==3)set(pc,"playerMp",0);if(mode==5)set(pc,"playerHp",row.hpCost());if(mode==11)set(pc,"playerHp",row.hpCost()+1);
  if(mode==6)set(peer,"playerHp",0);if(mode==7)set(peer,"positionX",(int)get(pc,"positionX")+40);
  if(mode==8)set(peer,"invisible",true);if(mode==9)set(peer,"observer",true);
  if(mode==10){set(pc,"lastCombatTargetObjectId",0x7feeeeeeL);set(peer,"positionX",(int)get(pc,"positionX")+40);}
  int item=Math.toIntExact(row.itemConsumeId()),count=row.itemConsumeCount();if(mode!=2)NativeDeItemPublication.grant(pc,item,count+3);
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   if(mode==4){NativeCastMaterialProbe.cast(pc,2);long ready=((Map<Integer,Long>)get(pc,"spellReadyNanos")).get(2);while(System.nanoTime()-ready<=0)Thread.sleep(1);need(!pc.nativeCastTimelineState().timeline.canReserveSkillAction(),"genuine prior Light window");}
   int mp=(int)get(pc,"playerMp"),hp=(int)get(pc,"playerHp"),bytes=wire.raw.size();boolean accepted=mode==0||mode==1||mode==11||mode==4&&before;
   cast(pc,112,row);
   need((int)get(pc,"playerMp")==Math.max(0,Math.min(1000,mp-(accepted?cost(pc,112,row):0)))&&(int)get(pc,"playerHp")==hp-(accepted?row.hpCost():0),"Source MP/HP resources mode="+mode+" world="+world);
   need(pc.nativeDeInventory(0).countCatalog(item)==(mode==2?0:count+3-(accepted?count:0)),"material once after valid target and resources");
   need(((Map<?,?>)get(pc,"spellReadyNanos")).containsKey(112)==accepted,"reuse follows accepted material");
   need(active(peer,hub,world)==(accepted&&mode!=1)&&!active(pc,hub,world),"actual target effect follows Source probability; failed roll still pays");
   if(mode==4){need((wire.raw.size()>bytes)==before,"shared Source time window");if(before)gaps++;}
   var self=collect(wire);var peers=collect(other);
   int code=mode==2?299:mode==3?278:mode==5?279:0;
   if(code!=0)need(self.equals(List.of(HexFormat.of().formatHex(Protocol.messageCode(code))))&&peers.isEmpty(),"original resource-specific failure message");
   if(mode==0||mode==1||mode==11)goldens.add("world="+world+" mode="+mode+" hp="+get(pc,"playerHp")+" mp="+get(pc,"playerMp")+" self="+self+" peer="+peers);
   need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0&&SourceActionPairProbe.state(pc).queue.isEmpty(),"all output unlocked; owned receipts drained");
  }cases++;
 }
'''+t[end:]
t=t.replace('mode<8','mode<12')
t=t.replace('Map.of("battleNaturalHpRegenEnabled",false,','Map.of("armorBreakFixtureD100",mode==1?100:1,"battleNaturalHpRegenEnabled",false,')
t=t.replace('exercise(p.session,m.session,hub,world,mode);','set(f.session,"observer",true);set(o.session,"observer",true);exercise(p.session,m.session,hub,world,mode);')
(R/'ArmorMaterialProbe.java').write_text(t,encoding='utf-8')
t=(W/'test.py').read_text('utf-8').replace('WaterLifeMaterialProbe','ArmorMaterialProbe').replace("v[mode]['cases']==64","v[mode]['cases']==96")
# The direct comparison is the last tested pair candidate, before both Armor Break layers.
t=t.replace("b=read(R/'build/build.json');", "b=read(R/'build/build.json');b['base']=read(R.parent/'protection-material/build/build.json')['runtime'];")
(R/'test.py').write_text(t,encoding='utf-8')
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
(R/'test-adaptation.json').write_text(json.dumps(dict(inputs=[dict(path=str(p),sha256=sha(p)) for p in [W/'WaterLifeMaterialProbe.java',W/'test.py',Path(__file__)]],scope='selector-only skill112, real PC target, HP threshold/MP/material, probability1/100, Source-only memory fixture; behavior unqualified'),indent=2),encoding='utf-8')
