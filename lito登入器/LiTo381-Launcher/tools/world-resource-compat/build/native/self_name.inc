// LiTo's world renderer has a separate unconditional self-name pass.
// Use the same hovered actor (75A98C) as the ordinary native name pass.
static DWORD g_selfNameShow=0x489E59,g_selfNameHide=0x489F30;
__declspec(naked) void HoverSelfNameBridge() {
    __asm {
        mov ecx,dword ptr ds:[0834EE4h]
        cmp ecx,dword ptr ds:[075A98Ch]
        jne hidden
        test ecx,ecx
        jz hidden
        jmp dword ptr [g_selfNameShow]
    hidden:
        jmp dword ptr [g_selfNameHide]
    }
}
void AddSelfNamePatch(std::vector<NumericPatch>& patches) {
    AddNumericRelativePatch(patches,0x489E53,{0x8B,0x0D,0xE4,0x4E,0x83,0x00},0xE9,
        reinterpret_cast<void*>(&HoverSelfNameBridge),"self-name-hover-only");
}

#ifdef LITO_SOURCE_UI_TEST
__declspec(naked) void NameShownFixture() {__asm {mov eax,1
 ret}}
__declspec(naked) void NameHiddenFixture() {__asm {xor eax,eax
 ret}}
bool VerifySelfNameHover() {
    auto self=reinterpret_cast<DWORD*>(0x834EE4),hover=reinterpret_cast<DWORD*>(0x75A98C);
    DWORD oldSelf=*self,oldHover=*hover;
    g_selfNameShow=reinterpret_cast<DWORD>(&NameShownFixture);g_selfNameHide=reinterpret_cast<DWORD>(&NameHiddenFixture);
    auto branch=reinterpret_cast<int(__cdecl*)()>(0x489E53);bool ok=true;
    *self=0x1234;*hover=0;ok=branch()==0 && ok;
    *hover=0x5678;ok=branch()==0 && ok;
    *hover=*self;ok=branch()==1 && ok;
    *self=*hover=0;ok=branch()==0 && ok;
    *self=oldSelf;*hover=oldHover;g_selfNameShow=0x489E59;g_selfNameHide=0x489F30;
    Log("fixture self-name installed branch absent/other/self/null passed=%d",ok);return ok;
}
#endif
