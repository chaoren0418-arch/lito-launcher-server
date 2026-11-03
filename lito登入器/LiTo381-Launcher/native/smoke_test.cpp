#define WIN32_LEAN_AND_MEAN
#define DIRECTDRAW_VERSION 0x0700
#include <windows.h>
#include <ddraw.h>
#include <cstdio>

int main() {
    HMODULE proxy = LoadLibraryW(L"publish\\ddraw.dll");
    if (!proxy) { std::printf("LoadLibrary failed %lu\n", GetLastError()); return 1; }
    using CreateFn = HRESULT (WINAPI*)(GUID*, LPDIRECTDRAW*, IUnknown*);
    auto create = reinterpret_cast<CreateFn>(GetProcAddress(proxy, "DirectDrawCreate"));
    if (!create) return 2;
    LPDIRECTDRAW draw{};
    HRESULT hr = create(nullptr, &draw, nullptr);
    std::printf("DirectDrawCreate=%08lX object=%p\n", hr, draw);
    if (FAILED(hr) || !draw) return 3;

    HWND window = CreateWindowExW(0, L"STATIC", L"LiTo381 compat smoke",
        WS_OVERLAPPEDWINDOW, 100, 100, 640, 480, nullptr, nullptr,
        GetModuleHandleW(nullptr), nullptr);
    hr = draw->SetCooperativeLevel(window, DDSCL_EXCLUSIVE | DDSCL_FULLSCREEN);
    std::printf("SetCooperativeLevel=%08lX\n", hr);
    if (FAILED(hr)) return 4;
    hr = draw->SetDisplayMode(640, 480, 16);
    std::printf("SetDisplayMode=%08lX\n", hr);
    if (FAILED(hr)) return 5;
    RECT configured{};
    GetWindowRect(window, &configured);
    const LPARAM captionPoint = MAKELPARAM(
        configured.left + (configured.right - configured.left) / 2,
        configured.top + 10);
    const LRESULT captionHit = SendMessageW(
        window, WM_NCHITTEST, 0, captionPoint);
    std::printf("CaptionHitTest=%lld expected=%d\n",
                static_cast<long long>(captionHit), HTCAPTION);
    if (captionHit != HTCAPTION) return 6;
    SetWindowPos(window, nullptr, 37, 53, 0, 0,
                 SWP_NOSIZE | SWP_NOZORDER | SWP_NOACTIVATE);
    RECT moved{};
    GetWindowRect(window, &moved);
    hr = draw->SetDisplayMode(640, 480, 16);
    RECT afterModeReset{};
    GetWindowRect(window, &afterModeReset);
    std::printf("PositionAfterModeReset=(%ld,%ld) expected=(%ld,%ld)\n",
                afterModeReset.left, afterModeReset.top, moved.left, moved.top);
    if (FAILED(hr) || afterModeReset.left != moved.left || afterModeReset.top != moved.top) return 7;
    DDSURFACEDESC desc{};
    desc.dwSize = sizeof(desc);
    desc.dwFlags = DDSD_CAPS;
    desc.ddsCaps.dwCaps = DDSCAPS_PRIMARYSURFACE;
    LPDIRECTDRAWSURFACE surface{};
    hr = draw->CreateSurface(&desc, &surface, nullptr);
    std::printf("CreateSurface=%08lX surface=%p\n", hr, surface);
    if (FAILED(hr) || !surface) return 8;

    DDSURFACEDESC sourceDesc{};
    sourceDesc.dwSize = sizeof(sourceDesc);
    sourceDesc.dwFlags = DDSD_CAPS | DDSD_WIDTH | DDSD_HEIGHT | DDSD_PIXELFORMAT;
    sourceDesc.dwWidth = 640;
    sourceDesc.dwHeight = 480;
    sourceDesc.ddsCaps.dwCaps = DDSCAPS_OFFSCREENPLAIN | DDSCAPS_SYSTEMMEMORY;
    sourceDesc.ddpfPixelFormat.dwSize = sizeof(DDPIXELFORMAT);
    sourceDesc.ddpfPixelFormat.dwFlags = DDPF_RGB;
    sourceDesc.ddpfPixelFormat.dwRGBBitCount = 16;
    sourceDesc.ddpfPixelFormat.dwRBitMask = 0xF800;
    sourceDesc.ddpfPixelFormat.dwGBitMask = 0x07E0;
    sourceDesc.ddpfPixelFormat.dwBBitMask = 0x001F;
    LPDIRECTDRAWSURFACE source{};
    hr = draw->CreateSurface(&sourceDesc, &source, nullptr);
    if (FAILED(hr) || !source) return 9;
    DDBLTFX fill{};
    fill.dwSize = sizeof(fill);
    fill.dwFillColor = 0xF800;
    hr = source->Blt(nullptr, nullptr, nullptr, DDBLT_COLORFILL | DDBLT_WAIT, &fill);
    if (FAILED(hr)) return 10;
    POINT clientOrigin{};
    ClientToScreen(window, &clientOrigin);
    RECT frameRect{clientOrigin.x, clientOrigin.y, clientOrigin.x + 640, clientOrigin.y + 480};
    hr = surface->Blt(&frameRect, source, nullptr, DDBLT_WAIT, nullptr);
    if (FAILED(hr)) return 11;

    // Intentionally do not pump the window message queue.  The legacy
    // WM_TIMER-based presenter stalls here; the background-safe worker must
    // publish the frame independently.
    Sleep(120);
    HDC windowDc = GetDC(window);
    COLORREF sampled = windowDc ? GetPixel(windowDc, 320, 240) : CLR_INVALID;
    if (windowDc) ReleaseDC(window, windowDc);
    const bool backgroundPresent = sampled != CLR_INVALID
        && GetRValue(sampled) >= 200 && GetGValue(sampled) <= 80 && GetBValue(sampled) <= 80;
    std::printf("BackgroundPresentPixel=%08lX red=%d\n", sampled, backgroundPresent);
    // This host is not the verified fixed-base LiTo image, so exercise the
    // proxy's smoke-only close message without depending on LiTo's WndProc.
    SendMessageW(window, WM_APP + 0x381, 0, 0);
    const bool closeWorked = !IsWindow(window);
    std::printf("WindowClose=%d expected=1\n", closeWorked);
    source->Release();
    surface->Release();
    draw->Release();
    if (IsWindow(window)) DestroyWindow(window);
    FreeLibrary(proxy);
    return backgroundPresent && closeWorked ? 0 : 12;
}
