// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.util.Objects;
import local.lito381.server.CombatAccess;
import local.lito381.server.NativeDeCharacterFields;
import local.lito381.server.NativeDeClientViewer;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeDeletionState;
import local.lito381.server.NativeDeObjectFields;
import local.lito381.server.NativeDeWorldContext;
import local.lito381.server.NativeDeWorldMap;
import local.lito381.server.NativeDeWorldScope;

final class NativeDeObjectScope {
    private static final ThreadLocal<NativeDeObjectFields> current = new ThreadLocal();

    NativeDeObjectScope() {
    }

    static void run(NativeDeObjectFields nativeDeObjectFields, Runnable runnable) {
        NativeDeObjectFields nativeDeObjectFields2 = current.get();
        current.set(Objects.requireNonNull(nativeDeObjectFields));
        try {
            runnable.run();
        }
        finally {
            if (nativeDeObjectFields2 == null) {
                current.remove();
            } else {
                current.set(nativeDeObjectFields2);
            }
        }
    }

    static NativeDeObjectFields actor() {
        NativeDeObjectFields nativeDeObjectFields = current.get();
        if (nativeDeObjectFields == null) {
            throw new IllegalStateException("source object mutation scope absent");
        }
        return nativeDeObjectFields;
    }

    static boolean present() {
        return current.get() != null;
    }

    static NativeDeWorldContext context(NativeDeObjectFields nativeDeObjectFields) {
        if (nativeDeObjectFields instanceof NativeDeObjectFields) {
            NativeDeCharacterFields nativeDeCharacterFields;
            NativeDeObjectFields nativeDeObjectFields2 = nativeDeObjectFields;
            NativeDeWorldContext.Entry entry = (NativeDeWorldContext.Entry)CombatAccess.field(nativeDeObjectFields2, "sourceDeWorldEntry");
            if (entry != null) {
                return entry.context;
            }
            if (nativeDeObjectFields2 instanceof NativeDeDeletionState) {
                NativeDeDeletionState deletionState = (NativeDeDeletionState)nativeDeObjectFields2;
                NativeDeDeletionPorts.Services services = deletionState.deletionOwner.services;
                if (services instanceof NativeDeWorldContext) {
                    NativeDeWorldContext nativeDeWorldContext = (NativeDeWorldContext)services;
                    return nativeDeWorldContext;
                }
            }
            if (nativeDeObjectFields2 instanceof NativeDeClientViewer) {
                nativeDeCharacterFields = (NativeDeClientViewer)nativeDeObjectFields2;
                return (NativeDeWorldContext)CombatAccess.field(((NativeDeClientViewer)nativeDeCharacterFields).hub, "sourceDeWorldContext");
            }
        }
        return NativeDeWorldScope.present() ? NativeDeWorldScope.owner() : null;
    }

    static final class World {
        World() {
        }

        static World get() {
            return new World();
        }

        void updateObjectSector(NativeDeObjectFields nativeDeObjectFields, int n, int n2, int n3, int n4, int n5, int n6) {
            NativeDeObjectFields nativeDeObjectFields2;
            if (nativeDeObjectFields instanceof NativeDeObjectFields) {
                nativeDeObjectFields2 = (NativeDeObjectFields)nativeDeObjectFields;
            } else {
                return;
            }
            NativeDeWorldContext.Entry entry = (NativeDeWorldContext.Entry)CombatAccess.field(nativeDeObjectFields2, "sourceDeWorldEntry");
            if (entry == null) {
                return;
            }
            entry.context.call("updateObjectSector", "(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;IIIIII)V", nativeDeObjectFields, n, n2, n3, n4, n5, n6);
        }
    }

    static final class Maps {
        Maps() {
        }

        static Maps get() {
            return new Maps();
        }

        NativeDeWorldMap getMap(short s) {
            NativeDeWorldContext nativeDeWorldContext = NativeDeObjectScope.present()
                    ? NativeDeObjectScope.context(NativeDeObjectScope.actor())
                    : (NativeDeWorldScope.present() ? NativeDeWorldScope.owner() : null);
            if (nativeDeWorldContext == null) {
                throw new IllegalStateException("source map lookup requires a native World binding");
            }
            return nativeDeWorldContext.maps.hasMap(s) ? nativeDeWorldContext.map(s) : NativeDeWorldMap.newNull();
        }
    }
}
