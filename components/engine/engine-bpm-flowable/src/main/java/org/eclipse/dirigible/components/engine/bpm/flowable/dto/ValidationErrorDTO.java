/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.dto;

/**
 * One problem the Flowable process validator found in a BPMN model. The field names mirror what the
 * BPMN modeler's validation popup renders ({@code activityName}, {@code defaultDescription},
 * {@code warning}) and navigates by ({@code activityId}).
 */
public class ValidationErrorDTO {

    /** The id of the activity the problem is on, null when it is process-level. */
    private String activityId;

    /** The name of the activity the problem is on, null when it has none. */
    private String activityName;

    /** The validator's problem key, e.g. {@code flowable-servicetask-missing-implementation}. */
    private String problem;

    /** A human-readable description of the problem. */
    private String defaultDescription;

    /** The validator set that raised it. */
    private String validatorSetName;

    /** True when it is only a warning, false when it blocks the model. */
    private boolean warning;

    /**
     * Gets the activity id.
     *
     * @return the activity id
     */
    public String getActivityId() {
        return activityId;
    }

    /**
     * Sets the activity id.
     *
     * @param activityId the new activity id
     */
    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    /**
     * Gets the activity name.
     *
     * @return the activity name
     */
    public String getActivityName() {
        return activityName;
    }

    /**
     * Sets the activity name.
     *
     * @param activityName the new activity name
     */
    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    /**
     * Gets the problem key.
     *
     * @return the problem key
     */
    public String getProblem() {
        return problem;
    }

    /**
     * Sets the problem key.
     *
     * @param problem the new problem key
     */
    public void setProblem(String problem) {
        this.problem = problem;
    }

    /**
     * Gets the default description.
     *
     * @return the default description
     */
    public String getDefaultDescription() {
        return defaultDescription;
    }

    /**
     * Sets the default description.
     *
     * @param defaultDescription the new default description
     */
    public void setDefaultDescription(String defaultDescription) {
        this.defaultDescription = defaultDescription;
    }

    /**
     * Gets the validator set name.
     *
     * @return the validator set name
     */
    public String getValidatorSetName() {
        return validatorSetName;
    }

    /**
     * Sets the validator set name.
     *
     * @param validatorSetName the new validator set name
     */
    public void setValidatorSetName(String validatorSetName) {
        this.validatorSetName = validatorSetName;
    }

    /**
     * Checks if it is a warning.
     *
     * @return true when it is a warning
     */
    public boolean isWarning() {
        return warning;
    }

    /**
     * Sets the warning flag.
     *
     * @param warning the new warning flag
     */
    public void setWarning(boolean warning) {
        this.warning = warning;
    }
}
