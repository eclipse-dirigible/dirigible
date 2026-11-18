/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.webhooks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

/**
 * The SSRF guard: HTTPS only, and no address inside the deployment's network, for every address a
 * name resolves to.
 */
class WebhookUrlGuardTest {

    /** A resolver over a fixed table, so no test depends on DNS. */
    private static final Map<String, String[]> HOSTS = Map.of("hooks.example.com", new String[] {"93.184.216.34"}, "split.example.com",
            new String[] {"93.184.216.34", "10.1.2.3"}, "metadata.example.com", new String[] {"169.254.169.254"}, "localhost",
            new String[] {"127.0.0.1"}, "v6.example.com", new String[] {"2606:4700::6810:84e5"});

    private static Optional<String> check(String url, boolean allowLoopback) {
        return WebhookUrlGuard.problem(url, allowLoopback, WebhookUrlGuardTest::resolve);
    }

    private static InetAddress[] resolve(String host) throws UnknownHostException {
        String[] literals = HOSTS.get(host);
        if (literals == null) {
            // a literal address resolves to itself, as the system resolver does
            if (host.matches("[0-9.]+") || host.contains(":")) {
                return new InetAddress[] {InetAddress.getByName(host.replace("[", "")
                                                                    .replace("]", ""))};
            }
            throw new UnknownHostException(host);
        }
        InetAddress[] addresses = new InetAddress[literals.length];
        for (int i = 0; i < literals.length; i++) {
            addresses[i] = InetAddress.getByName(literals[i]);
        }
        return addresses;
    }

    @Test
    void acceptsAPublicHttpsUrl() {
        assertEquals(Optional.empty(), check("https://hooks.example.com/dirigible?x=1", false));
        assertEquals(Optional.empty(), check("https://v6.example.com/hook", false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://127.0.0.1/hook", "https://localhost/hook", "https://169.254.169.254/latest/meta-data",
            "https://metadata.example.com/", "https://10.0.0.5/", "https://172.16.4.4/", "https://192.168.1.1/", "https://100.64.0.1/",
            "https://0.0.0.0/", "https://[::1]/", "https://[fd00::1]/", "https://[fe80::1]/", "https://[::ffff:10.0.0.1]/",
            "https://split.example.com/"})
    void refusesAnInternalAddress(String url) {
        Optional<String> problem = check(url, false);
        assertTrue(problem.isPresent() && problem.get()
                                                 .startsWith("resolves to an internal address"),
                url + " -> " + problem);
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://hooks.example.com/", "ftp://hooks.example.com/", "hooks.example.com/hook", ""})
    void refusesAnythingButHttps(String url) {
        assertTrue(check(url, false).isPresent(), url);
    }

    @Test
    void refusesCredentialsInTheUrlAndAHostThatDoesNotResolve() {
        assertEquals(Optional.of("must not carry credentials"), check("https://user:pw@hooks.example.com/", false));
        assertEquals(Optional.of("names a host that does not resolve"), check("https://nowhere.example.com/", false));
    }

    @Test
    void theTestSwitchAllowsPlainHttpOnLoopbackOnly() {
        assertEquals(Optional.empty(), check("http://127.0.0.1:8080/hook", true));
        assertEquals(Optional.empty(), check("http://localhost:8080/hook", true));
        assertEquals(Optional.of("must be an https URL"), check("http://hooks.example.com/", true));
        assertTrue(check("http://169.254.169.254/", true).isPresent());
        assertTrue(check("https://10.0.0.5/", true).isPresent());
    }
}
