#include "character_pages.h"
static LitoCharacterPages::Directory g_characterPages;
static unsigned g_characterPageCaption{};
static wchar_t g_characterBaseCaption[256]{};

__declspec(noinline) void __fastcall InvokeNativeCharacterRow(void* packet, void*) {
    const auto receiver = SELECTOR_CALL(0x4A7550, void (__fastcall*)(void*,void*));
    // sub_4A7550 supplies one fewer vararg than its trailing checksum format
    // consumes: that destination is its saved caller ESI. The native dispatch
    // at 4B3093 has ESI=packet. Reproduce that hidden ABI and preserve our ESI.
    __asm {
        push esi
        mov esi, packet
        mov ecx, esi
        xor edx, edx
        call receiver
        pop esi
    }
}

bool IsCharacterPagingPage() {
    if (!IsVerifiedTournamentClient() || !g_characterPages.Complete()
            || g_characterPages.Pages() < 2) return false;
    const auto root = SelectorValue<std::uintptr_t>(0x75F6E8);
    return root && *reinterpret_cast<DWORD*>(root + 12) == 7
        && SelectorValue<DWORD>(0x64BBD4) == 4
        && SelectorValue<DWORD>(0x64AF98) == 0
        && SelectorValue<WORD>(0x64AFBC) == 0;
}
void RefreshCharacterPageCaption(HWND hwnd) {
    const unsigned page = IsCharacterPagingPage() ? g_characterPages.Page() + 1 : 0;
    if (page == g_characterPageCaption) return;
    const bool wasPaged = g_characterPageCaption != 0;
    // Set before SetWindowText: WM_SETTEXT re-enters the subclass.
    g_characterPageCaption = page;
    if (page) {
        if (!wasPaged) GetWindowTextW(hwnd, g_characterBaseCaption, 256);
        wchar_t caption[384]{};
        swprintf_s(caption, L"%s — 角色 %u/%u 頁（PgUp/PgDn 或滑鼠滾輪）",
            g_characterBaseCaption, page, g_characterPages.Pages());
        SetWindowTextW(hwnd, caption);
    } else if (wasPaged) SetWindowTextW(hwnd, g_characterBaseCaption);
}
void RejectCharacterDirectory() {
    g_characterPages.Begin(0);
    Log("character directory rejected: invalid count or row; native four-slot buffer protected");
    PostMessageW(g_window, WM_CLOSE, 0, 0);
}
void __fastcall ReceivePagedCharacterCount(void* packet, void*) {
    const auto p = static_cast<const BYTE*>(packet);
    if (!p || !g_characterPages.Begin(p[1])) { RejectCharacterDirectory(); return; }
    // Only the four visible rows enter LiTo's 0x62FAA0..0x62FD6F buffer.
    // 0x62FD70 onwards is the portrait animation table, NOT another four slots.
    BYTE count[]{3, static_cast<BYTE>(g_characterPages.Visible())};
    ReceiveSelectorCharacterCount(count, nullptr);
    if (!g_characterPages.Count()) RefreshCharacterPageCaption(g_window);
}
void RenderCharacterPage(bool resetCount) {
    if (!g_characterPages.Complete()) return;
    SelectorValue<DWORD>(0x75F750) = 0;
    if (resetCount) {
        BYTE count[]{3, static_cast<BYTE>(g_characterPages.Visible())};
        ReceiveSelectorCharacterCount(count, nullptr);
    }
    for (unsigned i = 0; i < g_characterPages.Visible(); ++i) {
        // The native checksum path writes to packet[0]. Keep cached rows intact
        // so switching pages never mutates the authoritative directory copy.
        auto row = g_characterPages.At(i);
        InvokeNativeCharacterRow(row.bytes.data(), nullptr);
    }
    Log("character directory displayed total=%u page=%u visible=%u",
        g_characterPages.Count(), g_characterPages.Page()+1, g_characterPages.Visible());
    RefreshCharacterPageCaption(g_window);
}
void __fastcall ReceivePagedCharacterRow(void* packet, void*) {
    // S_CB also carries the selected character's game handoff. Preserve that
    // native branch; it is not a directory row and must not consume this cache.
    if (!g_characterPages.Receiving() || SelectorValue<WORD>(0x64AFBC) != 0) {
        InvokeNativeCharacterRow(packet, nullptr);
        return;
    }
    MEMORY_BASIC_INFORMATION memory{};
    if (!packet || !VirtualQuery(packet, &memory, sizeof(memory))
            || memory.State != MEM_COMMIT || (memory.Protect & (PAGE_NOACCESS | PAGE_GUARD))) {
        RejectCharacterDirectory(); return;
    }
    const auto available = reinterpret_cast<std::uintptr_t>(memory.BaseAddress)
        + memory.RegionSize - reinterpret_cast<std::uintptr_t>(packet);
    if (!g_characterPages.Add(static_cast<const BYTE*>(packet), available)) {
        RejectCharacterDirectory(); return;
    }
    if (g_characterPages.Complete()) RenderCharacterPage(false);
}
bool TurnCharacterPage(int direction) {
    if (!IsCharacterPagingPage()) return false;
    if (g_characterPages.Turn(direction)) RenderCharacterPage(true);
    return true;
}
