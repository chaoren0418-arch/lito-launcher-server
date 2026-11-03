// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

public class NativeDeChatData {
    private int _npcId;
    private int _chatTiming;
    private int _startDelayTime;
    private String _chatId1;
    private String _chatId2;
    private String _chatId3;
    private String _chatId4;
    private String _chatId5;
    private int _chatInterval;
    private boolean _isShout;
    private boolean _isWorldChat;
    private boolean _isRepeat;
    private int _repeatInterval;
    private int _gameTime;

    public int getNpcId() {
        return this._npcId;
    }

    public void setNpcId(int n) {
        this._npcId = n;
    }

    public int getChatTiming() {
        return this._chatTiming;
    }

    public void setChatTiming(int n) {
        this._chatTiming = n;
    }

    public int getStartDelayTime() {
        return this._startDelayTime;
    }

    public void setStartDelayTime(int n) {
        this._startDelayTime = n;
    }

    public String getChatId1() {
        return this._chatId1;
    }

    public void setChatId1(String string) {
        this._chatId1 = string;
    }

    public String getChatId2() {
        return this._chatId2;
    }

    public void setChatId2(String string) {
        this._chatId2 = string;
    }

    public String getChatId3() {
        return this._chatId3;
    }

    public void setChatId3(String string) {
        this._chatId3 = string;
    }

    public String getChatId4() {
        return this._chatId4;
    }

    public void setChatId4(String string) {
        this._chatId4 = string;
    }

    public String getChatId5() {
        return this._chatId5;
    }

    public void setChatId5(String string) {
        this._chatId5 = string;
    }

    public int getChatInterval() {
        return this._chatInterval;
    }

    public void setChatInterval(int n) {
        this._chatInterval = n;
    }

    public boolean isShout() {
        return this._isShout;
    }

    public void setShout(boolean bl) {
        this._isShout = bl;
    }

    public boolean isWorldChat() {
        return this._isWorldChat;
    }

    public void setWorldChat(boolean bl) {
        this._isWorldChat = bl;
    }

    public boolean isRepeat() {
        return this._isRepeat;
    }

    public void setRepeat(boolean bl) {
        this._isRepeat = bl;
    }

    public int getRepeatInterval() {
        return this._repeatInterval;
    }

    public void setRepeatInterval(int n) {
        this._repeatInterval = n;
    }

    public int getGameTime() {
        return this._gameTime;
    }

    public void setGameTime(int n) {
        this._gameTime = n;
    }
}
