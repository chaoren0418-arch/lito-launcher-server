// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import local.lito381.server.NativeDeCharacterFields;
import local.lito381.server.NativeDeNpcMobSkill;
import local.lito381.server.NativeDeNpcTemplate;
import local.lito381.server.NativeDeNpcTemplateLog;
import local.lito381.server.NativeDeNpcTemplatePorts;
import local.lito381.server.NativeDeNpcTemplateState;
import local.lito381.server.NativeDeWorldLog;

public class NativeDeNpcMobSkillUse {
    private static final NativeDeWorldLog _log = NativeDeNpcTemplateLog.getLogger(NativeDeNpcMobSkillUse.class);
    private NativeDeNpcMobSkill _mobSkillTemplate = null;
    private NativeDeNpcTemplateState _attacker = null;
    private NativeDeCharacterFields _target = null;
    private int _sleepTime = 0;
    private int[] _skillUseCount;
    private final Object _skillDelayLock = new Object();
    private boolean[] _skillDelay;

    public NativeDeNpcMobSkillUse(NativeDeNpcTemplateState nativeDeNpcTemplateState) {
        try {
            this._sleepTime = 0;
            this._mobSkillTemplate = NativeDeNpcTemplatePorts.MobSkills.getInstance().getTemplate(((NativeDeNpcTemplate)nativeDeNpcTemplateState.getNpcTemplate()).get_npcId());
            if (this._mobSkillTemplate == null) {
                return;
            }
            this._attacker = nativeDeNpcTemplateState;
            this._skillUseCount = new int[this.getMobSkillTemplate().getSkillSize()];
            this._skillDelay = new boolean[this.getMobSkillTemplate().getSkillSize()];
        }
        catch (Exception exception) {
            _log.error(exception.getLocalizedMessage(), exception);
        }
    }

    private int getSkillUseCount(int n) {
        return this._skillUseCount[n];
    }

    private void skillUseCountUp(int n) {
        int n2 = n;
        this._skillUseCount[n2] = this._skillUseCount[n2] + 1;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean isSkillDelay(int n) {
        synchronized (this._skillDelayLock) {
            if (this._skillDelay == null || n < 0 || n >= this._skillDelay.length) {
                return false;
            }
            return this._skillDelay[n];
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean trySetSkillDelay(int n) {
        synchronized (this._skillDelayLock) {
            if (this._skillDelay == null || n < 0 || n >= this._skillDelay.length) {
                return false;
            }
            if (this._skillDelay[n]) {
                return false;
            }
            this._skillDelay[n] = true;
            return true;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void clearSkillDelay(int n) {
        synchronized (this._skillDelayLock) {
            if (this._skillDelay == null || n < 0 || n >= this._skillDelay.length) {
                return;
            }
            this._skillDelay[n] = false;
        }
    }

    public int getDelayNpcId() {
        if (this._attacker == null || this._attacker.getNpcTemplate() == null) {
            return 0;
        }
        return ((NativeDeNpcTemplate)this._attacker.getNpcTemplate()).get_npcId();
    }

    public void resetAllSkillUseCount() {
        if (this.getMobSkillTemplate() == null) {
            return;
        }
        for (int i = 0; i < this.getMobSkillTemplate().getSkillSize(); ++i) {
            this._skillUseCount[i] = 0;
        }
    }

    public int getSleepTime() {
        return this._sleepTime;
    }

    public void setSleepTime(int n) {
        this._sleepTime = n;
    }

    public NativeDeNpcMobSkill getMobSkillTemplate() {
        return this._mobSkillTemplate;
    }
}
