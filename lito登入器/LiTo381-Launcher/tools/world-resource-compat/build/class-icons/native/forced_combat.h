#pragma once
#include <cstdint>
#include <vector>

// Paired with ForcedCombat.java. The native layouts have no Ctrl bit.
inline std::uint32_t ForcedCombatTarget(const unsigned char* p,int n) {
    if (!p || n<7) return 0;
    int offset;
    if ((p[0]==0x17 || p[0]==0x18 || p[0]==0x7a) && n==9) offset=1;
    else if (p[0]==0x14 && (n==7 || n==11)) {
        int skill=(p[1]<<3)+p[2]+1;
        if(p[2]>7 || skill>220 || skill==5 || skill==51 || skill==58
            || skill==60 || skill==63 || skill==69 || skill==116 || skill==118) return 0;
        offset=3;
    } else return 0;
    return std::uint32_t(p[offset]) | (std::uint32_t(p[offset+1])<<8)
        | (std::uint32_t(p[offset+2])<<16) | (std::uint32_t(p[offset+3])<<24);
}
inline std::vector<unsigned char> ForcedCombatFrame(const unsigned char* frame,int length,
                                                   bool ctrl,bool ownedForeground,bool battle) {
    if (!ctrl || !ownedForeground || !battle || !frame || length<9 || length>13
        || (int(frame[0])|(int(frame[1])<<8))!=length
        || !ForcedCombatTarget(frame+2,length-2)) return {};
    std::vector<unsigned char> out(frame,frame+length);
    const unsigned char trailer[]={0x4c,0x46,0x43,0x31,frame[2],1,0x67,0xa5};
    out.insert(out.end(),trailer,trailer+8);
    out[0]=static_cast<unsigned char>(out.size());out[1]=0;
    return out;
}
