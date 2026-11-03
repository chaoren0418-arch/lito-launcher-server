// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

final class NativeDeWorldControls {
    NativeDeWorldControls() {
    }

    static int parse(Properties properties) {
        return Integer.parseInt(properties.getProperty("PcRecognizeRange", "13"));
    }

    static int load() {
        int n;
        block15: {
            Properties properties = new Properties();
            InputStream inputStream = NativeDeWorldControls.class.getClassLoader().getResourceAsStream("combat/de-world-controls.properties");
            try {
                if (inputStream == null) {
                    throw new IOException("World control resource absent");
                }
                try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);){
                    properties.load(inputStreamReader);
                }
                n = NativeDeWorldControls.parse(properties);
                if (inputStream == null) break block15;
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
                    throw new IllegalStateException("World controls unavailable", iOException);
                }
            }
            inputStream.close();
        }
        return n;
    }
}
