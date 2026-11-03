// New-class skill slots retain native list, drag/drop and casting objects.
// Icon IDs are from TW13081901's shared zero-based table at 0x962ED8.
static WORD g_sevenSkillIcons[256]{};
static int g_newClassSkillPalette[2][80]{};
// The native shortcut disabled-icon lookup indexes skill IDs minus 80.
// Reserve its full range, even though the visible list contains 80 slots.
static int g_newClassSkillDisabled[256]{};
static const WORD g_sharedNewSkillIcons[40]={3079,3076,3083,3087,3181,3074,3081,3090,4559,3183,3088,3093,3085,4583,3077,0,0,0,0,0,3122,3098,3130,3116,3102,3096,3121,3095,3114,3105,3126,3128,3094,3112,3107,3118,3124,3120,3110,3099};
#include "class_skill_names.inc"
char** __fastcall LoadSevenClassSkillDescriptions(const char* name,int* count) {
    auto original=reinterpret_cast<char** (__fastcall*)(const char*,int*)>(0x440910)(name,count);
    if(!original || *count!=176) {Log("skill description extension refused count=%d",*count);return original;}
    auto alloc=reinterpret_cast<void* (__cdecl*)(std::size_t)>(0x4F118C);
    auto release=reinterpret_cast<void (__cdecl*)(void*)>(0x4F103C);
    auto extended=static_cast<char**>(alloc(220*sizeof(char*)));
    if(!extended) return original;
    memset(extended,0,220*sizeof(char*));memcpy(extended,original,176*sizeof(char*));
    for(int i=176;i<220;++i) {
        const char* value=g_sharedExtraSkillNames[i-176];if(!*value) continue;
        std::size_t bytes=strlen(value)+1;extended[i]=static_cast<char*>(alloc(bytes));
        if(!extended[i]) {
            for(int j=176;j<i;++j) if(extended[j])release(extended[j]);
            release(extended);return original;
        }
        memcpy(extended[i],value,bytes);
    }
    release(original);*count=220;
    Log("skill descriptions extended 176 -> 220 from shared spelldesc-c.tbl");
    return extended;
}
void __cdecl SelectSevenClassSkillPalette() {
    int job=*reinterpret_cast<BYTE*>(0x75F694);
    if(job!=5 && job!=6) return;
    *reinterpret_cast<int**>(0x635B80)=g_newClassSkillPalette[job-5];
    *reinterpret_cast<int**>(0x635B84)=g_newClassSkillDisabled;
    Log("new-class skill palette selected class=%d slots=80",job);
}
__declspec(naked) void SevenClassSkillPaletteBridge() {
    __asm {
        pushad
        call SelectSevenClassSkillPalette
        popad
        mov ecx, dword ptr ds:[0648EBCh]
        mov eax, 0492B34h
        jmp eax
    }
}
// The legacy switch sends every skill above 176 through selector-only ccc.
// Reuse its ranged object picker and cccdhh producer for targeted Dragon/Illusion skills; self and cube spells retain selector-only.
int __cdecl DragonSkillTargetRange(int zeroBasedSkill) {
    switch(zeroBasedSkill+1) {
    case 183: case 188: case 193:
    case 202: case 204: case 207: case 208: case 209: case 211: case 212:
    case 213: case 214: case 216: case 217: case 218: case 219: return 3;
    case 203: return 1;
    case 184: case 194: return 10;
    case 187: return 2;
    case 192: return 8;
    default: return 0;
    }
}
__declspec(naked) void DragonSkillDispatchBridge() {
    __asm {
        push esi
        call DragonSkillTargetRange
        add esp, 4
        test eax, eax
        jz legacy
        mov edx, eax
        push dword ptr [ebp+0Ch]
        mov ecx, esi
        mov dword ptr [edi+68h], esi
        mov eax, 04CA6E0h
        call eax
        mov eax, 04CBC76h
        jmp eax
    legacy:
        mov edx, esi
        mov dword ptr [edi+68h], esi
        mov eax, 04CBC2Ch
        jmp eax
    }
}
#ifdef LITO_SOURCE_UI_TEST
static DWORD g_dragonFixtureReceiver[32]{};
// Enter the original switch's patched default arm with its real stack layout.
__declspec(naked) void __stdcall FixtureDragonDispatch(int skill,int direct) {
    __asm {
        push ebp
        mov ebp, esp
        push ebx
        push esi
        push edi
        mov esi, dword ptr [ebp+8]
        lea edi, g_dragonFixtureReceiver
        mov eax, 04CBC27h
        jmp eax
    }
}
bool VerifyDragonTargetPicker() {
    auto target=reinterpret_cast<DWORD*>(0x63D530);
    auto blocked=reinterpret_cast<BYTE*>(0x63D551);
    auto callback=reinterpret_cast<DWORD*>(0x75A82C);
    DWORD oldTarget=*target,oldCallback=*callback;BYTE oldBlocked=*blocked;
    *target=0;*blocked=0;bool ok=true;
    for(int id:{183,184,187,188,192,193,194,202,203,204,207,208,209,211,212,213,214,216,217,218,219}) {
        *callback=0;FixtureDragonDispatch(id-1,0);
        bool selected=*callback==0x4C9E40
            && *reinterpret_cast<int*>(0x834880)==id-1
            && *reinterpret_cast<int*>(0x75A828)==DragonSkillTargetRange(id-1);
        Log("fixture dragon target picker skill=%d range=%d selected=%d",id,DragonSkillTargetRange(id-1),selected);
        ok=selected&&ok;
    }
    for(int id:{201,205,206,210,215,220})ok=DragonSkillTargetRange(id-1)==0 && ok;
    *target=oldTarget;*blocked=oldBlocked;*callback=oldCallback;
    return ok;
}
bool VerifyIllusionActualCast() {
    DWORD self=*reinterpret_cast<DWORD*>(0x834EE4);if(!self)return false;
    auto target=reinterpret_cast<DWORD*>(0x63D530);
    auto blocked=reinterpret_cast<BYTE*>(0x63D551);
    auto alternate=reinterpret_cast<BYTE*>(0x83488C);
    DWORD previous=*target;BYTE oldBlocked=*blocked,oldAlternate=*alternate;
    *target=*reinterpret_cast<DWORD*>(self+8);*blocked=0;*alternate=0;
    FixtureDragonDispatch(203,0);
    *target=previous;*blocked=oldBlocked;*alternate=oldAlternate;
    return true;
}
#endif
#include "dragon_status.inc"
#include "shared_status_lifecycle.inc"
#include "shared_status_order.inc"
#include "shared_packetbox.inc"
#include "shared_frame_rate.inc"
#include "self_name.inc"
#include "dragon_combo.inc"
void AddSevenClassSkillPatches(std::vector<NumericPatch>& patches) {
    AddDragonStatusPatches(patches);
    AddSharedStatusLifecyclePatches(patches);
    AddSharedStatusOrderPatches(patches);
    AddSharedPacketBoxPatches(patches);
    AddSharedFrameRatePatches(patches);
    AddSelfNamePatch(patches);
    AddDragonComboPatches(patches);
    memcpy(g_sevenSkillIcons,reinterpret_cast<void*>(0x508648),176*sizeof(WORD));
    memcpy(g_sevenSkillIcons+180,g_sharedNewSkillIcons,sizeof(g_sharedNewSkillIcons));
    for(int job=0;job<2;++job) {
        for(int& icon:g_newClassSkillPalette[job]) icon=-1;
        int start=job==0?180:200, count=job==0?15:20;
        for(int i=0;i<count;++i) g_newClassSkillPalette[job][i]=g_sevenSkillIcons[start+i];
        // Common low-level spells remain visible if learned in the source DB.
        for(int i=0;i<16;++i) g_newClassSkillPalette[job][24+i]=g_sevenSkillIcons[i];
    }
    // Dynamic consumers share the extended map. Keep the four legacy palette
    // initializer loops on their original begin/end addresses.
    for(auto a:{0x4AECD8u,0x4AECF6u,0x4AEEBCu,0x4AEEE5u,0x4AF851u,0x4AF862u})
        patches.push_back({reinterpret_cast<BYTE*>(a),DwordBytes(0x508648),
            DwordBytes(reinterpret_cast<DWORD>(g_sevenSkillIcons)),"seven-class-skill-icon-map"});
    AddNumericRelativePatch(patches,0x47C8F0,{0xE8,0x1B,0x40,0xFC,0xFF},0xE8,
        reinterpret_cast<void*>(&LoadSevenClassSkillDescriptions),"shared-spell-descriptions");
    AddNumericRelativePatch(patches,0x492B2E,{0x8B,0x0D,0xBC,0x8E,0x64,0x00},0xE9,
        reinterpret_cast<void*>(&SevenClassSkillPaletteBridge),"seven-class-skill-palette");
    AddNumericRelativePatch(patches,0x4CBC27,{0x8B,0xD6,0x89,0x77,0x68},0xE9,
        reinterpret_cast<void*>(&DragonSkillDispatchBridge),"dragon-skill-target-picker");
}
