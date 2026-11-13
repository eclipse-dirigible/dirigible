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

/**
 * What an application contributes for an entity whose documents may be shared (issue #8613): the
 * name its links are stored under, and the answer to "may this record still be read through a
 * link?". The generated repository of an entity that declares the intent's {@code share:}
 * implements it; the public route asks it on every open.
 *
 * <p>
 * Asking at the moment of the open - rather than revoking from every place a record can change - is
 * what makes the rule hold whoever moved the record: a user's edit, a workflow step, a scheduled
 * job, a delete. A link whose subject is not contributed any more (the entity stopped declaring
 * {@code share:}, the application is not published) answers nothing.
 */
public interface ShareSubject {

    /**
     * The name this entity's links are stored under.
     *
     * @return the subject, unique among the application's shared entities
     */
    String shareSubject();

    /**
     * Whether a record is, right now, in the state its entity declared shareable.
     *
     * @param recordId the record's key, as text
     * @return {@code false} when the record left that state or does not exist
     */
    boolean isShareable(String recordId);
}
