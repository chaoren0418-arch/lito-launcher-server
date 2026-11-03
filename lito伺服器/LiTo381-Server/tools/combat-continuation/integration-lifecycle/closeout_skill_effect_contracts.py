"""Offline Source mode/identity contracts. This does not execute Source or the candidate."""
import csv
import re
import shutil
from pathlib import Path
from build_development_closeout import pin


def uncomment(text):
    # Preserve strings; comment text must never create a mode registration or method marker.
    token = r'"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|/\*.*?\*/|//[^\n]*'
    return re.sub(token, lambda m: ' ' if m[0].startswith(('//', '/*')) else m[0], text, flags=re.S)


def write_rows(path, rows):
    with path.open('w', encoding='utf-8', newline='') as stream:
        writer = csv.DictWriter(stream, fieldnames=list(rows[0]), delimiter='\t')
        writer.writeheader()
        writer.writerows(rows)
    return pin(path)


def capture(run, skills):
    root = Path('D:/lineage開發/1-伺服器/src/com/lineage/server')
    folder = run / 'inputs/skill-effect-contracts'
    folder.mkdir(parents=True)
    shutil.copyfile(Path(__file__), folder / Path(__file__).name)
    references = []

    def source(relative, markers=()):
        original = root / relative
        text = original.read_text('utf-8-sig')
        code = uncomment(text)
        locators = []
        for marker in markers:
            if marker not in code:
                raise ValueError('Review changed Source contract: ' + relative + ': ' + marker)
            lines = [i for i, line in enumerate(text.splitlines(), 1) if marker in line]
            locators.append({'text': marker, 'lines': lines})
        target = folder / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(original, target)
        references.append({'source': pin(original), 'frozen': pin(target), 'locators': locators})
        return code

    constants = source('model/skill/L1SkillId.java')
    values = {name: int(value) for name, value in re.findall(
        r'public\s+static\s+final\s+int\s+(\w+)\s*=\s*(\d+)\s*;', constants)}
    mode_code = source('model/skill/L1SkillMode.java', ['return _skillMode.get(skillid);'])
    entries = re.findall(r'_skillMode\.put\((\w+),\s*new\s+(\w+)\(\)\)', mode_code)
    if len(entries) != mode_code.count('_skillMode.put('):
        raise ValueError('Review changed Source mode registration syntax')
    modes = {}
    registry = []
    for symbol, implementation in entries:
        skill = values[symbol]
        if skill in modes:
            raise ValueError('Review duplicate Source mode registration')
        modes[skill] = implementation
        registry.append({'skill_id': skill, 'symbol': symbol, 'mode_class': implementation})
    source('model/skill/L1SkillUse.java', [
        'final SkillMode mode = getSkillMode();', 'mode = L1SkillMode.get().getSkill(_skill.getEffectSkillId());',
        'cha.setSkillEffect(getEffectSkillId(), _getBuffDuration);',
        'this._dmg = mode.start(this._player, cha, magic, this._getBuffIconDuration);',
        'this._dmg = mode.start(this._npc, cha, magic, this._getBuffIconDuration);',
        'cha.setSkillEffect(getEffectSkillId(), duration);'])
    proc_code = source('model/proc/ProcSkillEffectHelper.java', [
        'if (isHealSkill(skillId)) {', 'if (timeSec > 0) {',
        'applyWithCustomDuration(actor, skillTarget, skillId, timeSec, selfSkillTarget);',
        'applyWithOriginalSkill(actor, skillTarget, skillId, useProcsType);',
        'pc.setSkillEffect(skillId, timeSec * 1000);', 'cha.setSkillEffect(skillId, timeSec * 1000);',
        'L1SkillUse.TYPE_GMBUFF);'])
    if 'getEffectSkillId(' in proc_code or 'getSkillMode(' in proc_code:
        raise ValueError('Review changed custom proc identity policy')
    source('model/proc/EquipProcAction.java', ['ProcSkillEffectHelper.apply(actor, skillTarget, _proc.getTriggerSkill(),'])
    source('model/skill/skillmode/CONFUSION.java', [
        'if (!cha.hasSkillEffect(L1SkillId.SILENCE)) {',
        'cha.setSkillEffect(L1SkillId.SILENCE, integer * 1000);',
        'pc.sendPackets(new S_ServerMessage(1339));'])
    source('model/skill/skillmode/DECAY_POTION.java', [
        'if (cha.hasSkillEffect(L1SkillId.DECAY_POTION)) {', 'cha.set_decay_potion(true);',
        'cha.setSkillEffect(L1SkillId.DECAY_POTION, integer * 1000);'])
    source('model/skill/skillmode/WIND_SHACKLE.java', [
        'if (cha.hasSkillEffect(WIND_SHACKLE)) {', 'new S_PacketBoxWindShackle(pc.getId(), integer)',
        'cha.setSkillEffect(WIND_SHACKLE, integer * 1000);'])
    source('model/skill/skillmode/CURSE_PARALYZE.java', [
        'final int delay = ConfigSkill.CURSE_READY_TIME * 1000;',
        'final int time = ConfigSkill.CURSE_TIME * 1000;', 'L1CurseParalysis.curse(cha, delay, time, 1);'])
    source('model/L1CurseParalysis.java', [
        'this._target.setSkillEffect(STATUS_CURSE_PARALYZING, 0);',
        '_target.setSkillEffect(STATUS_CURSE_PARALYZED, 0);', '_target.setParalyzed(true);'])

    def resolve(row):
        skill, effect = int(row['skill_id']), int(row['effect_skill_id'])
        key = skill if skill in modes else effect if effect > 0 and effect != skill and effect in modes else None
        return {'own_mode': modes.get(skill, ''), 'resolved_mode_key': key or '',
                'resolved_mode_class': modes.get(key, ''),
                'mode_lookup': 'own-mode' if key == skill else 'one-effect-mode' if key is not None else 'no-mode-raw-special-and-generic-status-review',
                # This is the fallback accessor, not a claim about what a mode actually writes.
                'generic_status_accessor_id': effect if effect > 0 else skill,
                'proc_nonhealing_positive_seconds_status_id': skill,
                'proc_nonhealing_positive_seconds_mode_lookup': 'none-direct-raw-status-after-healing-special-case',
                'proc_zero_seconds_route': 'original-raw-skill-TYPE_GMBUFF-with-proc-range-LOS-policy',
                'cost_duration_graphics_target_row': skill,
                'runtime_effect_execution': 'per-consumer-review-open'}

    routing = [{k: row[k] for k in ['skill_id', 'name', 'effect_skill_id', 'type', 'target', 'buffDuration']} | resolve(row)
               for row in skills.values()]
    effects = {int(row['skill_id']): resolve(row) for row in skills.values() if int(row['effect_skill_id']) > 0}
    behavior = {
        'CONFUSION': (str(values['SILENCE']), 'raw-skill-duration-seconds-times1000;active-silence-not-refreshed',
                      'PC-caster-writes-SILENCE-and-message1339;NPC-caster-overload-no-effect'),
        'DECAY_POTION': (str(values['DECAY_POTION']), 'raw-skill-duration-seconds-times1000;active-decay-not-refreshed',
                         'set-decay-flag-and-timer;stop-clears-flag'),
        'WIND_SHACKLE': (str(values['WIND_SHACKLE']), 'raw-skill-duration-seconds-times1000;active-shackle-not-refreshed',
                         'PC-dedicated-wind-shackle-icon-and-timer'),
        'CURSE_PARALYZE': (str(values['STATUS_CURSE_PARALYZING']) + '->' + str(values['STATUS_CURSE_PARALYZED']),
                          'CURSE_READY_TIME-and-CURSE_TIME-controls;raw-buffDuration-not-the-curse-timers',
                          'freeze-exclusions;current-curse-owned-delayed-paralysis-and-cleanup'),
    }
    for skill, row in effects.items():
        mode = row['resolved_mode_class']
        if mode:
            if mode not in behavior:
                raise ValueError('Review new effect mode body: ' + mode)
            status, duration, note = behavior[mode]
        else:
            status = str(row['generic_status_accessor_id'])
            duration = 'raw-skill-duration;TYPE_OTHER-prewrite-and-addMagicList-millisecond-path-need-exact-order'
            note = 'no-registered-mode;raw-special-branches-and-generic-status-consumers-required'
        row.update({'mode_written_status_ids': status, 'mode_duration_contract': duration, 'mode_body_contract': note})

    summary = {
        'status': 'Source-identity-contracts-only-not-runtime-qualification',
        'sources': references,
        'tool': pin(folder / Path(__file__).name),
        'mode_registry': write_rows(run / 'source-skill-mode-registry.tsv', registry),
        'mode_registrations': len(registry), 'mode_implementations': len(set(modes.values())),
        'all_skill_resolution_matrix': write_rows(run / 'skill-mode-resolution.tsv', routing),
        'all_skill_rows': len(routing),
        'effect_rows_with_mode': sum(bool(row['resolved_mode_class']) for row in effects.values()),
        'effect_rows_without_mode': sum(not row['resolved_mode_class'] for row in effects.values()),
        'proc_custom_duration_uses_raw_status_id': True,
        'effect_reference_is_not_necessarily_written_status_id': True,
        'tests_executed': False, 'database_connected': False, 'runtime_changed': False,
    }
    return effects, summary
