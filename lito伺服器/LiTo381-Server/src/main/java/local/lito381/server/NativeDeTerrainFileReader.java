// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import java.io.Reader;
import local.lito381.server.NativeDeTerrainScope;

final class NativeDeTerrainFileReader
extends Reader {
    private final NativeDeTerrainScope.Context context = NativeDeTerrainScope.owner();
    private final Reader delegate;
    private boolean closed;

    NativeDeTerrainFileReader(String string) throws IOException {
        ++this.context.attempted;
        this.delegate = this.context.asset.open(string);
        this.context.readers.add(this);
        ++this.context.opened;
    }

    @Override
    public int read(char[] cArray, int n, int n2) throws IOException {
        return this.delegate.read(cArray, n, n2);
    }

    @Override
    public void close() throws IOException {
        if (this.closed) {
            return;
        }
        this.closed = true;
        try {
            this.delegate.close();
            ++this.context.closed;
        }
        finally {
            this.context.readers.remove(this);
        }
    }
}
