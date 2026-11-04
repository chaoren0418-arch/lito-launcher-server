// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

final class NativeReuseLog {
    public static Logger getLogger(Class<?> clazz) {
        return new Log(System.getLogger(clazz.getName()));
    }

    private NativeReuseLog() {
    }

    private record Log(System.Logger logger) implements Logger
    {
        @Override
        public void error(String string, Throwable throwable) {
            this.logger.log(System.Logger.Level.ERROR, string, throwable);
        }

        @Override
        public void error(String string, Object object, Object object2) {
            this.emit(System.Logger.Level.ERROR, string, object, object2);
        }

        @Override
        public void warn(String string, Object object, Object object2) {
            this.emit(System.Logger.Level.WARNING, string, object, object2);
        }

        @Override
        public void warn(String string, Object object, Object object2, Object object3) {
            this.emit(System.Logger.Level.WARNING, string, object, object2, object3);
        }

        void emit(System.Logger.Level level, String object, Object ... objectArray) {
            Throwable throwable = null;
            for (Object object2 : objectArray) {
                Throwable throwable2;
                int n = ((String)object).indexOf("{}");
                if (n >= 0) {
                    object = ((String)object).substring(0, n) + String.valueOf(object2) + ((String)object).substring(n + 2);
                    continue;
                }
                if (!(object2 instanceof Throwable)) continue;
                throwable = throwable2 = (Throwable)object2;
            }
            if (throwable == null) {
                this.logger.log(level, (String)object);
            } else {
                this.logger.log(level, (String)object, throwable);
            }
        }
    }

    static interface Logger {
        public void error(String var1, Throwable var2);

        public void error(String var1, Object var2, Object var3);

        public void warn(String var1, Object var2, Object var3);

        public void warn(String var1, Object var2, Object var3, Object var4);
    }
}
