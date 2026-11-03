package local.lito381.server;

import java.util.ArrayList;
import java.util.List;

/** Exact cell enumeration used by the retained classic World.createLineMap. */
final class BattleStraightLine {
    private BattleStraightLine() {}

    static List<ArenaMapCatalog.Point> points(int x0, int y0, int x1, int y1) {
        int x = x0;
        int y = y0;
        int sx = x1 > x0 ? 1 : -1;
        int dx = Math.abs(x1 - x0);
        int sy = y1 > y0 ? 1 : -1;
        int dy = Math.abs(y1 - y0);
        ArrayList<ArenaMapCatalog.Point> result = new ArrayList<>(Math.max(dx, dy) + 1);

        if (dx >= dy) {
            int error = -dx;
            for (int index = 0; index <= dx; index++) {
                result.add(new ArenaMapCatalog.Point(x, y));
                x += sx;
                error += dy << 1;
                if (error >= 0) {
                    y += sy;
                    error -= dx << 1;
                }
            }
        } else {
            int error = -dy;
            for (int index = 0; index <= dy; index++) {
                result.add(new ArenaMapCatalog.Point(x, y));
                y += sy;
                error += dx << 1;
                if (error >= 0) {
                    x += sx;
                    error -= dy << 1;
                }
            }
        }
        return List.copyOf(result);
    }

    static boolean contains(List<ArenaMapCatalog.Point> points, int x, int y) {
        return points.contains(new ArenaMapCatalog.Point(x, y));
    }
}
