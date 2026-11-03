#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <cwchar>
#include <initializer_list>
DWORD LiToSourceSession[8]{};
HWND g_window{};
void Log(const char*,...){}
#include "source_login_order.inc"
#define CHECK(c) do {if(!(c)){std::printf("FAIL line=%d\n",__LINE__);std::exit(1);}} while(0)
int main() {
    SetEnvironmentVariableA("LITO_NATIVE_LOGIN_HOST","127.0.0.1");
    SetEnvironmentVariableW(L"LITO_NATIVE_SOURCES",L"1:32001:41;3:32003:42;8:32008:43");
    CHECK(LoadLoginSources());CHECK(g_loginSourceCount==3);
    CHECK(g_loginSources[2].id==8&&g_loginSources[2].port==32008);
    CHECK(!RequestNativeSourceSession(2));
    CHECK(!RequestNativeSourceSession(8)); // No credential bridge: never enable login.
    CHECK(RequestNativeSourceSession(0));CHECK(g_loginSourcePending);
    CHECK(!RequestNativeSourceSession(0));g_loginSourcePending=false;
    g_loginSourceSelected=8;
    CHECK(RequestNativeAccountLogin());CHECK(g_logoutToAccountLogin && g_loginSourcePending);
    CHECK(!RequestNativeAccountLogin());g_loginSourcePending=false;g_logoutToAccountLogin=false;
    CHECK(MatchesAuthenticatedSource("C",1,0x48F580));
    CHECK(!MatchesAuthenticatedSource("B",1,0x48F580));
    CHECK(!MatchesAuthenticatedSource("C",2,0x48F580));
    CHECK(!MatchesAuthenticatedSource(nullptr,1,0x48F580));
    CHECK(!MatchesAuthenticatedSource("C",1,0x492FE0));
    g_loginSourceSelected=0;CHECK(!MatchesAuthenticatedSource("C",1,0x48F580));
    CHECK(!RequestNativeAccountLogin());
    // Repeated or malformed configuration cannot silently alias another source.
    g_loginSourceSelected=-1;g_loginSourceCount=0;
    SetEnvironmentVariableW(L"LITO_NATIVE_SOURCES",L"1:32001:41;1:32002:42");CHECK(!LoadLoginSources());
    g_loginSourceSelected=-1;g_loginSourceCount=0;
    SetEnvironmentVariableW(L"LITO_NATIVE_SOURCES",L"1:32001:xx");CHECK(!LoadLoginSources());
    std::puts("PASS source IDs/ports, disabled/missing bridge refusal, deferred logout, authenticated directory qualification and malformed catalog rejection.");
}
