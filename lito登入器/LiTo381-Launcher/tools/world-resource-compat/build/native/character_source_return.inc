// The original Server callback is disabled by /ltcharsvr even on the manual
// character page. Scope its ordinary-role branch to this one explicit click.
void RouteCharacterSourceReturn(volatile BYTE& launchRole, void (__cdecl* nativeReturn)()) {
    const BYTE saved = launchRole;
    launchRole = 0;
    nativeReturn();
    launchRole = saved;
}
void __cdecl ReturnCharacterToSources() {
    if (!IsVerifiedLiToClient()) return;
    if (LoadLoginSources()) { RequestNativeSourceSession(0); return; }
    RouteCharacterSourceReturn(*reinterpret_cast<volatile BYTE*>(0x0064AF9D),
        reinterpret_cast<void (__cdecl*)()>(0x00492FE0));
    Log("opened native source selector from manual character page");
}
bool BindCharacterSourceReturn(std::uintptr_t root) {
    DWORD page{}, count{}, slots{}, button{}, vtable{}, resource{}, callback{};
    if (!root || !ReadToolbarBytes(root + 12, &page, 4) || page != 7
        || !ReadToolbarBytes(root + 4, &count, 4) || count != 8
        || !ReadToolbarBytes(root, &slots, 4) || !slots
        || !ReadToolbarBytes(slots + 16, &button, 4) || !button
        || !ReadToolbarBytes(button, &vtable, 4) || vtable != 0x004F7B60
        || !ReadToolbarBytes(button + 0x20, &resource, 4) || resource != 0x18A8
        || !ReadToolbarBytes(button + 0x24, &callback, 4)) return false;
    DWORD replacement = reinterpret_cast<DWORD>(&ReturnCharacterToSources);
    if (callback == replacement) return true;
    if (callback != 0x00492FE0) return false;
    *reinterpret_cast<DWORD*>(button + 0x24) = replacement;
    return true;
}
void __cdecl ReturnCharacterToLogin() {
    if (IsVerifiedLiToClient()) RequestNativeAccountLogin();
}
bool BindCharacterLoginReturn(std::uintptr_t root) {
    DWORD page{}, count{}, slots{}, button{}, vtable{}, resource{}, callback{};
    if (!LoadLoginSources() || !root || !ReadToolbarBytes(root + 12, &page, 4) || page != 7
        || !ReadToolbarBytes(root + 4, &count, 4) || count != 8
        || !ReadToolbarBytes(root, &slots, 4) || !slots
        || !ReadToolbarBytes(slots + 24, &button, 4) || !button
        || !ReadToolbarBytes(button, &vtable, 4) || vtable != 0x004F7B60
        || !ReadToolbarBytes(button + 0x20, &resource, 4) || resource != 0x183D
        || !ReadToolbarBytes(button + 0x24, &callback, 4)) return false;
    const DWORD replacement = reinterpret_cast<DWORD>(&ReturnCharacterToLogin);
    if (callback == replacement) return true;
    if (callback != 0x00494D50) return false;
    *reinterpret_cast<DWORD*>(button + 0x24) = replacement;
    return true;
}
void EnsureCharacterSourceReturn() {
    if (!IsVerifiedLiToClient()) return;
    DWORD root{};
    if (ReadToolbarBytes(0x0075F6E8, &root, 4)) {
        BindCharacterSourceReturn(root);
        BindCharacterLoginReturn(root);
    }
}
