/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.jobs.domain;

/**
 * The Enum JobStatus.
 */
public enum JobStatus {
    // values are used in DB and in the UI as well
    /** The triggred. */
    // change them with caution
    TRIGGRED,
    /** The finished. */
    FINISHED,
    /** The failed. */
    FAILED,
    /** The logged. */
    LOGGED,
    /** The unknown. */
    UNKNOWN,
    /** The info. */
    INFO,
    /** The warn. */
    WARN,
    /** The error. */
    ERROR
}
