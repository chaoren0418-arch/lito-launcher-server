package local.lito381.server;
/** Projection of the already-loaded common static range; Source -1 means the native screen bound. */
final class NativeSourceVisibilityRange {
 static int value(int skill,int original){if(NativeSkillTargetMetadata.size()==0)return original;int range=NativeSkillTargetMetadata.row(skill).ranged();return range<0?19:range;}
 static int distance(int skill,int x,int y,int tx,int ty){if(NativeSkillTargetMetadata.size()!=0&&NativeSkillTargetMetadata.row(skill).ranged()==-1&&!BattleSpellCatalog.isInNativeScreen(x,y,tx,ty))return Integer.MAX_VALUE;return ClientSession.nativeSourceVisibilityOriginalDistance(x,y,tx,ty);}
 static int distance29(int x,int y,int tx,int ty){return distance(29,x,y,tx,ty);}
 static int distance44(int x,int y,int tx,int ty){return distance(44,x,y,tx,ty);}
 static int distance76(int x,int y,int tx,int ty){return distance(76,x,y,tx,ty);}
 static int signedDistance(int skill,int sxMinusTx,int syMinusTy){if(NativeSkillTargetMetadata.size()!=0&&NativeSkillTargetMetadata.row(skill).ranged()==-1&&!BattleSpellCatalog.isInNativeScreen(sxMinusTx,syMinusTy,0,0))return Integer.MAX_VALUE;return Math.max(Math.abs(sxMinusTx),Math.abs(syMinusTy));}
 static int signedDistance29(int dx,int dy){return signedDistance(29,dx,dy);}
 static int signedDistance44(int dx,int dy){return signedDistance(44,dx,dy);}
 static int signedDistance76(int dx,int dy){return signedDistance(76,dx,dy);}
 private NativeSourceVisibilityRange(){}
}
