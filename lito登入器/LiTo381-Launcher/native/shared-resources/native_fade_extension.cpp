// Shared-source frame fades. No CRT, no renderer replacement, no actor ABI growth.
// Verified with the original RGB555/RGB565 blend routines in isolation.
typedef unsigned char u8;
typedef unsigned short u16;
typedef unsigned int u32;
struct Fade {
    Fade* next; u8* actor; u32 objectId;
    int duration, remaining, progress, direction, alpha;
};
static Fade* head;
template<class T> static T& at(void* p,int offset) { return *(T*)((u8*)p+offset); }
typedef void* (__cdecl *Alloc)(u32);
typedef void (__cdecl *Free)(void*);
typedef void (__fastcall *Schedule)(u32,void*,void*);
typedef void (__fastcall *Cancel)(void*,void*);
static Fade* find(u8* actor) {
    for(Fade* p=head;p;p=p->next) if(p->actor==actor) return p;
    return 0;
}
extern "C" __declspec(dllexport) void __fastcall FadeTick(u8* actor);
extern "C" __declspec(dllexport) void __cdecl FadeForget(u8* actor) {
    ((Cancel)0x4D6870)((void*)FadeTick,actor);
    Fade** link=&head;
    while(*link) {
        Fade* p=*link;
        if(p->actor==actor) { *link=p->next; ((Free)0x4F103C)(p); return; }
        link=&p->next;
    }
}
static void applySprite(u8* actor,u8* sprite,int alpha,bool restore) {
    if(!sprite)return;
    int frame=(signed char)actor[0x19];
    if(frame<0 || frame>=at<u16>(sprite,0xC))return;
    u8* action=sprite+frame*24;
    int count=at<u16>(action,0x20);u8* parts=at<u8*>(action,0x24);
    u32* images=at<u32*>(sprite,4);
    u8* map=*(u8**)0x75AA5C;
    if(!map || !parts || !images)return;
    for(int i=0;i<count;++i) {
        u8* part=parts+i*12;
        int x=at<int>(actor,0x2C)+at<int>(part,0)-*(int*)0x75ABF0;
        int y=at<int>(actor,0x30)+at<int>(part,4)-*(int*)0x75ABF4;
        if((u32)x>=256 || (u32)y>=128)continue;
        u8* list=at<u8*>(map+(y*256+x)*16,8);
        if(!list)continue;
        int n=at<short>(list,0);
        for(int j=0;j<n;++j) {
            u8* entry=list+4+j*16;
            if(at<u32>(entry,4)!=images[at<u16>(part,10)] || entry[0]!=part[8])continue;
            if(restore) at<u32>(entry,8)&=~0x800u;
            else {
                at<u32>(entry,8)|=0x800u;
                // Native pixel fixture: 0 preserves background; 31 is source.
                entry[12]=(u8)alpha;
            }
            break;
        }
    }
}
extern "C" __declspec(dllexport) void __cdecl FadeApply(u8* actor) {
    Fade* p=find(actor);
    if(!p || p->objectId!=at<u32>(actor,8))return;
    bool restore=p->remaining<0 && p->direction>0;
    applySprite(actor,at<u8*>(actor,0x84),p->alpha,restore);
    applySprite(actor,at<u8*>(actor,0x88),p->alpha/2,restore);
}
extern "C" __declspec(dllexport) void __fastcall FadeTick(u8* actor) {
    Fade* p=find(actor);
    if(!p || p->objectId!=at<u32>(actor,8))return;
    if(p->remaining>0) {
        p->alpha=(p->progress*32)/p->duration;
        if(p->alpha>31)p->alpha=31;
        --p->remaining;p->progress+=p->direction;
        ((Schedule)0x4D65F0)(10,(void*)FadeTick,actor);
    } else {
        p->alpha=p->direction>0?31:0;p->remaining=-1;
    }
    FadeApply(actor);
}
extern "C" __declspec(dllexport) void __cdecl FadeFrameEvent(u8* actor) {
    if(!actor)return;
    int sprite=at<short>(actor,0x14),action=(signed char)actor[0x10],frame=(signed char)actor[0x13];
    if((u32)sprite>=*(u32*)0x647E7C || (u32)action>=100 || frame<0)return;
    u8* table=*(u8**)0x834E70;if(!table)return;
    u8* row=table+(sprite*143+action)*12;
    if(frame>=at<u8>(row,1))return;
    u8* frames=at<u8*>(row,8);if(!frames)return;
    u8* event=frames+frame*20;
    int flags=event[0x12];if(flags&4 || !(flags&24))return;
    int duration=at<short>(event,0xE);if(duration<=0)return;
    Fade* p=find(actor);
    if(!p) {
        p=(Fade*)((Alloc)0x4F118C)(sizeof(Fade));if(!p)return;
        p->next=head;p->actor=actor;head=p;
    } else ((Cancel)0x4D6870)((void*)FadeTick,actor);
    p->objectId=at<u32>(actor,8);p->duration=duration;p->remaining=duration;
    p->direction=(flags&8)?1:-1;p->progress=p->direction>0?0:duration;
    p->alpha=p->direction>0?0:31;
    ((Schedule)0x4D65F0)(10,(void*)FadeTick,actor);
}
extern "C" int __stdcall DllMain(void*,u32,void*) { return 1; }
