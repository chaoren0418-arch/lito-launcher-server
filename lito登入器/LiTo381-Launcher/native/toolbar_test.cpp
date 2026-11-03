#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <windowsx.h>
#include <cstdint>
#include <cstring>
#include <cstdio>
#include <cstdlib>
#include "toolbar_rect.inc"
void check(bool pass,const char* text){if(!pass){std::printf("FAIL %s\n",text);std::exit(1);}}
int main(){
    DWORD slots[8]{};DWORD root[2]{reinterpret_cast<DWORD>(slots),8};
    BYTE exitWidget[0x28]{},settings[0x28]{};
    auto fill=[](BYTE* b,RECT rect,DWORD resource,DWORD callback){DWORD vt=0x4F7B60;memcpy(b,&vt,4);memcpy(b+4,&rect,16);b[0x14]=1;memcpy(b+0x20,&resource,4);memcpy(b+0x24,&callback,4);};
    fill(exitWidget,{588,437,607,458},404,0x46B1A0);fill(settings,{567,437,586,458},392,0x47B8F0);
    slots[4]=reinterpret_cast<DWORD>(exitWidget);slots[6]=reinterpret_cast<DWORD>(settings);
    RECT exitRect{},settingsRect{};
    check(ReadTournamentToolbarRect(reinterpret_cast<DWORD>(root),true,exitRect),"captured native exit");
    check(ReadTournamentToolbarRect(reinterpret_cast<DWORD>(root),false,settingsRect),"captured native settings");
    for(int y=0;y<480;y++)for(int x=0;x<640;x++){
        bool expected=x>=588&&x<607&&y>=437&&y<458;
        check(TournamentToolbarRectContains(exitRect,MAKELPARAM(x,y))==expected,"full canvas exact exit coverage");
        check(!(TournamentToolbarRectContains(exitRect,MAKELPARAM(x,y))&&TournamentToolbarRectContains(settingsRect,MAKELPARAM(x,y))),"no settings overlap");
    }
    check(!TournamentToolbarRectContains(exitRect,MAKELPARAM(630,470)),"old bottom right ghost hit rejected");
    fill(exitWidget,{580,420,599,441},404,0x46B1A0);
    check(ReadTournamentToolbarRect(reinterpret_cast<DWORD>(root),true,exitRect)&&exitRect.left==580,"follows actual control position");
    exitWidget[0x14]=0;check(!ReadTournamentToolbarRect(reinterpret_cast<DWORD>(root),true,exitRect),"hidden control rejected");
    fill(exitWidget,{588,437,737,502},404,0x46B1A0);check(!ReadTournamentToolbarRect(reinterpret_cast<DWORD>(root),true,exitRect),"oversized tooltip alias rejected");
    check(!ReadTournamentToolbarRect(0,true,exitRect),"absent toolbar rejected");
    std::puts("PASS: native captured rectangles, every canvas pixel, no adjacent overlap, no old ghost hit, relocated control, hidden/alias/null guards.");
}
