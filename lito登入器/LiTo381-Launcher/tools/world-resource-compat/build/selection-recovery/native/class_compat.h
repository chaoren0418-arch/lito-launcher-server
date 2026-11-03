#pragma once
#include <cstddef>
#include <cstdint>

namespace LitoClasses {
constexpr int Count = 7;
constexpr int LegacyPngCount = 810;
constexpr int PngCount = 1128;
struct Animation { int first, count, milliseconds, blend, x, y; };
static_assert(sizeof(Animation) == 24, "native animation ABI");
// TW13081901 0x7770B0: male/female Dragon Knight, then male/female Illusionist.
// Each entry retains its original idle, hover, select and selected-loop timing.
inline const Animation ExtraPortraits[4][4] = {
    {{906,1,83,0,-12,0},{907,1,83,1,-12,0},{841,43,75,1,-12,0},{884,22,83,1,-12,0}},
    {{966,1,83,0,-12,0},{967,1,83,1,-12,0},{908,33,83,1,-12,0},{941,25,83,1,-12,0}},
    {{1037,1,83,0,-12,0},{1038,1,83,1,-12,0},{968,45,83,1,-12,0},{1013,24,83,1,-12,0}},
    {{1126,1,83,0,-12,0},{1127,1,83,1,-12,0},{1039,63,83,1,-12,0},{1102,24,83,1,-12,0}}
};
inline bool Valid(int type, int sex) { return type >= 0 && type < Count && (sex == 0 || sex == 1); }
inline int TextId(int type, int sex) {
    constexpr int ids[Count][2]={{0,1},{2,3},{6,7},{4,5},{8,9},{1302,1303},{1304,1305}};
    return Valid(type,sex) ? ids[type][sex] : -1;
}
inline int LegacyLobbyImage(int type,int sex) {
    constexpr int ids[5][2]={{6640,6642},{6652,6654},{6648,6650},{6644,6646},{6656,6658}};
    return Valid(type,sex) && type<5 ? ids[type][sex] : -1;
}
// Keep the original 61x73 frame. Fit the shared character portrait inside it,
// using the same RGB555/zero-mask bitmap representation as the native blitter.
inline bool ComposeLobbyPortrait(std::uint16_t* out,const std::uint16_t* frame,const std::uint16_t* portrait) {
    if(!out || !frame || !portrait || frame[0]!=61 || frame[1]!=73 || portrait[0]!=160 || portrait[1]!=270) return false;
    constexpr int words=4+61*73;
    for(int i=0;i<words;++i) out[i]=frame[i];
    // Preserve the surrounding gold frame, replace the old royal figure.
    const auto background=frame[4+20*61+4];
    for(int y=4;y<69;++y) for(int x=4;x<57;++x) out[4+y*61+x]=background;
    constexpr int width=38,height=65,left=(61-width)/2;
    for(int y=0;y<height;++y) for(int x=0;x<width;++x) {
        const auto pixel=portrait[4+(y*270/height)*160+x*160/width];
        if(pixel!=portrait[3]) out[4+(y+4)*61+left+x]=pixel;
    }
    return true;
}
// New client PNX encoding, verified against TW13081901 0x75A8E0.
// Decode only the temporary archive buffer; the shared PAK is never rewritten.
inline bool DecodePnx(unsigned char* bytes, std::size_t size) {
    if (!bytes || size < 8 || bytes[0]!=0x89 || bytes[1]!='P' || bytes[2]!='N' || bytes[3]!='X') return false;
    bytes[3]='G';
    for (std::size_t i=size-1;i>4;--i) bytes[i]^=bytes[i-1]^0x52;
    return true;
}
}
