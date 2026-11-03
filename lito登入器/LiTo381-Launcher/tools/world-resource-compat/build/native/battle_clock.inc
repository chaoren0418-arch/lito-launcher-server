#include "battle_clock.h"
BattleClock g_battleClock;
SRWLOCK g_battleClockLock = SRWLOCK_INIT;
volatile LONG g_battleClockPatchState{};

void __fastcall ReceiveBattleClockStart(const BYTE* packet) {
    AcquireSRWLockExclusive(&g_battleClockLock);
    g_battleClock.clear();
    ReleaseSRWLockExclusive(&g_battleClockLock);
    reinterpret_cast<void (__fastcall*)(const BYTE*)>(CLOCK_ADDRESS(0x004A98A0))(packet);
    AcquireSRWLockExclusive(&g_battleClockLock);
    g_battleClock.startScene();
    ReleaseSRWLockExclusive(&g_battleClockLock);
    Log("battle clock scene started; awaiting authoritative E9");
}
void __fastcall ReceiveBattleClockEnd(const BYTE* packet) {
    AcquireSRWLockExclusive(&g_battleClockLock);
    g_battleClock.clear();
    ReleaseSRWLockExclusive(&g_battleClockLock);
    reinterpret_cast<void (__fastcall*)(const BYTE*)>(CLOCK_ADDRESS(0x004A9A60))(packet);
    Log("battle clock cleared at result");
}
void __fastcall ReceiveBattleClockTime(const BYTE* packet) {
    DWORD seconds{};
    std::memcpy(&seconds, packet + 1, sizeof(seconds));
    // E9 remains the native c,d packet. Zero explicitly hides the clock.
    if (IsVerifiedTournamentClient()
            && *reinterpret_cast<volatile DWORD*>(CLOCK_ADDRESS(0x0064AF98)) == 4) {
        AcquireSRWLockExclusive(&g_battleClockLock);
        g_battleClock.synchronize(seconds, GetTickCount64());
        ReleaseSRWLockExclusive(&g_battleClockLock);
        Log("battle clock synchronized seconds=%lu", seconds);
    }
}
void DrawSharedBattleClock() {
    if (!IsVerifiedTournamentClient()) return;
    const DWORD phase = *reinterpret_cast<volatile DWORD*>(CLOCK_ADDRESS(0x0064AF98));
    AcquireSRWLockExclusive(&g_battleClockLock);
    if (phase != 4) g_battleClock.clear();
    const bool visible = g_battleClock.visible;
    const DWORD remaining = g_battleClock.remaining(GetTickCount64());
    ReleaseSRWLockExclusive(&g_battleClockLock);
    if (!visible) return;
    auto modes = *reinterpret_cast<DWORD**>(CLOCK_ADDRESS(0x0064AFE0));
    const DWORD map = *reinterpret_cast<DWORD*>(CLOCK_ADDRESS(0x0075A898));
    if (modes && modes[map*9+1] == 0) {
        // Pet Race already displays elapsed time at the original location.
        DrawRaceRemainingClock(remaining);
        return;
    }
    // Run ONLY from sub_488860's native render pass, before frame scaling.
    // The original two mode-specific calls are removed in the same transaction.
    if (remaining <= 5999) {
        *reinterpret_cast<volatile WORD*>(CLOCK_ADDRESS(0x0064AF78)) = static_cast<WORD>(remaining / 60);
        *reinterpret_cast<volatile WORD*>(CLOCK_ADDRESS(0x0064AF76)) = static_cast<WORD>(remaining % 60);
        *reinterpret_cast<volatile BYTE*>(CLOCK_ADDRESS(0x0064AF74)) = remaining < 60 ? 1 : 0;
        reinterpret_cast<void (__cdecl*)()>(CLOCK_ADDRESS(0x0045F4A0))();
    } else {
        DrawExtendedBattleClock(remaining);
    }
}
void __declspec(naked) SharedBattleClockRenderBridge() {
    __asm {
        pushfd
        pushad
        call DrawSharedBattleClock
        popad
        popfd
        mov edx, dword ptr ds:[0064AFE0h]
        push 0048AB80h
        ret
    }
}
bool EnsureSharedBattleClockPatch() {
    if (InterlockedCompareExchange(&g_battleClockPatchState, 0, 0) == 1) return true;
    if (!IsVerifiedLiToClient()) return false;
    std::vector<NumericPatch> patches;
    AddNumericRelativePatch(patches, 0x004B2F45, {0xE8,0x56,0x69,0xFF,0xFF},0xE8,
        reinterpret_cast<void*>(&ReceiveBattleClockStart),"clock-start");
    AddNumericRelativePatch(patches, 0x004B2F51, {0xE8,0x0A,0x6B,0xFF,0xFF},0xE8,
        reinterpret_cast<void*>(&ReceiveBattleClockEnd),"clock-end");
    AddNumericRelativePatch(patches, 0x004B2FC9, {0xE8,0xA2,0x94,0xFF,0xFF},0xE8,
        reinterpret_cast<void*>(&ReceiveBattleClockTime),"clock-time");
    AddNumericRelativePatch(patches, 0x0048AB7A, {0x8B,0x15,0xE0,0xAF,0x64,0x00},0xE9,
        reinterpret_cast<void*>(&SharedBattleClockRenderBridge),"clock-all-mode-render");
    AddNumericPatch(patches,0x0048AB8C,{0xE8,0x0F,0x49,0xFD,0xFF},{0x90,0x90,0x90,0x90,0x90},"clock-remove-duplicate-timeattack");
    AddNumericPatch(patches,0x0048ABA9,{0xE8,0xF2,0x48,0xFD,0xFF},{0x90,0x90,0x90,0x90,0x90},"clock-remove-duplicate-siege");
    // Old timer messages remain harmless. The monotonic presentation clock
    // replaces their decrement, so delayed messages cannot double-decrement.
    AddNumericRelativePatch(patches,0x0047FB4F,{0x66,0x83,0x3D,0x76,0xAF,0x64,0x00,0x00},0xE9,
        reinterpret_cast<void*>(CLOCK_ADDRESS(0x0047FFE4)),"clock-ignore-old-tick");
    if (!CommitNumericPatches(patches)) return false;
    InterlockedExchange(&g_battleClockPatchState,1);
    Log("shared battle clock installed; native resources, all modes, pre-scale render");
    return true;
}
