package local.lito381.server;
/** Projection of the already-loaded common static range; Source -1 means the Source screen bound. */
final class NativeSourceVisibilityRange {
 static int value(int skill,int original){if(NativeSkillTargetMetadata.size()==0)return original;int range=NativeSkillTargetMetadata.row(skill).ranged();return range<0?19:range;}
 static int distance(int skill,int x,int y,int tx,int ty){if(NativeSkillTargetMetadata.size()!=0&&NativeSkillTargetMetadata.row(skill).ranged()==-1&&!inSourceScreen(tx-x,ty-y))return Integer.MAX_VALUE;return ClientSession.nativeSourceVisibilityOriginalDistance(x,y,tx,ty);}
 static int distance29(int x,int y,int tx,int ty){return distance(29,x,y,tx,ty);}
 static int distance44(int x,int y,int tx,int ty){return distance(44,x,y,tx,ty);}
 static int distance76(int x,int y,int tx,int ty){return distance(76,x,y,tx,ty);}
 static int signedDistance(int skill,int sxMinusTx,int syMinusTy){if(NativeSkillTargetMetadata.size()!=0&&NativeSkillTargetMetadata.row(skill).ranged()==-1&&!inSourceScreen(sxMinusTx,syMinusTy))return Integer.MAX_VALUE;return Math.max(Math.abs(sxMinusTx),Math.abs(syMinusTy));}
 static int signedDistance29(int dx,int dy){return signedDistance(29,dx,dy);}
 static int signedDistance44(int dx,int dy){return signedDistance(44,dx,dy);}
 static int signedDistance76(int dx,int dy){return signedDistance(76,dx,dy);}
 /**
  * 1-伺服器 types/Point.java:157-177 isInScreen（L1SkillUse.java:1712 對 ranged=-1 的判定）。
  * 第 19 格同時接受正向（|dx+18|+|dy+18|）與反向（|18-dx|+|18-dy|）邊界，所以結果對 (dx,dy) 取負號不變；
  * BattleSpellCatalog.isInNativeScreen 只有正向，會在 (19,0)／(0,19)／(18,-1)／(-1,18) 四格拒絕 Source 允許的施法（G09）。
  * 只用於技能射程；畫面廣播仍走 isInNativeScreen。
  */
 static boolean inSourceScreen(int dx,int dy){int dist=Math.abs(dx)+Math.abs(dy);if(dist>19)return false;if(dist<=18)return true;int forward=Math.abs(dx+18)+Math.abs(dy+18);int reverse=Math.abs(18-dx)+Math.abs(18-dy);return 19<=forward&&forward<=52||19<=reverse&&reverse<=52;}
 private NativeSourceVisibilityRange(){}
}
