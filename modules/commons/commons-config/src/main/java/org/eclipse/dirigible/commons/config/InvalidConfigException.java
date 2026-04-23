package org.eclipse.dirigible.commons.config;

public class InvalidConfigException extends RuntimeException {
    private final String configKey;

    public InvalidConfigException(String message, String configKey) {
        super(message);
        this.configKey = configKey;
    }

    public String getConfigKey() {
        return configKey;
    }
}
