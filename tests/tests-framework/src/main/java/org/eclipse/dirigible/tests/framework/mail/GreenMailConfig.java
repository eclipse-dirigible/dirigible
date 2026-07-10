/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.framework.mail;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.annotation.PreDestroy;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.base.spring.BeanProvider;
import org.eclipse.dirigible.tests.framework.util.PortUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
public class GreenMailConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(GreenMailConfig.class);

    private static final String MAIL_USER = "mailUser";
    private static final String MAIL_PASSWORD = "mailPassword";

    /**
     * A healthy GreenMail confirms its socket within milliseconds; the startup timeout is only ever
     * consumed when the BIND itself failed - and waiting longer on a port someone else holds can never
     * succeed. That is why raising the timeout alone did not cure the CI flake: the port was picked
     * once per JVM at class-init, while the IT suite boots many Spring contexts in that JVM, each
     * starting a fresh GreenMail on the SAME port - one lingering socket (or any other service grabbing
     * the number in between) and every subsequent bind is dead on arrival. Keep the timeout moderate
     * and let the fresh-port retries below handle contention.
     */
    private static final long SERVER_STARTUP_TIMEOUT_MS = 20_000L;

    /** Bind attempts, each on a freshly probed random port. */
    private static final int START_ATTEMPTS = 5;

    @Bean
    GreenMail provideGreenMailServer() {
        IllegalStateException lastFailure = null;
        for (int attempt = 1; attempt <= START_ATTEMPTS; attempt++) {
            // Probe the port immediately before binding (per attempt, per context) - a port chosen at
            // class-init is stale by the time the Nth context boots.
            int port = PortUtil.getFreeRandomPort();
            ServerSetup serverSetup = new ServerSetup(port, "localhost", "smtp");
            serverSetup.setServerStartupTimeout(SERVER_STARTUP_TIMEOUT_MS);
            GreenMail greenMail = new GreenMail(serverSetup);
            try {
                greenMail.start();
            } catch (IllegalStateException e) {
                lastFailure = e;
                LOGGER.warn("GreenMail failed to start on port [{}] (attempt {}/{}) - retrying on a fresh port", port, attempt,
                        START_ATTEMPTS, e);
                stopQuietly(greenMail);
                continue;
            }
            greenMail.setUser(MAIL_USER, MAIL_PASSWORD);
            // Publish the ACTUAL port: the static pre-configuration in configureDirigibleEmailService
            // runs before this bean exists, and the mail service reads the config at send time - long
            // after this context booted - so the value set here is the one that counts.
            DirigibleConfig.MAIL_SMTP_PORT.setIntValue(port);
            LOGGER.info("GreenMail started on smtp:localhost:{}", port);
            return greenMail;
        }
        throw new IllegalStateException("GreenMail could not start after [" + START_ATTEMPTS + "] attempts on fresh random ports",
                lastFailure);
    }

    private static void stopQuietly(GreenMail greenMail) {
        try {
            greenMail.stop();
        } catch (RuntimeException e) {
            LOGGER.warn("Ignoring failure while stopping a GreenMail that did not start", e);
        }
    }

    @PreDestroy
    public void shutdownGreenMail() {
        Optional<GreenMail> greenMail = BeanProvider.getOptionalBean(GreenMail.class);
        greenMail.ifPresent((gm) -> {
            LOGGER.info("Shutting down green mail...");
            gm.stop();
        });
    }

    public static void configureDirigibleEmailService() {
        DirigibleConfig.MAIL_USERNAME.setStringValue(MAIL_USER);
        DirigibleConfig.MAIL_PASSWORD.setStringValue(MAIL_PASSWORD);
        DirigibleConfig.MAIL_TRANSPORT_PROTOCOL.setStringValue("smtp");
        DirigibleConfig.MAIL_SMTP_HOST.setStringValue("localhost");
        DirigibleConfig.MAIL_SMTP_AUTH.setBooleanValue(true);
        // The port is deliberately NOT set here: provideGreenMailServer publishes the actual bound
        // port when the context boots (a fresh one per attempt/context), before any test sends mail.
    }
}
