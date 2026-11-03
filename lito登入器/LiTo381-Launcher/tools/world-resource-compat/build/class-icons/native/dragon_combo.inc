// Reference TW13081901: effect handler 0x53190B/0x531931 sets burst
// counters 4 (4394) and 3 (7020); duration branch 0x5AEA2A divides the
// listed attack actions by three. LiTo only had the bow action21 branch.
static DWORD g_comboEffectContinuation=0x4ADE15;
static DWORD g_comboDurationContinuation=0x4E8899;
__declspec(naked) void DragonComboEffectBridge() {
    __asm {
        cmp word ptr [ebp-10h], 112Ah
        je triple
        cmp word ptr [ebp-10h], 1B6Ch
        jne done
        mov dword ptr [esi+11Ch], 3
        jmp done
    triple:
        mov dword ptr [esi+11Ch], 4
    done:
        jmp dword ptr [g_comboEffectContinuation]
    }
}
__declspec(naked) void DragonComboDurationBridge() {
    __asm {
        mov eax, dword ptr [ecx+11Ch]
        test eax, eax
        jle done
        cmp esi, 15h
        je faster
        cmp esi, 5
        je faster
        cmp esi, 0Ch
        je faster
        cmp esi, 33h
        je faster
        cmp esi, 19h
        je faster
        cmp esi, 54h
        je faster
        cmp esi, 2Fh
        jne done
    faster:
        // Same signed ceil(duration/3) sequence as LiTo's triple-arrow branch.
        add edx, 2
        mov eax, 55555556h
        imul edx
        mov ecx, edx
        shr ecx, 1Fh
        add edx, ecx
    done:
        jmp dword ptr [g_comboDurationContinuation]
    }
}
void AddDragonComboPatches(std::vector<NumericPatch>& patches) {
    AddNumericRelativePatch(patches,0x4ADE03,{0x66,0x81,0x7D,0xF0,0x2A,0x11},0xE9,
        reinterpret_cast<void*>(&DragonComboEffectBridge),"shared-foe-slayer-burst-marker");
    AddNumericRelativePatch(patches,0x4E8879,{0x8B,0x81,0x1C,0x01,0x00,0x00},0xE9,
        reinterpret_cast<void*>(&DragonComboDurationBridge),"shared-melee-burst-duration");
}
#ifdef LITO_SOURCE_UI_TEST
__declspec(naked) void ComboEffectFixtureReturn() {
    __asm {
        pop esi
        mov esp,ebp
        pop ebp
        ret 8
    }
}
__declspec(naked) void __stdcall ComboEffectFixture(int effect,void* entity) {
    __asm {
        push ebp
        mov ebp,esp
        sub esp,10h
        push esi
        mov esi,dword ptr [ebp+0Ch]
        mov eax,dword ptr [ebp+8]
        mov word ptr [ebp-10h],ax
        mov eax,04ADE03h
        jmp eax
    }
}
__declspec(naked) void ComboDurationFixtureReturn() {
    __asm {
        mov eax,edx
        pop esi
        pop ebp
        ret 0Ch
    }
}
__declspec(naked) int __stdcall ComboDurationFixture(void* entity,int action,int duration) {
    __asm {
        push ebp
        mov ebp,esp
        push esi
        mov ecx,dword ptr [ebp+8]
        mov esi,dword ptr [ebp+0Ch]
        mov edx,dword ptr [ebp+10h]
        mov eax,04E8879h
        jmp eax
    }
}
bool VerifyDragonCombo() {
    DWORD entity[80]{};auto& count=entity[0x11C/4];
    g_comboEffectContinuation=reinterpret_cast<DWORD>(&ComboEffectFixtureReturn);
    g_comboDurationContinuation=reinterpret_cast<DWORD>(&ComboDurationFixtureReturn);
    ComboEffectFixture(7020,entity);bool ok=count==3;
    for(int action:{21,5,12,51,25,84,47}) {
        int accelerated=ComboDurationFixture(entity,action,800);
        Log("fixture combo action=%d counter=%u duration800=%d",action,count,accelerated);
        ok=ok && accelerated==267;
    }
    for(int action:{0,1,18,19,24,83})ok=ok && ComboDurationFixture(entity,action,800)==800;
    count=0;ok=ok && ComboDurationFixture(entity,25,800)==800;
    ComboEffectFixture(4394,entity);ok=ok && count==4 && ComboDurationFixture(entity,21,800)==267;
    ComboEffectFixture(6509,entity);ok=ok && count==4;
    // Exhausted native counter restores ordinary speed without a global setting.
    count=0;ok=ok && ComboDurationFixture(entity,21,800)==800;
    g_comboEffectContinuation=0x4ADE15;g_comboDurationContinuation=0x4E8899;
    Log("fixture shared combo effect7020=3 triple4394=4 normal-restored=1 passed=%d",ok);
    return ok;
}
#endif
