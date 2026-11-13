/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.share;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The public end of a share link (issue #8613): {@code GET /public/share/<token>}, answered without
 * a user. It is the only route this feature mounts under the public prefix, and it does one thing -
 * stream the one stored file the token was created for.
 *
 * <p>
 * It is reachable by anybody, so it gives nothing away: an unknown, expired or revoked token, a
 * token of another tenant, and a link whose file is gone are one and the same 404 with no body. It
 * reads no session and creates none, starts nothing, and the file it serves was fixed when the link
 * was created - there is no parameter that selects another.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_PUBLIC + "share")
class ShareEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShareEndpoint.class);

    private static final String FALLBACK_FILE_NAME = "document";

    private final ShareLinkService service;

    private final ShareRateLimiter rateLimiter;

    ShareEndpoint(ShareLinkService service, ShareRateLimiter rateLimiter) {
        this.service = service;
        this.rateLimiter = rateLimiter;
    }

    /**
     * Serves the document a token was created for.
     *
     * @param token the link's token
     * @param request the request, read for the remote address and the user agent only
     * @return the stored file, 404 when the token does not answer, 429 when the caller is throttled
     */
    @GetMapping("/{token}")
    ResponseEntity<byte[]> open(@PathVariable("token") String token, HttpServletRequest request) {
        String address = request.getRemoteAddr();
        // Hashed before anything looks at it: the limiter keys on a bounded value whatever was sent.
        long wait = rateLimiter.acquire(address, ShareTokens.hash(token));
        if (wait > 0) {
            return hardened(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)).header(HttpHeaders.RETRY_AFTER, Long.toString(wait))
                                                                                .build();
        }
        Optional<ShareLinkService.Opened> opened = service.open(token, address, request.getHeader(HttpHeaders.USER_AGENT));
        if (opened.isEmpty()) {
            rateLimiter.miss(address);
            return hardened(ResponseEntity.status(HttpStatus.NOT_FOUND)).build();
        }
        ShareLink link = opened.get()
                               .link();
        String fileName = link.fileName() == null || link.fileName()
                                                         .isBlank() ? FALLBACK_FILE_NAME : link.fileName();
        MediaType mediaType = mediaType(link.contentType());
        // Only a PDF is shown in the reader's browser. Anything else a caller of the SDK may have
        // linked is handed over as a download: a file rendered inline is rendered on this origin.
        ContentDisposition.Builder disposition =
                MediaType.APPLICATION_PDF.equalsTypeAndSubtype(mediaType) ? ContentDisposition.inline() : ContentDisposition.attachment();
        return hardened(ResponseEntity.ok()).contentType(mediaType)
                                            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.filename(fileName, StandardCharsets.UTF_8)
                                                                                                .build()
                                                                                                .toString())
                                            .body(opened.get()
                                                        .content());
    }

    /**
     * The headers every answer of this route carries - a hit, a miss and a throttle alike: the document
     * is not for search engines, not for a shared cache, and the address that opens it must not leak to
     * wherever the reader goes next.
     */
    private static ResponseEntity.BodyBuilder hardened(ResponseEntity.BodyBuilder response) {
        return response.cacheControl(CacheControl.noStore()
                                                 .cachePrivate())
                       .header("X-Robots-Tag", "noindex, nofollow")
                       .header("Referrer-Policy", "no-referrer")
                       .header("X-Content-Type-Options", "nosniff");
    }

    private static MediaType mediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException e) {
            LOGGER.debug("A shared file carries the unreadable content type [{}]", contentType, e);
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
