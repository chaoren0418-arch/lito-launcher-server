#include "caption_drag.h"
#include <cassert>
#include <cstdio>
using A=CaptionDragTracker::Action;
int main() {
 CaptionDragTracker top,covered;
 assert(top.update(true,true,true)==A::None); // Startup while button held.
 top.update(false,true,true);covered.update(false,false,false);
 assert(top.update(true,true,true)==A::Begin);
 assert(covered.update(true,false,false)==A::None); // Same geometry, occluded HWND.
 for(int i=0;i<100;i++) {
  assert(top.update(true,true,false)==A::Move); // Pointer can leave original caption.
  assert(covered.update(true,false,true)==A::None); // Drag crosses another caption.
 }
 assert(top.update(true,false,false)==A::End); // Alt-tab/focus loss cancels ownership.
 assert(covered.update(true,true,true)==A::None); // New foreground cannot steal held drag.
 top.update(false,false,false);covered.update(false,true,true);
 assert(covered.update(true,true,true)==A::Begin);
 assert(top.update(true,false,true)==A::None);
 assert(covered.update(false,true,true)==A::End);
 top.update(false,true,false);
 assert(top.update(true,true,false)==A::None); // Client/control click, not caption.
 assert(top.update(true,true,true)==A::None); // Sliding into caption cannot begin.
 top.update(false,true,true);
 assert(top.update(true,true,true)==A::Begin); // New click works normally.
 assert(top.update(false,true,true)==A::End);
 puts("PASS: overlapping windows; fresh press ownership; no held-button stealing; foreground loss; release; client/control exclusion");
}
