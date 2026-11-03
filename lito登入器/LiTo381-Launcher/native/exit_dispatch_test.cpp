#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <cstdio>
#include <cstdlib>
#include "exit_dispatch.inc"
#define CHECK(c) do { if (!(c)) { std::printf("FAIL line=%d\n", __LINE__); std::exit(1); } } while (0)
int roomCalls{}, dialogCalls{};
DWORD returnMode{};
bool dialogAvailable{};
void __cdecl LeaveRoom() { ++roomCalls; }
bool OpenDialog() { ++dialogCalls; returnMode = 2; return dialogAvailable; }
int main() {
    // A missing session dialog must not block room departure or normalize
    // its return mode. Each click calls the room callback exactly once.
    for (DWORD mode = 0; mode <= 2; ++mode) {
        roomCalls = dialogCalls = 0; returnMode = mode; dialogAvailable = false;
        CHECK(DispatchTournamentExit(2, LeaveRoom, OpenDialog));
        CHECK(roomCalls == 1 && dialogCalls == 0 && returnMode == mode);
    }
    // Lobby and world retain the existing dialog result, including failure;
    // their clicks must never send a room-departure request.
    for (DWORD phase = 1; phase <= 5; ++phase) {
        if (phase == 2) continue;
        for (int available = 0; available <= 1; ++available) {
            roomCalls = dialogCalls = 0; dialogAvailable = available != 0;
            CHECK(DispatchTournamentExit(phase, LeaveRoom, OpenDialog) == dialogAvailable);
            CHECK(roomCalls == 0 && dialogCalls == 1);
        }
    }
    std::puts("PASS: room exit bypasses session dialog and preserves return mode; lobby/world dialog success/failure preserved.");
}
