#ifndef RESULT_ADDRESS
#define RESULT_ADDRESS(address) (address)
#endif

volatile LONG g_resultReturnPatchState{};

bool NativeResultHasNoSummary() {
    if (*reinterpret_cast<volatile DWORD*>(RESULT_ADDRESS(0x0064AF98)) != 5)
        return false;
    const auto result = *reinterpret_cast<std::uintptr_t*>(RESULT_ADDRESS(0x0064A910));
    const auto summary = *reinterpret_cast<std::uintptr_t*>(RESULT_ADDRESS(0x0064A914));
    // Exact sub_4A9770 branch: both FF result selectors bypass the WIN/LOSE
    // modal. There is no Confirm button that could later produce C_D0.
    return result && summary
        && *reinterpret_cast<const BYTE*>(result + 0x0C) == 0xFF
        && *reinterpret_cast<const BYTE*>(summary + 0x1C) == 0xFF
        && !*reinterpret_cast<const BYTE*>(RESULT_ADDRESS(0x00648F1C))
        && !*reinterpret_cast<const BYTE*>(RESULT_ADDRESS(0x00648F08));
}

void __fastcall ReceiveResultReturn(const BYTE* packet) {
    const bool bypassedSummary = IsVerifiedTournamentClient() && NativeResultHasNoSummary();
    reinterpret_cast<void (__fastcall*)(const BYTE*)>(RESULT_ADDRESS(0x004A9770))(packet);
    if (bypassedSummary
            && *reinterpret_cast<volatile DWORD*>(RESULT_ADDRESS(0x0064AF98)) == 2) {
        InterlockedExchange(&g_resultSummaryPending, 0);
        const bool sent = SendTournamentOpcode(0xD0);
        Log("result without summary: native F0 completed; exit-room sent=%d pid=%lu", sent, GetCurrentProcessId());
    }
}

bool EnsureResultReturnPatch() {
    if (InterlockedCompareExchange(&g_resultReturnPatchState, 0, 0) == 1) return true;
    if (!IsVerifiedLiToClient()) return false;
    std::vector<NumericPatch> patches;
    AddNumericRelativePatch(patches, 0x004B2FB1, {0xE8,0xBA,0x67,0xFF,0xFF}, 0xE8,
        reinterpret_cast<void*>(&ReceiveResultReturn), "result-no-summary-return");
    if (!CommitNumericPatches(patches)) return false;
    InterlockedExchange(&g_resultReturnPatchState, 1);
    return true;
}
