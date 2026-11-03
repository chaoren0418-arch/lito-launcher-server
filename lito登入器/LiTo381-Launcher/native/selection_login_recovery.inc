// Only S_LOGIN_CHECK/ACCOUNT_IN_USE (selector 0x16) is rerouted.
// Native message text, appearance and modality are retained.
void __fastcall ConfirmRejectedSourceAccount(int result, void*) {
    if (RequestNativeAccountLogin()) {
        Log("account-in-use acknowledged; deferred logout to blank account login");
        return;
    }
    reinterpret_cast<void (__fastcall*)(int)>(0x48F1C0)(result);
}
void __fastcall ShowRejectedSourceAccount(const char* text, void*) {
    if (!LoadLoginSources() || g_loginSourceSelected <= 0) {
        reinterpret_cast<void (__fastcall*)(const char*)>(0x48F190)(text);
        return;
    }
    void* root=*reinterpret_cast<void**>(0x75F6E8);
    reinterpret_cast<void (__thiscall*)(void*,int)>(0x41B720)(root,1);
    reinterpret_cast<void (__thiscall*)(void*)>(0x41B6E0)(root);
    reinterpret_cast<void (__fastcall*)(const char*,int,void*)>(0x4980F0)(
        text,1,reinterpret_cast<void*>(&ConfirmRejectedSourceAccount));
}
void AddSelectionLoginRecoveryPatches(std::vector<NumericPatch>& patches) {
    AddNumericRelativePatch(patches,0x490745,{0xE8,0x46,0xEA,0xFF,0xFF},0xE8,
        reinterpret_cast<void*>(&ShowRejectedSourceAccount),"source-account-in-use-return");
}
