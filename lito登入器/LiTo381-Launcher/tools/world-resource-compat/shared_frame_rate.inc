// TW5AE900 frame-duration behavior absent from the older LiTo receiver.
// Preserve the native table reader, own-state decoder, burst and other rates.
__declspec(naked) int __fastcall SharedNativeFrameDuration(void*,void*,int,int) {
    __asm {
        push ebp
        mov ebp,esp
        push ecx
        mov eax,dword ptr [075AA48h]
        push 04E8759h
        ret
    }
}
static bool SharedMovementAction(int action) {
    switch(action){
    case 0:case 4:case 11:case 20:case 83:case 24:
    case 40:case 46:case 50:case 54:case 58:case 62:return true;
    default:return false;
    }
}
__declspec(naked) int __fastcall SharedNativeActionDuration(void*,void*,int) {
    __asm {
        push ebp
        mov ebp,esp
        movsx edx,word ptr [ecx+14h]
        push 04E8637h
        ret
    }
}
// Indirect ABI boundary: the compiler cannot infer preserved registers from
// a short naked gateway whose continuation is external native machine code.
static int (__fastcall* volatile g_sharedActionGateway)(void*,void*,int)=&SharedNativeActionDuration;
static int (__fastcall* volatile g_sharedFrameGateway)(void*,void*,int,int)=&SharedNativeFrameDuration;
int __fastcall SharedActionDuration(void* actor,void*,int action) {
    int duration=g_sharedActionGateway(actor,nullptr,action);
    // TW5AE681 applies mode6 to the complete action interval.
    if(reinterpret_cast<const BYTE*>(actor)[0x21]==6)duration=duration*3/4;
    return duration;
}
int __fastcall SharedFrameDuration(void* actor,void*,int action,int frame) {
    int duration=g_sharedFrameGateway(actor,nullptr,action,frame);
    // TW5AEB72 uses current actor action, including non-movement casts.
    if(reinterpret_cast<const BYTE*>(actor)[0x21]==6 &&
       !SharedMovementAction(static_cast<signed char>(reinterpret_cast<const BYTE*>(actor)[0x10])))
        duration=duration*3/4;
    return duration;
}
__declspec(naked) void SharedHasteWalkActionBridge() {
    __asm {
        cmp esi,3Eh
        je walk
        cmp esi,53h
        je walk
        push 04E8935h
        ret
    walk:
        push 04E8919h
        ret
    }
}
__declspec(naked) void SharedBraveWalkActionBridge() {
    __asm {
        cmp esi,3Eh
        je walk
        cmp esi,53h
        je walk
        push 04E89DBh
        ret
    walk:
        push 04E89ACh
        ret
    }
}
void AddSharedFrameRatePatches(std::vector<NumericPatch>& patches) {
    AddNumericRelativePatch(patches,0x4E8630,{0x55,0x8B,0xEC,0x0F,0xBF,0x51,0x14},0xE9,
        reinterpret_cast<void*>(&SharedActionDuration),"shared-bloodlust-action-duration");
    AddNumericRelativePatch(patches,0x4E8750,{0x55,0x8B,0xEC,0x51,0xA1,0x48,0xAA,0x75,0x00},0xE9,
        reinterpret_cast<void*>(&SharedFrameDuration),"shared-bloodlust-frame-duration");
    AddNumericRelativePatch(patches,0x4E8914,{0x83,0xFE,0x3E,0x75,0x1C},0xE9,
        reinterpret_cast<void*>(&SharedHasteWalkActionBridge),"shared-haste-chain-walk");
    AddNumericRelativePatch(patches,0x4E89A7,{0x83,0xFE,0x3E,0x75,0x2F},0xE9,
        reinterpret_cast<void*>(&SharedBraveWalkActionBridge),"shared-brave-chain-walk");
}
