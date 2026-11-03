// Pinned common catalog reader and transactional post-parser frame extensions.
// Input blob is retained for the process lifetime by the bootstrap installer.
typedef unsigned char u8; typedef unsigned short u16; typedef unsigned int u32;
typedef void* (__cdecl *Alloc)(u32); typedef void (__cdecl *Free)(void*);
struct Header { u32 magic,version,count,ltCount,bytes,sounds,fades,reserved; };
struct Sound { u32 sprite,action,frame,count; short choices[5]; u16 pad; };
struct FadeEvent { u32 sprite,action,frame,flag,duration; };
struct ExtraSound { u32 frame; short choices[5]; u16 pad; };
static const Header* source;
static ExtraSound* output;
static u32 state; // 0 unconfigured, 1 configured, 2 loaded, 3 extended, 0xE* failure
template<class T> static T& at(void* p,u32 offset) {return *(T*)((u8*)p+offset);}
static const u32* offsets() {return (const u32*)(source+1);}
static const u8* encoded() {return (const u8*)(offsets()+source->count+1);}
static const Sound* sounds() {return (const Sound*)(encoded()+source->bytes);}
static const FadeEvent* fades() {return (const FadeEvent*)(sounds()+source->sounds);}
extern "C" __declspec(dllexport) u32 __stdcall SharedState() {return state;}
extern "C" __declspec(dllexport) u32 __stdcall SharedConfigure(const Header* blob,u32 size,ExtraSound* extra,u32 capacity) {
    if(state || !blob || size<sizeof(Header) || !extra)return 0;
    if(blob->magic!=0x3152544C || blob->version!=1 || blob->count!=30000 || blob->ltCount!=285 ||
       blob->sounds>10000 || blob->fades>10000 || blob->bytes>0x2000000 || capacity<blob->sounds)return 0;
    u32 expected=sizeof(Header)+(blob->count+1)*4+blob->bytes+blob->sounds*sizeof(Sound)+blob->fades*sizeof(FadeEvent);
    if(size!=expected)return 0;
    source=blob;
    if(offsets()[0]!=0 || offsets()[source->count]!=source->bytes) {source=0;return 0;}
    for(u32 i=0;i<source->count;++i) {
        if(offsets()[i]>=offsets()[i+1] || offsets()[i+1]>source->bytes || encoded()[offsets()[i+1]-1]!=0) {
            source=0;return 0;
        }
    }
    output=extra;state=1;return 1;
}
extern "C" __declspec(dllexport) int __fastcall SharedLoad(void* reader,void*,u32,u32) {
    if(state!=1 || !source)return 0;
    u8* data=(u8*)((Alloc)0x4F118C)(source->bytes);
    u8** index=(u8**)((Alloc)0x4F118C)(source->count*4);
    if(!data || !index) {
        if(data)((Free)0x4F103C)(data);if(index)((Free)0x4F103C)(index);state=0xE1;return 0;
    }
    for(u32 i=0;i<source->bytes;++i)data[i]=encoded()[i];
    for(u32 i=0;i<source->count;++i)index[i]=data+offsets()[i];
    if(at<void*>(reader,8))((Free)0x4F103C)(at<void*>(reader,8));
    if(at<void*>(reader,4))((Free)0x4F103C)(at<void*>(reader,4));
    at<u32>(reader,0)=source->count;at<u8**>(reader,4)=index;at<u8*>(reader,8)=data;
    state=2;return 1;
}
static u8* frame(u32 sprite,u32 action,u32 ordinal) {
    if(sprite>=source->count || action>=100)return 0;
    u8* table=*(u8**)0x834E70;if(!table)return 0;
    u8* row=table+(sprite*143+action)*12;
    if(ordinal>=at<u8>(row,1) || !at<u8*>(row,8))return 0;
    return at<u8*>(row,8)+ordinal*20;
}
extern "C" __declspec(dllexport) int __stdcall SharedPostparse() {
    if(state!=2 || *(u32*)0x647E90!=30000 || *(u32*)0x647E7C!=30285) {state=0xE2;return 0;}
    // Validate the entire transaction before touching any native frame.
    for(u32 i=0;i<source->sounds;++i) {
        const Sound& s=sounds()[i];u8* f=frame(s.sprite,s.action,s.frame);
        if(!f || s.count<4 || s.count>5) {state=0xE3;return 0;}
        for(u32 j=0;j<3;++j)if(at<short>(f,2+j*2)!=s.choices[j]) {state=0xE4;return 0;}
    }
    for(u32 i=0;i<source->fades;++i) {
        const FadeEvent& e=fades()[i];u8* f=frame(e.sprite,e.action,e.frame);
        if(!f || (e.flag!=8 && e.flag!=16) || !e.duration || e.duration>32767 ||
           !(f[0x12]&4) || at<u16>(f,0xE)!=e.duration) {state=0xE5;return 0;}
    }
    for(u32 i=0;i<source->sounds;++i) {
        const Sound& s=sounds()[i];u8* f=frame(s.sprite,s.action,s.frame);
        output[i].frame=(u32)f;output[i].pad=0;
        for(u32 j=0;j<5;++j)output[i].choices[j]=s.choices[j];
        at<u16>(f,8)=(u16)s.count;
    }
    for(u32 i=0;i<source->fades;++i) {
        const FadeEvent& e=fades()[i];u8* f=frame(e.sprite,e.action,e.frame);
        f[0x12]=(u8)((f[0x12]&~4)|e.flag);
    }
    state=3;return 1;
}
