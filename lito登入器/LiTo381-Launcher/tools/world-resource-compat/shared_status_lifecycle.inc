// Included after dragon_status.inc; references its existing owned status data.
#include "shared_status_behavior.h"
#include "shared_status_lifecycle.h"
void __fastcall SharedStatusFinish(unsigned slot);
void __fastcall SharedStatusExpire(unsigned slot);
bool g_sharedExpiredIconsEnabled=true;
bool g_sharedGraphicSource=false;
bool g_sharedExplicitDuration[SharedStatusCatalog::Slots]{};

// Preserve the overwritten entry instructions, continuing inside the original
// functions. No copied relative calls or guessed calling convention.
__declspec(naked) void __fastcall SharedNativeClear(unsigned) {
    __asm {
        push esi
        mov esi,ecx
        mov edx,esi
        push 04875C5h
        ret
    }
}
__declspec(naked) void __fastcall SharedNativeApply(int,unsigned) {
    __asm {
        push esi
        push edi
        mov esi,edx
        mov di,cx
        push 0487607h
        ret
    }
}
struct SharedStatusHost {
    static bool HasManager(){return *reinterpret_cast<void**>(0x760744)!=nullptr;}
    static bool Active(unsigned slot){return g_statusActive[slot]!=0;}
    static bool ShowExpired(){return g_sharedExpiredIconsEnabled;}
    static unsigned Group(unsigned slot){return SharedStatusBehavior::Groups[slot];}
    static unsigned OffIcon(unsigned slot){return SharedStatusBehavior::OffIcons[slot];}
    static void RestoreIcon(unsigned slot){g_dragonStatusRows[slot][0]=g_sharedStatusIcons[slot];}
    static void SetOffIcon(unsigned slot){g_dragonStatusRows[slot][0]=OffIcon(slot);}
    static void CancelFinish(unsigned slot){
        reinterpret_cast<void(__fastcall*)(void*,unsigned)>(0x4D6870)(
            reinterpret_cast<void*>(&SharedStatusFinish),slot);
    }
    static void CancelBlink(unsigned slot){
        reinterpret_cast<void(__fastcall*)(void*,unsigned)>(0x4D6870)(
            reinterpret_cast<void*>(0x4876E0),slot);
    }
    static void ClearNative(unsigned slot){
        g_sharedExplicitDuration[slot]=false;
        if(HasManager())SharedNativeClear(slot);
        else {
            // Scene shutdown may have destroyed the manager before a timer.
            CancelBlink(slot);
            reinterpret_cast<void(__fastcall*)(void*,unsigned)>(0x4D6870)(
                reinterpret_cast<void*>(&SharedStatusExpire),slot);
            g_statusActive[slot]=g_statusVisible[slot]=0;
        }
    }
    static void ApplyNative(int seconds,unsigned slot){SharedNativeApply(seconds,slot);}
    static void ExpireNative(unsigned slot){reinterpret_cast<void(__fastcall*)(unsigned)>(0x4875A0)(slot);}
    static void EndMessageNative(unsigned slot){reinterpret_cast<void(__fastcall*)(unsigned)>(0x4876E0)(slot);}
    static void AddExpiredNative(unsigned slot){
        auto manager=*reinterpret_cast<void**>(0x760744);
        g_statusActive[slot]=1;g_statusVisible[slot]=0;
        reinterpret_cast<void(__thiscall*)(void*,int,unsigned)>(0x49F8A0)(manager,-1,slot);
    }
    static void ScheduleFinish(unsigned slot,unsigned delay){
        reinterpret_cast<void(__fastcall*)(unsigned,void*,unsigned)>(0x4D65F0)(
            delay,reinterpret_cast<void*>(&SharedStatusFinish),slot);
    }
};
SharedStatusLifecycle<SharedStatusHost,SharedStatusCatalog::Slots> g_sharedStatusLifecycle;
void __fastcall SharedStatusClear(unsigned slot){g_sharedStatusLifecycle.Clear(slot);}
void __fastcall SharedStatusApply(int seconds,unsigned slot){
    // Original 487600 consumes CX, not ECX. Strength/dexterity receivers
    // preserve unrelated high bits in ESI before moving it to ECX.
    seconds&=65535;
    if(seconds>=32768)seconds-=65536;
    if(slot>=SharedStatusCatalog::Slots || slot==76 || (seconds<0 && seconds!=-1) || seconds>32767 ||
       !SharedStatusHost::HasManager())return;
    g_sharedStatusLifecycle.Apply(seconds,slot);
    if(slot<SharedStatusCatalog::Slots && g_statusActive[slot] && (seconds>0 || seconds==-1))
        g_sharedExplicitDuration[slot]=!g_sharedGraphicSource;
}
void __fastcall SharedStatusExpire(unsigned slot){g_sharedStatusLifecycle.Expire(slot);}
void __fastcall SharedStatusFinish(unsigned slot){g_sharedStatusLifecycle.Finish(slot);}
void __fastcall SharedOwnGraphic(unsigned graphic){
    graphic&=65535;
    for(const auto& row:SharedStatusBehavior::Graphics){
        if(row.graphic!=graphic)continue;
        // An explicit server duration outranks a visual effect's XML default.
        // Auto-derived states still refresh on repeated effects.
        if(g_sharedExplicitDuration[row.slot] && g_statusActive[row.slot] &&
           !g_sharedStatusLifecycle.Expired(row.slot))return;
        bool previous=g_sharedGraphicSource;g_sharedGraphicSource=true;
        SharedStatusApply(row.seconds,row.slot);g_sharedGraphicSource=previous;
        return;
    }
    bool previous=g_sharedGraphicSource;g_sharedGraphicSource=true;
    reinterpret_cast<void(__fastcall*)(unsigned)>(0x4AD700)(graphic);
    g_sharedGraphicSource=previous;
}
void AddSharedStatusLifecyclePatches(std::vector<NumericPatch>& patches){
    AddNumericRelativePatch(patches,0x4875C0,{0x56,0x8B,0xF1,0x8B,0xD6},0xE9,
        reinterpret_cast<void*>(&SharedStatusClear),"shared-status-clear");
    AddNumericRelativePatch(patches,0x487600,{0x56,0x57,0x8B,0xF2,0x66,0x8B,0xF9},0xE9,
        reinterpret_cast<void*>(&SharedStatusApply),"shared-status-apply");
    // Expiry scheduling and cancellation must name the SAME callback.
    for(auto address:{0x487632u,0x4875D2u})
        patches.push_back({reinterpret_cast<BYTE*>(address),DwordBytes(0x4875A0),
            DwordBytes(reinterpret_cast<DWORD>(&SharedStatusExpire)),"shared-status-expiry-callback"});
    // Original receiver already proved target-object == own-object at
    // 4ADEC0..4ADECE. Replace its status dispatch ONLY, not effect rendering.
    AddNumericRelativePatch(patches,0x4ADED3,{0xE8,0x28,0xF8,0xFF,0xFF},0xE8,
        reinterpret_cast<void*>(&SharedOwnGraphic),"shared-own-effect-status");
}
