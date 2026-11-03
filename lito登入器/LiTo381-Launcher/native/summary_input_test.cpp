#include <windows.h>
#include <cassert>
#include <initializer_list>
#include "summary_input.inc"
int main() {
 for (UINT m : {WM_LBUTTONDOWN,WM_LBUTTONUP,WM_LBUTTONDBLCLK,WM_RBUTTONDOWN,WM_RBUTTONUP}) {
   assert(BlockSummaryBackgroundClick(true,m,611,455));
   assert(BlockSummaryBackgroundClick(true,m,630,468));
   assert(!BlockSummaryBackgroundClick(true,m,560,451));
   assert(!BlockSummaryBackgroundClick(false,m,630,468));
   assert(!BlockSummaryBackgroundClick(true,m,490,110));
 }
 assert(!BlockSummaryBackgroundClick(true,WM_MOUSEMOVE,630,468));
}
