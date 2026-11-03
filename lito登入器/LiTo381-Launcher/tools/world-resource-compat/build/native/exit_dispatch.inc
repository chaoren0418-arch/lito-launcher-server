// The Tournament room icon (404/405) is bound to sub_46B1A0.
// In phase 2 that native callback sends C_D0 through the client's cipher.
// sub_4786F0 belongs to the session/world dialog and must not handle this icon.
bool DispatchTournamentExit(DWORD phase, void (__cdecl* leaveRoom)(),
        bool (*openSessionDialog)()) {
    if (phase == 2) {
        leaveRoom();
        return true;
    }
    return openSessionDialog();
}
