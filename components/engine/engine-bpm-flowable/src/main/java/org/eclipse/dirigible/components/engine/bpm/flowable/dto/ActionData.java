/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.dto;

/**
 * The Class ActionData.
 */
public class ActionData {

    /** The action. */
    private String action;

    /**
     * Gets the action.
     *
     * @return the action
     */
    public String getAction() {
        return action;
    }

    /**
     * The Enum Action.
     */
    public enum Action {

        /** The retry. */
        RETRY("RETRY"),
        /** The skip. */
        SKIP("SKIP");

        /** The action name. */
        private final String actionName;

        /**
         * Instantiates a new action.
         *
         * @param actionName the action name
         */
        Action(String actionName) {
            this.actionName = actionName;
        }

        /**
         * Gets the action name.
         *
         * @return the action name
         */
        public String getActionName() {
            return actionName;
        }

    }
}
