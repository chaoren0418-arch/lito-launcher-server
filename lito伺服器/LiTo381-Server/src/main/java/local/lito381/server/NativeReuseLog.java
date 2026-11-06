// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.lang.System.Logger.Level;

final class NativeReuseLog {
   public static NativeReuseLog.Logger getLogger(Class<?> var0) {
      return new NativeReuseLog.Log(System.getLogger(var0.getName()));
   }

   private NativeReuseLog() {
   }

   private record Log(System.Logger logger) implements NativeReuseLog.Logger {
      @Override
      public void error(String var1, Throwable var2) {
         this.logger.log(Level.ERROR, var1, var2);
      }

      @Override
      public void error(String var1, Object var2, Object var3) {
         this.emit(Level.ERROR, var1, var2, var3);
      }

      @Override
      public void warn(String var1, Object var2, Object var3) {
         this.emit(Level.WARNING, var1, var2, var3);
      }

      @Override
      public void warn(String var1, Object var2, Object var3, Object var4) {
         this.emit(Level.WARNING, var1, var2, var3, var4);
      }

      void emit(Level var1, String var2, Object... var3) {
         Throwable var4 = null;

         for (Object var8 : var3) {
            int var9 = var2.indexOf("{}");
            if (var9 >= 0) {
               var2 = var2.substring(0, var9) + var8 + var2.substring(var9 + 2);
            } else if (var8 instanceof Throwable var10) {
               var4 = var10;
            }
         }

         if (var4 == null) {
            this.logger.log(var1, var2);
         } else {
            this.logger.log(var1, var2, var4);
         }
      }
   }

   interface Logger {
      void error(String var1, Throwable var2);

      void error(String var1, Object var2, Object var3);

      void warn(String var1, Object var2, Object var3);

      void warn(String var1, Object var2, Object var3, Object var4);
   }
}
