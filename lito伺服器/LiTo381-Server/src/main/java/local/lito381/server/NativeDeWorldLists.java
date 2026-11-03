// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NativeDeWorldLists {
    public static <E> ArrayList<E> newArrayList() {
        return new ArrayList();
    }

    public static <E> CopyOnWriteArrayList<E> newCopyOnWriteArrayList() {
        return new CopyOnWriteArrayList();
    }

    public static <E> List<E> newConcurrentList() {
        return new CopyOnWriteArrayList();
    }
}
