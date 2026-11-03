#pragma once
#include <array>
#include <cstddef>
#include <cstdint>
#include <cstring>

namespace LitoCharacterPages {
constexpr unsigned PageSize = 4, Capacity = 8;
// S_CB: two bounded, NUL-terminated strings followed by the native fixed fields.
constexpr std::size_t FixedBytes = 79, MaxPacket = 1 + 28 + 17 + FixedBytes;
struct Row { std::array<std::uint8_t, MaxPacket> bytes{}; std::size_t size{}; };
class Directory {
    std::array<Row, Capacity> rows_{};
    unsigned expected_{}, received_{}, page_{};
    bool valid_{};
public:
    bool Begin(unsigned count) {
        rows_ = {}; received_ = page_ = expected_ = 0;
        valid_ = count <= Capacity;
        if (valid_) expected_ = count;
        return valid_;
    }
    bool Add(const std::uint8_t* packet, std::size_t available) {
        if (!valid_ || received_ >= expected_ || !packet || !available || packet[0] != 0xCB)
            return false;
        std::size_t end = 1;
        for (auto limit : {28U, 17U}) {
            unsigned length = 0;
            while (end < available && length < limit && packet[end]) { ++end; ++length; }
            if (end >= available || length == limit) return false;
            ++end;
        }
        if (available - end < FixedBytes) return false;
        end += FixedBytes;
        Row& row = rows_[received_++]; row.size = end;
        std::memcpy(row.bytes.data(), packet, end);
        return true;
    }
    bool Complete() const { return valid_ && received_ == expected_; }
    bool Receiving() const { return valid_ && received_ < expected_; }
    unsigned Count() const { return expected_; }
    unsigned Page() const { return page_; }
    unsigned Pages() const { return (expected_ + PageSize - 1) / PageSize; }
    unsigned Visible() const {
        const unsigned left = expected_ - page_ * PageSize;
        return left < PageSize ? left : PageSize;
    }
    bool Turn(int direction) {
        if (!Complete() || Pages() < 2 || !direction) return false;
        const int next = static_cast<int>(page_) + (direction > 0 ? 1 : -1);
        if (next < 0 || next >= static_cast<int>(Pages())) return false;
        page_ = static_cast<unsigned>(next); return true;
    }
    const Row& At(unsigned visibleIndex) const { return rows_.at(page_ * PageSize + visibleIndex); }
};
}
