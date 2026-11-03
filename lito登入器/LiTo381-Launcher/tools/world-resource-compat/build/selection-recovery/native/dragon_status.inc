// Original LiTo has 77 status rows and two DWORD-aligned 80-byte flag arrays.
// Shared Tile.pak effectlist2.xml effect107 DRAGON_SKIN_S uses icon3080.
// Append Dragon Skin and nine illusion slots; relocate both flag arrays to 88 owned bytes.
// Do not repurpose any original icon or status group.
static DWORD g_dragonStatusRows[SharedStatusCatalog::Slots][5]{};
static BYTE g_statusVisible[96]{},g_statusActive[96]{};
static const int kIllusionSkills[]={201,204,206,209,211,214,216,217,219};
static const int kIllusionIcons[]={3123,3117,3097,3115,3127,3113,3119,3125,3111};
static int ExtendedStatusSlot(int skill) {
    if(skill==181)return 77;
    for(int i=0;i<9;++i)if(kIllusionSkills[i]==skill)return 78+i;
    return -1;
}
static constexpr int kDragonSkinSlot=77;
const char* __cdecl DragonStatusName(int slot) {
    if(slot==kDragonSkinSlot) return g_sharedExtraSkillNames[4];
    if(slot>=78 && slot<87)return g_sharedExtraSkillNames[kIllusionSkills[slot-78]-177];
    if(slot<0 || slot>=static_cast<int>(SharedStatusCatalog::Slots) || (slot>=77 && slot<87)) return "";
    DWORD text=g_dragonStatusRows[slot][3];
    return text<*reinterpret_cast<DWORD*>(0x834BC0)
        ? (*reinterpret_cast<char***>(0x834BC4))[text] : "";
}
__declspec(naked) void DragonStatusNameBridge() {
    __asm {
        push eax
        push ecx
        call DragonStatusName
        add esp, 4
        mov ecx, eax
        pop eax
        mov edx, 049FC18h
        jmp edx
    }
}
void __fastcall DragonSkinStatusPacket(const BYTE* packet,void*) {
    int slot=ExtendedStatusSlot(packet[3]);
    if(packet[3]==5)slot=4; // shared SHIELD; verified actual JAR writer
    else if(packet[3]==6)slot=6; // shared EARTH_SKIN

    if(slot<0) {
        reinterpret_cast<void (__fastcall*)(const BYTE*)>(0x4AFF30)(packet);
        return;
    }
    int seconds=packet[1] | (packet[2]<<8);
    if(seconds>32767 || !*reinterpret_cast<void**>(0x760744)) {
        Log("dragon skin status refused duration=%d manager=%p",seconds,*reinterpret_cast<void**>(0x760744));return;
    }
    // Native timer, blink, layout, tooltip and countdown are kept intact.
    reinterpret_cast<void (__fastcall*)(int,int)>(0x487600)(seconds,slot);
    Log("extended native status skill=%u seconds=%d slot=%d icon=%u",packet[3],seconds,slot,g_dragonStatusRows[slot][0]);
}
void AddDragonStatusPatches(std::vector<NumericPatch>& patches) {
    memcpy(g_dragonStatusRows,reinterpret_cast<void*>(0x6328D8),77*20);
    g_dragonStatusRows[kDragonSkinSlot][0]=3080;
    g_dragonStatusRows[kDragonSkinSlot][1]=1200;
    for(int i=0;i<9;++i)g_dragonStatusRows[78+i][0]=kIllusionIcons[i];
    for(unsigned i=0;i<SharedStatusCatalog::Slots;++i){g_dragonStatusRows[i][0]=g_sharedStatusIcons[i];g_dragonStatusRows[i][2]=SharedStatusBehavior::Groups[i];if(i>=87){g_dragonStatusRows[i][1]=SharedStatusBehavior::Durations[i];g_dragonStatusRows[i][3]=SharedStatusBehavior::StartMessages[i];g_dragonStatusRows[i][4]=SharedStatusBehavior::EndMessages[i];}}
    // Relocate both arrays together; slots above 79 must never overwrite adjacent globals.
    for(auto a:{0x46A307u,0x47E015u,0x47E380u,0x4875ECu,0x487655u,0x487691u,0x4876C6u,0x4876ECu,0x4876F7u,0x48772Cu})
        patches.push_back({reinterpret_cast<BYTE*>(a),DwordBytes(0x75AA78),DwordBytes(reinterpret_cast<DWORD>(g_statusVisible)),"extended-status-visible-array"});
    for(auto a:{0x46A318u,0x47E024u,0x47E391u,0x4875A2u,0x4875E6u,0x48765Bu,0x487697u,0x4876CCu,0x487702u})
        patches.push_back({reinterpret_cast<BYTE*>(a),DwordBytes(0x75AAC8),DwordBytes(reinterpret_cast<DWORD>(g_statusActive)),"extended-status-active-array"});

    for(auto a:{0x487614u,0x49FA38u})
        patches.push_back({reinterpret_cast<BYTE*>(a),DwordBytes(0x6328E0),DwordBytes(reinterpret_cast<DWORD>(&g_dragonStatusRows[0][2])),"dragon-status-category"});
    patches.push_back({reinterpret_cast<BYTE*>(0x487734),DwordBytes(0x6328E8),DwordBytes(reinterpret_cast<DWORD>(&g_dragonStatusRows[0][4])),"dragon-status-end-message"});
    patches.push_back({reinterpret_cast<BYTE*>(0x4A0171),DwordBytes(0x6328D8),DwordBytes(reinterpret_cast<DWORD>(g_dragonStatusRows)),"dragon-status-shared-icon"});
    // Clear all 88 owned bytes, including the appended slot, on scene reset.
    for(auto a:{0x46A300u,0x46A311u,0x47E00Eu,0x47E01Du,0x47E379u,0x47E38Au})
        patches.push_back({reinterpret_cast<BYTE*>(a),DwordBytes(19),DwordBytes(24),"dragon-status-clear-capacity"});
    for(auto a:{0x46A30Fu,0x46A324u,0x47E01Bu,0x47E02Au,0x47E388u,0x47E39Du})
        patches.push_back({reinterpret_cast<BYTE*>(a),{0xAA},{0x90},"dragon-status-clear-boundary"});
    AddNumericRelativePatch(patches,0x49FBF4,{0x8B,0x15,0xC0,0x4B,0x83,0x00},0xE9,
        reinterpret_cast<void*>(&DragonStatusNameBridge),"dragon-status-native-tooltip");
    AddNumericRelativePatch(patches,0x4B2E61,{0xE8,0xCA,0xD0,0xFF,0xFF},0xE8,
        reinterpret_cast<void*>(&DragonSkinStatusPacket),"dragon-status-packet-receiver");
}
#ifdef LITO_SOURCE_UI_TEST
bool VerifyDragonSkinStatus() {
    auto manager=*reinterpret_cast<BYTE**>(0x760744);
    bool owned=manager==nullptr;
    if(owned) {
        manager=static_cast<BYTE*>(reinterpret_cast<void*(__cdecl*)(std::size_t)>(0x4F118C)(0x18));
        if(!manager)return false;
        reinterpret_cast<void(__thiscall*)(void*)>(0x419D10)(manager);
        *reinterpret_cast<DWORD*>(manager)=0x4F8F20;
        *reinterpret_cast<DWORD*>(manager+0x14)=0;
        *reinterpret_cast<BYTE**>(0x760744)=manager;
    }
    auto receive=reinterpret_cast<void(__fastcall*)(const BYTE*,void*)>(0x4B2E66+*reinterpret_cast<LONG*>(0x4B2E62));
    BYTE packet[]={0x6D,0xB0,0x04,181};
    auto count=reinterpret_cast<int*>(manager+0x14);int baseline=*count;
    BYTE legacy[]={0x6D,0xA0,0x00,4};receive(legacy,nullptr);
    bool legacyOk=*count==baseline+1 && g_statusActive[6]==1;
    int before=*count;
    receive(packet,nullptr);
    bool ok=*count==before+1 && g_statusActive[kDragonSkinSlot]==1;
    auto first=reinterpret_cast<void*(__thiscall*)(void*)>(0x419DB0);
    auto next=reinterpret_cast<void*(__thiscall*)(void*)>(0x419E00);
    bool drawn=false;
    for(auto node=static_cast<DWORD*>(first(manager));node;node=static_cast<DWORD*>(next(manager))) {
        if(node[4]!=kDragonSkinSlot)continue;
        auto ui=reinterpret_cast<BYTE*>(node[0]);
        ok=ok && *reinterpret_cast<int*>(ui+0x4C)==1200
            && strcmp(*reinterpret_cast<char**>(ui+0x54),g_sharedExtraSkillNames[4])==0;
        reinterpret_cast<void(__thiscall*)(void*)>(0x4A00D0)(ui);drawn=true;
    }
    receive(packet,nullptr);ok=ok && *count==before+1;
    packet[1]=packet[2]=0;receive(packet,nullptr);
    ok=ok && *count==before && g_statusActive[kDragonSkinSlot]==0;
    legacy[1]=0;receive(legacy,nullptr);
    ok=ok && legacyOk && *count==baseline && g_statusActive[6]==0;
    for(int i=0;i<9;++i) {
        BYTE buff[]={0x6D,64,0,static_cast<BYTE>(kIllusionSkills[i])};
        receive(buff,nullptr);bool found=false;
        for(auto node=static_cast<DWORD*>(first(manager));node;node=static_cast<DWORD*>(next(manager))) {
            if(node[4]!=78+i)continue;
            auto ui=reinterpret_cast<BYTE*>(node[0]);
            found=strcmp(*reinterpret_cast<char**>(ui+0x54),g_sharedExtraSkillNames[kIllusionSkills[i]-177])==0;
            reinterpret_cast<void(__thiscall*)(void*)>(0x4A00D0)(ui);
        }
        ok=ok && found && g_statusActive[78+i]==1;
    }
    ok=ok && *count==baseline+9;
    for(int i=0;i<9;++i) { BYTE buff[]={0x6D,0,0,static_cast<BYTE>(kIllusionSkills[i])};receive(buff,nullptr);ok=ok && g_statusActive[78+i]==0; }
    ok=ok && *count==baseline;
    Log("fixture nine illusion native status icons/render/tooltips/remove passed=%d",ok);
    Log("fixture dragon native status add/refresh/remove=%d drawn=%d tooltip/shared-icon=3080",ok,drawn);
    if(owned) {
        auto vtable=*reinterpret_cast<DWORD**>(manager);
        reinterpret_cast<void(__thiscall*)(void*,int)>(vtable[0])(manager,1);
        *reinterpret_cast<void**>(0x760744)=nullptr;
    }
    return ok && drawn;
}
#endif
