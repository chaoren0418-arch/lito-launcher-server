#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <cassert>
#include <cstdio>
#include <cstring>
#include <cstdint>
#include <string>
#include <vector>
#include <initializer_list>
bool IsVerifiedLiToClient(){return true;}
bool IsVerifiedTournamentClient(){return true;}
void Log(const char*,...){}
struct NumericPatch {};
void AddNumericRelativePatch(std::vector<NumericPatch>&,std::uintptr_t,std::initializer_list<BYTE>,BYTE,const void*,const char*){}
void AddNumericPatch(std::vector<NumericPatch>&,std::uintptr_t,std::initializer_list<BYTE>,std::initializer_list<BYTE>,const char*){}
bool CommitNumericPatches(std::vector<NumericPatch>&){return true;}
DWORD clockTestBase;
#define CLOCK_ADDRESS(address) (clockTestBase + (address) - 0x00450000)
#include "battle_clock_resources.inc"
#include "battle_clock.inc"
int draws{},blits{};std::string glyphs;
void __cdecl NativeDraw(){++draws;}
void __cdecl Blit(void* pixel,void*,int x,int y){++blits;assert(x>=0&&x<640&&(y==15||y==40||y==55));int index=static_cast<int>(reinterpret_cast<std::uintptr_t>(pixel));if(index!=0x12F2)glyphs+=index==0x12FE?':':static_cast<char>('0'+index-0x12F4);}
void jump(DWORD addr,void* fn){auto p=reinterpret_cast<BYTE*>(addr);p[0]=0xe9;*reinterpret_cast<DWORD*>(p+1)=reinterpret_cast<DWORD>(fn)-addr-5;}
int main(){
 clockTestBase=reinterpret_cast<DWORD>(VirtualAlloc(nullptr,0x400000,MEM_RESERVE|MEM_COMMIT,PAGE_EXECUTE_READWRITE));assert(clockTestBase);
 *reinterpret_cast<BYTE*>(CLOCK_ADDRESS(0x004A98A0))=0xc3;*reinterpret_cast<BYTE*>(CLOCK_ADDRESS(0x004A9A60))=0xc3;
 jump(CLOCK_ADDRESS(0x0045F4A0),reinterpret_cast<void*>(&NativeDraw));jump(CLOCK_ADDRESS(0x004BA220),reinterpret_cast<void*>(&Blit));
 *reinterpret_cast<DWORD*>(CLOCK_ADDRESS(0x0064AF98))=4;
 std::vector<DWORD> table(0x1ec2);struct Img{DWORD vtable;DWORD pixel;} images[24]{};
 for(int i=0;i<24;i++){images[i].pixel=0x12F2+i;table[0x12F2+i]=reinterpret_cast<DWORD>(&images[i]);}
 *reinterpret_cast<DWORD*>(CLOCK_ADDRESS(0x00834BD4))=reinterpret_cast<DWORD>(table.data());*reinterpret_cast<DWORD*>(CLOCK_ADDRESS(0x006476D0))=1;
 BYTE start[]{0xd2},end[]{0xd3,0},time[]{0xe9,90,0,0,0};
 ReceiveBattleClockTime(time);DrawSharedBattleClock();assert(draws==0);
 ReceiveBattleClockStart(start);DrawSharedBattleClock();assert(draws==0);
 ReceiveBattleClockTime(time);DrawSharedBattleClock();assert(draws==1);
 assert(*reinterpret_cast<WORD*>(CLOCK_ADDRESS(0x0064AF78))==1&&*reinterpret_cast<WORD*>(CLOCK_ADDRESS(0x0064AF76))==30);
 *reinterpret_cast<DWORD*>(time+1)=6000;ReceiveBattleClockTime(time);DrawSharedBattleClock();assert(glyphs=="1:40:00"&&blits==7);
 DWORD modes[9]{};*reinterpret_cast<DWORD*>(CLOCK_ADDRESS(0x0064AFE0))=reinterpret_cast<DWORD>(modes);
 glyphs.clear();int previous=blits;*reinterpret_cast<DWORD*>(time+1)=90;ReceiveBattleClockTime(time);DrawSharedBattleClock();assert(glyphs=="01:30"&&blits==previous+6&&draws==1);
 *reinterpret_cast<DWORD*>(CLOCK_ADDRESS(0x0064AFE0))=0;
 *reinterpret_cast<DWORD*>(time+1)=0;ReceiveBattleClockTime(time);DrawSharedBattleClock();assert(draws==1&&blits==13);
 *reinterpret_cast<DWORD*>(time+1)=20;ReceiveBattleClockTime(time);ReceiveBattleClockEnd(end);DrawSharedBattleClock();assert(draws==1);
 ReceiveBattleClockStart(start);ReceiveBattleClockTime(time);*reinterpret_cast<DWORD*>(CLOCK_ADDRESS(0x0064AF98))=5;DrawSharedBattleClock();assert(!g_battleClock.armed&&!g_battleClock.visible);
 std::puts("PASS: actual E9/D2/D3 bridge, native MM:SS call, long-duration original glyph blits, hidden/cleared/result guards, native logical coordinates and separate Pet Race row.");
}
