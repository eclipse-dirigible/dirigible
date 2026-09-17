/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the AWS SDK v2 inventory of the executable jar (#7397).
 *
 * <p>
 * The platform reaches the SDK through two independent chains - {@code api-s3} through
 * {@code s3-transfer-manager} and {@code api-qldb} through the QLDB driver - and Maven mediates
 * them per artifact. Left to that, the jar shipped {@code aws-json-protocol} and
 * {@code apache-client} from the 2.15.x line under a 2.54.x {@code sdk-core}: a mix on which no
 * client works, which fails as a {@code NoSuchMethodError}/{@code NoClassDefFoundError} naming no
 * version, and which makes a user project's own AWS service module undeclarable - every coordinate
 * of the provided-BOM is pruned from its resolution graph, so it links against whatever the
 * platform ships. The BOM the root pom imports is what keeps the line single; these tests are what
 * notices when a new dependency breaks it again.
 */
class AwsSdkInventoryIT {

    /** The group whose artifacts must all carry one version. */
    private static final String AWS_SDK_GROUP = "software.amazon.awssdk";

    /**
     * The artifacts exempt from the single-line rule. AWS discontinued QLDB and stopped publishing
     * {@code qldbsession}, so the AWS SDK BOM no longer manages it and no version of it exists on the
     * current line; {@code api-qldb} pins the last one AWS published. It is a leaf service client that
     * nothing else links against, which is what makes the exemption harmless. Adding to this set is a
     * deliberate act - any other straggler is the defect above.
     */
    private static final Set<String> OFF_LINE_BY_DESIGN = Set.of("qldbsession");

    /** An artifact that must be in the BOM, so a parse that silently matched nothing fails. */
    private static final String BOM_SENTINEL = "sdk-core";

    /** The provided-BOM the application embeds. */
    private static final String BOM_RESOURCE = "META-INF/dirigible-provided-bom.xml";

    /** The generated BOM's entry shape. */
    private static final Pattern BOM_ENTRY =
            Pattern.compile("<groupId>([^<]+)</groupId>\\s*<artifactId>([^<]+)</artifactId>\\s*" + "<version>([^<]+)</version>");

    /**
     * The SDK resolves its default synchronous HTTP client by service loading, and refuses when the
     * classpath offers more than one implementation.
     */
    private static final String SYNC_HTTP_SERVICE = "META-INF/services/software.amazon.awssdk.http.SdkHttpService";

    /** The asynchronous counterpart, resolved the same way. */
    private static final String ASYNC_HTTP_SERVICE = "META-INF/services/software.amazon.awssdk.http.async.SdkAsyncHttpService";

    @Test
    void every_aws_sdk_artifact_carries_the_same_version() throws IOException {
        TreeMap<String, String> versions = awsSdkVersions();

        assertTrue(versions.containsKey(BOM_SENTINEL),
                () -> "the provided-BOM must enumerate the AWS SDK - read " + versions.size() + " " + AWS_SDK_GROUP + " entries");

        TreeMap<String, String> online = new TreeMap<>(versions);
        online.keySet()
              .removeAll(OFF_LINE_BY_DESIGN);
        Set<String> lines = Set.copyOf(online.values());

        assertEquals(1, lines.size(), () -> "the executable jar must ship ONE AWS SDK v2 line, so a user project's own AWS service"
                + " module links against the same core the platform runs on; it ships " + lines + " - " + online);
    }

    @Test
    void only_one_http_client_implementation_is_on_the_classpath() throws IOException {
        List<String> sync = new ArrayList<>();
        List<String> async = new ArrayList<>();
        try (JarFile jar = new JarFile(ExecutableJar.path()
                                                    .toFile())) {
            for (String nested : nestedLibraries(jar)) {
                Set<String> entries = entryNames(jar, nested);
                if (entries.contains(SYNC_HTTP_SERVICE)) {
                    sync.add(nested);
                }
                if (entries.contains(ASYNC_HTTP_SERVICE)) {
                    async.add(nested);
                }
            }
        }
        assertEquals(1, sync.size(),
                () -> "the SDK refuses to choose between several synchronous HTTP implementations, so exactly one may ship: " + sync);
        assertEquals(1, async.size(),
                () -> "the SDK refuses to choose between several asynchronous HTTP implementations, so exactly one may ship: " + async);
    }

    /**
     * The AWS SDK entries of the embedded provided-BOM - the inventory the dependency resolver prunes
     * from a user project's graph, and the same list the jar's {@code BOOT-INF/lib} is assembled from.
     *
     * @return the version per artifactId
     * @throws IOException when the jar or the BOM cannot be read
     */
    private static TreeMap<String, String> awsSdkVersions() throws IOException {
        TreeMap<String, String> versions = new TreeMap<>();
        try (JarFile jar = new JarFile(ExecutableJar.path()
                                                    .toFile())) {
            ZipEntry bom = jar.getEntry(BOM_RESOURCE);
            assertNotNull(bom, "the provided-BOM must ship inside the artifact");
            String content;
            try (InputStream stream = jar.getInputStream(bom)) {
                content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
            Matcher matcher = BOM_ENTRY.matcher(content);
            while (matcher.find()) {
                if (AWS_SDK_GROUP.equals(matcher.group(1))) {
                    versions.put(matcher.group(2), matcher.group(3));
                }
            }
        }
        return versions;
    }

    /**
     * The nested library jars of the executable jar.
     *
     * @param jar the executable jar
     * @return the entry names, in jar order
     */
    private static List<String> nestedLibraries(JarFile jar) {
        return jar.stream()
                  .map(ZipEntry::getName)
                  .filter(name -> name.startsWith("BOOT-INF/lib/") && name.endsWith(".jar"))
                  .toList();
    }

    /**
     * The entry names of a nested jar. The nested jars are STORED, so they are read as a stream rather
     * than extracted.
     *
     * @param jar the executable jar
     * @param nested the nested jar's entry name
     * @return the nested jar's entry names
     * @throws IOException when the nested jar cannot be read
     */
    private static Set<String> entryNames(JarFile jar, String nested) throws IOException {
        byte[] content;
        try (InputStream stream = jar.getInputStream(jar.getEntry(nested))) {
            content = stream.readAllBytes();
        }
        Set<String> names = new HashSet<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(content))) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                names.add(entry.getName());
            }
        }
        return names;
    }

}
