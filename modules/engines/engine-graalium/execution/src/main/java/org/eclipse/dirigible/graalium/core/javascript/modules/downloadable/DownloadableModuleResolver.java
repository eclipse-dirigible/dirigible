/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.graalium.core.javascript.modules.downloadable;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * The Class DownloadableModuleResolver.
 */
public class DownloadableModuleResolver {

    /** The dependencies cache path. */
    private final Path dependenciesCachePath;

    /**
     * Instantiates a new downloadable module resolver.
     *
     * @param dependenciesCachePath the dependencies cache path
     */
    public DownloadableModuleResolver(Path dependenciesCachePath) {
        dependenciesCachePath.toFile()
                             .mkdirs();
        this.dependenciesCachePath = dependenciesCachePath;
    }

    /**
     * Resolve.
     *
     * @param uri the uri
     * @return the path
     */
    public Path resolve(URI uri) {
        Path maybeCachePath = tryGetCachePath(uri);
        return maybeCachePath != null ? maybeCachePath : downloadDependency(uri);
    }

    /**
     * Try get cache path.
     *
     * @param uri the uri
     * @return the path
     */
    private Path tryGetCachePath(URI uri) {
        String base64DependencyName = getBase64FromURI(uri);
        Path expectedDependencyPath = dependenciesCachePath.resolve(base64DependencyName);
        File expectedDependencyFile = expectedDependencyPath.toFile();

        if (expectedDependencyFile.exists()) {
            return expectedDependencyPath;
        }

        return null;
    }

    /**
     * Download dependency.
     *
     * @param uri the uri
     * @return the path
     */
    private Path downloadDependency(URI uri) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                                         .GET()
                                         .uri(uri)
                                         .build();

        try {
            BodyHandler<byte[]> responseBodyHandler = HttpResponse.BodyHandlers.ofByteArray();
            HttpResponse<byte[]> response = client.send(request, responseBodyHandler);
            Path dependencyFilePath = getDependencyFilePathForOutput(uri);
            byte[] downloadedBytes = response.body();
            Files.write(dependencyFilePath, downloadedBytes);
            return dependencyFilePath;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the dependency file path for output.
     *
     * @param dependencyUri the dependency uri
     * @return the dependency file path for output
     */
    private Path getDependencyFilePathForOutput(URI dependencyUri) {
        String base64DependencyName = getBase64FromURI(dependencyUri);
        return dependenciesCachePath.resolve(base64DependencyName);
    }

    /**
     * Gets the base 64 from URI.
     *
     * @param uri the uri
     * @return the base 64 from URI
     */
    private static String getBase64FromURI(URI uri) {
        Base64.Encoder encoder = Base64.getEncoder();
        byte[] bytes = uri.toString()
                          .getBytes(StandardCharsets.UTF_8);
        return encoder.encodeToString(bytes);
    }
}
