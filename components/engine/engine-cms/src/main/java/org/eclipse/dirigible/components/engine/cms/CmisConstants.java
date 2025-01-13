/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms;

/**
 * The Interface CmisConstants.
 */
public interface CmisConstants {

    /** The Constant NAME. */
    // ---- Base ----
    public static final String NAME = "cmis:name";

    /** The Constant OBJECT_ID. */
    public static final String OBJECT_ID = "cmis:objectId";

    /** The Constant OBJECT_TYPE_ID. */
    public static final String OBJECT_TYPE_ID = "cmis:objectTypeId";

    /** The Constant BASE_TYPE_ID. */
    public static final String BASE_TYPE_ID = "cmis:baseTypeId";

    /** The Constant CREATED_BY. */
    public static final String CREATED_BY = "cmis:createdBy";

    /** The Constant CREATION_DATE. */
    public static final String CREATION_DATE = "cmis:creationDate";

    /** The Constant LAST_MODIFIED_BY. */
    public static final String LAST_MODIFIED_BY = "cmis:lastModifiedBy";

    /** The Constant LAST_MODIFICATION_DATE. */
    public static final String LAST_MODIFICATION_DATE = "cmis:lastModificationDate";

    /** The Constant CHANGE_TOKEN. */
    public static final String CHANGE_TOKEN = "cmis:changeToken";

    /** The Constant IS_IMMUTABLE. */
    // ---- CmisDocument ----
    public static final String IS_IMMUTABLE = "cmis:isImmutable";

    /** The Constant IS_LATEST_VERSION. */
    public static final String IS_LATEST_VERSION = "cmis:isLatestVersion";

    /** The Constant IS_MAJOR_VERSION. */
    public static final String IS_MAJOR_VERSION = "cmis:isMajorVersion";

    /** The Constant IS_LATEST_MAJOR_VERSION. */
    public static final String IS_LATEST_MAJOR_VERSION = "cmis:isLatestMajorVersion";

    /** The Constant VERSION_LABEL. */
    public static final String VERSION_LABEL = "cmis:versionLabel";

    /** The Constant VERSION_SERIES_ID. */
    public static final String VERSION_SERIES_ID = "cmis:versionSeriesId";

    /** The Constant IS_VERSION_SERIES_CHECKED_OUT. */
    public static final String IS_VERSION_SERIES_CHECKED_OUT = "cmis:isVersionSeriesCheckedOut";

    /** The Constant VERSION_SERIES_CHECKED_OUT_BY. */
    public static final String VERSION_SERIES_CHECKED_OUT_BY = "cmis:versionSeriesCheckedOutBy";

    /** The Constant VERSION_SERIES_CHECKED_OUT_ID. */
    public static final String VERSION_SERIES_CHECKED_OUT_ID = "cmis:versionSeriesCheckedOutId";

    /** The Constant CHECKIN_COMMENT. */
    public static final String CHECKIN_COMMENT = "cmis:checkinComment";

    /** The Constant CONTENT_STREAM_LENGTH. */
    public static final String CONTENT_STREAM_LENGTH = "cmis:contentStreamLength";

    /** The Constant CONTENT_STREAM_MIME_TYPE. */
    public static final String CONTENT_STREAM_MIME_TYPE = "cmis:contentStreamMimeType";

    /** The Constant CONTENT_STREAM_FILE_NAME. */
    public static final String CONTENT_STREAM_FILE_NAME = "cmis:contentStreamFileName";

    /** The Constant CONTENT_STREAM_ID. */
    public static final String CONTENT_STREAM_ID = "cmis:contentStreamId";

    /** The Constant PARENT_ID. */
    // ---- Folder ----
    public static final String PARENT_ID = "cmis:parentId";

    /** The Constant ALLOWED_CHILD_OBJECT_TYPE_IDS. */
    public static final String ALLOWED_CHILD_OBJECT_TYPE_IDS = "cmis:allowedChildObjectTypeIds";

    /** The Constant PATH. */
    public static final String PATH = "cmis:path";

    /** The Constant SOURCE_ID. */
    // ---- Relationship ----
    public static final String SOURCE_ID = "cmis:sourceId";

    /** The Constant TARGET_ID. */
    public static final String TARGET_ID = "cmis:targetId";

    /** The Constant POLICY_TEXT. */
    // ---- Policy ----
    public static final String POLICY_TEXT = "cmis:policyText";

    /** The Constant VERSIONING_STATE_NONE. */
    // ---- Versioning States ----
    public static final String VERSIONING_STATE_NONE = "none";

    /** The Constant VERSIONING_STATE_MAJOR. */
    public static final String VERSIONING_STATE_MAJOR = "major";

    /** The Constant VERSIONING_STATE_MINOR. */
    public static final String VERSIONING_STATE_MINOR = "minor";

    /** The Constant VERSIONING_STATE_CHECKEDOUT. */
    public static final String VERSIONING_STATE_CHECKEDOUT = "checkedout";

    /** The Constant OBJECT_TYPE_DOCUMENT. */
    // ---- Object Types ----
    public static final String OBJECT_TYPE_DOCUMENT = "cmis:document";

    /** The Constant OBJECT_TYPE_FOLDER. */
    public static final String OBJECT_TYPE_FOLDER = "cmis:folder";

    /** The Constant OBJECT_TYPE_RELATIONSHIP. */
    public static final String OBJECT_TYPE_RELATIONSHIP = "cmis:relationship";

    /** The Constant OBJECT_TYPE_POLICY. */
    public static final String OBJECT_TYPE_POLICY = "cmis:policy";

    /** The Constant OBJECT_TYPE_ITEM. */
    public static final String OBJECT_TYPE_ITEM = "cmis:item";

    /** The Constant OBJECT_TYPE_SECONDARY. */
    public static final String OBJECT_TYPE_SECONDARY = "cmis:secondary";

}
