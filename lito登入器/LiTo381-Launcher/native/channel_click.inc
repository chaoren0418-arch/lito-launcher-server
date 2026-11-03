struct ChannelClickTransition {
    bool armed{};
    bool suppressRelease{};
    DWORD releasedAt{};
    LPARAM point{};

    bool consume(UINT message, LPARAM mapped, DWORD now, DWORD doubleClickMs,
            bool onChannelPage, DWORD phase) {
        if (message == WM_LBUTTONUP && suppressRelease) {
            suppressRelease = false;
            return true;
        }
        if (message == WM_LBUTTONDOWN) {
            armed = onChannelPage;
            suppressRelease = false;
            releasedAt = now;
            point = mapped;
        }
        if (message == WM_LBUTTONUP && (onChannelPage || armed)) {
            armed = true;
            releasedAt = now;
            point = mapped;
            return false;
        }
        // The first channel click can receive S_CA and install the lobby
        // dispatcher before Windows delivers the second half of a double
        // click. Do not let that half enter the lobby's Join Room callback.
        if (message == WM_LBUTTONDBLCLK && armed && !onChannelPage && phase == 1
                && now - releasedAt <= doubleClickMs) {
            armed = false;
            suppressRelease = true;
            return true;
        }
        return false;
    }
};

bool IsNativeChannelPage() {
    DWORD root{}, page{}, mode{}, dispatcher{};
    return ReadToolbarBytes(0x0064BBD4, &mode, 4) && mode == 2
        && ReadToolbarBytes(0x0062C9BC, &dispatcher, 4) && dispatcher == 0x004962B0
        && ReadToolbarBytes(0x0075F6E8, &root, 4) && root
        && ReadToolbarBytes(root + 0x0C, &page, 4) && page == 40;
}
