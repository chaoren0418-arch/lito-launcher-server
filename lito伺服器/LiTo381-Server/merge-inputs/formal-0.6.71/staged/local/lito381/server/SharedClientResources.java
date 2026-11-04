// DECOMPILED from base-0.6.71.jar, TODO review
/*
 * Decompiled with CFR 0.152.
 */
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
    private static final boolean SHARED = SharedClientResources.initializeProfile();
    private static final Map<Long, Long> DURATIONS = SharedClientResources.loadDurations();

    private static Map<Long, Long> loadDurations() {
        if (!SHARED) {
            return Map.of();
        }
        HashMap<Long, Long> hashMap = new HashMap<Long, Long>();
        try (InputStream inputStream = SharedClientResources.class.getResourceAsStream("/battle/spr-duration-tw13081901.csv");){
            if (inputStream == null) {
                throw new IllegalStateException("Common animation durations missing");
            }
            for (String string : new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
                if (string.isBlank() || string.startsWith("#")) continue;
                String[] stringArray = string.split(",");
                if (stringArray.length != 3) {
                    throw new IllegalStateException("Invalid common duration row");
                }
                long l = Long.parseLong(stringArray[0]);
                long l2 = Long.parseLong(stringArray[1]);
                long l3 = Long.parseLong(stringArray[2]);
                if (l < 0L || l >= 30285L || l2 < 0L || l2 >= 100L || l3 < 0L || hashMap.put(l * 100L + l2, l3) != null) {
                    throw new IllegalStateException("Invalid or duplicate common animation duration");
                }
            }
        }
        catch (IOException iOException) {
            throw new IllegalStateException(iOException);
        }
        if (hashMap.size() != 60303) {
            throw new IllegalStateException("Incomplete common animation durations");
        }
        return Map.copyOf(hashMap);
    }

    public static String animationResource(String string) {
        return SHARED ? "/battle/spr-action-tw13081901.csv" : string;
    }

    public static long duration(int n, int n2, long l) {
        if (!SHARED) {
            return l;
        }
        Long l2 = DURATIONS.get((long)n * 100L + (long)n2);
        if (l2 == null) {
            throw new IllegalArgumentException("Missing common duration: " + n + ":" + n2);
        }
        return l2;
    }

    private static boolean initializeProfile() {
        String string = System.getProperty("lito.resourceProfile", "lito-legacy");
        if (string.equals("lito-legacy")) {
            return false;
        }
        if (!string.equals(PROFILE)) {
            throw new IllegalArgumentException("Unknown client resource profile: " + string);
        }
        String string2 = System.getProperty("lito.commonCatalog");
        if (string2 == null || string2.isBlank()) {
            throw new IllegalArgumentException("Common catalog path is required");
        }
        try {
            byte[] byArray = Files.readAllBytes(Path.of(string2, new String[0]));
            String string3 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(byArray));
            if (!CATALOG_SHA256.equals(string3)) {
                throw new IllegalArgumentException("Common catalog SHA-256 mismatch");
            }
            ByteBuffer byteBuffer = ByteBuffer.wrap(byArray).order(ByteOrder.LITTLE_ENDIAN);
            if (byteBuffer.getInt() != 827479116 || byteBuffer.getInt() != 1 || byteBuffer.getInt() != 30000 || byteBuffer.getInt() != 285) {
                throw new IllegalArgumentException("Common catalog namespace mismatch");
            }
            return true;
        }
        catch (IOException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Cannot validate common catalog", exception);
        }
    }

    public static void initialize() {
        System.out.println("[LiTo] resource-profile=" + (SHARED ? PROFILE : "lito-legacy") + (SHARED ? " catalog-sha256=b64ed920792805c3380ad0e50b1ae089a8779b4adddec2fbf3780c2fa88a3c01 sprites=30285" : ""));
    }

    public static int effect(int n, int n2) {
        if (!SHARED) {
            return n;
        }
        if (n2 <= 0 || n2 >= 30285) {
            throw new IllegalArgumentException("Effect outside common catalog: " + n2);
        }
        return n2;
    }

    public static void main(String[] stringArray) {
        SharedClientResources.initialize();
    }
}
