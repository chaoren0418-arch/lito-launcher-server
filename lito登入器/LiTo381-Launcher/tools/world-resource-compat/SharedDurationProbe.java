import java.nio.charset.StandardCharsets;
import java.lang.reflect.Method;

/** Execute the existing runtime duration accessor without starting a server. */
public final class SharedDurationProbe {
    public static void main(String[] args) throws Exception {
        Class<?> shared = Class.forName("local.lito381.server.SharedClientResources");
        Method duration = shared.getDeclaredMethod("duration", int.class, int.class, long.class);
        byte[] raw;
        try (var input = shared.getResourceAsStream("/battle/spr-duration-tw13081901.csv")) {
            if (input == null) throw new AssertionError("missing duration table");
            raw = input.readAllBytes();
        }
        int count = 0;
        for (String row : new String(raw, StandardCharsets.UTF_8).split("\\R")) {
            if (row.isBlank() || row.startsWith("#")) continue;
            String[] fields = row.split(",");
            int sprite = Integer.parseInt(fields[0]), action = Integer.parseInt(fields[1]);
            long expected = Long.parseLong(fields[2]);
            long actual = (Long) duration.invoke(null, sprite, action, -987654321L);
            if (actual != expected) throw new AssertionError(row + " actual=" + actual);
            ++count;
        }
        if (count != 60303) throw new AssertionError("incomplete duration coverage " + count);
        System.out.println("PASS " + count + " actual shared duration lookups");
    }
}
