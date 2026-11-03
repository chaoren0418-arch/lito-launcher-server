#include "forced_combat.h"
void __fastcall SendWithForcedCombat(BYTE* frame,int length,int encrypt) {
    DWORD foregroundPid{};
    GetWindowThreadProcessId(GetForegroundWindow(),&foregroundPid);
    const bool ctrl=(GetAsyncKeyState(VK_CONTROL)&0x8000)!=0;
    auto extended=ForcedCombatFrame(frame,length,ctrl,
        foregroundPid==GetCurrentProcessId(),
        *reinterpret_cast<volatile DWORD*>(0x64af98)==4);
    auto send=reinterpret_cast<void (__fastcall*)(BYTE*,int,int)>(0x4c6770);
    if(extended.empty()) send(frame,length,encrypt);
    else send(extended.data(),static_cast<int>(extended.size()),encrypt);
}
void AddForcedCombatPatch(std::vector<NumericPatch>& patches) {
    // sub_4C6AE0 has finished serializing the plaintext length-prefixed frame.
    // Wrap its single send call; the original cipher still encrypts once.
    AddNumericRelativePatch(patches,0x4c6b27,{0xe8,0x44,0xfc,0xff,0xff},0xe8,
        reinterpret_cast<void*>(&SendWithForcedCombat),"explicit-ctrl-combat-intent");
}
