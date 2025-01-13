/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.graalium.core.javascript;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.eclipse.dirigible.graalium.core.javascript.modules.ModuleType;
import org.graalvm.polyglot.Source;

/**
 * The Class GraalJSSourceCreator.
 */
public class GraalJSSourceCreator {

    /** The js module type. */
    private final ModuleType jsModuleType;

    /**
     * Instantiates a new graal JS source creator.
     *
     * @param jsModuleType the js module type
     */
    public GraalJSSourceCreator(ModuleType jsModuleType) {
        this.jsModuleType = jsModuleType;
    }

    /**
     * Creates the source.
     *
     * @param source the source
     * @param fileName the file name
     * @return the source
     */
    public Source createSource(String source, String fileName) {
        Source.Builder sourceBuilder = Source.newBuilder("js", source, fileName);
        return createSource(sourceBuilder);
    }

    /**
     * Creates the internal source.
     *
     * @param source the source
     * @param fileName the file name
     * @return the source
     */
    public Source createInternalSource(String source, String fileName) {
        Source.Builder sourceBuilder = Source.newBuilder("js", source, fileName)
                                             .internal(true);
        return createSource(sourceBuilder);
    }

    /**
     * Creates the source.
     *
     * @param sourceFilePath the source file path
     * @return the source
     */
    public Source createSource(String sourceFilePath) {
        return createSource(Path.of(sourceFilePath));
    }

    /**
     * Creates the source.
     *
     * @param sourceFilePath the source file path
     * @return the source
     */
    public Source createSource(Path sourceFilePath) {
        File codeFile = sourceFilePath.toFile();
        Source.Builder sourceBuilder = Source.newBuilder("js", codeFile);
        return createSource(sourceBuilder);
    }

    /**
     * Creates the source.
     *
     * @param sourceBuilder the source builder
     * @return the source
     */
    private Source createSource(Source.Builder sourceBuilder) {
        try {
            // if (ModuleType.ESM.equals(jsModuleType)) {
            // }
            sourceBuilder.mimeType("application/javascript+module");

            return sourceBuilder.cached(false)
                                .encoding(StandardCharsets.UTF_8)
                                .build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
