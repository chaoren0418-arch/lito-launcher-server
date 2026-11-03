#include "forced_combat.inc"
static LONG g_selectorPatchState{};
void __fastcall ObserveRangedAttack(void* packet, void*) {
    const auto p = static_cast<const BYTE*>(packet);
    DWORD sequence{}; memcpy(&sequence,p+12,4);
    if (p[1] == 21 && sequence) {
        WORD gfx{}; memcpy(&gfx,p+16,2);
        auto table=*reinterpret_cast<BYTE**>(0x647E5C);
        auto count=*reinterpret_cast<DWORD*>(0x647E7C);
        Log("native ranged packet action=%u gfx=%u flight=%u sequence=%lu effect-type=%d",
            p[1],gfx,p[18],sequence,table&&gfx<count?table[gfx]:-1);
    }
    reinterpret_cast<void (__fastcall*)(void*,void*)>(0x4A5ED0)(packet,nullptr);
    if (p[1]==21 && sequence) {
        DWORD id{};memcpy(&id,p+2,4);
        auto actor=reinterpret_cast<BYTE* (__fastcall*)(DWORD)>(0x4E8160)(id);
        if(actor) Log("native bow actor id=%lu gfx=%u action=%u frame=%u family=%u queue=%d pending-arrow=%p",
            id,*reinterpret_cast<WORD*>(actor+0x14),actor[0x10],actor[0x13],actor[0x18],
            *reinterpret_cast<int*>(actor+0x74),*reinterpret_cast<void**>(actor+0x80));
    }
}
#include "arrow_observe.inc"
#include "arrow_flight_observe.inc"
bool EnsureSelectorReturnPatch() {
    if (InterlockedCompareExchange(&g_selectorPatchState,0,0)==1) return true;
    std::vector<NumericPatch> patches;
    AddForcedCombatPatch(patches);
    AddNumericRelativePatch(patches,0x4967A5,{0xE8,0x36,0xFC,0xFF,0xFF},0xE8,
        reinterpret_cast<void*>(&NativeLoginPageBuilt),"pre-login-source-page");
    AddNumericRelativePatch(patches,0x48FC01,{0xE8,0x7A,0x93,0x00,0x00},0xE8,
        reinterpret_cast<void*>(&SelectAuthenticatedSource),"authenticated-source-retained");
    AddNumericRelativePatch(patches,0x4B3177,{0xE8,0xD4,0x42,0xFF,0xFF},0xE8,
        reinterpret_cast<void*>(&ReceivePagedCharacterCount),"selector-character-count");
    AddNumericRelativePatch(patches,0x4B3095,{0xE8,0xB6,0x44,0xFF,0xFF},0xE8,
        reinterpret_cast<void*>(&ReceivePagedCharacterRow),"selector-character-page-rows");
    AddNumericRelativePatch(patches,0x4A7781,{0xE8,0x9A,0xF8,0xFE,0xFF},0xE8,
        reinterpret_cast<void*>(&OpenChannelSelector),"selector-channel-phase");
    AddNumericRelativePatch(patches,0x4B2B41,{0xE8,0x8A,0x33,0xFF,0xFF},0xE8,
        reinterpret_cast<void*>(&ObserveRangedAttack),"ranged-receiver-evidence");
    AddNumericRelativePatch(patches,0x4A60A8,{0xE8,0x33,0x82,0xF6,0xFF},0xE8,
        reinterpret_cast<void*>(&ObserveNativeArrow),"arrow-constructor-evidence");
    patches.push_back({reinterpret_cast<BYTE*>(0x4F7760),DwordBytes(0x40E8F0),
        DwordBytes(static_cast<DWORD>(reinterpret_cast<std::uintptr_t>(&ObserveArrowStart))),"arrow-start-evidence"});
    AddNumericRelativePatch(patches,0x40E82E,{0xE8,0x9D,0xF1,0x0B,0x00},0xE8,
        reinterpret_cast<void*>(&ObserveArrowDraw),"arrow-draw-evidence");
    if (!CommitNumericPatches(patches)) return false;
    InterlockedExchange(&g_selectorPatchState,1);
    Log("native selector transitions installed; channel page=40, phase=0");
    return true;
}
