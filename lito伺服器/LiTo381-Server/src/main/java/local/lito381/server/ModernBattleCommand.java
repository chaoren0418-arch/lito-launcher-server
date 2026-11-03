package local.lito381.server;

/** Trusted exact command result, captured before releasing the engine monitor.
 * Status/reason stays in each typed result. Capture contains PRIVATE server
 * data: authenticate/scope/project it before any future negotiated response.
 * Legacy snapshot()/frame() methods are output adapters, never runtime state. */
sealed interface ModernBattleCommand permits ModernBattleObservation.Capture,
        ModernBattleEngine.MoveResult,ModernBattleEngine.AttackResult,ModernBattleEngine.CastResult,
        ModernBattleEngine.DisintegrateResult,ModernBattleEngine.GreaterHasteResult,ModernBattleEngine.SlowResult,ModernBattleEngine.HasteResult,ModernBattleEngine.PlayerHealingResult,ModernBattleEngine.ItemResult,ModernBattleEngine.AmmoResult,
        ModernBattleEngine.SiegePortalResult,ModernNpcBattle.Result,ModernNpcMagic.Result,
        ModernNpcDisintegrateState.Result,ModernNpcFireStormState.Result,ModernQuestLife.Result {
    ModernBattleObservation.Capture observation();
}
