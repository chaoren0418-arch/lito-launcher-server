// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeVisibilityScope;

public abstract class NativeDeVisibilityPacket
extends NativeDeDeletionPorts.Packet {
    protected final ByteArrayOutputStream _bao = new ByteArrayOutputStream();
    private volatile byte[] _byteCache = null;
    private volatile byte[] _preparedPayload = null;
    private volatile IllegalStateException _constructionFailure = null;

    private void assertPayloadMutable() {
        if (this._byteCache != null || this._preparedPayload != null || this._constructionFailure != null) {
            long l = NativeDeVisibilityScope.POST_FINALIZE_WRITE_COUNT().incrementAndGet();
            NativeDeVisibilityScope.log().warn("[PacketSafety] rejected write after payload finalization: packet={} thread={} violations={}", (Object)this.getDiagnosticContext(), (Object)Thread.currentThread().getName(), (Object)l);
            throw new IllegalStateException("Packet payload is immutable after getBytes(): " + this.getType());
        }
    }

    protected final synchronized void markConstructionFailed(String string, Throwable throwable) {
        if (this._constructionFailure != null) {
            return;
        }
        String string2 = string != null ? string : "unspecified";
        long l = NativeDeVisibilityScope.CONSTRUCTION_FAILURE_COUNT().incrementAndGet();
        this._constructionFailure = new IllegalStateException("Packet construction failed: " + this.getType() + " operation=" + string2, throwable);
        NativeDeVisibilityScope.log().warn("[PacketSafety] packet construction failed; partial payload blocked: packet={} operation={} thread={} failures={}", (Object)this.getDiagnosticContext(), (Object)string2, (Object)Thread.currentThread().getName(), (Object)l, (Object)throwable);
    }

    protected final synchronized void writeD(int n) {
        this.assertPayloadMutable();
        this._bao.write(n & 0xFF);
        this._bao.write(n >> 8 & 0xFF);
        this._bao.write(n >> 16 & 0xFF);
        this._bao.write(n >> 24 & 0xFF);
    }

    protected final synchronized void writeH(int n) {
        this.assertPayloadMutable();
        this._bao.write(n & 0xFF);
        this._bao.write(n >> 8 & 0xFF);
    }

    protected final synchronized void writeC(int n) {
        this.assertPayloadMutable();
        this._bao.write(n & 0xFF);
    }

    protected final synchronized void writeP(int n) {
        this.assertPayloadMutable();
        this._bao.write(n);
    }

    protected final synchronized void writeS(String string) {
        this.assertPayloadMutable();
        try {
            if (string != null) {
                StringBuilder stringBuilder = new StringBuilder(string);
                for (String string2 : NativeDeVisibilityScope.BAD_TEXT_LIST()) {
                    int n;
                    while ((n = stringBuilder.indexOf(string2)) != -1) {
                        int n2 = n + string2.length();
                        stringBuilder.delete(n, n2);
                    }
                }
                this._bao.write(stringBuilder.toString().getBytes(NativeDeVisibilityScope.CLIENT_LANGUAGE_CODE()));
            }
        }
        catch (Exception exception) {
            NativeDeVisibilityScope.log().error(exception.getLocalizedMessage(), exception);
        }
        this._bao.write(0);
    }

    protected final synchronized void writeByte(byte[] byArray) {
        this.assertPayloadMutable();
        try {
            if (byArray != null) {
                this._bao.write(byArray);
            }
        }
        catch (Exception exception) {
            NativeDeVisibilityScope.log().error(exception.getLocalizedMessage(), exception);
        }
        this._bao.write(0);
    }

    protected final synchronized byte[] getBytes() {
        IllegalStateException illegalStateException = this._constructionFailure;
        if (illegalStateException != null) {
            throw illegalStateException;
        }
        byte[] byArray2 = this._byteCache;
        if (byArray2 != null) {
            return byArray2;
        }
        int n2 = this._bao.size();
        int n3 = 8 - (n2 & 7) & 7;
        byte[] byArray;
        try {
            for (int n = 0; n < n3; ++n) {
                this._bao.write(0);
            }
            byArray = this._bao.toByteArray();
        }
        catch (RuntimeException | Error throwable) {
            long l = NativeDeVisibilityScope.FINALIZE_FAILURE_COUNT().incrementAndGet();
            NativeDeVisibilityScope.log().warn("[PacketSafety] payload finalization failed: packet={} rawLength={} thread={} failures={}", (Object)this.getDiagnosticContext(), (Object)n2, (Object)Thread.currentThread().getName(), (Object)l, (Object)throwable);
            throw throwable;
        }
        int n = n2 + n3;
        if (byArray.length != n || (byArray.length & 7) != 0) {
            long l = NativeDeVisibilityScope.FINALIZE_INVARIANT_COUNT().incrementAndGet();
            NativeDeVisibilityScope.log().warn("[PacketSafety] payload finalization invariant failed: packet={} rawLength={} padding={} actualLength={} thread={} violations={}", (Object)this.getDiagnosticContext(), (Object)n2, (Object)n3, (Object)byArray.length, (Object)Thread.currentThread().getName(), (Object)l);
            throw new IllegalStateException("Invalid finalized packet length: " + byArray.length);
        }
        this._byteCache = byArray;
        return byArray;
    }

    public boolean isValidForWrite() {
        return true;
    }

    public void onInvalidForWrite() {
    }

    public final synchronized byte[] prepareForWrite() throws IOException {
        IllegalStateException illegalStateException = this._constructionFailure;
        if (illegalStateException != null) {
            throw illegalStateException;
        }
        byte[] byArray = this._preparedPayload;
        if (byArray != null) {
            return byArray;
        }
        byte[] byArray2 = this._byteCache;
        if (byArray2 != null) {
            this._preparedPayload = byArray2;
            return byArray2;
        }
        byte[] byArray3 = this.getContent();
        if (byArray3 != null) {
            this._preparedPayload = byArray3;
        }
        return byArray3;
    }

    public abstract byte[] getContent() throws IOException;

    public String getType() {
        return this.getClass().getSimpleName();
    }

    public String getDiagnosticContext() {
        return this.getType();
    }

    protected void debugPacket() {
        NativeDeVisibilityScope.debug(this);
    }
}

