package local.lito381.server;

/** Candidate-only SELECT result, never a selection or appearance capability. */
interface NativeAppearanceReadProvider extends ModernEquipmentAuthority.PhysicalProvider {
    record Base(int source,long object,int professionType,int sex,int persistedRenderClass) {
        public Base {
            if(source<1||object<1||professionType<0||professionType>6||sex<0||sex>1||persistedRenderClass<0||persistedRenderClass>65535)
                throw new IllegalArgumentException("appearance base facts");
        }
    }
    final class Bundle {
        private final Base base;private final ModernEquipmentAuthority.PhysicalLoaded physical;private final ModernBackendScope actualScope;
        Bundle(Base base,ModernEquipmentAuthority.PhysicalLoaded physical,ModernBackendScope actualScope) {
            var s=physical.source().input().equipment().source();var c=s.character();
            if(base.source()!=s.sourceId()||base.object()!=c.objectId()||base.professionType()!=c.characterClass()||base.sex()!=c.sex())
                throw new IllegalArgumentException("appearance bundle identity");
            this.base=base;this.physical=physical;this.actualScope=java.util.Objects.requireNonNull(actualScope);
        }
        Base base(){return base;}
        ModernEquipmentAuthority.PhysicalLoaded physical(){return physical;}
        boolean belongsTo(ModernBackendScope expected){return actualScope==expected;}
        @Override public String toString(){return "NativeAppearanceSourceBundle[redacted,UNKNOWN]";}
    }
    Bundle readObserved(ModernAccountAuthentication.Principal principal,int source,long object)throws Exception;
}
