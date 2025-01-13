/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.components.base.helpers.JsonHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Class ObjectStoreTest.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
@Transactional
public class DataStoreTest {

    /** The object store. */
    @Autowired
    private DataStore dataStore;

    /** The data source. */
    @Autowired
    private DataSource dataSource;

    /**
     * Setup.
     *
     * @throws Exception the exception
     */
    @BeforeEach
    public void setup() throws Exception {
        String mappingCustomer = IOUtils.toString(DataStoreTest.class.getResourceAsStream("/hbm/Customer.hbm.xml"), StandardCharsets.UTF_8);
        String mappingOrder = IOUtils.toString(DataStoreTest.class.getResourceAsStream("/hbm/Order.hbm.xml"), StandardCharsets.UTF_8);
        String mappingOrderItem =
                IOUtils.toString(DataStoreTest.class.getResourceAsStream("/hbm/OrderItem.hbm.xml"), StandardCharsets.UTF_8);

        dataStore.setDataSource(dataSource);
        dataStore.addMapping("Customer", mappingCustomer);
        dataStore.addMapping("Order", mappingOrder);
        dataStore.addMapping("OrderItem", mappingOrderItem);
        // objectStore.setDataSource(...);
        dataStore.initialize();
    }

    /**
     * Save object.
     */
    @Test
    public void save() {

        String json = "{\"name\":\"John\",\"address\":\"Sofia, Bulgaria\"}";

        dataStore.save("Customer", json);

        List list = dataStore.list("Customer");
        System.out.println(JsonHelper.toJson(list));

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
     * Save object.
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
     * Save object.
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
     * Save object.
     */
    @Test
    public void query() {

        String json = "{\"name\":\"John\",\"address\":\"Sofia, Bulgaria\"}";

        dataStore.save("Customer", json);

        List list = dataStore.query("select * from Customer");
        System.out.println(JsonHelper.toJson(list));

        assertNotNull(list);
        assertEquals(1, list.size());
        assertNotNull(list.get(0));
        assertEquals("John", ((Object[]) list.get(0))[1]);

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
