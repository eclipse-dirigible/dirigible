/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Set;

import org.eclipse.dirigible.components.jobs.domain.Job;
import org.eclipse.dirigible.components.jobs.domain.JobParameter;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * A job declaring parameters serialises with them, without following the parameter's back-reference
 * to its job (#8145).
 */
class JobFacadeTest {

    @Test
    void serialisesAJobWithItsDeclaredParameters() {
        JsonObject json = JsonParser.parseString(JobFacade.toJson(jobWithParameter("nightly")))
                                    .getAsJsonObject();

        assertEquals("nightly", json.get("name")
                                    .getAsString());
        JsonObject parameter = json.getAsJsonArray("parameters")
                                   .get(0)
                                   .getAsJsonObject();
        assertEquals("region", parameter.get("name")
                                        .getAsString());
        assertEquals("EU", parameter.get("defaultValue")
                                    .getAsString());
        assertFalse(parameter.has("job"));
    }

    @Test
    void serialisesAListOfJobsWithDeclaredParameters() {
        JsonArray json = JsonParser.parseString(JobFacade.toJson(List.of(jobWithParameter("nightly"), jobWithParameter("weekly"))))
                                   .getAsJsonArray();

        assertEquals(2, json.size());
        assertEquals("weekly", json.get(1)
                                   .getAsJsonObject()
                                   .get("name")
                                   .getAsString());
    }

    private static Job jobWithParameter(String name) {
        Job job = new Job(name, "group", "", null, null, "description", "0 0 2 * * ?", false, null, "/project/" + name + ".job", Set.of());
        job.setParameters(List.of(new JobParameter("region", "string", "EU", "", null, job)));
        return job;
    }
}
