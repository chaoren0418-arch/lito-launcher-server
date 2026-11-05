// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import local.lito381.server.NativeDeDeletionPorts.Packet;

public abstract class NativeDePerceptionPacket extends Packet {
   protected final ByteArrayOutputStream _bao;
   private volatile byte[] _byteCache;
   private volatile byte[] _preparedPayload;
   private volatile IllegalStateException _constructionFailure;
   final NativeDePerceptionState subject = NativeDePerceptionScope.actor();

   public NativeDePerceptionPacket() {
      this._bao = new ByteArrayOutputStream();
      this._byteCache = null;
      this._preparedPayload = null;
      this._constructionFailure = null;
   }

   private void assertPayloadMutable() {
      if (this._byteCache != null || this._preparedPayload != null || this._constructionFailure != null) {
         long violations = NativeDePerceptionScope.POST_FINALIZE_WRITE_COUNT().incrementAndGet();
         NativeDePerceptionScope._log()
            .warn(
               "[PacketSafety] rejected write after payload finalization: packet={} thread={} violations={}",
               this.getDiagnosticContext(),
               Thread.currentThread().getName(),
               violations
            );
         throw new IllegalStateException("Packet payload is immutable after getBytes(): " + this.getType());
      }
   }

   protected final synchronized void markConstructionFailed(String operation, Throwable failure) {
      if (this._constructionFailure == null) {
         String detail = operation != null ? operation : "unspecified";
         long failures = NativeDePerceptionScope.CONSTRUCTION_FAILURE_COUNT().incrementAndGet();
         this._constructionFailure = new IllegalStateException("Packet construction failed: " + this.getType() + " operation=" + detail, failure);
         NativeDePerceptionScope._log()
            .warn(
               "[PacketSafety] packet construction failed; partial payload blocked: packet={} operation={} thread={} failures={}",
               this.getDiagnosticContext(),
               detail,
               Thread.currentThread().getName(),
               failures,
               failure
            );
      }
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

            int index;
            for (String bad : NativeDePerceptionScope.BAD_TEXT_LIST()) {
               while ((index = chtext.indexOf(bad)) != -1) {
                  int endIndex = index + bad.length();
                  chtext.delete(index, endIndex);
               }
            }

            this._bao.write(chtext.toString().getBytes(NativeDePerceptionScope.CLIENT_LANGUAGE_CODE()));
         }
      } catch (Exception var7) {
         NativeDePerceptionScope._log().error(var7.getLocalizedMessage(), var7);
      }

      this._bao.write(0);
   }

   protected final synchronized void writeByte(byte[] text) {
      this.assertPayloadMutable();

      try {
         if (text != null) {
            this._bao.write(text);
         }
      } catch (Exception var3) {
         NativeDePerceptionScope._log().error(var3.getLocalizedMessage(), var3);
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
        catch (RuntimeException | Error failure) {
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
      } else {
         byte[] prepared = this._preparedPayload;
         if (prepared != null) {
            return prepared;
         } else {
            byte[] finalized = this._byteCache;
            if (finalized != null) {
               this._preparedPayload = finalized;
               return finalized;
            } else {
               byte[] payload = this.getContent();
               if (payload != null) {
                  this._preparedPayload = payload;
               }

               return payload;
            }
         }
      }
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
