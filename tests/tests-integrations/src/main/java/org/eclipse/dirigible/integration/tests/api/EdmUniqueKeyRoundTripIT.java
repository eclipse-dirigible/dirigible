/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.restassured.path.json.JsonPath;

/**
 * Saving an {@code .edm} in the EDM modeler regenerates its {@code .model} from the XML - so
 * whatever the XML cannot say is destroyed by the next save, for any unrelated edit, with nothing
 * reported (#6795).
 *
 * <p>
 * That is what makes the {@code <constraints>} section load-bearing rather than cosmetic: a
 * composite business key authored in an intent has to survive somebody opening the model in the
 * modeler and pressing Save. This drives the real save endpoint, which fires the real on-save
 * transformer, and reads the {@code .model} it produced.
 */
class EdmUniqueKeyRoundTripIT extends IntegrationTest {

    private static final String WORKSPACE = "edm-unique-key-it";
    private static final String PROJECT = "provisioning";
    private static final String FILES = "/services/ide/workspaces/" + WORKSPACE + "/" + PROJECT;

    /** An {@code .edm} in exactly the shape the intent generator emits for a composite key. */
    private static final String EDM =
            """
                    <model>
                     <entities>
                      <entity name="TenantApplication" dataName="PROVISIONING_TENANT_APPLICATION" type="PRIMARY" \
                    title="TenantApplication" caption="Manage entity TenantApplication" tooltip="TenantApplication" \
                    layoutType="MANAGE_MASTER" perspectiveName="Provisioning">
                       <property name="Id" dataName="TENANT_APPLICATION_ID" dataType="INTEGER" dataPrimaryKey="true" dataAutoIncrement="true"></property>
                       <property name="Tenant" dataName="TENANT_APPLICATION_TENANT" dataType="INTEGER"></property>
                       <property name="Application" dataName="TENANT_APPLICATION_APPLICATION" dataType="INTEGER"></property>
                      </entity>
                     </entities>
                     <constraints>
                      <uniqueKey><entity>TenantApplication</entity><name>TenantApplication_Tenant_Application</name>\
                    <properties>Tenant,Application</properties><message>This application is already provisioned for the tenant</message></uniqueKey>
                      <uniqueKey><entity>Ghost</entity><name>Ghost_A_B</name><properties>A,B</properties><message>never</message></uniqueKey>
                     </constraints>
                     <perspectives>
                      <perspective><name>Provisioning</name><label>Provisioning</label><icon>/services/web/resources/unicons/copy.svg</icon><order>1</order></perspective>
                     </perspectives>
                     <navigations>
                     </navigations>
                     <mxGraphModel></mxGraphModel>
                    </model>
                    """;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void aKeyDeclaredInTheEdmSurvivesTheSaveThatRebuildsTheModel() {
        restAssuredExecutor.execute(() -> {
            given().when()
                   .post("/services/ide/workspaces/" + WORKSPACE)
                   .then()
                   .statusCode(anyOfCreatedOrUnchanged());
            given().when()
                   .post("/services/ide/workspaces/" + WORKSPACE + "/" + PROJECT)
                   .then()
                   .statusCode(anyOfCreatedOrUnchanged());

            // Creating the file and updating it both fire the on-save transformer that rebuilds the
            // .model, and this exercises the create - a PUT to a file that is not there is a 404.
            given().contentType("text/plain")
                   .body(EDM)
                   .when()
                   .post(FILES + "/app.edm")
                   .then()
                   .statusCode(anyOfOkOrCreated());

            // ...and again through the update path, which is the one a modeler Save actually takes.
            given().contentType("text/plain")
                   .body(EDM)
                   .when()
                   .put(FILES + "/app.edm")
                   .then()
                   .statusCode(anyOfOkOrCreated());

            String model = given().when()
                                  .get(FILES + "/app.model")
                                  .then()
                                  .statusCode(200)
                                  .extract()
                                  .asString();

            JsonPath json = JsonPath.from(model);
            String base = "model.entities.find { it.name == 'TenantApplication' }.uniqueConstraints[0]";
            assertEquals("TenantApplication_Tenant_Application", json.getString(base + ".name"),
                    "the key must survive the rebuild - and keep the name the generated controller matches on: " + model);
            assertEquals("TENANT_APPLICATION_TENANT,TENANT_APPLICATION_APPLICATION", json.getString(base + ".columnsCsv"),
                    "the declared PROPERTIES must be resolved to their columns, which is what the schema is emitted from");
            assertEquals("This application is already provisioned for the tenant", json.getString(base + ".message"));

            // The key naming an entity this model does not have is dropped, not carried: a constraint
            // over a table that is not there fails the whole schema.
            assertTrue(!model.contains("Ghost_A_B"), "a key whose entity does not resolve must be dropped: " + model);
        });
    }

    /** The idempotent creates answer 201 the first time and 304 afterwards. */
    private static org.hamcrest.Matcher<Integer> anyOfCreatedOrUnchanged() {
        return org.hamcrest.Matchers.anyOf(org.hamcrest.Matchers.is(201), org.hamcrest.Matchers.is(304), org.hamcrest.Matchers.is(200));
    }

    private static org.hamcrest.Matcher<Integer> anyOfOkOrCreated() {
        return org.hamcrest.Matchers.anyOf(org.hamcrest.Matchers.is(200), org.hamcrest.Matchers.is(201), org.hamcrest.Matchers.is(204));
    }
}
