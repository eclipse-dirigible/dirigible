/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.flowable.engine.delegate;

/**
 * A test-only stand-in for Flowable's delegate interface, under Flowable's own package so its
 * {@linkplain Class#getName() binary name} is the real one.
 *
 * <p>
 * {@code engine-java} has no Flowable dependency (the dependency runs the other way:
 * {@code engine-bpm-flowable} depends on this module), which is why
 * {@code ComponentContainer.isJavaDelegate} matches the interface by <em>name</em>. This fixture is
 * what lets that match be tested at all — and it deliberately declares no {@code execute} method,
 * because the name is the whole of what the container checks.
 */
public interface JavaDelegate {
}
