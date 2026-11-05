// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

final class NativeDePerceptionControls {
   private static final NativeDeWorldLog LOG = (var0, var1, var2) -> System.getLogger(NativeDePerceptionControls.class.getName())
      .log(var0.equals("error") ? Level.ERROR : (var0.equals("warn") ? Level.WARNING : Level.INFO), var1 + " " + Arrays.toString(var2));
   private static final NativeDePerceptionControls.Data DATA = load();

   static NativeDeWorldLog log() {
      return LOG;
   }

   static NativeDePerceptionControls.Data current() {
      return DATA;
   }

   static String language(Properties var0) {
      return new String[]{"UTF8", "EUCKR", "UTF8", "BIG5", "SJIS", "GBK"}[Integer.parseInt(var0.getProperty("ClientLanguage", "3"))];
   }

   private static NativeDePerceptionControls.Data load() {
      Properties var0 = new Properties();

      try (InputStream var1 = NativeDePerceptionControls.class.getClassLoader().getResourceAsStream("combat/de-perception-controls.properties")) {
         if (var1 == null) {
            throw new IOException("perception controls absent");
         }

         var0.load(new InputStreamReader(var1, StandardCharsets.UTF_8));
      } catch (IOException var6) {
         throw new IllegalStateException(var6);
      }

      ArrayList var7 = new ArrayList();
      NativeDePerceptionBadText.load("combat/de-perception-badtext.txt", var7, false);
      return new NativeDePerceptionControls.Data(language(var0), var7);
   }

   record Data(String language, ArrayList<String> badText) {
   }
}
