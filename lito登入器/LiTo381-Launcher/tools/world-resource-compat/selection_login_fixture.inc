static void* selectionCallback;
static const char* selectionText;
static int selectionModalKind;
static DWORD selectionRoot[8]{},selectionStrings[0x50]{};
static void __fastcall SelectionDisable(void*,void*,int){}
static void __fastcall SelectionHide(void*,void*){}
static void __fastcall SelectionMode(int){}
static char* __cdecl SelectionCopy(char* target,const char* source,size_t count){
    return std::strncpy(target,source,count);
}
static const char* __cdecl SelectionFind(const char* text,int value){
    return std::strchr(text,value);
}
static void __fastcall SelectionModal(const char* text,int kind,void* callback){
    selectionText=text;selectionModalKind=kind;selectionCallback=callback;
}
static int RunSelectionLoginFixture(){
    CHECK_INSTALL(*reinterpret_cast<BYTE*>(0x490745)==0xE8);
    CHECK_INSTALL(0x49074A+*reinterpret_cast<LONG*>(0x490746)
        ==reinterpret_cast<DWORD>(&ShowRejectedSourceAccount));
    RedirectFixtureCall(0x41B720,reinterpret_cast<void*>(&SelectionDisable));
    RedirectFixtureCall(0x41B6E0,reinterpret_cast<void*>(&SelectionHide));
    RedirectFixtureCall(0x41B6C0,reinterpret_cast<void*>(&SelectionHide));
    RedirectFixtureCall(0x49A510,reinterpret_cast<void*>(&SelectionMode));
    RedirectFixtureCall(0x4980F0,reinterpret_cast<void*>(&SelectionModal));
    RedirectFixtureRelativeCall(0x4A2AF9,reinterpret_cast<void*>(&SelectionCopy));
    RedirectFixtureRelativeCall(0x4A2B09,reinterpret_cast<void*>(&SelectionFind));
    // Preserve the real login parser and selector table. Only the UI rendering
    // sink is modeled; the actual modal callback must use deferred logout.
    selectionStrings[0x42]=reinterpret_cast<DWORD>("account already in use");
    *reinterpret_cast<DWORD*>(0x834BC0)=0x50;
    *reinterpret_cast<DWORD*>(0x834BC4)=reinterpret_cast<DWORD>(selectionStrings);
    *reinterpret_cast<DWORD*>(0x75F6E8)=reinterpret_cast<DWORD>(selectionRoot);
    *reinterpret_cast<WORD*>(0x64AFBC)=0;
    *reinterpret_cast<BYTE*>(0x64B6DC)=0;
    g_loginSourceSelected=1;g_loginSourceCount=1;
    g_loginSourcePending=false;g_logoutToAccountLogin=false;
    BYTE packet[]={0,0x16,0,0,0,0,'X',0,0,0,0};
    reinterpret_cast<void(__fastcall*)(const BYTE*)>(0x4903D0)(packet);
    CHECK_INSTALL(selectionCallback==reinterpret_cast<void*>(&ConfirmRejectedSourceAccount));
    CHECK_INSTALL(selectionText==reinterpret_cast<const char*>(selectionStrings[0x42]));
    CHECK_INSTALL(selectionModalKind==1);
    CHECK_INSTALL(!g_loginSourcePending);
    reinterpret_cast<void(__fastcall*)(int,void*)>(selectionCallback)(1,nullptr);
    CHECK_INSTALL(g_loginSourcePending&&g_logoutToAccountLogin);
    CHECK_INSTALL(*reinterpret_cast<DWORD*>(0x75F6E8)==reinterpret_cast<DWORD>(selectionRoot));
    CHECK_INSTALL(g_loginSourceSelected==1);
    std::puts("PASS real S_LOGIN_CHECK/0x16 -> native message -> confirm -> deferred blank-login logout; root remains until dispatch unwinds");
    return 0;
}
