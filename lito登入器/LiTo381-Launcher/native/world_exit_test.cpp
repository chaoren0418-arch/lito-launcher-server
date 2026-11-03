#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
bool IsVerifiedTournamentClient(){return false;}
bool ReadToolbarBytes(std::uintptr_t,void*,SIZE_T){return false;}
bool SendTournamentOpcode(BYTE){return false;}
void Log(const char*,...){}
#include "world_exit.inc"
#define CHECK(c) do {if(!(c)){std::printf("FAIL line=%d\n",__LINE__);std::exit(1);}} while(0)
int main(){
    CHECK(IsWorldExitPhase(3)&&IsWorldExitPhase(4)&&IsWorldExitPhase(5));
    CHECK(!IsWorldExitPhase(0)&&!IsWorldExitPhase(1)&&!IsWorldExitPhase(2)&&!IsWorldExitPhase(6));
    CHECK(NativeWorldExitActionAllowed(0));CHECK(NativeWorldExitActionAllowed(0x400));
    CHECK(!NativeWorldExitActionAllowed(1));CHECK(!NativeWorldExitActionAllowed(0x200));CHECK(!NativeWorldExitActionAllowed(0x401));
    alignas(4) BYTE b[0x50]{};
    DWORD slots[]{0,0,reinterpret_cast<DWORD>(b),0x12345678};
    DWORD manager[]{reinterpret_cast<DWORD>(slots),4};
    DWORD dialog[]{0x4fa368,0,reinterpret_cast<DWORD>(manager)};
    *reinterpret_cast<DWORD*>(b)=0x4f9ad0;
    *reinterpret_cast<DWORD*>(b+0x20)=0x429;
    *reinterpret_cast<DWORD*>(b+0x48)=0x4ff;
    *reinterpret_cast<DWORD*>(b+0x2c)=0x4c22a0;
    CHECK(BindWorldExitButton(dialog));CHECK(BindWorldExitButton(dialog));
    CHECK(slots[3]==0x12345678);
    *reinterpret_cast<DWORD*>(b+0x2c)=0x455330;CHECK(!BindWorldExitButton(dialog));
    dialog[0]=0x4f85f0;CHECK(!BindWorldExitButton(dialog));
    std::puts("PASS: original native action gate; world confirmation binding; cancel preserved; lobby/unknown callback rejected.");
}
