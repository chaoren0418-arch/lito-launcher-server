// Compile the SAME candidate translation unit and execute its actual class/
// status installation transaction against mapped retained native image data.
// No game process, network, DirectDraw initialization or user input is created.
#include "ddraw_proxy.cpp"
#include "shared_doll_wire_fixture.h"
#include "shared_frame_fixture.h"
#include <fstream>
#include <iterator>
#include <iostream>
static unsigned installChecks;
static int fixtureRateStage,fixtureRateCase;
static LONG WINAPI FixtureException(EXCEPTION_POINTERS* error){
    std::printf("FAIL native exception=%08lX address=%p rate-stage=%d case=%d ecx=%08lX edx=%08lX\n",
        error->ExceptionRecord->ExceptionCode,error->ExceptionRecord->ExceptionAddress,
        fixtureRateStage,fixtureRateCase,error->ContextRecord->Ecx,error->ContextRecord->Edx);
    std::printf("eax=%08lX ebx=%08lX esi=%08lX edi=%08lX ebp=%08lX esp=%08lX combo=%08lX\n",
        error->ContextRecord->Eax,error->ContextRecord->Ebx,error->ContextRecord->Esi,
        error->ContextRecord->Edi,error->ContextRecord->Ebp,error->ContextRecord->Esp,g_comboDurationContinuation);
    auto stack=reinterpret_cast<const DWORD*>(error->ContextRecord->Esp);
    for(unsigned i=0;i<8;++i)std::printf("%08lX ",stack[i]);
    std::printf("\n");
    return EXCEPTION_EXECUTE_HANDLER;
}
static int capturedSeconds, capturedSlot, capturedFallback;
static unsigned capturedCalls;
struct FixtureEvent {unsigned at;void* callback;unsigned slot;};
static std::vector<FixtureEvent> fixtureEvents;
static unsigned fixtureNow,fixtureMessages;
static BYTE fixtureActor[0x100]{},fixturePeer[0x100]{};
static void* __fastcall FixtureActor(DWORD id){
    return (id==1 || id==0x12345678)?fixtureActor:(id==2?fixturePeer:nullptr);
}
static void __fastcall FixtureSpeedUi(void*,void*,int){}
static bool fixtureUi[96]{},fixtureVisible[96]{};
static int fixtureUiSeconds[96]{};
static unsigned nativeTimerStorage[256*3+2]{};
static DWORD orderNodes[96][5]{};
static BYTE orderUi[96][0x58]{};
static unsigned orderCount,orderCursor,orderDraws;
static void* __fastcall OrderFirst(void*,void*){orderCursor=0;return orderCount?orderNodes[0]:nullptr;}
static void* __fastcall OrderNext(void*,void*){return ++orderCursor<orderCount?orderNodes[orderCursor]:nullptr;}
static void __fastcall OrderDraw(void*,void*){++orderDraws;}
static unsigned __cdecl FixtureClock(){return fixtureNow;}
static void __cdecl FixturePump(){}
__declspec(naked) static void __cdecl FixtureResetBlock(DWORD) {
    __asm {
        pushad
        mov eax,[esp+36]
        cld
        call eax
        popad
        ret
    }
}
static void* __cdecl FixtureMove(void* to,const void* from,std::size_t size){return std::memmove(to,from,size);}
static void __fastcall FixtureSchedule(unsigned delay,void* callback,unsigned slot){
    fixtureEvents.push_back({fixtureNow+delay,callback,slot});
}
static void __fastcall FixtureCancel(void* callback,unsigned slot){
    fixtureEvents.erase(std::remove_if(fixtureEvents.begin(),fixtureEvents.end(),
        [=](const FixtureEvent& e){return e.callback==callback && e.slot==slot;}),fixtureEvents.end());
}
static void __fastcall FixtureRemove(void*,void*,unsigned slot){
    if(slot<96)fixtureUi[slot]=false;
}
static void __fastcall FixtureAdd(void*,void*,int seconds,unsigned slot){
    if(slot<96){fixtureUi[slot]=fixtureVisible[slot]=true;fixtureUiSeconds[slot]=seconds;}
}
static void __fastcall FixtureToggle(void*,void*,unsigned slot){
    if(slot<96 && fixtureUi[slot])fixtureVisible[slot]=!fixtureVisible[slot];
}
static void __fastcall FixtureGroup(void*,void*,unsigned group){
    for(unsigned slot=0;slot<96;++slot)if(g_dragonStatusRows[slot][2]==group)fixtureUi[slot]=false;
}
static void __fastcall FixtureMessage(const char*,unsigned,unsigned){++fixtureMessages;}
static bool FixtureAdvance(unsigned until){
    unsigned limit=10000;
    while(!fixtureEvents.empty() && limit--){
        auto it=std::min_element(fixtureEvents.begin(),fixtureEvents.end(),
            [](const FixtureEvent& a,const FixtureEvent& b){return a.at<b.at;});
        if(it->at>until)break;
        auto event=*it;fixtureEvents.erase(it);fixtureNow=event.at;
        reinterpret_cast<void(__fastcall*)(unsigned)>(event.callback)(event.slot);
    }
    fixtureNow=until;return limit!=0;
}
static void __fastcall CaptureStatus(int seconds,int slot) {
    ++capturedCalls;capturedSeconds=seconds;capturedSlot=slot;
}
static void __fastcall CaptureFallback(const BYTE* packet) {
    ++capturedCalls;capturedFallback=packet[3];
}
static void RedirectFixtureCall(DWORD address,void* target) {
    BYTE jump[5]={0xE9};
    LONG offset=reinterpret_cast<BYTE*>(target)-reinterpret_cast<BYTE*>(address+5);
    std::memcpy(jump+1,&offset,4);
    std::memcpy(reinterpret_cast<void*>(address),jump,5);
    FlushInstructionCache(GetCurrentProcess(),reinterpret_cast<void*>(address),5);
}
static void RedirectFixtureRelativeCall(DWORD address,void* target) {
    BYTE call[5]={0xE8};
    LONG offset=reinterpret_cast<BYTE*>(target)-reinterpret_cast<BYTE*>(address+5);
    std::memcpy(call+1,&offset,4);
    std::memcpy(reinterpret_cast<void*>(address),call,5);
    FlushInstructionCache(GetCurrentProcess(),reinterpret_cast<void*>(address),5);
}
#pragma bss_seg(push, ".fixture")
unsigned char g_fixtureSpace[0xC00000];
#pragma bss_seg(pop)
#define CHECK_INSTALL(x) do{++installChecks;if(!(x)){std::printf("FAIL install line=%d\n",__LINE__);return 1;}}while(0)
int wmain(int argc,wchar_t** argv) {
    setvbuf(stdout,nullptr,_IONBF,0);
    SetUnhandledExceptionFilter(&FixtureException);
    CHECK_INSTALL(argc==3);
    // The test EXE reserves its own .fixture section around the native range,
    // before CRT/heap initialization. Never release or overwrite another mapping.
    auto image=reinterpret_cast<unsigned char*>(0x400000);
    CHECK_INSTALL(image>=g_fixtureSpace && image+0x6c4000<=g_fixtureSpace+sizeof g_fixtureSpace);
    DWORD priorProtection{};
    CHECK_INSTALL(VirtualProtect(image,0x6c4000,PAGE_EXECUTE_READWRITE,&priorProtection));
    std::ifstream stream(argv[1],std::ios::binary);
    std::vector<unsigned char> raw{std::istreambuf_iterator<char>(stream),std::istreambuf_iterator<char>()};
    CHECK_INSTALL(raw.size()>4096);
    auto dos=reinterpret_cast<const IMAGE_DOS_HEADER*>(raw.data());
    auto nt=reinterpret_cast<const IMAGE_NT_HEADERS32*>(raw.data()+dos->e_lfanew);
    CHECK_INSTALL(nt->OptionalHeader.ImageBase==0x400000);
    CHECK_INSTALL(nt->OptionalHeader.SizeOfImage==0x6c4000);
    std::memcpy(image,raw.data(),nt->OptionalHeader.SizeOfHeaders);
    auto section=IMAGE_FIRST_SECTION(nt);
    for(unsigned i=0;i<nt->FileHeader.NumberOfSections;++i) {
        CHECK_INSTALL(section[i].PointerToRawData+section[i].SizeOfRawData<=raw.size());
        std::memcpy(image+section[i].VirtualAddress,raw.data()+section[i].PointerToRawData,section[i].SizeOfRawData);
    }
    g_self=GetModuleHandleW(nullptr);
    SetEnvironmentVariableW(L"LITO_SHARED_STATUS_CATALOG",nullptr);
    SetEnvironmentVariableA("LITO_SHARED_EXPIRED_ICONS",nullptr);
    unsigned char nativeRows[1540];std::memcpy(nativeRows,reinterpret_cast<void*>(0x6328D8),1540);
    // No portrait object has been constructed in this fixture; the real
    // constructor path is not pretended to be covered by this test.
    CHECK_INSTALL(*reinterpret_cast<DWORD*>(0x834BD0)==0);
    const bool reject=wcscmp(argv[2],L"accept")!=0;
    if(wcscmp(argv[2],L"reject")==0)SetEnvironmentVariableW(L"LITO_SHARED_STATUS_CATALOG",L"relative-path-refused.bin");
    if(wcscmp(argv[2],L"reject-option")==0)SetEnvironmentVariableA("LITO_SHARED_EXPIRED_ICONS","invalid");
    const auto before=std::vector<unsigned char>(image,image+nt->OptionalHeader.SizeOfImage);
    bool installed=EnsureClassCompatibilityPatch();
    if(reject) {
        CHECK_INSTALL(!installed);
        CHECK_INSTALL(!std::memcmp(image,before.data(),before.size()));
        SetEnvironmentVariableW(L"LITO_SHARED_STATUS_CATALOG",nullptr);
        SetEnvironmentVariableA("LITO_SHARED_EXPIRED_ICONS",nullptr);
        CHECK_INSTALL(!EnsureClassCompatibilityPatch()); // failure memoized
        CHECK_INSTALL(!std::memcmp(image,before.data(),before.size()));
    } else {
        CHECK_INSTALL(installed);
        CHECK_INSTALL(g_dragonStatusRows[0][0]==5397);
        CHECK_INSTALL(g_dragonStatusRows[13][0]==728);
        CHECK_INSTALL(g_dragonStatusRows[38][0]==5399 && g_dragonStatusRows[76][0]==0);
        CHECK_INSTALL(g_dragonStatusRows[77][0]==3080 && g_dragonStatusRows[86][0]==3111);
        CHECK_INSTALL(*reinterpret_cast<DWORD*>(0x4A0171)==reinterpret_cast<DWORD>(g_dragonStatusRows));
        CHECK_INSTALL(!std::memcmp(nativeRows,reinterpret_cast<void*>(0x6328D8),1540));
        for(unsigned slot=0;slot<77;++slot) {
            CHECK_INSTALL(g_dragonStatusRows[slot][1]==SharedStatusCatalog::Word(nativeRows+slot*20+4));
            CHECK_INSTALL(g_dragonStatusRows[slot][2]==SharedStatusBehavior::Groups[slot]);
            CHECK_INSTALL(!std::memcmp(&g_dragonStatusRows[slot][3],nativeRows+slot*20+12,8));
        }
        CHECK_INSTALL(*reinterpret_cast<BYTE*>(0x487600)==0xE9);
        CHECK_INSTALL(0x487605+*reinterpret_cast<LONG*>(0x487601)==reinterpret_cast<DWORD>(&SharedStatusApply));
        CHECK_INSTALL(0x4875C5+*reinterpret_cast<LONG*>(0x4875C1)==reinterpret_cast<DWORD>(&SharedStatusClear));
        CHECK_INSTALL(*reinterpret_cast<DWORD*>(0x487632)==reinterpret_cast<DWORD>(&SharedStatusExpire));
        CHECK_INSTALL(*reinterpret_cast<DWORD*>(0x4875D2)==reinterpret_cast<DWORD>(&SharedStatusExpire));
        CHECK_INSTALL(0x4ADED8+*reinterpret_cast<LONG*>(0x4ADED4)==reinterpret_cast<DWORD>(&SharedOwnGraphic));
        CHECK_INSTALL(0x48A83E+*reinterpret_cast<LONG*>(0x48A83A)==reinterpret_cast<DWORD>(&SharedStatusOrderAndDraw));
        CHECK_INSTALL(!std::memcmp(reinterpret_cast<void*>(0x4ADEC0),before.data()+0xADEC0,16));
        CHECK_INSTALL(EnsureClassCompatibilityPatch()); // idempotent
        CHECK_INSTALL(g_dragonStatusRows[0][0]==5397);
        // Execute the real candidate receiver, intercepting only its native
        // status/UI consumer. This does not pretend to test actual rendering.
        BYTE installedApply[7];std::memcpy(installedApply,reinterpret_cast<void*>(0x487600),7);
        RedirectFixtureCall(0x487600,reinterpret_cast<void*>(&CaptureStatus));
        RedirectFixtureCall(0x4AFF30,reinterpret_cast<void*>(&CaptureFallback));
        *reinterpret_cast<DWORD*>(0x760744)=1; // non-null manager sentinel; never dereferenced
        for(int selector:{5,6,181,201,204,206,209,211,214,216,217,219}) {
            int slot=selector==5?4:selector==6?6:ExtendedStatusSlot(selector);
            for(int seconds:{0,1,10,600,32767}) {
                BYTE packet[]={109,static_cast<BYTE>(seconds),static_cast<BYTE>(seconds>>8),static_cast<BYTE>(selector)};
                capturedCalls=0;capturedFallback=-1;
                DragonSkinStatusPacket(packet,nullptr);
                CHECK_INSTALL(capturedCalls==1 && capturedSeconds==seconds && capturedSlot==slot && capturedFallback==-1);
            }
            BYTE invalid[]={109,0,128,static_cast<BYTE>(selector)};
            capturedCalls=0;DragonSkinStatusPacket(invalid,nullptr);
            CHECK_INSTALL(capturedCalls==0);
        }
        for(int selector:{0,3,4,7,8,10,255}) {
            BYTE packet[]={109,88,2,static_cast<BYTE>(selector)};
            capturedCalls=0;capturedFallback=-1;
            DragonSkinStatusPacket(packet,nullptr);
            CHECK_INSTALL(capturedCalls==1 && capturedFallback==selector);
        }
        *reinterpret_cast<DWORD*>(0x760744)=0;
        BYTE absentManager[]={109,88,2,5};
        capturedCalls=0;DragonSkinStatusPacket(absentManager,nullptr);
        CHECK_INSTALL(capturedCalls==0);
        // Restore the actual installed jump, then execute the compiled adapter,
        // its gateways AND original update/clear/expiry/blink machine code.
        // Only external scheduling, UI list/paint and message display are modeled.
        std::memcpy(reinterpret_cast<void*>(0x487600),installedApply,7);
        FlushInstructionCache(GetCurrentProcess(),reinterpret_cast<void*>(0x487600),7);
        RedirectFixtureCall(0x4D65F0,reinterpret_cast<void*>(&FixtureSchedule));
        RedirectFixtureCall(0x4D6870,reinterpret_cast<void*>(&FixtureCancel));
        RedirectFixtureCall(0x49F960,reinterpret_cast<void*>(&FixtureRemove));
        RedirectFixtureCall(0x49F8A0,reinterpret_cast<void*>(&FixtureAdd));
        RedirectFixtureCall(0x49FAC0,reinterpret_cast<void*>(&FixtureToggle));
        RedirectFixtureCall(0x49F9F0,reinterpret_cast<void*>(&FixtureGroup));
        RedirectFixtureCall(0x417860,reinterpret_cast<void*>(&FixtureMessage));
        *reinterpret_cast<DWORD*>(0x834BC0)=0; // fallback text; never call uninitialized string table
        *reinterpret_cast<DWORD*>(0x760744)=1;
        auto apply=reinterpret_cast<void(__fastcall*)(int,unsigned)>(0x487600);
        auto clear=reinterpret_cast<void(__fastcall*)(unsigned)>(0x4875C0);
        for(unsigned slot=0;slot<96;++slot){
            if(slot==76)continue;
            fixtureEvents.clear();fixtureNow=0;
            apply(11,slot);
            CHECK_INSTALL(g_statusActive[slot] && fixtureUi[slot] && fixtureUiSeconds[slot]==11);
            CHECK_INSTALL(FixtureAdvance(10999));
            CHECK_INSTALL(g_statusActive[slot] && fixtureUi[slot]);
            CHECK_INSTALL(FixtureAdvance(11000));
            if(SharedStatusBehavior::OffIcons[slot]){
                CHECK_INSTALL(g_sharedStatusLifecycle.Expired(slot) && g_statusActive[slot] && g_statusVisible[slot]==0);
                CHECK_INSTALL(fixtureUi[slot] && fixtureVisible[slot] && fixtureUiSeconds[slot]==-1);
                CHECK_INSTALL(g_dragonStatusRows[slot][0]==SharedStatusBehavior::OffIcons[slot]);
                CHECK_INSTALL(fixtureEvents.size()==1 && fixtureEvents[0].at==21000);
                CHECK_INSTALL(FixtureAdvance(20999));CHECK_INSTALL(fixtureUi[slot]);
                CHECK_INSTALL(FixtureAdvance(21000));
            }else CHECK_INSTALL(FixtureAdvance(11500));
            CHECK_INSTALL(!g_statusActive[slot] && !fixtureUi[slot] && fixtureEvents.empty());
            CHECK_INSTALL(g_dragonStatusRows[slot][0]==g_sharedStatusIcons[slot]);
            apply(1,slot);CHECK_INSTALL(FixtureAdvance(fixtureNow+1000));
            apply(20,slot);CHECK_INSTALL(!g_sharedStatusLifecycle.Expired(slot));
            CHECK_INSTALL(g_statusActive[slot] && fixtureUiSeconds[slot]==20);
            clear(slot);CHECK_INSTALL(!g_statusActive[slot] && !fixtureUi[slot] && fixtureEvents.empty());
        }
        fixtureNow=0;
        apply(1,4);CHECK_INSTALL(FixtureAdvance(1000));
        CHECK_INSTALL(g_sharedStatusLifecycle.Expired(4));
        apply(600,6);
        CHECK_INSTALL(!g_statusActive[4] && !fixtureUi[4] && !g_sharedStatusLifecycle.Expired(4));
        apply(0,4);CHECK_INSTALL(g_statusActive[6] && fixtureUi[6]);
        clear(6);CHECK_INSTALL(fixtureEvents.empty());
        g_sharedExpiredIconsEnabled=false;
        apply(1,4);CHECK_INSTALL(FixtureAdvance(fixtureNow+1500));
        CHECK_INSTALL(!g_sharedStatusLifecycle.Expired(4) && !g_statusActive[4] && fixtureEvents.empty());
        g_sharedExpiredIconsEnabled=true;
        // Reset/manager destruction while the off-icon timer is outstanding.
        apply(1,4);CHECK_INSTALL(FixtureAdvance(fixtureNow+1000));
        *reinterpret_cast<DWORD*>(0x760744)=0;
        CHECK_INSTALL(FixtureAdvance(fixtureNow+10000));
        CHECK_INSTALL(!g_statusActive[4] && !g_sharedStatusLifecycle.Expired(4) && fixtureEvents.empty());
        // Execute original schedule/cancel/dispatch too. Only the clock,
        // CRT memmove import and unrelated event pump are substituted.
        std::memcpy(reinterpret_cast<void*>(0x4D65F0),before.data()+0xD65F0,5);
        std::memcpy(reinterpret_cast<void*>(0x4D6870),before.data()+0xD6870,5);
        RedirectFixtureCall(0x4D69F0,reinterpret_cast<void*>(&FixtureClock));
        RedirectFixtureCall(0x4C89F0,reinterpret_cast<void*>(&FixturePump));
        RedirectFixtureRelativeCall(0x4D65B7,reinterpret_cast<void*>(&FixtureMove));
        RedirectFixtureRelativeCall(0x4D66B7,reinterpret_cast<void*>(&FixtureMove));
        *reinterpret_cast<DWORD*>(0x4F7228)=reinterpret_cast<DWORD>(&FixtureMove);
        *reinterpret_cast<DWORD*>(0x834C08)=reinterpret_cast<DWORD>(&nativeTimerStorage[1]);
        *reinterpret_cast<DWORD*>(0x834C04)=256;
        nativeTimerStorage[0]=nativeTimerStorage[256*3+1]=0xABCDEF12;
        FlushInstructionCache(GetCurrentProcess(),image,nt->OptionalHeader.SizeOfImage);
        auto nativeCount=reinterpret_cast<unsigned*>(0x834C00);
        auto dispatch=reinterpret_cast<void(__cdecl*)()>(0x4D6570);
        *reinterpret_cast<DWORD*>(0x760744)=1;
        for(unsigned slot=0;slot<96;++slot){
            if(slot==76)continue;
            fixtureNow=0;*nativeCount=0;
            apply(11,slot);CHECK_INSTALL(*nativeCount==2);
            fixtureNow=1000;dispatch();CHECK_INSTALL(g_statusActive[slot]);
            fixtureNow=11000;dispatch();
            if(SharedStatusBehavior::OffIcons[slot]){
                CHECK_INSTALL(*nativeCount==1 && g_sharedStatusLifecycle.Expired(slot));
                fixtureNow=20999;dispatch();CHECK_INSTALL(fixtureUi[slot]);
                fixtureNow=21000;dispatch();
            }else{fixtureNow=11500;dispatch();}
            CHECK_INSTALL(*nativeCount==0 && !g_statusActive[slot] && !fixtureUi[slot]);
            apply(1,slot);fixtureNow+=1000;dispatch();
            apply(60,slot);CHECK_INSTALL(!g_sharedStatusLifecycle.Expired(slot) && *nativeCount==2);
            clear(slot);CHECK_INSTALL(*nativeCount==0 && !g_statusActive[slot]);
            CHECK_INSTALL(nativeTimerStorage[0]==0xABCDEF12 && nativeTimerStorage[256*3+1]==0xABCDEF12);
        }
        apply(1,4);fixtureNow+=1000;dispatch();
        CHECK_INSTALL(g_sharedStatusLifecycle.Expired(4));
        apply(60,6);CHECK_INSTALL(!g_statusActive[4] && !fixtureUi[4] && *nativeCount==2);
        clear(6);CHECK_INSTALL(*nativeCount==0);
        for(const auto& row:SharedStatusBehavior::Graphics){
            clear(row.slot);
            SharedOwnGraphic(row.graphic);
            CHECK_INSTALL(!g_sharedExplicitDuration[row.slot]);
            CHECK_INSTALL(bool(g_statusActive[row.slot])==(row.seconds>0));
            if(row.seconds>0){
                CHECK_INSTALL(fixtureUiSeconds[row.slot]==row.seconds);
                fixtureNow+=1;SharedOwnGraphic(row.graphic);
                CHECK_INSTALL(*nativeCount==2 && fixtureUiSeconds[row.slot]==row.seconds);
            }
            apply(7,row.slot);CHECK_INSTALL(g_sharedExplicitDuration[row.slot]);
            SharedOwnGraphic(row.graphic);
            CHECK_INSTALL(g_sharedExplicitDuration[row.slot] && fixtureUiSeconds[row.slot]==7);
            clear(row.slot);CHECK_INSTALL(!g_sharedExplicitDuration[row.slot] && *nativeCount==0);
        }
        // The XML candidates with an explicit-icon-only source path remain
        // native no-ops on their graphic alone.
        for(unsigned gfx:{2249u,2287u,2252u,2182u,2286u,2242u,2246u,2288u,2248u,2247u,2945u,65535u}){
            SharedOwnGraphic(gfx);CHECK_INSTALL(*nativeCount==0);
        }
        // Actual original world dispatcher -> installed packet-box call ->
        // candidate status lifecycle -> original scheduler/UI model.
        CHECK_INSTALL(0x4B2EDE+*reinterpret_cast<LONG*>(0x4B2EDA)==reinterpret_cast<DWORD>(&SharedPacketBox));
        CHECK_INSTALL(!std::memcmp(reinterpret_cast<void*>(0x4B3101),before.data()+0xB3101,5));
        *reinterpret_cast<BYTE*>(0x64AFA2)=0;
        *reinterpret_cast<DWORD*>(0x64BBD4)=4;
        *reinterpret_cast<DWORD*>(0x64AF98)=3;
        auto worldDispatch=reinterpret_cast<void(__fastcall*)(const BYTE*)>(0x4B2970);
        RedirectFixtureCall(0x417910,reinterpret_cast<void*>(&FixtureMessage));
        // Native strength/dexterity handlers write SI only. Dispatch retains
        // the packet pointer in ESI, exercising the real polluted-high-word ABI.
        for(unsigned opcode:{0x6Bu,0x6Cu})for(unsigned type:{1u,2u}){
            unsigned slot=opcode==0x6B?(type==2?34:9):(type==2?35:10);
            for(unsigned seconds:{1u,600u,32767u}){
                BYTE ability[]={static_cast<BYTE>(opcode),static_cast<BYTE>(seconds),
                    static_cast<BYTE>(seconds>>8),18,0,0};
                ability[opcode==0x6B?5:4]=static_cast<BYTE>(type);
                worldDispatch(ability);
                CHECK_INSTALL(g_statusActive[slot] && fixtureUiSeconds[slot]==static_cast<int>(seconds));
                ability[1]=ability[2]=0;worldDispatch(ability);
                CHECK_INSTALL(!g_statusActive[slot] && *nativeCount==0);
            }
        }
        std::printf("PASS native strength/dexterity world receivers: CX duration preserved, active/clear and both types\n");
        RedirectFixtureCall(0x4E8160,reinterpret_cast<void*>(&FixtureActor));
        RedirectFixtureCall(0x4C3E90,reinterpret_cast<void*>(&FixtureSpeedUi));
        *reinterpret_cast<void**>(0x834EE4)=fixtureActor;
        for(unsigned id:{1u,2u,3u})for(unsigned seconds:{1u,600u,32767u}){
            BYTE brave[]={0x62,static_cast<BYTE>(id),0,0,0,6,
                static_cast<BYTE>(seconds),static_cast<BYTE>(seconds>>8)};
            worldDispatch(brave);
            CHECK_INSTALL(bool(g_statusActive[95])==(id==1));
            if(id==1)CHECK_INSTALL(fixtureUiSeconds[95]==static_cast<int>(seconds) && g_dragonStatusRows[95][0]==3075);
            if(id!=3)CHECK_INSTALL((id==1?fixtureActor:fixturePeer)[0x21]==6);
            brave[5]=brave[6]=brave[7]=0;worldDispatch(brave);
            CHECK_INSTALL(!g_statusActive[95] && *nativeCount==0);
        }
        std::printf("PASS native brave receiver: bloodlust mode6/zero, own/peer/missing actor; actor mode retained\n");
        *reinterpret_cast<BYTE*>(0x75F694)=0;
        for(const auto& row:kSharedStateWireCases){
            for(unsigned slot=0;slot<96;++slot)if(slot!=76)clear(slot);
            if(row.slot>=0 && row.seconds==0)apply(600,row.slot);
            worldDispatch(row.packet);
            for(unsigned slot=0;slot<96;++slot)if(slot!=76){
                bool expected=static_cast<int>(slot)==row.slot && row.seconds!=0;
                CHECK_INSTALL(bool(g_statusActive[slot])==expected);
                if(expected)CHECK_INSTALL(fixtureUiSeconds[slot]==row.seconds);
            }
        }
        for(unsigned slot=0;slot<96;++slot)if(slot!=76)clear(slot);
        CHECK_INSTALL(*nativeCount==0);
        std::printf("PASS 36 actual paired JAR strength/dexterity/breath/speed/brave packets through original world receivers\n");
        // Execute complete original frame lookup/rate code plus compiled
        // candidate bridges, compared against the actual TW machine code.
        BYTE frameActor[0x140]{},frameBytes[60]{};
        DWORD frameTable[96][3]{},protectedValues[3]{};
        DWORD savedTable=*reinterpret_cast<DWORD*>(0x834E70);
        for(auto& row:frameTable)row[2]=reinterpret_cast<DWORD>(frameBytes);
        *reinterpret_cast<DWORD*>(0x834E70)=reinterpret_cast<DWORD>(frameTable);
        for(unsigned i=0;i<3;++i){
            DWORD address=i==0?0x831670:(i==1?0x831718:0x831680);
            *reinterpret_cast<DWORD*>(address)=0xB104FA87;
            *reinterpret_cast<DWORD*>(address+4)=reinterpret_cast<DWORD>(&protectedValues[i]);
            *reinterpret_cast<DWORD*>(address+8)=0;
        }
        *reinterpret_cast<BYTE*>(0x64BC13)=0;
        auto frameDuration=reinterpret_cast<int(__thiscall*)(void*,int,int)>(0x4E8750);
        fixtureRateStage=1;fixtureRateCase=0;
        for(const auto& row:kSharedFrameCases){
            ++fixtureRateCase;
            *reinterpret_cast<DWORD*>(frameActor+8)=1;
            frameActor[0x10]=static_cast<BYTE>(row.action);
            frameActor[0x1C]=static_cast<BYTE>(row.speed);
            frameActor[0x21]=static_cast<BYTE>(row.brave);
            *reinterpret_cast<DWORD*>(frameActor+0x11C)=row.burst;
            protectedValues[1]=row.speed;protectedValues[2]=row.brave;
            *reinterpret_cast<DWORD*>(0x75AA48)=row.own?1:2;
            *reinterpret_cast<WORD*>(frameBytes+10)=static_cast<WORD>(row.duration^0x1812);
            int actual=frameDuration(frameActor,row.action,0);
            if(actual!=row.expected)std::printf("FRAME mismatch action=%d speed=%d brave=%d duration=%d own=%d actual=%d expected=%d\n",
                row.action,row.speed,row.brave,row.duration,row.own,actual,row.expected);
            CHECK_INSTALL(actual==row.expected);
        }
        auto totalDuration=reinterpret_cast<int(__thiscall*)(void*,int)>(0x4E8630);
        fixtureRateStage=2;fixtureRateCase=0;
        for(const auto& row:kSharedTotalCases){
            ++fixtureRateCase;
            frameActor[0x10]=static_cast<BYTE>(row.action);
            frameActor[0x1C]=static_cast<BYTE>(row.speed);
            frameActor[0x21]=static_cast<BYTE>(row.brave);
            protectedValues[1]=row.speed;protectedValues[2]=row.brave;
            *reinterpret_cast<DWORD*>(0x75AA48)=row.own?1:2;
            reinterpret_cast<BYTE*>(frameTable[row.action])[1]=static_cast<BYTE>(row.frames);
            for(int index=0;index<3;++index)
                *reinterpret_cast<WORD*>(frameBytes+index*20+10)=static_cast<WORD>(row.duration^0x1812);
            int actual=totalDuration(frameActor,row.action);
            if(actual!=row.expected)std::printf("TOTAL mismatch action=%d speed=%d brave=%d duration=%d own=%d frames=%d actual=%d expected=%d\n",
                row.action,row.speed,row.brave,row.duration,row.own,row.frames,actual,row.expected);
            CHECK_INSTALL(actual==row.expected);
        }
        *reinterpret_cast<DWORD*>(0x834E70)=savedTable;
        fixtureRateStage=0;
        std::printf("PASS 10560 native frame and10560 action calculations match TW: own/peer,22 actions,5 speed,6 brave modes and burst\n");
        for(const auto& row:kSharedDollWireCases){
            for(unsigned slot=87;slot<96;++slot)clear(slot);
            if(row.slot>=0 && row.seconds==0)apply(600,row.slot);
            CHECK_INSTALL(row.size==6 || row.size==10);
            worldDispatch(row.packet);
            for(unsigned slot=87;slot<96;++slot){
                bool expected=static_cast<int>(slot)==row.slot && row.seconds>0;
                CHECK_INSTALL((g_statusActive[slot]!=0)==expected);
                if(expected)CHECK_INSTALL(fixtureUiSeconds[slot]==static_cast<int>(row.seconds));
            }
        }
        for(unsigned slot=87;slot<96;++slot)clear(slot);
        CHECK_INSTALL(*nativeCount==0);
        BYTE genericDoll[]={0x7B,56,88,2,0,0};
        worldDispatch(genericDoll);
        CHECK_INSTALL(g_statusActive[87] && fixtureUiSeconds[87]==600 && g_dragonStatusRows[87][0]==2588);
        apply(600,88);
        genericDoll[2]=genericDoll[3]=0;worldDispatch(genericDoll);
        CHECK_INSTALL(!g_statusActive[87] && !g_statusActive[88] && *nativeCount==0);
        // Existing four-byte generic packet is accepted without reading a
        // nonexistent subtype. The paired new producer adds the zero word.
        BYTE shortDoll[]={0x7B,56,1,0};worldDispatch(shortDoll);
        CHECK_INSTALL(g_statusActive[87] && fixtureUiSeconds[87]==1);clear(87);
        for(unsigned graphic=512;graphic<=516;++graphic){
            unsigned slot=89+graphic-512;
            BYTE packet[]={0x7B,154,88,2,static_cast<BYTE>(graphic),static_cast<BYTE>(graphic>>8),0,0,0,0};
            worldDispatch(packet);
            CHECK_INSTALL(g_statusActive[slot] && fixtureUiSeconds[slot]==600 && *nativeCount==2);
            packet[2]=60;packet[3]=0;worldDispatch(packet);
            CHECK_INSTALL(fixtureUiSeconds[slot]==60 && *nativeCount==2);
            packet[2]=packet[3]=0;packet[8]=1;worldDispatch(packet);
            CHECK_INSTALL(!g_statusActive[slot] && *nativeCount==0);
        }
        BYTE unknownDoll[]={0x7B,154,88,2,0xff,0xff,0,0,0,0};worldDispatch(unknownDoll);
        CHECK_INSTALL(*nativeCount==0);
        BYTE badDoll[]={0x7B,56,0,128,0,0};worldDispatch(badDoll);
        CHECK_INSTALL(*nativeCount==0);
        for(int selected:{0,2464,2465,2466,-1}){
            *reinterpret_cast<int*>(0x647EC0)=selected;
            unsigned expected=selected==2465?94:38;
            for(unsigned seconds:{1u,600u,32767u}){
                BYTE blue[]={0x7B,34,static_cast<BYTE>(seconds),static_cast<BYTE>(seconds>>8)};
                worldDispatch(blue);
                CHECK_INSTALL(g_statusActive[expected] && fixtureUiSeconds[expected]==static_cast<int>(seconds));
                CHECK_INSTALL(!g_statusActive[expected==94?38:94]);
                CHECK_INSTALL(g_dragonStatusRows[expected][0]==(expected==94?1478u:5399u));
                blue[2]=blue[3]=0;worldDispatch(blue);CHECK_INSTALL(g_statusActive[expected]);
                clear(expected);CHECK_INSTALL(*nativeCount==0);
            }
        }
        std::printf("PASS blue-potion packet34: native selected-item counterpart, alternative91, ordinary38 and zero no-op\n");
        // generic packet-box effects retain the original receiver.
        BYTE savedBox[5];std::memcpy(savedBox,reinterpret_cast<void*>(0x4B07C0),5);
        RedirectFixtureCall(0x4B07C0,reinterpret_cast<void*>(&CaptureFallback));
        BYTE legacyBox[]={0x7B,35,88,2};
        capturedFallback=0;worldDispatch(legacyBox);CHECK_INSTALL(capturedFallback==2);
        std::memcpy(reinterpret_cast<void*>(0x4B07C0),savedBox,5);
        FlushInstructionCache(GetCurrentProcess(),reinterpret_cast<void*>(0x4B07C0),5);
        *reinterpret_cast<DWORD*>(0x64BBD4)=3;SharedPacketBox(shortDoll,nullptr);
        CHECK_INSTALL(*nativeCount==0);
        *reinterpret_cast<DWORD*>(0x64BBD4)=4;
        *reinterpret_cast<DWORD*>(0x760744)=0;
        SharedPacketBox(shortDoll,nullptr);CHECK_INSTALL(*nativeCount==0);
        SharedOwnGraphic(6531);CHECK_INSTALL(*nativeCount==0 && !g_statusActive[77]);
        // Execute the three original scene-reset clear blocks, with only a
        // return inserted after each block so unrelated scene logic is not run.
        for(auto range:{std::pair<DWORD,DWORD>{0x46A2FF,0x46A325},
                        {0x47E00D,0x47E02B},{0x47E378,0x47E39E}}){
            BYTE tail=*reinterpret_cast<BYTE*>(range.second);
            *reinterpret_cast<BYTE*>(range.second)=0xC3;
            FlushInstructionCache(GetCurrentProcess(),reinterpret_cast<void*>(range.second),1);
            std::memset(g_statusActive,0xA5,sizeof g_statusActive);
            std::memset(g_statusVisible,0x5A,sizeof g_statusVisible);
            FixtureResetBlock(range.first);
            *reinterpret_cast<BYTE*>(range.second)=tail;
            FlushInstructionCache(GetCurrentProcess(),reinterpret_cast<void*>(range.second),1);
            for(unsigned i=0;i<96;++i)
                CHECK_INSTALL(g_statusActive[i]==0 && g_statusVisible[i]==0);
        }
        std::printf("PASS three original scene-reset blocks clear all 96 owned flag bytes\n");
        // Execute actual compiled ordering bridge; model only manager iteration
        // and the renderer. Native comparator oracle checks the emitted order.
        RedirectFixtureCall(0x419DB0,reinterpret_cast<void*>(&OrderFirst));
        RedirectFixtureCall(0x419E00,reinterpret_cast<void*>(&OrderNext));
        RedirectFixtureCall(0x49FA90,reinterpret_cast<void*>(&OrderDraw));
        for(int slot=95;slot>=0;--slot){
            if(slot==76)continue;
            auto n=orderNodes[orderCount];auto ui=orderUi[orderCount];
            n[0]=reinterpret_cast<DWORD>(ui);n[1]=static_cast<DWORD>(slot)*7919u;
            if(slot==41)n[1]=0xFFFFFFFFu;
            n[3]=orderCount;n[4]=slot;
            *reinterpret_cast<int*>(ui+4)=800+slot;
            *reinterpret_cast<int*>(ui+12)=832+slot;
            *reinterpret_cast<int*>(ui+8)=123;
            *reinterpret_cast<int*>(ui+16)=155;
            ++orderCount;
        }
        DWORD preservedNodes[96][5];std::memcpy(preservedNodes,orderNodes,sizeof orderNodes);
        SharedStatusOrderAndDraw(reinterpret_cast<void*>(1));
        CHECK_INSTALL(orderDraws==1);
        CHECK_INSTALL(!std::memcmp(preservedNodes,orderNodes,sizeof orderNodes));
        std::printf("PRIORITY_ORDER:");
        for(unsigned rank=0;rank<orderCount;++rank){
            unsigned found=96;
            for(unsigned i=0;i<orderCount;++i)
                if(*reinterpret_cast<int*>(orderUi[i]+4)==600-static_cast<int>(rank)*40){
                    CHECK_INSTALL(found==96);found=i;
                }
            CHECK_INSTALL(found<96);
            CHECK_INSTALL(*reinterpret_cast<int*>(orderUi[found]+12)-*reinterpret_cast<int*>(orderUi[found]+4)==32);
            CHECK_INSTALL(*reinterpret_cast<int*>(orderUi[found]+8)==123 && *reinterpret_cast<int*>(orderUi[found]+16)==155);
            std::printf("%s%u",rank?",":"",orderNodes[found][4]);
        }
        std::printf("\n");
        BYTE beforeSort[96][0x58];std::memcpy(beforeSort,orderUi,sizeof orderUi);
        SharedStatusOrderAndDraw(reinterpret_cast<void*>(1));
        CHECK_INSTALL(orderDraws==2 && !std::memcmp(beforeSort,orderUi,sizeof orderUi));
        orderNodes[0][4]=96;SharedStatusOrderAndDraw(reinterpret_cast<void*>(1));
        CHECK_INSTALL(orderDraws==3 && !std::memcmp(beforeSort,orderUi,sizeof orderUi));
        SharedStatusOrderAndDraw(nullptr);CHECK_INSTALL(orderDraws==3);
        std::printf("PASS native world packet-box dispatch: generic/custom dolls, refresh/remove, invalid/state guards; clan receiver untouched\n");
        std::printf("PASS actual paired JAR doll packets: 36 cases through original world dispatcher and compiled receiver\n");
        std::printf("PASS own graphic XML dispatch: %u mappings, explicit-duration precedence, legacy no-ops\n",
            static_cast<unsigned>(sizeof(SharedStatusBehavior::Graphics)/sizeof(SharedStatusBehavior::Graphics[0])));
        std::printf("PASS original scheduler/cancel/dispatch + candidate lifecycle for 95 slots; UI/clock/import modeled\n");
    }
    DWORD ignored{};
    CHECK_INSTALL(VirtualProtect(image,0x6c4000,priorProtection,&ignored));
    std::printf("PASS actual status installer %s: %u checks; game not started\n",reject?"reject":"accept",installChecks);
}
