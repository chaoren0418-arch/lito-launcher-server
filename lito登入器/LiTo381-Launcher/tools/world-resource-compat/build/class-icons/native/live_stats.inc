#include "live_stats.h"
#ifndef LITO_LIVE_ADDRESS
#define LITO_LIVE_ADDRESS(a) (a)
#endif
bool ReceiveLiToLiveStats(const char* text,size_t length){
 LiToLiveStats s;if(!ParseLiToLiveStats(text,length,s)){Log("live stats rejected malformed payload");return false;}
 if(!LoadLoginSources()||g_loginSourceSelected<=0)return false;
 bool source=false;for(unsigned i=0;i<g_loginSourceCount;i++)if(g_loginSources[i].id==static_cast<DWORD>(g_loginSourceSelected)&&s.source==g_loginSources[i].name)source=true;
 if(!source)return false;
 auto profile=reinterpret_cast<unsigned char*>(LITO_LIVE_ADDRESS(0x64ABF8));
 const size_t nameLength=strnlen_s(reinterpret_cast<char*>(profile),28);
 if(nameLength>=28)return false;
 std::string name(reinterpret_cast<char*>(profile),nameLength);
 if(name!=s.raw&&name!=s.display)return false;
 const DWORD phase=*reinterpret_cast<DWORD*>(LITO_LIVE_ADDRESS(0x64AF98));
 if(phase!=1&&phase!=2)return false;
 for(unsigned i=0;i<4;i++){
  auto row=reinterpret_cast<unsigned char*>(LITO_LIVE_ADDRESS(0x62FAA0)+i*180);size_t n=strnlen_s(reinterpret_cast<char*>(row),28);
  if(n<28){std::string rowName(reinterpret_cast<char*>(row),n);if(rowName==s.raw||rowName==s.display)ApplyLiToLiveStats(row,nullptr,s);}
 }
 ApplyLiToLiveStats(nullptr,profile,s);
 Log("native live lobby statistics applied battles=%d wins=%d losses=%d rate=%u",s.values[0],s.values[1],s.values[2],profile[0x42]);
 return true;
}
