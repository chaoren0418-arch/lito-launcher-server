// Bound the common-source item RLE before LiTo's in-place 555 -> 565 converter.
// A row omitted at EOF is transparent, as in the reference resource decoder.
// Only complete rows are retained if a span is truncated; original archives stay intact.
typedef unsigned char byte;
typedef unsigned int uint;
static volatile uint repairedTbt;
static volatile uint rejectedTbt;
typedef void* (__cdecl *Allocate)(uint);
typedef void (__cdecl *Free)(void*);
static const Allocate allocate = (Allocate)0x4F118C;
static const Free release = (Free)0x4F103C;

extern "C" __declspec(dllexport) byte* __stdcall NormalizeTbt(byte* data, uint size) {
    if (!data) return data;
    if (size < 4) { ++rejectedTbt; release(data); return 0; }
    uint cursor = 4, row = 0, completeEnd = 4;
    const uint height = data[3];
    for (; row < height; ++row) {
        if (cursor >= size) break;
        uint spans = data[cursor++];
        bool complete = true;
        for (uint span = 0; span < spans; ++span) {
            if (size - cursor < 2) { complete = false; break; }
            uint pixels = data[cursor + 1]; cursor += 2;
            if (pixels * 2 > size - cursor) { complete = false; break; }
            cursor += pixels * 2;
        }
        if (!complete) break;
        completeEnd = cursor;
    }
    if (row == height) return data;
    const uint paddedSize = completeEnd + height - row;
    byte* padded = (byte*)allocate(paddedSize);
    if (!padded) { ++rejectedTbt; release(data); return 0; }
    volatile byte* output = padded; // no implicit external CRT memcpy dependency
    for (uint i = 0; i < completeEnd; ++i) output[i] = data[i];
    for (uint i = completeEnd; i < paddedSize; ++i) output[i] = 0;
    release(data); ++repairedTbt;
    return padded;
}

extern "C" __declspec(dllexport) uint __stdcall TbtRepairCount() { return repairedTbt; }
extern "C" __declspec(dllexport) uint __stdcall TbtRejectCount() { return rejectedTbt; }
