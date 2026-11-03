// LiTo packet-box opcode is 0x7B. Never redirect 0xFA (LiTo clan).
// The paired candidate producer sends 56 as seconds+zero subtype, and 154
// as seconds+DWORD graphic. A zero duration removes the identified state.
void __fastcall SharedPacketBox(const BYTE* packet,void*) {
    if(!packet)return;
    const unsigned code=packet[1];
    if(code!=34 && code!=56 && code!=154) {
        reinterpret_cast<void(__fastcall*)(const BYTE*)>(0x4B07C0)(packet);
        return;
    }
    if(*reinterpret_cast<DWORD*>(0x64BBD4)!=4 || !SharedStatusHost::HasManager())return;
    const unsigned seconds=packet[2] | (packet[3]<<8);
    if(code==34){
        // Both native clients retain the most recently selected inventory
        // graphic. TW chooses the alternative blue-potion row for2465.
        if(seconds)SharedStatusApply(static_cast<short>(seconds),
            *reinterpret_cast<int*>(0x647EC0)==2465?94:38);
        return; // Native blue-potion zero means no update, not removal.
    }
    if(seconds>32767)return;
    if(code==56) {
        // Existing LiTo producer historically omitted TW's subtype word.
        // Keep the current supported generic doll contract; do not read
        // beyond its four bytes. The candidate producer emits an explicit0.
        if(seconds==0){SharedStatusClear(87);SharedStatusClear(88);}
        else SharedStatusApply(static_cast<int>(seconds),87);
        return;
    }
    const unsigned graphic=unsigned(packet[4]) | (unsigned(packet[5])<<8) |
                           (unsigned(packet[6])<<16) | (unsigned(packet[7])<<24);
    // graphic is a lookup key, not a TBT icon number.
    for(const auto& row:SharedStatusBehavior::PacketGraphics){
        if(row.graphic==graphic){
            SharedStatusApply(static_cast<int>(seconds),row.slot);
            return;
        }
    }
    Log("shared packet-box154 has no qualified XML binding graphic=%u",graphic);
}
void __fastcall SharedBravePacket(const BYTE* packet) {
    // Retain actor state and existing branches. TW 534ED2..534F06 adds
    // effect110 for mode6; 534DD1..534E18 clears it on mode0.
    reinterpret_cast<void(__fastcall*)(const BYTE*)>(0x4AF9F0)(packet);
    DWORD objectId=SharedStatusCatalog::Word(packet+1);
    void* actor=reinterpret_cast<void*(__fastcall*)(DWORD)>(0x4E8160)(objectId);
    if(!actor || actor!=*reinterpret_cast<void**>(0x834EE4))return;
    if(packet[5]==6)SharedStatusApply(packet[6]|(packet[7]<<8),95);
    else if(packet[5]==0)SharedStatusClear(95);
}
void AddSharedPacketBoxPatches(std::vector<NumericPatch>& patches) {
    AddNumericRelativePatch(patches,0x4B2ED9,{0xE8,0xE2,0xD8,0xFF,0xFF},0xE8,
        reinterpret_cast<void*>(&SharedPacketBox),"shared-packet-box-receiver");
    AddNumericRelativePatch(patches,0x4B2DF7,{0xE8,0xF4,0xCB,0xFF,0xFF},0xE8,
        reinterpret_cast<void*>(&SharedBravePacket),"shared-brave-status-receiver");
}
