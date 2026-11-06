// DECOMPILED from base-0.6.71.jar, TODO review
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

final class NativeDeCharacterControls {
    static final String RESOURCE = "combat/de-character-controls.properties";
    private static volatile Integer current;

    private NativeDeCharacterControls() {
    }

    static int parse(Properties properties) {
        return Integer.parseInt(properties.getProperty("ClientLogin", "1"));
    }

    static int read(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new FileNotFoundException(RESOURCE);
        }
        Properties properties = new Properties();
        try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);){
            properties.load(inputStreamReader);
        }
        return NativeDeCharacterControls.parse(properties);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static int clientType() throws IOException {
        Integer n = current;
        if (n != null) {
            return n;
        }
        synchronized (NativeDeCharacterControls.class) {
            if (current == null) {
                current = NativeDeCharacterControls.read(NativeDeCharacterControls.class.getClassLoader().getResourceAsStream(RESOURCE));
            }
            return current;
        }
    }
}
