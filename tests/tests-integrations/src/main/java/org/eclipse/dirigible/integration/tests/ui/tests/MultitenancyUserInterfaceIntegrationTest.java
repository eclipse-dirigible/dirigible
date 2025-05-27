package org.eclipse.dirigible.integration.tests.ui.tests;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;

public class MultitenancyUserInterfaceIntegrationTest extends UserInterfaceIntegrationTest {

    @BeforeAll
    public static void setUp() {
        DirigibleConfig.MULTI_TENANT_MODE_ENABLED.setBooleanValue(true);
    }

    protected List<DirigibleTestTenant> provisionTenants() {
        DirigibleTestTenant defaultTenant = DirigibleTestTenant.createDefaultTenant();
        DirigibleTestTenant tenant1 = new DirigibleTestTenant("test-tenant-1");
        DirigibleTestTenant tenant2 = new DirigibleTestTenant("test-tenant-2");

        List<DirigibleTestTenant> tenants = List.of(defaultTenant, tenant1, tenant2);

        createTenants(tenants);

        waitForTenantsProvisioning(tenants);

        return tenants;
    }

}
