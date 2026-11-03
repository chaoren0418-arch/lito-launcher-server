#pragma once
// Display lifetime only. No combat state, packet duration or database writes.
// Host owns native UI/timer calls; the same template is exercised by tests.
template<class Host,unsigned Slots> class SharedStatusLifecycle {
    bool expired_[Slots]{};
public:
    bool Expired(unsigned slot) const {return slot<Slots && expired_[slot];}
    void Clear(unsigned slot) {
        if(slot>=Slots)return;
        Host::CancelFinish(slot);
        expired_[slot]=false;
        Host::RestoreIcon(slot);
        Host::ClearNative(slot);
    }
    void Apply(int seconds,unsigned slot) {
        if(slot>=Slots || slot==76 || (seconds<0 && seconds!=-1) || seconds>32767)return;
        if(!Host::HasManager())return;
        Clear(slot);
        if(seconds==0)return;
        unsigned group=Host::Group(slot);
        if(group)for(unsigned other=0;other<Slots;++other)
            if(other!=slot && Host::Group(other)==group &&
               (Host::Active(other) || expired_[other]))Clear(other);
        Host::ApplyNative(seconds,slot);
    }
    void Expire(unsigned slot) {
        if(slot>=Slots || expired_[slot])return;
        if(!Host::HasManager() || !Host::Active(slot)) {
            Clear(slot);return;
        }
        if(!Host::ShowExpired() || !Host::OffIcon(slot)) {
            Host::ExpireNative(slot);return;
        }
        // Stop warning flashes, complete ordinary expiry and its end message,
        // then create a display-only, untimed off-icon using the same UI.
        Host::CancelBlink(slot);
        Host::ExpireNative(slot);
        Host::EndMessageNative(slot);
        Host::SetOffIcon(slot);
        expired_[slot]=true;
        Host::AddExpiredNative(slot);
        Host::ScheduleFinish(slot,10000);
    }
    void Finish(unsigned slot) {
        if(slot>=Slots || !expired_[slot])return;
        // This callback is already executing: don't cancel its own queue node.
        expired_[slot]=false;
        Host::RestoreIcon(slot);
        Host::ClearNative(slot);
    }
};
