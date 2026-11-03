bool IsWorldExitPhase(DWORD phase) { return phase >= 3 && phase <= 5; }

bool NativeWorldExitActionAllowed(DWORD action) {
    // Original sub_4C22A0 accepts idle/dead, rejects attack and other actions.
    return action == 0 || action == 0x400;
}

void __fastcall RequestWorldExitResult(void*, void*) {
    if (!IsVerifiedTournamentClient()) return;
    const DWORD phase = *reinterpret_cast<volatile DWORD*>(0x0064AF98);
    if (!IsWorldExitPhase(phase)) {
        reinterpret_cast<void (__cdecl*)()>(0x004C22A0)();
        return;
    }
    if (phase == 5) {
        // S_D3 already settled the match; no live combat/action lease remains.
        reinterpret_cast<void (__thiscall*)(void*)>(0x004EBEB0)(nullptr);
        SendTournamentOpcode(0xD9);
        Log("result exit requested native summary through C_D9/F0");
        return;
    }
    DWORD encodedIndex{}, table{}, key{}, encodedAction{};
    if (!ReadToolbarBytes(0x00831708, &encodedIndex, 4)
            || !ReadToolbarBytes(0x0083170C, &table, 4)
            || !ReadToolbarBytes(0x00831710, &key, 4)) return;
    const std::uint64_t address = static_cast<std::uint64_t>(table)
        + static_cast<std::uint64_t>(encodedIndex ^ 0xB104FA87U) * 4;
    if (address > 0xFFFFFFFCULL
            || !ReadToolbarBytes(static_cast<std::uintptr_t>(address), &encodedAction, 4)) return;
    const DWORD action = encodedAction ^ key;
    if (!NativeWorldExitActionAllowed(action)) {
        Log("world exit blocked by native action=%08lX", action);
        return;
    }
    // Close just the modal. Keep the world and its packet receivers intact
    // until S_D3 starts the ordinary RESULT -> D9/F0 -> Confirm/D0 lifecycle.
    reinterpret_cast<void (__thiscall*)(void*)>(0x004EBEB0)(nullptr);
    SendTournamentOpcode(0xD9);
    Log("world exit requested settlement; native world teardown deferred to S_D3");
}

bool BindWorldExitButton(void* dialog) {
    if (!dialog || *reinterpret_cast<DWORD*>(dialog) != 0x004FA368) return false;
    const auto manager = *reinterpret_cast<std::uintptr_t*>(static_cast<BYTE*>(dialog) + 8);
    if (!manager || *reinterpret_cast<DWORD*>(manager + 4) != 4) return false;
    const auto slots = *reinterpret_cast<std::uintptr_t*>(manager);
    if (!slots) return false;
    const auto button = *reinterpret_cast<std::uintptr_t*>(slots + 2 * sizeof(DWORD));
    if (!button || *reinterpret_cast<DWORD*>(button) != 0x004F9AD0
        || *reinterpret_cast<DWORD*>(button + 0x20) != 0x429
        || *reinterpret_cast<DWORD*>(button + 0x48) != 0x4FF) return false;
    auto callback = reinterpret_cast<std::uintptr_t*>(button + 0x2C);
    const auto replacement = reinterpret_cast<std::uintptr_t>(&RequestWorldExitResult);
    if (*callback == replacement) return true;
    if (*callback != 0x004C22A0) return false;
    *callback = replacement;
    Log("bound native world exit confirmation to settlement request");
    return true;
}
