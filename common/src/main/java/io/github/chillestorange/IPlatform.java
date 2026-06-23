package io.github.chillestorange;

import java.nio.file.Path;

/**
 * Loader-agnostic abstraction for platform-specific environment paths.
 *
 * <p>Each platform module (fabric, neoforge) provides exactly one implementation
 * of this interface, registered as a JDK {@link java.util.ServiceLoader} service
 * in {@code META-INF/services/io.github.chillestorange.IPlatform}.
 */
public interface IPlatform {

    /** Returns the directory where mod configuration files should be stored. */
    Path getConfigDir();

    /** Returns the root game directory (the {@code .minecraft} folder). */
    Path getGameDir();
}