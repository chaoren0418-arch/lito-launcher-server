#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <cassert>
#include <cstdio>
#include <cstdint>
#include <cstring>
#include <vector>
#include <initializer_list>
bool IsVerifiedLiToClient(){return true;}
void Log(const char*,...){}
struct NumericPatch{};
void AddNumericRelativePatch(std::vector<NumericPatch>&,std::uintptr_t,std::initializer_list<BYTE>,BYTE,const void*,const char*){}
bool CommitNumericPatches(std::vector<NumericPatch>&){return true;}
#define IDLE_ADDRESS(address) ((address) + 0x20000000)
#include "battle_window_idle.inc"
int main(){
 assert(VirtualAlloc(reinterpret_cast<void*>(IDLE_ADDRESS(0x480000)),0x10000,MEM_RESERVE|MEM_COMMIT,PAGE_EXECUTE_READWRITE));
 assert(VirtualAlloc(reinterpret_cast<void*>(IDLE_ADDRESS(0x640000)),0x10000,MEM_RESERVE|MEM_COMMIT,PAGE_READWRITE));
 // Original continuation still checks the renderer pause guard before work.
 const BYTE run[]{0xa1,0xfc,0xb8,0x64,0x20,0x85,0xc0,0x75,0x0d,0xb8,1,0,0,0,0xc3};
 const BYTE wait[]{0x31,0xc0,0xc3};
 memcpy(reinterpret_cast<void*>(IDLE_ADDRESS(0x48210c)),run,sizeof(run));
 memcpy(reinterpret_cast<void*>(IDLE_ADDRESS(0x482122)),wait,sizeof(wait));
 auto bridge=reinterpret_cast<int (__cdecl*)()>(&BattleWindowIdleBridge);
 for(int phase=0;phase<=6;++phase)for(int active=0;active<2;++active)for(int pause=0;pause<2;++pause){
  *reinterpret_cast<DWORD*>(IDLE_ADDRESS(0x64af98))=phase;
  *reinterpret_cast<BYTE*>(IDLE_ADDRESS(0x64bbe8))=static_cast<BYTE>(active);
  *reinterpret_cast<DWORD*>(IDLE_ADDRESS(0x64b8fc))=pause;
  assert(bridge()==(!pause && (active || phase==4 || phase==5)));
  assert(*reinterpret_cast<BYTE*>(IDLE_ADDRESS(0x64bbe8))==active); // no fabricated focus
 }
 puts("PASS: actual x86 idle bridge all 28 phase/focus/pause combinations; active state preserved.");
}
