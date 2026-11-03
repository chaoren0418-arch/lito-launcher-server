#ifndef IDLE_ADDRESS
#define IDLE_ADDRESS(address) (address)
#endif
enum : unsigned {
    idleActive = IDLE_ADDRESS(0x0064BBE8),
    idlePhase = IDLE_ADDRESS(0x0064AF98),
    idleAdvance = IDLE_ADDRESS(0x0048210C),
    idleWait = IDLE_ADDRESS(0x00482122)
};
volatile LONG g_battleWindowIdlePatchState{};

void __declspec(naked) BattleWindowIdleBridge() {
    __asm {
        mov al, byte ptr ds:[idleActive]
        test al, al
        jne advance
        cmp dword ptr ds:[idlePhase], 4
        je advance
        cmp dword ptr ds:[idlePhase], 5
        je advance
        push idleWait
        ret
    advance:
        // Retain the native rendering-pause guard at 64B8FC and execute
        // its existing scheduler/render callback on the original UI thread.
        push idleAdvance
        ret
    }
}

bool EnsureBattleWindowIdlePatch() {
    if (InterlockedCompareExchange(&g_battleWindowIdlePatchState, 0, 0) == 1) return true;
    if (!IsVerifiedLiToClient()) return false;
    std::vector<NumericPatch> patches;
    // A background-created HWND can retain active=0 even though later inactive
    // notifications are swallowed. Do not fabricate focus or route input to it.
    AddNumericRelativePatch(patches, 0x00482103,
        {0xA0,0xE8,0xBB,0x64,0x00,0x84,0xC0,0x74,0x16}, 0xE9,
        reinterpret_cast<void*>(&BattleWindowIdleBridge), "battle-background-idle");
    if (!CommitNumericPatches(patches)) return false;
    InterlockedExchange(&g_battleWindowIdlePatchState, 1);
    Log("battle/result idle scheduler enabled independently of foreground focus");
    return true;
}
