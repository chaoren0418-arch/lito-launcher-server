struct IconDrawEvent {int id,x,y,clip;};
static std::vector<IconDrawEvent> iconDraws;
static int iconTextX,iconTextY,iconTextCalls;
static void* iconTestTable[243]{};
static void __fastcall CaptureRosterIcon(void*,void* image,void*,int x,int y) {
    iconDraws.push_back({static_cast<int>((reinterpret_cast<DWORD>(image)-0x200000)/16),x,y,-1});
}
static void __fastcall CaptureRosterClip(void*,void* image,int clip,int x,int y) {
    iconDraws.push_back({static_cast<int>((reinterpret_cast<DWORD>(image)-0x200000)/16),x,y,clip});
}
static void __fastcall CaptureRosterText(void*,const char*,int x,int y,int,int) {
    ++iconTextCalls;iconTextX=x;iconTextY=y;
}
static int RunClassIconFixture() {
    CHECK_INSTALL(*reinterpret_cast<BYTE*>(0x4633B0)==0xE9);
    CHECK_INSTALL(*reinterpret_cast<BYTE*>(0x463646)==0xE9);
    CHECK_INSTALL(*reinterpret_cast<BYTE*>(0x463B01)==0xE9);
    RedirectFixtureCall(0x4B9980,reinterpret_cast<void*>(&CaptureRosterIcon));
    RedirectFixtureCall(0x4B9AF0,reinterpret_cast<void*>(&CaptureRosterClip));
    RedirectFixtureCall(0x430D20,reinterpret_cast<void*>(&CaptureRosterText));
    for(int i=0;i<243;++i)iconTestTable[i]=reinterpret_cast<void*>(0x200000+i*16);
    *reinterpret_cast<void***>(0x64A964)=iconTestTable;
    *reinterpret_cast<int*>(0x64A95C)=243;
    *reinterpret_cast<void**>(0x64B438)=reinterpret_cast<void*>(0x1234);
    *reinterpret_cast<int*>(0x63D54C)=50000;
    constexpr int ids[7][5]={{1,28,24,37,41},{2,29,25,38,42},{3,30,26,39,43},
        {4,31,27,40,44},{45,47,46,48,49},{233,234,235,236,237},{238,239,240,241,242}};
    unsigned cases=0;
    for(int renderer=0;renderer<2;++renderer)for(int type=0;type<7;++type)
    for(int state=0;state<=34;++state)for(int ready=0;ready<3;++ready) {
        BYTE row[0x7c]{};const char* name="ClassIcon";
        *reinterpret_cast<DWORD*>(row+8)=static_cast<DWORD>((1<<16)|(type+28));
        *reinterpret_cast<const char**>(row+0x10)=name;
        *reinterpret_cast<int*>(row+0x70)=state;*reinterpret_cast<int*>(row+0x6c)=ready;
        BYTE before[sizeof row];std::memcpy(before,row,sizeof row);
        iconDraws.clear();iconTextCalls=0;
        if(renderer==0)reinterpret_cast<void(__thiscall*)(void*,int,int)>(0x463350)(row,11,22);
        else reinterpret_cast<void(__thiscall*)(void*,int,int,int)>(0x4635F0)(row,11,22,0x7fff);
        const int variant=state==34?4:state==33?3:1;
        CHECK_INSTALL(iconDraws.size()==static_cast<unsigned>(1+(variant==1?2:1)+(renderer==1 && ready?1:0)));
        CHECK_INSTALL(iconDraws[0].id==193 && iconDraws[0].x==11 && iconDraws[0].y==22);
        CHECK_INSTALL(iconDraws[1].id==ids[type][variant] && iconDraws[1].x==27 && iconDraws[1].y==22);
        if(variant==1)CHECK_INSTALL(iconDraws[2].id==ids[type][2] && iconDraws[2].clip==state);
        if(renderer==1 && ready)CHECK_INSTALL(iconDraws.back().id==(ready==1?18:23));
        CHECK_INSTALL(iconTextCalls==1 && iconTextX==(renderer==0?47:43) && iconTextY==22);
        CHECK_INSTALL(!std::memcmp(before,row,sizeof row));++cases;
    }
    for(int type=0;type<7;++type) {
        BYTE row[0x7c]{};
        *reinterpret_cast<DWORD*>(row+8)=(1<<16)|(type+1);
        *reinterpret_cast<const char**>(row+0x10)="ResultIcon";
        iconDraws.clear();iconTextCalls=0;
        reinterpret_cast<void(__thiscall*)(void*,int,int)>(0x463AB0)(row,11,22);
        CHECK_INSTALL(iconDraws.size()==2 && iconDraws[0].id==193 && iconDraws[1].id==ids[type][0]);
        CHECK_INSTALL(iconTextCalls==1 && iconTextX==45 && iconTextY==22);++cases;
    }
    CHECK_INSTALL(ExtendedClassIconId(-1,0)==-1 && ExtendedClassIconId(7,0)==-1);
    CHECK_INSTALL(ExtendedClassIconId(5,5)==-1 && ExtendedClassIconId(6,-1)==-1);
    *reinterpret_cast<int*>(0x64A95C)=233;
    CHECK_INSTALL(ExtendedClassBitmap(5,0)==nullptr && ExtendedClassBitmap(6,4)==nullptr);
    std::printf("PASS %u actual native roster/result renders: seven classes,35 states,ready flags,rank/name positions,read-only rows\n",cases);
    return 0;
}
