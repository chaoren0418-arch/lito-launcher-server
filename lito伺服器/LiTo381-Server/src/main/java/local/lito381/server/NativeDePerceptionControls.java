// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import local.lito381.server.NativeDePerceptionBadText;
import local.lito381.server.NativeDeWorldLog;

final class NativeDePerceptionControls {
    private static final NativeDeWorldLog LOG = (string, string2, objectArray) -> System.getLogger(NativeDePerceptionControls.class.getName()).log(string.equals("error") ? System.Logger.Level.ERROR : (string.equals("warn") ? System.Logger.Level.WARNING : System.Logger.Level.INFO), string2 + " " + Arrays.toString(objectArray));
    private static final Data DATA = NativeDePerceptionControls.load();

    NativeDePerceptionControls() {
    }

    static NativeDeWorldLog log() {
        return LOG;
    }

    static Data current() {
        return DATA;
    }

    static String language(Properties properties) {
        return (new String[]{"UTF8", "EUCKR", "UTF8", "BIG5", "SJIS", "GBK"})[Integer.parseInt(properties.getProperty("ClientLanguage", "3"))];
    }

    private static Data load() {
        Object object;
        Properties properties = new Properties();
        try {
            object = NativeDePerceptionControls.class.getClassLoader().getResourceAsStream("combat/de-perception-controls.properties");
            try {
                if (object == null) {
                    throw new IOException("perception controls absent");
                }
                properties.load(new InputStreamReader((InputStream)object, StandardCharsets.UTF_8));
            }
            finally {
                if (object != null) {
                    ((InputStream)object).close();
                }
            }
        }
        catch (IOException iOException) {
            throw new IllegalStateException(iOException);
        }
        object = new ArrayList();
        NativeDePerceptionBadText.load("combat/de-perception-badtext.txt", (ArrayList)object, false);
        return new Data(NativeDePerceptionControls.language(properties), (ArrayList<String>)object);
    }

    record Data(String language, ArrayList<String> badText) {
    }
}
