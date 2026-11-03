#define WIN32_LEAN_AND_MEAN
#define NOMINMAX
#include <windows.h>
#include <windowsx.h>
#include <cstdio>
#include <cstring>
#include <cstdint>
#include <cassert>
#include <map>
#include "item_details.h"
void Log(const char*,...){}
bool ReceiveLiToLiveStats(const char*,size_t){return false;}
bool IsVerifiedLiToClient(){return true;}
struct NumericPatch{};
void AddNumericRelativePatch(std::vector<NumericPatch>&,uintptr_t,std::initializer_list<BYTE>,BYTE,void*,const char*){}
bool CommitNumericPatches(std::vector<NumericPatch>&){return true;}
DWORD itemTestBase;
#define ITEM_ADDRESS(a) (itemTestBase+(a)-0x00400000)
#include "item_details.inc"
void* volatile keepBridges[]={ (void*)&LiToItemGridBridge,(void*)&LiToItemFixedBridge,(void*)&LiToItemEquipmentBridge };
std::vector<std::string> drawn;int fills{},edges{},expectedColor=321,colored{};
void __fastcall Fill(void* surface,int x,int y,int w,int h){assert(surface==(void*)0x1234);assert(w==142&&x>=0&&y>=0&&x+w<=640&&y+h<=480);++fills;}
void __fastcall Edge(void* surface,int x,int y,int w,int h,int color){assert(surface==(void*)0x1234);assert(x>=0&&y>=0&&x+w<=640&&y+h<=480);assert(color==(edges%8<4?99:100));++edges;}
void __fastcall Text(void* surface,const char* text,int length,int x,int y,int color,int last){assert(surface==(void*)0x1234);assert(length<=64&&x>=0&&y>=0&&x+length*6<=640&&y+12<=480);assert(color==expectedColor);if(std::string(text,length).find("\\f")!=std::string::npos){assert(last==0);++colored;}else assert(last==1);drawn.emplace_back(text,length);}
void __fastcall ColorText(void* surface,const char* text,int length,int x,int y,int color){Text(surface,text,length,x,y,color,1);}
void jump(uintptr_t address,void* to){BYTE b[5]={0xe9};int d=int(uintptr_t(to)-address-5);memcpy(b+1,&d,4);memcpy((void*)address,b,5);}
int main(){assert(keepBridges[0] && keepBridges[1] && keepBridges[2]);
 itemTestBase=(DWORD)VirtualAlloc(nullptr,0x500000,MEM_RESERVE|MEM_COMMIT,PAGE_EXECUTE_READWRITE);assert(itemTestBase);
 *(void**)ITEM_ADDRESS(0x6476d0)=(void*)0x1234;*(int*)ITEM_ADDRESS(0x50345c)=99;*(int*)ITEM_ADDRESS(0x503464)=100;*(short*)(ITEM_ADDRESS(0x834f34)+6)=321;*(short*)(ITEM_ADDRESS(0x834f34)+34)=456;
 jump(ITEM_ADDRESS(0x4bb4e0),(void*)&Fill);jump(ITEM_ADDRESS(0x4b9700),(void*)&Edge);jump(ITEM_ADDRESS(0x431030),(void*)&Text);jump(ITEM_ADDRESS(0x4321a0),(void*)&ColorText);
 BYTE object[0x84]{};*(DWORD*)(object+4)=0x1234;object[0xc]=1;
 LiToDetail detail;detail.id=0x1234;for(int i=0;i<63;i++)detail.lines.push_back("row "+std::to_string(i));detail.text="fixture";g_litoItemDetails[detail.id]=detail;
 BYTE before[sizeof(object)];memcpy(before,object,sizeof(object));
 assert(DrawLiToItemDetail(object,630,470));assert(fills==1&&edges==8&&drawn.size()==35);assert(drawn.front()=="row 0");
 ScrollLiToItemDetail(MAKEWPARAM(0,short(-120*10)));drawn.clear();assert(DrawLiToItemDetail(object,-500,-500));assert(drawn.front()=="row 28");assert(memcmp(before,object,sizeof(object))==0);
 object[0x78]=1;*(int*)(object+0x70)=17;expectedColor=456;g_litoItemDetails[detail.id].lines[28]="\\fRcustom";drawn.clear();assert(DrawLiToItemDetail(object,10,10));assert(colored==1&&drawn.front()=="\\fRcustom");
 object[0xc]=0;assert(!DrawLiToItemDetail(object,0,0));object[0xc]=1;g_litoItemDetails.clear();assert(!DrawLiToItemDetail(object,0,0));
 puts("PASS: native fastcall font/background ABI, 63 rows, scrolling, all edges inside 640x480, no item-object writes, unidentified/missing fallback");
}
