package local.lito381.server;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Auto-registered safety net: skip a test class when its class-file constants
 * name local files that are not on this host.
 */
public final class LocalResourcesExtension implements BeforeAllCallback {
    @Override
    public void beforeAll(ExtensionContext context) {
        context.getTestClass().ifPresent(LocalResources::assumeFor);
    }
}
