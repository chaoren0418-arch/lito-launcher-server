#pragma once
// Polling fallback for the legacy WndProc. Ownership is decided only on a
// fresh button press, never by entering another caption while already held.
struct CaptionDragTracker {
    enum class Action { None, Begin, Move, End };
    bool previousDown = true; // Observe a release before acquiring the first drag.
    bool active = false;
    Action update(bool down, bool foreground, bool hitOwnCaption) {
        const bool pressed = down && !previousDown;
        previousDown = down;
        if (active) {
            if (!down || !foreground) { active = false; return Action::End; }
            return Action::Move;
        }
        if (pressed && foreground && hitOwnCaption) {
            active = true;
            return Action::Begin;
        }
        return Action::None;
    }
};
