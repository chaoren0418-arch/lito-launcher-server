// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import local.lito381.server.NativeDeCharacterFields;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeNpcTemplateState;

public abstract class NativeDeNpcExecutor {
    public abstract int type();

    public void talk(NativeDeDeletionPorts.Viewer viewer, NativeDeNpcTemplateState nativeDeNpcTemplateState) {
    }

    public void action(NativeDeDeletionPorts.Viewer viewer, NativeDeNpcTemplateState nativeDeNpcTemplateState, String string, long l) {
    }

    public void attack(NativeDeDeletionPorts.Viewer viewer, NativeDeNpcTemplateState nativeDeNpcTemplateState) {
    }

    public NativeDeDeletionPorts.Viewer death(NativeDeCharacterFields nativeDeCharacterFields, NativeDeNpcTemplateState nativeDeNpcTemplateState) {
        return null;
    }

    public int workTime() {
        return 0;
    }

    public void work(NativeDeNpcTemplateState nativeDeNpcTemplateState) {
    }

    public void spawn(NativeDeNpcTemplateState nativeDeNpcTemplateState) {
    }

    public String[] get_set() {
        return null;
    }

    public void set_set(String[] stringArray) {
    }
}
