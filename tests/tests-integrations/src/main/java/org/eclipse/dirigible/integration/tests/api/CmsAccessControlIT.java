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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.base.http.roles.Roles;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.engine.cms.access.CmsAccessService;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.framework.security.SecurityUtil;
import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import io.restassured.http.ContentType;

/**
 * End-to-end test of the dynamic CMS access control: an administrator grants a role access to a
 * path and it takes effect for the next request, with no job, no artefact and no waiting.
 * <p>
 * Each assertion here is a failure mode of the mechanism this replaces: propagation took up to a
 * minute through a Quartz job and two caches; a restriction on a folder never removed it from a
 * listing (the resolver it used matched an exact path only); and grants lived in a global table
 * while the content they guard is per tenant.
 */
// One Dirigible boot for the whole class: each method cleans up after itself, so the per-method
// context reset inherited from IntegrationTest would only add ~10s of boot time per test.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CmsAccessControlIT extends IntegrationTest {

    private static final String DOCUMENTS = "/services/documents";
    private static final String ACCESS = DOCUMENTS + "/access";

    /** The raw content path - the same tenant content, served by file path. */
    private static final String CMS = "/services/cms";

    private static final String FOLDER = "cms-access-it";
    private static final String FOLDER_PATH = "/" + FOLDER;
    private static final String FILE_NAME = "secret.txt";
    /** The kill-switch test uploads its own file - the class shares one database. */
    private static final String SWITCH_FILE_NAME = "kill-switch.txt";
    private static final String GRANTED_ROLE = "cms-access-it-role";

    /** A path only the second tenant ever writes a rule for. */
    private static final String OTHER_TENANT_PATH = "/cms-access-it-other";

    private static final String ADMIN = "cms-access-it-admin";
    private static final String PLAIN = "cms-access-it-user";
    private static final String PASSWORD = "cms-access-it-password";

    /** The kill switch, which must disable every surface at once. */
    private static final String CMS_ROLES_ENABLED = "DIRIGIBLE_CMS_ROLES_ENABLED";

    /** A caller holding nothing - restricted by any rule that covers the path. */
    private static final Predicate<String> NO_ROLES = role -> false;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private CmsAccessService cmsAccessService;

    @Autowired
    private TenantContext tenantContext;

    /**
     * The class shares one Spring context (and thus one database) across its methods, so the users are
     * created once and the grants each test leaves behind are revoked in {@link #revokeGrants()}.
     */
    @BeforeEach
    void createUsers() {
        securityUtil.ensureUserInDefaultTenant(ADMIN, PASSWORD, Roles.RoleNames.ADMINISTRATOR);
        securityUtil.ensureUserInDefaultTenant(PLAIN, PASSWORD);
    }

    /**
     * A leaked grant would break the "open by default - no rule exists" premise the other tests start
     * from. {@code revoke} is best-effort (it asserts no status), so this is a no-op when the test
     * already revoked or never granted.
     */
    @AfterEach
    void revokeGrants() {
        restAssuredExecutor.execute(() -> revoke(FOLDER_PATH, "READ", GRANTED_ROLE), ADMIN, PASSWORD);
    }

    @Test
    void a_read_grant_hides_the_folder_from_everybody_without_the_role_and_takes_effect_at_once() {
        restAssuredExecutor.execute(() -> createFolder(), ADMIN, PASSWORD);

        // before the grant the folder is visible to any authenticated user - the CMS is open by
        // default and a rule is what restricts it
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(DOCUMENTS)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("children.name", hasItem(FOLDER)),
                PLAIN, PASSWORD);

        restAssuredExecutor.execute(() -> grant(FOLDER_PATH, "READ", GRANTED_ROLE), ADMIN, PASSWORD);

        // NO waiting: the very next request sees it. The folder disappears from the listing AND is
        // unreachable by direct path - the old resolver did neither.
        restAssuredExecutor.execute(() -> {
            given().when()
                   .get(DOCUMENTS)
                   .then()
                   .statusCode(200)
                   .body("children.name", not(hasItem(FOLDER)));

            given().when()
                   .get(DOCUMENTS + "?path=" + FOLDER_PATH)
                   .then()
                   .statusCode(403);
        }, PLAIN, PASSWORD);

        // an administrator does not hold the granted role either, so the rule applies to them too -
        // access is by role, not by privilege level
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(DOCUMENTS + "?path=" + FOLDER_PATH)
                                                 .then()
                                                 .statusCode(403),
                ADMIN, PASSWORD);

        // revoking restores it, again on the next request
        restAssuredExecutor.execute(() -> revoke(FOLDER_PATH, "READ", GRANTED_ROLE), ADMIN, PASSWORD);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(DOCUMENTS + "?path=" + FOLDER_PATH)
                                                 .then()
                                                 .statusCode(200),
                PLAIN, PASSWORD);
    }

    @Test
    void the_effective_grants_report_what_a_path_declares_and_what_it_inherits() {
        restAssuredExecutor.execute(() -> {
            createFolder();
            grant(FOLDER_PATH, "READ", GRANTED_ROLE);

            given().when()
                   .get(ACCESS + "?path=" + FOLDER_PATH)
                   .then()
                   .statusCode(200)
                   .body("path", equalTo(FOLDER_PATH))
                   .body("own.role", hasItem(GRANTED_ROLE));

            // a child declares nothing of its own but inherits the parent's grant
            given().when()
                   .get(ACCESS + "?path=" + FOLDER_PATH + "/child")
                   .then()
                   .statusCode(200)
                   .body("own", equalTo(java.util.List.of()))
                   .body("inherited.role", hasItem(GRANTED_ROLE));
        }, ADMIN, PASSWORD);
    }

    @Test
    void managing_access_requires_an_administrative_role() {
        restAssuredExecutor.execute(() -> {
            given().when()
                   .get(ACCESS + "?path=/")
                   .then()
                   .statusCode(403);

            given().contentType(ContentType.JSON)
                   .body(grantBody("/", "READ", GRANTED_ROLE))
                   .when()
                   .put(ACCESS)
                   .then()
                   .statusCode(403);
        }, PLAIN, PASSWORD);
    }

    /**
     * A grant must govern every way the content can be read, not only the Documents API. The raw
     * content path serves the same tenant files by path, so if it ignored the grants, restricting a
     * folder would hide it from the user interface while leaving every file under it downloadable by
     * any authenticated caller who knows - or guesses - the path.
     */
    @Test
    void a_read_grant_also_blocks_the_raw_cms_content_path() {
        restAssuredExecutor.execute(() -> {
            createFolder();
            upload(FILE_NAME);
        }, ADMIN, PASSWORD);

        // open by default: both surfaces serve the file while no rule exists
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(CMS + FOLDER_PATH + "/" + FILE_NAME)
                                                 .then()
                                                 .statusCode(200),
                PLAIN, PASSWORD);

        restAssuredExecutor.execute(() -> grant(FOLDER_PATH, "READ", GRANTED_ROLE), ADMIN, PASSWORD);

        restAssuredExecutor.execute(() -> {
            given().when()
                   .get(DOCUMENTS + "/download?path=" + FOLDER_PATH + "/" + FILE_NAME)
                   .then()
                   .statusCode(403);

            given().when()
                   .get(CMS + FOLDER_PATH + "/" + FILE_NAME)
                   .then()
                   .statusCode(403);
        }, PLAIN, PASSWORD);
    }

    @Test
    void the_internal_folders_are_not_grantable() {
        restAssuredExecutor.execute(() -> given().contentType(ContentType.JSON)
                                                 .body(grantBody("/__internal", "READ", GRANTED_ROLE))
                                                 .when()
                                                 .put(ACCESS)
                                                 .then()
                                                 .statusCode(400),
                ADMIN, PASSWORD);
    }

    /**
     * The kill switch carried over from the JavaScript implementation must still disable the whole
     * mechanism, in every surface at once - it is what an operator reaches for when a rule locks the
     * wrong people out of their own documents.
     */
    @Test
    void the_kill_switch_disables_enforcement_wholesale() {
        restAssuredExecutor.execute(() -> {
            createFolder();
            // Its own file: the class shares one database, so uploading the name another test
            // already used would depend on the order the methods happen to run in.
            upload(SWITCH_FILE_NAME);
            grant(FOLDER_PATH, "READ", GRANTED_ROLE);
        }, ADMIN, PASSWORD);

        restAssuredExecutor.execute(() -> given().when()
                                                 .get(DOCUMENTS + "?path=" + FOLDER_PATH)
                                                 .then()
                                                 .statusCode(403),
                PLAIN, PASSWORD);

        Configuration.set(CMS_ROLES_ENABLED, "false");
        try {
            // The rule is still stored - it is simply not consulted any more, in either surface.
            restAssuredExecutor.execute(() -> {
                given().when()
                       .get(DOCUMENTS + "?path=" + FOLDER_PATH)
                       .then()
                       .statusCode(200);

                given().when()
                       .get(CMS + FOLDER_PATH + "/" + SWITCH_FILE_NAME)
                       .then()
                       .statusCode(200);

                given().when()
                       .get(DOCUMENTS)
                       .then()
                       .statusCode(200)
                       .body("children.name", hasItem(FOLDER));
            }, PLAIN, PASSWORD);
        } finally {
            Configuration.remove(CMS_ROLES_ENABLED);
        }

        restAssuredExecutor.execute(() -> given().when()
                                                 .get(DOCUMENTS + "?path=" + FOLDER_PATH)
                                                 .then()
                                                 .statusCode(403),
                PLAIN, PASSWORD);
    }

    /**
     * Grants are tenant data. The mechanism this replaces kept them in the global
     * {@code DIRIGIBLE_SECURITY_ACCESS} while every CMS path is tenant-resolved, so one tenant's rule
     * governed every tenant's like-named folder - a tenant could restrict, or expose, a folder it
     * cannot even see.
     */
    @Test
    void a_grant_in_one_tenant_does_not_govern_another() throws Exception {
        DirigibleTestTenant other = new DirigibleTestTenant("cms-access-it-tenant");
        createTenants(other);
        waitForTenantProvisioning(other);

        restAssuredExecutor.execute(() -> {
            createFolder();
            grant(FOLDER_PATH, "READ", GRANTED_ROLE);
        }, ADMIN, PASSWORD);

        // Outside a tenant scope the store resolves against the default tenant's schema - the very
        // rows the request above wrote. A caller holding no role is refused there ...
        cmsAccessService.invalidate();
        assertFalse(cmsAccessService.isAllowed(FOLDER_PATH, "READ", NO_ROLES), "the granting tenant's own rule applies to it");

        // ... and the same path in the other tenant is untouched by it.
        assertTrue(tenantContext.execute(other.getId(), () -> cmsAccessService.isAllowed(FOLDER_PATH, "READ", NO_ROLES)),
                "a rule of one tenant must not reach another tenant's identically named path");

        // The isolation holds in the other direction too: a grant written inside the other tenant
        // lands in that tenant's schema only. It needs no cleanup, unlike the grants the other tests
        // leave: it lives in a tenant this test creates and nothing else in the class ever enters.
        tenantContext.execute(other.getId(), () -> {
            cmsAccessService.grant(OTHER_TENANT_PATH, "READ", GRANTED_ROLE, "test");
            return null;
        });

        assertFalse(tenantContext.execute(other.getId(), () -> cmsAccessService.isAllowed(OTHER_TENANT_PATH, "READ", NO_ROLES)),
                "the other tenant's own rule applies to it");

        // Re-read the default tenant's rows rather than trusting what is already cached, so this
        // asserts an absent row and not a stale answer.
        cmsAccessService.invalidate();
        assertTrue(cmsAccessService.isAllowed(OTHER_TENANT_PATH, "READ", NO_ROLES),
                "the default tenant never sees the row the other tenant wrote");
    }

    private static void createFolder() {
        given().contentType(ContentType.JSON)
               .body("{\"parentFolder\":\"/\",\"name\":\"" + FOLDER + "\"}")
               .when()
               .post(DOCUMENTS + "/folder");
    }

    private static void upload(String fileName) {
        given().multiPart("file", fileName, "secret".getBytes(StandardCharsets.UTF_8))
               .when()
               .post(DOCUMENTS + "?path=" + FOLDER_PATH)
               .then()
               .statusCode(200);
    }

    private static void grant(String path, String method, String role) {
        given().contentType(ContentType.JSON)
               .body(grantBody(path, method, role))
               .when()
               .put(ACCESS)
               .then()
               .statusCode(200);
    }

    private static void revoke(String path, String method, String role) {
        given().contentType(ContentType.JSON)
               .body(grantBody(path, method, role))
               .when()
               .delete(ACCESS);
    }

    private static String grantBody(String path, String method, String role) {
        return "{\"path\":\"" + path + "\",\"method\":\"" + method + "\",\"role\":\"" + role + "\"}";
    }
}
