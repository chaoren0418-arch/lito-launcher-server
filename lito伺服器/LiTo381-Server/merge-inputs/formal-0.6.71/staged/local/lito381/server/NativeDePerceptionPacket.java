// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDePerceptionScope;
import local.lito381.server.NativeDePerceptionState;

public abstract class NativeDePerceptionPacket
extends NativeDeDeletionPorts.Packet {
    protected final ByteArrayOutputStream _bao;
    private volatile byte[] _byteCache = null;
    private volatile byte[] _preparedPayload = null;
    private volatile IllegalStateException _constructionFailure = null;
    final NativeDePerceptionState subject = NativeDePerceptionScope.actor();

    public NativeDePerceptionPacket() {
        this._bao = new ByteArrayOutputStream();
    }

    private void assertPayloadMutable() {
        if (this._byteCache != null || this._preparedPayload != null || this._constructionFailure != null) {
            long violations = NativeDePerceptionScope.POST_FINALIZE_WRITE_COUNT().incrementAndGet();
            NativeDePerceptionScope._log().warn("[PacketSafety] rejected write after payload finalization: packet={} thread={} violations={}", (Object)this.getDiagnosticContext(), (Object)Thread.currentThread().getName(), (Object)violations);
            throw new IllegalStateException("Packet payload is immutable after getBytes(): " + this.getType());
        }
    }

    protected final synchronized void markConstructionFailed(String operation, Throwable failure) {
        if (this._constructionFailure != null) {
            return;
        }
        String detail = operation != null ? operation : "unspecified";
        long failures = NativeDePerceptionScope.CONSTRUCTION_FAILURE_COUNT().incrementAndGet();
        this._constructionFailure = new IllegalStateException("Packet construction failed: " + this.getType() + " operation=" + detail, failure);
        NativeDePerceptionScope._log().warn("[PacketSafety] packet construction failed; partial payload blocked: packet={} operation={} thread={} failures={}", (Object)this.getDiagnosticContext(), (Object)detail, (Object)Thread.currentThread().getName(), (Object)failures, (Object)failure);
    }

    protected final synchronized void writeD(int value) {
        this.assertPayloadMutable();
        this._bao.write(value & 0xFF);
        this._bao.write(value >> 8 & 0xFF);
        this._bao.write(value >> 16 & 0xFF);
        this._bao.write(value >> 24 & 0xFF);
    }

    protected final synchronized void writeH(int value) {
        this.assertPayloadMutable();
        this._bao.write(value & 0xFF);
        this._bao.write(value >> 8 & 0xFF);
    }

    protected final synchronized void writeC(int value) {
        this.assertPayloadMutable();
        this._bao.write(value & 0xFF);
    }

    protected final synchronized void writeP(int value) {
        this.assertPayloadMutable();
        this._bao.write(value);
    }

    protected final synchronized void writeS(String text) {
        this.assertPayloadMutable();
        try {
            if (text != null) {
                StringBuilder chtext = new StringBuilder(text);
                for (String bad : NativeDePerceptionScope.BAD_TEXT_LIST()) {
                    int index;
                    while ((index = chtext.indexOf(bad)) != -1) {
                        int endIndex = index + bad.length();
                        chtext.delete(index, endIndex);
                    }
                }
                this._bao.write(chtext.toString().getBytes(NativeDePerceptionScope.CLIENT_LANGUAGE_CODE()));
            }
        }
        catch (Exception e) {
            NativeDePerceptionScope._log().error(e.getLocalizedMessage(), e);
        }
        this._bao.write(0);
    }

    protected final synchronized void writeByte(byte[] text) {
        this.assertPayloadMutable();
        try {
            if (text != null) {
                this._bao.write(text);
            }
        }
        catch (Exception e) {
            NativeDePerceptionScope._log().error(e.getLocalizedMessage(), e);
        }
        this._bao.write(0);
    }

    protected final synchronized byte[] getBytes() {
        byte[] finalized;
        IllegalStateException constructionFailure = this._constructionFailure;
        if (constructionFailure != null) {
            throw constructionFailure;
        }
        byte[] cached = this._byteCache;
        if (cached != null) {
            return cached;
        }
        int rawLength = this._bao.size();
        int padding = 8 - (rawLength & 7) & 7;
        try {
            for (int i = 0; i < padding; ++i) {
                this._bao.write(0);
            }
            finalized = this._bao.toByteArray();
        }
        catch (Error | RuntimeException failure) {
            long failures = NativeDePerceptionScope.FINALIZE_FAILURE_COUNT().incrementAndGet();
            NativeDePerceptionScope._log().warn("[PacketSafety] payload finalization failed: packet={} rawLength={} thread={} failures={}", (Object)this.getDiagnosticContext(), (Object)rawLength, (Object)Thread.currentThread().getName(), (Object)failures, (Object)failure);
            throw failure;
        }
        int expectedLength = rawLength + padding;
        if (finalized.length != expectedLength || (finalized.length & 7) != 0) {
            long violations = NativeDePerceptionScope.FINALIZE_INVARIANT_COUNT().incrementAndGet();
            NativeDePerceptionScope._log().warn("[PacketSafety] payload finalization invariant failed: packet={} rawLength={} padding={} actualLength={} thread={} violations={}", (Object)this.getDiagnosticContext(), (Object)rawLength, (Object)padding, (Object)finalized.length, (Object)Thread.currentThread().getName(), (Object)violations);
            throw new IllegalStateException("Invalid finalized packet length: " + finalized.length);
        }
        this._byteCache = finalized;
        return finalized;
    }

    public boolean isValidForWrite() {
        return true;
    }

    public void onInvalidForWrite() {
    }

    public final synchronized byte[] prepareForWrite() throws IOException {
        IllegalStateException constructionFailure = this._constructionFailure;
        if (constructionFailure != null) {
            throw constructionFailure;
        }
        byte[] prepared = this._preparedPayload;
        if (prepared != null) {
            return prepared;
        }
        byte[] finalized = this._byteCache;
        if (finalized != null) {
            this._preparedPayload = finalized;
            return finalized;
        }
        byte[] payload = this.getContent();
        if (payload != null) {
            this._preparedPayload = payload;
        }
        return payload;
    }

    public abstract byte[] getContent() throws IOException;

    public String getType() {
        return this.getClass().getSimpleName();
    }

    public String getDiagnosticContext() {
        return this.getType();
    }

    protected void debugPacket() {
        this.subject.perception.debug(this);
    }
}
