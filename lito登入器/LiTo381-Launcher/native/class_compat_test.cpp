#include "class_compat.h"
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <initializer_list>
#define CHECK(c) do { if(!(c)) {std::printf("FAIL line=%d\n",__LINE__);std::exit(1);} } while(0)
int main() {
    const int expected[14]={0,1,2,3,6,7,4,5,8,9,1302,1303,1304,1305};
    for(int type=0;type<7;++type) for(int sex=0;sex<2;++sex)
        CHECK(LitoClasses::TextId(type,sex)==expected[type*2+sex]);
    for(int type:{-1,7,255}) CHECK(LitoClasses::TextId(type,0)==-1);
    CHECK(LitoClasses::TextId(5,-1)==-1);CHECK(LitoClasses::TextId(6,2)==-1);
    // Four contiguous, complete native select/loop sequences, including their
    // hover frames and the disabled-idle +1 access used by LiTo's renderer.
    int next=841;
    for(const auto& sex:LitoClasses::ExtraPortraits) {
        CHECK(sex[2].first==next);
        CHECK(sex[2].first+sex[2].count==sex[3].first);
        CHECK(sex[3].first+sex[3].count==sex[0].first);
        CHECK(sex[1].first==sex[0].first+1);
        for(const auto& s:sex) CHECK(s.first>=0 && s.first+s.count<=LitoClasses::PngCount && s.milliseconds>0);
        next=sex[1].first+1;
    }
    CHECK(next==LitoClasses::PngCount);
    CHECK(LitoClasses::LegacyLobbyImage(0,0)==6640);
    CHECK(LitoClasses::LegacyLobbyImage(4,1)==6658);
    CHECK(LitoClasses::LegacyLobbyImage(5,0)==-1);
    CHECK(LitoClasses::LegacyLobbyImage(2,2)==-1);
    std::uint16_t frame[4+61*73]{61,73,0,0x72ee},portrait[4+160*270]{160,270,0,0},out[4+61*73]{};
    for(int i=4;i<4+61*73;++i) frame[i]=0x1234;
    for(int i=4;i<4+160*270;++i) portrait[i]=0x5678;
    CHECK(LitoClasses::ComposeLobbyPortrait(out,frame,portrait));
    CHECK(out[4]==frame[4] && out[4+72*61+60]==frame[4+72*61+60]);
    CHECK(out[4+40*61+30]==0x5678 && out[4+40*61+5]==0x1234);
    portrait[0]=0;CHECK(!LitoClasses::ComposeLobbyPortrait(out,frame,portrait));
    unsigned char pnx[]={0x89,0x50,0x4e,0x58,0x0d,0x55,0x1d,0x45,0x17,0x45,0x17,0x48,0x53,0x49,0x5f,0x5f};
    const unsigned char png[]={0x89,0x50,0x4e,0x47,0x0d,0x0a,0x1a,0x0a,0,0,0,0x0d,0x49,0x48,0x44,0x52};
    CHECK(LitoClasses::DecodePnx(pnx,sizeof(pnx)));CHECK(!memcmp(pnx,png,sizeof(png)));
    CHECK(!LitoClasses::DecodePnx(pnx,sizeof(pnx)));CHECK(!memcmp(pnx,png,sizeof(png)));
    CHECK(!LitoClasses::DecodePnx(nullptr,20));CHECK(!LitoClasses::DecodePnx(pnx,3));
    std::puts("PASS: 14 class labels, invalid inputs, complete native portrait ranges, PNX decode and idempotence.");
}
