/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.bpm;

import org.eclipse.dirigible.sdk.bpm.internal.BpmFacadeBridge;

/**
 * Programmatic Flowable process deployer — sibling to the {@code .bpmn} synchronizer for the cases
 * where you need to push process definitions from code (one-off migrations, sample data loaders,
 * tests).
 * <p>
 * {@link #deployProcess(String)} accepts a path inside the platform repository (typically under
 * {@code /registry/public/<project>/<file>.bpmn}); the returned id is Flowable's deployment id,
 * which you then pass to {@link #undeployProcess(String)} or {@link #deleteProcess(String, String)}
 * to clean up.
 * <p>
 * For long-lived processes the synchronizer-based flow is preferable — drop the {@code .bpmn} into
 * the project and let the platform pick it up; reach for this class only when ad-hoc deployment is
 * actually required.
 */
public final class Deployer {

    private Deployer() {}

    /**
     * Deploys the BPMN process definition at {@code location} (a repository path) and returns the
     * Flowable deployment id.
     */
    public static String deployProcess(String location) {
        return BpmFacadeBridge.invoke("deployProcess", new Class<?>[] {String.class}, location);
    }

    /**
     * Removes a deployment previously registered via {@link #deployProcess(String)}. Running instances
     * of processes from that deployment are <em>not</em> terminated — use
     * {@link #deleteProcess(String, String)} for instance-level cleanup.
     */
    public static void undeployProcess(String deploymentId) {
        BpmFacadeBridge.invoke("undeployProcess", new Class<?>[] {String.class}, deploymentId);
    }

    /**
     * Terminates a specific process instance with the given reason text (visible in Flowable's history
     * tables and the BPM perspective).
     */
    public static void deleteProcess(String processInstanceId, String reason) {
        BpmFacadeBridge.invoke("deleteProcess", new Class<?>[] {String.class, String.class}, processInstanceId, reason);
    }
}
