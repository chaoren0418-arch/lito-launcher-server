#include <cstdio>
#include <cstring>
#include <initializer_list>
#include "shared_status_lifecycle.h"
struct Host {
    static inline bool manager=true,show=true,active[96]{},ui[96]{},timer[96]{},warning[96]{};
    static inline unsigned icon[96]{},off[96]{},groups[96]{},finish[96]{},ended[96]{};
    static inline int seconds[96]{};
    static bool HasManager(){return manager;}
    static bool Active(unsigned s){return active[s];}
    static bool ShowExpired(){return show;}
    static unsigned Group(unsigned s){return groups[s];}
    static unsigned OffIcon(unsigned s){return off[s];}
    static void RestoreIcon(unsigned s){icon[s]=s+100;}
    static void SetOffIcon(unsigned s){icon[s]=off[s];}
    static void CancelFinish(unsigned s){finish[s]=0;}
    static void CancelBlink(unsigned s){warning[s]=false;}
    static void ClearNative(unsigned s){active[s]=ui[s]=timer[s]=warning[s]=false;}
    static void ApplyNative(int time,unsigned s){
        active[s]=ui[s]=true;seconds[s]=time;timer[s]=time>0;warning[s]=time>0;
    }
    static void ExpireNative(unsigned s){active[s]=ui[s]=timer[s]=false;}
    static void EndMessageNative(unsigned s){++ended[s];}
    static void AddExpiredNative(unsigned s){active[s]=ui[s]=true;seconds[s]=-1;}
    static void ScheduleFinish(unsigned s,unsigned delay){finish[s]=delay;}
};
static unsigned checks;
#define CHECK(x) do{++checks;if(!(x)){std::printf("FAIL lifecycle line=%d\n",__LINE__);return 1;}}while(0)
int main(){
    SharedStatusLifecycle<Host,96> state;
    for(unsigned s=0;s<96;++s){
        if(s==76)continue;
        Host::off[s]=s+5000;
        for(int seconds:{1,10,11,600,32767}){
            state.Apply(seconds,s);
            CHECK(Host::active[s] && Host::ui[s] && Host::seconds[s]==seconds && !state.Expired(s));
            state.Expire(s);
            CHECK(state.Expired(s) && Host::active[s] && Host::ui[s] && Host::icon[s]==s+5000);
            CHECK(!Host::timer[s] && !Host::warning[s] && Host::finish[s]==10000 && Host::seconds[s]==-1);
            unsigned ended=Host::ended[s];state.Expire(s);
            CHECK(Host::ended[s]==ended); // no duplicate expiry
            Host::finish[s]=0;state.Finish(s);
            CHECK(!state.Expired(s) && !Host::active[s] && !Host::ui[s] && Host::icon[s]==s+100);
        }
        state.Apply(600,s);state.Expire(s);state.Apply(20,s);
        CHECK(!state.Expired(s) && Host::finish[s]==0 && Host::icon[s]==s+100 && Host::seconds[s]==20);
        state.Finish(s);CHECK(Host::active[s] && Host::seconds[s]==20); // stale callback ignored
        state.Apply(0,s);CHECK(!Host::active[s] && !Host::timer[s] && !Host::finish[s]);
        state.Apply(-1,s);CHECK(Host::active[s] && !Host::timer[s] && !Host::warning[s]);
        state.Clear(s);
    }
    Host::groups[4]=Host::groups[6]=5;
    state.Apply(600,4);state.Expire(4);state.Apply(600,6);
    CHECK(!Host::active[4] && !Host::ui[4] && !Host::finish[4] && !state.Expired(4));
    CHECK(Host::active[6]);
    state.Apply(0,4);CHECK(Host::active[6]); // removing one must not remove its replacement
    state.Apply(600,4);CHECK(!Host::active[6] && !Host::timer[6] && !Host::warning[6]);
    Host::show=false;state.Expire(4);
    CHECK(!Host::active[4] && !Host::ui[4] && !state.Expired(4));
    Host::show=true;Host::off[6]=0;state.Apply(600,6);state.Expire(6);
    CHECK(!Host::active[6] && !state.Expired(6));
    state.Apply(600,4);state.Expire(4);Host::manager=false;state.Clear(4);
    CHECK(!Host::finish[4] && !Host::active[4] && Host::icon[4]==104);
    state.Apply(600,4);CHECK(!Host::active[4]);Host::manager=true;
    for(unsigned s:{76u,96u,0xFFFFFFFFu})state.Apply(600,s);
    state.Apply(-2,4);state.Apply(32768,4);CHECK(!Host::active[4]);
    // Shutdown during a pending active or expired timer.
    state.Apply(600,4);Host::manager=false;state.Expire(4);
    CHECK(!Host::active[4] && !Host::timer[4]);
    Host::manager=true;state.Apply(600,4);state.Expire(4);
    Host::manager=false;state.Finish(4);CHECK(!Host::active[4] && !state.Expired(4));
    std::printf("PASS lifecycle %u checks: expiry, refresh, removal, groups, shutdown; scheduler/UI modeled\n",checks);
}
