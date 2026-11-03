// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.NativeDeCharacterFields;
import local.lito381.server.NativeDeClientViewer;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeDeletionState;
import local.lito381.server.NativeDeInstanceState;
import local.lito381.server.NativeDeNpcConstructionState;
import local.lito381.server.NativeDeObjectFields;
import local.lito381.server.NativeDeVisibilityRuntime;

final class NativeDeVisibilityPorts {
    static final Services NATIVE = new Services(){

        @Override
        public void perceive(NativeDeDeletionPorts.ObjectRef objectRef, NativeDeDeletionPorts.Viewer viewer) {
            if (objectRef instanceof NativeDeInstanceState) {
                NativeDeInstanceState nativeDeInstanceState = (NativeDeInstanceState)objectRef;
                nativeDeInstanceState.onPerceive(viewer);
            } else {
                if (objectRef instanceof NativeDeDeletionState) {
                    throw new UnsupportedOperationException("full non-De NPC perception producer remains unqualified");
                }
                ((NativeDeObjectFields)objectRef).onPerceive(viewer);
            }
        }

        @Override
        public void approach(NativeDeDeletionState nativeDeDeletionState, NativeDeDeletionPorts.Viewer viewer) {
            throw new UnsupportedOperationException("full hidden NPC reveal/item/chat producer remains unqualified");
        }

        @Override
        public boolean teleport(NativeDeDeletionPorts.Viewer viewer) {
            if (viewer instanceof NativeDeClientViewer) {
                NativeDeClientViewer nativeDeClientViewer = (NativeDeClientViewer)viewer;
                return NativeDeVisibilityRuntime.client(nativeDeClientViewer) == null;
            }
            throw new UnsupportedOperationException("non-client teleport state producer must be bound");
        }

        @Override
        public int hidden(NativeDeDeletionState nativeDeDeletionState) {
            if (nativeDeDeletionState instanceof NativeDeNpcConstructionState) {
                NativeDeNpcConstructionState nativeDeNpcConstructionState = (NativeDeNpcConstructionState)nativeDeDeletionState;
                return nativeDeNpcConstructionState.getHiddenStatus();
            }
            throw new UnsupportedOperationException("legacy NPC hidden state is not an authoritative full NPC");
        }
    };

    private NativeDeVisibilityPorts() {
    }

    static interface Services {
        public void perceive(NativeDeDeletionPorts.ObjectRef var1, NativeDeDeletionPorts.Viewer var2);

        public void approach(NativeDeDeletionState var1, NativeDeDeletionPorts.Viewer var2);

        public boolean teleport(NativeDeDeletionPorts.Viewer var1);

        public int hidden(NativeDeDeletionState var1);
    }

    static interface FieldObject
    extends NativeDeDeletionPorts.ObjectRef {
    }

    static interface Follower
    extends NativeDeDeletionPorts.ObjectRef {
        public NativeDeCharacterFields getMaster();
    }
}
