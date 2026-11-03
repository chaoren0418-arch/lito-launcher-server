// Observation only: preserve original arrow start and draw calls exactly once.
#ifndef FLIGHT_CALL
#define FLIGHT_CALL(address,type) reinterpret_cast<type>(address)
#endif
static LONG g_arrowFlightEvidence{};
void __fastcall ObserveArrowStart(void* self, void*) {
    auto p=static_cast<const BYTE*>(self);
    if (InterlockedIncrement(&g_arrowFlightEvidence)<=128)
        Log("native arrow start object=%p gfx=%u xy=%d,%d to=%d,%d",
            self,*reinterpret_cast<const WORD*>(p+0x14),
            *reinterpret_cast<const int*>(p+0x2C),*reinterpret_cast<const int*>(p+0x30),
            *reinterpret_cast<const int*>(p+0x14C),*reinterpret_cast<const int*>(p+0x150));
    using Start=void (__thiscall*)(void*);
    FLIGHT_CALL(0x40E8F0,Start)(self);
    // Native start may delete a zero-distance arrow; never read self afterward.
}
DWORD __fastcall ObserveArrowDraw(void* sprite,void*,int frame,int x,int y,int flags) {
    if (InterlockedIncrement(&g_arrowFlightEvidence)<=128)
        Log("native arrow draw sprite=%p frame=%d xy=%d,%d flags=%d",sprite,frame,x,y,flags);
    using Draw=DWORD (__thiscall*)(void*,int,int,int,int);
    return FLIGHT_CALL(0x4CD9D0,Draw)(sprite,frame,x,y,flags);
}
