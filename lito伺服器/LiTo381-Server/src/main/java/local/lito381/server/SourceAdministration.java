package local.lito381.server;

import java.util.Map;
import java.util.WeakHashMap;

/** Source-account authority only. A snapshot lives no longer than its login/source session. */
final class SourceAdministration {
    private record Grant(AccountRepository repository, String account, int level) {}
    private static final class Slot { Grant grant; }
    private static final Map<ClientSession, Slot> sessions = new WeakHashMap<>();

    static void clear(ClientSession session) {
        synchronized (sessions) { sessions.remove(session); }
    }

    static boolean administrator(AccountRepository repository, String account, ClientSession session) {
        return accessLevel(repository, account, session) > 0;
    }

    static int accessLevel(AccountRepository repository, String account, ClientSession session) {
        if (session == null || repository == null || account == null || account.isBlank()) {
            if (session != null) clear(session);
            return 0;
        }
        Slot slot;
        synchronized (sessions) { slot = sessions.computeIfAbsent(session, ignored -> new Slot()); }
        synchronized (slot) {
            synchronized (sessions) { if (sessions.get(session) != slot) return 0; }
            Grant grant = slot.grant;
            if (grant != null && grant.repository() == repository && grant.account().equals(account))
                return grant.level();
            int level;
            try { level = Math.max(0, repository.accessLevel(account)); }
            catch (Exception failure) {
                slot.grant = null;
                System.err.println("[LiTo] source account authority unavailable; denied: "
                        + failure.getClass().getSimpleName());
                return 0;
            }
            synchronized (sessions) {
                // A concurrent logout/source change must not republish a stale authority.
                if (sessions.get(session) != slot) return 0;
                slot.grant = new Grant(repository, account, level);
            }
            return level;
        }
    }
}
