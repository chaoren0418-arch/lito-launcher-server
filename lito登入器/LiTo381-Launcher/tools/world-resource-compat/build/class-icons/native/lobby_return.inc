#ifndef LOBBY_NATIVE_ADDRESS
#define LOBBY_NATIVE_ADDRESS(address) (address)
#endif

void RouteLobbyReturn(DWORD phase, volatile DWORD& returnMode,
        void (__cdecl* nativeReturn)()) {
    if (phase == 1) {
        // sub_455330 -> loc_46F010 mode 1 owns modal teardown and C_CB/C_CA.
        // S_F5 only emits C_C8 when the source-refresh flag is armed.
        *reinterpret_cast<volatile BYTE*>(LOBBY_NATIVE_ADDRESS(0x0064AFC5)) = 1;
        // Mode 2 instead sends C_QUIT and reconnects using a launcher cache
        // absent from this standalone client (observed empty C_C8 source).
        returnMode = 1;
        Log("lobby Return selected native character-refresh path C_CB/C_CA100");
    }
    nativeReturn();
    if (phase == 1) {
        // Native Return arms automatic source B. The explicit C_C8 response
        // must instead populate manual character rows, retaining source index.
        *reinterpret_cast<volatile WORD*>(LOBBY_NATIVE_ADDRESS(0x0064AFBC)) = 0;
    }
}

void __fastcall ReturnLobbyToCharacters(void*, void*) {
    if (!IsVerifiedTournamentClient()) return;
    RouteLobbyReturn(*reinterpret_cast<volatile DWORD*>(0x0064AF98),
        *reinterpret_cast<volatile DWORD*>(0x0064AFB8),
        reinterpret_cast<void (__cdecl*)()>(0x00455330));
}

bool BindLobbyReturnButton(void* dialog) {
    if (!dialog || *reinterpret_cast<DWORD*>(dialog) != 0x004F85F0) return false;
    const auto manager = *reinterpret_cast<std::uintptr_t*>(static_cast<BYTE*>(dialog) + 8);
    if (!manager) return false;
    const auto slots = *reinterpret_cast<std::uintptr_t*>(manager);
    const auto count = *reinterpret_cast<DWORD*>(manager + 4);
    if (!slots || count != 5) return false;
    const auto button = *reinterpret_cast<std::uintptr_t*>(slots + 2 * sizeof(DWORD));
    if (!button || *reinterpret_cast<DWORD*>(button) != 0x004F9AD0
        || *reinterpret_cast<DWORD*>(button + 0x20) != 0x1D92
        || *reinterpret_cast<DWORD*>(button + 0x48) != 0x1D94) return false;
    auto callback = reinterpret_cast<std::uintptr_t*>(button + 0x2C);
    const auto replacement = reinterpret_cast<std::uintptr_t>(&ReturnLobbyToCharacters);
    if (*callback == replacement) return true;
    if (*callback != 0x00455330) return false;
    *callback = replacement;
    Log("bound lobby Return button=%p callback=455330 phase-guard=1", reinterpret_cast<void*>(button));
    return true;
}

#ifndef LOBBY_NATIVE_ADDRESS
#define LOBBY_NATIVE_ADDRESS(address) (address)
#endif
bool EnsureLobbyReturnAction() {
    void* dialog = *reinterpret_cast<void* volatile*>(LOBBY_NATIVE_ADDRESS(0x00648F20));
    const bool bound = BindLobbyReturnButton(dialog);
    if (!bound) Log("lobby Return binding skipped: native dialog/control signature mismatch");
    return bound;
}
