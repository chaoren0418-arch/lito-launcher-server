// The unpacker emits either NOP/CALL or CALL/NOP in the same six-byte slot.
// Validate both the argument instructions and the entire slot before editing.
BYTE* FindEditCall(BYTE* slot, const char* prefix, size_t length) {
    if (std::memcmp(slot - length, prefix, length)) return nullptr;
    if (slot[0] == 0x90 && slot[1] == 0xE8) return slot + 1;
    if (slot[0] == 0xE8 && slot[5] == 0x90) return slot;
    return nullptr;
}
