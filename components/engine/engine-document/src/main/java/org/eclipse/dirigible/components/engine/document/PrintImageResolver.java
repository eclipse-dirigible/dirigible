/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.document;

import java.io.IOException;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Pattern;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.parsers.document.renderer.ImageResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Resolves a {@code .print} template's {@code <image src="...">} against the tenant's content
 * store, inlining the bytes as a {@code data:} URI.
 *
 * <p>
 * A source is one of three things, and the shape says which: a {@code data:} URI (the data already
 * carried the image) or any other scheme-bearing URI is handed to the renderer unchanged, and
 * everything else is a path in the tenant CMS. That single rule covers both images a printed
 * document needs: the issuer's own logo, uploaded once under {@code Templates/} and shared by every
 * document, and a file of the record itself — a {@code function: Attachment} row's
 * {@code StoragePath} is a CMS path, so a print reads it through the same {@code src}.
 *
 * <p>
 * Inlining rather than referencing is not an optimization: the renderer's output is a
 * self-contained stylesheet handed to FOP with no session, no credentials and no tenant scope, so
 * an image left as a CMS reference could only be fetched by opening that content to an
 * unauthenticated read. Resolution therefore happens here, while the caller's own tenant scope and
 * authorization still apply.
 *
 * <p>
 * Every failure is <b>soft</b>: a missing file, an oversized one, a document that is not an image
 * and an unreadable store all resolve to {@code null}, and the renderer then omits the image
 * entirely. A logo that cannot be read must not cost the invoice.
 */
@Component
class PrintImageResolver implements ImageResolver {

    private static final Logger logger = LoggerFactory.getLogger(PrintImageResolver.class);

    /**
     * A URI scheme — a letter followed by letters, digits and {@code + - .} up to the colon (RFC 3986).
     * Anchored and non-repeating, so it scans the source once.
     */
    private static final Pattern SCHEME = Pattern.compile("^[A-Za-z][A-Za-z0-9+.-]*:");

    /**
     * The media type a document must carry to be embedded. Matched in full rather than by prefix
     * because the type is interpolated into the data URI, and an ATTACHMENT's content type is whatever
     * the browser's multipart header claimed - so it is data, not a platform value.
     */
    private static final Pattern IMAGE_MEDIA_TYPE = Pattern.compile("^image/[A-Za-z0-9.+-]{1,64}$");

    private final CmsStore cmsStore;

    PrintImageResolver(CmsStore cmsStore) {
        this.cmsStore = cmsStore;
    }

    @Override
    public String resolve(String source) {
        String src = source.trim();
        if (SCHEME.matcher(src)
                  .find()) {
            // Already a URI - a data: URI the data carried inline, or an address FOP resolves itself.
            return src;
        }
        if (isTraversing(src)) {
            logger.warn("Print image path [{}] traverses out of the content store - it is skipped", LoggedPath.of(src));
            return null;
        }
        try {
            Optional<CmsStore.Content> content = cmsStore.readDocument(src, DirigibleConfig.PRINT_IMAGE_MAX_SIZE.getIntValue());
            if (content.isEmpty()) {
                logger.debug("Print image [{}] was not read - printing without it", LoggedPath.of(src));
                return null;
            }
            String mediaType = content.get()
                                      .mediaType();
            if (mediaType == null || !IMAGE_MEDIA_TYPE.matcher(mediaType)
                                                      .matches()) {
                logger.warn("Print image [{}] is [{}], not an image - it is skipped", LoggedPath.of(src), mediaType);
                return null;
            }
            return "data:" + mediaType + ";base64," + Base64.getEncoder()
                                                            .encodeToString(content.get()
                                                                                   .content());
        } catch (IOException e) {
            logger.warn("Failed to read the print image [{}] - printing without it", LoggedPath.of(src), e);
            return null;
        }
    }

    /** Whether any segment of the path is {@code ..} - the store's root is the tenant's boundary. */
    private static boolean isTraversing(String path) {
        for (String segment : path.split("/")) {
            if ("..".equals(segment)) {
                return true;
            }
        }
        return false;
    }
}
