#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <cstdint>
#include <cstdio>
#include <cassert>
#include <cstring>
#include <vector>
#include "character_pages.h"
bool IsVerifiedTournamentClient() { return true; }
void Log(const char*, ...) {}
HWND g_window{};
alignas(16) BYTE testMemory[0x160000]{};
DWORD testRoot[4]{};
template<typename T> volatile T& SelectorValue(std::uintptr_t a) {
    return *reinterpret_cast<T*>(testMemory + a - 0x620000);
}
unsigned nativeCount{}, nativeRows{}, profiles{}, batches{};
std::vector<unsigned> shown;
void __fastcall nativeRow(void* packet, void*) {
    if (SelectorValue<WORD>(0x64AFBC)) { ++profiles; return; }
    assert(nativeCount <= 4 && nativeRows < nativeCount);
    const auto p = static_cast<BYTE*>(packet);
    const auto address = 0x62FAA0 + nativeRows * 0xB4;
    std::memset(testMemory + address - 0x620000, p[1], 0xB4);
    shown.push_back(p[1]); ++nativeRows;
}
std::uintptr_t nativeCall(std::uintptr_t a) { assert(a == 0x4A7550); return reinterpret_cast<std::uintptr_t>(&nativeRow); }
#define SELECTOR_CALL(address,type) reinterpret_cast<type>(nativeCall(address))
void __fastcall ReceiveSelectorCharacterCount(void* packet, void*) {
    nativeCount = static_cast<BYTE*>(packet)[1]; assert(nativeCount <= 4);
    nativeRows = 0; shown.clear(); ++batches;
    std::memset(testMemory + 0xFAA0, 0, 4*0xB4);
}
#include "character_pages.inc"
std::vector<BYTE> row(unsigned id) {
    std::vector<BYTE> result{0xCB, static_cast<BYTE>('A'+id), 0, 0};
    result.resize(result.size() + LitoCharacterPages::FixedBytes, 0);
    return result;
}
void canary() { for (unsigned i = 0; i < 720; ++i) assert(testMemory[0xFD70+i] == 0xA5); }
int main() {
    std::memset(testMemory + 0xFD70, 0xA5, 720);
    testRoot[3] = 7; SelectorValue<DWORD>(0x75F6E8) = reinterpret_cast<DWORD>(testRoot);
    SelectorValue<DWORD>(0x64BBD4) = 4;
    for (unsigned count = 0; count <= 8; ++count) {
        BYTE packet[]{3,static_cast<BYTE>(count)}; ReceivePagedCharacterCount(packet,nullptr);
        for (unsigned i = 0; i < count; ++i) {
            auto p = row(i); ReceivePagedCharacterRow(p.data(),nullptr);
            if (i+1 < count) assert(nativeRows == 0);
        }
        assert(nativeRows == (count < 4 ? count : 4)); canary();
        if (count > 4) {
            assert(TurnCharacterPage(1)); assert(nativeRows == count-4);
            assert(shown.front() == 'E'); canary();
            assert(TurnCharacterPage(-1)); assert(nativeRows == 4 && shown.front() == 'A'); canary();
            testRoot[3] = 40; assert(!TurnCharacterPage(1)); testRoot[3] = 7;
            SelectorValue<WORD>(0x64AFBC) = 1;
            assert(!TurnCharacterPage(1)); auto p = row(0); ReceivePagedCharacterRow(p.data(),nullptr);
            SelectorValue<WORD>(0x64AFBC) = 0;
        }
    }
    assert(profiles == 4);
    LitoCharacterPages::Directory d;
    assert(!d.Begin(9)); assert(!d.Receiving());
    assert(d.Begin(1)); auto p = row(0);
    assert(!d.Add(p.data(),p.size()-1)); assert(d.Add(p.data(),p.size()));
    assert(!d.Add(p.data(),p.size()));
    assert(d.Begin(1)); std::vector<BYTE> bad(128,'X');bad[0]=0xCB;
    assert(!d.Add(bad.data(),bad.size()));
    // Maximum byte lengths include multibyte CP950 names; retain exact bytes.
    assert(d.Begin(1)); std::vector<BYTE> longest{0xCB};
    longest.insert(longest.end(),27,0xA4);longest.push_back(0);
    longest.insert(longest.end(),16,0xA5);longest.push_back(0);
    longest.resize(longest.size()+LitoCharacterPages::FixedBytes,0x7F);
    assert(longest.size()==LitoCharacterPages::MaxPacket);
    assert(d.Add(longest.data(),longest.size()));
    assert(d.At(0).size==longest.size() && !std::memcmp(d.At(0).bytes.data(),longest.data(),longest.size()));
    assert(d.Begin(0) && d.Complete() && d.Pages()==0);
    std::puts("PASS: 0..8 characters, two pages, refresh, native four-slot/portrait canary, handoff routing, phase guards, malformed rows, cache reset");
}
