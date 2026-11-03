// Preserve LiTo's logical 40-pixel spacing and native drawing/hit rectangles.
// Reorder display positions only; never mutate the manager's linked list.
struct SharedStatusOrderRow {DWORD* node;BYTE* ui;int key;DWORD created;};
static bool SharedStatusOrderLess(const SharedStatusOrderRow& a,const SharedStatusOrderRow& b){
    if(a.key<100 || b.key<100)return a.key<b.key;
    return a.created<b.created;
}
void __fastcall SharedStatusOrderAndDraw(void* manager){
    if(!manager)return;
    SharedStatusOrderRow rows[SharedStatusCatalog::Slots]{};bool seen[SharedStatusCatalog::Slots]{};unsigned count=0;
    bool valid=true;
    auto first=reinterpret_cast<void*(__thiscall*)(void*)>(0x419DB0);
    auto next=reinterpret_cast<void*(__thiscall*)(void*)>(0x419E00);
    for(auto node=static_cast<DWORD*>(first(manager));node;node=static_cast<DWORD*>(next(manager))){
        unsigned slot=node[4];
        if(count>=SharedStatusCatalog::Slots || slot>=SharedStatusCatalog::Slots || slot==76 || seen[slot] || !node[0]){valid=false;break;}
        seen[slot]=true;
        rows[count++]={node,reinterpret_cast<BYTE*>(node[0]),SharedStatusBehavior::PriorityKeys[slot],node[1]};
    }
    if(valid){
        // Stable insertion sort bounded by catalog slots, no allocation per frame.
        for(unsigned i=1;i<count;++i){
            auto value=rows[i];unsigned j=i;
            while(j && SharedStatusOrderLess(value,rows[j-1])){rows[j]=rows[j-1];--j;}
            rows[j]=value;
        }
        for(unsigned rank=0;rank<count;++rank){
            auto ui=rows[rank].ui;
            int delta=(600-static_cast<int>(rank)*40)-*reinterpret_cast<int*>(ui+4);
            *reinterpret_cast<int*>(ui+4)+=delta;
            *reinterpret_cast<int*>(ui+12)+=delta;
        }
    }
    reinterpret_cast<void(__thiscall*)(void*)>(0x49FA90)(manager);
}
void AddSharedStatusOrderPatches(std::vector<NumericPatch>& patches){
    AddNumericRelativePatch(patches,0x48A839,{0xE8,0x52,0x52,0x01,0x00},0xE8,
        reinterpret_cast<void*>(&SharedStatusOrderAndDraw),"shared-status-priority-draw");
}
