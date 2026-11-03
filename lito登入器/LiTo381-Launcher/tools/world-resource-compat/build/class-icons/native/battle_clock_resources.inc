#ifndef CLOCK_ADDRESS
#define CLOCK_ADDRESS(address) (address)
#endif
// Same native image table and blitter used by sub_45F4A0. Long durations
// extend to H:MM:SS instead of indexing past the native two-digit minutes.
void DrawBattleClockImage(DWORD index, int x, int y) {
    const auto table = *reinterpret_cast<std::uintptr_t*>(CLOCK_ADDRESS(0x00834BD4));
    if (!table || index >= 0x1EC2) return;
    auto object = *reinterpret_cast<std::uintptr_t*>(table + index * 4);
    if (!object) return;
    auto pixels = *reinterpret_cast<void**>(object + 4);
    if (!pixels) {
        auto vtable = *reinterpret_cast<std::uintptr_t*>(object);
        reinterpret_cast<void (__thiscall*)(void*)>(
            *reinterpret_cast<std::uintptr_t*>(vtable))(reinterpret_cast<void*>(object));
        pixels = *reinterpret_cast<void**>(object + 4);
    }
    if (!pixels) return;
    auto surface = *reinterpret_cast<void**>(CLOCK_ADDRESS(0x006476D0));
    if (!surface) return;
    reinterpret_cast<void (__cdecl*)(void*,void*,int,int)>(CLOCK_ADDRESS(0x004BA220))(pixels,surface,x,y);
}
void DrawExtendedBattleClock(DWORD remaining, int yOffset = 0) {
    char value[32]{};
    sprintf_s(value,"%lu:%02lu:%02lu",remaining/3600,(remaining/60)%60,remaining%60);
    const int originX = *reinterpret_cast<int*>(CLOCK_ADDRESS(0x0075A998));
    const int originY = *reinterpret_cast<int*>(CLOCK_ADDRESS(0x0075A99C));
    int x = originX + 320 - static_cast<int>(std::strlen(value))*17/2;
    for (char ch : std::string(value)) {
        DrawBattleClockImage(ch == ':' ? 0x12FE : 0x12F4 + ch - '0',x,originY+yOffset+15);
        x += 17;
    }
}
void DrawRaceRemainingClock(DWORD remaining) {
    if (remaining > 5999) { DrawExtendedBattleClock(remaining,40); return; }
    const int x = *reinterpret_cast<int*>(CLOCK_ADDRESS(0x0075A998));
    const int y = *reinterpret_cast<int*>(CLOCK_ADDRESS(0x0075A99C))+40;
    DrawBattleClockImage(0x12F2,x+276,y);
    const DWORD digit = remaining < 60 ? 0x12FF : 0x12F4;
    const DWORD colon = remaining < 60 ? 0x1309 : 0x12FE;
    DrawBattleClockImage(digit + remaining/600,x+297,y+15);
    DrawBattleClockImage(digit + (remaining/60)%10,x+314,y+15);
    DrawBattleClockImage(colon,x+331,y+15);
    DrawBattleClockImage(digit + (remaining%60)/10,x+348,y+15);
    DrawBattleClockImage(digit + remaining%10,x+365,y+15);
}
