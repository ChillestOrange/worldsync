package io.github.chillestorange;

import java.nio.file.Path;
import java.util.ServiceLoader;

/**
 * Static facade over the platform-specific {@link IPlatform} implementation.
 *
 * <p>The correct implementation is resolved once at class-load time via the JDK
 * {@link ServiceLoader}, using the classloader that loaded this class so that
 * the service file in the platform jar is always visible.
 *
 * <p>Usage in shared code:
 * <pre>{@code
 *   Path cfg = Platform.getConfigDir().resolve("worldsync");
 *   Path world = Platform.getGameDir().resolve("saves").resolve(name);
 * }</pre>
 */
public final class Platform {

    private static final IPlatform IMPL = ServiceLoader
            .load(IPlatform.class, Platform.class.getClassLoader())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                    "[WorldSync] No IPlatform service provider found on the classpath. "
                            + "Ensure the platform jar contains META-INF/services/"
                            + IPlatform.class.getName()));

    private Platform() {}

    public static Path getConfigDir() { return IMPL.getConfigDir(); }
    public static Path getGameDir()   { return IMPL.getGameDir();   }
}