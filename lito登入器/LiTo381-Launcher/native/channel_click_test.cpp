#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <windowsx.h>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
bool ReadToolbarBytes(std::uintptr_t,void*,SIZE_T){return false;}
#include "channel_click.inc"
#define CHECK(c) do{if(!(c)){printf("FAIL %d\n",__LINE__);exit(1);}}while(0)
int main(){
 ChannelClickTransition c;
 CHECK(!c.consume(WM_LBUTTONDOWN,0,100,500,true,0));
 CHECK(!c.consume(WM_LBUTTONUP,0,110,500,false,1));
 CHECK(c.consume(WM_LBUTTONDBLCLK,0,150,500,false,1));
 CHECK(c.consume(WM_LBUTTONUP,0,160,500,false,1));
 CHECK(!c.consume(WM_LBUTTONDOWN,0,200,500,false,1));
 CHECK(!c.consume(WM_LBUTTONUP,0,210,500,false,1));
 CHECK(!c.consume(WM_LBUTTONDBLCLK,0,240,500,false,1));
 c={};CHECK(!c.consume(WM_LBUTTONDOWN,0,100,500,true,0));
 CHECK(!c.consume(WM_LBUTTONUP,0,110,500,true,0));
 CHECK(!c.consume(WM_LBUTTONDBLCLK,0,150,500,true,0));
 c={};CHECK(!c.consume(WM_LBUTTONDOWN,0,100,500,true,0));
 CHECK(!c.consume(WM_LBUTTONUP,0,110,500,false,1));
 CHECK(!c.consume(WM_LBUTTONDBLCLK,0,800,500,false,1));
 puts("PASS: delayed S_CA before/after release; consume trailing click only; normal room double-click, untransitioned selector, timeout preserved.");
}
