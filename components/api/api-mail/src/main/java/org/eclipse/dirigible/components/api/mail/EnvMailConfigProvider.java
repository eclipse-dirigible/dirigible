/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.mail;

import org.eclipse.dirigible.commons.config.Configuration;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * The Class EnvMailConfigProvider.
 */

@Component
public class EnvMailConfigProvider implements MailConfigurationProvider {

    /** The Constant MAIL_USER. */
    // Mail properties
    private static final String MAIL_USER = "mail.user";

    /** The Constant MAIL_PASSWORD. */
    private static final String MAIL_PASSWORD = "mail.password";

    /** The Constant MAIL_TRANSPORT_PROTOCOL. */
    private static final String MAIL_TRANSPORT_PROTOCOL = "mail.transport.protocol";

    /** The Constant MAIL_SMTPS_HOST. */
    // SMTPS properties
    private static final String MAIL_SMTPS_HOST = "mail.smtps.host";

    /** The Constant MAIL_SMTPS_PORT. */
    private static final String MAIL_SMTPS_PORT = "mail.smtps.port";

    /** The Constant MAIL_SMTPS_AUTH. */
    private static final String MAIL_SMTPS_AUTH = "mail.smtps.auth";

    /** The Constant MAIL_SMTP_HOST. */
    // SMTP properties
    private static final String MAIL_SMTP_HOST = "mail.smtp.host";

    /** The Constant MAIL_SMTP_PORT. */
    private static final String MAIL_SMTP_PORT = "mail.smtp.port";

    /** The Constant MAIL_SMTP_AUTH. */
    private static final String MAIL_SMTP_AUTH = "mail.smtp.auth";

    /** The Constant DIRIGIBLE_MAIL_USERNAME. */
    private static final String DIRIGIBLE_MAIL_USERNAME = "DIRIGIBLE_MAIL_USERNAME";

    /** The Constant DIRIGIBLE_MAIL_PASSWORD. */
    private static final String DIRIGIBLE_MAIL_PASSWORD = "DIRIGIBLE_MAIL_PASSWORD";

    /** The Constant DIRIGIBLE_MAIL_TRANSPORT_PROTOCOL. */
    private static final String DIRIGIBLE_MAIL_TRANSPORT_PROTOCOL = "DIRIGIBLE_MAIL_TRANSPORT_PROTOCOL";

    /** The Constant DIRIGIBLE_MAIL_SMTPS_HOST. */
    // SMTPS properties
    private static final String DIRIGIBLE_MAIL_SMTPS_HOST = "DIRIGIBLE_MAIL_SMTPS_HOST";

    /** The Constant DIRIGIBLE_MAIL_SMTPS_PORT. */
    private static final String DIRIGIBLE_MAIL_SMTPS_PORT = "DIRIGIBLE_MAIL_SMTPS_PORT";

    /** The Constant DIRIGIBLE_MAIL_SMTPS_AUTH. */
    private static final String DIRIGIBLE_MAIL_SMTPS_AUTH = "DIRIGIBLE_MAIL_SMTPS_AUTH";

    /** The Constant DIRIGIBLE_MAIL_SMTP_HOST. */
    // SMTP properties
    private static final String DIRIGIBLE_MAIL_SMTP_HOST = "DIRIGIBLE_MAIL_SMTP_HOST";

    /** The Constant DIRIGIBLE_MAIL_SMTP_PORT. */
    private static final String DIRIGIBLE_MAIL_SMTP_PORT = "DIRIGIBLE_MAIL_SMTP_PORT";

    /** The Constant DIRIGIBLE_MAIL_SMTP_AUTH. */
    private static final String DIRIGIBLE_MAIL_SMTP_AUTH = "DIRIGIBLE_MAIL_SMTP_AUTH";

    /** The Constant DEFAULT_MAIL_TRANSPORT_PROTOCOL. */
    // Default values
    private static final String DEFAULT_MAIL_TRANSPORT_PROTOCOL = "smtps";

    /** The Constant PROVIDER_NAME. */
    private static final String PROVIDER_NAME = "environment";

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
        return PROVIDER_NAME;
    }

    /**
     * Gets the properties.
     *
     * @return the properties
     */
    public Properties getProperties() {
        Properties properties = new Properties();

        addValue(properties, MAIL_USER, DIRIGIBLE_MAIL_USERNAME);
        addValue(properties, MAIL_PASSWORD, DIRIGIBLE_MAIL_PASSWORD);

        addValue(properties, MAIL_TRANSPORT_PROTOCOL, DIRIGIBLE_MAIL_TRANSPORT_PROTOCOL, DEFAULT_MAIL_TRANSPORT_PROTOCOL);

        addValue(properties, MAIL_SMTPS_HOST, DIRIGIBLE_MAIL_SMTPS_HOST);
        addValue(properties, MAIL_SMTPS_PORT, DIRIGIBLE_MAIL_SMTPS_PORT);
        addValue(properties, MAIL_SMTPS_AUTH, DIRIGIBLE_MAIL_SMTPS_AUTH);

        addValue(properties, MAIL_SMTP_HOST, DIRIGIBLE_MAIL_SMTP_HOST);
        addValue(properties, MAIL_SMTP_PORT, DIRIGIBLE_MAIL_SMTP_PORT);
        addValue(properties, MAIL_SMTP_AUTH, DIRIGIBLE_MAIL_SMTP_AUTH);

        return properties;
    }

    /**
     * Adds the value.
     *
     * @param properties the properties
     * @param key the key
     * @param envKey the env key
     */
    private void addValue(Properties properties, String key, String envKey) {
        addValue(properties, key, envKey, null);
    }

    /**
     * Adds the value.
     *
     * @param properties the properties
     * @param key the key
     * @param envKey the env key
     * @param defaultValue the default value
     */
    private void addValue(Properties properties, String key, String envKey, String defaultValue) {
        String value = Configuration.get(envKey);
        if (value != null) {
            properties.put(key, value);
        } else if (defaultValue != null) {
            properties.put(key, defaultValue);
        }
    }
}
