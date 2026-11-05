// DECOMPILED from base-0.6.71.jar, TODO review
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
        Properties properties = new Properties();
        try {
            try (InputStream inputStream = NativeDeWorldControls.class.getClassLoader().getResourceAsStream("combat/de-world-controls.properties");) {
                if (inputStream == null) {
                    throw new IOException("World control resource absent");
                }
                try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);) {
                    properties.load(inputStreamReader);
                }
                return NativeDeWorldControls.parse(properties);
            }
        }
        catch (IOException iOException) {
            throw new IllegalStateException("World controls unavailable", iOException);
        }
    }
}
