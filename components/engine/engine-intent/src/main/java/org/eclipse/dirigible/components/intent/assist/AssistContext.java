/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.assist;

import java.util.List;

/**
 * Everything the Java assistant knows about the file it is helping with, assembled server-side.
 *
 * <p>
 * The siblings are the whole point: a {@code custom/} class references the project's generated
 * entities and repositories, so a proposal can only be compiled - and only be written well - with
 * the rest of the project's sources in hand.
 *
 * @param project the workspace project name
 * @param path the target Java file's project-relative path
 * @param source the target file's current content, as the developer's buffer has it - empty for a
 *        port to a Java file that does not exist yet
 * @param intentYaml the project's {@code .intent} document, or {@code null} for a classic project
 * @param siblings every other Java source the proposal is compiled with
 * @param port the script being ported to the target, or {@code null} when the target is the file
 *        being worked on
 */
record AssistContext(String project, String path, String source, String intentYaml, List<ProjectSource> siblings, PortedScript port) {

    /**
     * A turn about a Java file.
     *
     * @param project the workspace project name
     * @param path the Java file's project-relative path
     * @param source the file's current content
     * @param intentYaml the project's {@code .intent} document, or {@code null}
     * @param siblings every other Java source the proposal is compiled with
     */
    AssistContext(String project, String path, String source, String intentYaml, List<ProjectSource> siblings) {
        this(project, path, source, intentYaml, siblings, null);
    }

    /**
     * A server-side script being ported to Java (#8051).
     *
     * @param path the script's project-relative path
     * @param language the script's language, as a Markdown fence names it
     * @param source the script, as the developer's buffer has it
     */
    record PortedScript(String path, String language, String source) {
    }
}
