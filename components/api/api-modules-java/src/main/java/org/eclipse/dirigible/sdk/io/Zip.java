/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.eclipse.dirigible.components.api.io.ZipFacade;

/**
 * ZIP archive creation and extraction. Two styles are supported:
 *
 * <ul>
 * <li>The folder-level shortcuts {@link #importZip(String, String)} (extract everything under a
 * directory) and {@link #exportZip(String, String)} (zip a folder onto disk).</li>
 * <li>The streaming form via {@link ZipInputStream} / {@link ZipOutputStream} when you want
 * entry-by-entry control — useful for producing a download on the fly, scanning archives without
 * writing intermediate files, or filtering entries during extraction.</li>
 * </ul>
 *
 * For end-to-end "zip this folder, send it" workflows the shortcuts are enough; reach for the
 * streaming overloads only when you need entry-level control.
 */
public final class Zip {

    private Zip() {}

    public static void importZip(String zipPath, String targetPath) {
        ZipFacade.importZip(zipPath, targetPath);
    }

    public static void exportZip(String folderPath, String zipPath) {
        ZipFacade.exportZip(folderPath, zipPath);
    }

    public static ZipInputStream createZipInputStream(InputStream in) throws IOException {
        return ZipFacade.createZipInputStream(in);
    }

    public static ZipOutputStream createZipOutputStream(OutputStream out) throws IOException {
        return ZipFacade.createZipOutputStream(out);
    }

    public static ZipEntry createZipEntry(String name) throws IOException {
        return ZipFacade.createZipEntry(name);
    }

    public static void write(ZipOutputStream output, byte[] bytes) throws IOException {
        ZipFacade.write(output, bytes);
    }

    public static void write(ZipOutputStream output, String data) throws IOException {
        ZipFacade.write(output, data);
    }

    public static void writeNative(ZipOutputStream output, byte[] data) throws IOException {
        ZipFacade.writeNative(output, data);
    }

    public static void writeText(ZipOutputStream output, String text) throws IOException {
        ZipFacade.writeText(output, text);
    }

    public static String read(ZipInputStream input) throws IOException {
        return ZipFacade.read(input);
    }

    public static byte[] readNative(ZipInputStream input) throws IOException {
        return ZipFacade.readNative(input);
    }

    public static String readText(ZipInputStream input) throws IOException {
        return ZipFacade.readText(input);
    }
}
