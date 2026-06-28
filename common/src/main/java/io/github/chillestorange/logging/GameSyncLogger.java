package io.github.chillestorange.logging;

import io.github.chillestorange.GameSyncConstants;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GameSyncLogger {

    private static final String LOGGER_NAME = GameSyncConstants.MOD_ID;
    private static final Logger LOGGER = LoggerFactory.getLogger(LOGGER_NAME);

    private GameSyncLogger() {
    }

    public static void info(String format, Object... args) {
        LOGGER.info(format, args);
    }

    public static void warn(String msg) {
        LOGGER.warn(msg);
    }

    public static void error(String msg) {
        LOGGER.error(msg);
    }

    public static void error(String format, Object... args) {
        LOGGER.error(format, args);
    }

    public static void error(String msg, Throwable t) {
        LOGGER.error(msg, t);
    }

    public static void debug(String msg) {
        LOGGER.debug(msg);
    }

    public static void debug(String format, Object... args) {
        LOGGER.debug(format, args);
    }

    // Helper functions
    public static boolean isDebugEnabled() {
        return LOGGER.isDebugEnabled();
    }

    public static void setDebugEnabled(boolean enabled) {
        Configurator.setLevel(LOGGER_NAME, enabled ? Level.DEBUG : Level.INFO);
        LOGGER.info("Debug logging {}", enabled ? "enabled" : "disabled");
    }

}