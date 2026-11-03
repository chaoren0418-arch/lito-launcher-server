"""Sequential, hash-bound candidate acceptance; never deploys or changes SQL."""
from datetime import datetime, timezone
from pathlib import Path
import hashlib
import json
import os
import subprocess
import sys

R = Path(__file__).resolve().parent
S = R.parents[1]
B = R / 'build'
B.mkdir(exist_ok=True)
receipt_path = B / 'combat-acceptance-current.json'
sha = lambda path: hashlib.sha256(path.read_bytes()).hexdigest()
receipt = {
    'started_utc': datetime.now(timezone.utc).isoformat(),
    'status': 'running',
    'runner_sha256': sha(Path(__file__)),
    'runners': [],
    'formal_deployment_changed': False,
    'full_behavior_parity_verified': False,
    'test_jvm_options': '-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m',
    'verification_jvm_options': '-Xms32m -Xmx768m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m',
}
verification_inputs = sorted([*R.glob('*.java'), *R.glob('*.py'),
    *[S / name for name in ['tools/dragon-combat/Regression.java',
       'tools/shared-database/SourceLoginRegression.java', 'tools/durable-settlement/FlowRegression.java',
       'tools/native-room-state/NativeDirectoryRegression.java', 'tools/native-room-state/MovementRegression.java',
       'tools/room-rules/RoomRulesRegression.java']]])
receipt['verification_inputs'] = [{'path': str(path.relative_to(S)), 'sha256': sha(path)} for path in verification_inputs]


def save():
    receipt_path.write_text(json.dumps(receipt, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def run(name, *arguments):
    print('RUN ' + name, flush=True)
    log = B / ('acceptance-' + str(len(receipt['runners']) + 1).zfill(2) + '-' + name.removesuffix('.py') + '.log')
    with log.open('w', encoding='utf-8') as output:
        environment = dict(os.environ)
        options = receipt['verification_jvm_options'] if name == 'verify_combat_preservation.py' else receipt['test_jvm_options']
        environment['JAVA_TOOL_OPTIONS'] = (environment.get('JAVA_TOOL_OPTIONS', '') + ' ' + options).strip()
        result = subprocess.run([sys.executable, '-B', '-X', 'utf8', str(R / name), *arguments],
                                cwd=S, stdout=output, stderr=subprocess.STDOUT, env=environment)
    receipt['runners'].append({'runner': name, 'arguments': list(arguments),
                               'exit_code': result.returncode, 'log': str(log.relative_to(S)),
                               'log_sha256': sha(log)})
    save()
    print('RESULT ' + name + ' ' + str(result.returncode), flush=True)
    if result.returncode:
        print(log.read_text(encoding='utf-8', errors='replace')[-5000:])
        raise RuntimeError('Failed: ' + name)


save()
try:
    run('build.py')
    candidate = B / 'candidate.jar'
    expected = sha(candidate)
    receipt['candidate_sha256'] = expected
    # Both full builds start from the pinned base and clear only generated classes.
    run('build.py')
    if sha(candidate) != expected:
        raise RuntimeError('Candidate is not reproducible from unchanged inputs')
    receipt['deterministic_rebuild'] = True
    entry = S / 'config/combat.properties'
    pointers = dict(line.strip().split('=', 1) for line in entry.read_text(encoding='utf-8').splitlines()
                    if line.strip() and not line.lstrip().startswith(('#', '!')))
    inputs = [entry, *(entry.parent / pointers[key] for key in ('source.skill.file', 'source.damage.file', 'source.character.file'))]
    receipt['control_inputs'] = [{'path': str(path.resolve()), 'sha256': sha(path)} for path in inputs]
    run('probe_special_buff_origins.py')
    receipt['source_map_inputs'] = [{'path':path,'sha256':digest} for path,digest in
        (line.split('\t') for line in (B/'source-blessing-map-inputs.tsv').read_text(encoding='utf-8').splitlines())]
    for item in receipt['source_map_inputs']:
        if sha(Path(item['path'])) != item['sha256']:
            raise RuntimeError('Source map changed while loading the candidate snapshot')
    save()
    run('run_npc_physical_test.py')
    run('run_npc_poison_test.py')
    run('run_poison_atomic_test.py')
    run('run_poison_paralysis_test.py')
    run('run_poison_callback_test.py')
    run('run_silence_callback_test.py')
    run('run_death_cancellation_test.py')
    run('run_paralysis_callback_test.py')
    run('run_curse_callback_test.py')
    run('run_monster_paralysis_test.py')
    run('run_monster_curse_callback_test.py')
    run('run_monster_silence_source_test.py')
    run('run_monster_silence_callback_test.py')
    run('run_monster_damage_source_test.py')
    run('run_monster_damage_callback_test.py')
    run('run_monster_receive_source_test.py')
    run('run_monster_receive_test.py')
    run('run_npc_damage_queue_test.py')
    run('run_world_physical_source_test.py')
    run('run_world_physical_commit_test.py')
    run('run_magic_hit_plan_source_test.py')
    run('run_magic_hit_plan_gap_probe.py')
    run('run_world_magic_commit_test.py')
    run('run_world_multi_magic_commit_test.py')
    run('run_freeze_effect_source_test.py')
    run('run_world_freeze_gap_probe.py')
    run('run_world_freeze_commit_test.py')
    run('run_foe_mode_source_test.py')
    run('run_world_foe_gap_probe.py')
    run('run_world_foe_commit_test.py')
    run('run_pc_magic_commit_source_test.py')
    run('run_pc_damage_commit_source_test.py')
    run('run_pc_damage_queue_source_test.py')
    run('run_player_commit_ingress_probe.py')
    run('run_player_foe_test.py')
    run('run_native_target_contract_test.py')
    run('run_monster_poison_paralysis_source_test.py')
    run('run_monster_poison_paralysis_callback_test.py')
    run('run_incoming_source_test.py')
    for name in ['run_combat_test.py', 'run_class_damage_test.py', 'run_class_magic_test.py', 'run_counter_detection_test.py', 'run_player_magic_test.py', 'run_player_mirror_test.py', 'run_player_damage_test.py', 'run_patience_test.py', 'run_kiringku_test.py', 'run_companion_damage_test.py', 'run_npc_magic_test.py', 'run_world_companion_physical_test.py', 'run_npc_debuff_test.py', 'run_pollute_healing_test.py', 'run_group_healing_test.py', 'run_companion_shock_test.py', 'run_centered_companion_freeze_test.py', 'run_line_companion_test.py', 'run_ranged_damage_test.py', 'run_probability_test.py', 'run_return_nature_test.py', 'run_dragon_combat_test.py', 'run_awaken_combat_test.py', 'run_buff_combat_test.py', 'run_weapon_buff_test.py', 'run_special_buff_test.py', 'run_blessing_npc_test.py', 'run_combat_death_test.py',
                 'run_forced_combat_test.py', 'run_travel_test.py', 'run_equipment_test.py',
                 'test.py', 'verify_combat_preservation.py', 'run_combat_inventory.py', 'probe_group_healing_source.py',
                 'probe_probability_targets.py', 'run_combat_parity_audit.py']:
        if sha(candidate) != expected:
            raise RuntimeError('Candidate changed during acceptance')
        run(name, *(['--jar', str(candidate)] if name == 'run_combat_parity_audit.py' else []))
    if sha(candidate) != expected:
        raise RuntimeError('Candidate changed during acceptance')
    for item in [*receipt['control_inputs'], *receipt['source_map_inputs']]:
        if sha(Path(item['path'])) != item['sha256']:
            raise RuntimeError('Combat controls changed during acceptance')
    for item in receipt['verification_inputs']:
        if sha(S / item['path']) != item['sha256']:
            raise RuntimeError('Verification source changed during acceptance: ' + item['path'])
    source_contract = json.loads((B / 'group-healing-source-current.json').read_text(encoding='utf-8'))
    if source_contract['candidate_sha256'] != expected:
        raise RuntimeError('Source healing contract is not bound to current candidate')
    npc_contract = json.loads((B / 'npc-physical-oracle-current.json').read_text(encoding='utf-8'))
    if npc_contract['status'] != 'passed' or npc_contract['candidate_sha256'] != expected:
        raise RuntimeError('Current compiled source NPC formula oracle is not bound to candidate')
    receipt['source_npc_formula_oracle_sha256'] = sha(B / 'npc-physical-oracle-current.json')
    poison_contract = json.loads((B / 'npc-poison-oracle-current.json').read_text(encoding='utf-8'))
    if poison_contract['status'] != 'passed' or poison_contract['candidate_sha256'] != expected:
        raise RuntimeError('Current compiled source NPC poison producer oracle is not bound to candidate')
    receipt['source_npc_poison_oracle_sha256'] = sha(B / 'npc-poison-oracle-current.json')
    atomic_contract = json.loads((B / 'poison-atomic-oracle-current.json').read_text(encoding='utf-8'))
    if atomic_contract['status'] != 'passed' or atomic_contract['candidate_sha256'] != expected:
        raise RuntimeError('Poison state regression source producer oracle is not bound to candidate')
    receipt['source_poison_atomic_oracle_sha256'] = sha(B / 'poison-atomic-oracle-current.json')
    paralysis_contract = json.loads((B / 'poison-paralysis-oracle-current.json').read_text(encoding='utf-8'))
    if paralysis_contract['status'] != 'passed' or paralysis_contract['candidate_sha256'] != expected:
        raise RuntimeError('Independent poison/curse source lease contract is not bound to candidate')
    if not paralysis_contract['source_lease_contract']['source_CAS_callback_bodies_executed']:
        raise RuntimeError('Current source lease CAS/callback bodies were not executed')
    receipt['source_poison_paralysis_oracle_sha256'] = sha(B / 'poison-paralysis-oracle-current.json')
    callback_contract = json.loads((B / 'poison-callback-oracle-current.json').read_text(encoding='utf-8'))
    if callback_contract['status'] != 'passed' or callback_contract['candidate_sha256'] != expected:
        raise RuntimeError('Damage poison callback oracle is not bound to candidate')
    if not callback_contract['source_lease_contract']['source_CAS_callback_bodies_executed']:
        raise RuntimeError('Damage callback source lease bodies were not executed')
    if not callback_contract['source_lease_contract']['source_damage_poison_final_attacker_constructor_and_both_callback_reads_verified']:
        raise RuntimeError('Source damage poison immutable attacker constructor/callback shape was not verified')
    receipt['source_poison_callback_oracle_sha256'] = sha(B / 'poison-callback-oracle-current.json')
    silence_contract = json.loads((B / 'poison-silence-callback-oracle-current.json').read_text(encoding='utf-8'))
    if silence_contract['status'] != 'passed' or silence_contract['candidate_sha256'] != expected:
        raise RuntimeError('Silence callback oracle is not bound to candidate')
    if not silence_contract['source_lease_contract']['source_silence_compiled_initial_palette310_and_cleanup_palette311_order_verified']:
        raise RuntimeError('Source silence publication/cleanup ordering was not verified')
    receipt['source_silence_callback_oracle_sha256'] = sha(B / 'poison-silence-callback-oracle-current.json')
    death_contract = json.loads((B / 'death-cancellation-oracle-current.json').read_text(encoding='utf-8'))
    if death_contract['status'] != 'passed' or death_contract['candidate_sha256'] != expected:
        raise RuntimeError('Death cancellation oracle is not bound to candidate')
    death_source = death_contract['source_death_contract']
    if not (death_source['source_Death_TYPE_LOGIN_self_no_cost_cast_shape_verified']
            and death_source['source_CANCELLATION_range_cure_body_executed']
            and death_source['source_death_ignores_skill_status_immunity_but_keeps_cooking_immunity']
            and death_source['source_execution_scenarios'] == 8
            and not death_source['source_boot_or_SQL_executed']):
        raise RuntimeError('Source death TYPE_LOGIN and cancellation range/cure contract was not verified')
    if len(death_contract['source_files']) != 69 or len(death_contract['generated_executable_files']) != 20:
        raise RuntimeError('Incomplete death source/executable bindings')
    receipt['source_death_cancellation_oracle_sha256'] = sha(B / 'death-cancellation-oracle-current.json')
    paralysis_callback_contract = json.loads((B / 'poison-paralysis-callback-oracle-current.json').read_text(encoding='utf-8'))
    if paralysis_callback_contract['status'] != 'passed' or paralysis_callback_contract['candidate_sha256'] != expected:
        raise RuntimeError('Paralysis callback oracle is not bound to candidate')
    callback_source = paralysis_callback_contract['source_paralysis_callback_contract']
    if not (callback_source['source_actual_paralysis_callback_cure_timer_bodies_executed']
            and callback_source['source_actual_CAS_callback_bases_used']
            and callback_source['source_begin_and_stage_deadlines_after_publication_and_late_callbacks_verified']
            and callback_source['source_controlled_scheduler_clock_used']
            and callback_source['source_PC_alive_dead_and_schedule_rejection_order_verified']
            and callback_source['source_callback_body_count'] == 9
            and callback_source['source_execution_scenarios'] == 12
            and not callback_source['source_boot_or_SQL_or_real_scheduler_executed']):
        raise RuntimeError('Current source paralysis callback/cure/timer bodies were not executed')
    if len(paralysis_callback_contract['source_files']) != 64 or len(paralysis_callback_contract['generated_executable_files']) != 27:
        raise RuntimeError('Incomplete paralysis callback source/executable bindings')
    receipt['source_paralysis_callback_oracle_sha256'] = sha(B / 'poison-paralysis-callback-oracle-current.json')
    curse_contract = json.loads((B / 'curse-callback-oracle-current.json').read_text(encoding='utf-8'))
    if curse_contract['status'] != 'passed' or curse_contract['candidate_sha256'] != expected:
        raise RuntimeError('Ordinary curse callback oracle is not bound to candidate')
    curse_source = curse_contract['source_curse_callback_contract']
    if not (curse_source['source_actual_curse_callback_cure_timer_bodies_executed']
            and curse_source['source_actual_CAS_callback_bases_used']
            and curse_source['source_modes_212_291_verified']
            and curse_source['source_lock_before_server_state_and_cleanup_color_before_unlock_verified']
            and curse_source['source_begin_and_stage_deadlines_after_publication_and_late_callbacks_verified']
            and curse_source['source_controlled_scheduler_clock_used']
            and curse_source['source_PC_alive_dead_and_schedule_rejection_order_verified']
            and curse_source['source_callback_body_count'] == 8
            and curse_source['source_execution_scenarios'] == 24
            and not curse_source['source_boot_or_SQL_or_real_scheduler_executed']):
        raise RuntimeError('Current source ordinary curse callback/cure/timer bodies were not executed')
    if len(curse_contract['source_files']) != 64 or len(curse_contract['generated_executable_files']) != 36:
        raise RuntimeError('Incomplete ordinary curse source/executable bindings')
    receipt['source_curse_callback_oracle_sha256'] = sha(B / 'curse-callback-oracle-current.json')
    monster_contract = json.loads((B / 'monster-paralysis-oracle-current.json').read_text(encoding='utf-8'))
    if monster_contract['status'] != 'passed' or monster_contract['candidate_sha256'] != expected:
        raise RuntimeError('WORLD monster independent paralysis oracle is not bound to candidate')
    monster_source = monster_contract['source_monster_curse_contract']
    if not (monster_source['source_actual_NPC_curse_callback_cure_timer_bodies_executed']
            and monster_source['source_actual_CAS_callback_bases_used']
            and monster_source['source_NPC_target_is_not_PC_fixture_type']
            and monster_source['source_NPC_no_PC_message_lock_or_unlock_packets']
            and monster_source['source_current_CURSE_PARALYZE_compiled_overloads_NPC_zero_delay_mode_zero_verified']
            and monster_source['source_NPC_delayed_zero_delay_callback_and_full_duration_verified']
            and monster_source['source_controlled_scheduler_clock_used']
            and monster_source['source_factory_overloads_checked'] == 2
            and monster_source['source_callback_body_count'] == 8
            and monster_source['source_execution_scenarios'] == 6
            and not monster_source['source_boot_or_SQL_or_real_scheduler_executed']):
        raise RuntimeError('Current source NPC curse callback/executor bodies were not verified')
    if len(monster_contract['source_files']) != 66 or len(monster_contract['generated_executable_files']) != 46:
        raise RuntimeError('Incomplete WORLD monster paralysis source/executable bindings')
    receipt['source_monster_paralysis_oracle_sha256'] = sha(B / 'monster-paralysis-oracle-current.json')
    monster_callback_contract = json.loads((B / 'monster-curse-callback-oracle-current.json').read_text(encoding='utf-8'))
    if monster_callback_contract['status'] != 'passed' or monster_callback_contract['candidate_sha256'] != expected:
        raise RuntimeError('WORLD monster curse callback oracle is not bound to candidate')
    if monster_callback_contract['source_monster_curse_contract'] != monster_source:
        raise RuntimeError('WORLD monster callback source bodies differ between state and callback regressions')
    if len(monster_callback_contract['source_files']) != 66 or len(monster_callback_contract['generated_executable_files']) != 46:
        raise RuntimeError('Incomplete WORLD monster curse callback source/executable bindings')
    receipt['source_monster_curse_callback_oracle_sha256'] = sha(B / 'monster-curse-callback-oracle-current.json')
    monster_silence_contract = json.loads((B / 'monster-silence-source-oracle-current.json').read_text(encoding='utf-8'))
    if monster_silence_contract['status'] != 'passed' or monster_silence_contract['candidate_sha256'] != expected:
        raise RuntimeError('NPC silence source reference is not bound to candidate')
    reference = monster_silence_contract['source_monster_silence_contract']
    if not all(reference[key] for key in [
            'source_actual_NPC_silence_initial_cure_and_message_bodies_executed',
            'source_actual_CAS_callback_bases_used', 'source_NPC_target_is_not_PC_fixture_type',
            'source_PC_message_branch_positive_control', 'source_NPC_no_310_311_or_PC_paralysis_packets',
            'source_silence_indefinite_no_scheduler', 'source_initial_and_terminal_publication_barriers',
            'source_closing_blocks_all_poison_subtypes', 'source_independent_curse_slot_and_both_install_orders',
            'source_initial_cleanup_failure_finally_and_stale_lease_guards']):
        raise RuntimeError('Incomplete actual NPC silence source reference')
    if (reference['source_callback_body_count'], reference['source_execution_scenarios'],
            reference['generated_fixture_classes'], reference['checks']) != (7, 12, 12, 86):
        raise RuntimeError('NPC silence source execution inventory changed')
    if reference['source_boot_or_SQL_or_real_scheduler_executed'] or monster_silence_contract['native_NPC_silence_callback_port_verified']:
        raise RuntimeError('Source reference must not claim a native NPC silence callback port')
    if len(monster_silence_contract['source_files']) != 20 or len(monster_silence_contract['generated_executable_files']) != 21:
        raise RuntimeError('Incomplete NPC silence source/executable bindings')
    if monster_silence_contract['source_lease_contract'] != monster_callback_contract['source_lease_contract']:
        raise RuntimeError('NPC silence reference differs from current source CAS bases')
    for item in [*monster_silence_contract['verification_inputs'], *monster_silence_contract['executable_contracts']]:
        if sha(Path(item['path'])) != item['sha256']:
            raise RuntimeError('NPC silence reference input/contract changed')
    receipt['source_monster_silence_reference_oracle_sha256'] = sha(B / 'monster-silence-source-oracle-current.json')
    monster_silence_callbacks = json.loads((B / 'monster-silence-callback-oracle-current.json').read_text(encoding='utf-8'))
    if monster_silence_callbacks['status'] != 'passed' or monster_silence_callbacks['candidate_sha256'] != expected:
        raise RuntimeError('Native WORLD NPC silence callback oracle is not bound to candidate')
    if monster_silence_callbacks['source_monster_silence_contract'] != reference or monster_silence_callbacks['source_monster_curse_contract'] != monster_source:
        raise RuntimeError('Native WORLD NPC silence callback source references differ')
    if monster_silence_callbacks['native_regression_checks'] != 1271:
        raise RuntimeError('NPC silence callback regression coverage changed')
    if len(monster_silence_callbacks['source_files']) != 66 or len(monster_silence_callbacks['generated_executable_files']) != 67:
        raise RuntimeError('Incomplete native NPC silence callback source/executable bindings')
    ref_binding = monster_silence_callbacks['source_monster_silence_reference_receipt']
    if sha(Path(ref_binding['path'])) != ref_binding['sha256']:
        raise RuntimeError('Native NPC silence callback source reference changed')
    receipt['source_monster_silence_callback_oracle_sha256'] = sha(B / 'monster-silence-callback-oracle-current.json')
    monster_damage_reference = json.loads((B / 'monster-damage-source-oracle-current.json').read_text('utf-8'))
    monster_damage_callbacks = json.loads((B / 'monster-damage-callback-oracle-current.json').read_text('utf-8'))
    damage_reference = monster_damage_reference['source_monster_damage_contract']
    if monster_damage_reference['status'] != 'passed' or monster_damage_reference['candidate_sha256'] != expected or monster_damage_callbacks['status'] != 'passed' or monster_damage_callbacks['candidate_sha256'] != expected:
        raise RuntimeError('WORLD damage source/native callback references must be current candidate-bound passes')
    if not all(damage_reference[key] for key in ['source_actual_NPC_damage_initial_scheduler_tick_cure_bodies_executed','source_actual_CAS_callback_bases_used','source_NPC_target_distinct_from_PC','source_PC_positive_control','source_status_30s_before_green_and_fixed_rate_3s_after_green','source_one_damage_per_callback_and_expired_status_no_damage','source_final_attacker_reference_used_by_NPC_callback','source_initial_HP_terminal_closing_and_failure_finally_verified']):
        raise RuntimeError('Incomplete actual NPC damage source callback reference')
    if (damage_reference['source_callback_body_count'],damage_reference['source_execution_scenarios'],damage_reference['checks']) != (12,10,82) or damage_reference['source_boot_SQL_or_real_scheduler_executed']:
        raise RuntimeError('NPC damage source body/execution inventory changed')
    if monster_damage_reference['native_NPC_damage_callback_port_verified'] or not monster_damage_callbacks['native_WORLD_damage_callback_boundary_port_verified'] or monster_damage_callbacks['full_behavior_parity_verified']:
        raise RuntimeError('Damage callback scope must not claim full NPC damage-kernel parity')
    if monster_damage_reference['source_lease_contract'] != monster_callback_contract['source_lease_contract'] or monster_damage_callbacks['source_monster_damage_contract'] != damage_reference:
        raise RuntimeError('Damage callbacks differ from current source CAS/callback bodies')
    if len(monster_damage_reference['source_files']) != 24 or len(monster_damage_callbacks['source_files']) != 28 or len(monster_damage_reference['generated_executable_files']) != 23 or len(monster_damage_callbacks['generated_executable_files']) != 23 or monster_damage_callbacks['native_regression_checks'] != 1627:
        raise RuntimeError('Incomplete damage source/executable/native check inventory')
    reference_binding = monster_damage_callbacks['source_monster_damage_reference_receipt']
    if sha(Path(reference_binding['path'])) != reference_binding['sha256']:
        raise RuntimeError('Damage callback source reference changed')
    for item in [*monster_damage_reference['verification_inputs'],*monster_damage_reference['executable_contracts'],*monster_damage_callbacks['verification_inputs']]:
        if sha(Path(item['path'])) != item['sha256']:
            raise RuntimeError('Damage oracle input or executable contract changed')
    receipt['source_monster_damage_reference_oracle_sha256'] = sha(B / 'monster-damage-source-oracle-current.json')
    receipt['source_monster_damage_callback_oracle_sha256'] = sha(B / 'monster-damage-callback-oracle-current.json')
    receive_reference = json.loads((B / 'monster-receive-source-oracle-current.json').read_text('utf-8'))
    receive_native = json.loads((B / 'monster-receive-oracle-current.json').read_text('utf-8'))
    receive = receive_reference['source_monster_receive_contract']
    for oracle in [receive_reference, receive_native]:
        if oracle['status'] != 'passed' or oracle['candidate_sha256'] != expected or oracle['full_behavior_parity_verified']:
            raise RuntimeError('NPC receive oracle must be a candidate-bound limited pass')
    if receive != receive_native['source_monster_receive_contract'] or (receive['scenarios'],receive['checks']) != (31,95):
        raise RuntimeError('NPC receive source execution inventory changed')
    if not all(receive[key] for key in ['source_actual_int_receive_and_nonlethal_entries','source_actual_damage_event_and_enqueue','source_actual_HP_lambda_receipt_and_death_reservation','source_actual_poison_enqueue_callback_finishes_before_HP_and_cure_keeps_queued_inputs','typed_queue_storage_and_vital_lock']):
        raise RuntimeError('Incomplete source poison-to-queue and HP receipt proof')
    if receive['full_hate_AI_transform_death_handoff_executed'] or receive['source_boot_SQL_or_real_scheduler_executed'] or receive_native['native_NPC_damage_FIFO_or_full_HP_kernel_verified']:
        raise RuntimeError('NPC receive reference must retain its explicit queue/side-effect limits')
    if not receive_native['native_NPC_poison_EarthBind_receive_boundary_verified'] or receive_native['native_regression_checks'] != 456:
        raise RuntimeError('Native NPC poison EarthBind boundary coverage changed')
    if len(receive_reference['source_files']) != 37 or len(receive_native['source_files']) != 41 or len(receive_reference['generated_executable_files']) != 33 or len(receive_native['generated_executable_files']) != 33:
        raise RuntimeError('Incomplete NPC receive source/executable bindings')
    for item in [*receive_reference['verification_inputs'],*receive_reference['executable_contracts'],*receive_native['verification_inputs'],receive_native['source_monster_receive_reference_receipt']]:
        if sha(Path(item['path'])) != item['sha256']:
            raise RuntimeError('NPC receive oracle inputs or contracts changed')
    receipt['source_monster_receive_reference_oracle_sha256'] = sha(B / 'monster-receive-source-oracle-current.json')
    receipt['source_monster_receive_oracle_sha256'] = sha(B / 'monster-receive-oracle-current.json')
    queue_native = json.loads((B / 'npc-damage-queue-oracle-current.json').read_text('utf-8'))
    if queue_native['status'] != 'passed' or queue_native['candidate_sha256'] != expected or queue_native['full_behavior_parity_verified']:
        raise RuntimeError('Native NPC accepted-event queue must be a candidate-bound scoped pass')
    if not queue_native['native_WORLD_poison_accepted_FIFO_and_detached_callback_verified'] or queue_native['native_NPC_damage_FIFO_or_full_HP_kernel_verified']:
        raise RuntimeError('Poison queue cohort must remain distinct from all-ingress FIFO/full damage kernel')
    if queue_native['native_regression_checks'] != 83 or len(queue_native['source_files']) != 43 or len(queue_native['generated_executable_files']) != 33 or queue_native['source_monster_receive_contract'] != receive:
        raise RuntimeError('Incomplete actual NPC accepted-event queue source/native inventory')
    for item in [*queue_native['verification_inputs'],*queue_native['native_fixture_classes'],queue_native['source_monster_receive_reference_receipt']]:
        if sha(Path(item['path'])) != item['sha256']:
            raise RuntimeError('NPC accepted-event queue evidence changed')
    receipt['source_npc_damage_queue_oracle_sha256'] = sha(B / 'npc-damage-queue-oracle-current.json')
    physical_reference = json.loads((B / 'world-physical-source-oracle-current.json').read_text('utf-8'))
    physical_native = json.loads((B / 'world-physical-commit-oracle-current.json').read_text('utf-8'))
    physical = physical_reference['source_world_physical_contract']
    for oracle in [physical_reference, physical_native]:
        if oracle['status'] != 'passed' or oracle['candidate_sha256'] != expected or oracle['full_behavior_parity_verified']:
            raise RuntimeError('WORLD physical commit must remain a candidate-bound limited pass')
    if (physical['cases'], physical['checks']) != (640, 655) or physical_native['source_world_physical_contract'] != physical:
        raise RuntimeError('Source physical scalar prefix inventory changed')
    if not all(physical[k] for k in ['source_actual_commitNpc_numeric_prefix_executed', 'source_manual_and_auto_action_before_commit_compiled_order_verified', 'source_direct_DamageEvent_enqueue_and_afterAccepted_gate_verified', 'source_status_and_config_are_typed_scalar_edges']):
        raise RuntimeError('Incomplete source physical commit boundary proof')
    if physical['source_full_PhysicalHitCommitPlan_effects_executed'] or physical['source_boot_SQL_or_scheduler_executed'] or physical_native['native_all_NPC_ingress_FIFO_or_full_HP_kernel_verified']:
        raise RuntimeError('Physical commit evidence must retain full effects and all-ingress limits')
    if physical_native['native_regression_checks'] != 523 or not all(physical_native[k] for k in ['native_WORLD_physical_poison_doll_shared_FIFO_verified', 'native_post_action_Striker_and_signed_HP_verified']):
        raise RuntimeError('Incomplete native physical commit coverage')
    if (len(physical_reference['source_files']),len(physical_reference['generated_executable_files']),len(physical_native['source_files']),len(physical_native['generated_executable_files']),len(physical_native['native_fixture_classes'])) != (10,3,45,36,5):
        raise RuntimeError('Physical source/native inventory changed')
    for item in [*physical_reference['verification_inputs'],*physical_reference['executable_contracts'],*physical_native['verification_inputs'],*physical_native['native_fixture_classes'],physical_native['source_monster_receive_reference_receipt'],physical_native['source_world_physical_reference_receipt']]:
        if sha(Path(item['path'])) != item['sha256']:
            raise RuntimeError('Physical commit evidence changed')
    receipt['source_world_physical_reference_oracle_sha256'] = sha(B / 'world-physical-source-oracle-current.json')
    receipt['source_world_physical_commit_oracle_sha256'] = sha(B / 'world-physical-commit-oracle-current.json')
    magic_plan_reference = json.loads((B / 'magic-hit-plan-source-oracle-current.json').read_text('utf-8'))
    magic_plan_observation = json.loads((B / 'magic-hit-plan-gap-oracle-current.json').read_text('utf-8'))
    magic_plan = magic_plan_reference['source_magic_hit_plan_contract']
    for oracle in [magic_plan_reference, magic_plan_observation]:
        if oracle['status'] != 'passed' or oracle['candidate_sha256'] != expected or oracle['full_behavior_parity_verified']:
            raise RuntimeError('Magic accepted-plan reference and gap observation must remain scoped evidence')
    if (magic_plan['cases'],magic_plan['checks']) != (428,851) or magic_plan_observation['source_magic_hit_plan_contract'] != magic_plan:
        raise RuntimeError('Source magic accepted-plan execution inventory changed')
    if not all(magic_plan[k] for k in ['source_actual_magic_plan_full_bodies_executed','source_actual_skill_plan_and_mutation_full_bodies_executed','source_actual_composite_outcome_event_and_direct_NPC_commit_executed','source_actual_positive_gate_expression_executed','source_actual_negative_MR_return_executed','source_terminal_heal_and_impact_but_no_target_mutations','source_captured_erase_lease_forwarding_and_per_effect_failure_isolation','typed_actor_HP_MP_erase_timer_mutation_visual_message_talent_edges','typed_enqueue_stores_events_without_real_executor']):
        raise RuntimeError('Incomplete actual source accepted-effect body proof')
    if magic_plan['full_source_skill_calculation_admission_or_all_target_effect_bodies_executed'] or magic_plan['source_boot_SQL_or_real_scheduler_executed']:
        raise RuntimeError('Magic source reference must retain explicit typed actor/queue edges')
    if (len(magic_plan_reference['source_files']),len(magic_plan_reference['generated_executable_files']),len(magic_plan_observation['source_files']),len(magic_plan_observation['generated_executable_files']),len(magic_plan_observation['native_fixture_classes'])) != (21,26,49,59,1) or magic_plan_observation['native_observation_checks'] != 10:
        raise RuntimeError('Magic source/native observation inventory changed')
    observed=magic_plan_observation['native_observation_contract']
    if observed['native_behavior_parity_verified'] != (observed['known_open_differences_count']==0):
        raise RuntimeError('Known magic differences cannot be reported as a parity pass')
    for item in [*magic_plan_reference['verification_inputs'],*magic_plan_reference['executable_contracts'],*magic_plan_observation['verification_inputs'],*magic_plan_observation['native_fixture_classes'],magic_plan_observation['source_magic_hit_plan_reference_receipt'],magic_plan_observation['source_monster_receive_reference_receipt'],magic_plan_observation['native_observation_receipt']]:
        if sha(Path(item['path'])) != item['sha256']:
            raise RuntimeError('Magic plan or native observation evidence changed')
    receipt['source_magic_hit_plan_oracle_sha256']=sha(B/'magic-hit-plan-source-oracle-current.json')
    receipt['native_magic_hit_plan_gap_observation_sha256']=sha(B/'magic-hit-plan-gap-oracle-current.json')
    receipt['known_open_magic_differences']=observed
    world_magic = json.loads((B/'world-magic-commit-oracle-current.json').read_text('utf-8'))
    if world_magic['status']!='passed' or world_magic['candidate_sha256']!=expected or world_magic['full_behavior_parity_verified']:
        raise RuntimeError('Ordinary WORLD magic must remain a candidate-bound scoped pass')
    if not all(world_magic[k] for k in ['native_ordinary_WORLD_signed_magic_target_lane_verified','native_accepted_life_drain_erase_ABA_and_frozen_Thunder_verified']) or world_magic['native_full_all_spell_family_actor_effects_and_target_FIFO_verified']:
        raise RuntimeError('WORLD single-magic ingress must not claim all spell/actor effects')
    if (world_magic['native_regression_checks'],len(world_magic['source_files']),len(world_magic['generated_executable_files']),len(world_magic['native_fixture_classes']))!=(1581,49,59,5) or world_magic['source_magic_hit_plan_contract']!=magic_plan or world_magic['source_monster_receive_contract']!=receive:
        raise RuntimeError('Incomplete ordinary WORLD magic source/native inventory')
    if observed['known_open_differences_count']!=0 or not observed['native_behavior_parity_verified']:
        raise RuntimeError('Previously observed ordinary WORLD signed HP and FIFO differences are unresolved')
    for item in [*world_magic['verification_inputs'],*world_magic['native_fixture_classes'],world_magic['source_magic_hit_plan_reference_receipt'],world_magic['source_monster_receive_reference_receipt']]:
        if sha(Path(item['path']))!=item['sha256']:
            raise RuntimeError('WORLD magic commit evidence changed')
    receipt['source_world_magic_commit_oracle_sha256']=sha(B/'world-magic-commit-oracle-current.json')
    world_multi = json.loads((B/'world-multi-magic-commit-oracle-current.json').read_text('utf-8'))
    if world_multi['status']!='passed' or world_multi['candidate_sha256']!=expected or world_multi['full_behavior_parity_verified'] or world_multi['native_full_all_spell_family_actor_effects_and_target_FIFO_verified']:
        raise RuntimeError('WORLD multi-magic must remain a candidate-bound limited ingress pass')
    if not all(world_multi[k] for k in ['native_ordinary_WORLD_line_centered_and_target_area_signed_target_lane_verified','native_accepted_erase_ABA_calculation_protection_action_epoch_no_viewer_and_fatal_verified']):
        raise RuntimeError('Incomplete three-family ordinary monster commit/effect proof')
    if (world_multi['native_regression_checks'],len(world_multi['source_files']),len(world_multi['generated_executable_files']),len(world_multi['native_fixture_classes']),world_multi['native_three_family_bridge_count'],world_multi['native_numeric_cast_cases'],world_multi['native_numeric_target_outcomes'])!=(2921,49,59,5,12,504,1008) or world_multi['source_magic_hit_plan_contract']!=magic_plan or world_multi['source_monster_receive_contract']!=receive:
        raise RuntimeError('Incomplete WORLD multi-magic source/native inventory')
    for item in [*world_multi['verification_inputs'],*world_multi['native_fixture_classes'],world_multi['source_magic_hit_plan_reference_receipt'],world_multi['source_monster_receive_reference_receipt']]:
        if sha(Path(item['path']))!=item['sha256']:raise RuntimeError('WORLD multi-magic evidence changed')
    receipt['source_world_multi_magic_commit_oracle_sha256']=sha(B/'world-multi-magic-commit-oracle-current.json')
    freeze_reference=json.loads((B/'freeze-effect-source-oracle-current.json').read_text('utf-8'))
    freeze_gap=json.loads((B/'world-freeze-gap-oracle-current.json').read_text('utf-8'))
    freeze_contract=freeze_reference['source_freeze_effect_contract']
    for oracle in [freeze_reference,freeze_gap]:
        if oracle['status']!='passed' or oracle['candidate_sha256']!=expected or oracle['full_behavior_parity_verified'] or oracle['native_freeze_commit_port_verified']:
            raise RuntimeError('Freeze source reference and native gap observation must not imply runtime parity')
    if (freeze_contract['cases'],freeze_contract['checks'],len(freeze_reference['source_files']),len(freeze_reference['generated_executable_files']))!=(57,292,19,13):
        raise RuntimeError('Actual source freeze effect/duration/packet inventory changed')
    if not all(freeze_contract[k] for k in ['actual_applyFreezeTargetEffect_full_body','actual_NPC_raw_paralysis_getter_and_setter','actual_book_duration_getter','actual_poison_and_paralysis_packet_constructor_and_content_bodies','actual_addMagicList_duration_and_freeze_success_guard_fragment','actor_coordinates_packet_delivery_and_spawn_are_typed_edges']) or freeze_contract['full_addMagicList_or_freeze_probability_admission_executed'] or freeze_contract['source_boot_SQL_or_scheduler_executed']:
        raise RuntimeError('Freeze source reference must preserve explicit spawn/delivery/actor edges and narrow guard scope')
    if (freeze_gap['native_observation_checks'],len(freeze_gap['source_files']),len(freeze_gap['generated_executable_files']),len(freeze_gap['native_fixture_classes']))!=(26,63,72,2) or freeze_gap['source_freeze_effect_contract']!=freeze_contract or freeze_gap['source_magic_hit_plan_contract']!=magic_plan or freeze_gap['source_monster_receive_contract']!=receive:
        raise RuntimeError('Incomplete freeze source/native observation bindings')
    freeze_observed=freeze_gap['native_observation_contract']
    if [r['skill'] for r in freeze_observed['families']]!=[50,80,194] or freeze_observed['native_full_freeze_parity_verified']:
        raise RuntimeError('Freeze observations must retain actual three-family scope and unresolved full parity')
    if freeze_observed['known_differences']!=sum(r['signed_HP']!=40001 for r in freeze_observed['families'])+sum(r['held_HP']!=40000 for r in freeze_observed['families'])+sum(not r['zero_freeze_active'] for r in freeze_observed['families']):
        raise RuntimeError('Freeze gap count is inconsistent with native observations')
    for item in [*freeze_reference['verification_inputs'],*freeze_reference['executable_contracts'],*freeze_gap['verification_inputs'],*freeze_gap['native_fixture_classes'],freeze_gap['source_freeze_effect_reference_receipt'],freeze_gap['source_magic_hit_plan_reference_receipt'],freeze_gap['source_monster_receive_reference_receipt'],freeze_gap['native_observation_receipt']]:
        if sha(Path(item['path']))!=item['sha256']:raise RuntimeError('Freeze reference or observation evidence changed')
    receipt['source_freeze_effect_oracle_sha256']=sha(B/'freeze-effect-source-oracle-current.json')
    receipt['native_world_freeze_gap_oracle_sha256']=sha(B/'world-freeze-gap-oracle-current.json')
    receipt['known_open_freeze_differences']=freeze_observed
    world_freeze=json.loads((B/'world-freeze-commit-oracle-current.json').read_text('utf-8'))
    if world_freeze['status']!='passed' or world_freeze['candidate_sha256']!=expected or world_freeze['full_behavior_parity_verified'] or world_freeze['native_full_freeze_parity_verified'] or world_freeze['native_full_all_spell_family_actor_effects_and_target_FIFO_verified']:
        raise RuntimeError('WORLD ordinary-monster freeze must remain a candidate-bound limited port')
    if not all(world_freeze[k] for k in ['native_freeze_commit_port_verified','native_ordinary_WORLD_freeze_signed_target_lane_verified','native_positive_accepted_and_nonpositive_independent_freeze_verified','native_explicit194_and_original_admission_guards_verified','native_erase_ABA_action_and_publication_epoch_no_viewer_fatal_and_failure_verified','native_raw_zero_duration_AI_flag_and_finite_duration_verified']):
        raise RuntimeError('Incomplete freeze target-commit, exact ingress or effect boundary proof')
    if (world_freeze['native_regression_checks'],len(world_freeze['source_files']),len(world_freeze['generated_executable_files']),len(world_freeze['native_fixture_classes']),world_freeze['native_producer_bridge_count'],world_freeze['native_selector_bridge_count'],world_freeze['native_numeric_cast_cases'],world_freeze['native_numeric_target_outcomes'])!=(951,63,72,7,11,0,126,210) or world_freeze['source_freeze_effect_contract']!=freeze_contract or world_freeze['source_magic_hit_plan_contract']!=magic_plan or world_freeze['source_monster_receive_contract']!=receive:
        raise RuntimeError('Incomplete WORLD freeze source/native inventory')
    if freeze_observed['known_differences']!=0 or any(r['freeze_while_consumers_held'] for r in freeze_observed['families']):
        raise RuntimeError('Previously observed freeze signed HP/FIFO/zero-effect differences are unresolved')
    for item in [*world_freeze['verification_inputs'],*world_freeze['native_fixture_classes'],world_freeze['source_freeze_effect_reference_receipt'],world_freeze['source_magic_hit_plan_reference_receipt'],world_freeze['source_monster_receive_reference_receipt']]:
        if sha(Path(item['path']))!=item['sha256']:raise RuntimeError('WORLD freeze commit evidence changed')
    receipt['source_world_freeze_commit_oracle_sha256']=sha(B/'world-freeze-commit-oracle-current.json')
    foe_reference=json.loads((B/'foe-mode-source-oracle-current.json').read_text('utf-8'))
    foe_gap=json.loads((B/'world-foe-gap-oracle-current.json').read_text('utf-8'))
    foe_contract=foe_reference['source_foe_mode_contract']
    for oracle in [foe_reference,foe_gap]:
        if oracle['status']!='passed' or oracle['candidate_sha256']!=expected or oracle['full_behavior_parity_verified'] or oracle['native_foe_commit_port_verified']:
            raise RuntimeError('Foe source mode and native observations must retain separate unresolved runtime scope')
    if (foe_contract['cases'],foe_contract['checks'],len(foe_reference['source_files']),len(foe_reference['generated_executable_files']))!=(93,967,20,10):
        raise RuntimeError('Actual source Foe mode body/packet inventory changed')
    if not all(foe_contract[k] for k in ['actual_full_FOE_SLAYER_PC_NPC_and_noop_method_bodies','actual_PC_Foe_flag_getter_setter','actual_abstract_SkillMode_constructor_and_method_headers','actual_S_SkillSound_two_arg_constructor_build_and_getContent','source_PC_five_onAction_then_caster7020_target6509_then_one_extra_rng','source_NPC_three_attackTarget_then_target6509_target7020_no_extra_rng','source_nonpositive_FOE_SLAYER_DMG_skips_rng_and_returns_zero','source_positive_extra_inclusive_one_through_configured_int_max','source_typed_failure_boundaries_preserve_prefix_order','action_delivery_random_and_base_packet_sink_are_typed_edges']) or foe_contract['full_source_physical_kernel_skill_admission_target_masks_or_commit_handoff_executed'] or foe_contract['source_boot_SQL_or_real_scheduler_executed']:
        raise RuntimeError('Foe source reference must keep physical/action/delivery/RNG edges explicit')
    if (foe_gap['native_observation_checks'],len(foe_gap['source_files']),len(foe_gap['generated_executable_files']),len(foe_gap['native_fixture_classes']))!=(16,55,43,1) or foe_gap['source_foe_mode_contract']!=foe_contract or foe_gap['source_monster_receive_contract']!=receive:
        raise RuntimeError('Incomplete real Foe source/native ingress observation bindings')
    foe_observed=foe_gap['native_observation_contract']
    if not foe_observed['real_C_USE_SPELL187'] or foe_observed['native_physical_strikes']!=5 or foe_observed['native_physical_damage_sum']<=0 or foe_observed['native_full_Foe_parity_verified']:
        raise RuntimeError('Foe native observation must be a real five-strike cast and retain incomplete full parity')
    if foe_observed['known_differences']!=int(foe_observed['native_held_HP']!=40000)+int(foe_observed['negative_control_throws']) or foe_observed['negative_control_rng_draws']!=int(foe_observed['negative_control_throws']) or foe_observed['negative_control_real_packet_rejected_as_malformed']!=foe_observed['negative_control_throws']:
        raise RuntimeError('Known Foe differences do not match actual HP and negative-control packet observations')
    for item in [*foe_reference['verification_inputs'],*foe_reference['executable_contracts'],*foe_gap['verification_inputs'],*foe_gap['native_fixture_classes'],foe_gap['source_foe_mode_reference_receipt'],foe_gap['source_monster_receive_reference_receipt'],foe_gap['native_observation_receipt']]:
        if sha(Path(item['path']))!=item['sha256']:raise RuntimeError('Foe source/native evidence changed')
    receipt['source_foe_mode_oracle_sha256']=sha(B/'foe-mode-source-oracle-current.json')
    receipt['native_world_foe_gap_oracle_sha256']=sha(B/'world-foe-gap-oracle-current.json')
    receipt['known_open_foe_differences']=foe_observed
    world_foe=json.loads((B/'world-foe-commit-oracle-current.json').read_text('utf-8'))
    if world_foe['status']!='passed' or world_foe['candidate_sha256']!=expected or world_foe['full_behavior_parity_verified'] or world_foe['native_full_Foe_parity_verified'] or world_foe['native_full_all_spell_family_actor_effects_and_target_FIFO_verified']:
        raise RuntimeError('WORLD ordinary-monster Foe extra must remain a candidate-bound limited ingress port')
    if not all(world_foe[k] for k in ['native_ordinary_WORLD_Foe_extra_target_lane_verified','native_nonpositive_source_control_and_independent_erase_verified','native_before_first_strike_epoch_and_erase_capture_verified','native_erase_ABA_owner_rebind_FX_barriers_and_end_mode_bound_read_verified','native_actual_explicit187_and_original_admission_verified','native_no_viewer_fatal_and_publication_failure_no_HP_retry_verified','native_foe_commit_port_verified']):
        raise RuntimeError('Incomplete Foe target handoff, independent effects or native packet proof')
    if (world_foe['native_regression_checks'],len(world_foe['source_files']),len(world_foe['generated_executable_files']),len(world_foe['native_fixture_classes']),world_foe['native_capture_finish_bridges'],world_foe['native_selector_bridges'],world_foe['native_numeric_cast_cases'],world_foe['native_numeric_physical_outcomes'])!=(302,55,43,7,2,0,15,75) or world_foe['source_foe_mode_contract']!=foe_contract or world_foe['source_monster_receive_contract']!=receive:
        raise RuntimeError('Incomplete WORLD Foe source/native inventory')
    if foe_observed['known_differences']!=0 or foe_observed['native_held_HP']!=40000 or foe_observed['native_pending_events']!=7:
        raise RuntimeError('Known Foe inline HP and negative-control differences remain unresolved')
    for item in [*world_foe['verification_inputs'],*world_foe['native_fixture_classes'],world_foe['source_foe_mode_reference_receipt'],world_foe['source_monster_receive_reference_receipt']]:
        if sha(Path(item['path']))!=item['sha256']:raise RuntimeError('WORLD Foe commit evidence changed')
    receipt['source_world_foe_commit_oracle_sha256']=sha(B/'world-foe-commit-oracle-current.json')
    pc_magic=json.loads((B/'pc-magic-commit-source-oracle-current.json').read_text('utf-8'))
    player_foe=json.loads((B/'player-foe-oracle-current.json').read_text('utf-8'))
    pc_contract=pc_magic['source_PC_magic_commit_contract'];player_observed=player_foe['native_observation_contract']
    for oracle in [pc_magic,player_foe]:
        if oracle['status']!='passed' or oracle['candidate_sha256']!=expected or oracle['full_behavior_parity_verified']:raise RuntimeError('PC Foe/receiver evidence is not current and limited')
    if (pc_contract['cases'],pc_contract['checks'],len(pc_magic['source_files']),len(pc_magic['generated_executable_files']))!=(235,2503,35,33):raise RuntimeError('Incomplete actual PC receiver and accepted plan inventory')
    if not all(pc_contract[k] for k in ['actual_commitPc_and_full_receiveDamageInternal_bodies','actual_receiveDamage_forwarding_overloads','actual_magic_incoming_feedback_composite_outcome_event_plan_bodies','signed_input_proc_reduction_equipment_avatar_and_final_int_order','perception_positive_hit_exact_leases_and_mana_drain_wait_for_acceptance','actor_inventory_magic_rate_Proc_feedback_packets_and_queue_are_typed_edges']) or pc_contract['source_real_damage_executor_HP_death_or_full_skill_admission_executed'] or pc_contract['source_boot_SQL_or_real_scheduler_executed']:raise RuntimeError('PC source execution scope mismatch')
    if player_foe['native_physical_strikes']!=630:raise RuntimeError('All original five-strike PC loops must execute')
    if (player_foe['native_regression_checks'],player_observed['cases'],player_observed['known_calculation_boundary_observations'],len(player_foe['source_files']),len(player_foe['generated_executable_files']),len(player_foe['native_fixture_classes']))!=(631,126,0,47,43,1) or player_foe['native_PC_target_FIFO_or_full_Foe_parity_verified'] or not player_foe['native_PC_Foe_calculation_only_cooking_not_reapplied_verified'] or player_foe['source_PC_magic_commit_contract']!=pc_contract or player_foe['source_foe_mode_contract']!=foe_contract:raise RuntimeError('PC Foe extra must retain raw mode value and bounded native/source scope')
    for oracle in [pc_magic,player_foe]:
        for item in [*oracle['verification_inputs'],*oracle['source_files'],*oracle['generated_executable_files'],*oracle.get('executable_contracts',[]),*oracle.get('native_fixture_classes',[])]:
            if sha(Path(item['path']))!=item['sha256']:raise RuntimeError('PC source/native evidence changed')
    for item in [player_foe['native_observation_receipt'],player_foe['source_foe_mode_reference_receipt'],player_foe['source_PC_magic_commit_reference_receipt']]:
        if sha(Path(item['path']))!=item['sha256']:raise RuntimeError('PC Foe reference/observation changed')
    receipt['source_PC_magic_commit_oracle_sha256']=sha(B/'pc-magic-commit-source-oracle-current.json')
    receipt['native_player_foe_oracle_sha256']=sha(B/'player-foe-oracle-current.json')
    receipt['native_player_foe_calculation_observations']=player_observed
    pc_hp=json.loads((B/'pc-damage-commit-source-oracle-current.json').read_text('utf-8'))
    pc_ingress=json.loads((B/'player-commit-ingress-oracle-current.json').read_text('utf-8'))
    hp_contract=pc_hp['source_PC_damage_commit_contract'];ingress_observed=pc_ingress['native_observation_contract']
    for oracle in [pc_hp,pc_ingress]:
        if oracle['status']!='passed' or oracle['candidate_sha256']!=expected or oracle['full_behavior_parity_verified']:raise RuntimeError('PC HP reference/ingress observation must remain current and limited')
    if (hp_contract['cases'],hp_contract['checks'],len(pc_hp['source_files']),len(pc_hp['generated_executable_files']))!=(535,8260,47,45) or hp_contract['concurrent_callback_assertions']<10:raise RuntimeError('Incomplete actual PC HP/lock/death gate reference')
    if not all(hp_contract[k] for k in ['actual_PC_magic_receiver_and_processDamageCommit_lambda_bodies','actual_beginDeathStateLocked_PlayerActionLock_and_DeathLifecycleGate_bodies','actual_DamageCommitApplier_and_connected_accepted_plan_path','source_long_signed_caps_floor_GM_dead_and_invalid_zero_acceptance','source_HP_party_Proc_mining_plan_failures_preserve_HP_receipt','source_simultaneous_terminal_single_owner_and_cross_character_callbacks_without_locks','actor_vitals_Proc_terminal_finish_packets_party_and_queue_storage_are_typed_edges']) or any(hp_contract[k] for k in ['source_real_queue_executor_full_Proc_death_finalizer_or_skill_admission_executed','source_item_skill_XP_penalties_executed','source_boot_SQL_or_real_scheduler_executed']):raise RuntimeError('PC HP reference cannot claim real queue/Proc/finalizer/penalty execution')
    if (ingress_observed['checks'],ingress_observed['cases'],ingress_observed['native_producer_thread_HP_publications'],len(pc_ingress['source_files']),len(pc_ingress['generated_executable_files']),len(pc_ingress['native_fixture_classes']))!=(42,8,8,51,45,3) or pc_ingress['native_PC_FIFO_or_source_threading_parity_verified'] or not pc_ingress['source_queue_worker_not_executed_by_this_reference'] or pc_ingress['source_PC_damage_commit_contract']!=hp_contract:raise RuntimeError('Native PC producer-thread observations remain an unresolved FIFO boundary')
    for oracle in [pc_hp,pc_ingress]:
        for item in [*oracle['verification_inputs'],*oracle['source_files'],*oracle['generated_executable_files'],*oracle.get('executable_contracts',[]),*oracle.get('native_fixture_classes',[])]:
            if sha(Path(item['path']))!=item['sha256']:raise RuntimeError('PC HP/ingress evidence changed')
    for item in [pc_ingress['source_PC_damage_commit_reference_receipt'],pc_ingress['native_observation_receipt'],pc_ingress['source_queue_policy']]:
        if sha(Path(item['path']))!=item['sha256']:raise RuntimeError('PC HP/ingress reference changed')
    receipt['source_PC_damage_commit_oracle_sha256']=sha(B/'pc-damage-commit-source-oracle-current.json')
    receipt['native_PC_commit_ingress_oracle_sha256']=sha(B/'player-commit-ingress-oracle-current.json')
    receipt['native_PC_commit_ingress_observations']=ingress_observed
    pc_queue=json.loads((B/'pc-damage-queue-source-oracle-current.json').read_text('utf-8'))
    pc_queue_contract=pc_queue['source_PC_damage_queue_contract']
    if pc_queue['status']!='passed' or pc_queue['candidate_sha256']!=expected or pc_queue['full_behavior_parity_verified'] or pc_queue['native_PC_FIFO_port_verified'] or pc_queue['runtime_changed']:raise RuntimeError('Source PC queue evidence must remain current and distinct from native FIFO port')
    if (pc_queue_contract['cases'],pc_queue_contract['checks'],len(pc_queue['source_files']),len(pc_queue['generated_executable_files']))!=(18,473,60,53) or pc_queue_contract['source_callback_assertions']<100:raise RuntimeError('Incomplete actual source PC queue execution proof')
    if not all(pc_queue_contract[k] for k in ['actual_source_queue_executor_retry_clock_and_actor_lazy_owner','actual_PC_HP_applier_and_magic_plan_connected_to_real_queue','no_inline_HP_or_accepted_effects','target_FIFO_nested_tail_independent_targets_and_deadline','target_global_capacity_closed_queue_and_dispatch_retry','four_producers_64_unique_events_preserve_per_producer_order','dead_target_and_two_lethal_events_separate_dispatch_from_HP_acceptance','handler_packet_plan_failures_never_replay_HP','unmodified_12_retry_exhaustion_and_timer_rejection_account_drops','graceful_drain_external_fence_and_causal_nested_admission','terminal_drop_does_not_wait_behind_running_handler','actor_vitals_Proc_packets_party_terminal_finish_remain_typed_edges']) or pc_queue_contract['source_boot_SQL_full_Proc_and_terminal_penalties_executed'] or pc_queue_contract['native_PC_FIFO_port_verified']:raise RuntimeError('Source PC queue scope mismatch')
    if (pc_queue['source_PC_HP_prerequisite_contract']['cases'],pc_queue['source_PC_HP_prerequisite_contract']['checks'])!=(535,8260) or pc_queue['actual_retry_delays_ms']!=[5,10,20,40,80,160,320,640,1000,1000,1000,1000]:raise RuntimeError('Source PC queue HP prerequisite or retry policy mismatch')
    for item in [*pc_queue['verification_inputs'],*pc_queue['source_files'],*pc_queue['generated_executable_files'],*pc_queue['executable_contracts'],pc_queue['source_PC_HP_reference_receipt']]:
        if sha(Path(item['path']))!=item['sha256']:raise RuntimeError('Source PC queue evidence changed')
    receipt['source_PC_damage_queue_oracle_sha256']=sha(B/'pc-damage-queue-source-oracle-current.json')
    receipt['source_PC_damage_queue_contract']=pc_queue_contract
    native_target=json.loads((B/'native-target-contract-oracle-current.json').read_text('utf-8'))
    target_contract=native_target['native_client_protocol_contract']
    target_observed=native_target['native_observation_contract']
    if native_target['status']!='passed' or native_target['candidate_sha256']!=expected or not native_target['native_formal_explicit_request_contract_verified'] or native_target['full_behavior_parity_verified']:
        raise RuntimeError('Native target requests must preserve the pinned release client protocol')
    if (native_target['native_regression_checks'],target_observed['cases'],target_observed['formal_targeted_skill_count'],target_observed['unexpected_selector_acceptances'],len(native_target['native_fixture_classes']),len(native_target['native_client_protocol_inputs']))!=(729,84,21,0,1,6):
        raise RuntimeError('Incomplete 21-skill BATTLE/WORLD explicit and missing target matrix')
    if target_contract['status']!='verified-offline-pinned-release-protocol' or not target_contract['release_DLL_pin_matches_formal_startup'] or not target_contract['original_callback_and_producer_instructions_verified'] or (target_contract['actual_DLL_lookup_evaluated_cases'],target_contract['actual_native_packet_bytes'],target_contract['actual_native_packet_schema'])!=(259,11,'cccdhh') or target_contract['live_UI_or_full_formal_startup_executed'] or target_observed['full_skill_effect_or_eligibility_parity_verified']:
        raise RuntimeError('Native client binary proof scope or release pin mismatch')
    for item in [*native_target['verification_inputs'],*native_target['native_fixture_classes'],*native_target['native_client_protocol_inputs'],native_target['binary_contract_receipt'],native_target['native_observation_receipt']]:
        if sha(Path(item['path']))!=item['sha256']:raise RuntimeError('Native target request evidence changed')
    receipt['native_target_contract_oracle_sha256']=sha(B/'native-target-contract-oracle-current.json')
    receipt['native_client_protocol_inputs']=native_target['native_client_protocol_inputs']
    receipt['native_target_request_observation']=target_observed
    monster_poison_paralysis_reference = json.loads((B / 'monster-poison-paralysis-source-oracle-current.json').read_text('utf-8'))
    monster_poison_paralysis_callbacks = json.loads((B / 'monster-poison-paralysis-callback-oracle-current.json').read_text('utf-8'))
    npc_para = monster_poison_paralysis_reference['source_monster_poison_paralysis_contract']
    for oracle in [monster_poison_paralysis_reference,monster_poison_paralysis_callbacks]:
        if oracle['status'] != 'passed' or oracle['candidate_sha256'] != expected:
            raise RuntimeError('NPC poison paralysis source/native callback oracle is not candidate-bound')
        if len(oracle['source_files']) != 34 or len(oracle['generated_executable_files']) != 27:
            raise RuntimeError('Incomplete NPC poison paralysis source/executable bindings')
    if not all(npc_para[key] for key in ['source_actual_NPC_paralysis_poison_initial_scheduler_stage_cure_bodies_executed','source_actual_CAS_callback_bases_used','source_NPC_distinct_PC_alive_dead_positive_controls','source_NPC_no_212_or_PC_lock_release_packets','source_gray_publication_before_active_status_and_terminal_timer','source_full20s_after_initial_and45s_after_gray_with_late_callbacks','source_null_rejected_both_timers_and_callback_failure_finally','source_initial_gray_terminal_closing_barriers','source_NPC_actual_permanent_status_map_and_raw_paralysis_flag_methods_executed','source_NPC_poison_active_status_does_not_lock_AI_flag','source_NPC_compiled_AI_movement_inherited_raw_flag_calls_verified']):
        raise RuntimeError('Incomplete actual NPC poison paralysis source callback/action evidence')
    if (npc_para['source_callback_body_count'],npc_para['source_character_permanent_status_and_flag_methods'],npc_para['source_execution_scenarios'],npc_para['checks']) != (14,5,37,446) or npc_para['source_full_NPC_AI_executed'] or npc_para['source_boot_SQL_or_real_scheduler_executed']:
        raise RuntimeError('NPC poison paralysis source scope/count changed')
    if monster_poison_paralysis_reference['native_NPC_paralysis_poison_callback_port_verified'] or not monster_poison_paralysis_callbacks['native_WORLD_poison_paralysis_callback_boundary_port_verified'] or monster_poison_paralysis_callbacks['full_behavior_parity_verified']:
        raise RuntimeError('NPC poison paralysis reference/native scope must remain distinct')
    if monster_poison_paralysis_callbacks['native_regression_checks'] != 1735 or monster_poison_paralysis_callbacks['source_monster_poison_paralysis_contract'] != npc_para or monster_poison_paralysis_reference['source_lease_contract'] != monster_callback_contract['source_lease_contract']:
        raise RuntimeError('NPC poison paralysis callback checks or shared CAS reference differ')
    for item in [*monster_poison_paralysis_reference['verification_inputs'],*monster_poison_paralysis_reference['executable_contracts'],*monster_poison_paralysis_callbacks['verification_inputs'],monster_poison_paralysis_callbacks['source_monster_poison_paralysis_reference_receipt']]:
        if sha(Path(item['path'])) != item['sha256']:
            raise RuntimeError('NPC poison paralysis oracle source or executable contract changed')
    receipt['source_monster_poison_paralysis_reference_oracle_sha256'] = sha(B / 'monster-poison-paralysis-source-oracle-current.json')
    receipt['source_monster_poison_paralysis_callback_oracle_sha256'] = sha(B / 'monster-poison-paralysis-callback-oracle-current.json')
    incoming_contract = json.loads((B / 'incoming-source-oracle-current.json').read_text(encoding='utf-8'))
    if incoming_contract['status'] != 'passed' or incoming_contract['candidate_sha256'] != expected:
        raise RuntimeError('Current source incoming tail oracle is not bound to candidate')
    receipt['source_incoming_oracle_sha256'] = sha(B / 'incoming-source-oracle-current.json')
    equipment_input = incoming_contract['shared_static_equipment_snapshot']
    if sha(Path(equipment_input['path'])) != equipment_input['sha256'] or not incoming_contract['shared_static_equipment_SELECT_only']:
        raise RuntimeError('Shared incoming equipment static template snapshot changed')
    receipt['shared_incoming_equipment_input'] = equipment_input
    receipt['source_code_inputs'] = list({item['path']: item for item in
        [*source_contract['source_files'], *npc_contract['source_files'], *poison_contract['source_files'], *atomic_contract['source_files'], *paralysis_contract['source_files'], *callback_contract['source_files'], *silence_contract['source_files'], *death_contract['source_files'], *paralysis_callback_contract['source_files'], *curse_contract['source_files'], *monster_contract['source_files'], *monster_callback_contract['source_files'], *monster_silence_contract['source_files'], *monster_silence_callbacks['source_files'], *monster_damage_reference['source_files'], *monster_damage_callbacks['source_files'], *receive_reference['source_files'], *receive_native['source_files'], *queue_native['source_files'], *physical_reference['source_files'], *physical_native['source_files'], *magic_plan_reference['source_files'], *magic_plan_observation['source_files'], *world_magic['source_files'], *world_multi['source_files'], *freeze_reference['source_files'], *freeze_gap['source_files'], *world_freeze['source_files'], *foe_reference['source_files'], *foe_gap['source_files'], *world_foe['source_files'], *pc_magic['source_files'], *player_foe['source_files'], *pc_hp['source_files'], *pc_queue['source_files'], *pc_ingress['source_files'], *monster_poison_paralysis_reference['source_files'], *monster_poison_paralysis_callbacks['source_files'], *incoming_contract['source_files']]}.values())
    for item in [*npc_contract['generated_executable_files'], *poison_contract['generated_executable_files'], *atomic_contract['generated_executable_files'], *paralysis_contract['generated_executable_files'], *callback_contract['generated_executable_files'], *silence_contract['generated_executable_files'], *death_contract['generated_executable_files'], *paralysis_callback_contract['generated_executable_files'], *curse_contract['generated_executable_files'], *monster_contract['generated_executable_files'], *monster_callback_contract['generated_executable_files'], *monster_silence_contract['generated_executable_files'], *monster_silence_callbacks['generated_executable_files'], *monster_damage_reference['generated_executable_files'], *monster_damage_callbacks['generated_executable_files'], *receive_reference['generated_executable_files'], *receive_native['generated_executable_files'], *queue_native['generated_executable_files'], *physical_reference['generated_executable_files'], *physical_native['generated_executable_files'], *magic_plan_reference['generated_executable_files'], *magic_plan_observation['generated_executable_files'], *world_magic['generated_executable_files'], *world_multi['generated_executable_files'], *freeze_reference['generated_executable_files'], *freeze_gap['generated_executable_files'], *world_freeze['generated_executable_files'], *foe_reference['generated_executable_files'], *foe_gap['generated_executable_files'], *world_foe['generated_executable_files'], *pc_magic['generated_executable_files'], *player_foe['generated_executable_files'], *pc_hp['generated_executable_files'], *pc_queue['generated_executable_files'], *pc_ingress['generated_executable_files'], *monster_poison_paralysis_reference['generated_executable_files'], *monster_poison_paralysis_callbacks['generated_executable_files'], *incoming_contract['generated_executable_files']]:
        if sha(Path(item['path'])) != item['sha256']:
            raise RuntimeError('Compiled source executable oracle changed during acceptance')
    for item in receipt['source_code_inputs']:
        if sha(Path(item['path'])) != item['sha256']:
            raise RuntimeError('Source healing Java/class changed during acceptance: ' + item['path'])
    receipt['status'] = 'passed'
except BaseException as failure:
    receipt['status'] = 'failed'
    receipt['failure'] = str(failure)
    raise
finally:
    receipt['finished_utc'] = datetime.now(timezone.utc).isoformat()
    save()
print('PASS candidate acceptance ' + expected, flush=True)
