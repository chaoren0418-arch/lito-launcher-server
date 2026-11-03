    private Map<ClientSession,GameHub.NativeClanSnapshot> captureNativeClanRecipients() {
        Map<ClientSession,GameHub.NativeClanSnapshot> captured=new LinkedHashMap<>();
        for(ClientSession recipient:hub.clanSessions(sourceCharacterIdentity())) {
            GameHub.NativeClanSnapshot snapshot=hub.nativeClanSnapshot(recipient);
            if(snapshot!=null && snapshot.current(recipient))captured.put(recipient,snapshot);
        }
        if(captured.isEmpty()) {
            GameHub.NativeClanSnapshot own=hub.nativeClanSnapshot(this);
            if(own!=null && own.current(this))captured.put(this,own);
        }
        return captured;
    }
    private Map<ClientSession,GameHub.NativeClanSnapshot> recaptureNativeClanRecipients(
            Map<ClientSession,GameHub.NativeClanSnapshot> before) {
        Map<ClientSession,GameHub.NativeClanSnapshot> captured=new LinkedHashMap<>();
        for(var entry:before.entrySet()) {
            if(!entry.getValue().selectionCurrent(entry.getKey()))continue;
            GameHub.NativeClanSnapshot current=hub.nativeClanSnapshot(entry.getKey());
            if(current!=null && current.current(entry.getKey()))captured.put(entry.getKey(),current);
        }
        return captured;
    }
    private void deleteClanMember(byte[] payload) throws IOException {
        String memberName=readClanNamePacket(payload,Protocol.C_LITO_CLAN_DELETE);
        GameHub.NativeClanSnapshot admission;
        GameHub.ClanMemberSnapshot expected;
        Map<ClientSession,GameHub.NativeClanSnapshot> before;
        CharacterSummary owner;AccountRepository source;
        synchronized(hub) {
            admission=hub.nativeClanSnapshot(this);
            if(admission==null || !admission.owner() || !admission.current(this))return;
            expected=hub.clanData(sourceCharacterIdentity()).members().stream()
                    .filter(row->row.name().equalsIgnoreCase(memberName)).findFirst().orElse(null);
            // Retained C_F7 only removes an admitted role-2 member.
            if(expected==null || expected.role()!=Protocol.LitoClanRole.MEMBER)return;
            before=captureNativeClanRecipients();owner=selectedCharacterSummary();source=selectedSourceAccounts();
            if(owner==null)return;
        }
        RepositoryClanMutation result;
        try { result=source.changeClanMember(owner.objectId(),memberName,expected.role().wireValue(),0); }
        catch(Exception failure) { System.out.println("[LiTo] clan delete source SQL failed: "+failure.getMessage());return; }
        Map<ClientSession,GameHub.NativeClanSnapshot> after;
        GameHub.ClanMemberSnapshot removed;
        synchronized(hub) {
            if(!admission.current(this))return;
            if(result.status()==RepositoryClanMutationStatus.SUCCESS) {
                hub.reconcileSqlClan(sourceCharacterIdentity(),result.snapshot());removed=expected;
            } else if(result.status()==RepositoryClanMutationStatus.UNAVAILABLE) {
                removed=hub.removeClanMember(sourceCharacterIdentity(),memberName);
            } else return;
            if(removed==null)return;
            Map<ClientSession,GameHub.NativeClanSnapshot> visible=new LinkedHashMap<>();
            for(var entry:before.entrySet())if(entry.getValue().data().members().contains(expected))visible.put(entry.getKey(),entry.getValue());
            after=recaptureNativeClanRecipients(visible);
        }
        for(var entry:after.entrySet())entry.getKey().sendNativeClanMemberDeleted(entry.getValue(),removed);
    }
    private void sendNativeClanMemberDeleted(GameHub.NativeClanSnapshot snapshot,
                                             GameHub.ClanMemberSnapshot removed) throws IOException {
        if(snapshot==null || removed==null || !snapshot.current(this))return;
        synchronized(this) {
            if(!snapshot.sameContext(this) || !transportOpen())return;
            sendEncrypted(Protocol.clanMemberDeleted(removed.name()),"S_LITO_CLAN_DELETE member="+removed.name());
        }
    }
    private void changeClanRole(byte[] payload) throws IOException {
        Protocol.ClanRankControlRequest request=Protocol.clanRankControlRequest(payload);
        if(request.command()==0) {
            System.out.println("[LiTo] C_RANK_CONTROL native clan-list close player="+playerName());return;
        }
        GameHub.NativeClanSnapshot admission;
        GameHub.ClanMemberSnapshot expected;
        Map<ClientSession,GameHub.NativeClanSnapshot> before;
        CharacterSummary owner;AccountRepository source;
        synchronized(hub) {
            admission=hub.nativeClanSnapshot(this);
            if(admission==null || !admission.owner() || !admission.current(this))return;
            expected=hub.clanData(sourceCharacterIdentity()).members().stream()
                    .filter(row->row.name().equalsIgnoreCase(request.characterName())).findFirst().orElse(null);
            if(expected==null || expected.role()==Protocol.LitoClanRole.OWNER)return;
            before=captureNativeClanRecipients();owner=selectedCharacterSummary();source=selectedSourceAccounts();
            if(owner==null)return;
        }
        RepositoryClanMutation result;
        try { result=source.changeClanMember(owner.objectId(),request.characterName(),expected.role().wireValue(),request.role()); }
        catch(Exception failure) { System.out.println("[LiTo] clan rank source SQL failed: "+failure.getMessage());return; }
        Map<ClientSession,GameHub.NativeClanSnapshot> after;
        synchronized(hub) {
            if(!admission.current(this))return;
            GameHub.ClanData clan;
            if(result.status()==RepositoryClanMutationStatus.SUCCESS)clan=hub.reconcileSqlClan(sourceCharacterIdentity(),result.snapshot());
            else if(result.status()==RepositoryClanMutationStatus.UNAVAILABLE)clan=hub.changeClanRole(sourceCharacterIdentity(),request.characterName(),request.role());
            else return;
            if(clan==null)return;
            after=recaptureNativeClanRecipients(before);
        }
        for(var entry:after.entrySet())entry.getKey().sendClanInfo(entry.getValue());
    }
