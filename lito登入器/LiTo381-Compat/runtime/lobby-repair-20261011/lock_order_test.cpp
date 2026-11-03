#define NOMINMAX
#include <windows.h>
#include <cstdio>
#ifdef BASELINE
#include "ddraw_proxy.before.cpp"
#else
#include "../../ddraw_proxy.cpp"
#endif

CRITICAL_SECTION testDriverLock;
HANDLE attempted;
extern "C" void WINAPI AcquireDDThreadLock() {
    SetEvent(attempted);
    EnterCriticalSection(&testDriverLock);
}
extern "C" void WINAPI ReleaseDDThreadLock() { LeaveCriticalSection(&testDriverLock); }
ULONG WINAPI MockAddRef(void*) { AcquireDDThreadLock(); ReleaseDDThreadLock(); return 2; }
ULONG WINAPI MockRelease(void*) { AcquireDDThreadLock(); ReleaseDDThreadLock(); return 1; }
DWORD WINAPI AcquireOnWorker(void*) {
    void* p = AcquirePrimaryReference();
    if (p) ReleaseSurfaceReference(p);
    return p ? 0 : 1;
}
int main() {
    InitializeCriticalSection(&testDriverLock);
    InitializeCriticalSection(&g_lock);
    InitializeCriticalSection(&g_presentLock);
    attempted = CreateEvent(nullptr, TRUE, FALSE, nullptr);
    g_systemDdraw = GetModuleHandle(nullptr);
    void* table[3]{};
    void** fake = table;
    g_surfaceHooks[0].vtable = table;
    g_surfaceHooks[0].addRef = MockAddRef;
    g_surfaceHooks[0].release = MockRelease;
    g_primaries[0] = &fake;
    EnterCriticalSection(&testDriverLock);
    HANDLE worker = CreateThread(nullptr, 0, AcquireOnWorker, nullptr, 0, nullptr);
    if (WaitForSingleObject(attempted, 3000) != WAIT_OBJECT_0) return 3;
    // Emulate the UI's driver callback while the background reference waits.
    const bool callbackCanProgress = TryEnterCriticalSection(&g_lock) != FALSE;
    if (callbackCanProgress) LeaveCriticalSection(&g_lock);
    LeaveCriticalSection(&testDriverLock);
    if (WaitForSingleObject(worker, 3000) != WAIT_OBJECT_0) return 4;
    DWORD code{}; GetExitCodeThread(worker, &code);
    CloseHandle(worker); CloseHandle(attempted);
    std::printf("driver callback progresses=%d worker=%lu\n", callbackCanProgress, code);
    return callbackCanProgress && code == 0 ? 0 : 1;
}
