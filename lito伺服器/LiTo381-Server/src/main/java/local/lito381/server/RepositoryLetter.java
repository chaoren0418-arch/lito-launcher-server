// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.GameStateStore;

record RepositoryLetter(long itemObjectId, int code, String sender, String receiver, String date, int templateId, String subject, String content, boolean opened) {
    RepositoryLetter {
        GameStateStore.Letter validated = new GameStateStore.Letter(itemObjectId, code, sender, receiver, date, templateId, subject, content, opened);
        sender = validated.sender();
        receiver = validated.receiver();
        date = validated.date();
        subject = validated.subject();
        content = validated.content();
    }

    GameStateStore.Letter gameStateLetter() {
        return new GameStateStore.Letter(this.itemObjectId, this.code, this.sender, this.receiver, this.date, this.templateId, this.subject, this.content, this.opened);
    }
}
