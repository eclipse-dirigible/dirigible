/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api.java.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.components.base.helpers.JsonHelper;
import org.eclipse.dirigible.components.data.store.DataStore;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class DataStoreIT extends IntegrationTest {

    @Autowired
    private DataStore dataStore;

    @BeforeEach
    public void setup() throws Exception {
        String mappingCustomer =
                IOUtils.toString(DataStoreIT.class.getResourceAsStream("/typescript/CustomerEntity.ts"), StandardCharsets.UTF_8);
        String mappingOrder = IOUtils.toString(DataStoreIT.class.getResourceAsStream("/typescript/OrderEntity.ts"), StandardCharsets.UTF_8);
        String mappingOrderItem =
                IOUtils.toString(DataStoreIT.class.getResourceAsStream("/typescript/OrderItemEntity.ts"), StandardCharsets.UTF_8);

        dataStore.addMapping("Customer", mappingCustomer);
        dataStore.addMapping("Order", mappingOrder);
        dataStore.addMapping("OrderItem", mappingOrderItem);

        dataStore.recreate();
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
     *
     * @throws SQLException
     */
    @Test
    public void query() throws SQLException {

        String json = "{\"name\":\"John\",\"address\":\"Sofia, Bulgaria\"}";

        dataStore.save("Customer", json);

        List list = dataStore.query("from Customer", null, 100, 0);
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
     *
     * @throws SQLException
     */
    @Test
    public void queryNative() throws SQLException {

        String json = "{\"name\":\"John\",\"address\":\"Sofia, Bulgaria\"}";

        dataStore.save("Customer", json);

        List list = dataStore.queryNative("select * from Customer", null, 100, 0);
        System.out.println(JsonHelper.toJson(list));

        assertNotNull(list);
        assertEquals(1, list.size());
        assertNotNull(list.get(0));
        assertEquals("John", ((Map) list.get(0)).get("customer_name"));

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

}
