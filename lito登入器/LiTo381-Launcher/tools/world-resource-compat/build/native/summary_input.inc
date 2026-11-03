bool BlockSummaryBackgroundClick(bool summary, UINT message, int x, int y) {
    if (!summary) return false;
    const bool button = message == WM_LBUTTONDOWN || message == WM_LBUTTONUP
        || message == WM_LBUTTONDBLCLK || message == WM_RBUTTONDOWN
        || message == WM_RBUTTONUP || message == WM_RBUTTONDBLCLK;
    // The summary owns its two lists and native Confirm. Background toolbar
    // is below/right of the lists; only the verified Confirm box is interactive.
    return button && y >= 426 && !(x >= 532 && x <= 588 && y >= 436 && y <= 468);
}
