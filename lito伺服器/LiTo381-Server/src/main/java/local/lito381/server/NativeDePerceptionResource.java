// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

final class NativeDePerceptionResource {
    NativeDePerceptionResource() {
    }

    static final class Stream
    extends FilterInputStream {
        Stream(File file) throws IOException {
            super(Stream.open(file));
        }

        private static InputStream open(File file) throws IOException {
            InputStream inputStream = NativeDePerceptionResource.class.getClassLoader().getResourceAsStream(file.path);
            if (inputStream == null) {
                throw new FileNotFoundException(file.path);
            }
            return inputStream;
        }
    }

    static final class File {
        final String path;

        File(String string) {
            this.path = string;
        }

        boolean exists() {
            return NativeDePerceptionResource.class.getClassLoader().getResource(this.path) != null;
        }

        String getName() {
            return this.path.substring(this.path.lastIndexOf(47) + 1);
        }
    }
}
