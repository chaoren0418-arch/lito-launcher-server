#ifndef EDIT_CANVAS_NATIVE_ADDRESS
#define EDIT_CANVAS_NATIVE_ADDRESS(address) (address)
#endif
// LiTo's Unicode edit paints through GDI, then copies its pixels from the
// DirectDraw primary back into the game canvas. Our primary is an offscreen
// surface: painting the actual HWND cannot populate it. Route only the native
// edit's paint/IME DC calls to that primary, in its original logical coordinates.
// The existing final presentation scales the entire frame exactly once.
volatile LONG g_editCanvasPatchState{};
volatile LONG g_editCanvasPaintCount{};
volatile LONG g_editCanvasMissCount{};
constexpr UINT kMaskEditSurfacesMessage = WM_APP + 0x382;

bool IsCanvasEdit(HWND hwnd) {
    if (!hwnd || !g_window || GetParent(hwnd) != g_window
        || !IsVerifiedTournamentClient()) return false;
    const DWORD phase = *reinterpret_cast<volatile DWORD*>(EDIT_CANVAS_NATIVE_ADDRESS(0x0064AF98));
    if (phase < 1 || phase > 5) return false;
    wchar_t name[32]{};
    return GetClassNameW(hwnd, name, _countof(name))
        && _wcsicmp(name, L"LUnicodeEdit") == 0;
}

struct EditCanvasDc {
    HWND window{};
    HDC dc{};
    void* surface{};
    SurfaceVtableHook* hook{};
    int saved{};
    PAINTSTRUCT originalPaint{};
    bool painting{};
};
thread_local EditCanvasDc g_editDcs[4]{};

HDC AcquireEditCanvasDc(HWND hwnd, const PAINTSTRUCT* paint) {
    if (!IsCanvasEdit(hwnd)) return nullptr;
    EditCanvasDc* slot = nullptr;
    for (auto& current : g_editDcs) if (!current.dc) { slot = &current; break; }
    if (!slot) return nullptr;
    RECT rect{}, size{};
    POINT parentOrigin{};
    if (!GetWindowRect(hwnd, &rect) || !GetClientRect(hwnd, &size)
        || !ClientToScreen(g_window, &parentOrigin)) return nullptr;
    OffsetRect(&rect, -parentOrigin.x, -parentOrigin.y);
    if (rect.left < 0 || rect.top < 0 || rect.right > 640 || rect.bottom > 480
        || size.right <= 0 || size.bottom <= 0) return nullptr;
    // This is the origin used by sub_4DDE60 and sub_4DE1A0 when they read the
    // emulated desktop primary, not the scaled on-screen position of the edit.
    if (*reinterpret_cast<volatile BYTE*>(EDIT_CANVAS_NATIVE_ADDRESS(0x006476C8)) == 0)
        OffsetRect(&rect, parentOrigin.x, parentOrigin.y);
    void* surface = *reinterpret_cast<void* volatile*>(EDIT_CANVAS_NATIVE_ADDRESS(0x006476D8));
    if (!surface) return nullptr;
    auto* hook = FindSurfaceHook(surface);
    if (!hook || !hook->getDC || !hook->releaseDC) return nullptr;
    EnterCriticalSection(&g_presentLock);
    HDC dc{};
    if (FAILED(hook->getDC(surface, &dc)) || !dc) {
        LeaveCriticalSection(&g_presentLock);
        return nullptr;
    }
    const int saved = SaveDC(dc);
    if (!saved || !SetViewportOrgEx(dc, rect.left, rect.top, nullptr)
        || IntersectClipRect(dc, 0, 0, size.right, size.bottom) == ERROR) {
        if (saved) RestoreDC(dc, saved);
        hook->releaseDC(surface, dc);
        LeaveCriticalSection(&g_presentLock);
        return nullptr;
    }
    *slot = {hwnd, dc, surface, hook, saved, {}, paint != nullptr};
    if (paint) slot->originalPaint = *paint;
    return dc;
}

bool ReleaseEditCanvasDc(HWND hwnd, HDC dc, PAINTSTRUCT* originalPaint) {
    for (auto& slot : g_editDcs) {
        if (slot.window != hwnd || slot.dc != dc) continue;
        if (originalPaint) *originalPaint = slot.originalPaint;
        GdiFlush();
        RestoreDC(slot.dc, slot.saved);
        slot.hook->releaseDC(slot.surface, slot.dc);
        slot = {};
        LeaveCriticalSection(&g_presentLock);
        return true;
    }
    return false;
}

HDC WINAPI EditBeginPaint(HWND hwnd, PAINTSTRUCT* paint) {
    HDC original = BeginPaint(hwnd, paint);
    HDC canvas = AcquireEditCanvasDc(hwnd, paint);
    if (!canvas) {
        if (IsCanvasEdit(hwnd) && InterlockedIncrement(&g_editCanvasMissCount) <= 10)
            Log("edit canvas DC unavailable hwnd=%p", hwnd);
        return original;
    }
    paint->hdc = canvas;
    paint->fErase = FALSE;
    GetClientRect(hwnd, &paint->rcPaint);
    const LONG count = InterlockedIncrement(&g_editCanvasPaintCount);
    if (count <= 10)
        Log("edit canvas paint count=%ld hwnd=%p logical-size=%ldx%ld", count, hwnd,
            paint->rcPaint.right, paint->rcPaint.bottom);
    return canvas;
}

BOOL WINAPI EditEndPaint(HWND hwnd, const PAINTSTRUCT* paint) {
    PAINTSTRUCT original{};
    if (ReleaseEditCanvasDc(hwnd, paint->hdc, &original))
        return EndPaint(hwnd, &original);
    return EndPaint(hwnd, paint);
}

HDC WINAPI EditGetDC(HWND hwnd) {
    HDC canvas = AcquireEditCanvasDc(hwnd, nullptr);
    return canvas ? canvas : GetDC(hwnd);
}

int WINAPI EditReleaseDC(HWND hwnd, HDC dc) {
    return ReleaseEditCanvasDc(hwnd, dc, nullptr) ? 1 : ReleaseDC(hwnd, dc);
}

bool MaskEditDesktopSurface(HWND hwnd) {
    if (!IsCanvasEdit(hwnd)) return false;
    // SetWindowRgn synchronously sends window messages. A presentation worker
    // can hold DirectDraw's global lock while the owner thread waits to render.
    // Never wait for the owner from that worker; let its message loop mask later.
    if (GetWindowThreadProcessId(hwnd, nullptr) != GetCurrentThreadId()) {
        PostMessageW(g_window, kMaskEditSurfacesMessage, 0, 0);
        return false;
    }
    if (GetPropW(hwnd, L"LiTo381.CanvasClip")) return true;
    // Do not SW_HIDE the native keyboard/IME owner: WM_KILLFOCUS clears
    // dword_64149C in sub_4DC730. Clip only its duplicate desktop pixels.
    HRGN region = CreateRectRgn(0, 0, 0, 0);
    if (!region) return false;
    if (!SetWindowRgn(hwnd, region, FALSE)) {
        DeleteObject(region);
        return false;
    }
    SetPropW(hwnd, L"LiTo381.CanvasClip", reinterpret_cast<HANDLE>(1));
    return true; // Windows now owns the region; HWND destruction removes the property.
}

// Empty desktop regions keep duplicate GDI pixels off the scaled canvas, but
// also exclude edits from Windows hit testing. Route logical clicks using each
// live native edit's actual rectangle; preserve its own keyboard/IME handler.
bool RouteCanvasEditMouse(UINT message, WPARAM keys, LPARAM logicalPoint) {
    if (message != WM_LBUTTONDOWN && message != WM_LBUTTONUP
        && message != WM_LBUTTONDBLCLK) return false;
    if (!g_window || GetWindowThreadProcessId(g_window, nullptr) != GetCurrentThreadId())
        return false;
    POINT point{static_cast<short>(LOWORD(logicalPoint)),
                static_cast<short>(HIWORD(logicalPoint))};
    POINT origin{};
    if (!ClientToScreen(g_window, &origin)) return false;
    for (HWND child = GetWindow(g_window, GW_CHILD); child;
         child = GetWindow(child, GW_HWNDNEXT)) {
        if (!IsCanvasEdit(child) || !IsWindowEnabled(child)
            || !(GetWindowLongW(child, GWL_STYLE) & WS_VISIBLE)
            || !GetPropW(child, L"LiTo381.CanvasClip")) continue;
        RECT rect{};
        if (!GetWindowRect(child, &rect)) continue;
        OffsetRect(&rect, -origin.x, -origin.y);
        if (rect.left < 0 || rect.top < 0 || rect.right > 640 || rect.bottom > 480
            || !PtInRect(&rect, point)) continue;
        if (message != WM_LBUTTONUP) SetFocus(child);
        SendMessageW(child, message, keys,
            MAKELPARAM(point.x - rect.left, point.y - rect.top));
        return true;
    }
    return false;
}

void HideTournamentEditHelperSurfaces() {
    if (!g_window || !IsWindow(g_window)) return;
    if (GetWindowThreadProcessId(g_window, nullptr) != GetCurrentThreadId()) {
        PostMessageW(g_window, kMaskEditSurfacesMessage, 0, 0);
        return;
    }
    EnumChildWindows(g_window, [](HWND child, LPARAM) -> BOOL {
        if (IsCanvasEdit(child)) MaskEditDesktopSurface(child);
        return TRUE;
    }, 0);
}

BOOL WINAPI EditShowWindow(HWND hwnd, int command) {
    if (IsCanvasEdit(hwnd)) MaskEditDesktopSurface(hwnd);
    return ShowWindow(hwnd, command);
}

BOOL WINAPI EditUpdateWindow(HWND hwnd) {
    if (!IsCanvasEdit(hwnd)) return UpdateWindow(hwnd);
    // UpdateWindow skips invisible HWNDs. The game explicitly requests this
    // paint before copying from the virtual primary; deliver it synchronously
    // on the same native UI thread, retaining its text and IME implementation.
    SendMessageW(hwnd, WM_PAINT, 0, 0);
    return TRUE;
}

#include "edit_call_layout.inc"

struct EditCallPatch {
    std::uintptr_t address;
    void* replacement;
    const char* prefix;
    size_t prefixLength;
    BYTE original[5];
};

bool EnsureNativeEditCanvasPatch() {
    if (g_editCanvasPatchState == 1) return true;
    if (g_editCanvasPatchState == -1 || !IsVerifiedLiToClient()) return false;
    // Guard the instruction boundaries independently of ASLR-dependent API
    // thunk addresses. Only these nine call operands in the verified client
    // are changed; no global User32 hooks, DLL injection, or disk binary edits.
    EditCallPatch patches[] = {
        {0x004DC139, reinterpret_cast<void*>(&EditUpdateWindow), "\x57", 1, {}},
        {0x004DCB56, reinterpret_cast<void*>(&EditBeginPaint), "\x51", 1, {}},
        {0x004DCC93, reinterpret_cast<void*>(&EditEndPaint), "\x51\x52", 2, {}},
        {0x004DCF28, reinterpret_cast<void*>(&EditUpdateWindow), "\x50", 1, {}},
        {0x004DD17D, reinterpret_cast<void*>(&EditGetDC), "\x89\x7D\xD0", 3, {}},
        {0x004DD725, reinterpret_cast<void*>(&EditReleaseDC), "\x53\x52", 2, {}},
        {0x004DE261, reinterpret_cast<void*>(&EditShowWindow), "\x6A\x05\x52", 3, {}},
        {0x004DE27F, reinterpret_cast<void*>(&EditUpdateWindow), "\x51", 1, {}},
        {0x004DE2A6, reinterpret_cast<void*>(&EditUpdateWindow), "\x50", 1, {}},
    };
    for (auto& patch : patches) {
        BYTE* site = FindEditCall(reinterpret_cast<BYTE*>(patch.address),
            patch.prefix, patch.prefixLength);
        if (!site) {
            g_editCanvasPatchState = -1;
            Log("edit canvas patch skipped unexpected site=%08lX", static_cast<DWORD>(patch.address));
            return false;
        }
        patch.address = reinterpret_cast<std::uintptr_t>(site);
        std::memcpy(patch.original, site, 5);
    }
    // All sites are in one code page range. One protection operation makes
    // preparation failure leave every call untouched.
    constexpr auto first = 0x004DC139;
    constexpr auto span = 0x004DE2AC - first;
    DWORD old{};
    if (!VirtualProtect(reinterpret_cast<void*>(first), span, PAGE_EXECUTE_READWRITE, &old)) return false;
    for (auto& patch : patches) {
        const auto offset = static_cast<std::int32_t>(
            reinterpret_cast<std::uintptr_t>(patch.replacement) - patch.address - 5);
        std::memcpy(reinterpret_cast<void*>(patch.address + 1), &offset, 4);
    }
    FlushInstructionCache(GetCurrentProcess(), reinterpret_cast<void*>(first), span);
    DWORD ignored{};
    VirtualProtect(reinterpret_cast<void*>(first), span, old, &ignored);
    g_editCanvasPatchState = 1;
    Log("native Unicode edit canvas routing applied sites=9 logical=640x480 final-scale=%d disk=unchanged", g_scalePercent);
    return true;
}
