#ifndef ITEM_ADDRESS
#define ITEM_ADDRESS(a) (a)
#endif
#include "item_details.h"
#include <map>
std::map<DWORD,LiToDetail> g_litoItemDetails;
SRWLOCK g_litoItemDetailsLock=SRWLOCK_INIT;
DWORD g_litoHoverItem{};
ULONGLONG g_litoHoverTime{};
int g_litoDetailScroll{};
volatile LONG g_litoItemDetailsInstalled{};
// Bounds checked before any allocation; packet text never reaches native 512-byte decoding.
size_t LiToDetailPacketLength(const char* text) {
 __try { return strnlen_s(text,16431); }
 __except(EXCEPTION_EXECUTE_HANDLER) {return 16431;}
}
bool ReceiveLiToLiveStats(const char*,size_t);
void __fastcall ReceiveLiToItemMessage(const BYTE* packet) {
 if(packet[1]==254 && std::memcmp(packet+2,"LITOSTAT1:",10)==0) {
  const char* text=reinterpret_cast<const char*>(packet+2);
  ReceiveLiToLiveStats(text,LiToDetailPacketLength(text));return;
 }
 if(packet[1]==254 && std::memcmp(packet+2,"LITOITEM1:",10)==0) {
  const char* text=reinterpret_cast<const char*>(packet+2);
  LiToDetail detail;
  if(ParseLiToDetail(text,LiToDetailPacketLength(text),detail)) {
   AcquireSRWLockExclusive(&g_litoItemDetailsLock);
   if(detail.text.empty())g_litoItemDetails.erase(detail.id);
   else if(g_litoItemDetails.size()<1024 || g_litoItemDetails.count(detail.id))g_litoItemDetails[detail.id]=std::move(detail);
   ReleaseSRWLockExclusive(&g_litoItemDetailsLock);
  } else Log("LiTo item detail rejected invalid v1 payload");
  return;
 }
 reinterpret_cast<void (__fastcall*)(const BYTE*)>(ITEM_ADDRESS(0x004A4190))(packet);
}
bool __cdecl DrawLiToItemDetail(void* object,int x,int y) {
 if(!object || !reinterpret_cast<BYTE*>(object)[0xC])return false;
 const DWORD id=*reinterpret_cast<DWORD*>(reinterpret_cast<BYTE*>(object)+4);
 AcquireSRWLockShared(&g_litoItemDetailsLock);
 auto found=g_litoItemDetails.find(id);
 std::vector<std::string> lines=found==g_litoItemDetails.end()?std::vector<std::string>{}:found->second.lines;
 const size_t captionLines=found==g_litoItemDetails.end()?0:found->second.captionLines;
 ReleaseSRWLockShared(&g_litoItemDetailsLock);
 if(lines.empty())return false;
 // The native name already resolves $resource tokens and localized equipment markers.
 // Keep that caption instead of printing server resource IDs literally.
 const char* nativeName=*reinterpret_cast<const char**>(reinterpret_cast<BYTE*>(object)+0x10);
 if(nativeName && captionLines && captionLines<=lines.size()) {
  size_t length=strnlen_s(nativeName,512);
  if(length && length<512) {
   std::vector<std::string> caption;std::string line;
   for(size_t i=0;i<length;) {
    size_t size=static_cast<unsigned char>(nativeName[i])>=0x81?2:1;
    if(i+size>length)break;
    if(line.size()+size>22){caption.push_back(line);line.clear();}
    line.append(nativeName+i,size);i+=size;
   }
   if(!line.empty())caption.push_back(line);
   lines.erase(lines.begin(),lines.begin()+captionLines);lines.insert(lines.begin(),caption.begin(),caption.end());
  }
 }
 const auto now=GetTickCount64();
 if(g_litoHoverItem!=id || now-g_litoHoverTime>250)g_litoDetailScroll=0;
 g_litoHoverItem=id;g_litoHoverTime=now;
 constexpr int rows=35,width=142; // Native LiTo inventory tooltip width (0x8e).
 g_litoDetailScroll=std::clamp(g_litoDetailScroll,0,std::max(0,int(lines.size())-rows));
 const int count=std::min(rows,int(lines.size())-g_litoDetailScroll);

 const int height=count*12+10;
 x=std::clamp(x,3,640-width-3);y=std::clamp(y,3,480-height-3);
 void* surface=*reinterpret_cast<void**>(ITEM_ADDRESS(0x006476D0));
 if(!surface)return false;
 auto fill=reinterpret_cast<void (__fastcall*)(void*,int,int,int,int)>(ITEM_ADDRESS(0x004BB4E0));
 auto edge=reinterpret_cast<void (__fastcall*)(void*,int,int,int,int,int)>(ITEM_ADDRESS(0x004B9700));
 auto draw=reinterpret_cast<void (__fastcall*)(void*,const char*,int,int,int,int,int)>(ITEM_ADDRESS(0x00431030));
 auto drawColor=reinterpret_cast<void (__fastcall*)(void*,const char*,int,int,int,int)>(ITEM_ADDRESS(0x004321A0));
 fill(surface,x,y,width,height);
 const int light=*reinterpret_cast<int*>(ITEM_ADDRESS(0x0050345C));
 const int dark=*reinterpret_cast<int*>(ITEM_ADDRESS(0x00503464));
 // Exact native two-pixel bevel: upper/left light, lower/right dark.
 edge(surface,x,y,width-1,1,light);edge(surface,x+1,y+1,width-2,1,light);
 edge(surface,x,y+1,1,height-1,light);edge(surface,x+1,y+2,1,height-2,light);
 edge(surface,x+1,y+height-1,width-1,1,dark);edge(surface,x+2,y+height-2,width-2,1,dark);
 edge(surface,x+width-1,y,1,height-1,dark);edge(surface,x+width-2,y+1,1,height-2,dark);
 int colorIndex=reinterpret_cast<BYTE*>(object)[0x78]?*reinterpret_cast<int*>(reinterpret_cast<BYTE*>(object)+0x70):3;
 if(colorIndex<0 || colorIndex>255)colorIndex=3;
 const int color=*reinterpret_cast<short*>(ITEM_ADDRESS(0x00834F34)+colorIndex*2);
 for(int i=0;i<count;++i){const auto& line=lines[g_litoDetailScroll+i];if(line.find("\\f")!=std::string::npos)draw(surface,line.c_str(),int(line.size()),x+5,y+5+i*12,color,0);else if(g_litoDetailScroll+i==0)draw(surface,line.c_str(),int(line.size()),x+5,y+5+i*12,color,1);else drawColor(surface,line.c_str(),int(line.size()),x+5,y+5+i*12,color);}
 return true;
}
bool ScrollLiToItemDetail(WPARAM value) {
 if(GetTickCount64()-g_litoHoverTime>200 || !g_litoHoverItem)return false;
 g_litoDetailScroll-=GET_WHEEL_DELTA_WPARAM(value)/WHEEL_DELTA*3;
 return true;
}
void __declspec(naked) LiToItemGridBridge() {
 __asm {
  pushfd
  pushad
  push ebx
  push dword ptr [ebp-20h]
  push eax
  call DrawLiToItemDetail
  add esp,12
  test al,al
  jz legacy
  popad
  popfd
  push 00449B14h
  ret
 legacy:
  popad
  popfd
  mov ecx,dword ptr [eax+18h]
  test ecx,ecx
  push 004496BDh
  ret
 }
}
void __declspec(naked) LiToItemFixedBridge() {
 __asm {
  pushfd
  pushad
  push ebx
  push 30h
  push eax
  call DrawLiToItemDetail
  add esp,12
  test al,al
  jz legacy
  popad
  popfd
  push 0044A0B5h
  ret
 legacy:
  popad
  popfd
  mov ecx,dword ptr [eax+18h]
  test ecx,ecx
  push 00449CC6h
  ret
 }
}
void __declspec(naked) LiToItemEquipmentBridge() {
 __asm {
  pushfd
  pushad
  push edi
  push ebx
  push ecx
  call DrawLiToItemDetail
  add esp,12
  test al,al
  jz legacy
  popad
  popfd
  push 004D96BAh
  ret
 legacy:
  popad
  popfd
  mov eax,dword ptr [ecx+14h]
  mov ecx,dword ptr [ecx+18h]
  push 004D931Eh
  ret
 }
}
bool EnsureLiToItemDetailExtension() {
 if(InterlockedCompareExchange(&g_litoItemDetailsInstalled,0,0))return true;
 if(!IsVerifiedLiToClient())return false;
 std::vector<NumericPatch> p;
 AddNumericRelativePatch(p,0x004B2AF9,{0xE8,0x92,0x16,0xFF,0xFF},0xE8,reinterpret_cast<void*>(&ReceiveLiToItemMessage),"item-detail-v1-receiver");
 AddNumericRelativePatch(p,0x004496B8,{0x8B,0x48,0x18,0x85,0xC9},0xE9,reinterpret_cast<void*>(&LiToItemGridBridge),"item-detail-grid");
 AddNumericRelativePatch(p,0x00449CC1,{0x8B,0x48,0x18,0x85,0xC9},0xE9,reinterpret_cast<void*>(&LiToItemFixedBridge),"item-detail-fixed-grid");
 AddNumericRelativePatch(p,0x004D9318,{0x8B,0x41,0x14,0x8B,0x49,0x18},0xE9,reinterpret_cast<void*>(&LiToItemEquipmentBridge),"item-detail-equipment");
 if(!CommitNumericPatches(p))return false;
 InterlockedExchange(&g_litoItemDetailsInstalled,1);
 Log("LiTo item detail v1 installed: native 142px bevel, item palette, color renderer, wide values, scroll");return true;
}
