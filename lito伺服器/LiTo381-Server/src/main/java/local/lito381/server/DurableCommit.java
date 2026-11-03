package local.lito381.server;

import java.io.IOException;

/** One durable sidecar commit executed before a transient transfer is published. */
@FunctionalInterface
interface DurableCommit {
    void commit() throws IOException;
}
