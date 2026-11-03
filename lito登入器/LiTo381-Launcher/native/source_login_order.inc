// Source selection and account logout keep the same process/window.
// Restore protected input containers before clearing the plain GAME handoff cache.
struct LoginSourceRow { DWORD id; DWORD port; char name[64]; };
static LoginSourceRow g_loginSources[8]{};
static unsigned g_loginSourceCount{};
static int g_loginSourceSelected{-1};
static char g_loginHost[64]{};
static BYTE g_accountContainers[28*12]{}, g_passwordContainers[14*12]{};
static bool g_credentialContainersSaved{};
constexpr UINT kSourceLogoutMessage = WM_APP + 0x389;
static bool g_loginSourcePending{};
static bool g_logoutToAccountLogin{};

bool LoadLoginSources() {
    if (g_loginSourceSelected >= 0) return g_loginSourceCount != 0;
    wchar_t catalog[2048]{};
    g_loginSourceSelected = 0;
    if (!GetEnvironmentVariableA("LITO_NATIVE_LOGIN_HOST", g_loginHost, 64)
        || !GetEnvironmentVariableW(L"LITO_NATIVE_SOURCES", catalog, 2048)) return false;
    wchar_t* context{};
    for (wchar_t* row = wcstok_s(catalog, L";", &context); row; row = wcstok_s(nullptr, L";", &context)) {
        wchar_t* separator = wcschr(row, L':');
        if (!separator || g_loginSourceCount == 8) { g_loginSourceCount = 0; return false; }
        *separator++ = 0;
        const int id = _wtoi(row);
        wchar_t* name = wcschr(separator, L':');
        if (!name) { g_loginSourceCount = 0; return false; }
        *name++ = 0; const int port = _wtoi(separator); separator = name;
        const size_t length = wcslen(separator);
        if (id < 1 || id > 8 || port < 1025 || port > 65535 || length == 0 || length > 126 || length % 2) { g_loginSourceCount = 0; return false; }
        for (unsigned i = 0; i < g_loginSourceCount; ++i)
            if (g_loginSources[i].id == static_cast<DWORD>(id)) { g_loginSourceCount = 0; return false; }
        auto& output = g_loginSources[g_loginSourceCount++]; output.id = id; output.port = port;
        for (size_t i = 0; i < length / 2; ++i) {
            wchar_t hex[]{separator[i*2], separator[i*2+1], 0}; wchar_t* end{};
            const auto value = wcstoul(hex, &end, 16);
            if (*end || !value) { g_loginSourceCount = 0; return false; }
            output.name[i] = static_cast<char>(value);
        }
    }
    return g_loginSourceCount != 0;
}

bool ClearSourceCredentials() {
    const DWORD bridge = LiToSourceSession[2];
    if (!bridge || !g_credentialContainersSaved) return false;
    // The reconnect hook writes plain strings over the first protected cells.
    // Their original allocation pointers must be restored before invoking setters.
    *reinterpret_cast<volatile DWORD*>(bridge + 0x810) = 0;
    SecureZeroMemory(reinterpret_cast<void*>(bridge + 0x810), 0x70);
    // The physical-login page builder is once per authenticated session.
    if (LiToSourceSession[6]) *reinterpret_cast<DWORD*>(LiToSourceSession[6]) = 0;
    if (LiToSourceSession[7]) *reinterpret_cast<DWORD*>(LiToSourceSession[7]) = 0;
    memcpy(reinterpret_cast<void*>(0x641348), g_accountContainers, sizeof(g_accountContainers));
    memcpy(reinterpret_cast<void*>(0x6412A0), g_passwordContainers, sizeof(g_passwordContainers));
    for (unsigned i = 0; i < 28; ++i)
        reinterpret_cast<void (__thiscall*)(void*, int)>(0x4C3D30)(reinterpret_cast<void*>(0x641348 + i*12), 0);
    for (unsigned i = 0; i < 14; ++i)
        reinterpret_cast<void (__thiscall*)(void*, int)>(0x4C3D30)(reinterpret_cast<void*>(0x6412A0 + i*12), 0);
    *reinterpret_cast<BYTE*>(0x75F755) = 0;
    *reinterpret_cast<BYTE*>(0x75F756) = 1;
    *reinterpret_cast<WORD*>(0x64AFBC) = 0;
    *reinterpret_cast<BYTE*>(0x64AFBE) = 0;
    *reinterpret_cast<BYTE*>(0x64AF9D) = 0;
    return true;
}

void CloseSourceConnection() {
    for (DWORD timer : {0x469B10U, 0x469BD0U, 0x48F110U})
        reinterpret_cast<void (__fastcall*)(DWORD)>(0x4D6970)(timer);
    reinterpret_cast<void (__cdecl*)()>(0x4C65F0)();
    // Discard socket notifications already queued for the closed connection.
    MSG message{};
    while (PeekMessageW(&message, g_window, 0x401, 0x402, PM_REMOVE)) {}
    *reinterpret_cast<BYTE*>(0x75F754) = 0;
}

void ShowNativeSourceLogin() {
    *reinterpret_cast<DWORD*>(0x64BBD4) = 2;
    *reinterpret_cast<DWORD*>(0x64AF98) = 0;
    *reinterpret_cast<DWORD*>(0x62C9B8) = 0x496200;
    *reinterpret_cast<DWORD*>(0x62C9BC) = 0x4962B0;
    *reinterpret_cast<DWORD*>(0x62C9C0) = 0x495E40;
    const DWORD root = *reinterpret_cast<DWORD*>(0x75F6E8);
    reinterpret_cast<void (__fastcall*)(DWORD, bool)>(0x4952F0)(root, true);
    reinterpret_cast<void (__fastcall*)(int)>(0x49A510)(0);
}

bool RequestNativeSourceSession(DWORD id) {
    if (!LoadLoginSources() || g_loginSourcePending) return false;
    if (id == 0) {
        g_logoutToAccountLogin = false;
        g_loginSourcePending = true;
        // Defer root destruction until the native button dispatcher unwinds.
        PostMessageW(g_window, kSourceLogoutMessage, 0, 0);
        return true;
    }
    const LoginSourceRow* selected{};
    for (unsigned i = 0; i < g_loginSourceCount; ++i)
        if (g_loginSources[i].id == id) selected = &g_loginSources[i];
    if (!selected || !ClearSourceCredentials()) return false;
    CloseSourceConnection();
    g_loginSourceSelected = id;
    LiToSourceSession[3] = id; LiToSourceSession[4] = selected->port;
    ++LiToSourceSession[5];
    ShowNativeSourceLogin();
    const int connected = reinterpret_cast<int (__fastcall*)(HWND, const char*, int)>(0x4C62A0)(
        g_window, g_loginHost, selected->port);
    Log("source selected in existing window id=%lu port=%lu connect=%d; waiting physical login", id, selected->port, connected);
    return connected != 0;
}

bool RequestNativeAccountLogin() {
    if (!LoadLoginSources() || g_loginSourceSelected <= 0 || g_loginSourcePending) return false;
    g_logoutToAccountLogin = true;
    g_loginSourcePending = true;
    PostMessageW(g_window, kSourceLogoutMessage, 0, 0);
    return true;
}

void __fastcall ChoosePreLoginSource(int accepted, void*) {
    if (accepted != 1) { RequestNativeSourceSession(0); return; }
    const DWORD index = *reinterpret_cast<DWORD*>(0x75F6A0);
    if (index < g_loginSourceCount) RequestNativeSourceSession(g_loginSources[index].id);
}

void __cdecl NativeLoginPageBuilt() {
    reinterpret_cast<void (__cdecl*)()>(0x4963E0)();
    if (!LoadLoginSources()) return;
    const DWORD root = *reinterpret_cast<DWORD*>(0x75F6E8);
    if (!root || *reinterpret_cast<DWORD*>(root + 12) != 0
        || *reinterpret_cast<DWORD*>(root + 4) != 5) return;
    if (!g_credentialContainersSaved) {
        memcpy(g_accountContainers, reinterpret_cast<void*>(0x641348), sizeof(g_accountContainers));
        memcpy(g_passwordContainers, reinterpret_cast<void*>(0x6412A0), sizeof(g_passwordContainers));
        g_credentialContainersSaved = true;
    }
    if (!ClearSourceCredentials()) {
        reinterpret_cast<void (__thiscall*)(void*, int)>(0x41B720)(reinterpret_cast<void*>(root), 1);
        Log("source login bridge unavailable; login page kept disabled"); return;
    }
    const auto slots = *reinterpret_cast<DWORD**>(root);
    for (unsigned i = 0; i < 2; ++i)
        reinterpret_cast<void (__thiscall*)(void*, const char*)>(0x440E90)(reinterpret_cast<void*>(slots[i]), "");
    if (g_loginSourceSelected != 0) return;
    CloseSourceConnection();
    auto& rows = *reinterpret_cast<BYTE**>(0x75F758);
    if (rows) reinterpret_cast<void (__cdecl*)(void*)>(0x4F103C)(rows);
    rows = static_cast<BYTE*>(reinterpret_cast<void* (__cdecl*)(size_t)>(0x4F118C)(g_loginSourceCount * 72));
    if (!rows) return;
    memset(rows, 0, g_loginSourceCount * 72);
    for (unsigned i = 0; i < g_loginSourceCount; ++i) memcpy(rows + i*72, g_loginSources[i].name, 64);
    *reinterpret_cast<BYTE*>(0x75F6E4) = static_cast<BYTE>(g_loginSourceCount);
    *reinterpret_cast<DWORD*>(0x75F6A0) = 0;
    reinterpret_cast<void (__thiscall*)(void*, int)>(0x41B720)(reinterpret_cast<void*>(root), 1);
    reinterpret_cast<void (__thiscall*)(void*)>(0x41B6E0)(reinterpret_cast<void*>(root));
    reinterpret_cast<void (__fastcall*)(DWORD, bool)>(0x498F80)(reinterpret_cast<DWORD>(&ChoosePreLoginSource), true);
    Log("native pre-login source selector opened count=%u same-process=1", g_loginSourceCount);
}

void LogoutToNativeSources() {
    if (!g_loginSourcePending || !LoadLoginSources()) return;
    const DWORD retainedSource = g_logoutToAccountLogin ? static_cast<DWORD>(g_loginSourceSelected) : 0;
    CloseSourceConnection();
    if (!ClearSourceCredentials()) return;
    g_loginSourceSelected = retainedSource; LiToSourceSession[3] = retainedSource;
    if (!retainedSource) LiToSourceSession[4] = 0;
    ++LiToSourceSession[5];
    *reinterpret_cast<DWORD*>(0x64AF98) = 0;
    *reinterpret_cast<DWORD*>(0x64AFB8) = 0;
    *reinterpret_cast<DWORD*>(0x75F750) = 0;
    DWORD depth = *reinterpret_cast<DWORD*>(0x75F738);
    if (depth > 19) { Log("source logout refused invalid native page stack"); return; }
    for (unsigned i = 0; i <= depth && *reinterpret_cast<DWORD*>(0x75F6E8); ++i)
        reinterpret_cast<bool (__fastcall*)(bool)>(0x495280)(false);
    if (*reinterpret_cast<DWORD*>(0x75F6E8) || *reinterpret_cast<DWORD*>(0x75F738)) {
        Log("source logout native page stack not empty"); return;
    }
    g_loginSourcePending = false;
    g_logoutToAccountLogin = false;
    reinterpret_cast<void (__fastcall*)(bool)>(0x4966A0)(true);
    if (retainedSource) RequestNativeSourceSession(retainedSource);
    Log("account logged out; original process/window retained; destination=%s", retainedSource ? "blank-account-login" : "sources");
}

bool MatchesAuthenticatedSource(const char* name, unsigned count, DWORD callback) {
    if (!name || g_loginSourceSelected <= 0 || count != 1 || callback != 0x0048F580) return false;
    for (unsigned i = 0; i < g_loginSourceCount; ++i)
        if (g_loginSources[i].id == static_cast<DWORD>(g_loginSourceSelected)
            && strncmp(name, g_loginSources[i].name, 64) == 0) return true;
    return false;
}

void __fastcall SelectAuthenticatedSource(DWORD callback, bool visible) {
    if (LoadLoginSources()) {
        if (MatchesAuthenticatedSource(*reinterpret_cast<const char**>(0x0075F758),
                *reinterpret_cast<BYTE*>(0x0075F6E4), callback)) {
            *reinterpret_cast<DWORD*>(0x0075F6A0) = 0;
            reinterpret_cast<void (__fastcall*)(int)>(0x0048F580)(1);
            Log("authenticated source retained source=%d; proceeding to characters", g_loginSourceSelected);
            return;
        }
        Log("authenticated source directory mismatch; closing session");
        PostMessageW(g_window, WM_CLOSE, 0, 0);
        return;
    }
    reinterpret_cast<void (__fastcall*)(DWORD, bool)>(0x00498F80)(callback, visible);
}
