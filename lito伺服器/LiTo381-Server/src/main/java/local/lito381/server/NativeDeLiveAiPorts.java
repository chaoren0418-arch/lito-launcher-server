// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import local.lito381.server.NativeDeCharacterFields;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeInstancePorts;
import local.lito381.server.NativeDeWorldLog;
import local.lito381.server.NativeDeWorldMap;

final class NativeDeLiveAiPorts {
    private NativeDeLiveAiPorts() {
    }

    record Config(boolean wakeupEnable, int wakeupInterval, int wakeupDuration) {
        static Config load() {
            Config config;
            block9: {
                InputStream inputStream = NativeDeLiveAiPorts.class.getResourceAsStream("/combat/de-ai-controls.properties");
                try {
                    if (inputStream == null) {
                        throw new IllegalStateException("De AI controls absent");
                    }
                    Properties properties = new Properties();
                    properties.load(inputStream);
                    config = new Config(Boolean.parseBoolean(properties.getProperty("DeWakeupEnable", "true")), Integer.parseInt(properties.getProperty("DeWakeupInterval", "60")), Integer.parseInt(properties.getProperty("DeWakeupDuration", "5")));
                    if (inputStream == null) break block9;
                }
                catch (Throwable throwable) {
                    try {
                        if (inputStream != null) {
                            try {
                                inputStream.close();
                            }
                            catch (Throwable throwable2) {
                                throwable.addSuppressed(throwable2);
                            }
                        }
                        throw throwable;
                    }
                    catch (IOException iOException) {
                        throw new IllegalStateException(iOException);
                    }
                }
                inputStream.close();
            }
            return config;
        }
    }

    static interface Environment {
        public boolean passable(NativeDeWorldMap var1, int var2, int var3, NativeDeCharacterFields var4);

        public void update(NativeDeDeletionPorts.Viewer var1);

        public NativeDeWorldLog log();
    }

    static interface Doll {
        public void setNpcMoveSpeed();
    }

    static interface Fishing
    extends NativeDeInstancePorts.Fishing {
        public int execute();
    }

    static interface Combat
    extends NativeDeInstancePorts.Combat {
        public int execute();

        public void executeBackground();

        public boolean hasTarget();
    }
}
