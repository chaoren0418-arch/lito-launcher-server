#include "forced_combat.h"
#include <cassert>
#include <iostream>
int main() {
    for(unsigned char opcode:{0x17,0x18,0x7a}) {
        unsigned char p[]={11,0,opcode,42,0,0,0,1,128,2,128};
        auto v=ForcedCombatFrame(p,11,true,true,true);
        assert(v.size()==19 && v[0]==19 && v[13]==0x43 && v[15]==opcode);
        assert(ForcedCombatFrame(p,11,false,true,true).empty());
        assert(ForcedCombatFrame(p,11,true,false,true).empty());
        assert(ForcedCombatFrame(p,11,true,true,false).empty());
        p[3]=0;assert(ForcedCombatFrame(p,11,true,true,true).empty());
    }
    unsigned char magic[]={13,0,0x14,0,3,42,0,0,0,1,128,2,128};
    assert(ForcedCombatFrame(magic,13,true,true,true).size()==21);
    unsigned char heal[]={9,0,0x14,0,0,42,0,0,0};
    assert(ForcedCombatFrame(heal,9,true,true,true).size()==17);
    for(int skill:{5,51,58,60,63,69,116,118}) {
        heal[3]=static_cast<unsigned char>((skill-1)>>3);heal[4]=static_cast<unsigned char>((skill-1)&7);
        assert(ForcedCombatFrame(heal,9,true,true,true).empty());
    }
    magic[0]=12;assert(ForcedCombatFrame(magic,13,true,true,true).empty());
    std::cout<<"PASS Ctrl-only, foreground-process isolation, battle-only, exact attack/spell framing, zero-target and non-target spell exclusion\n";
}
