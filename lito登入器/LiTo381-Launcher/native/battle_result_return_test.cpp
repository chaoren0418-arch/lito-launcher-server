#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <cassert>
#include <cstdio>
#include <cstdint>
#include <vector>
#include <initializer_list>
DWORD memory;
#define RESULT_ADDRESS(address) (memory + (address) - 0x00450000)
bool IsVerifiedLiToClient(){return true;}
bool IsVerifiedTournamentClient(){return true;}
void Log(const char*,...){}
volatile LONG g_resultSummaryPending{};
int sent{},calls{};
bool SendTournamentOpcode(BYTE op){assert(op==0xD0);++sent;return true;}
struct NumericPatch{};
void AddNumericRelativePatch(std::vector<NumericPatch>&,std::uintptr_t,std::initializer_list<BYTE>,BYTE,const void*,const char*){}
bool CommitNumericPatches(std::vector<NumericPatch>&){return true;}
#include "battle_result_return.inc"
void __fastcall NativeReturn(const BYTE* packet){assert(*packet==0xf0);++calls;*reinterpret_cast<DWORD*>(RESULT_ADDRESS(0x64af98))=2;}
int main(){
 memory=reinterpret_cast<DWORD>(VirtualAlloc(nullptr,0x300000,MEM_RESERVE|MEM_COMMIT,PAGE_EXECUTE_READWRITE));assert(memory);
 auto fn=reinterpret_cast<BYTE*>(RESULT_ADDRESS(0x4a9770));fn[0]=0xe9;*reinterpret_cast<DWORD*>(fn+1)=reinterpret_cast<DWORD>(&NativeReturn)-reinterpret_cast<DWORD>(fn)-5;
 BYTE a[32]{},b[32]{},packet[]{0xf0,0};
 *reinterpret_cast<DWORD*>(RESULT_ADDRESS(0x64a910))=reinterpret_cast<DWORD>(a);
 *reinterpret_cast<DWORD*>(RESULT_ADDRESS(0x64a914))=reinterpret_cast<DWORD>(b);
 auto phase=reinterpret_cast<DWORD*>(RESULT_ADDRESS(0x64af98));
 for(int i=0;i<7;++i){
  *phase=5;a[12]=b[28]=0xff;
  *reinterpret_cast<BYTE*>(RESULT_ADDRESS(0x648f1c))=0;
  *reinterpret_cast<BYTE*>(RESULT_ADDRESS(0x648f08))=0;
  if(i==1)a[12]=0; // winner/loser summary waits for actual Confirm
  if(i==2)b[28]=1;
  if(i==3)*phase=2; // ordinary room F0
  if(i==4)*phase=1; // login/transfer F0
  if(i==5)*reinterpret_cast<BYTE*>(RESULT_ADDRESS(0x648f1c))=1;
  if(i==6)*reinterpret_cast<BYTE*>(RESULT_ADDRESS(0x648f08))=1;
  int before=sent;g_resultSummaryPending=1;
  ReceiveResultReturn(packet);
  assert(sent==before+(i==0));assert(calls==i+1);
  if(i==0)assert(g_resultSummaryPending==0);
 }
 puts("PASS: no-winner F0 completes D0 once; normal summary, room, login, observer and argo retain native routing.");
}
