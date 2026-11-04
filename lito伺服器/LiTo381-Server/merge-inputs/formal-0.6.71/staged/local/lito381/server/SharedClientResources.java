// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

public final class SharedClientResources {
   public static final String PROFILE = "tw13081901-v1";
   public static final String CATALOG_SHA256 = "b64ed920792805c3380ad0e50b1ae089a8779b4adddec2fbf3780c2fa88a3c01";
   private static final boolean SHARED = initializeProfile();
   private static final Map<Long, Long> DURATIONS = loadDurations();

   private static Map<Long, Long> loadDurations() {
      if (!SHARED) {
         return Map.of();
      } else {
         HashMap<Long, Long> var0 = new HashMap<>();

         try (InputStream var1 = SharedClientResources.class.getResourceAsStream("/battle/spr-duration-tw13081901.csv")) {
            if (var1 == null) {
               throw new IllegalStateException("Common animation durations missing");
            }

            for (String var5 : new String(var1.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
               if (!var5.isBlank() && !var5.startsWith("#")) {
                  String[] var6 = var5.split(",");
                  if (var6.length != 3) {
                     throw new IllegalStateException("Invalid common duration row");
                  }

                  long var7 = Long.parseLong(var6[0]);
                  long var9 = Long.parseLong(var6[1]);
                  long var11 = Long.parseLong(var6[2]);
                  if (var7 < 0L || var7 >= 30285L || var9 < 0L || var9 >= 100L || var11 < 0L || var0.put(var7 * 100L + var9, var11) != null) {
                     throw new IllegalStateException("Invalid or duplicate common animation duration");
                  }
               }
            }
         } catch (IOException var15) {
            throw new IllegalStateException(var15);
         }

         if (var0.size() != 60303) {
            throw new IllegalStateException("Incomplete common animation durations");
         } else {
            return Map.copyOf(var0);
         }
      }
   }

   public static String animationResource(String var0) {
      return SHARED ? "/battle/spr-action-tw13081901.csv" : var0;
   }

   public static long duration(int var0, int var1, long var2) {
      if (!SHARED) {
         return var2;
      } else {
         Long var4 = DURATIONS.get(var0 * 100L + var1);
         if (var4 == null) {
            throw new IllegalArgumentException("Missing common duration: " + var0 + ":" + var1);
         } else {
            return var4;
         }
      }
   }

   private static boolean initializeProfile() {
      String var0 = System.getProperty("lito.resourceProfile", "lito-legacy");
      if (var0.equals("lito-legacy")) {
         return false;
      } else if (!var0.equals("tw13081901-v1")) {
         throw new IllegalArgumentException("Unknown client resource profile: " + var0);
      } else {
         String var1 = System.getProperty("lito.commonCatalog");
         if (var1 != null && !var1.isBlank()) {
            try {
               byte[] var2 = Files.readAllBytes(Path.of(var1));
               String var3 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(var2));
               if (!"b64ed920792805c3380ad0e50b1ae089a8779b4adddec2fbf3780c2fa88a3c01".equals(var3)) {
                  throw new IllegalArgumentException("Common catalog SHA-256 mismatch");
               } else {
                  ByteBuffer var4 = ByteBuffer.wrap(var2).order(ByteOrder.LITTLE_ENDIAN);
                  if (var4.getInt() == 827479116 && var4.getInt() == 1 && var4.getInt() == 30000 && var4.getInt() == 285) {
                     return true;
                  } else {
                     throw new IllegalArgumentException("Common catalog namespace mismatch");
                  }
               }
            } catch (NoSuchAlgorithmException | IOException var5) {
               throw new IllegalStateException("Cannot validate common catalog", var5);
            }
         } else {
            throw new IllegalArgumentException("Common catalog path is required");
         }
      }
   }

   public static void initialize() {
      System.out
         .println(
            "[LiTo] resource-profile="
               + (SHARED ? "tw13081901-v1" : "lito-legacy")
               + (SHARED ? " catalog-sha256=b64ed920792805c3380ad0e50b1ae089a8779b4adddec2fbf3780c2fa88a3c01 sprites=30285" : "")
         );
   }

   public static int effect(int var0, int var1) {
      if (!SHARED) {
         return var0;
      }
      if (var1 <= 0 || var1 >= 30285) {
         throw new IllegalArgumentException("Effect outside common catalog: " + var1);
      }
      return var1;
   }

   public static void main(String[] var0) {
      initialize();
   }
}
