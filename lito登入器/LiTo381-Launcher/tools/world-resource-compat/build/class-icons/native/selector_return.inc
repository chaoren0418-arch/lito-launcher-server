#ifndef LOBBY_NATIVE_ADDRESS
#define LOBBY_NATIVE_ADDRESS(address) (address)
#endif
#ifndef SELECTOR_CALL
#define SELECTOR_CALL(address, type) reinterpret_cast<type>(address)
#endif
static bool g_returnCharactersPending{};
template<typename T> volatile T& SelectorValue(std::uintptr_t address) {
    return *reinterpret_cast<volatile T*>(LOBBY_NATIVE_ADDRESS(address));
}
bool IsChannelSelectorRoot() {
    auto root = SelectorValue<std::uintptr_t>(0x75F6E8);
    // Page 5 selects a source/server; page 40 selects a Tournament channel.
    return root && *reinterpret_cast<DWORD*>(root + 0x0C) == 40
        && *reinterpret_cast<DWORD*>(root + 4) == 5;
}
void __fastcall ReturnChannelToCharacters(void*, void*) {
    if (!IsVerifiedTournamentClient() || !IsChannelSelectorRoot()
            || SelectorValue<DWORD>(0x64AF98) != 0) return;
    g_returnCharactersPending = true;
    SelectorValue<BYTE>(0x64AFC5) = 1;
    SelectorValue<DWORD>(0x64AFB8) = 1;
    SELECTOR_CALL(0x46B1A0, void (__cdecl*)())();
    SelectorValue<WORD>(0x64AFBC) = 0;
    Log("channel Return requested character directory; waiting S03");
}
bool BindChannelReturnButton() {
    if (!IsChannelSelectorRoot()) return false;
    auto root = SelectorValue<std::uintptr_t>(0x75F6E8);
    auto slots = *reinterpret_cast<std::uintptr_t*>(root);
    if (!slots) return false;
    auto button = *reinterpret_cast<std::uintptr_t*>(slots + 16);
    if (!button || *reinterpret_cast<DWORD*>(button) != 0x4F7B60
            || *reinterpret_cast<DWORD*>(button + 0x20) != 0x183D) return false;
    auto& callback = *reinterpret_cast<std::uintptr_t*>(button + 0x24);
    auto replacement = reinterpret_cast<std::uintptr_t>(&ReturnChannelToCharacters);
    if (callback == replacement) return true;
    if (callback != 0x46B1A0) return false;
    callback = replacement;
    return true;
}
void __cdecl OpenChannelSelector() {
    SelectorValue<DWORD>(0x64AF98) = 0;
    SelectorValue<DWORD>(0x64AFB8) = 1;
    SelectorValue<BYTE>(0x64AFC7) = 0;
    SelectorValue<BYTE>(0x64AFC8) = 0;
    SELECTOR_CALL(0x496A00, void (__cdecl*)())();
    if (IsChannelSelectorRoot()) {
        // Lobby rendering retains this root. Reuse it instead of pushing a
        // second channel page onto the native selector stack on every return.
        SELECTOR_CALL(0x4952F0, void (__fastcall*)(void*,BYTE))(
            reinterpret_cast<void*>(SelectorValue<std::uintptr_t>(0x75F6E8)),1);
    } else {
        SELECTOR_CALL(0x497020, void (__cdecl*)())();
    }
    Log("channel selector ready phase=0 bound-return=%d", BindChannelReturnButton());
}
void __fastcall ReturnLobbyToCharacters(void*, void*) {
    if (!IsVerifiedTournamentClient()) return;
    if (SelectorValue<DWORD>(0x64AF98) != 1) {
        SELECTOR_CALL(0x455330, void (__cdecl*)())();
        return;
    }
    // C_CA100 belongs to channel -> characters. The lobby first sends C_CB
    // and displays the channel selector without requesting another directory.
    auto close = SELECTOR_CALL(0x4EBEB0, void (__fastcall*)(int));
    close(0); close(0);
    SELECTOR_CALL(0x4C6AE0, void (__cdecl*)(const char*,...))(
        reinterpret_cast<const char*>(0x5025A8), 0xCB);
    SELECTOR_CALL(0x469CA0, void (__fastcall*)(int))(0);
    SelectorValue<WORD>(0x64AFBC) = 0;
    SelectorValue<BYTE>(0x64AFC5) = 0;
    g_returnCharactersPending = false;
    OpenChannelSelector();
}
void __fastcall ReceiveSelectorCharacterCount(void* packet, void*) {
    if (g_returnCharactersPending && SelectorValue<std::uintptr_t>(0x75F6E8)) {
        SelectorValue<WORD>(0x64AFBC) = 0;
        SelectorValue<DWORD>(0x64AF98) = 0;
        SelectorValue<DWORD>(0x64BBD4) = 4;
        auto root=SelectorValue<std::uintptr_t>(0x75F6E8);
        if (*reinterpret_cast<DWORD*>(root+0x0C) != 7)
            SELECTOR_CALL(0x496EB0, void (__cdecl*)())();
        g_returnCharactersPending = false;
        Log("character selector rebuilt before directory rows");
    }
    SELECTOR_CALL(0x4A7450, void (__fastcall*)(void*,void*))(packet,nullptr);
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
    return true;
}
bool EnsureLobbyReturnAction() {
    return BindLobbyReturnButton(*reinterpret_cast<void* volatile*>(LOBBY_NATIVE_ADDRESS(0x648F20)));
}
