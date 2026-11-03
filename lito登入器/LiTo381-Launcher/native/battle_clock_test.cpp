#include "battle_clock.h"
#include <cassert>
#include <cstdio>
int main() {
    BattleClock c;
    c.synchronize(90,100);assert(!c.visible);
    for(int mode=0;mode<6;mode++) {
        c.startScene();assert(!c.visible);
        c.synchronize(90,100);assert(c.visible&&c.remaining(100)==90);
        assert(c.remaining(1099)==90&&c.remaining(1100)==89);
        assert(c.remaining(61100)==29);
        c.synchronize(20,70100);assert(c.remaining(70100)==20);
        assert(c.remaining(120100)==0&&c.visible);
        c.clear();assert(!c.visible&&!c.armed);
        c.synchronize(200,120100);assert(!c.visible);
    }
    c.startScene();c.synchronize(0,0);assert(!c.visible);
    c.synchronize(6000,0);assert(c.remaining(1000)==5999);
    c.synchronize(0xffffffff,0);assert(c.remaining(1000)==0xfffffffe);
    c.startScene();assert(!c.visible&&c.remaining(0)==0);
    std::puts("PASS: six modes, hidden before sync, monotonic countdown, late-join correction, zero hide, expiry retained, stale/result/reset, long duration and no overflow.");
}
