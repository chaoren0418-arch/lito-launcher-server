"""Source original-role identity and competition-bounded cube scans; isolated candidate only."""
from pathlib import Path
from datetime import datetime,timezone
import hashlib,json,subprocess,shutil,zipfile,os,sys
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
replay=len(sys.argv)>1
if replay:seed=read(Path(sys.argv[1]));S=Path(sys.argv[2]);run=Path(sys.argv[3]);parent=seed['base']
else:
 progress=read(D/'source-program-update-progress-current.json');pm=read(Path(progress['manifest']['path']));bm=read(Path(pm['build']['path']));seed=dict(asm=bm['asm']);parent=progress['runtime'];run=D/'source-cube-relation-builds'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
base=Path(parent['path']);assert sha(base)==parent['sha256']=='7cd7d86e67dbfa45e07f0799613e5039fa36e3530cb046b6d78019bc3495d42a'
inputs=run/'inputs';inputs.mkdir(parents=True)
source_root=Path('D:/lineage開發/1-伺服器/src/com/lineage/server')
source_paths=[source_root/'model/L1Cube.java',source_root/'model/Instance/L1EffectInstance.java',source_root/'model/Instance/L1PcInstance.java',source_root/'model/Instance/L1MonsterInstance.java',source_root/'utils/L1SpawnUtil.java',source_root/'model/skill/L1SkillUse.java',source_root/'model/combat/DamageCommitQueue.java',source_root/'model/combat/DamageCommit.java',source_root/'model/poison/L1DamagePoison.java',source_root/'model/combat/PhysicalHitCommitPlan.java',source_root/'model/combat/MagicHitCommitPlan.java',source_root/'model/skill/skillmode/SHOCK_STUN.java',source_root/'model/skill/skillmode/MIND_BREAK.java',source_root/'model/skill/skillmode/BONE_BREAK.java',source_root/'model/skill/skillmode/ARM_BREAKER.java',source_root/'model/L1Character.java',source_root/'model/combat/LegacyDefenseFeedbackPlan.java',source_root/'datatables/HitReactionSuppressTable.java',source_root/'model/L1AttackNpc.java',source_root/'model/L1MagicNpc.java',source_root.parent/'data/item_etcitem/wand/Lightning_Magic_Wand.java',source_root/'timecontroller/skill/EffectFirewallTimer.java',source_root/'world/WorldEffect.java',source_root/'model/Instance/L1NpcInstance.java',source_root/'model/L1AttackList.java',source_root/'model/skill/L1SkillId.java',source_root/'timecontroller/skill/EffectCubeBurnTimer.java',source_root/'timecontroller/skill/EffectCubeHarmonizeTimer.java',source_root/'timecontroller/skill/EffectCubeExecutor.java',source_root/'timecontroller/skill/EffectCubeEruptionTimer.java',source_root/'timecontroller/skill/EffectCubeShockTimer.java',source_root/'model/skill/L1SkillStop.java',source_root/'serverpackets/S_SkillSound.java']
if replay:
 for ref in seed['Source_inputs']:assert sha(Path(ref['path']))==ref['sha256']
source_refs=[]
for p in source_paths:
 snapshot=inputs/('Source-reference-'+p.name);shutil.copyfile(p,snapshot);source_refs.append(dict(original=pin(p),frozen=pin(snapshot)))
assert 'pc.receiveDamage(this._effect, 10, false, true);' in source_paths[0].read_text('utf-8')
assert 'case 80149:' in source_paths[1].read_text('utf-8')
assert 'final boolean suppressCombatFeedback = isCounterBarrier' in source_paths[2].read_text('utf-8')
assert 'attacker = effect.getMaster();' in source_paths[3].read_text('utf-8')
assert 'effect.setMaster(user);' in source_paths[4].read_text('utf-8')
names=['NativeSourceCubeRelation.java','SourceCubeRelationMethods.java','SourceCubeRelationDelta.java','SourceCubeRelationLoad.java','SourceCubeRelationPlan.java','SourceCubePresentationPlan.java','SourceCubeRemainderPlan.java','SourceCubeStatusPlan.java','SourceCubePlacementPlan.java','SourceGroundPlayerPlan.java','SourceGroundNpcPlan.java','SourceGroundPlacementPlan.java','SourceGroundQueuedPlan.java','SourceGroundOriginPlan.java','SourceWandReactionPlan.java','SourceNpcReactionPlan.java','SourceHitReactionPlan.java','FreezeAcceptedMethods.java','FreezeSourceTargetDelta.java','SourceRefRawNpcPlan.java','SourceCubeNpcQueuePlan.java','SourceDamageCapacityPlan.java','SourcePoisonNpcPlan.java','SourcePhysicalNpcPlan.java','SourceMagicNpcPlan.java','SourceSingleNpcPlan.java','SourceTargetNpcPlan.java','SourceSpecialNpcPlan.java','SourceRawNpcPlan.java','build_source_cube_relation.py']
for name in names:shutil.copyfile(R/name,inputs/name)
for ref in seed['asm']:assert sha(Path(ref['path']))==ref['sha256']
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');classes=run/'classes';cp=';'.join(map(str,[run/'tools',*[x['path']for x in seed['asm']]]));env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');log=run/'build.log'
v=dict(status='building',base=parent,asm=seed['asm'],inputs=[pin(inputs/n)for n in names],Source_inputs=[x['original']for x in source_refs],Source_frozen_references=source_refs,new_game_runtime_cases=0,new_SQL_connections=0,full_goal_achieved=False,all_doll_paths_complete=False,qualified_for_release=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:
  def invoke(a):subprocess.run(list(map(str,a)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=180)
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',run/'tools',*[inputs/n for n in names if n.endswith('.java')and n not in ['NativeSourceCubeRelation.java','SourceCubeRelationPlan.java','SourceCubePresentationPlan.java','SourceCubeRemainderPlan.java','SourceCubeStatusPlan.java','SourceCubePlacementPlan.java','SourceGroundPlayerPlan.java','SourceGroundNpcPlan.java','SourceGroundPlacementPlan.java','SourceGroundQueuedPlan.java','SourceGroundOriginPlan.java','SourceWandReactionPlan.java','SourceNpcReactionPlan.java','SourceHitReactionPlan.java','SourceRefRawNpcPlan.java','SourceCubeNpcQueuePlan.java','SourceDamageCapacityPlan.java','SourcePoisonNpcPlan.java','SourcePhysicalNpcPlan.java','SourceMagicNpcPlan.java','SourceSingleNpcPlan.java','SourceTargetNpcPlan.java','SourceSpecialNpcPlan.java','SourceRawNpcPlan.java']]])
  invoke([J/'java.exe','-Xverify:all','-cp',cp,'SourceCubeRelationMethods','splice',base,classes,S/'lib'])
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',';'.join(map(str,[classes,base,S/'lib/*'])),'-d',classes,inputs/'NativeSourceCubeRelation.java'])
  with zipfile.ZipFile(base)as z:entries={n:z.read(n)for n in z.namelist()}
  for p in classes.rglob('*.class'):entries[p.relative_to(classes).as_posix()]=p.read_bytes()
  jar=run/'candidate-source-cube-relation-unqualified.jar'
  with zipfile.ZipFile(jar,'w',zipfile.ZIP_DEFLATED)as z:
   for n,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(n,(1980,1,1,0,0,0)),data)
  invoke([J/'java.exe','-Xverify:all','-cp',cp,'SourceCubeRelationMethods','verify',base,jar,S/'lib']);invoke([J/'java.exe','-Xverify:all','-cp',cp,'SourceCubeRelationDelta',base,jar]);invoke([J/'java.exe','-Xverify:all','-cp',cp,'SourceCubeRelationLoad',jar,S/'lib'])
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',';'.join(map(str,[jar,S/'lib/*'])),'-d',run/'plan',inputs/'SourceCubeRelationPlan.java',inputs/'SourceCubePresentationPlan.java',inputs/'SourceCubeRemainderPlan.java',inputs/'SourceCubeStatusPlan.java',inputs/'SourceCubePlacementPlan.java',inputs/'SourceGroundPlayerPlan.java',inputs/'SourceGroundNpcPlan.java',inputs/'SourceGroundPlacementPlan.java',inputs/'SourceGroundQueuedPlan.java',inputs/'SourceGroundOriginPlan.java',inputs/'SourceWandReactionPlan.java',inputs/'SourceNpcReactionPlan.java',inputs/'SourceHitReactionPlan.java',inputs/'SourceRefRawNpcPlan.java',inputs/'SourceCubeNpcQueuePlan.java',inputs/'SourceDamageCapacityPlan.java',inputs/'SourcePoisonNpcPlan.java',inputs/'SourcePhysicalNpcPlan.java',inputs/'SourceMagicNpcPlan.java',inputs/'SourceSingleNpcPlan.java',inputs/'SourceTargetNpcPlan.java',inputs/'SourceSpecialNpcPlan.java',inputs/'SourceRawNpcPlan.java'])
  invoke([J/'java.exe','-Xverify:all','-cp',';'.join(map(str,[run/'plan',jar,S/'lib/*'])),'local.lito381.server.SourceCubeRelationPlan',inputs/'Source-reference-EffectCubeExecutor.java'])
  invoke([J/'java.exe','-Xverify:all','-cp',';'.join(map(str,[run/'plan',jar,S/'lib/*'])),'local.lito381.server.SourceCubePresentationPlan',inputs/'Source-reference-EffectCubeExecutor.java',inputs/'Source-reference-S_SkillSound.java'])
  invoke([J/'java.exe','-Xverify:all','-cp',';'.join(map(str,[run/'plan',jar,S/'lib/*'])),'local.lito381.server.SourceCubeRemainderPlan'])
  invoke([J/'java.exe','-Xverify:all','-cp',';'.join(map(str,[run/'plan',jar,S/'lib/*'])),'local.lito381.server.SourceCubeStatusPlan'])
  invoke([J/'java.exe','-Xverify:all','-cp',';'.join(map(str,[run/'plan',jar,S/'lib/*'])),'local.lito381.server.SourceCubePlacementPlan'])
  invoke([J/'java.exe','-Xverify:all','-cp',';'.join(map(str,[run/'plan',jar,S/'lib/*'])),'local.lito381.server.SourceGroundPlayerPlan',inputs/'Source-reference-L1AttackList.java',inputs/'Source-reference-L1SkillId.java'])
  invoke([J/'java.exe','-Xverify:all','-cp',';'.join(map(str,[run/'plan',jar,S/'lib/*'])),'local.lito381.server.SourceGroundNpcPlan'])
  invoke([J/'java.exe','-Xverify:all','-cp',';'.join(map(str,[run/'plan',jar,S/'lib/*'])),'local.lito381.server.SourceGroundPlacementPlan'])
  invoke([J/'java.exe','-Xverify:all','-cp',';'.join(map(str,[run/'plan',jar,S/'lib/*'])),'local.lito381.server.SourceGroundQueuedPlan'])
  invoke([J/'java.exe','-Xverify:all','-cp',';'.join(map(str,[run/'plan',jar,S/'lib/*'])),'local.lito381.server.SourceGroundOriginPlan'])
  invoke([J/'java.exe','-Xverify:all','-cp',';'.join(map(str,[run/'plan',jar,S/'lib/*'])),'local.lito381.server.SourceWandReactionPlan'])
  invoke([J/'java.exe','-Xverify:all','-cp',';'.join(map(str,[run/'plan',jar,S/'lib/*'])),'local.lito381.server.SourceNpcReactionPlan'])
  invoke([J/'java.exe','-Xverify:all','-cp',';'.join(map(str,[run/'plan',jar,S/'lib/*'])),'local.lito381.server.SourceHitReactionPlan'])
  invoke([J/'java.exe','-Xverify:all','-cp',';'.join(map(str,[run/'plan',jar,S/'lib/*'])),'local.lito381.server.SourceRefRawNpcPlan'])
  invoke([J/'java.exe','-Xverify:all','-cp',';'.join(map(str,[run/'plan',jar,S/'lib/*'])),'local.lito381.server.SourceCubeNpcQueuePlan'])
  invoke([J/'java.exe','-Xverify:all','-cp',';'.join(map(str,[run/'plan',jar,S/'lib/*'])),'local.lito381.server.SourceDamageCapacityPlan'])
  invoke([J/'java.exe','-Xverify:all','-cp',';'.join(map(str,[run/'plan',jar,S/'lib/*'])),'local.lito381.server.SourcePoisonNpcPlan'])
  invoke([J/'java.exe','-Xverify:all','-cp',';'.join(map(str,[run/'plan',jar,S/'lib/*'])),'local.lito381.server.SourcePhysicalNpcPlan'])
  invoke([J/'java.exe','-Xverify:all','-cp',';'.join(map(str,[run/'plan',jar,S/'lib/*'])),'local.lito381.server.SourceMagicNpcPlan'])
  invoke([J/'java.exe','-Xverify:all','-cp',';'.join(map(str,[run/'plan',jar,S/'lib/*'])),'local.lito381.server.SourceSingleNpcPlan'])
  invoke([J/'java.exe','-Xverify:all','-cp',';'.join(map(str,[run/'plan',jar,S/'lib/*'])),'local.lito381.server.SourceTargetNpcPlan'])
  invoke([J/'java.exe','-Xverify:all','-cp',';'.join(map(str,[run/'plan',jar,S/'lib/*'])),'local.lito381.server.SourceSpecialNpcPlan'])
  invoke([J/'java.exe','-Xverify:all','-cp',';'.join(map(str,[run/'plan',jar,S/'lib/*'])),'local.lito381.server.SourceRawNpcPlan'])
 v.update(pure_cube_original_Source_identity_match_relation_contract_scenarios=33,inherited_cube_Source_presentation_relation_contract_scenarios=27,inherited_cube_Ignition_Harmony_MP_status_contract_scenarios=25,inherited_cube_Quake_Shock_status_contract_scenarios=24,inherited_cube_material_factory_and_cadence_contract_scenarios=20,inherited_ground_player_Source_SKD3_contract_scenarios=35,inherited_ground_natural_NPC_contract_scenarios=26,inherited_material_ground_staged_live_entity_contract_scenarios=18,inherited_ground_queued_lease_scene_tuple_contract_scenarios=18,inherited_ground_lease_pulse_tick_contract_scenarios=16,inherited_wand_reaction_NPC_packet_contract_scenarios=16,inherited_npc_reaction_Ref_snapshot_contract_scenarios=16,inherited_hit_reaction_readonly_contract_scenarios=14,inherited_Ref_raw_NPC_ABI_contract_scenarios=12,inherited_raw_NPC_admission_contract_scenarios=12,inherited_special_npc_contract_scenarios=24,inherited_target_npc_contract_scenarios=18,inherited_single_npc_contract_scenarios=14,inherited_queue_contract_scenarios=33,inherited_damage_capacity_contract_scenarios=16,inherited_poison_queue_contract_scenarios=15,inherited_physical_queue_contract_scenarios=30,inherited_magic_identity_erase_contract_scenarios=16,status='built-Source-cube-original-role-identity-and-match-bounded-scan-program-unqualified',runtime=pin(jar))
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-3500:]);raise
finally:
 v['log']=pin(log);write(run/'manifest.json',v)
 if not replay:write(D/'source-cube-relation-build-current.json',dict(status=v['status'],manifest=pin(run/'manifest.json'),**({'runtime':v['runtime']}if'runtime'in v else{})))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'])))
