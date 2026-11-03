#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <cstdarg>
#include <fstream>
#include <filesystem>
#include <vector>
#define CHECK(c) do { if (!(c)) { std::printf("FAIL line=%d error=%lu\n", __LINE__, GetLastError()); std::exit(1); } } while (0)
bool verified=true;
bool IsVerifiedTournamentClient(){return verified;}
void Log(const char*,...){}
alignas(16) BYTE nativeRegistry[0x200000]{};
#define LOBBY_NATIVE_ADDRESS(address) (reinterpret_cast<std::uintptr_t>(nativeRegistry)+(address)-0x640000)
#include "lobby_return.inc"
volatile DWORD testMode{};
int called{},modeSeen{};
void __cdecl nativeReturn(){++called;modeSeen=testMode;
    *reinterpret_cast<BYTE*>(LOBBY_NATIVE_ADDRESS(0x64afbd))=1;
}
int main() {
    for(DWORD phase : {0,1,2,3,4,5}) {
        testMode=2;
        *reinterpret_cast<BYTE*>(LOBBY_NATIVE_ADDRESS(0x64afc5))=0;
        *reinterpret_cast<WORD*>(LOBBY_NATIVE_ADDRESS(0x64afbc))=0;
        *reinterpret_cast<DWORD*>(LOBBY_NATIVE_ADDRESS(0x75f6a0))=1;
        const int before=called;
        RouteLobbyReturn(phase,testMode,&nativeReturn);
        CHECK(called==before+1 && modeSeen==(phase==1?1:2));
        CHECK(*reinterpret_cast<BYTE*>(LOBBY_NATIVE_ADDRESS(0x64afc5))==(phase==1?1:0));
        CHECK(*reinterpret_cast<WORD*>(LOBBY_NATIVE_ADDRESS(0x64afbc))==(phase==1?0:256));
        CHECK(*reinterpret_cast<DWORD*>(LOBBY_NATIVE_ADDRESS(0x75f6a0))==1);
    }
    alignas(4) BYTE control[0x50]{};
    DWORD slots[5]{0,0,reinterpret_cast<DWORD>(control),0x11111111,0x22222222};
    DWORD manager[]{reinterpret_cast<DWORD>(slots),5};
    DWORD dialog[]{0x4f85f0,0,reinterpret_cast<DWORD>(manager)};
    *reinterpret_cast<DWORD*>(control)=0x4f9ad0;
    *reinterpret_cast<DWORD*>(control+0x20)=0x1d92;
    *reinterpret_cast<DWORD*>(control+0x48)=0x1d94;
    *reinterpret_cast<DWORD*>(control+0x2c)=0x455330;
    CHECK(BindLobbyReturnButton(dialog));CHECK(BindLobbyReturnButton(dialog));
    DWORD worldDialog[]{0x4fa368,0,0};
    *reinterpret_cast<void**>(LOBBY_NATIVE_ADDRESS(0x648f20))=dialog;
    *reinterpret_cast<void**>(LOBBY_NATIVE_ADDRESS(0x831564))=worldDialog;
    CHECK(EnsureLobbyReturnAction());
    CHECK(*reinterpret_cast<void**>(LOBBY_NATIVE_ADDRESS(0x831564))==worldDialog);
    CHECK(slots[3]==0x11111111 && slots[4]==0x22222222);
    CHECK(*reinterpret_cast<DWORD*>(control+0x2c)==reinterpret_cast<DWORD>(&ReturnLobbyToCharacters));
    *reinterpret_cast<DWORD*>(control+0x2c)=0x12345678;CHECK(!BindLobbyReturnButton(dialog));
    *reinterpret_cast<DWORD*>(control+0x2c)=0x455330;manager[1]=4;CHECK(!BindLobbyReturnButton(dialog));
    CHECK(!BindLobbyReturnButton(nullptr));
    std::puts("PASS: lobby-only native Return dispatch; room/world mode preserved; guarded/idempotent control binding; unknown callback rejected; Quit/Cancel slots preserved.");
}
