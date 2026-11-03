#include <windows.h>
#include <cstdio>
#include <cstring>
#include <cassert>
static int calls=0, logs=0;
static unsigned char object[0x160]{};
void Log(const char*,...) { ++logs; }
struct NativeCtor {
 void* construct(DWORD seq,int gfx,int flight,int x,int y,int tx,int ty,int heading,DWORD target,int damage,int extra,void* text) {
  assert(reinterpret_cast<void*>(this)==object);
  assert(seq==4500013 && gfx==66 && flight==127 && x==65478 && y==32801 && tx==65482 && ty==32802);
  assert(heading==3 && target==2 && damage==11 && extra==0 && text==reinterpret_cast<void*>(0x12345678));
  ++calls; WORD g=66, f=9;memcpy(object+0x14,&g,2);memcpy(object+0x1A,&f,2);return object;
 }
};
void* ConstructorAddress() { auto method=&NativeCtor::construct;void* result{};static_assert(sizeof method==sizeof result);memcpy(&result,&method,sizeof result);return result; }
#define ARROW_CALL(address,type) reinterpret_cast<type>(ConstructorAddress())
#include "arrow_observe.inc"
int main() {
 auto result=ObserveNativeArrow(object,nullptr,4500013,66,127,65478,32801,65482,32802,3,2,11,0,reinterpret_cast<void*>(0x12345678));
 assert(result==object && calls==1 && logs==1);
 std::puts("PASS: native arrow thiscall receiver, all twelve arguments and return object preserved; observer does not remap sprite.");
}
