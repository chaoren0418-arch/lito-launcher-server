#include <windows.h>
#include <cstdio>
#include <cstring>
#include <cassert>
static BYTE object[0x160]{};
static int started{},drawn{},logs{};
void Log(const char*,...){++logs;}
struct Native {
 void start(){assert(reinterpret_cast<void*>(this)==object);++started;}
 DWORD draw(int f,int x,int y,int flags){assert(reinterpret_cast<void*>(this)==object);assert(f==2&&x==32718&&y==32796&&flags==0);++drawn;return 0x87654321;}
};
void* Address(int a){void* out{};if(a==0x40E8F0){auto fn=&Native::start;memcpy(&out,&fn,4);}else{assert(a==0x4CD9D0);auto fn=&Native::draw;memcpy(&out,&fn,4);}return out;}
#define FLIGHT_CALL(address,type) reinterpret_cast<type>(Address(address))
#include "arrow_flight_observe.inc"
int main(){
 ObserveArrowStart(object,nullptr);
 assert(started==1&&logs==1);
 assert(ObserveArrowDraw(object,nullptr,2,32718,32796,0)==0x87654321);
 assert(drawn==1&&logs==2);
 for(int i=0;i<150;++i)ObserveArrowStart(object,nullptr);
 assert(started==151&&logs==128);
 puts("PASS: observation preserves native thiscall, draw arguments/result, exactly-once dispatch and bounded logging.");
}
