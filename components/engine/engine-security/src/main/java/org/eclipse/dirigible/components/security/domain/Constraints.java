/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.domain;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.Expose;

/**
 * The Class Constraints.
 */
public class Constraints {

    /** The constraints. */
    @Expose
    private List<Constraint> constraints = new ArrayList<>();

    /**
     * Builds the security accesses.
     *
     * @param location the location
     * @return the list
     */
    public List<Access> buildSecurityAccesses(String location) {
        List<Access> securityAccesses = new ArrayList<>();

        Integer keyIndex = 1;

        for (Constraint securityAccessConstraint : constraints) {
            for (String role : securityAccessConstraint.getRoles()) {
                Access securityAccess = new Access();
                securityAccess.setLocation(location);
                securityAccess.setType(Access.ARTEFACT_TYPE);
                securityAccess.setName(keyIndex.toString());
                securityAccess.updateKey();
                securityAccess.setMethod(securityAccessConstraint.getMethod());
                securityAccess.setRole(role);
                securityAccess.setPath(securityAccessConstraint.getPath());
                securityAccess.setScope(securityAccessConstraint.getScope());

                securityAccesses.add(securityAccess);
                keyIndex++;
            }
        }

        return securityAccesses;
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "Constraints [constraints=" + constraints + "]";
    }


}
