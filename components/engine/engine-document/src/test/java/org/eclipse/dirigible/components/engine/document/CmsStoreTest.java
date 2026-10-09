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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;

import org.eclipse.dirigible.components.engine.cms.CmisConstants;
import org.eclipse.dirigible.components.engine.cms.CmisContentStream;
import org.eclipse.dirigible.components.engine.cms.CmisDocument;
import org.eclipse.dirigible.components.engine.cms.CmisFolder;
import org.eclipse.dirigible.components.engine.cms.CmisObject;
import org.eclipse.dirigible.components.engine.cms.CmisObjectFactory;
import org.eclipse.dirigible.components.engine.cms.CmisRepositoryInfo;
import org.eclipse.dirigible.components.engine.cms.CmisSession;
import org.eclipse.dirigible.components.engine.cms.CmisSessionFactory;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.eclipse.dirigible.components.engine.cms.ObjectType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * {@link CmsStore} on a CMS that has no rename and no in-place update (S3): a write is a delete and
 * a create, a move a copy and a delete - and neither may lose the document when a step fails. The
 * CMS is a fake whose create or delete can be made to fail by name.
 */
class CmsStoreTest {

    private static final String FOLDER = "/Templates/SalesInvoice/Print/en";
    private static final byte[] OLD = "<document>old</document>".getBytes(StandardCharsets.UTF_8);
    private static final byte[] NEW = "<document>new</document>".getBytes(StandardCharsets.UTF_8);

    /** The documents of the one folder, by path. */
    private final Map<String, byte[]> documents = new TreeMap<>();
    private Predicate<String> failingCreate = name -> false;
    private Predicate<String> failingDelete = name -> false;
    private MockedStatic<CmisSessionFactory> factory;
    private final CmsStore store = new CmsStore();

    @BeforeEach
    void setUp() {
        factory = mockStatic(CmisSessionFactory.class);
        factory.when(CmisSessionFactory::getSession)
               .thenReturn(new FakeSession());
    }

    @AfterEach
    void tearDown() {
        factory.close();
    }

    @Test
    void aWriteReplacesTheDocument() throws IOException {
        documents.put(FOLDER + "/acme.print", OLD);

        store.write(FOLDER + "/acme.print", NEW);

        assertArrayEquals(NEW, store.read(FOLDER + "/acme.print")
                                    .orElseThrow());
    }

    @Test
    void aWriteWhoseCreateFailsRestoresThePreviousContent() {
        documents.put(FOLDER + "/acme.print", OLD);
        // The first create (the new content) fails, the second (the restore) succeeds.
        int[] creates = {0};
        failingCreate = name -> "acme.print".equals(name) && creates[0]++ == 0;

        assertThrows(IOException.class, () -> store.write(FOLDER + "/acme.print", NEW));

        assertArrayEquals(OLD, documents.get(FOLDER + "/acme.print"), "the tenant's template must not be lost");
    }

    @Test
    void aMoveCopiesThenDeletesTheOriginalWithoutARename() throws IOException {
        documents.put(FOLDER + "/standard.print", OLD);

        store.move(FOLDER + "/standard.print", "standard@1.28.0.print", OLD);

        assertFalse(documents.containsKey(FOLDER + "/standard.print"));
        assertArrayEquals(OLD, documents.get(FOLDER + "/standard@1.28.0.print"));
    }

    @Test
    void aMoveWhoseDeleteOfTheOriginalFailsRemovesTheCopyAgain() {
        documents.put(FOLDER + "/standard.print", OLD);
        failingDelete = "standard.print"::equals;

        assertThrows(IOException.class, () -> store.move(FOLDER + "/standard.print", "standard@1.28.0.print", OLD));

        assertTrue(documents.containsKey(FOLDER + "/standard.print"), "the original stays until the next attempt");
        assertFalse(documents.containsKey(FOLDER + "/standard@1.28.0.print"), "no half-moved duplicate is left behind");
    }

    @Test
    void aMoveOntoAnExistingNameIsRefused() {
        documents.put(FOLDER + "/standard.print", OLD);
        documents.put(FOLDER + "/standard@1.28.0.print", NEW);

        assertThrows(IOException.class, () -> store.move(FOLDER + "/standard.print", "standard@1.28.0.print", OLD));

        assertTrue(documents.containsKey(FOLDER + "/standard.print"));
        assertArrayEquals(NEW, documents.get(FOLDER + "/standard@1.28.0.print"));
    }

    private static byte[] bytesOf(CmisContentStream stream) throws IOException {
        try (InputStream in = stream.getStream()) {
            return in.readAllBytes();
        }
    }

    private final class FakeSession implements CmisSession {
        @Override
        public CmisObject getObject(String id) throws IOException {
            return getObjectByPath(id);
        }

        @Override
        public CmisRepositoryInfo getRepositoryInfo() {
            return null;
        }

        @Override
        public CmisFolder getRootFolder() {
            return new FakeFolder("/");
        }

        @Override
        public CmisObjectFactory getObjectFactory() {
            return (filename, length, mimetype, inputStream) -> new FakeStream(filename, mimetype, inputStream);
        }

        @Override
        public CmisObject getObjectByPath(String path) throws IOException {
            if ("/".equals(path) || FOLDER.equals(path) || FOLDER.startsWith(path + "/")) {
                return new FakeFolder(path);
            }
            if (!documents.containsKey(path)) {
                throw new IOException("no object at [" + path + "]");
            }
            return new FakeDocument(path);
        }
    }

    private record FakeStream(String getFilename, String getMimeType, InputStream getStream) implements CmisContentStream {
        @Override
        public long getLength() {
            return -1;
        }

        @Override
        public InputStream getInputStream() {
            return getStream;
        }
    }

    private abstract class FakeObject implements CmisObject {
        final String path;

        FakeObject(String path) {
            this.path = path;
        }

        @Override
        public String sanitize(String value) {
            return value;
        }

        @Override
        public String getId() {
            return path;
        }

        @Override
        public String getName() {
            return path.substring(path.lastIndexOf('/') + 1);
        }

        @Override
        public void delete(boolean allVersions) throws IOException {
            delete();
        }

        @Override
        public void rename(String newName) {
            throw new UnsupportedOperationException("the S3 CMS has no rename - the store must not need one");
        }
    }

    private final class FakeFolder extends FakeObject implements CmisFolder {
        FakeFolder(String path) {
            super(path);
        }

        @Override
        public ObjectType getType() {
            return ObjectType.FOLDER;
        }

        @Override
        public boolean isRootFolder() {
            return "/".equals(path);
        }

        @Override
        public String getPath() {
            return path;
        }

        @Override
        public CmisDocument createDocument(Map<String, String> properties, CmisContentStream contentStream) throws IOException {
            String name = properties.get(CmisConstants.NAME);
            if (failingCreate.test(name)) {
                throw new IOException("create of [" + name + "] refused");
            }
            String documentPath = path + "/" + name;
            documents.put(documentPath, bytesOf(contentStream));
            return new FakeDocument(documentPath);
        }

        @Override
        public CmisDocument createDocument(Map<String, String> properties, CmisContentStream contentStream, VersioningState versioningState)
                throws IOException {
            return createDocument(properties, contentStream);
        }

        @Override
        public CmisFolder createFolder(Map<String, String> properties) {
            return new FakeFolder(path + "/" + properties.get(CmisConstants.NAME));
        }

        @Override
        public List<? extends CmisObject> getChildren() {
            List<CmisObject> children = new ArrayList<>();
            documents.keySet()
                     .stream()
                     .filter(each -> each.startsWith(path + "/"))
                     .forEach(each -> children.add(new FakeDocument(each)));
            return children;
        }

        @Override
        public CmisFolder getFolderParent() {
            return new FakeFolder("/");
        }

        @Override
        public void delete() {
            throw new UnsupportedOperationException();
        }
    }

    private final class FakeDocument extends FakeObject implements CmisDocument {
        FakeDocument(String path) {
            super(path);
        }

        @Override
        public ObjectType getType() {
            return ObjectType.DOCUMENT;
        }

        @Override
        public String getPath() {
            return path;
        }

        @Override
        public CmisContentStream getContentStream() {
            return new FakeStream(getName(), "text/xml", new ByteArrayInputStream(documents.get(path)));
        }

        @Override
        public void delete() throws IOException {
            if (failingDelete.test(getName())) {
                throw new IOException("delete of [" + getName() + "] refused");
            }
            documents.remove(path);
        }
    }
}
