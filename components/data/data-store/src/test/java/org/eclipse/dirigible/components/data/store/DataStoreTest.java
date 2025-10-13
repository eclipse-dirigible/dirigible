/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.base.helpers.JsonHelper;
import org.eclipse.dirigible.components.base.tenant.DefaultTenant;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.data.sources.config.DataSourceConfig;
import org.eclipse.dirigible.components.data.sources.domain.DataSource;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.data.sources.repository.DataSourceRepository;
import org.eclipse.dirigible.components.data.store.config.CurrentTenantIdentifierResolverImpl;
import org.eclipse.dirigible.components.data.store.config.MultiTenantConnectionProviderImpl;
import org.eclipse.dirigible.components.database.DirigibleConnection;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.eclipse.dirigible.components.initializers.SynchronousSpringEventsConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.DependsOn;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Class ObjectStoreTest.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {SynchronousSpringEventsConfig.class, DataSourceConfig.class}, loader = AnnotationConfigContextLoader.class)
@Transactional
@TestInstance(Lifecycle.PER_CLASS)
public class DataStoreTest {

    /** The datasource repository. */
    @Autowired
    private DataSourceRepository datasourceRepository;

    @Autowired
    private DataSourcesManager datasourcesManager;

    /** The object store. */
    private DataStore dataStore;

    private DirigibleDataSource dataSource;

    @MockBean
    private TenantContext tenantContext;

    @MockBean
    @DefaultTenant
    private Tenant defaultTenant;

    /**
     * Setup.
     *
     * @throws Exception the exception
     */
    @BeforeAll
    public void setup() throws Exception {
        Configuration.set("DIRIGIBLE_DATABASE_DATASOURCE_NAME_DEFAULT", "StoreDB");
        DataSource datasource = new DataSource("/test/StoreDB.datasource", "StoreDB", "", "org.h2.Driver", "jdbc:h2:~/StoreDB", "sa", "");
        datasourceRepository.save(datasource);
        dataSource = datasourcesManager.getDataSource("StoreDB");
        assertNotNull(dataSource);
        setupMocks();
        String mappingCustomer =
                IOUtils.toString(DataStoreTest.class.getResourceAsStream("/entity/Customer.entity"), StandardCharsets.UTF_8);
        String mappingOrder = IOUtils.toString(DataStoreTest.class.getResourceAsStream("/entity/Order.entity"), StandardCharsets.UTF_8);
        String mappingOrderItem =
                IOUtils.toString(DataStoreTest.class.getResourceAsStream("/entity/OrderItem.entity"), StandardCharsets.UTF_8);

        dataStore.addMapping("Customer", mappingCustomer);
        dataStore.addMapping("Order", mappingOrder);
        dataStore.addMapping("OrderItem", mappingOrderItem);
        dataStore.recreate();
    }

    @AfterAll
    public void cleanup() {
        Configuration.set("DIRIGIBLE_DATABASE_DATASOURCE_NAME_DEFAULT", "DefaultDB");
    }

    private void setupMocks() {
        CurrentTenantIdentifierResolverImpl tenantIdentifier = new CurrentTenantIdentifierResolverImpl(tenantContext);
        MultiTenantConnectionProviderImpl connectionProvider =
                new MultiTenantConnectionProviderImpl(this.datasourcesManager, this.dataSource);
        dataStore = new DataStore(this.dataSource, this.datasourcesManager, connectionProvider, tenantIdentifier);
    }

    /**
     * Save object.
     */
    @Test
    public void save() {
        String json = "{\"name\":\"John\",\"address\":\"Sofia, Bulgaria\"}";

        dataStore.save("Customer", json);

        List list = dataStore.list("Customer");
        System.err.println(JsonHelper.toJson(list));

        assertNotNull(list);
        assertThat(list).hasSize(1);
        assertNotNull(list.get(0));
        assertEquals("John", ((Map) list.get(0)).get("name"));

        Map object = dataStore.get("Customer", ((Long) ((Map) list.get(0)).get("id")));
        System.out.println(JsonHelper.toJson(object));

        assertNotNull(object);
        assertEquals("John", object.get("name"));

        for (Object element : list) {
            dataStore.delete("Customer", ((Long) ((Map) element).get("id")));
        }
        list = dataStore.list("Customer");
        assertNotNull(list);
        assertEquals(0, list.size());
    }

    /**
     * Criteria.
     */
    @Test
    public void criteria() {

        String json = "{\"name\":\"John\",\"address\":\"Sofia, Bulgaria\"}";
        dataStore.save("Customer", json);
        json = "{\"name\":\"Jane\",\"address\":\"Sofia, Bulgaria\"}";
        dataStore.save("Customer", json);
        json = "{\"name\":\"Matthias\",\"address\":\"Berlin, Germany\"}";
        dataStore.save("Customer", json);

        List list = dataStore.list("Customer");
        System.out.println(JsonHelper.toJson(list));

        assertNotNull(list);
        assertEquals(3, list.size());

        list = dataStore.list("Customer");
        for (Object element : list) {
            dataStore.delete("Customer", ((Long) ((Map) element).get("id")));
        }
    }

    /**
     * Bag in object.
     */
    @Test
    public void bag() {

        String json = "{\"number\":\"001\",\"items\":[{\"name\":\"TV\"},{\"name\":\"Fridge\"}]}";
        dataStore.save("Order", json);

        List list = dataStore.list("Order");
        System.out.println(JsonHelper.toJson(list));

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("001", ((Map) list.get(0)).get("number"));
        assertEquals(2, ((List) ((Map) list.get(0)).get("items")).size());
        Map order001 = dataStore.get("Order", (Long) ((Map) list.get(0)).get("id"));
        System.out.println(JsonHelper.toJson(order001));
        assertEquals("TV", ((Map) ((List) order001.get("items")).get(0)).get("name"));
        dataStore.delete("Order", ((Long) ((Map) list.get(0)).get("id")));
    }

    /**
     * Query object.
     */
    @Test
    public void query() {

        String json = "{\"name\":\"John\",\"address\":\"Sofia, Bulgaria\"}";

        dataStore.save("Customer", json);

        List list = dataStore.query("from Customer", 100, 0);
        System.out.println(JsonHelper.toJson(list));

        assertNotNull(list);
        assertEquals(1, list.size());
        assertNotNull(list.get(0));
        assertEquals("John", ((Map) list.get(0)).get("name"));

        list = dataStore.list("Customer");
        for (Object element : list) {
            dataStore.delete("Customer", ((Long) ((Map) element).get("id")));
        }
    }

    /**
     * Query native object.
     */
    @Test
    public void queryNative() {

        String json = "{\"name\":\"John\",\"address\":\"Sofia, Bulgaria\"}";

        dataStore.save("Customer", json);

        List list = dataStore.queryNative("select * from Customer");
        System.out.println(JsonHelper.toJson(list));

        assertNotNull(list);
        assertEquals(1, list.size());
        assertNotNull(list.get(0));
        assertEquals("John", ((Map) list.get(0)).get("name"));

        list = dataStore.list("Customer");
        for (Object element : list) {
            dataStore.delete("Customer", ((Long) ((Map) element).get("id")));
        }
    }

    /**
     * Find by example.
     */
    @Test
    public void findByExample() {

        String json = "{\"name\":\"John\",\"address\":\"Sofia, Bulgaria\"}";
        dataStore.save("Customer", json);
        json = "{\"name\":\"Jane\",\"address\":\"Varna, Bulgaria\"}";
        dataStore.save("Customer", json);
        json = "{\"name\":\"Matthias\",\"address\":\"Berlin, Germany\"}";
        dataStore.save("Customer", json);

        String example = "{\"name\":\"John\"}";

        List list = dataStore.findByExample("Customer", example, 10, 0);
        System.out.println(JsonHelper.toJson(list));

        assertNotNull(list);
        assertEquals(1, list.size());

        list = dataStore.list("Customer");
        for (Object element : list) {
            dataStore.delete("Customer", ((Long) ((Map) element).get("id")));
        }
    }

    /**
     * List with options.
     */
    @Test
    public void listWithOptions() {

        String json = "{\"name\":\"John\",\"address\":\"Sofia, Bulgaria\"}";
        dataStore.save("Customer", json);
        json = "{\"name\":\"Jane\",\"address\":\"Varna, Bulgaria\"}";
        dataStore.save("Customer", json);
        json = "{\"name\":\"Matthias\",\"address\":\"Berlin, Germany\"}";
        dataStore.save("Customer", json);

        String options = "{\"conditions\":[{\"propertyName\":\"name\",\"operator\":\"LIKE\",\"value\":\"J%\"}],"
                + "\"sorts\":[{\"propertyName\":\"name\",\"direction\":\"ASC\"}],\"limit\":\"100\"}";

        List list = dataStore.list("Customer", options);
        System.out.println(JsonHelper.toJson(list));

        assertNotNull(list);
        assertEquals(2, list.size());

        list = dataStore.list("Customer");
        for (Object element : list) {
            dataStore.delete("Customer", ((Long) ((Map) element).get("id")));
        }
    }

    /**
     * The Class TestConfiguration.
     */
    @SpringBootApplication
    static class TestConfiguration {
    }

}
