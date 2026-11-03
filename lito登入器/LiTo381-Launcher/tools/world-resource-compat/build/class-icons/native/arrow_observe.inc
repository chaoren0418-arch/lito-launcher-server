#ifndef ARROW_CALL
#define ARROW_CALL(address,type) reinterpret_cast<type>(address)
#endif
void* __fastcall ObserveNativeArrow(void* self, void*, DWORD sequence, int gfx,
        int flight, int x, int y, int targetX, int targetY, int heading,
        DWORD target, int damage, int extra, void* text) {
    using Constructor = void* (__thiscall*)(void*,DWORD,int,int,int,int,int,int,int,DWORD,int,int,void*);
    void* result = ARROW_CALL(0x40E2E0, Constructor)(self,sequence,gfx,
        flight,x,y,targetX,targetY,heading,target,damage,extra,text);
    if (result) {
        auto bytes=static_cast<const BYTE*>(result);
        WORD actual{},frame{};memcpy(&actual,bytes+0x14,2);memcpy(&frame,bytes+0x1A,2);
        Log("native arrow constructed sequence=%lu requested=%d actual=%u frame-map=%u heading=%d target=%lu xy=%d,%d to=%d,%d",
            sequence,gfx,actual,frame,heading,target,x,y,targetX,targetY);
    }
    return result;
}
