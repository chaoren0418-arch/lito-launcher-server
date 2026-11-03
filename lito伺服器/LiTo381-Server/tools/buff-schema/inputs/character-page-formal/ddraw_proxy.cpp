#include <map>
#include "item_details.h"
#include "live_stats.h"
#include "caption_drag.h"
#define WIN32_LEAN_AND_MEAN
#define NOMINMAX
#define DIRECTDRAW_VERSION 0x0700
#include <windows.h>
#include <windowsx.h>
#include <tlhelp32.h>
#include <ddraw.h>
#include <algorithm>
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <string>
#include <vector>

namespace {

constexpr size_t kMaxObjects = 32;
constexpr size_t kMaxDdVtables = 8;
constexpr size_t kMaxSurfaceVtables = 12;
constexpr size_t kMaxPrimaries = 8;
constexpr UINT_PTR kPresentTimerId = 0x382;
constexpr UINT kConfirmedCloseMessage = WM_APP + 0x381;
constexpr DWORD kWindowedStyle = WS_OVERLAPPED | WS_CAPTION | WS_SYSMENU
    | WS_MINIMIZEBOX | WS_CLIPCHILDREN | WS_CLIPSIBLINGS;
constexpr DWORD kWindowedExStyle = WS_EX_APPWINDOW | WS_EX_WINDOWEDGE;

using QueryInterfaceFn = HRESULT (WINAPI*)(void*, REFIID, void**);
using CreateSurfaceFn = HRESULT (WINAPI*)(void*, void*, void**, IUnknown*);
using SetCooperativeLevelFn = HRESULT (WINAPI*)(void*, HWND, DWORD);
using SetDisplayMode3Fn = HRESULT (WINAPI*)(void*, DWORD, DWORD, DWORD);
using SetDisplayMode5Fn = HRESULT (WINAPI*)(void*, DWORD, DWORD, DWORD, DWORD, DWORD);
using SurfaceReleaseFn = ULONG (WINAPI*)(void*);
using SurfaceAddRefFn = ULONG (WINAPI*)(void*);
using SurfaceBltFn = HRESULT (WINAPI*)(void*, RECT*, void*, RECT*, DWORD, DDBLTFX*);
using SurfaceBltFastFn = HRESULT (WINAPI*)(void*, DWORD, DWORD, void*, RECT*, DWORD);
using SurfaceFlipFn = HRESULT (WINAPI*)(void*, void*, DWORD);
using SurfaceGetDCFn = HRESULT (WINAPI*)(void*, HDC*);
using SurfaceLockFn = HRESULT (WINAPI*)(void*, RECT*, DDSURFACEDESC*, DWORD, HANDLE);
using SurfaceGetDescFn = HRESULT (WINAPI*)(void*, DDSURFACEDESC*);
using SurfaceReleaseDCFn = HRESULT (WINAPI*)(void*, HDC);
using SurfaceUnlockFn = HRESULT (WINAPI*)(void*, RECT*);
using LiToToolbarToggleFn = void (__fastcall*)(std::uintptr_t, std::uintptr_t);

struct DdVtableHook {
    void** vtable{};
    QueryInterfaceFn queryInterface{};
    CreateSurfaceFn createSurface{};
    SetCooperativeLevelFn setCooperativeLevel{};
    SetDisplayMode3Fn setDisplayMode3{};
    SetDisplayMode5Fn setDisplayMode5{};
    bool extended{};
};

struct ObjectRecord { void* object{}; DdVtableHook* hook{}; };

struct SurfaceVtableHook {
    void** vtable{};
    SurfaceAddRefFn addRef{};
    SurfaceReleaseFn release{};
    SurfaceBltFn blt{};
    SurfaceBltFastFn bltFast{};
    SurfaceFlipFn flip{};
    SurfaceGetDCFn getDC{};
    SurfaceLockFn lock{};
    SurfaceGetDescFn getSurfaceDesc{};
    SurfaceReleaseDCFn releaseDC{};
    SurfaceUnlockFn unlock{};
};

HMODULE g_self{};
HMODULE g_systemDdraw{};
INIT_ONCE g_once = INIT_ONCE_STATIC_INIT;
CRITICAL_SECTION g_lock;
CRITICAL_SECTION g_presentLock;
DdVtableHook g_ddHooks[kMaxDdVtables]{};
ObjectRecord g_objects[kMaxObjects]{};
SurfaceVtableHook g_surfaceHooks[kMaxSurfaceVtables]{};
void* g_primaries[kMaxPrimaries]{};
HWND g_window{};
HWND g_configuredWindow{};
WNDPROC g_originalWndProc{};
RECT g_primaryContentRect{};
DWORD g_virtualWidth = 640;
DWORD g_virtualHeight = 480;
int g_scalePercent = 100;
bool g_loggingEnabled = false;
volatile LONG g_buddyClanOpen{};
volatile LONG g_toolbarWorkerActive{};
volatile LONG g_presenting{};
volatile LONG g_bltCount{};
volatile LONG g_bltFastCount{};
volatile LONG g_flipCount{};
volatile LONG g_lockCount{};
volatile LONG g_unlockCount{};
volatile LONG g_releaseDcCount{};
volatile LONG g_presentCount{};
volatile LONG g_presentTimerPending{};
volatile LONG g_lastPresentQueueTick{};
volatile LONG g_presentWorkerState{};
HANDLE g_presentEvent{};
HDC g_candidateFrameDc{};
HDC g_stableFrameDc{};
HBITMAP g_candidateFrameBitmap{};
HBITMAP g_stableFrameBitmap{};
std::uint32_t* g_candidateFrameBits{};
bool g_stableFrameValid{};
bool g_stableFrameHasContent{};
ULONGLONG g_nearBlackSince{};
volatile LONG g_rejectedIncompleteFrames{};
volatile LONG g_lastTournamentPhase{-1};
volatile LONG g_resultSummaryPending{};
volatile LONG g_visibilityRestoreLogged{};
volatile LONG g_visibilityWorkerStarted{};
volatile LONG g_backgroundFocusVirtualized{};
volatile LONG g_runClanPacketPatchState{};
volatile LONG g_runClanPacketPatchUnexpectedLogged{};
volatile LONG g_worldTreePacketPatchState{};
volatile LONG g_worldTreePacketPatchUnexpectedLogged{};
volatile LONG g_tournamentShopCurrencyPatchState{};
volatile LONG g_tournamentShopCurrencyPatchUnexpectedLogged{};
volatile LONG g_itemDetailLayoutPatchState{};
volatile LONG g_itemDetailLayoutPatchUnexpectedLogged{};
volatile LONG g_numericBreakthroughPatchState{};
volatile LONG g_numericBreakthroughPatchUnexpectedLogged{};
volatile LONG g_fullMaximumHp{1};
volatile LONG g_fullMaximumMp{1};
volatile LONG g_fullArmorClass{};
volatile LONG g_fullLevel{1};
volatile LONG g_fullAbilities[6]{1, 1, 1, 1, 1, 1};
volatile LONG g_fullSpellPowerModifier{};
volatile LONG g_fullMagicResistanceModifier{};
// Wire order is fire, water, wind, earth.  LiTo's native globals are stored
// in the reverse address order at 0x8348C1..0x8348BE.
volatile LONG g_fullElementalResistance[4]{};
bool g_numericBreakthroughEnabled = false;
int g_numericBreakthroughSchema = 0;
ULONGLONG g_forceVisibleUntil{};
wchar_t g_directory[MAX_PATH]{};

bool IsVerifiedLiToClient();
bool EnsureNativeRunClanPacketPatch();
bool EnsureNativeWorldTreePacketPatch();
bool EnsureNativeTournamentShopCurrencyPatch();
bool EnsureSharedBattleClockPatch();
bool EnsureResultReturnPatch();
bool EnsureBattleWindowIdlePatch();
bool EnsureLiToItemDetailExtension();
bool EnsureSelectorReturnPatch();
bool EnsureClassCompatibilityPatch();
bool EnsureNativeItemDetailLayoutPatch();
bool EnsureNativeEditCanvasPatch();
bool EnsureNumericBreakthroughPatches();

DWORD __cdecl TournamentShopWorldModeGuard() {
    const auto worldMode = *reinterpret_cast<volatile BYTE**>(0x00834C18);
    return worldMode ? *reinterpret_cast<volatile DWORD*>(worldMode + 0x14) : 0;
}

template<typename T>
T SystemProc(const char* name) {
    return reinterpret_cast<T>(GetProcAddress(g_systemDdraw, name));
}

void Log(const char* format, ...) {
    if (!g_loggingEnabled) return;
    char line[1024]{};
    va_list args;
    va_start(args, format);
    _vsnprintf_s(line, _countof(line), _TRUNCATE, format, args);
    va_end(args);
    char path[MAX_PATH]{};
    WideCharToMultiByte(CP_ACP, 0, g_directory, -1, path, MAX_PATH, nullptr, nullptr);
    strcat_s(path, "lito381-compat.log");
    FILE* file{};
    if (fopen_s(&file, path, "a") == 0 && file) {
        SYSTEMTIME now{};
        GetLocalTime(&now);
        fprintf(file, "%02u:%02u:%02u.%03u [pid=%lu tid=%lu] %s\n", now.wHour, now.wMinute,
                now.wSecond, now.wMilliseconds, GetCurrentProcessId(), GetCurrentThreadId(), line);
        fclose(file);
    }
}

// The host installs the common catalog before the first graphics initialization.
// This callback is outside DllMain and precedes native sprite/actor construction.
// Only the candidate launcher enables the gate; default compatibility is unchanged.
extern "C" {
__declspec(dllexport) DWORD LiToSharedResourceGate[4] = {0x3152534C, 0, 0, 0};
// magic, schema, connect-cache address, source ID, host-order port, generation.
__declspec(dllexport) DWORD LiToSourceSession[8] = {0x31534C53, 1, 0, 0, 0, 0, 0, 0};
}
void WaitForSharedResources() {
    wchar_t enabled[8]{};
    if (GetEnvironmentVariableW(L"LITO_SHARED_RESOURCE_GATE", enabled, 8) != 1 || enabled[0] != L'1') return;
    auto* state = reinterpret_cast<volatile LONG*>(&LiToSharedResourceGate[1]);
    LiToSharedResourceGate[2] = GetCurrentThreadId();
    InterlockedExchange(state, 1);
    const ULONGLONG deadline = GetTickCount64() + 30000;
    while (InterlockedCompareExchange(state, 0, 0) != 2) {
        if (GetTickCount64() >= deadline) {
            InterlockedExchange(state, 3);
            TerminateProcess(GetCurrentProcess(), 0xC0DE0028);
            return;
        }
        Sleep(10);
    }
    LiToSharedResourceGate[3] = 1;
}

BOOL CALLBACK Initialize(PINIT_ONCE, PVOID, PVOID*) {
    WaitForSharedResources();
    InitializeCriticalSection(&g_lock);
    InitializeCriticalSection(&g_presentLock);
    GetModuleFileNameW(g_self, g_directory, MAX_PATH);
    if (wchar_t* slash = wcsrchr(g_directory, L'\\')) *(slash + 1) = 0;

    wchar_t ini[MAX_PATH]{};
    swprintf_s(ini, L"%slito381-compat.ini", g_directory);
    g_virtualWidth = std::clamp<DWORD>(
        GetPrivateProfileIntW(L"Window", L"Width", 640, ini), 320, 1920);
    g_virtualHeight = std::clamp<DWORD>(
        GetPrivateProfileIntW(L"Window", L"Height", 480, ini), 240, 1080);
    g_scalePercent = std::clamp(
        static_cast<int>(GetPrivateProfileIntW(L"Window", L"ScalePercent", 100, ini)),
        75, 250);
    g_loggingEnabled = GetPrivateProfileIntW(L"Diagnostics", L"Logging", 0, ini) != 0;
    g_numericBreakthroughEnabled =
        GetPrivateProfileIntW(L"NumericBreakthrough", L"Enabled", 0, ini) != 0;
    if (g_numericBreakthroughEnabled) {
        const int requestedSchema = static_cast<int>(
            GetPrivateProfileIntW(L"NumericBreakthrough", L"Schema", 1, ini));
        if (requestedSchema >= 1 && requestedSchema <= 4) {
            g_numericBreakthroughSchema = requestedSchema;
        } else {
            g_numericBreakthroughEnabled = false;
            Log("numeric breakthrough disabled reason=unsupported-schema requested=%d",
                requestedSchema);
        }
    }
    wchar_t systemDirectory[MAX_PATH]{};
    if (!GetSystemDirectoryW(systemDirectory, MAX_PATH)) return FALSE;
    wchar_t systemPath[MAX_PATH]{};
    swprintf_s(systemPath, L"%s\\ddraw.dll", systemDirectory);
    g_systemDdraw = LoadLibraryW(systemPath);
    Log("init system=%p virtual=%lux%lu scale=%d", g_systemDdraw,
        g_virtualWidth, g_virtualHeight, g_scalePercent);
    return g_systemDdraw != nullptr;
}

bool EnsureInitialized() {
    const bool initialized = InitOnceExecuteOnce(
        &g_once, Initialize, nullptr, nullptr) != FALSE
        && g_systemDdraw != nullptr;
    if (initialized) {
        EnsureNativeRunClanPacketPatch();
        EnsureNativeWorldTreePacketPatch();
        EnsureNativeTournamentShopCurrencyPatch();
        EnsureNativeItemDetailLayoutPatch();
        EnsureNativeEditCanvasPatch();
        if (IsVerifiedLiToClient() && !EnsureSharedBattleClockPatch()) return false;
        if (IsVerifiedLiToClient() && !EnsureResultReturnPatch()) return false;
        if (IsVerifiedLiToClient() && !EnsureBattleWindowIdlePatch()) return false;
        if (IsVerifiedLiToClient() && !EnsureLiToItemDetailExtension()) return false;
        if (IsVerifiedLiToClient() && !EnsureSelectorReturnPatch()) return false;
        if (IsVerifiedLiToClient() && !EnsureClassCompatibilityPatch()) return false;
        if (g_numericBreakthroughEnabled && !EnsureNumericBreakthroughPatches()) {
            Log("numeric breakthrough fail-closed before DirectDraw initialization");
            return FALSE;
        }
    }
    return initialized;
}

DdVtableHook* FindDdHookByVtable(void** vtable) {
    for (auto& hook : g_ddHooks) if (hook.vtable == vtable) return &hook;
    return nullptr;
}

ObjectRecord* FindObject(void* object) {
    for (auto& record : g_objects) if (record.object == object) return &record;
    return nullptr;
}

SurfaceVtableHook* FindSurfaceHook(void* surface) {
    if (!surface) return nullptr;
    void** vtable = *static_cast<void***>(surface);
    for (auto& hook : g_surfaceHooks) if (hook.vtable == vtable) return &hook;
    return nullptr;
}

bool IsPrimary(void* surface) {
    for (void* primary : g_primaries) if (primary == surface) return true;
    return false;
}

void* AcquirePrimaryReference() {
    void* surface{};
    EnterCriticalSection(&g_lock);
    for (void* primary : g_primaries) {
        if (!primary) continue;
        SurfaceVtableHook* hook = FindSurfaceHook(primary);
        if (hook && hook->addRef) {
            hook->addRef(primary);
            surface = primary;
        }
        break;
    }
    LeaveCriticalSection(&g_lock);
    return surface;
}

void ReleaseSurfaceReference(void* surface) {
    if (!surface) return;
    EnterCriticalSection(&g_lock);
    SurfaceVtableHook* hook = FindSurfaceHook(surface);
    ULONG remaining = hook && hook->release ? hook->release(surface) : 1;
    if (remaining == 0) {
        for (void*& slot : g_primaries) if (slot == surface) slot = nullptr;
    }
    LeaveCriticalSection(&g_lock);
}

bool IsVerifiedTournamentClient();

void RememberPrimary(void* surface) {
    EnterCriticalSection(&g_lock);
    for (void*& slot : g_primaries) {
        if (slot == surface) break;
        if (!slot) { slot = surface; break; }
    }
    LeaveCriticalSection(&g_lock);
}

void ForgetPrimary(void* surface) {
    EnterCriticalSection(&g_lock);
    for (void*& slot : g_primaries) if (slot == surface) slot = nullptr;
    LeaveCriticalSection(&g_lock);
}

RECT AspectFit(int targetWidth, int targetHeight) {
    RECT result{0, 0, targetWidth, targetHeight};
    if (targetWidth <= 0 || targetHeight <= 0 || !g_virtualWidth || !g_virtualHeight) return result;
    const long long byWidth = static_cast<long long>(targetWidth) * g_virtualHeight;
    const long long byHeight = static_cast<long long>(targetHeight) * g_virtualWidth;
    if (byWidth <= byHeight) {
        int height = MulDiv(targetWidth, static_cast<int>(g_virtualHeight), static_cast<int>(g_virtualWidth));
        result.top = (targetHeight - height) / 2;
        result.bottom = result.top + height;
    } else {
        int width = MulDiv(targetHeight, static_cast<int>(g_virtualWidth), static_cast<int>(g_virtualHeight));
        result.left = (targetWidth - width) / 2;
        result.right = result.left + width;
    }
    return result;
}

void HideCompletedCharacterLoadingFooter(HWND hwnd) {
    if (!hwnd || !IsWindow(hwnd) || !IsVerifiedTournamentClient()) return;
    const auto screenRoot = *reinterpret_cast<volatile DWORD*>(0x0075F6E8);
    const auto clientMode = *reinterpret_cast<volatile DWORD*>(0x0064BBD4);
    if (screenRoot < 0x10000 || clientMode != 4
        || *reinterpret_cast<volatile DWORD*>(screenRoot + 0x0C) != 7) return;

    RECT client{};
    if (!GetClientRect(hwnd, &client)) return;
    const RECT viewport = AspectFit(client.right, client.bottom);
    const int footerTop = viewport.top + MulDiv(
        462, viewport.bottom - viewport.top,
        static_cast<int>(g_virtualHeight));
    if (footerTop >= viewport.bottom) return;
    HDC target = GetDC(hwnd);
    if (!target) return;
    PatBlt(target, viewport.left, footerTop,
        viewport.right - viewport.left,
        viewport.bottom - footerTop, BLACKNESS);
    ReleaseDC(hwnd, target);
}

void ClearLetterboxBars(HDC target, const RECT& client, const RECT& viewport) {
    // Clearing the entire client before every StretchBlt exposes a black
    // intermediate frame to DWM and makes the continuously redrawn lobby
    // flash.  Only clear the bars which are outside the final frame.
    if (viewport.top > 0)
        PatBlt(target, 0, 0, client.right, viewport.top, BLACKNESS);
    if (viewport.bottom < client.bottom)
        PatBlt(target, 0, viewport.bottom, client.right,
               client.bottom - viewport.bottom, BLACKNESS);
    if (viewport.left > 0)
        PatBlt(target, 0, viewport.top, viewport.left,
               viewport.bottom - viewport.top, BLACKNESS);
    if (viewport.right < client.right)
        PatBlt(target, viewport.right, viewport.top,
               client.right - viewport.right,
               viewport.bottom - viewport.top, BLACKNESS);
}

bool EnsureFrameCache() {
    if (g_candidateFrameDc && g_stableFrameDc && g_candidateFrameBits) return true;
    BITMAPINFO info{};
    info.bmiHeader.biSize = sizeof(BITMAPINFOHEADER);
    info.bmiHeader.biWidth = static_cast<LONG>(g_virtualWidth);
    info.bmiHeader.biHeight = -static_cast<LONG>(g_virtualHeight);
    info.bmiHeader.biPlanes = 1;
    info.bmiHeader.biBitCount = 32;
    info.bmiHeader.biCompression = BI_RGB;
    HDC screen = GetDC(nullptr);
    if (!screen) return false;
    void* candidateBits{};
    void* stableBits{};
    HBITMAP candidate = CreateDIBSection(screen, &info, DIB_RGB_COLORS,
                                         &candidateBits, nullptr, 0);
    HBITMAP stable = CreateDIBSection(screen, &info, DIB_RGB_COLORS,
                                      &stableBits, nullptr, 0);
    HDC candidateDc = candidate ? CreateCompatibleDC(screen) : nullptr;
    HDC stableDc = stable ? CreateCompatibleDC(screen) : nullptr;
    ReleaseDC(nullptr, screen);
    if (!candidate || !stable || !candidateDc || !stableDc) {
        if (candidateDc) DeleteDC(candidateDc);
        if (stableDc) DeleteDC(stableDc);
        if (candidate) DeleteObject(candidate);
        if (stable) DeleteObject(stable);
        return false;
    }
    SelectObject(candidateDc, candidate);
    SelectObject(stableDc, stable);
    g_candidateFrameDc = candidateDc;
    g_stableFrameDc = stableDc;
    g_candidateFrameBitmap = candidate;
    g_stableFrameBitmap = stable;
    g_candidateFrameBits = static_cast<std::uint32_t*>(candidateBits);
    return true;
}

bool CaptureStableFrame(HDC source, const RECT& sourceRect) {
    if (!EnsureFrameCache()) return false;
    const int sourceWidth = sourceRect.right - sourceRect.left;
    const int sourceHeight = sourceRect.bottom - sourceRect.top;
    if (sourceWidth <= 0 || sourceHeight <= 0) return false;
    BOOL copied = StretchBlt(g_candidateFrameDc, 0, 0,
        static_cast<int>(g_virtualWidth), static_cast<int>(g_virtualHeight),
        source, sourceRect.left, sourceRect.top, sourceWidth, sourceHeight, SRCCOPY);
    if (!copied) return false;

    // The 3.81C renderer clears its desktop-coordinate primary between UI
    // composition passes.  Publishing that intermediate black buffer causes
    // visible flashing and makes a pressed button appear to do nothing.  Hold
    // the last complete frame when a near-black candidate is only transient;
    // a real black screen is accepted after it remains stable for 750 ms.
    std::size_t samples{};
    std::size_t nonBlack{};
    const std::size_t pixels = static_cast<std::size_t>(g_virtualWidth) * g_virtualHeight;
    for (std::size_t index = 0; index < pixels; index += 32) {
        const std::uint32_t pixel = g_candidateFrameBits[index];
        const unsigned blue = pixel & 0xFF;
        const unsigned green = (pixel >> 8) & 0xFF;
        const unsigned red = (pixel >> 16) & 0xFF;
        if (red + green + blue >= 36) ++nonBlack;
        ++samples;
    }
    const bool hasContent = samples && nonBlack * 100 >= samples * 2;
    const ULONGLONG now = GetTickCount64();
    if (!hasContent && g_stableFrameValid && g_stableFrameHasContent) {
        if (!g_nearBlackSince) g_nearBlackSince = now;
        if (now - g_nearBlackSince < 750) {
            LONG rejected = InterlockedIncrement(&g_rejectedIncompleteFrames);
            if (rejected <= 20 || rejected % 120 == 0)
                Log("held incomplete near-black frame count=%ld contentSamples=%zu/%zu",
                    rejected, nonBlack, samples);
            return true;
        }
    } else {
        g_nearBlackSince = 0;
    }
    BitBlt(g_stableFrameDc, 0, 0,
           static_cast<int>(g_virtualWidth), static_cast<int>(g_virtualHeight),
           g_candidateFrameDc, 0, 0, SRCCOPY);
    g_stableFrameValid = true;
    g_stableFrameHasContent = hasContent;
    return true;
}

#include "edit_canvas.inc"

void Present(void* surface, bool allowOffscreen = false) {
    const bool primary = IsPrimary(surface);
    (void)allowOffscreen;
    if (!surface || !primary || !g_window || !IsWindow(g_window)) return;
    // LiTo owns 744x552 and 640x480 work surfaces, but neither is a completed
    // presentation frame.  Publishing them exposes incremental composition
    // between primary updates and causes the lobby to flash or show fragments.
    // The desktop-coordinate primary is the sole completed output surface.
    if (InterlockedCompareExchange(&g_presenting, 1, 0) != 0) return;
    EnterCriticalSection(&g_presentLock);
    SurfaceVtableHook* hook = FindSurfaceHook(surface);
    HDC source{};
    HRESULT getDcResult = (hook && hook->getDC) ? hook->getDC(surface, &source) : DDERR_INVALIDOBJECT;
    LONG presentCount = InterlockedIncrement(&g_presentCount);
    if (SUCCEEDED(getDcResult) && source) {
        HDC target = GetDC(g_window);
        if (target) {
            RECT client{};
            GetClientRect(g_window, &client);
            RECT sourceRect{0, 0, static_cast<LONG>(g_virtualWidth), static_cast<LONG>(g_virtualHeight)};
            if (primary) {
                EnterCriticalSection(&g_lock);
                sourceRect = g_primaryContentRect;
                LeaveCriticalSection(&g_lock);
            }
            DDSURFACEDESC sourceDescription{};
            sourceDescription.dwSize = sizeof(sourceDescription);
            if (!primary && hook->getSurfaceDesc
                && SUCCEEDED(hook->getSurfaceDesc(surface, &sourceDescription))
                && sourceDescription.dwWidth && sourceDescription.dwHeight) {
                sourceRect.right = static_cast<LONG>(sourceDescription.dwWidth);
                sourceRect.bottom = static_cast<LONG>(sourceDescription.dwHeight);
            }
            if (primary && (sourceRect.right <= sourceRect.left || sourceRect.bottom <= sourceRect.top)) {
                POINT sourceOrigin{};
                ClientToScreen(g_window, &sourceOrigin);
                sourceRect = {sourceOrigin.x, sourceOrigin.y,
                    sourceOrigin.x + static_cast<LONG>(g_virtualWidth),
                    sourceOrigin.y + static_cast<LONG>(g_virtualHeight)};
            }
            RECT viewport = AspectFit(client.right, client.bottom);
            ClearLetterboxBars(target, client, viewport);
            int oldMode = SetStretchBltMode(target, COLORONCOLOR);
            if (IsVerifiedTournamentClient()) {
                const LONG phase = static_cast<LONG>(
                    *reinterpret_cast<volatile DWORD*>(0x0064AF98));
                const LONG previousPhase = InterlockedExchange(&g_lastTournamentPhase, phase);
                if (phase >= 1 && phase <= 4 && previousPhase != phase) {
                    HideTournamentEditHelperSurfaces();
                }
                if (previousPhase >= 0 && previousPhase != phase) {
                    // A cached login/room frame is not a valid arena frame.
                    // Invalidate it at every native Tournament phase boundary
                    // so a stalled transition shows the real new surface (or
                    // black), never an unrelated old picture.
                    g_stableFrameValid = false;
                    g_stableFrameHasContent = false;
                    g_nearBlackSince = 0;
                    Log("invalidated stable frame on Tournament phase %ld -> %ld",
                        previousPhase, phase);
                    if (previousPhase == 5 && phase == 2) {
                        InterlockedExchange(&g_resultSummaryPending, 1);
                        Log("armed battle-summary confirmation bridge on phase 5 -> 2");
                    } else if (phase != 2) {
                        InterlockedExchange(&g_resultSummaryPending, 0);
                    }
                }
            }
            const bool cached = CaptureStableFrame(source, sourceRect);
            HDC renderSource = cached && g_stableFrameValid ? g_stableFrameDc : source;
            RECT renderRect = cached && g_stableFrameValid
                ? RECT{0, 0, static_cast<LONG>(g_virtualWidth), static_cast<LONG>(g_virtualHeight)}
                : sourceRect;
            BOOL stretched = StretchBlt(target, viewport.left, viewport.top,
                       viewport.right - viewport.left, viewport.bottom - viewport.top,
                       renderSource, renderRect.left, renderRect.top,
                       renderRect.right - renderRect.left,
                       renderRect.bottom - renderRect.top, SRCCOPY);
            if (stretched && IsVerifiedTournamentClient()) {
                // Page 7 is composed by LiTo's GDI UI over the last DirectDraw
                // loading surface.  The page intentionally covers only y=0..461,
                // so a standalone launch otherwise leaves the old "Loading..."
                // footer visible after all S_CB rows have already arrived.  The
                // official launcher hid this footer during its page handoff.
                // Reproduce that presentation-only result without modifying the
                // page controls, selection state, packet path or executable.
                const auto screenRoot = *reinterpret_cast<volatile DWORD*>(0x0075F6E8);
                const auto clientMode = *reinterpret_cast<volatile DWORD*>(0x0064BBD4);
                if (screenRoot >= 0x10000 && clientMode == 4
                    && *reinterpret_cast<volatile DWORD*>(screenRoot + 0x0C) == 7) {
                    const int footerTop = viewport.top + MulDiv(
                        462, viewport.bottom - viewport.top,
                        static_cast<int>(g_virtualHeight));
                    if (footerTop < viewport.bottom) {
                        PatBlt(target, viewport.left, footerTop,
                            viewport.right - viewport.left,
                            viewport.bottom - footerTop, BLACKNESS);
                    }
                }
            }
            GdiFlush();
            if (presentCount <= 200 || presentCount % 120 == 0)
                Log("Present GetDC count=%ld surface=%p primary=%d source=(%ld,%ld)-(%ld,%ld) bpp=%lu caps=%08lX client=%ldx%ld stretch=%d error=%lu",
                    presentCount, surface, primary, sourceRect.left, sourceRect.top, sourceRect.right, sourceRect.bottom,
                    sourceDescription.ddpfPixelFormat.dwRGBBitCount, sourceDescription.ddsCaps.dwCaps,
                    client.right, client.bottom, stretched, GetLastError());
            if (oldMode) SetStretchBltMode(target, oldMode);
            ReleaseDC(g_window, target);
        }
        hook->releaseDC(surface, source);
    } else if (hook && hook->lock && hook->unlock) {
        DDSURFACEDESC locked{};
        locked.dwSize = sizeof(locked);
        HRESULT lockResult = hook->lock(surface, nullptr, &locked, DDLOCK_WAIT | DDLOCK_READONLY, nullptr);
        if (SUCCEEDED(lockResult) && locked.lpSurface) {
            HDC target = GetDC(g_window);
            if (target) {
                RECT client{};
                GetClientRect(g_window, &client);
                RECT viewport = AspectFit(client.right, client.bottom);
                ClearLetterboxBars(target, client, viewport);
                struct BitmapInfo565 {
                    BITMAPINFOHEADER header;
                    DWORD masks[3];
                } bitmapInfo{};
                bitmapInfo.header.biSize = sizeof(BITMAPINFOHEADER);
                bitmapInfo.header.biWidth = static_cast<LONG>(locked.dwWidth);
                bitmapInfo.header.biHeight = -static_cast<LONG>(locked.dwHeight);
                bitmapInfo.header.biPlanes = 1;
                bitmapInfo.header.biBitCount = 16;
                bitmapInfo.header.biCompression = BI_BITFIELDS;
                bitmapInfo.masks[0] = 0xF800;
                bitmapInfo.masks[1] = 0x07E0;
                bitmapInfo.masks[2] = 0x001F;
                int lines = StretchDIBits(target,
                    viewport.left, viewport.top,
                    viewport.right - viewport.left, viewport.bottom - viewport.top,
                    0, 0, locked.dwWidth, locked.dwHeight,
                    locked.lpSurface, reinterpret_cast<BITMAPINFO*>(&bitmapInfo),
                    DIB_RGB_COLORS, SRCCOPY);
                GdiFlush();
                if (presentCount <= 20)
                    Log("Present Lock count=%ld getDC=%08lX lock=%08lX pitch=%ld lines=%d error=%lu",
                        presentCount, getDcResult, lockResult, locked.lPitch, lines, GetLastError());
                ReleaseDC(g_window, target);
            }
            hook->unlock(surface, static_cast<RECT*>(locked.lpSurface));
        } else if (presentCount <= 20) {
            Log("Present failed count=%ld getDC=%08lX lock=%08lX", presentCount, getDcResult, lockResult);
        }
    } else if (presentCount <= 20) {
        Log("Present unavailable count=%ld getDC=%08lX", presentCount, getDcResult);
    }
    LeaveCriticalSection(&g_presentLock);
    InterlockedExchange(&g_presenting, 0);
}

// Window messages only queue this work; DirectDraw stays off the move callback.
volatile LONG g_windowMoveRebasePending{};
bool RebasePrimaryContentToWindow(HWND hwnd, void* primary);
bool ConsumeWindowMoveRebaseRequest(void* primary) {
    // A missing primary must retain the request until a real frame is available.
    return primary && InterlockedExchange(&g_windowMoveRebasePending, 0) != 0;
}

void ApplyWindowMoveRebase(void* primary) {
    if (ConsumeWindowMoveRebaseRequest(primary)
            && !RebasePrimaryContentToWindow(g_window, primary)) {
        // A busy surface retries on the next existing frame event. Do not
        // self-signal or spin, and do not clear a newer queued move request.
        InterlockedExchange(&g_windowMoveRebasePending, 1);
    }
}

DWORD WINAPI PresentWorker(void*) {
    Log("background-safe present worker started");
    for (;;) {
        HANDLE eventHandle = g_presentEvent;
        if (!eventHandle || WaitForSingleObject(eventHandle, INFINITE) != WAIT_OBJECT_0)
            break;
        // A LiTo frame is assembled as a short burst of DirectDraw operations.
        // Debounce that burst on this independent worker, so only the completed
        // primary reaches DWM and presentation does not depend on the game
        // window pumping WM_TIMER while it is inactive.
        Sleep(16);
        void* primary = AcquirePrimaryReference();
        if (primary) {
            ApplyWindowMoveRebase(primary);
            Present(primary);
            ReleaseSurfaceReference(primary);
        }
    }
    return 0;
}

bool EnsurePresentWorker() {
    LONG state = InterlockedCompareExchange(&g_presentWorkerState, 0, 0);
    if (state == 2) return g_presentEvent != nullptr;
    if (state == -1 || InterlockedCompareExchange(&g_presentWorkerState, 1, 0) != 0)
        return false;

    g_presentEvent = CreateEventW(nullptr, FALSE, FALSE, nullptr);
    if (!g_presentEvent) {
        InterlockedExchange(&g_presentWorkerState, -1);
        return false;
    }
    // The proxy is process-lifetime infrastructure.  Pin it before a worker can
    // execute its code so an unusual FreeLibrary call cannot unload beneath it.
    HMODULE pinned{};
    GetModuleHandleExW(GET_MODULE_HANDLE_EX_FLAG_FROM_ADDRESS | GET_MODULE_HANDLE_EX_FLAG_PIN,
        reinterpret_cast<LPCWSTR>(&EnsurePresentWorker), &pinned);
    HANDLE worker = CreateThread(nullptr, 0, &PresentWorker, nullptr, 0, nullptr);
    if (!worker) {
        CloseHandle(g_presentEvent);
        g_presentEvent = nullptr;
        InterlockedExchange(&g_presentWorkerState, -1);
        return false;
    }
    CloseHandle(worker);
    InterlockedExchange(&g_presentWorkerState, 2);
    return true;
}

void QueueWindowMoveRebase(HWND hwnd) {
    if (!hwnd || hwnd != g_window || !IsWindow(hwnd) || IsIconic(hwnd)) return;
    InterlockedExchange(&g_windowMoveRebasePending, 1);
    // Coalesce movement with the existing present worker. Never wait for its
    // critical section, surface DC or native window processing from WndProc.
    if (EnsurePresentWorker()) SetEvent(g_presentEvent);
}

void CALLBACK PresentTimerProc(HWND hwnd, UINT, UINT_PTR timerId, DWORD) {
    KillTimer(hwnd, timerId);
    InterlockedExchange(&g_presentTimerPending, 0);
    for (void* primary : g_primaries) {
        if (primary) {
            Present(primary);
            break;
        }
    }
}

void QueuePrimaryPresent(void* surface) {
    if (!surface || !IsPrimary(surface) || !g_window || !IsWindow(g_window)) return;
    // LiTo draws one frame as a burst of many Lock/Blt/BltFast operations.
    // Presenting from each hook exposes the burst's intermediate stages.  A
    // background-safe worker coalesces the burst independently of the game's
    // message pump.  LiTo throttles that pump when it loses foreground focus.
    if (EnsurePresentWorker()) {
        SetEvent(g_presentEvent);
        return;
    }
    // Retain the window-timer path as a conservative fallback if worker setup
    // is unavailable on an unusually restricted system.
    if (InterlockedCompareExchange(&g_presentTimerPending, 0, 0) != 0) return;
    DWORD now = GetTickCount();
    LONG previous = InterlockedCompareExchange(&g_lastPresentQueueTick, 0, 0);
    if (now - static_cast<DWORD>(previous) < 16) return;
    if (InterlockedCompareExchange(&g_lastPresentQueueTick, static_cast<LONG>(now), previous) != previous) return;
    if (InterlockedCompareExchange(&g_presentTimerPending, 1, 0) != 0) return;
    if (!SetTimer(g_window, kPresentTimerId, 1, &PresentTimerProc))
        InterlockedExchange(&g_presentTimerPending, 0);
}

bool RebasePrimaryContentToWindow(HWND hwnd, void* primary) {
    // A minimized top-level window may temporarily report the legacy
    // (-32000,-32000) parking position.  It is not a user move and cannot be
    // represented by the zero-based offscreen DirectDraw surface.
    if (!hwnd || !IsWindow(hwnd) || IsIconic(hwnd)) return true;
    POINT origin{};
    if (!ClientToScreen(hwnd, &origin)) return false;
    const RECT next{
        origin.x,
        origin.y,
        origin.x + static_cast<LONG>(g_virtualWidth),
        origin.y + static_cast<LONG>(g_virtualHeight)
    };

    // The original renderer mixes DirectDraw with desktop-coordinate GDI on
    // page 7.  Windows tells that GDI path about the new client origin, but the
    // static DirectDraw background remains at the primary surface's old
    // origin.  Seed the new origin from the last complete frame before using
    // it as the presentation source, so both halves of the legacy renderer
    // continue to address one coherent 640x480 canvas after a move.
    // Caller holds the exact primary reference for both rebase and presentation.
    EnterCriticalSection(&g_presentLock);
    RECT previous{};
    EnterCriticalSection(&g_lock);
    previous = g_primaryContentRect;
    LeaveCriticalSection(&g_lock);
    if (!primary || EqualRect(&previous, &next)) {
        LeaveCriticalSection(&g_presentLock);
        return primary != nullptr;
    }

    SurfaceVtableHook* hook = FindSurfaceHook(primary);
    HDC primaryDc{};
    bool copied = false;
    if (hook && hook->getDC && hook->releaseDC
        && SUCCEEDED(hook->getDC(primary, &primaryDc)) && primaryDc) {
        if (g_stableFrameValid && g_stableFrameDc) {
            copied = BitBlt(primaryDc, next.left, next.top,
                static_cast<int>(g_virtualWidth),
                static_cast<int>(g_virtualHeight),
                g_stableFrameDc, 0, 0, SRCCOPY) != FALSE;
        } else if (previous.right > previous.left
                   && previous.bottom > previous.top
                   && EnsureFrameCache()) {
            const BOOL captured = StretchBlt(g_candidateFrameDc, 0, 0,
                static_cast<int>(g_virtualWidth),
                static_cast<int>(g_virtualHeight),
                primaryDc, previous.left, previous.top,
                previous.right - previous.left,
                previous.bottom - previous.top, SRCCOPY);
            if (captured) {
                copied = BitBlt(primaryDc, next.left, next.top,
                    static_cast<int>(g_virtualWidth),
                    static_cast<int>(g_virtualHeight),
                    g_candidateFrameDc, 0, 0, SRCCOPY) != FALSE;
            }
        }
        hook->releaseDC(primary, primaryDc);
    }
    if (copied) {
        EnterCriticalSection(&g_lock);
        g_primaryContentRect = next;
        LeaveCriticalSection(&g_lock);
        Log("rebased virtual primary content (%ld,%ld) -> (%ld,%ld)",
            previous.left, previous.top, next.left, next.top);
    } else {
        Log("could not rebase virtual primary content (%ld,%ld) -> (%ld,%ld)",
            previous.left, previous.top, next.left, next.top);
    }
    LeaveCriticalSection(&g_presentLock);
    // PresentWorker presents once after this serialized operation.
    return copied;
}

LPARAM MapMouse(LPARAM original) {
    if (!g_window) return original;
    RECT client{};
    if (!GetClientRect(g_window, &client)
            || client.right <= client.left || client.bottom <= client.top)
        return original;
    int width = std::max(1L, client.right - client.left);
    int height = std::max(1L, client.bottom - client.top);
    RECT viewport = AspectFit(width, height);
    int x = GET_X_LPARAM(original) - viewport.left;
    int y = GET_Y_LPARAM(original) - viewport.top;
    x = std::clamp(MulDiv(x, static_cast<int>(g_virtualWidth),
                          std::max(1L, viewport.right - viewport.left)), 0,
                   static_cast<int>(g_virtualWidth) - 1);
    y = std::clamp(MulDiv(y, static_cast<int>(g_virtualHeight),
                          std::max(1L, viewport.bottom - viewport.top)), 0,
                   static_cast<int>(g_virtualHeight) - 1);
    return MAKELPARAM(x, y);
}

bool ShouldBridgeTournamentBuddyClan(LPARAM mapped) {
    // These addresses belong to the verified fixed-base LiTo 3.81C image.
    // Never touch them in the standalone smoke-test host or another program
    // that happens to load this DirectDraw proxy.
    if (reinterpret_cast<std::uintptr_t>(GetModuleHandleW(nullptr)) != 0x00400000)
        return false;
    MEMORY_BASIC_INFORMATION info{};
    if (!VirtualQuery(reinterpret_cast<void*>(0x0046B3D0), &info, sizeof(info))
            || info.State != MEM_COMMIT)
        return false;
    auto tournamentUi = reinterpret_cast<volatile BYTE*>(0x0062C21C);
    auto worldScene = reinterpret_cast<void* volatile*>(0x00648EBC);
    auto staticScene = reinterpret_cast<void* volatile*>(0x00834888);
    if (*tournamentUi == 0 || *worldScene != nullptr || *staticScene != nullptr)
        return false;
    const int x = GET_X_LPARAM(mapped);
    const int y = GET_Y_LPARAM(mapped);
    // sub_47CF00 constructs this native toggle at virtual (578,460).
    return x >= 568 && x <= 588 && y >= 448 && y <= 476;
}

bool IsVerifiedLiToClient() {
    if (reinterpret_cast<std::uintptr_t>(GetModuleHandleW(nullptr)) != 0x00400000)
        return false;
    wchar_t imagePath[MAX_PATH]{};
    if (!GetModuleFileNameW(nullptr, imagePath, _countof(imagePath))) return false;
    const wchar_t* imageName = wcsrchr(imagePath, L'\\');
    imageName = imageName ? imageName + 1 : imagePath;
    if (_wcsicmp(imageName, L"LiTo.bin") != 0) return false;
    MEMORY_BASIC_INFORMATION info{};
    if (!VirtualQuery(reinterpret_cast<void*>(0x004C6AE0), &info, sizeof(info))
            || info.State != MEM_COMMIT)
        return false;
    return true;
}

char* __fastcall SafeNativeItemLineLayout(
        char* text, int* nativeLineEnds, int* nativeLineCount) {
    if (!nativeLineEnds || !nativeLineCount) return nullptr;
    *nativeLineCount = 0;
    if (!text) return nullptr;

    // sub_4438F0 clears exactly 0x14 dwords at item+0x1c before calling
    // sub_443750.  The original sub_443750 nevertheless gives its generic
    // wrapper an unbounded stack array and copies every returned line end into
    // those twenty dwords.  Rich SQL armor plus the retained itemdesc can
    // exceed that capacity and corrupt the item object.
    constexpr int kNativeItemLineCapacity = 0x14;
    constexpr std::size_t kDecodedItemBufferCapacity = 0x200;
    const std::size_t measured = strnlen_s(text, kDecodedItemBufferCapacity);
    if (measured >= kDecodedItemBufferCapacity) {
        // sub_4438F0 supplies this writable 512-byte decode buffer. Force a
        // terminator before any later native strlen-style walk.
        text[kDecodedItemBufferCapacity - 1] = '\0';
    }
    const int length = static_cast<int>(std::min(
        measured, kDecodedItemBufferCapacity - 1));
    short safeLineEnds[kDecodedItemBufferCapacity + 1]{};
    int nativeWidth = 0x16;
    auto layout = reinterpret_cast<int (__fastcall*)(
        char*, int, short*, int*, int)>(0x00414080);
    int lineCount = layout(text, length, safeLineEnds, &nativeWidth, 0);
    if (lineCount < 0) lineCount = 0;
    const int retainedLines = std::min(lineCount, kNativeItemLineCapacity);
    if (lineCount > retainedLines) {
        const int end = static_cast<unsigned short>(safeLineEnds[retainedLines]);
        if (end >= 0 && end <= length) text[end] = '\0';
        Log("native item detail truncated decoded-bytes=%d lines=%d retained=%d",
            length, lineCount, retainedLines);
    }
    *nativeLineCount = retainedLines;
    for (int index = 0; index < retainedLines; ++index) {
        nativeLineEnds[index] = static_cast<unsigned short>(safeLineEnds[index + 1]);
    }
    for (char* current = text; *current; ++current) {
        if (*current == '\n') *current = ' ';
    }

    const std::size_t retainedLength = std::strlen(text);
    auto allocate = reinterpret_cast<void* (__cdecl*)(std::size_t)>(0x004F118C);
    char* duplicate = static_cast<char*>(allocate(retainedLength + 1));
    if (!duplicate) return nullptr;
    std::memcpy(duplicate, text, retainedLength + 1);
    return duplicate;
}

bool EnsureNativeItemDetailLayoutPatch() {
    if (InterlockedCompareExchange(&g_itemDetailLayoutPatchState, 0, 0) == 1)
        return true;
    if (!IsVerifiedLiToClient()) return false;

    constexpr std::uintptr_t kLayoutEntryAddress = 0x00443750;
    constexpr BYTE kExpectedEntry[5] = {0x55, 0x8B, 0xEC, 0x83, 0xEC};
    BYTE replacement[5] = {0xE9, 0, 0, 0, 0};
    const auto displacement = static_cast<std::int32_t>(
        reinterpret_cast<std::uintptr_t>(&SafeNativeItemLineLayout)
        - (kLayoutEntryAddress + sizeof(replacement)));
    std::memcpy(replacement + 1, &displacement, sizeof(displacement));

    BYTE* entry = reinterpret_cast<BYTE*>(kLayoutEntryAddress);
    if (std::memcmp(entry, replacement, sizeof(replacement)) == 0) {
        InterlockedExchange(&g_itemDetailLayoutPatchState, 1);
        return true;
    }
    if (std::memcmp(entry, kExpectedEntry, sizeof(kExpectedEntry)) != 0) {
        if (InterlockedCompareExchange(
                &g_itemDetailLayoutPatchUnexpectedLogged, 1, 0) == 0) {
            Log("native item detail layout guard skipped reason=unexpected-bytes "
                "entry=%02X%02X%02X%02X%02X", entry[0], entry[1], entry[2],
                entry[3], entry[4]);
        }
        return false;
    }

    DWORD oldProtect{};
    if (!VirtualProtect(entry, sizeof(replacement),
            PAGE_EXECUTE_READWRITE, &oldProtect)) {
        Log("native item detail layout guard failed stage=protect error=%lu",
            GetLastError());
        return false;
    }
    std::memcpy(entry, replacement, sizeof(replacement));
    FlushInstructionCache(GetCurrentProcess(), entry, sizeof(replacement));
    const bool committed = std::memcmp(
        entry, replacement, sizeof(replacement)) == 0;
    if (!committed) {
        std::memcpy(entry, kExpectedEntry, sizeof(kExpectedEntry));
        FlushInstructionCache(GetCurrentProcess(), entry, sizeof(kExpectedEntry));
    }
    DWORD ignored{};
    VirtualProtect(entry, sizeof(replacement), oldProtect, &ignored);
    if (!committed) {
        Log("native item detail layout guard failed stage=verify rolled-back=1");
        return false;
    }
    InterlockedExchange(&g_itemDetailLayoutPatchState, 1);
    Log("native item detail layout guard applied capacity=20 entry=443750 "
        "disk=unchanged");
    return true;
}

struct NumericPatch {
    BYTE* address{};
    std::vector<BYTE> expected;
    std::vector<BYTE> replacement;
    const char* name{};
};

DWORD NumericSidecarAddress(volatile LONG* value) {
    const auto address = reinterpret_cast<std::uintptr_t>(value);
    return static_cast<DWORD>(address);
}

std::vector<BYTE> DwordBytes(DWORD value) {
    return {
        static_cast<BYTE>(value),
        static_cast<BYTE>(value >> 8),
        static_cast<BYTE>(value >> 16),
        static_cast<BYTE>(value >> 24)
    };
}

DWORD ReadNumericDword(const BYTE* source) {
    DWORD value{};
    std::memcpy(&value, source, sizeof(value));
    return value;
}

BYTE NumericAbilityShadow(DWORD value) {
    return static_cast<BYTE>(std::min<DWORD>(value, 0xFE));
}

void __fastcall NumericSchema2AbilityScores(const BYTE* packet, void*) {
    if (!packet) return;
    BYTE* native = reinterpret_cast<BYTE*>(0x008348B0);
    for (size_t index = 0; index < 6; ++index) {
        const DWORD value = ReadNumericDword(packet + 1 + index * sizeof(DWORD));
        g_fullAbilities[index] = static_cast<LONG>(value);
        native[index] = NumericAbilityShadow(value);
    }
    *reinterpret_cast<BYTE*>(0x008348CB) = packet[25];
}

void __fastcall NumericSchema2Experience(const BYTE* packet, void*) {
    if (!packet) return;
    const DWORD level = ReadNumericDword(packet + 1);
    const DWORD experience = ReadNumericDword(packet + 5);
    g_fullLevel = static_cast<LONG>(level);
    *reinterpret_cast<BYTE*>(0x008348CA) =
        static_cast<BYTE>(std::min<DWORD>(level, 0xFF));
    *reinterpret_cast<DWORD*>(0x008348C4) = experience;
}

void __fastcall NumericSchema3MagicStatus(const BYTE* packet, void*) {
    if (!packet) return;
    const LONG spellPower = static_cast<LONG>(ReadNumericDword(packet + 1));
    const LONG magicResistance = static_cast<LONG>(ReadNumericDword(packet + 5));
    g_fullSpellPowerModifier = spellPower;
    g_fullMagicResistanceModifier = magicResistance;
    *reinterpret_cast<BYTE*>(0x008348DF) = static_cast<BYTE>(
        std::clamp<LONG>(spellPower, -128, 127));
    *reinterpret_cast<BYTE*>(0x008348E0) = static_cast<BYTE>(
        std::clamp<LONG>(magicResistance, -128, 127));
}

BYTE NumericSignedByteShadow(LONG value) {
    return static_cast<BYTE>(std::clamp<LONG>(value, -128, 127));
}

void __fastcall NumericSchema4ArmorAndElements(const BYTE* packet, void*) {
    if (!packet) return;
    const LONG armorClass = static_cast<LONG>(ReadNumericDword(packet + 1));
    g_fullArmorClass = armorClass;
    *reinterpret_cast<BYTE*>(0x008348B6) = NumericSignedByteShadow(armorClass);
    const std::uintptr_t nativeAddresses[4] = {
        0x008348C1, 0x008348C0, 0x008348BF, 0x008348BE,
    };
    for (size_t index = 0; index < 4; ++index) {
        const LONG value = static_cast<LONG>(
            ReadNumericDword(packet + 5 + index * sizeof(DWORD)));
        g_fullElementalResistance[index] = value;
        *reinterpret_cast<BYTE*>(nativeAddresses[index]) =
            NumericSignedByteShadow(value);
    }
}

int __cdecl NumericSchema2IntelligenceBonus() {
    const LONG intelligence = g_fullAbilities[1];
    if (intelligence <= 5) return -2;
    if (intelligence <= 8) return -1;
    if (intelligence <= 11) return 0;
    if (intelligence <= 14) return 1;
    if (intelligence <= 17) return 2;
    return intelligence - 15;
}

__declspec(naked) void NumericSchema2IndexedAbilityEbxToEcx() {
    __asm {
        mov ecx, dword ptr [g_fullAbilities + ebx * 4]
        ret
    }
}

__declspec(naked) void NumericSchema2IndexedAbilityEcxToEax() {
    __asm {
        mov eax, dword ptr [g_fullAbilities + ecx * 4]
        ret
    }
}

void AddNumericPatch(std::vector<NumericPatch>& patches, std::uintptr_t address,
                     std::initializer_list<BYTE> expected,
                     std::initializer_list<BYTE> replacement,
                     const char* name) {
    patches.push_back({reinterpret_cast<BYTE*>(address), expected, replacement, name});
}

void AddNumericPointerPatch(std::vector<NumericPatch>& patches,
                            std::uintptr_t address, BYTE opcode,
                            DWORD original, DWORD replacement,
                            const char* name) {
    auto expected = std::vector<BYTE>{opcode};
    auto oldBytes = DwordBytes(original);
    expected.insert(expected.end(), oldBytes.begin(), oldBytes.end());
    auto updated = std::vector<BYTE>{opcode};
    auto newBytes = DwordBytes(replacement);
    updated.insert(updated.end(), newBytes.begin(), newBytes.end());
    patches.push_back({reinterpret_cast<BYTE*>(address), expected, updated, name});
}

void AddNumericAbsoluteReadPatch(std::vector<NumericPatch>& patches,
                                 std::uintptr_t address, BYTE originalOpcode,
                                 BYTE dwordOpcode, int modrm, DWORD original,
                                 DWORD replacement, const char* name) {
    std::vector<BYTE> expected{originalOpcode};
    std::vector<BYTE> updated{dwordOpcode};
    if (modrm >= 0) {
        expected.push_back(static_cast<BYTE>(modrm));
        updated.push_back(static_cast<BYTE>(modrm));
    }
    auto oldBytes = DwordBytes(original);
    expected.insert(expected.end(), oldBytes.begin(), oldBytes.end());
    auto newBytes = DwordBytes(replacement);
    updated.insert(updated.end(), newBytes.begin(), newBytes.end());
    patches.push_back({reinterpret_cast<BYTE*>(address), expected, updated, name});
}

void AddNumericDwordComparePatch(std::vector<NumericPatch>& patches,
                                 std::uintptr_t address, DWORD original,
                                 DWORD replacement, BYTE immediate,
                                 const char* name) {
    std::vector<BYTE> expected{0x80, 0x3D};
    auto oldBytes = DwordBytes(original);
    expected.insert(expected.end(), oldBytes.begin(), oldBytes.end());
    expected.push_back(immediate);
    std::vector<BYTE> updated{0x83, 0x3D};
    auto newBytes = DwordBytes(replacement);
    updated.insert(updated.end(), newBytes.begin(), newBytes.end());
    updated.push_back(immediate);
    patches.push_back({reinterpret_cast<BYTE*>(address), expected, updated, name});
}

void AddNumericRelativePatch(std::vector<NumericPatch>& patches,
                             std::uintptr_t address,
                             std::initializer_list<BYTE> expected,
                             BYTE opcode, const void* destination,
                             const char* name) {
    std::vector<BYTE> updated{opcode};
    const auto target = reinterpret_cast<std::uintptr_t>(destination);
    const DWORD displacement = static_cast<DWORD>(target - (address + 5));
    auto bytes = DwordBytes(displacement);
    updated.insert(updated.end(), bytes.begin(), bytes.end());
    while (updated.size() < expected.size()) updated.push_back(0x90);
    patches.push_back({reinterpret_cast<BYTE*>(address), expected, updated, name});
}

bool IsNumericPointerExclusion(std::uintptr_t immediate, DWORD original) {
    if (original == 0x008348BC)
        return immediate == 0x004A3757 || immediate == 0x004A3909;
    if (original == 0x008348BA)
        return immediate == 0x004A3751 || immediate == 0x004AEFC9;
    return false;
}

bool AddNumericWordConsumers(std::vector<NumericPatch>& patches,
                             DWORD original, DWORD replacement,
                             size_t expectedCount, const char* name) {
    BYTE* begin = reinterpret_cast<BYTE*>(0x00401000);
    BYTE* end = reinterpret_cast<BYTE*>(0x0062B000);
    const auto oldBytes = DwordBytes(original);
    const auto newBytes = DwordBytes(replacement);
    size_t found = 0;
    for (BYTE* immediate = begin + 3; immediate + 4 <= end; ++immediate) {
        if (std::memcmp(immediate, oldBytes.data(), oldBytes.size()) != 0) continue;
        const auto immediateAddress = reinterpret_cast<std::uintptr_t>(immediate);
        if (IsNumericPointerExclusion(immediateAddress, original)) continue;

        BYTE* instruction = immediate - 3;
        const BYTE prefix = instruction[0];
        const BYTE opcode = instruction[1];
        const BYTE modrm = instruction[2];
        const bool supported =
            (prefix == 0x0F && (opcode == 0xBF || opcode == 0xB7))
            || (prefix == 0x66 && (opcode == 0x8B || opcode == 0x39 || opcode == 0x3B));
        if (!supported) {
            Log("numeric breakthrough preflight unknown %s reference @ %p bytes=%02X%02X%02X",
                name, immediate, prefix, opcode, modrm);
            return false;
        }

        std::vector<BYTE> expected{prefix, opcode, modrm};
        expected.insert(expected.end(), oldBytes.begin(), oldBytes.end());
        std::vector<BYTE> updated{static_cast<BYTE>(opcode == 0x8B ? 0x8B : opcode), modrm};
        if (prefix == 0x0F) updated[0] = 0x8B;
        updated.insert(updated.end(), newBytes.begin(), newBytes.end());
        updated.push_back(0x90);
        patches.push_back({instruction, expected, updated, name});
        ++found;
        immediate += 3;
    }
    if (found != expectedCount) {
        Log("numeric breakthrough preflight %s count mismatch got=%zu expected=%zu",
            name, found, expectedCount);
        return false;
    }
    return true;
}

bool CommitNumericPatches(std::vector<NumericPatch>& patches) {
    // Mirror the 380 Rust launcher's patch transaction contract. Validate the
    // complete immutable set, including accidental overlaps, before touching
    // any executable page.
    for (size_t index = 0; index < patches.size(); ++index) {
        const auto& patch = patches[index];
        if (patch.expected.empty() || patch.expected.size() != patch.replacement.size()) {
            Log("numeric breakthrough invalid patch name=%s expected=%zu replacement=%zu",
                patch.name, patch.expected.size(), patch.replacement.size());
            return false;
        }
        const auto begin = reinterpret_cast<std::uintptr_t>(patch.address);
        const auto end = begin + patch.expected.size();
        for (size_t otherIndex = index + 1; otherIndex < patches.size(); ++otherIndex) {
            const auto& other = patches[otherIndex];
            const auto otherBegin = reinterpret_cast<std::uintptr_t>(other.address);
            const auto otherEnd = otherBegin + other.expected.size();
            if (begin < otherEnd && otherBegin < end) {
                Log("numeric breakthrough overlapping patches first=%s second=%s",
                    patch.name, other.name);
                return false;
            }
        }
    }
    for (const auto& patch : patches) {
        if (std::memcmp(patch.address, patch.expected.data(), patch.expected.size()) != 0) {
            Log("numeric breakthrough preflight mismatch name=%s address=%p",
                patch.name, patch.address);
            return false;
        }
    }

    // Collect ids before suspending anything so vector allocation cannot wait
    // on a heap lock held by a thread that this transaction already stopped.
    std::vector<DWORD> threadIds;
    const DWORD currentProcessId = GetCurrentProcessId();
    const DWORD currentThreadId = GetCurrentThreadId();
    HANDLE snapshot = CreateToolhelp32Snapshot(TH32CS_SNAPTHREAD, 0);
    if (snapshot == INVALID_HANDLE_VALUE) {
        Log("numeric breakthrough thread snapshot failed error=%lu", GetLastError());
        return false;
    }
    THREADENTRY32 entry{};
    entry.dwSize = sizeof(entry);
    if (!Thread32First(snapshot, &entry)) {
        const DWORD error = GetLastError();
        CloseHandle(snapshot);
        Log("numeric breakthrough thread enumeration failed error=%lu", error);
        return false;
    }
    do {
        if (entry.th32OwnerProcessID == currentProcessId
                && entry.th32ThreadID != currentThreadId) {
            threadIds.push_back(entry.th32ThreadID);
        }
    } while (Thread32Next(snapshot, &entry));
    CloseHandle(snapshot);

    std::vector<HANDLE> suspendedThreads;
    suspendedThreads.reserve(threadIds.size());
    bool suspendFailed = false;
    DWORD suspendError = ERROR_SUCCESS;
    for (DWORD threadId : threadIds) {
        HANDLE thread = OpenThread(THREAD_SUSPEND_RESUME, FALSE, threadId);
        if (!thread) {
            suspendError = GetLastError();
            // A thread that exited after the snapshot cannot execute patched
            // code and therefore does not invalidate the transaction.
            if (suspendError == ERROR_INVALID_PARAMETER) continue;
            suspendFailed = true;
            break;
        }
        if (SuspendThread(thread) == static_cast<DWORD>(-1)) {
            suspendError = GetLastError();
            CloseHandle(thread);
            suspendFailed = true;
            break;
        }
        suspendedThreads.push_back(thread);
    }
    auto resumeAll = [&suspendedThreads]() {
        bool resumed = true;
        for (auto iterator = suspendedThreads.rbegin();
                iterator != suspendedThreads.rend(); ++iterator) {
            if (ResumeThread(*iterator) == static_cast<DWORD>(-1)) resumed = false;
            CloseHandle(*iterator);
        }
        suspendedThreads.clear();
        return resumed;
    };
    if (suspendFailed) {
        const bool resumed = resumeAll();
        Log("numeric breakthrough thread suspension failed error=%lu resumed=%d",
            suspendError, resumed ? 1 : 0);
        return false;
    }

    // The Rust reference rechecks after suspension to close the preflight/
    // commit race. Do the same before the first write.
    const NumericPatch* changedAfterPreflight = nullptr;
    for (const auto& patch : patches) {
        if (std::memcmp(patch.address, patch.expected.data(), patch.expected.size()) != 0) {
            changedAfterPreflight = &patch;
            break;
        }
    }
    if (changedAfterPreflight) {
        const bool resumed = resumeAll();
        Log("numeric breakthrough changed after preflight name=%s address=%p resumed=%d",
            changedAfterPreflight->name, changedAfterPreflight->address, resumed ? 1 : 0);
        return false;
    }

    size_t committed = 0;
    for (; committed < patches.size(); ++committed) {
        auto& patch = patches[committed];
        DWORD oldProtect{};
        if (!VirtualProtect(patch.address, patch.replacement.size(),
                PAGE_EXECUTE_READWRITE, &oldProtect)) break;
        std::memcpy(patch.address, patch.replacement.data(), patch.replacement.size());
        FlushInstructionCache(GetCurrentProcess(), patch.address, patch.replacement.size());
        DWORD ignored{};
        const BOOL restored = VirtualProtect(
            patch.address, patch.replacement.size(), oldProtect, &ignored);
        if (!restored
                || std::memcmp(patch.address, patch.replacement.data(), patch.replacement.size()) != 0) {
            // Include the current site in the reverse rollback even when its
            // write was only partially observable or protection restoration failed.
            ++committed;
            break;
        }
    }
    if (committed == patches.size()) {
        const bool resumed = resumeAll();
        if (!resumed) {
            Log("numeric breakthrough commit succeeded but thread resume failed");
        }
        return true;
    }

    bool rollbackVerified = true;
    while (committed > 0) {
        auto& patch = patches[--committed];
        DWORD oldProtect{};
        if (!VirtualProtect(patch.address, patch.expected.size(),
                PAGE_EXECUTE_READWRITE, &oldProtect)) {
            rollbackVerified = false;
            continue;
        }
        std::memcpy(patch.address, patch.expected.data(), patch.expected.size());
        FlushInstructionCache(GetCurrentProcess(), patch.address, patch.expected.size());
        DWORD ignored{};
        if (!VirtualProtect(patch.address, patch.expected.size(), oldProtect, &ignored)
                || std::memcmp(patch.address, patch.expected.data(), patch.expected.size()) != 0) {
            rollbackVerified = false;
        }
    }
    const bool resumed = resumeAll();
    Log("numeric breakthrough commit failed rolled-back=%d resumed=%d",
        rollbackVerified ? 1 : 0, resumed ? 1 : 0);
    return false;
}

void AddNumericSchema2Patches(std::vector<NumericPatch>& patches) {
    const DWORD fullLevel = NumericSidecarAddress(&g_fullLevel);
    DWORD fullAbilities[6]{};
    for (size_t index = 0; index < 6; ++index)
        fullAbilities[index] = NumericSidecarAddress(&g_fullAbilities[index]);

    // S_STATUS keeps its one-byte level shadow, but widens the six runtime
    // abilities in the selected client's STR, INT, WIS, DEX, CON, CHA order.
    AddNumericPatch(patches, 0x006342CF,
        {0x63,0x63,0x63,0x63,0x63,0x63},
        {0x64,0x64,0x64,0x64,0x64,0x64}, "S_STATUS ability formats");
    const struct {
        std::uintptr_t address;
        DWORD original;
        size_t sidecar;
        const char* name;
    } statusAbilities[] = {
        {0x004A375C, 0x008348B5, 5, "S_STATUS CHA destination"},
        {0x004A3761, 0x008348B4, 4, "S_STATUS CON destination"},
        {0x004A3766, 0x008348B3, 3, "S_STATUS DEX destination"},
        {0x004A376B, 0x008348B2, 2, "S_STATUS WIS destination"},
        {0x004A3770, 0x008348B1, 1, "S_STATUS INT destination"},
        {0x004A3775, 0x008348B0, 0, "S_STATUS STR destination"},
    };
    for (const auto& ability : statusAbilities) {
        AddNumericPointerPatch(patches, ability.address, 0x68,
            ability.original, fullAbilities[ability.sidecar], ability.name);
    }

    // These two native receivers contain only packet-to-global copies.  Entry
    // detours avoid partial byte stores and keep the wire contract explicit.
    AddNumericRelativePatch(patches, 0x004AF0A0,
        {0x8A,0x41,0x01,0x41,0x41}, 0xE9,
        reinterpret_cast<const void*>(&NumericSchema2AbilityScores),
        "S_ABILITY_SCORES schema2 decoder");
    AddNumericRelativePatch(patches, 0x004AF0E0,
        {0x68,0xC4,0x48,0x83,0x00}, 0xE9,
        reinterpret_cast<const void*>(&NumericSchema2Experience),
        "S_EXP schema2 decoder");

    // Level consumers that naturally use the whole destination register can
    // be widened in place without changing instruction size.
    const std::uintptr_t levelEaxReads[] = {
        0x0040A0A0, 0x0040B5A3, 0x004B0D3D, 0x004C8C1A,
        0x004C8C35, 0x004C8D30, 0x004CBB41, 0x004D1BD1,
    };
    for (const auto address : levelEaxReads) {
        AddNumericAbsoluteReadPatch(patches, address, 0xA0, 0xA1, -1,
            0x008348CA, fullLevel, "level EAX consumer");
    }
    const std::uintptr_t levelEcxReads[] = {
        0x0048A4CE, 0x004C8BEE, 0x004C8C55, 0x004CDE1E,
        0x004CDE38, 0x004CDE67, 0x004CDEA5, 0x004CE29C,
        0x004CE2B6, 0x004CE2CB, 0x004CE31B, 0x004CEC9D,
        0x004CECBA, 0x004CECD3, 0x004CED29, 0x004D116B,
        0x004D117F, 0x004D119B, 0x004D11AE, 0x004D11C0,
        0x004D1213, 0x004D122B, 0x004D19FB, 0x004D1A0F,
        0x004D1A2A, 0x004D1C06,
    };
    for (const auto address : levelEcxReads) {
        AddNumericAbsoluteReadPatch(patches, address, 0x8A, 0x8B, 0x0D,
            0x008348CA, fullLevel, "level ECX consumer");
    }
    const std::uintptr_t levelEdxReads[] = {0x004CDE8B, 0x004CE2FA};
    for (const auto address : levelEdxReads) {
        AddNumericAbsoluteReadPatch(patches, address, 0x8A, 0x8B, 0x15,
            0x008348CA, fullLevel, "level EDX consumer");
    }
    const struct { std::uintptr_t address; BYTE immediate; } levelCompares[] = {
        {0x004075AD, 0x32}, {0x00409D17, 0x32},
        {0x004A5673, 0x01}, {0x004C8C0D, 0x32},
        {0x004CAC1F, 0x32},
    };
    for (const auto& comparison : levelCompares) {
        AddNumericDwordComparePatch(patches, comparison.address,
            0x008348CA, fullLevel, comparison.immediate, "level dword compare");
    }

    // 0x40A160 and 0x4E7AE4 intentionally retain the saturated native level
    // shadow because their following instructions consume AL-only threshold
    // categories.  The S_EXP detour synchronizes that shadow.

    // Direct DEX consumers can be widened in place.
    AddNumericAbsoluteReadPatch(patches, 0x004CDE19, 0xA0, 0xA1, -1,
        0x008348B3, fullAbilities[3], "DEX EAX consumer A");
    AddNumericAbsoluteReadPatch(patches, 0x004CDE32, 0x8A, 0x8B, 0x15,
        0x008348B3, fullAbilities[3], "DEX EDX consumer");
    AddNumericAbsoluteReadPatch(patches, 0x004CDE5D, 0x8A, 0x8B, 0x0D,
        0x008348B3, fullAbilities[3], "DEX ECX consumer A");
    AddNumericAbsoluteReadPatch(patches, 0x004CDE85, 0x8A, 0x8B, 0x0D,
        0x008348B3, fullAbilities[3], "DEX ECX consumer B");
    AddNumericAbsoluteReadPatch(patches, 0x004CDEA0, 0xA0, 0xA1, -1,
        0x008348B3, fullAbilities[3], "DEX EAX consumer B");

    // WIS initially flows through CL and then truncates into AL.  Widen both
    // moves and the default-range comparison as one preflighted block.
    AddNumericAbsoluteReadPatch(patches, 0x004C8CD0, 0x8A, 0x8B, 0x0D,
        0x008348B2, fullAbilities[2], "WIS ECX consumer");
    AddNumericPatch(patches, 0x004C8CD6,
        {0x33,0xC0,0x8A,0xC1}, {0x8B,0xC1,0x90,0x90},
        "WIS ECX-to-EAX truncation");
    AddNumericPatch(patches, 0x004C8D23,
        {0x80,0xF9,0x19}, {0x83,0xF9,0x19}, "WIS upper range compare");

    // INT contains several AL-only branches, so replace its complete leaf
    // function with an equivalent dword implementation.
    AddNumericRelativePatch(patches, 0x004C8B90,
        {0xA0,0xB1,0x48,0x83,0x00}, 0xE9,
        reinterpret_cast<const void*>(&NumericSchema2IntelligenceBonus),
        "INT bonus schema2 function");

    // The two status-page loops index a six-byte array.  Calls into tiny naked
    // helpers preserve surrounding registers while applying dword stride.
    AddNumericRelativePatch(patches, 0x004D111B,
        {0x8A,0x8B,0xB0,0x48,0x83,0x00}, 0xE8,
        reinterpret_cast<const void*>(&NumericSchema2IndexedAbilityEbxToEcx),
        "indexed ability EBX-to-ECX");
    AddNumericRelativePatch(patches, 0x004D18F7,
        {0x8A,0x81,0xB0,0x48,0x83,0x00}, 0xE8,
        reinterpret_cast<const void*>(&NumericSchema2IndexedAbilityEcxToEax),
        "indexed ability ECX-to-EAX");
}

void AddNumericSchema3Patches(std::vector<NumericPatch>& patches) {
    const DWORD fullSpellPower = NumericSidecarAddress(&g_fullSpellPowerModifier);
    const DWORD fullMagicResistance = NumericSidecarAddress(
        &g_fullMagicResistanceModifier);

    // The original S_MAGIC_STATUS receiver is exactly `cc` at sub_4AF320.
    // Schema 3 replaces the receiver entry so no shared `cc` format string is
    // modified, then redirects every direct SP/MR consumer to the dword
    // sidecars. The original byte globals remain saturated compatibility
    // shadows for code outside the proven direct-reference census.
    AddNumericRelativePatch(patches, 0x004AF320,
        {0x68,0xE0,0x48,0x83,0x00}, 0xE9,
        reinterpret_cast<const void*>(&NumericSchema3MagicStatus),
        "S_MAGIC_STATUS schema3 decoder");
    const auto addSignedByteToDwordEaxPatch = [&patches](
            std::uintptr_t address, DWORD original, DWORD replacement,
            const char* name) {
        std::vector<BYTE> expected{0x0F,0xBE,0x05};
        auto oldAddress = DwordBytes(original);
        expected.insert(expected.end(), oldAddress.begin(), oldAddress.end());
        std::vector<BYTE> updated{0xA1};
        auto newAddress = DwordBytes(replacement);
        updated.insert(updated.end(), newAddress.begin(), newAddress.end());
        updated.push_back(0x90);
        updated.push_back(0x90);
        patches.push_back({reinterpret_cast<BYTE*>(address), expected, updated, name});
    };
    addSignedByteToDwordEaxPatch(0x004C8CB1, 0x008348DF,
        fullSpellPower, "SP modifier dword consumer");
    const std::uintptr_t mrReads[] = {
        0x004C8D4C, 0x004C8D60, 0x004C8D74, 0x004C8D88,
    };
    for (const auto address : mrReads) {
        addSignedByteToDwordEaxPatch(address, 0x008348E0,
            fullMagicResistance, "MR modifier dword consumer");
    }
    {
        std::vector<BYTE> expected{0x80,0x05,0xE0,0x48,0x83,0x00,0x14};
        std::vector<BYTE> replacement{0x83,0x05};
        auto address = DwordBytes(fullMagicResistance);
        replacement.insert(replacement.end(), address.begin(), address.end());
        replacement.push_back(0x14);
        patches.push_back({reinterpret_cast<BYTE*>(0x004B17B4), expected,
            replacement, "MR local plus-20 dword mutation"});
    }
}

void AddNumericSchema4Patches(std::vector<NumericPatch>& patches) {
    DWORD fullElements[4]{};
    for (size_t index = 0; index < 4; ++index) {
        fullElements[index] = NumericSidecarAddress(
            &g_fullElementalResistance[index]);
    }

    // S_STATUS resistance wire order is fire/water/wind/earth, while the
    // receiver pushes native destinations earth/wind/water/fire.  Widen the
    // four exact format slots and redirect each destination to its semantic
    // sidecar without replacing the rest of sub_4A36F0.
    AddNumericPatch(patches, 0x006342DE,
        {0x63,0x63,0x63,0x63}, {0x64,0x64,0x64,0x64},
        "S_STATUS elemental resistance formats");
    AddNumericPointerPatch(patches, 0x004A3723, 0x68, 0x008348BE,
        fullElements[3], "S_STATUS earth destination");
    AddNumericPointerPatch(patches, 0x004A3728, 0x68, 0x008348BF,
        fullElements[2], "S_STATUS wind destination");
    AddNumericPointerPatch(patches, 0x004A372D, 0x68, 0x008348C0,
        fullElements[1], "S_STATUS water destination");
    AddNumericPointerPatch(patches, 0x004A3732, 0x68, 0x008348C1,
        fullElements[0], "S_STATUS fire destination");

    // S_AC is a compact ccccc receiver.  Schema 4 replaces it as one decoder
    // so AC and all four resistances are committed together and native byte
    // shadows stay saturated for any unproven byte-only code.
    AddNumericRelativePatch(patches, 0x004AF100,
        {0x68,0xBE,0x48,0x83,0x00}, 0xE9,
        reinterpret_cast<const void*>(&NumericSchema4ArmorAndElements),
        "S_AC schema4 decoder");

    const struct {
        std::uintptr_t address;
        DWORD original;
        size_t sidecar;
        const char* name;
    } reads[] = {
        {0x004D1374, 0x008348BE, 3, "earth resistance consumer A"},
        {0x004D195D, 0x008348BE, 3, "earth resistance consumer B"},
        {0x004D13DB, 0x008348BF, 2, "wind resistance consumer A"},
        {0x004D1983, 0x008348BF, 2, "wind resistance consumer B"},
        {0x004D1398, 0x008348C0, 1, "water resistance consumer A"},
        {0x004D19C3, 0x008348C0, 1, "water resistance consumer B"},
        {0x004D13B4, 0x008348C1, 0, "fire resistance consumer A"},
        {0x004D19A0, 0x008348C1, 0, "fire resistance consumer B"},
    };
    for (const auto& read : reads) {
        std::vector<BYTE> expected{0x0F,0xBE,0x0D};
        auto oldAddress = DwordBytes(read.original);
        expected.insert(expected.end(), oldAddress.begin(), oldAddress.end());
        std::vector<BYTE> replacement{0x8B,0x0D};
        auto newAddress = DwordBytes(fullElements[read.sidecar]);
        replacement.insert(replacement.end(), newAddress.begin(), newAddress.end());
        replacement.push_back(0x90);
        patches.push_back({reinterpret_cast<BYTE*>(read.address), expected,
            replacement, read.name});
    }
}

bool EnsureNumericBreakthroughPatches() {
    // 0=not attempted, 2=one thread is installing, 1=installed, -1=failed.
    // DirectDraw exports may be entered concurrently during startup.  Only one
    // caller may own the transactional write; every other caller waits for its
    // fail-closed result instead of preflighting or rewriting the same sites.
    constexpr LONG kInstalling = 2;
    constexpr LONG kInstalled = 1;
    constexpr LONG kFailed = -1;
    const LONG observed = InterlockedCompareExchange(
        &g_numericBreakthroughPatchState, kInstalling, 0);
    if (observed == kInstalled) return true;
    if (observed == kFailed) return false;
    if (observed == kInstalling) {
        const ULONGLONG deadline = GetTickCount64() + 5000;
        LONG state = kInstalling;
        while (state == kInstalling && GetTickCount64() < deadline) {
            Sleep(1);
            state = InterlockedCompareExchange(
                &g_numericBreakthroughPatchState, 0, 0);
        }
        if (state == kInstalling) {
            Log("numeric breakthrough install wait timed out");
            return false;
        }
        return state == kInstalled;
    }

    auto failInstall = [=]() {
        InterlockedExchange(&g_numericBreakthroughPatchState, kFailed);
        return false;
    };
    if (!IsVerifiedLiToClient()) return failInstall();

    const DWORD fullHp = NumericSidecarAddress(&g_fullMaximumHp);
    const DWORD fullMp = NumericSidecarAddress(&g_fullMaximumMp);
    const DWORD fullAc = NumericSidecarAddress(&g_fullArmorClass);
    std::vector<NumericPatch> patches;

    // S_STATUS: current HP, max HP, current MP and max MP become dword.
    AddNumericPatch(patches, 0x006342D5,
        {0x68, 0x68, 0x68, 0x68}, {0x64, 0x64, 0x64, 0x64}, "S_STATUS HP/MP format");
    AddNumericPointerPatch(patches, 0x004A3750, 0x68, 0x008348BA, fullMp,
        "S_STATUS max MP destination");
    AddNumericPointerPatch(patches, 0x004A3756, 0x68, 0x008348BC, fullHp,
        "S_STATUS max HP destination");
    AddNumericPatch(patches, 0x004A3799,
        {0x81,0xE1,0xFF,0xFF,0x00,0x00}, {0x90,0x90,0x90,0x90,0x90,0x90},
        "S_STATUS current HP truncation");
    AddNumericPatch(patches, 0x004A37B2,
        {0x81,0xE2,0xFF,0xFF,0x00,0x00}, {0x90,0x90,0x90,0x90,0x90,0x90},
        "S_STATUS current MP truncation");

    // S_HIT_POINT and S_MANA_POINT: hh -> dd without a shared reader cave.
    AddNumericPatch(patches, 0x004A38F9, {0x0F,0xBF,0x06}, {0x8B,0x06,0x90},
        "S_HIT_POINT current HP dword");
    AddNumericPatch(patches, 0x004A3902, {0x66,0x8B,0x4E,0x02}, {0x8B,0x4E,0x04,0x90},
        "S_HIT_POINT max HP offset");
    {
        auto expected = std::vector<BYTE>{0x66,0x89,0x0D,0xBC,0x48,0x83,0x00};
        auto replacement = std::vector<BYTE>{0x89,0x0D};
        auto address = DwordBytes(fullHp);
        replacement.insert(replacement.end(), address.begin(), address.end());
        replacement.push_back(0x90);
        patches.push_back({reinterpret_cast<BYTE*>(0x004A3906), expected, replacement,
            "S_HIT_POINT max HP store"});
    }
    AddNumericPatch(patches, 0x004AEFB9, {0x0F,0xBF,0x06}, {0x8B,0x06,0x90},
        "S_MANA_POINT current MP dword");
    AddNumericPatch(patches, 0x004AEFC2, {0x66,0x8B,0x4E,0x02}, {0x8B,0x4E,0x04,0x90},
        "S_MANA_POINT max MP offset");
    {
        auto expected = std::vector<BYTE>{0x66,0x89,0x0D,0xBA,0x48,0x83,0x00};
        auto replacement = std::vector<BYTE>{0x89,0x0D};
        auto address = DwordBytes(fullMp);
        replacement.insert(replacement.end(), address.begin(), address.end());
        replacement.push_back(0x90);
        patches.push_back({reinterpret_cast<BYTE*>(0x004AEFC6), expected, replacement,
            "S_MANA_POINT max MP store"});
    }

    if (!AddNumericWordConsumers(patches, 0x008348BC, fullHp, 16, "max HP consumer")
            || !AddNumericWordConsumers(patches, 0x008348BA, fullMp, 10, "max MP consumer")) {
        return failInstall();
    }
    AddNumericPatch(patches, 0x00489962, {0x66,0x85,0xC9}, {0x85,0xC9,0x90},
        "max HP positive test A");
    AddNumericPatch(patches, 0x00489982, {0x0F,0xBF,0xC9}, {0x8B,0xC9,0x90},
        "max HP effect divisor A dword");
    AddNumericPatch(patches, 0x00489B46, {0x66,0x85,0xC9}, {0x85,0xC9,0x90},
        "max HP positive test B");
    AddNumericPatch(patches, 0x00489B66, {0x0F,0xBF,0xC9}, {0x8B,0xC9,0x90},
        "max HP effect divisor B dword");
    AddNumericPatch(patches, 0x004CE89F, {0x66,0x85,0xD2}, {0x85,0xD2,0x90},
        "max HP positive test C");
    // The bar renderer reloads the widened maximum into EDX and then, just
    // before IDIV, sign-extends DX back to a dword.  Values whose low word has
    // bit 15 set (for example 100000 -> -31072) otherwise produce a negative
    // source rectangle and make sub_4BCE30 copy almost 4 GiB.
    AddNumericPatch(patches, 0x004CE8CC, {0x0F,0xBF,0xCA}, {0x8B,0xCA,0x90},
        "max HP bar divisor dword");
    AddNumericPatch(patches, 0x004CEB3F, {0x66,0x85,0xD2}, {0x85,0xD2,0x90},
        "max MP positive test");
    AddNumericPatch(patches, 0x004CEB6C, {0x0F,0xBF,0xCA}, {0x8B,0xCA,0x90},
        "max MP bar divisor dword");

    // AC is a dword only for the base value; the four modifier fields stay bytes.
    AddNumericPatch(patches, 0x006342D9, {0x63}, {0x64}, "S_STATUS AC format");
    AddNumericPointerPatch(patches, 0x004A374B, 0x68, 0x008348B6, fullAc,
        "S_STATUS AC destination");
    AddNumericPatch(patches, 0x0062B9FC, {0x63}, {0x64}, "S_AC base format");
    AddNumericPointerPatch(patches, 0x004AF114, 0x68, 0x008348B6, fullAc,
        "S_AC base destination");
    {
        const struct { std::uintptr_t address; BYTE modrm; const char* name; } reads[] = {
            {0x004CE226, 0x05, "AC compute"},
            {0x004D1149, 0x0D, "AC status text"},
            {0x004D22CA, 0x0D, "AC secondary text"},
        };
        for (const auto& read : reads) {
            std::vector<BYTE> expected{0x0F,0xBE,read.modrm,0xB6,0x48,0x83,0x00};
            std::vector<BYTE> replacement;
            if (read.modrm == 0x05) replacement = {0xA1};
            else replacement = {0x8B, read.modrm};
            auto address = DwordBytes(fullAc);
            replacement.insert(replacement.end(), address.begin(), address.end());
            while (replacement.size() < expected.size()) replacement.push_back(0x90);
            patches.push_back({reinterpret_cast<BYTE*>(read.address), expected, replacement, read.name});
        }
    }

    if (g_numericBreakthroughSchema >= 2) AddNumericSchema2Patches(patches);
    if (g_numericBreakthroughSchema >= 3) AddNumericSchema3Patches(patches);
    if (g_numericBreakthroughSchema >= 4) AddNumericSchema4Patches(patches);

    if (!CommitNumericPatches(patches)) {
        if (InterlockedCompareExchange(
                &g_numericBreakthroughPatchUnexpectedLogged, 1, 0) == 0) {
            Log("numeric breakthrough disabled reason=preflight-or-commit-failure");
        }
        return failInstall();
    }
    InterlockedExchange(&g_numericBreakthroughPatchState, kInstalled);
    Log("numeric breakthrough installed schema=%d hp-mp=dword ac=dword level-ability=%s sp-mr=%s elements=%s sites=%zu disk=unchanged",
        g_numericBreakthroughSchema, g_numericBreakthroughSchema >= 2 ? "dword" : "native",
        g_numericBreakthroughSchema >= 3 ? "dword" : "native",
        g_numericBreakthroughSchema >= 4 ? "dword" : "native", patches.size());
    return true;
}

bool EnsureNativeRunClanPacketPatch() {
    if (InterlockedCompareExchange(&g_runClanPacketPatchState, 0, 0) == 1)
        return true;
    if (!IsVerifiedLiToClient()) return false;

    // The selected LiTo image has two 19-byte name-input cases.  Both call
    // sub_4CAFB0, but its callback accidentally hard-codes column 3.  The
    // otherwise-unused byte_636014 writes below were intended to distinguish
    // Call Clan (1) from Run Clan (0), as the same-version official callback
    // does.  Redirect only those two seven-byte writes to the callback's
    // immediate byte at 0x004CB03E: case115 restores 3 and case117 selects 5.
    // No executable or resource file is changed on disk.
    constexpr std::uintptr_t kCallClanCaseAddress = 0x004CBBAE;
    constexpr std::uintptr_t kRunClanCaseAddress = 0x004CBBC1;
    constexpr std::size_t kInstructionSize = 7;
    constexpr std::size_t kPatchSpan =
        (kRunClanCaseAddress - kCallClanCaseAddress) + kInstructionSize;
    const BYTE expectedCall[kInstructionSize] = {
        0xC6, 0x05, 0x14, 0x60, 0x63, 0x00, 0x01
    };
    const BYTE expectedRun[kInstructionSize] = {
        0xC6, 0x05, 0x14, 0x60, 0x63, 0x00, 0x00
    };
    const BYTE repairedCall[kInstructionSize] = {
        0xC6, 0x05, 0x3E, 0xB0, 0x4C, 0x00, 0x03
    };
    const BYTE repairedRun[kInstructionSize] = {
        0xC6, 0x05, 0x3E, 0xB0, 0x4C, 0x00, 0x05
    };
    BYTE* callCase = reinterpret_cast<BYTE*>(kCallClanCaseAddress);
    BYTE* runCase = reinterpret_cast<BYTE*>(kRunClanCaseAddress);
    if (std::memcmp(callCase, repairedCall, kInstructionSize) == 0
            && std::memcmp(runCase, repairedRun, kInstructionSize) == 0) {
        InterlockedExchange(&g_runClanPacketPatchState, 1);
        return true;
    }
    if (std::memcmp(callCase, expectedCall, kInstructionSize) != 0
            || std::memcmp(runCase, expectedRun, kInstructionSize) != 0) {
        if (InterlockedCompareExchange(
                &g_runClanPacketPatchUnexpectedLogged, 1, 0) == 0) {
            Log("native Run Clan packet repair skipped reason=unexpected-bytes "
                "call=%02X%02X run=%02X%02X",
                callCase[0], callCase[1], runCase[0], runCase[1]);
        }
        return false;
    }

    DWORD oldProtect{};
    if (!VirtualProtect(callCase, kPatchSpan,
            PAGE_EXECUTE_READWRITE, &oldProtect)) {
        Log("native Run Clan packet repair failed stage=protect error=%lu",
            GetLastError());
        return false;
    }
    std::memcpy(callCase, repairedCall, kInstructionSize);
    std::memcpy(runCase, repairedRun, kInstructionSize);
    FlushInstructionCache(GetCurrentProcess(), callCase, kPatchSpan);
    const bool committed =
        std::memcmp(callCase, repairedCall, kInstructionSize) == 0
        && std::memcmp(runCase, repairedRun, kInstructionSize) == 0;
    if (!committed) {
        std::memcpy(callCase, expectedCall, kInstructionSize);
        std::memcpy(runCase, expectedRun, kInstructionSize);
        FlushInstructionCache(GetCurrentProcess(), callCase, kPatchSpan);
    }
    DWORD ignored{};
    VirtualProtect(callCase, kPatchSpan, oldProtect, &ignored);
    if (!committed) {
        Log("native Run Clan packet repair failed stage=verify rolled-back=1");
        return false;
    }
    InterlockedExchange(&g_runClanPacketPatchState, 1);
    Log("native Run Clan packet repair applied call-selector=14/3 "
        "run-selector=14/5 disk=unchanged");
    return true;
}

bool EnsureNativeWorldTreePacketPatch() {
    if (InterlockedCompareExchange(&g_worldTreePacketPatchState, 0, 0) == 1)
        return true;
    if (!IsVerifiedLiToClient()) return false;

    // The selected LiTo image routes zero-based skill case130 (skill131,
    // World Tree's Call) through byte-table entry 3.  That shared target calls
    // sub_4CAB70, whose first comparison returns immediately for selector 0x82,
    // so the click emits no packet.  The same-version official image emits the
    // ordinary selector-only request 06 10 02.  Entry 1 is LiTo's already-used
    // selector-only producer and therefore yields the LiTo opcode 14 10 02
    // without introducing a custom sender or changing LiTo.bin on disk.
    constexpr std::uintptr_t kWorldTreeCaseEntryAddress = 0x004CBD82;
    constexpr BYTE kExpectedEntry = 0x03;
    constexpr BYTE kRepairedEntry = 0x01;
    BYTE* entry = reinterpret_cast<BYTE*>(kWorldTreeCaseEntryAddress);
    if (*entry == kRepairedEntry) {
        InterlockedExchange(&g_worldTreePacketPatchState, 1);
        return true;
    }
    if (*entry != kExpectedEntry) {
        if (InterlockedCompareExchange(
                &g_worldTreePacketPatchUnexpectedLogged, 1, 0) == 0) {
            Log("native World Tree packet repair skipped reason=unexpected-byte "
                "entry=%02X", *entry);
        }
        return false;
    }

    DWORD oldProtect{};
    if (!VirtualProtect(entry, sizeof(*entry),
            PAGE_EXECUTE_READWRITE, &oldProtect)) {
        Log("native World Tree packet repair failed stage=protect error=%lu",
            GetLastError());
        return false;
    }
    *entry = kRepairedEntry;
    FlushInstructionCache(GetCurrentProcess(), entry, sizeof(*entry));
    const bool committed = *entry == kRepairedEntry;
    if (!committed) {
        *entry = kExpectedEntry;
        FlushInstructionCache(GetCurrentProcess(), entry, sizeof(*entry));
    }
    DWORD ignored{};
    VirtualProtect(entry, sizeof(*entry), oldProtect, &ignored);
    if (!committed) {
        Log("native World Tree packet repair failed stage=verify rolled-back=1");
        return false;
    }
    InterlockedExchange(&g_worldTreePacketPatchState, 1);
    Log("native World Tree packet repair applied packet=14 10 02 selector=16/2 "
        "table-entry=3->1 disk=unchanged");
    return true;
}

bool EnsureNativeTournamentShopCurrencyPatch() {
    if (InterlockedCompareExchange(&g_tournamentShopCurrencyPatchState, 0, 0) == 1)
        return true;
    if (!IsVerifiedLiToClient()) return false;

    // Tournament's buy/sell list renderers reuse the ordinary-world Adena
    // counter.  In the standalone lobby dword_648EBC is intentionally null,
    // so each renderer faults in sub_447FF0 as soon as its otherwise-valid
    // native window becomes visible.  Replace only those five renderer calls
    // with the Tournament allowance (20,000); the ordinary world counter and
    // every non-shop caller remain untouched.
    struct Site { std::uintptr_t address; BYTE expected[5]; };
    constexpr Site sites[] = {
        {0x004773C9, {0xE8, 0x22, 0x0C, 0xFD, 0xFF}},
        {0x0047745C, {0xE8, 0x8F, 0x0B, 0xFD, 0xFF}},
        {0x0047749C, {0xE8, 0x4F, 0x0B, 0xFD, 0xFF}},
        {0x004774CD, {0xE8, 0x1E, 0x0B, 0xFD, 0xFF}},
        {0x004776AC, {0xE8, 0x3F, 0x09, 0xFD, 0xFF}},
    };
    constexpr BYTE replacement[5] = {0xB8, 0x20, 0x4E, 0x00, 0x00};
    for (const auto& site : sites) {
        const BYTE* current = reinterpret_cast<const BYTE*>(site.address);
        if (std::memcmp(current, site.expected, sizeof(site.expected)) != 0
                && std::memcmp(current, replacement, sizeof(replacement)) != 0) {
            if (InterlockedCompareExchange(
                    &g_tournamentShopCurrencyPatchUnexpectedLogged, 1, 0) == 0) {
                Log("Tournament shop currency guard skipped address=%p bytes=%02X%02X%02X%02X%02X",
                    current, current[0], current[1], current[2], current[3], current[4]);
            }
            return false;
        }
    }
    for (const auto& site : sites) {
        BYTE* current = reinterpret_cast<BYTE*>(site.address);
        if (std::memcmp(current, replacement, sizeof(replacement)) == 0) continue;
        DWORD oldProtect{};
        if (!VirtualProtect(current, sizeof(replacement),
                PAGE_EXECUTE_READWRITE, &oldProtect)) return false;
        std::memcpy(current, replacement, sizeof(replacement));
        FlushInstructionCache(GetCurrentProcess(), current, sizeof(replacement));
        DWORD ignored{};
        VirtualProtect(current, sizeof(replacement), oldProtect, &ignored);
        if (std::memcmp(current, replacement, sizeof(replacement)) != 0) return false;
    }

    // The shared item-row painter also reads dword_834C18+0x14 to choose a
    // world-only price style.  Tournament lobby intentionally has no world
    // controller, so route this one read through a preserving null guard.
    constexpr std::uintptr_t modeSiteAddress = 0x004D85BB;
    constexpr BYTE modeExpected[11] = {
        0x8B, 0x15, 0x18, 0x4C, 0x83, 0x00,
        0x2B, 0xF8, 0x8B, 0x42, 0x14
    };
    BYTE modeReplacement[11] = {
        0x2B, 0xF8, 0xE8, 0, 0, 0, 0,
        0x90, 0x90, 0x90, 0x90
    };
    const auto displacement = static_cast<std::int32_t>(
        reinterpret_cast<std::uintptr_t>(&TournamentShopWorldModeGuard)
        - (modeSiteAddress + 7));
    std::memcpy(modeReplacement + 3, &displacement, sizeof(displacement));
    BYTE* modeSite = reinterpret_cast<BYTE*>(modeSiteAddress);
    if (std::memcmp(modeSite, modeExpected, sizeof(modeExpected)) != 0
            && std::memcmp(modeSite, modeReplacement, sizeof(modeReplacement)) != 0) {
        Log("Tournament shop world-mode guard skipped address=%p bytes=%02X%02X%02X%02X",
            modeSite, modeSite[0], modeSite[1], modeSite[2], modeSite[3]);
        return false;
    }
    if (std::memcmp(modeSite, modeReplacement, sizeof(modeReplacement)) != 0) {
        DWORD oldProtect{};
        if (!VirtualProtect(modeSite, sizeof(modeReplacement),
                PAGE_EXECUTE_READWRITE, &oldProtect)) return false;
        std::memcpy(modeSite, modeReplacement, sizeof(modeReplacement));
        FlushInstructionCache(GetCurrentProcess(), modeSite, sizeof(modeReplacement));
        DWORD ignored{};
        VirtualProtect(modeSite, sizeof(modeReplacement), oldProtect, &ignored);
        if (std::memcmp(modeSite, modeReplacement, sizeof(modeReplacement)) != 0)
            return false;
    }
    InterlockedExchange(&g_tournamentShopCurrencyPatchState, 1);
    Log("Tournament shop renderer guards applied currency-sites=5 world-mode-site=1 "
        "allowance=20000 disk=unchanged");
    return true;
}

#include "battle_clock_resources.inc"
#include "item_details.inc"
#include "battle_clock.inc"

bool IsVerifiedTournamentClient() {
    if (!IsVerifiedLiToClient()) return false;
    auto tournamentUi = reinterpret_cast<volatile BYTE*>(0x0062C21C);
    return *tournamentUi != 0;
}

bool SendTournamentOpcode(BYTE opcode) {
    if (!IsVerifiedTournamentClient()) return false;
    auto sendPacket = reinterpret_cast<void (__cdecl*)(const char*, int)>(0x004C6AE0);
    sendPacket(reinterpret_cast<const char*>(0x005025A8), opcode);
    return true;
}

bool OpenTournamentBuyWindow() {
    if (!IsVerifiedTournamentClient()) return false;
    void* window = *reinterpret_cast<void* volatile*>(0x0064B3F8);
    void* model = *reinterpret_cast<void* volatile*>(0x0064B3FC);
    if (!window || !model) {
        Log("Tournament buy window is unavailable window=%p model=%p", window, model);
        return false;
    }
    // S_DF also opens this object, but its socket callback runs outside the
    // legacy foreground input pass and the old four-window registry can remove
    // it again before the next frame.  Register the already-constructed native
    // Tournament buy window on the actual button-up/UI thread first; S_DF then
    // clears and fills its paired model without any synthetic shop widgets.
    auto isVisible = reinterpret_cast<bool (__thiscall*)(void*)>(0x004EBB30);
    if (!isVisible(window)) {
        auto legacyShopWindow = reinterpret_cast<void* volatile*>(0x00647E10);
        const bool guardNullLegacyWindow = *legacyShopWindow == nullptr;
        if (guardNullLegacyWindow) {
            // sub_476FA0 asks sub_4EBB30 whether dword_647E10 is visible.
            // The latter compares against four registry slots and therefore
            // mistakes nullptr for an empty/visible slot, then calls
            // sub_4EBEB0(nullptr), which closes every registered window,
            // including the Tournament buy window being opened.  A temporary
            // non-null, non-registered sentinel makes that exact check false;
            // no code dereferences it on this path.
            *legacyShopWindow = reinterpret_cast<void*>(1);
        }
        auto show = reinterpret_cast<void (__thiscall*)(void*)>(0x004EBEB0);
        show(window);
        if (guardNullLegacyWindow) *legacyShopWindow = nullptr;
    }
    return true;
}

bool ShouldBridgeTournamentCreateRoom(LPARAM mapped) {
    if (!IsVerifiedTournamentClient()) return false;
    const LONG phase = static_cast<LONG>(
        *reinterpret_cast<volatile DWORD*>(0x0064AF98));
    if (phase != 1) return false;
    const int x = GET_X_LPARAM(mapped);
    const int y = GET_Y_LPARAM(mapped);
    // Native lobby Create control.  Keep this between the Match button above
    // and LT shop button below; coordinates are already mapped to 640x480.
    return x >= 414 && x <= 454 && y >= 386 && y <= 406;
}

bool InvokeTournamentCreateRoomDialog() {
    if (!IsVerifiedTournamentClient()) return false;
    auto dialog = reinterpret_cast<void* volatile*>(0x0064A930);
    if (*dialog == nullptr) return false;
    // sub_46A730 is the client's own create/edit room dispatcher.  ECX=1
    // selects the create-room object, which preserves the native name,
    // password, capacity and C_CC serializer instead of fabricating a packet.
    auto openCreate = reinterpret_cast<void (__fastcall*)(std::uintptr_t,
                                                           std::uintptr_t)>(
        0x0046A730);
    openCreate(1, 0);
    return true;
}

constexpr bool IsTournamentShopPoint(LONG phase, int x, int y) {
    return (phase == 1 || phase == 2)
        && x >= 414 && x <= 454 && y >= 412 && y <= 432;
}

static_assert(IsTournamentShopPoint(1, 434, 422));
static_assert(IsTournamentShopPoint(2, 434, 422));
static_assert(!IsTournamentShopPoint(3, 434, 422));
static_assert(!IsTournamentShopPoint(2, 455, 422));

bool ShouldBridgeTournamentShop(LPARAM mapped) {
    if (!IsVerifiedTournamentClient()) return false;
    const LONG phase = static_cast<LONG>(
        *reinterpret_cast<volatile DWORD*>(0x0064AF98));
    // The old LTshop callback dereferences an ordinary-world scene which is
    // intentionally absent in the standalone Tournament lobby.  Its runtime
    // crash guard therefore makes this visible control a no-op; request the
    // native Tournament buy list directly and let S_DF fill/open its existing
    // lobby-owned list and window objects.
    // Phase 1 (lobby) and phase 2 (joined battle room) both expose this exact
    // LTshop control.  Do not additionally
    // require the ordinary-world scene pointers to be null: the protected
    // client may keep one of those objects allocated after a prior screen,
    // even though the visible LTshop callback remains the same guarded no-op.
    const int x = GET_X_LPARAM(mapped);
    const int y = GET_Y_LPARAM(mapped);
    return IsTournamentShopPoint(phase, x, y);
}

constexpr bool IsTournamentToolbarIconPoint(int x, int y, int left) {
    // sub_47CF00 passes x=599/settings and x=620/exit into sub_49E570.
    // sub_49E110/sub_49CD70 store those constructor arguments as the control's
    // left/top and derive right/bottom from the resource dimensions.  The
    // toolbar slots are 21 pixels apart, so keep an exact non-overlapping
    // 21x20 native hit box instead of treating the two values as centres.
    return x >= left && x < left + 21
        && y >= 460 && y < 480;
}

static_assert(IsTournamentToolbarIconPoint(599, 460, 599));
static_assert(IsTournamentToolbarIconPoint(619, 479, 599));
static_assert(!IsTournamentToolbarIconPoint(598, 460, 599));
static_assert(!IsTournamentToolbarIconPoint(620, 460, 599));
static_assert(IsTournamentToolbarIconPoint(620, 460, 620));
static_assert(IsTournamentToolbarIconPoint(640, 479, 620));
static_assert(!IsTournamentToolbarIconPoint(619, 460, 620));
static_assert(!IsTournamentToolbarIconPoint(620, 459, 620));

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

// F0 handler 4A9878 shows the result manager at 64A914. Read the native
// active-window registry as well as the phase lease, so input cannot beat Present.
bool IsBattleSummaryModal() {
    if (!IsVerifiedTournamentClient()) return false;
    const void* summary = *reinterpret_cast<void* volatile*>(0x0064A914);
    if (summary) {
        for (int i = 0; i < 4; ++i)
            if (reinterpret_cast<void* volatile*>(0x00836B54)[i] == summary) return true;
    }
    return InterlockedCompareExchange(&g_resultSummaryPending, 0, 0) != 0
        && *reinterpret_cast<volatile DWORD*>(0x0064AF98) == 2;
}

#include "summary_input.inc"
#include "battle_result_return.inc"
#include "battle_window_idle.inc"

bool ShouldBridgeTournamentToolbarIcon(LPARAM mapped, int worldLeft) {
    if (!IsVerifiedTournamentClient() || IsBattleSummaryModal()) return false;
    const LONG phase = static_cast<LONG>(
        *reinterpret_cast<volatile DWORD*>(0x0064AF98));
    if (phase < 1 || phase > 5) return false;
    if (phase == 1 || phase == 2) {
        DWORD root{};
        RECT rect{};
        return ReadToolbarBytes(0x0064AD10, &root, sizeof(root))
            && ReadTournamentToolbarRect(root, worldLeft == 620, rect)
            && TournamentToolbarRectContains(rect, mapped);
    }
    return IsTournamentToolbarIconPoint(GET_X_LPARAM(mapped), GET_Y_LPARAM(mapped), worldLeft);
}

bool InvokeTournamentSettings() {
    if (!IsVerifiedTournamentClient()) return false;
    // sub_47B8F0 is the callback installed by sub_47CF00 for the original
    // 3F2/3F1 option icon.  It selects LToptiong/LToptiong2 for the active
    // phase and toggles the client's own option window.
    if (*reinterpret_cast<void* volatile*>(0x00647E10) == nullptr
            || *reinterpret_cast<void* volatile*>(0x00647E14) == nullptr) {
        Log("Tournament option window registry is unavailable");
        return false;
    }
    auto openSettings = reinterpret_cast<void (__cdecl*)()>(0x0047B8F0);
    void* manager = *reinterpret_cast<void* volatile*>(0x00647E10);
    void* activeBefore = *reinterpret_cast<void* volatile*>(0x00836B54);
    auto isVisible = reinterpret_cast<bool (__thiscall*)(void*)>(0x004EBB30);
    const bool visibleBefore = isVisible(manager);
    openSettings();
    if (!visibleBefore && !isVisible(manager)) {
        // The standalone Tournament bootstrap constructs and selects
        // LToptiong2, but unlike the original combined launcher path its
        // callback can return without registering the manager in the four-slot
        // native UI stack.  Its vfunc+0x20 also asks whether dword_834C18 is
        // active.  The original lookup treats nullptr as matching any empty
        // slot, then calls sub_4EBBD0(nullptr) and closes every window.  Hold a
        // non-null, non-registered sentinel only across registration, exactly
        // as required by the standalone shop bridge.
        auto legacySystemWindow = reinterpret_cast<void* volatile*>(0x00834C18);
        const bool guardNullLegacyWindow = *legacySystemWindow == nullptr;
        if (guardNullLegacyWindow) *legacySystemWindow = reinterpret_cast<void*>(1);
        auto registerWindow = reinterpret_cast<void (__thiscall*)(void*)>(0x004EBBD0);
        registerWindow(manager);
        if (guardNullLegacyWindow) *legacySystemWindow = nullptr;
    }
    void* activeAfter = *reinterpret_cast<void* volatile*>(0x00836B54);
    const bool visibleAfter = isVisible(manager);
    Log("Tournament settings native callback manager=%p active-before=%p visible-before=%d active-after=%p visible-after=%d",
        manager, activeBefore, visibleBefore ? 1 : 0,
        activeAfter, visibleAfter ? 1 : 0);
    if (!visibleBefore && !visibleAfter) {
        Log("Tournament settings native UI registration failed");
        return false;
    }
    return true;
}

#include "selector_return.inc"
#include "source_login_order.inc"
#include "live_stats.inc"
#include "selector_patch.inc"
#include "class_compat.inc"
#include "world_exit.inc"
#include "character_source_return.inc"
#include "channel_click.inc"
ChannelClickTransition g_channelClickTransition;

bool OpenTournamentSessionExitDialog() {
    // Keep the original dialog selection while opening/cancelling it. Only
    // the lobby Return button chooses the native same-socket selector path;
    // Quit, Cancel and room/world return keep their original callbacks.
    auto phase = reinterpret_cast<volatile DWORD*>(0x0064AF98);
    auto returnMode = reinterpret_cast<volatile DWORD*>(0x0064AFB8);
    if (IsWorldExitPhase(*phase)) {
        // Original sub_47CF00 puts the three-button 4FA338 dialog in
        // 831568 and the two-button 4FA368 dialog in 831564. Do not change
        // returnMode: it also owns later result/source transitions.
        void* dialog = *reinterpret_cast<void**>(0x00831564);
        if (!BindWorldExitButton(dialog)) {
            Log("world exit unavailable: two-button native dialog signature mismatch");
            return false;
        }
        reinterpret_cast<void (__thiscall*)(void*)>(0x004EBEC0)(dialog);
        const bool visible = reinterpret_cast<bool (__thiscall*)(void*)>(0x004EBB30)(dialog);
        Log("world exit menu dialog=%p visible=%d return-mode=%lu", dialog,
            visible ? 1 : 0, *returnMode);
        return visible;
    }
    if (*phase != 1) return false;
    // sub_47CF00 stores the lobby dialog at 648F20. 831564 is
    // independently owned by the world and remains non-null after settlement.
    void* dialog = *reinterpret_cast<void**>(0x00648F20);
    if (!dialog) {
        if (!*reinterpret_cast<void**>(0x00834BD4)) return false;
        void* storage = reinterpret_cast<void* (__cdecl*)(std::size_t)>(0x004F118C)(0x0C);
        if (!storage) return false;
        dialog = reinterpret_cast<void* (__thiscall*)(void*)>(0x00455350)(storage);
        *reinterpret_cast<void**>(0x00648F20) = dialog;
    }
    if (!BindLobbyReturnButton(dialog)) {
        Log("lobby exit unavailable: native lobby dialog signature mismatch");
        return false;
    }
    reinterpret_cast<void (__thiscall*)(void*)>(0x004EBEC0)(dialog);
    const bool visible = reinterpret_cast<bool (__thiscall*)(void*)>(0x004EBB30)(dialog);
    Log("lobby exit menu dialog=%p visible=%d return-mode=%lu", dialog,
        visible ? 1 : 0, *returnMode);
    return visible;
}

#include "exit_dispatch.inc"

bool InvokeTournamentExitDialog() {
    if (!IsVerifiedTournamentClient() || IsBattleSummaryModal()) return false;
    const DWORD phase = *reinterpret_cast<volatile DWORD*>(0x0064AF98);
    if (phase == 2) Log("Tournament room exit uses native 46B1A0 C_D0 callback");
    return DispatchTournamentExit(phase,
        reinterpret_cast<void (__cdecl*)()>(0x0046B1A0),
        &OpenTournamentSessionExitDialog);
}

bool ShouldBridgeBattleSummaryConfirm(LPARAM mapped) {
    if (!IsVerifiedTournamentClient()) return false;
    if (!IsBattleSummaryModal()) return false;
    const LONG phase = static_cast<LONG>(
        *reinterpret_cast<volatile DWORD*>(0x0064AF98));
    if (phase != 2) return false;
    const int x = GET_X_LPARAM(mapped);
    const int y = GET_Y_LPARAM(mapped);
    // Native 640x480 WIN/LOSE summary Confirm button.  Keep the hit box
    // intentionally tight so normal phase-2 room controls are not bridged.
    return x >= 532 && x <= 588 && y >= 436 && y <= 468;
}

DWORD WINAPI TournamentBuddyClanWorker(void* parameter) {
    const LONG next = static_cast<LONG>(reinterpret_cast<std::uintptr_t>(parameter));
    auto toggle = reinterpret_cast<LiToToolbarToggleFn>(0x0046B3D0);
    toggle(static_cast<std::uintptr_t>(next), 0);
    InterlockedExchange(&g_buddyClanOpen, next);
    Log("tournament buddy/clan toolbar bridge open=%ld", next);
    InterlockedExchange(&g_toolbarWorkerActive, 0);
    return 0;
}

void ToggleTournamentBuddyClan() {
    if (InterlockedCompareExchange(&g_toolbarWorkerActive, 1, 0) != 0) return;
    const LONG opened = InterlockedCompareExchange(&g_buddyClanOpen, 0, 0);
    const LONG next = opened == 0 ? 1 : 0;
    HANDLE worker = CreateThread(nullptr, 0, &TournamentBuddyClanWorker,
                                 reinterpret_cast<void*>(static_cast<std::uintptr_t>(next)),
                                 0, nullptr);
    if (worker) {
        CloseHandle(worker);
    } else {
        InterlockedExchange(&g_toolbarWorkerActive, 0);
        Log("tournament buddy/clan toolbar worker creation failed error=%lu", GetLastError());
    }
}

LRESULT CALLBACK WindowProc(HWND hwnd, UINT message, WPARAM wParam, LPARAM lParam) {
    bool bridgeBattleSummaryConfirm = false;
    bool bridgeTournamentCreateRoom = false;
    bool bridgeTournamentShop = false;
    bool rebaseAfterWindowMove = false;
    if (message == 0x401 && LoadLoginSources()
        && static_cast<DWORD>(wParam) != *reinterpret_cast<DWORD*>(0x8343E8)) return 0;
    if (message == WM_KEYDOWN || message == WM_CHAR) EnsureCharacterSourceReturn();
    switch (message) {
#ifdef LITO_SOURCE_UI_TEST
        // Compiled only into the isolated no-SQL fixture DLL, never the release.
        case WM_APP + 0x39F:
            if(LiToSourceSession[4]>=52001 && LiToSourceSession[4]<=52008) {
                if(wParam==1) {
                    BYTE peer[0x8C]{};*reinterpret_cast<DWORD*>(peer)=37;
                    strcpy_s(reinterpret_cast<char*>(peer+4),28,"FixtureOtherPlayer");
                    return ProfileLocationRoom(peer);
                }
                return ProfileLocationRoom(reinterpret_cast<const BYTE*>(0x64ABF4));
            }
            return -1;
        case WM_APP + 0x39E:
            if(LiToSourceSession[4]>=52001 && LiToSourceSession[4]<=52008)
                Log("fixture illusion actual cast invoked=%d",VerifyIllusionActualCast());
            return 0;
        case WM_APP + 0x39D:
            if(LiToSourceSession[4]>=52001 && LiToSourceSession[4]<=52008) VerifySelfNameHover();
            return 0;
        case WM_APP + 0x39C:
            if(LiToSourceSession[4]>=52001 && LiToSourceSession[4]<=52008)
                Log("fixture dragon combo all passed=%d",VerifyDragonCombo());
            return 0;
        case WM_APP + 0x39B:
            if(LiToSourceSession[4]>=52001 && LiToSourceSession[4]<=52008)
                Log("fixture dragon native status all passed=%d",VerifyDragonSkinStatus());
            return 0;
        case WM_APP + 0x39A:
            if(LiToSourceSession[4]>=52001 && LiToSourceSession[4]<=52008)
                Log("fixture dragon target picker all passed=%d",VerifyDragonTargetPicker());
            return 0;
        case WM_APP + 0x396: {
            Log("fixture render request port=%u phase=%u",LiToSourceSession[4],*reinterpret_cast<DWORD*>(0x64AF98));
            if(LiToSourceSession[4]<52001 || LiToSourceSession[4]>52008
                || *reinterpret_cast<DWORD*>(0x64AF98)!=1) return 0;
            bool ok=true;
            for(int type=0;type<7;++type) for(int sex=0;sex<2;++sex)
                ok=DrawSevenClassLobbyPortrait(type,sex) && ok;
            ok=!DrawSevenClassLobbyPortrait(-1,0) && !DrawSevenClassLobbyPortrait(7,0)
                && !DrawSevenClassLobbyPortrait(5,2) && ok;
            Log("fixture lobby all 14 class portraits rendered=%d invalid-inputs-safe=1",ok);
            return 0;
        }
        case WM_APP + 0x399: {
            if(LiToSourceSession[4]<52001 || LiToSourceSession[4]>52008) return 0;
            auto list=*reinterpret_cast<BYTE**>(0x834888);int count=0;
            if(list) {
                auto slots=*reinterpret_cast<BYTE***>(list+0x58);
                int size=*reinterpret_cast<int*>(list+0x2C);
                for(int i=0;i<size;++i) if(slots[i] && *reinterpret_cast<int*>(slots[i]+4)>=0) {
                    reinterpret_cast<void (__thiscall*)(void*,int,int)>(0x4C8F90)(slots[i],50+(i%4)*50,60+(i/4)*30);
                    ++count;
                }
            }
            Log("fixture skill native icons rendered=%d",count);return 0;
        }
        case WM_APP + 0x397:
            if(LiToSourceSession[4]>=52001 && LiToSourceSession[4]<=52008
                && *reinterpret_cast<DWORD*>(0x64AF98)==1 && OpenTournamentSessionExitDialog())
                ReturnLobbyToCharacters(nullptr,nullptr);
            return 0;
        case WM_APP + 0x398:
            if(LiToSourceSession[4]>=52001 && LiToSourceSession[4]<=52008)
                ReturnChannelToCharacters(nullptr,nullptr);
            return 0;
        case WM_APP + 0x394: {
            if (LiToSourceSession[4] < 52001 || LiToSourceSession[4] > 52008) return 0;
            const DWORD root=*reinterpret_cast<DWORD*>(0x75F6E8);
            if(root && *reinterpret_cast<DWORD*>(root+12)==7)
                reinterpret_cast<void (__cdecl*)()>(0x4928C0)();
            return 0;
        }
        case WM_APP + 0x395:
            if(LiToSourceSession[4]>=52001 && LiToSourceSession[4]<=52008)
                reinterpret_cast<void (__cdecl*)(const char*,...)>(0x4C6AE0)("cc",0xCA,0);
            return 0;
        case WM_APP + 0x393: {
            EnsureCharacterSourceReturn();
            const DWORD root = *reinterpret_cast<DWORD*>(0x75F6E8);
            if (root && *reinterpret_cast<DWORD*>(root + 12) == 7 && LiToSourceSession[4] >= 52001 && LiToSourceSession[4] <= 52008) {
                auto slots = *reinterpret_cast<DWORD**>(root);
                reinterpret_cast<void (__cdecl*)()>(*reinterpret_cast<DWORD*>(slots[6] + 0x24))();
            }
            return 0;
        }
        case WM_APP + 0x392:
            if (g_loginSourceCount >= 2 && g_loginSources[1].port == 52002
                && *reinterpret_cast<DWORD*>(0x75FF7C))
                reinterpret_cast<void (__fastcall*)(int)>(0x4999B0)(1);
            return 0;
        case WM_APP + 0x390: {
            const DWORD root = *reinterpret_cast<DWORD*>(0x75F6E8);
            if (LiToSourceSession[4] < 52001 || LiToSourceSession[4] > 52008 || !root) return 0;
            if (*reinterpret_cast<DWORD*>(root + 12) == 0 && *reinterpret_cast<DWORD*>(root + 4) == 5) {
                auto slots = *reinterpret_cast<DWORD**>(root);
                reinterpret_cast<void (__thiscall*)(void*, const char*)>(0x440E90)(reinterpret_cast<void*>(slots[0]), "qatest");
                reinterpret_cast<void (__thiscall*)(void*, const char*)>(0x440E90)(reinterpret_cast<void*>(slots[1]), "testpass");
                reinterpret_cast<void (__cdecl*)()>(0x492270)();
            }
            return 0;
        }
        case WM_APP + 0x391: {
            EnsureCharacterSourceReturn();
            const DWORD root = *reinterpret_cast<DWORD*>(0x75F6E8);
            if (root && *reinterpret_cast<DWORD*>(root + 12) == 7 && LiToSourceSession[4] >= 52001 && LiToSourceSession[4] <= 52008) {
                auto slots = *reinterpret_cast<DWORD**>(root);
                Log("fixture server button verified=%d button=%08lx vtable=%08lx resource=%08lx callback=%08lx", IsVerifiedTournamentClient(), slots[4], *reinterpret_cast<DWORD*>(slots[4]), *reinterpret_cast<DWORD*>(slots[4]+0x20), *reinterpret_cast<DWORD*>(slots[4]+0x24));
                reinterpret_cast<void (__cdecl*)()>(*reinterpret_cast<DWORD*>(slots[4] + 0x24))();
            }
            return 0;
        }
#endif
        case kSourceLogoutMessage:
            LogoutToNativeSources();
            return 0;
        case kMaskEditSurfacesMessage:
            HideTournamentEditHelperSurfaces();
            return 0;
        case kConfirmedCloseMessage:
            // Used only by the standalone, non-LiTo smoke-test window.
            return DefWindowProcW(hwnd, WM_CLOSE, 0, 0);
        case WM_CLOSE:
            // sub_46B1A0 is Tournament's *stage-return* command, not the
            // whole-application close path: phases 2/3 send C_D0 and phase 0
            // only dismisses the current Tournament UI.  Routing the caption X
            // through it therefore leaves LiTo running at the outer stage.
            // The original Lineage WndProc owns process shutdown in every
            // phase; its lParam=1 sentinel performs the native socket/window
            // cleanup.  Native room-return buttons and Alt+X continue to reach
            // sub_46B1A0 through LiTo's own input dispatcher.
            if (IsVerifiedLiToClient() && g_originalWndProc) {
                Log("forwarding close to native LiTo WndProc hwnd=%p lParam=1", hwnd);
                return CallWindowProcW(g_originalWndProc, hwnd, message, wParam, 1);
            }
            return DefWindowProcW(hwnd, message, wParam, lParam);
        case WM_NCHITTEST:
            // The game replaces its window procedure during screen changes.
            // Use the standard non-client hit test for the frame we installed,
            // so the visible caption remains HTCAPTION in every lobby state.
            return DefWindowProcW(hwnd, message, wParam, lParam);
        case WM_NCLBUTTONDOWN:
            // The protected client consumes the normal caption message in some
            // lobby states.  Let Windows run its native move loop directly so
            // the compatibility window remains draggable from the title bar.
            if (wParam == HTCAPTION) {
                ReleaseCapture();
                return DefWindowProcW(hwnd, message, wParam, lParam);
            }
            break;
        case WM_ACTIVATEAPP:
            if (IsVerifiedLiToClient() && wParam == FALSE) {
                // LiTo's 2003 main loop stops advancing after its native
                // inactive notification.  Windowed DirectDraw already keeps
                // cooperative ownership safe, so keep only the native logic
                // state active while Windows still deactivates this HWND.
                if (InterlockedExchange(&g_backgroundFocusVirtualized, 1) == 0)
                    Log("virtualized native WM_ACTIVATEAPP(FALSE) for background rendering");
                return DefWindowProcW(hwnd, message, wParam, lParam);
            }
            if (wParam != FALSE)
                InterlockedExchange(&g_backgroundFocusVirtualized, 0);
            break;
        case WM_ACTIVATE:
            if (IsVerifiedLiToClient() && LOWORD(wParam) == WA_INACTIVE) {
                if (InterlockedExchange(&g_backgroundFocusVirtualized, 1) == 0)
                    Log("virtualized native WM_ACTIVATE(WA_INACTIVE) for background rendering");
                return DefWindowProcW(hwnd, message, wParam, lParam);
            }
            if (LOWORD(wParam) != WA_INACTIVE)
                InterlockedExchange(&g_backgroundFocusVirtualized, 0);
            break;
        case WM_KILLFOCUS:
            if (IsVerifiedLiToClient()) {
                if (InterlockedExchange(&g_backgroundFocusVirtualized, 1) == 0)
                    Log("virtualized native WM_KILLFOCUS for background rendering");
                return DefWindowProcW(hwnd, message, wParam, lParam);
            }
            break;
        case WM_ENTERSIZEMOVE:
            Log("window move begin hwnd=%p", hwnd);
            break;
        case WM_EXITSIZEMOVE: {
            RECT moved{};
            GetWindowRect(hwnd, &moved);
            Log("window move end hwnd=%p position=(%ld,%ld)", hwnd,
                moved.left, moved.top);
            break;
        }
        case WM_MOUSEWHEEL:
            if (ScrollLiToItemDetail(wParam)) return 1;
            break;
        case WM_MOUSEMOVE: case WM_LBUTTONDOWN: case WM_LBUTTONUP:
        case WM_RBUTTONDOWN: case WM_RBUTTONUP: case WM_MBUTTONDOWN: case WM_MBUTTONUP:
        case WM_LBUTTONDBLCLK: case WM_RBUTTONDBLCLK: case WM_MBUTTONDBLCLK:
            lParam = MapMouse(lParam);
            EnsureCharacterSourceReturn();
            // Results are modal: stale room/lobby hit rectangles must never
            // receive input beside Confirm (including the original WndProc).
            if (BlockSummaryBackgroundClick(IsBattleSummaryModal(), message,
                    GET_X_LPARAM(lParam), GET_Y_LPARAM(lParam))) return 1;
            if (RouteCanvasEditMouse(message, wParam, lParam)) return 1;
            if (IsVerifiedTournamentClient()) {
                const DWORD phase = *reinterpret_cast<volatile DWORD*>(0x0064AF98);
                if (g_channelClickTransition.consume(message, lParam,
                        GetTickCount(), GetDoubleClickTime(), IsNativeChannelPage(), phase)) {
                    Log("consumed trailing channel double-click message=%04X phase=%lu", message, phase);
                    return 1;
                }
                // Also bind the confirmation when the original native toolbar
                // or keyboard handler opens it without our mouse bridge.
                if (IsWorldExitPhase(phase))
                    BindWorldExitButton(*reinterpret_cast<void**>(0x00831564));
            }
            if ((message == WM_LBUTTONDOWN || message == WM_LBUTTONUP)
                    && ShouldBridgeTournamentToolbarIcon(lParam, 599)) {
                if (message == WM_LBUTTONUP) {
                    if (InvokeTournamentSettings()) {
                        Log("opened native Tournament settings through toolbar bridge");
                    } else {
                        Log("Tournament settings toolbar bridge failed");
                    }
                }
                return 1;
            }
            if ((message == WM_LBUTTONDOWN || message == WM_LBUTTONUP)
                    && ShouldBridgeTournamentToolbarIcon(lParam, 620)) {
                if (message == WM_LBUTTONUP) {
                    if (InvokeTournamentExitDialog()) {
                        Log("dispatched native Tournament exit action through toolbar bridge");
                    } else {
                        Log("Tournament exit toolbar bridge failed");
                    }
                }
                return 1;
            }
            if ((message == WM_LBUTTONDOWN || message == WM_LBUTTONUP)
                    && ShouldBridgeTournamentBuddyClan(lParam)) {
                if (message == WM_LBUTTONUP) ToggleTournamentBuddyClan();
                return 1;
            }
            if (message == WM_LBUTTONUP && ShouldBridgeBattleSummaryConfirm(lParam)) {
                // Let the original WndProc close the summary first.  C_D0 is
                // emitted immediately afterwards on this same UI thread.
                bridgeBattleSummaryConfirm = true;
            }
            if (message == WM_LBUTTONUP && ShouldBridgeTournamentCreateRoom(lParam)) {
                // Give the original callback the first opportunity.  Some
                // client states open the dialog correctly; the compatibility
                // fallback below only re-opens the same native object.
                bridgeTournamentCreateRoom = true;
            }
            if (message == WM_LBUTTONUP && ShouldBridgeTournamentShop(lParam)) {
                bridgeTournamentShop = true;
            }
            // Start/Cancel deliberately falls through to LiTo's original
            // WndProc.  The protected client owns the D2 payload shape and
            // outbound cipher state.  Injecting a raw Winsock frame here
            // desynchronizes that stream and disconnects at the room screen.
            break;
        case WM_ERASEBKGND:
            return 1;
        case WM_WINDOWPOSCHANGING: {
            auto* position = reinterpret_cast<WINDOWPOS*>(lParam);
            if (position && !(position->flags & SWP_NOSIZE)) {
                RECT desired{0, 0,
                    MulDiv(static_cast<int>(g_virtualWidth), g_scalePercent, 100),
                    MulDiv(static_cast<int>(g_virtualHeight), g_scalePercent, 100)};
                AdjustWindowRectEx(&desired, kWindowedStyle, FALSE, kWindowedExStyle);
                position->cx = desired.right - desired.left;
                position->cy = desired.bottom - desired.top;
            }
            break;
        }
        case WM_WINDOWPOSCHANGED: {
            const auto* position = reinterpret_cast<const WINDOWPOS*>(lParam);
            rebaseAfterWindowMove = position && !(position->flags & SWP_NOMOVE);
            break;
        }
        case WM_STYLECHANGING:
            if (wParam == GWL_STYLE) {
                auto* styles = reinterpret_cast<STYLESTRUCT*>(lParam);
                if (styles) styles->styleNew = kWindowedStyle
                    | (styles->styleNew & (WS_VISIBLE | WS_DISABLED));
            } else if (wParam == GWL_EXSTYLE) {
                auto* styles = reinterpret_cast<STYLESTRUCT*>(lParam);
                if (styles) styles->styleNew = kWindowedExStyle;
            }
            break;
        case WM_TIMER:
            if (wParam == 0x381) {
                if (GetTickCount64() <= g_forceVisibleUntil) {
                    if (!IsWindowVisible(hwnd)) {
                        ShowWindowAsync(hwnd, SW_SHOWNORMAL);
                        if (InterlockedCompareExchange(&g_visibilityRestoreLogged, 1, 0) == 0)
                            Log("restored hidden game window hwnd=%p", hwnd);
                    }
                } else {
                    KillTimer(hwnd, 0x381);
                }
            }
            break;
    }
    LRESULT result = CallWindowProcW(g_originalWndProc, hwnd, message, wParam, lParam);
    if (IsVerifiedTournamentClient()
            && (message == WM_LBUTTONDOWN || message == WM_LBUTTONUP)) {
        const LONG phase = static_cast<LONG>(
            *reinterpret_cast<volatile DWORD*>(0x0064AF98));
        if (phase >= 1 && phase <= 4) HideTournamentEditHelperSurfaces();
    }
    if (rebaseAfterWindowMove) QueueWindowMoveRebase(hwnd);
    if (bridgeTournamentCreateRoom) {
        auto createDialog = reinterpret_cast<void* volatile*>(0x0064A930);
        bool visible = false;
        if (*createDialog != nullptr) {
            auto isVisible = reinterpret_cast<bool (__thiscall*)(void*)>(0x004EBB30);
            visible = isVisible(*createDialog);
        }
        if (!visible && InvokeTournamentCreateRoomDialog()) {
            Log("opened native Tournament create-room dialog through lobby bridge");
        }
    }
    if (bridgeTournamentShop) {
        if (OpenTournamentBuyWindow() && SendTournamentOpcode(0xDA)) {
            Log("requested native Tournament buy list through lobby shop bridge");
        } else {
            Log("lobby shop bridge could not open the native buy window or send C_LITO_BUY_LIST");
        }
    }
    if (bridgeBattleSummaryConfirm) {
        if (SendTournamentOpcode(0xD0)) {
            InterlockedExchange(&g_resultSummaryPending, 0);
            Log("battle summary confirmed; sent native C_LITO_EXIT_ROOM");
        } else {
            Log("battle summary confirmation bridge could not send C_LITO_EXIT_ROOM");
        }
    }
    // Surface hooks present DirectDraw frames when their contents change.
    // Re-presenting the last primary surface for WM_PAINT overwrites LiTo's
    // separate lobby/UI drawing path with a stale loading frame (or black).
    if (message == WM_SIZE) {
        for (void* primary : g_primaries) if (primary) { QueuePrimaryPresent(primary); break; }
    }
    return result;
}

DWORD WINAPI VisibilityWorker(void* parameter) {
    HWND hwnd = static_cast<HWND>(parameter);
    CaptionDragTracker drag;
    POINT dragOffset{};
    POINT previousCursor{};
    DWORD lastStyleCheck{};
    while (IsWindow(hwnd)) {
        const ULONGLONG now = GetTickCount64();
        if (now <= g_forceVisibleUntil && !IsWindowVisible(hwnd)) {
            ShowWindowAsync(hwnd, SW_SHOWNORMAL);
            if (InterlockedCompareExchange(&g_visibilityRestoreLogged, 1, 0) == 0)
                Log("restored hidden game window hwnd=%p", hwnd);
        }

        // Only the real foreground/root window under a fresh press can own
        // this fallback. Geometry alone also matches covered game windows.
        POINT cursor{};
        const bool cursorKnown = GetCursorPos(&cursor) != FALSE;
        const bool leftDown = (GetAsyncKeyState(VK_LBUTTON) & 0x8000) != 0;
        const bool foreground = GetAncestor(GetForegroundWindow(), GA_ROOT) == hwnd;
        const HWND hit = cursorKnown ? WindowFromPoint(cursor) : nullptr;
        const bool ownsHit = hit && GetAncestor(hit, GA_ROOT) == hwnd;
        RECT windowRect{};
        POINT clientOrigin{};
        bool captionHit = false;
        if (ownsHit && GetWindowRect(hwnd, &windowRect)
                && ClientToScreen(hwnd, &clientOrigin)) {
            const int buttonWidth = std::max(1, GetSystemMetrics(SM_CXSIZE));
            captionHit = cursor.x >= windowRect.left + buttonWidth
                && cursor.x < windowRect.right - buttonWidth * 3
                && cursor.y >= windowRect.top && cursor.y < clientOrigin.y;
        }
        const auto action = drag.update(leftDown, foreground && cursorKnown, captionHit);
        if (action == CaptionDragTracker::Action::Begin) {
            dragOffset = {cursor.x - windowRect.left, cursor.y - windowRect.top};
            previousCursor = cursor;
            Log("compat caption drag begin hwnd=%p position=(%ld,%ld)",
                hwnd, windowRect.left, windowRect.top);
        } else if (action == CaptionDragTracker::Action::Move) {
            if (cursor.x != previousCursor.x || cursor.y != previousCursor.y) {
                SetWindowPos(hwnd, HWND_NOTOPMOST,
                    cursor.x - dragOffset.x, cursor.y - dragOffset.y, 0, 0,
                    SWP_NOSIZE | SWP_NOACTIVATE | SWP_ASYNCWINDOWPOS);
                previousCursor = cursor;
            }
        } else if (action == CaptionDragTracker::Action::End) {
            RECT moved{};
            GetWindowRect(hwnd, &moved);
            Log("compat caption drag end hwnd=%p position=(%ld,%ld)",
                hwnd, moved.left, moved.top);
        }

        // Mirror the stable style contract from the existing TW client
        // compatibility layer.  LiTo reapplies its 2003-era popup style at
        // several native screen changes, so restore only when it actually
        // diverges instead of continuously resizing the window.
        const DWORD tick = GetTickCount();
        if (tick - lastStyleCheck >= 500) {
            lastStyleCheck = tick;
            // LiTo swaps the main window procedure while changing native
            // login/lobby/Tournament screens.  Re-subclass that same HWND and
            // retain the newly installed LiTo procedure as the forwarding
            // target; otherwise the caption X and coordinate mapping silently
            // disappear after a screen transition.
            const auto currentWndProc = reinterpret_cast<WNDPROC>(
                GetWindowLongPtrW(hwnd, GWLP_WNDPROC));
            if (currentWndProc && currentWndProc != &WindowProc) {
                SetLastError(0);
                const LONG_PTR replaced = SetWindowLongPtrW(
                    hwnd, GWLP_WNDPROC, reinterpret_cast<LONG_PTR>(&WindowProc));
                if (replaced || GetLastError() == 0) {
                    InterlockedExchangePointer(
                        reinterpret_cast<PVOID volatile*>(&g_originalWndProc),
                        reinterpret_cast<PVOID>(replaced));
                    Log("restored LiTo WndProc subclass hwnd=%p native=%p",
                        hwnd, reinterpret_cast<void*>(replaced));
                }
            }
            const DWORD currentStyle = static_cast<DWORD>(GetWindowLongPtrW(hwnd, GWL_STYLE));
            const DWORD currentExStyle = static_cast<DWORD>(GetWindowLongPtrW(hwnd, GWL_EXSTYLE));
            const DWORD requiredStyle = kWindowedStyle | (currentStyle & (WS_VISIBLE | WS_DISABLED));
            if ((currentStyle & (kWindowedStyle | WS_POPUP | WS_THICKFRAME | WS_MAXIMIZEBOX))
                    != kWindowedStyle
                || (currentExStyle & (WS_EX_APPWINDOW | WS_EX_WINDOWEDGE | WS_EX_TOPMOST))
                    != kWindowedExStyle) {
                SetWindowLongPtrW(hwnd, GWL_STYLE, requiredStyle);
                SetWindowLongPtrW(hwnd, GWL_EXSTYLE, kWindowedExStyle);
                SetWindowPos(hwnd, HWND_NOTOPMOST, 0, 0, 0, 0,
                    SWP_NOMOVE | SWP_NOSIZE | SWP_NOACTIVATE | SWP_FRAMECHANGED | SWP_ASYNCWINDOWPOS);
                Log("restored LiTo window style hwnd=%p old=%08lX ex=%08lX",
                    hwnd, currentStyle, currentExStyle);
            }
        }
        // LiTo paints its page-7 controls through a GDI pass that follows the
        // DirectDraw present.  Keep the launcher's completed-page footer state
        // after that pass too; the helper is a no-op outside mode 4/page 7.
        HideCompletedCharacterLoadingFooter(hwnd);
        // Visibility recovery must not repaint.  LiTo switches from the
        // DirectDraw loading surface to a separately rendered lobby; forcing
        // the old primary over it every 250 ms hides the live UI.
        Sleep(drag.active ? 8 : 16);
    }
    return 0;
}

void ConfigureWindow(HWND hwnd) {
    if (!hwnd || !IsWindow(hwnd)) return;
    const bool firstConfiguration = g_configuredWindow != hwnd;
    g_window = hwnd;
    SetWindowLongPtrW(hwnd, GWL_STYLE, kWindowedStyle);
    SetWindowLongPtrW(hwnd, GWL_EXSTYLE, kWindowedExStyle);
    RECT desired{0, 0,
        MulDiv(static_cast<int>(g_virtualWidth), g_scalePercent, 100),
        MulDiv(static_cast<int>(g_virtualHeight), g_scalePercent, 100)};
    AdjustWindowRectEx(&desired, kWindowedStyle, FALSE, kWindowedExStyle);
    int width = desired.right - desired.left;
    int height = desired.bottom - desired.top;
    if (firstConfiguration) {
        int x = std::max(0, (GetSystemMetrics(SM_CXSCREEN) - width) / 2);
        int y = std::max(0, (GetSystemMetrics(SM_CYSCREEN) - height) / 2);
        SetWindowPos(hwnd, HWND_NOTOPMOST, x, y, width, height,
                     SWP_FRAMECHANGED | SWP_SHOWWINDOW);
        // ConfigureWindow runs on LiTo's own UI thread during the user-initiated
        // launch.  Give the legacy intro/login state the activation it expects;
        // otherwise it can remain on native modes 8/9 while the launcher keeps
        // keyboard focus.  The window is never made topmost.
        ShowWindow(hwnd, SW_SHOWNORMAL);
        const BOOL foreground = SetForegroundWindow(hwnd);
        SetActiveWindow(hwnd);
        SetFocus(hwnd);
        Log("activated LiTo window hwnd=%p foreground=%d", hwnd, foreground);
        g_configuredWindow = hwnd;
    } else {
        // SetDisplayMode can be called again after the user has moved the game.
        // Resize the client area if needed without snapping it back to centre.
        SetWindowPos(hwnd, HWND_NOTOPMOST, 0, 0, width, height,
                     SWP_NOMOVE | SWP_FRAMECHANGED | SWP_SHOWWINDOW);
    }
    if (!g_originalWndProc) {
        SetLastError(0);
        LONG_PTR old = SetWindowLongPtrW(hwnd, GWLP_WNDPROC, reinterpret_cast<LONG_PTR>(&WindowProc));
        if (old || GetLastError() == 0) g_originalWndProc = reinterpret_cast<WNDPROC>(old);
    }
    g_forceVisibleUntil = GetTickCount64() + 30000;
    SetTimer(hwnd, 0x381, 250, nullptr);
    if (InterlockedCompareExchange(&g_visibilityWorkerStarted, 1, 0) == 0) {
        HANDLE worker = CreateThread(nullptr, 0, &VisibilityWorker, hwnd, 0, nullptr);
        if (worker) CloseHandle(worker);
    }
    Log("window configured hwnd=%p client=%lux%lu scale=%d", hwnd,
        g_virtualWidth, g_virtualHeight, g_scalePercent);
}

HRESULT WINAPI HookQueryInterface(void* self, REFIID iid, void** out);
HRESULT WINAPI HookCreateSurface(void* self, void* desc, void** out, IUnknown* outer);
HRESULT WINAPI HookSetCooperativeLevel(void* self, HWND hwnd, DWORD flags);
HRESULT WINAPI HookSetDisplayMode3(void* self, DWORD width, DWORD height, DWORD bpp);
HRESULT WINAPI HookSetDisplayMode5(void* self, DWORD width, DWORD height, DWORD bpp,
                                   DWORD refresh, DWORD flags);

void PatchDirectDraw(void* object, bool extended) {
    if (!object) return;
    EnterCriticalSection(&g_lock);
    if (FindObject(object)) { LeaveCriticalSection(&g_lock); return; }
    void** vtable = *static_cast<void***>(object);
    DdVtableHook* hook = FindDdHookByVtable(vtable);
    if (!hook) {
        for (auto& candidate : g_ddHooks) if (!candidate.vtable) { hook = &candidate; break; }
        if (!hook) { LeaveCriticalSection(&g_lock); return; }
        hook->vtable = vtable;
        hook->queryInterface = reinterpret_cast<QueryInterfaceFn>(vtable[0]);
        hook->createSurface = reinterpret_cast<CreateSurfaceFn>(vtable[6]);
        hook->setCooperativeLevel = reinterpret_cast<SetCooperativeLevelFn>(vtable[20]);
        hook->extended = extended;
        if (extended) hook->setDisplayMode5 = reinterpret_cast<SetDisplayMode5Fn>(vtable[21]);
        else hook->setDisplayMode3 = reinterpret_cast<SetDisplayMode3Fn>(vtable[21]);
        DWORD old{};
        if (!VirtualProtect(vtable, 23 * sizeof(void*), PAGE_EXECUTE_READWRITE, &old)) {
            *hook = {};
            LeaveCriticalSection(&g_lock);
            return;
        }
        vtable[0] = reinterpret_cast<void*>(&HookQueryInterface);
        vtable[6] = reinterpret_cast<void*>(&HookCreateSurface);
        vtable[20] = reinterpret_cast<void*>(&HookSetCooperativeLevel);
        vtable[21] = extended ? reinterpret_cast<void*>(&HookSetDisplayMode5)
                              : reinterpret_cast<void*>(&HookSetDisplayMode3);
        DWORD ignored{};
        VirtualProtect(vtable, 23 * sizeof(void*), old, &ignored);
    }
    for (auto& record : g_objects) {
        if (!record.object) { record = {object, hook}; break; }
    }
    LeaveCriticalSection(&g_lock);
    Log("DirectDraw object patched object=%p vtable=%p extended=%d", object, vtable, extended);
}

HRESULT WINAPI HookQueryInterface(void* self, REFIID iid, void** out) {
    EnterCriticalSection(&g_lock);
    ObjectRecord* record = FindObject(self);
    QueryInterfaceFn original = record ? record->hook->queryInterface : nullptr;
    LeaveCriticalSection(&g_lock);
    if (!original) return E_NOINTERFACE;
    HRESULT hr = original(self, iid, out);
    if (SUCCEEDED(hr) && out && *out) {
        bool extended = IsEqualGUID(iid, IID_IDirectDraw2) ||
                        IsEqualGUID(iid, IID_IDirectDraw4) ||
                        IsEqualGUID(iid, IID_IDirectDraw7);
        PatchDirectDraw(*out, extended);
    }
    return hr;
}

HRESULT WINAPI HookSetCooperativeLevel(void* self, HWND hwnd, DWORD flags) {
    EnterCriticalSection(&g_lock);
    ObjectRecord* record = FindObject(self);
    SetCooperativeLevelFn original = record ? record->hook->setCooperativeLevel : nullptr;
    LeaveCriticalSection(&g_lock);
    if (!original) return DDERR_INVALIDOBJECT;
    ConfigureWindow(hwnd);
    DWORD effective = (flags & ~(DDSCL_EXCLUSIVE | DDSCL_FULLSCREEN | DDSCL_ALLOWMODEX))
        | DDSCL_NORMAL | DDSCL_MULTITHREADED;
    HRESULT hr = original(self, hwnd, effective);
    Log("SetCooperativeLevel requested=%08lX effective=%08lX hr=%08lX", flags, effective, hr);
    return hr;
}

HRESULT WINAPI HookSetDisplayMode3(void*, DWORD width, DWORD height, DWORD bpp) {
    if (width && height) { g_virtualWidth = width; g_virtualHeight = height; }
    if (g_window) ConfigureWindow(g_window);
    Log("SetDisplayMode3 virtualized %lux%lux%lu", width, height, bpp);
    return DD_OK;
}

HRESULT WINAPI HookSetDisplayMode5(void*, DWORD width, DWORD height, DWORD bpp,
                                   DWORD refresh, DWORD flags) {
    if (width && height) { g_virtualWidth = width; g_virtualHeight = height; }
    if (g_window) ConfigureWindow(g_window);
    Log("SetDisplayMode5 virtualized %lux%lux%lu refresh=%lu flags=%08lX",
        width, height, bpp, refresh, flags);
    return DD_OK;
}

HRESULT WINAPI HookSurfaceBlt(void* self, RECT* dst, void* src, RECT* srcRect,
                              DWORD flags, DDBLTFX* fx);
HRESULT WINAPI HookSurfaceBltFast(void* self, DWORD x, DWORD y, void* src,
                                  RECT* srcRect, DWORD flags);
HRESULT WINAPI HookSurfaceFlip(void* self, void* target, DWORD flags);
HRESULT WINAPI HookSurfaceLock(void* self, RECT* rect, DDSURFACEDESC* desc,
                               DWORD flags, HANDLE eventHandle);
HRESULT WINAPI HookSurfaceUnlock(void* self, RECT* rect);
HRESULT WINAPI HookSurfaceReleaseDC(void* self, HDC dc);
ULONG WINAPI HookSurfaceRelease(void* self);

void PatchSurface(void* surface) {
    if (!surface) return;
    void** vtable = *static_cast<void***>(surface);
    EnterCriticalSection(&g_lock);
    if (FindSurfaceHook(surface)) { LeaveCriticalSection(&g_lock); return; }
    SurfaceVtableHook* hook{};
    for (auto& candidate : g_surfaceHooks) if (!candidate.vtable) { hook = &candidate; break; }
    if (!hook) { LeaveCriticalSection(&g_lock); return; }
    hook->vtable = vtable;
    hook->addRef = reinterpret_cast<SurfaceAddRefFn>(vtable[1]);
    hook->release = reinterpret_cast<SurfaceReleaseFn>(vtable[2]);
    hook->blt = reinterpret_cast<SurfaceBltFn>(vtable[5]);
    hook->bltFast = reinterpret_cast<SurfaceBltFastFn>(vtable[7]);
    hook->flip = reinterpret_cast<SurfaceFlipFn>(vtable[11]);
    hook->getDC = reinterpret_cast<SurfaceGetDCFn>(vtable[17]);
    hook->getSurfaceDesc = reinterpret_cast<SurfaceGetDescFn>(vtable[22]);
    hook->lock = reinterpret_cast<SurfaceLockFn>(vtable[25]);
    hook->releaseDC = reinterpret_cast<SurfaceReleaseDCFn>(vtable[26]);
    hook->unlock = reinterpret_cast<SurfaceUnlockFn>(vtable[32]);
    DWORD old{};
    if (VirtualProtect(vtable, 33 * sizeof(void*), PAGE_EXECUTE_READWRITE, &old)) {
        vtable[2] = reinterpret_cast<void*>(&HookSurfaceRelease);
        vtable[5] = reinterpret_cast<void*>(&HookSurfaceBlt);
        vtable[7] = reinterpret_cast<void*>(&HookSurfaceBltFast);
        vtable[11] = reinterpret_cast<void*>(&HookSurfaceFlip);
        vtable[25] = reinterpret_cast<void*>(&HookSurfaceLock);
        vtable[26] = reinterpret_cast<void*>(&HookSurfaceReleaseDC);
        vtable[32] = reinterpret_cast<void*>(&HookSurfaceUnlock);
        DWORD ignored{};
        VirtualProtect(vtable, 33 * sizeof(void*), old, &ignored);
    } else {
        *hook = {};
    }
    LeaveCriticalSection(&g_lock);
}

HRESULT WINAPI HookCreateSurface(void* self, void* rawDesc, void** out, IUnknown* outer) {
    EnterCriticalSection(&g_lock);
    ObjectRecord* record = FindObject(self);
    CreateSurfaceFn original = record ? record->hook->createSurface : nullptr;
    LeaveCriticalSection(&g_lock);
    if (!original || !rawDesc) return DDERR_INVALIDPARAMS;
    auto* incoming = static_cast<DDSURFACEDESC2*>(rawDesc);
    bool primary = (incoming->ddsCaps.dwCaps & DDSCAPS_PRIMARYSURFACE) != 0;
    bool softwareWorkSurface =
        (incoming->ddsCaps.dwCaps & DDSCAPS_OFFSCREENPLAIN) != 0
        && (incoming->ddsCaps.dwCaps & DDSCAPS_SYSTEMMEMORY) != 0;
    DDSURFACEDESC2 copy{};
    void* effective = rawDesc;
    if (primary || softwareWorkSurface) {
        memcpy(&copy, rawDesc, std::min<size_t>(incoming->dwSize, sizeof(copy)));
        copy.dwSize = incoming->dwSize;
        // In exclusive 16-bit mode these work surfaces inherited RGB565 from
        // the display.  Windowed mode leaves the desktop at 32-bit, while the
        // LiTo software renderer still copies two bytes per pixel.  Explicitly
        // preserve RGB565 or every pair of game pixels becomes one corrupted
        // 32-bit desktop pixel (half-width/green lobby output).
        copy.dwFlags |= DDSD_PIXELFORMAT;
        copy.ddpfPixelFormat = {};
        copy.ddpfPixelFormat.dwSize = sizeof(DDPIXELFORMAT);
        copy.ddpfPixelFormat.dwFlags = DDPF_RGB;
        copy.ddpfPixelFormat.dwRGBBitCount = 16;
        copy.ddpfPixelFormat.dwRBitMask = 0xF800;
        copy.ddpfPixelFormat.dwGBitMask = 0x07E0;
        copy.ddpfPixelFormat.dwBBitMask = 0x001F;
        effective = &copy;
    }
    if (primary) {
        copy.dwFlags |= DDSD_CAPS | DDSD_WIDTH | DDSD_HEIGHT;
        copy.dwFlags &= ~DDSD_BACKBUFFERCOUNT;
        DWORD surfaceWidth = std::max<DWORD>(g_virtualWidth,
            GetSystemMetrics(SM_CXSCREEN) + g_virtualWidth);
        DWORD surfaceHeight = std::max<DWORD>(g_virtualHeight,
            GetSystemMetrics(SM_CYSCREEN) + g_virtualHeight);
        if (g_window) {
            RECT client{};
            POINT origin{};
            if (GetClientRect(g_window, &client) && ClientToScreen(g_window, &origin)) {
                surfaceWidth = std::max<DWORD>(surfaceWidth,
                    std::max<LONG>(0, origin.x) + client.right);
                surfaceHeight = std::max<DWORD>(surfaceHeight,
                    std::max<LONG>(0, origin.y) + client.bottom);
            }
        }
        copy.dwWidth = surfaceWidth;
        copy.dwHeight = surfaceHeight;
        copy.dwBackBufferCount = 0;
        copy.ddsCaps.dwCaps &= ~(DDSCAPS_PRIMARYSURFACE | DDSCAPS_FLIP
                              | DDSCAPS_COMPLEX | DDSCAPS_BACKBUFFER);
        copy.ddsCaps.dwCaps |= DDSCAPS_OFFSCREENPLAIN | DDSCAPS_SYSTEMMEMORY;
        Log("virtual primary allocation=%lux%lu desktop-coordinate surface",
            copy.dwWidth, copy.dwHeight);
    }
    HRESULT hr = original(self, effective, out, outer);
    if (SUCCEEDED(hr) && out && *out && primary) {
        // LiTo 3.81 renders its emulated primary surface in desktop
        // coordinates captured when that surface is created.  Moving the
        // window does not relocate those pixels.  Present() used to fall back
        // to the window's *current* client origin on every frame, so moving a
        // static GDI-composed screen such as character selection cropped a
        // different (mostly black) part of the primary and visibly offset the
        // picture.  Pin the source rectangle for the lifetime of this primary;
        // a later source-backed full-frame blit may still replace it through
        // HookSurfaceBlt, and a newly created primary gets a fresh origin.
        POINT origin{};
        if (g_window && IsWindow(g_window) && ClientToScreen(g_window, &origin)) {
            EnterCriticalSection(&g_lock);
            g_primaryContentRect = {
                origin.x,
                origin.y,
                origin.x + static_cast<LONG>(g_virtualWidth),
                origin.y + static_cast<LONG>(g_virtualHeight)
            };
            LeaveCriticalSection(&g_lock);
            Log("pinned virtual primary content origin=(%ld,%ld)",
                origin.x, origin.y);
        }
        RememberPrimary(*out);
        PatchSurface(*out);
    }
    Log("CreateSurface primary=%d size=%lu flags=%08lX width=%lu height=%lu bpp=%lu caps=%08lX result=%08lX surface=%p",
        primary, incoming->dwSize, incoming->dwFlags, incoming->dwWidth, incoming->dwHeight,
        incoming->ddpfPixelFormat.dwRGBBitCount, incoming->ddsCaps.dwCaps, hr, out ? *out : nullptr);
    return hr;
}

HRESULT WINAPI HookSurfaceBlt(void* self, RECT* dst, void* src, RECT* srcRect,
                              DWORD flags, DDBLTFX* fx) {
    SurfaceVtableHook* hook = FindSurfaceHook(self);
    if (!hook || !hook->blt) return DDERR_INVALIDOBJECT;
    // A null source is commonly a DDBLT_COLORFILL used to clear the part of the
    // desktop-coordinate primary surface that lies behind the window.  LiTo's
    // clear rectangle can extend from the client origin to the bottom-right of
    // the desktop; treating it as the game viewport makes the real 640x480
    // frame appear as a tiny image in the top-left corner.  Only a source-backed
    // blit identifies a presented content rectangle.  Direct primary locks keep
    // using the client-origin fallback in Present().
    if (IsPrimary(self) && dst && src
        && dst->right > dst->left && dst->bottom > dst->top) {
        EnterCriticalSection(&g_lock);
        g_primaryContentRect = *dst;
        LeaveCriticalSection(&g_lock);
    }
    HRESULT hr = hook->blt(self, dst, src, srcRect, flags, fx);
    LONG count = InterlockedIncrement(&g_bltCount);
    if (count <= 200 || (src && count % 600 == 0) || (!src && count % 6000 == 0)) Log("primary Blt count=%ld self=%p primary=%d src=%p dst=(%ld,%ld)-(%ld,%ld) flags=%08lX hr=%08lX",
        count, self, IsPrimary(self), src,
        dst ? dst->left : 0, dst ? dst->top : 0,
        dst ? dst->right : 0, dst ? dst->bottom : 0,
        flags, hr);
    // Null-source color fills are intermediate clears and may occur thousands
    // of times per second.  Publishing them both flickers and floods the old
    // client's render path.  Source-backed blits identify a completed frame;
    // direct primary writes are still covered by Unlock/ReleaseDC/Flip.
    if (SUCCEEDED(hr) && src && IsPrimary(self)) QueuePrimaryPresent(self);
    return hr;
}

HRESULT WINAPI HookSurfaceBltFast(void* self, DWORD x, DWORD y, void* src,
                                  RECT* srcRect, DWORD flags) {
    SurfaceVtableHook* hook = FindSurfaceHook(self);
    if (!hook || !hook->bltFast) return DDERR_INVALIDOBJECT;
    HRESULT hr = hook->bltFast(self, x, y, src, srcRect, flags);
    LONG count = InterlockedIncrement(&g_bltFastCount);
    if (count <= 20) Log("primary BltFast count=%ld src=%p hr=%08lX", count, src, hr);
    if (SUCCEEDED(hr)) QueuePrimaryPresent(self);
    return hr;
}

HRESULT WINAPI HookSurfaceFlip(void* self, void*, DWORD) {
    if (!IsPrimary(self)) return DDERR_NOTFLIPPABLE;
    LONG count = InterlockedIncrement(&g_flipCount);
    if (count <= 20) Log("primary Flip count=%ld", count);
    QueuePrimaryPresent(self);
    return DD_OK;
}

HRESULT WINAPI HookSurfaceLock(void* self, RECT* rect, DDSURFACEDESC* desc,
                               DWORD flags, HANDLE eventHandle) {
    SurfaceVtableHook* hook = FindSurfaceHook(self);
    if (!hook || !hook->lock) return DDERR_INVALIDOBJECT;
    HRESULT hr = hook->lock(self, rect, desc, flags, eventHandle);
    LONG count = InterlockedIncrement(&g_lockCount);
    if (count <= 40) {
        Log("surface Lock count=%ld self=%p primary=%d rect=(%ld,%ld)-(%ld,%ld) flags=%08lX hr=%08lX pitch=%ld",
            count, self, IsPrimary(self),
            rect ? rect->left : 0, rect ? rect->top : 0,
            rect ? rect->right : 0, rect ? rect->bottom : 0,
            flags, hr, (SUCCEEDED(hr) && desc) ? desc->lPitch : 0);
    }
    return hr;
}

HRESULT WINAPI HookSurfaceUnlock(void* self, RECT* rect) {
    SurfaceVtableHook* hook = FindSurfaceHook(self);
    if (!hook || !hook->unlock) return DDERR_INVALIDOBJECT;
    HRESULT hr = hook->unlock(self, rect);
    LONG count = InterlockedIncrement(&g_unlockCount);
    if (count <= 40) Log("surface Unlock count=%ld self=%p primary=%d hr=%08lX",
                         count, self, IsPrimary(self), hr);
    if (SUCCEEDED(hr) && IsPrimary(self)) QueuePrimaryPresent(self);
    return hr;
}

HRESULT WINAPI HookSurfaceReleaseDC(void* self, HDC dc) {
    SurfaceVtableHook* hook = FindSurfaceHook(self);
    if (!hook || !hook->releaseDC) return DDERR_INVALIDOBJECT;
    HRESULT hr = hook->releaseDC(self, dc);
    LONG count = InterlockedIncrement(&g_releaseDcCount);
    if (count <= 20) Log("primary ReleaseDC count=%ld hr=%08lX", count, hr);
    if (SUCCEEDED(hr)) QueuePrimaryPresent(self);
    return hr;
}

ULONG WINAPI HookSurfaceRelease(void* self) {
    EnterCriticalSection(&g_lock);
    SurfaceVtableHook* hook = FindSurfaceHook(self);
    if (!hook || !hook->release) {
        LeaveCriticalSection(&g_lock);
        return 0;
    }
    ULONG remaining = hook->release(self);
    if (remaining == 0) {
        for (void*& slot : g_primaries) if (slot == self) slot = nullptr;
    }
    LeaveCriticalSection(&g_lock);
    return remaining;
}

} // namespace

extern "C" HRESULT WINAPI DirectDrawCreate(GUID* guid, LPDIRECTDRAW* out, IUnknown* outer) {
    if (!EnsureInitialized()) return E_FAIL;
    auto fn = SystemProc<decltype(&DirectDrawCreate)>("DirectDrawCreate");
    HRESULT hr = fn ? fn(guid, out, outer) : E_FAIL;
    if (SUCCEEDED(hr) && out && *out) PatchDirectDraw(*out, false);
    return hr;
}

extern "C" HRESULT WINAPI DirectDrawCreateEx(GUID* guid, void** out, REFIID iid, IUnknown* outer) {
    if (!EnsureInitialized()) return E_FAIL;
    using Fn = HRESULT (WINAPI*)(GUID*, void**, REFIID, IUnknown*);
    auto fn = SystemProc<Fn>("DirectDrawCreateEx");
    HRESULT hr = fn ? fn(guid, out, iid, outer) : E_FAIL;
    if (SUCCEEDED(hr) && out && *out) PatchDirectDraw(*out, true);
    return hr;
}

extern "C" HRESULT WINAPI DirectDrawCreateClipper(DWORD flags, LPDIRECTDRAWCLIPPER* out, IUnknown* outer) {
    if (!EnsureInitialized()) return E_FAIL;
    using Fn = HRESULT (WINAPI*)(DWORD, LPDIRECTDRAWCLIPPER*, IUnknown*);
    auto fn = SystemProc<Fn>("DirectDrawCreateClipper");
    return fn ? fn(flags, out, outer) : E_FAIL;
}

extern "C" HRESULT WINAPI DirectDrawEnumerateA(LPDDENUMCALLBACKA callback, LPVOID context) {
    if (!EnsureInitialized()) return E_FAIL;
    auto fn = SystemProc<decltype(&DirectDrawEnumerateA)>("DirectDrawEnumerateA");
    return fn ? fn(callback, context) : E_FAIL;
}

extern "C" HRESULT WINAPI DirectDrawEnumerateW(LPDDENUMCALLBACKW callback, LPVOID context) {
    if (!EnsureInitialized()) return E_FAIL;
    auto fn = SystemProc<decltype(&DirectDrawEnumerateW)>("DirectDrawEnumerateW");
    return fn ? fn(callback, context) : E_FAIL;
}

extern "C" HRESULT WINAPI DirectDrawEnumerateExA(LPDDENUMCALLBACKEXA callback, LPVOID context, DWORD flags) {
    if (!EnsureInitialized()) return E_FAIL;
    auto fn = SystemProc<decltype(&DirectDrawEnumerateExA)>("DirectDrawEnumerateExA");
    return fn ? fn(callback, context, flags) : E_FAIL;
}

extern "C" HRESULT WINAPI DirectDrawEnumerateExW(LPDDENUMCALLBACKEXW callback, LPVOID context, DWORD flags) {
    if (!EnsureInitialized()) return E_FAIL;
    auto fn = SystemProc<decltype(&DirectDrawEnumerateExW)>("DirectDrawEnumerateExW");
    return fn ? fn(callback, context, flags) : E_FAIL;
}

extern "C" HRESULT WINAPI DllCanUnloadNow() {
    if (!EnsureInitialized()) return S_FALSE;
    using Fn = HRESULT (WINAPI*)();
    auto fn = SystemProc<Fn>("DllCanUnloadNow");
    return fn ? fn() : S_FALSE;
}

extern "C" HRESULT WINAPI DllGetClassObject(REFCLSID clsid, REFIID iid, LPVOID* out) {
    if (!EnsureInitialized()) return CLASS_E_CLASSNOTAVAILABLE;
    using Fn = HRESULT (WINAPI*)(REFCLSID, REFIID, LPVOID*);
    auto fn = SystemProc<Fn>("DllGetClassObject");
    return fn ? fn(clsid, iid, out) : CLASS_E_CLASSNOTAVAILABLE;
}

BOOL WINAPI DllMain(HINSTANCE instance, DWORD reason, LPVOID) {
    if (reason == DLL_PROCESS_ATTACH) {
        g_self = instance;
        DisableThreadLibraryCalls(instance);
    }
    return TRUE;
}
