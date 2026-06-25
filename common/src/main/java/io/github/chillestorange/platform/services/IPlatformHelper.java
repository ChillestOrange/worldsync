package io.github.chillestorange.platform.services;

import java.nio.file.Path;

public interface IPlatformHelper {

    ModPlatform getPlatform();

    Path getConfigDirectory();
}
