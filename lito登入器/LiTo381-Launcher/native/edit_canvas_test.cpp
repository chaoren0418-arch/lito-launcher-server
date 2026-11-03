#define WIN32_LEAN_AND_MEAN
#define NOMINMAX
#include <windows.h>
#include <cstdint>
#include <cstring>
#include <cstdio>
#include <cstdlib>
#include <algorithm>

#define CHECK(c) do { if (!(c)) { std::printf("FAIL line=%d error=%lu\n", __LINE__, GetLastError()); std::exit(1); } } while (0)
struct SurfaceVtableHook {
    HRESULT (WINAPI *getDC)(void*, HDC*);
    HRESULT (WINAPI *releaseDC)(void*, HDC);
};
HWND g_window{};
CRITICAL_SECTION g_presentLock;
int g_scalePercent=200;
bool verified=true;
bool IsVerifiedTournamentClient() { return verified; }
bool IsVerifiedLiToClient() { return false; }
void Log(const char*,...) {}
HDC primaryDc{};
int acquired{},released{};
HRESULT WINAPI acquire(void*,HDC* dc) { *dc=primaryDc; ++acquired; return S_OK; }
HRESULT WINAPI release(void*,HDC) { ++released; return S_OK; }
SurfaceVtableHook hooks{acquire,release};
SurfaceVtableHook* FindSurfaceHook(void*) { return &hooks; }
alignas(16) BYTE testNativeMemory[0x10000]{};
#define EDIT_CANVAS_NATIVE_ADDRESS(address) (reinterpret_cast<std::uintptr_t>(testNativeMemory)+(address)-0x640000)
#include "edit_canvas.inc"

int focusLost{}, clickX{-1}, clickY{-1}, typedDigit{};
LRESULT CALLBACK ParentProc(HWND hwnd,UINT msg,WPARAM wp,LPARAM lp) {
    if(msg==kMaskEditSurfacesMessage) { HideTournamentEditHelperSurfaces();return 0; }
    return DefWindowProcW(hwnd,msg,wp,lp);
}
DWORD WINAPI MaskFromRenderThread(void* child) {
    EnterCriticalSection(&g_presentLock);
    HideTournamentEditHelperSurfaces();
    CHECK(!MaskEditDesktopSurface(static_cast<HWND>(child)));
    LeaveCriticalSection(&g_presentLock);
    return 0;
}
LRESULT CALLBACK EditProc(HWND hwnd,UINT msg,WPARAM wp,LPARAM lp) {
    if (msg==WM_KILLFOCUS) ++focusLost;
    if (msg==WM_LBUTTONDOWN) { clickX=LOWORD(lp);clickY=HIWORD(lp);return 0; }
    if (msg==WM_CHAR) { typedDigit=static_cast<int>(wp);return 0; }
    if (msg==WM_PAINT) {
        PAINTSTRUCT ps{};
        HDC dc=EditBeginPaint(hwnd,&ps);
        CHECK(dc && ps.rcPaint.right==276 && ps.rcPaint.bottom==12);
        // Exercise clipping: this may never overwrite outside the native edit.
        PatBlt(dc,-20,-20,400,80,WHITENESS);
        SetBkMode(dc,TRANSPARENT);
        SetTextColor(dc,RGB(0,0,0));
        TextOutW(dc,2,0,L"Ab中文",4);
        EditEndPaint(hwnd,&ps);
        return 0;
    }
    return DefWindowProcW(hwnd,msg,wp,lp);
}

int main() {
    BYTE before[] = {0x51,0x90,0xE8,1,2,3,4};
    BYTE after[] = {0x51,0xE8,1,2,3,4,0x90};
    CHECK(FindEditCall(before+1, "\x51", 1)==before+2);
    CHECK(FindEditCall(after+1, "\x51", 1)==after+1);
    CHECK(!FindEditCall(after+1, "\x52", 1));
    after[6]=0xCC; CHECK(!FindEditCall(after+1, "\x51", 1));
    before[2]=0xE9; CHECK(!FindEditCall(before+1, "\x51", 1));
    SetProcessDPIAware();
    InitializeCriticalSection(&g_presentLock);
    *reinterpret_cast<DWORD*>(EDIT_CANVAS_NATIVE_ADDRESS(0x64af98))=1;
    *reinterpret_cast<BYTE*>(EDIT_CANVAS_NATIVE_ADDRESS(0x6476c8))=1;
    *reinterpret_cast<void**>(EDIT_CANVAS_NATIVE_ADDRESS(0x6476d8))=reinterpret_cast<void*>(1);
    WNDCLASSW wc{};wc.lpfnWndProc=ParentProc;wc.hInstance=GetModuleHandleW(nullptr);wc.lpszClassName=L"CanvasFixture";
    CHECK(RegisterClassW(&wc));
    wc.lpfnWndProc=EditProc;wc.lpszClassName=L"LUnicodeEdit";
    CHECK(RegisterClassW(&wc));
    g_window=CreateWindowW(L"CanvasFixture",L"Offscreen test fixture",WS_OVERLAPPEDWINDOW,100,100,1300,1000,nullptr,nullptr,wc.hInstance,nullptr);
    CHECK(g_window);
    HWND child=CreateWindowW(L"LUnicodeEdit",L"",WS_CHILD,192,347,276,12,g_window,nullptr,wc.hInstance,nullptr);
    CHECK(child);
    // Simulate a UI thread waiting for a rendering worker without pumping
    // messages. The old cross-thread SetWindowRgn cannot complete here.
    for (int phase : {1,2,3,4,5}) {
        *reinterpret_cast<DWORD*>(EDIT_CANVAS_NATIVE_ADDRESS(0x64af98))=phase;
        CHECK(SetWindowRgn(child,nullptr,FALSE));RemovePropW(child,L"LiTo381.CanvasClip");
        HANDLE worker=CreateThread(nullptr,0,MaskFromRenderThread,child,0,nullptr);CHECK(worker);
        CHECK(WaitForSingleObject(worker,2000)==WAIT_OBJECT_0);CloseHandle(worker);
        CHECK(!GetPropW(child,L"LiTo381.CanvasClip"));
        MSG queued{};int count=0;
        while(PeekMessageW(&queued,g_window,kMaskEditSurfacesMessage,kMaskEditSurfacesMessage,PM_REMOVE)) {
            DispatchMessageW(&queued);++count;
        }
        CHECK(count==2 && GetPropW(child,L"LiTo381.CanvasClip"));
        HRGN region=CreateRectRgn(0,0,1,1);CHECK(GetWindowRgn(child,region)==NULLREGION);DeleteObject(region);
    }
    *reinterpret_cast<DWORD*>(EDIT_CANVAS_NATIVE_ADDRESS(0x64af98))=1;
    HDC screen=GetDC(nullptr);
    primaryDc=CreateCompatibleDC(screen);
    struct Info { BITMAPINFOHEADER header;DWORD masks[3]; } info{};
    info.header.biSize=sizeof(BITMAPINFOHEADER);info.header.biWidth=2048;info.header.biHeight=-1600;
    info.header.biPlanes=1;info.header.biBitCount=16;info.header.biCompression=BI_BITFIELDS;
    info.masks[0]=0xf800;info.masks[1]=0x7e0;info.masks[2]=0x1f;
    void* bits{};
    HBITMAP bitmap=CreateDIBSection(screen,reinterpret_cast<BITMAPINFO*>(&info),DIB_RGB_COLORS,&bits,nullptr,0);
    CHECK(bitmap);auto old=SelectObject(primaryDc,bitmap);
    ReleaseDC(nullptr,screen);
    ShowWindow(child,SW_SHOW);SetFocus(child);CHECK(GetFocus()==child);
    int beforeHide=focusLost;
    ShowWindow(child,SW_HIDE);
    CHECK(GetFocus()!=child && focusLost>beforeHide);
    for (int phase : {1,2,3,4,5}) {
        *reinterpret_cast<DWORD*>(EDIT_CANVAS_NATIVE_ADDRESS(0x64af98))=phase;
        PatBlt(primaryDc,0,0,2048,1600,BLACKNESS);
        EditShowWindow(child,SW_SHOW);
        CHECK(GetWindowLongW(child,GWL_STYLE)&WS_VISIBLE);
        HRGN clipped=CreateRectRgn(0,0,1,1);
        CHECK(GetWindowRgn(child,clipped)==NULLREGION);DeleteObject(clipped);
        SetFocus(child);CHECK(GetFocus()==child);
        EditShowWindow(child,SW_SHOW);CHECK(GetFocus()==child);
        EditUpdateWindow(child);
        CHECK(GetPixel(primaryDc,192,347)==RGB(255,255,255));
        CHECK(GetPixel(primaryDc,467,358)==RGB(255,255,255));
        CHECK(GetPixel(primaryDc,191,347)==0 && GetPixel(primaryDc,468,358)==0);
        CHECK(GetPixel(primaryDc,192,346)==0 && GetPixel(primaryDc,192,359)==0);
        POINT origin{};CHECK(GetViewportOrgEx(primaryDc,&origin) && origin.x==0 && origin.y==0);
        HDC ime=EditGetDC(child);CHECK(ime==primaryDc);
        SetPixel(ime,5,5,RGB(255,0,0));CHECK(EditReleaseDC(child,ime)==1);
        CHECK(GetPixel(primaryDc,197,352)==RGB(255,0,0));
        CHECK(acquired==released);
    }
    // Native windowed origin: preserved even when the main window moves.
    *reinterpret_cast<BYTE*>(EDIT_CANVAS_NATIVE_ADDRESS(0x6476c8))=0;
    for(int x : {100,350}) {
        CHECK(SetWindowPos(g_window,nullptr,x,80,0,0,SWP_NOSIZE|SWP_NOACTIVATE));
        PatBlt(primaryDc,0,0,2048,1600,BLACKNESS);
        POINT origin{};CHECK(ClientToScreen(g_window,&origin));
        EditUpdateWindow(child);
        CHECK(GetPixel(primaryDc,origin.x+192,origin.y+347)==RGB(255,255,255));
        CHECK(GetPixel(primaryDc,192,347)==0);
    }
    // A completed canvas goes through the same StretchBlt path at 1x/1.5x/2x.
    *reinterpret_cast<BYTE*>(EDIT_CANVAS_NATIVE_ADDRESS(0x6476c8))=1;
    PatBlt(primaryDc,0,0,2048,1600,BLACKNESS);EditUpdateWindow(child);
    HDC output=CreateCompatibleDC(primaryDc);HBITMAP enlarged=CreateCompatibleBitmap(primaryDc,1280,960);auto previous=SelectObject(output,enlarged);
    for(int scale : {100,150,200}) {
        SetStretchBltMode(output,COLORONCOLOR);
        CHECK(StretchBlt(output,0,0,640*scale/100,480*scale/100,primaryDc,0,0,640,480,SRCCOPY));
        CHECK(GetPixel(output,192*scale/100,347*scale/100)==RGB(255,255,255));
        CHECK(GetPixel(output,192*scale/100-1,347*scale/100)==0);
    }
    for (int scale : {100,150,200}) {
        int deviceX=200*scale/100,deviceY=350*scale/100;
        SetFocus(g_window);
        CHECK(RouteCanvasEditMouse(WM_LBUTTONDOWN,MK_LBUTTON,
            MAKELPARAM(deviceX*100/scale,deviceY*100/scale)));
        CHECK(GetFocus()==child && clickX==8 && clickY==3);
        SendMessageW(GetFocus(),WM_CHAR,L'7',0);CHECK(typedDigit==L'7');
        CHECK(RouteCanvasEditMouse(WM_LBUTTONUP,0,MAKELPARAM(200,350)));
        CHECK(!RouteCanvasEditMouse(WM_LBUTTONDOWN,0,MAKELPARAM(190,350)));
    }
    EnableWindow(child,FALSE);
    CHECK(!RouteCanvasEditMouse(WM_LBUTTONDOWN,0,MAKELPARAM(200,350)));
    EnableWindow(child,TRUE);ShowWindow(child,SW_HIDE);
    CHECK(!RouteCanvasEditMouse(WM_LBUTTONDOWN,0,MAKELPARAM(200,350)));
    ShowWindow(child,SW_SHOW);
    verified=false;CHECK(!IsCanvasEdit(child));verified=true;
    *reinterpret_cast<DWORD*>(EDIT_CANVAS_NATIVE_ADDRESS(0x64af98))=0;CHECK(!IsCanvasEdit(child));
    *reinterpret_cast<DWORD*>(EDIT_CANVAS_NATIVE_ADDRESS(0x64af98))=1;
    CHECK(!IsCanvasEdit(g_window));
    CHECK(acquired==released);
    for(auto& slot:g_editDcs) CHECK(!slot.dc);
    SelectObject(output,previous);DeleteObject(enlarged);DeleteDC(output);
    SelectObject(primaryDc,old);DeleteObject(bitmap);DeleteDC(primaryDc);
    DestroyWindow(g_window);DeleteCriticalSection(&g_presentLock);
    std::puts("PASS: nonblocking render-thread masking in 5 phases; owner-thread region update; real 16-bit GDI canvas; retained focus; IME DC; clipping; moved origin; 100/150/200% output; balanced DCs; phase/host guards.");
}
