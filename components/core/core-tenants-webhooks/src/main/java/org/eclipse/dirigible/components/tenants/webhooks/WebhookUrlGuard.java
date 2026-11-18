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

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Optional;

import org.eclipse.dirigible.commons.config.DirigibleConfig;

/**
 * Decides whether a webhook URL may be called: HTTPS only, and never an address inside the
 * deployment's own network (SSRF). Asked at registration and again before every attempt, because a
 * name can be pointed elsewhere after it was registered.
 *
 * <p>
 * Refused: a loopback, link-local (the cloud metadata service lives at {@code 169.254.169.254}),
 * private (RFC 1918, IPv6 unique-local), shared (RFC 6598, {@code 100.64.0.0/10}), unspecified or
 * multicast address - checked for every address the name resolves to, so one public record beside a
 * private one does not pass. An IPv4-mapped IPv6 address is judged as its IPv4 address.
 *
 * <p>
 * The guard resolves the name itself and the HTTP client resolves it again; a name that answers
 * differently within that window is not caught here. The connection is not pinned to the checked
 * address because that would defeat the TLS host-name check.
 *
 * <p>
 * {@link DirigibleConfig#TENANT_WEBHOOKS_ALLOW_LOOPBACK_FOR_TESTS} lets a test receive on a plain
 * HTTP loopback URL; link-local and private addresses stay refused with it.
 */
final class WebhookUrlGuard {

    /** Resolves a host name. */
    @FunctionalInterface
    interface Resolver {

        /**
         * Resolves a host.
         *
         * @param host the host name or literal
         * @return every address it resolves to
         * @throws UnknownHostException when it resolves to nothing
         */
        InetAddress[] resolve(String host) throws UnknownHostException;
    }

    private WebhookUrlGuard() {}

    /**
     * Why the URL may not be called, under the configured switch and the system resolver.
     *
     * @param url the URL
     * @return the reason, empty when it may be called
     */
    static Optional<String> problem(String url) {
        return problem(url, DirigibleConfig.TENANT_WEBHOOKS_ALLOW_LOOPBACK_FOR_TESTS.getBooleanValue(), InetAddress::getAllByName);
    }

    /**
     * Why the URL may not be called.
     *
     * @param url the URL
     * @param allowLoopback whether the test switch is on
     * @param resolver the resolver
     * @return the reason, empty when it may be called
     */
    static Optional<String> problem(String url, boolean allowLoopback, Resolver resolver) {
        if (url == null || url.isBlank()) {
            return Optional.of("is empty");
        }
        URI uri;
        try {
            uri = new URI(url.trim());
        } catch (URISyntaxException ex) {
            return Optional.of("is not a valid URL");
        }
        String scheme = uri.getScheme() == null ? ""
                : uri.getScheme()
                     .toLowerCase(Locale.ROOT);
        if (!"https".equals(scheme) && !("http".equals(scheme) && allowLoopback)) {
            return Optional.of("must be an https URL");
        }
        if (uri.getRawUserInfo() != null) {
            return Optional.of("must not carry credentials");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            return Optional.of("names no host");
        }
        InetAddress[] addresses;
        try {
            addresses = resolver.resolve(host);
        } catch (UnknownHostException ex) {
            return Optional.of("names a host that does not resolve");
        }
        if (addresses == null || addresses.length == 0) {
            return Optional.of("names a host that does not resolve");
        }
        boolean allLoopback = true;
        for (InetAddress address : addresses) {
            InetAddress judged = unmapped(address);
            boolean loopback = judged.isLoopbackAddress();
            allLoopback &= loopback;
            if (loopback && allowLoopback) {
                continue;
            }
            if (isInternal(judged)) {
                return Optional.of("resolves to an internal address [" + judged.getHostAddress() + "]");
            }
        }
        if ("http".equals(scheme) && !allLoopback) {
            return Optional.of("must be an https URL");
        }
        return Optional.empty();
    }

    private static boolean isInternal(InetAddress address) {
        if (address.isLoopbackAddress() || address.isAnyLocalAddress() || address.isLinkLocalAddress() || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }
        byte[] bytes = address.getAddress();
        if (address instanceof Inet4Address) {
            int first = bytes[0] & 0xff;
            int second = bytes[1] & 0xff;
            // 0.0.0.0/8 "this network", 100.64.0.0/10 shared address space
            return first == 0 || (first == 100 && second >= 64 && second <= 127);
        }
        // fc00::/7 unique local
        return address instanceof Inet6Address && (bytes[0] & 0xfe) == 0xfc;
    }

    /** An IPv4-mapped IPv6 address ({@code ::ffff:a.b.c.d}) is judged as the IPv4 address it maps. */
    private static InetAddress unmapped(InetAddress address) {
        if (!(address instanceof Inet6Address)) {
            return address;
        }
        byte[] bytes = address.getAddress();
        for (int i = 0; i < 10; i++) {
            if (bytes[i] != 0) {
                return address;
            }
        }
        if ((bytes[10] & 0xff) != 0xff || (bytes[11] & 0xff) != 0xff) {
            return address;
        }
        try {
            return InetAddress.getByAddress(new byte[] {bytes[12], bytes[13], bytes[14], bytes[15]});
        } catch (UnknownHostException impossible) {
            throw new IllegalStateException("A four-byte address is always valid", impossible);
        }
    }
}
