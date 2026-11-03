#include "live_stats.h"
#include <fstream>
#include <iostream>
#include <vector>
#include <cassert>
using DWORD=unsigned long;
unsigned char memory[0x300000]{};
#define LITO_LIVE_ADDRESS(a) (memory+(a)-0x400000)
struct TestSource {DWORD id;char name[64];};
TestSource g_loginSources[1]{};unsigned g_loginSourceCount=1;int g_loginSourceSelected=2;
bool LoadLoginSources(){return true;}void Log(const char*,...){}
#include "live_stats.inc"
int main(int argc,char**argv){
 assert(argc==2);std::ifstream f(argv[1],std::ios::binary);std::string p((std::istreambuf_iterator<char>(f)),{});assert(p.size()>3);LiToLiveStats s;
 assert(ParseLiToLiveStats(p.data()+2,p.size()-3,s));assert(s.values[0]==21&&s.values[1]==20&&s.values[2]==1&&s.values[4]==2);
 unsigned char row[180],profile[220];memset(row,0x55,sizeof row);memset(profile,0x66,sizeof profile);ApplyLiToLiveStats(row,profile,s);
 auto d=[](unsigned char*p){int x;memcpy(&x,p,4);return x;};auto w=[](unsigned char*p){uint16_t x;memcpy(&x,p,2);return x;};
 assert(d(row+0x60)==21&&d(row+0x64)==20&&d(row+0x68)==1);assert(w(profile+0x38)==21&&w(profile+0x3a)==20&&w(profile+0x3c)==1&&profile[0x42]==95);
 assert(d(profile+0x8c)==2);assert(row[0]==0x55&&row[0x37]==0x55&&profile[0]==0x66&&profile[0x40]==0x66);
 std::string text=p.substr(2,p.size()-3);for(auto bad:{text+",0",text.substr(0,text.size()-1),std::string("LITOSTAT1:zz:61:61:1,1,0,0,0,0,0,0,0,0,0,0,0,0")}){LiToLiveStats x;assert(!ParseLiToLiveStats(bad.data(),bad.size(),x));}
 LiToLiveStats high=s;high.values[0]=70000;high.values[1]=70000;high.values[2]=0;ApplyLiToLiveStats(row,profile,high);assert(d(row+0x64)==70000&&w(profile+0x3a)==65535&&profile[0x42]==100);
 std::strcpy(g_loginSources[0].name,s.source.c_str());g_loginSources[0].id=2;
 auto nativeProfile=LITO_LIVE_ADDRESS(0x64ABF8);auto nativeRow=LITO_LIVE_ADDRESS(0x62FAA0);
 std::strcpy(reinterpret_cast<char*>(nativeProfile),s.display.c_str());std::strcpy(reinterpret_cast<char*>(nativeRow),s.raw.c_str());
 *reinterpret_cast<DWORD*>(LITO_LIVE_ADDRESS(0x64AF98))=1;
 assert(ReceiveLiToLiveStats(text.data(),text.size()));assert(w(nativeProfile+0x3a)==20&&d(nativeRow+0x64)==20);
 g_loginSourceSelected=1;assert(!ReceiveLiToLiveStats(text.data(),text.size()));g_loginSourceSelected=2;
 *reinterpret_cast<DWORD*>(LITO_LIVE_ADDRESS(0x64AF98))=4;assert(!ReceiveLiToLiveStats(text.data(),text.size()));
 *reinterpret_cast<DWORD*>(LITO_LIVE_ADDRESS(0x64AF98))=1;std::strcpy(reinterpret_cast<char*>(nativeProfile),"other");assert(!ReceiveLiToLiveStats(text.data(),text.size()));
 std::cout<<"PASS actual server packet -> native field mapping; full directory counts; bounded native words; no names/stats overwritten; malformed rejection\n";
}
