#pragma once
#include <cstdint>
// Presentation only. Expiry never settles a match or sends a client request.
struct BattleClock {
    bool armed{}, visible{};
    std::uint64_t deadlineMs{};
    void clear() { armed = visible = false; deadlineMs = 0; }
    void startScene() { clear(); armed = true; }
    void synchronize(std::uint32_t seconds, std::uint64_t now) {
        if (!armed) return;
        visible = seconds != 0;
        deadlineMs = now + std::uint64_t(seconds) * 1000;
    }
    std::uint32_t remaining(std::uint64_t now) const {
        return now >= deadlineMs ? 0 : static_cast<std::uint32_t>((deadlineMs - now + 999) / 1000);
    }
};
