// Read only the native Tournament toolbar. The world toolbar at 64B908 has
// different resources and coordinates and is absent in the standalone lobby.
bool ReadToolbarBytes(std::uintptr_t address, void* value, SIZE_T size) {
    SIZE_T copied{};
    return address && ReadProcessMemory(GetCurrentProcess(),
        reinterpret_cast<const void*>(address), value, size, &copied) && copied == size;
}

bool ReadTournamentToolbarRect(std::uintptr_t root, bool exitIcon, RECT& rect) {
    DWORD header[2]{};
    if (!ReadToolbarBytes(root, header, sizeof(header)) || !header[0]
            || header[1] < 7 || header[1] > 64) return false;
    const DWORD slot = exitIcon ? 4 : 6;
    DWORD object{};
    BYTE widget[0x28]{};
    if (!ReadToolbarBytes(header[0] + slot * sizeof(DWORD), &object, sizeof(object))
            || !ReadToolbarBytes(object, widget, sizeof(widget))) return false;
    DWORD vtable{}, resource{}, callback{};
    memcpy(&vtable, widget, 4);
    memcpy(&resource, widget + 0x20, 4);
    memcpy(&callback, widget + 0x24, 4);
    memcpy(&rect, widget + 4, sizeof(rect));
    // Verified native 404/405 exit and 392/393 settings controls, not an alias,
    // hidden widget, stale pointer or an unrelated screen's control.
    return vtable == 0x004F7B60 && widget[0x14] != 0
        && resource == (exitIcon ? 404U : 392U)
        && callback == (exitIcon ? 0x0046B1A0U : 0x0047B8F0U)
        && rect.left >= 0 && rect.top >= 0
        && rect.right <= 640 && rect.bottom <= 480
        && rect.right - rect.left == 19 && rect.bottom - rect.top == 21;
}

bool TournamentToolbarRectContains(const RECT& rect, LPARAM mapped) {
    const int x = GET_X_LPARAM(mapped), y = GET_Y_LPARAM(mapped);
    return x >= rect.left && x < rect.right && y >= rect.top && y < rect.bottom;
}
