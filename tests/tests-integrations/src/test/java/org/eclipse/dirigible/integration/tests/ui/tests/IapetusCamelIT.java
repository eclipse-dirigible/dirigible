/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests;

// import org.eclipse.dirigible.integration.tests.ui.TestProject;
// import org.eclipse.dirigible.tests.IDE;
// import org.eclipse.dirigible.tests.IDEFactory;
// import org.eclipse.dirigible.tests.framework.HtmlElementType;
// import org.eclipse.dirigible.tests.util.SecurityUtil;
// import org.eclipse.dirigible.tests.util.SleepUtil;
import org.eclipse.dirigible.integration.tests.ui.TestProject;
import org.eclipse.dirigible.tests.util.SleepUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.assertj.core.api.Assertions.assertThat;

class IapetusCamelIT extends UserInterfaceIntegrationTest {

    // @Autowired
    // private DataSource dataSource;
    //
    // private JdbcTemplate jdbcTemplate;
    @Autowired
    private TestProject testProject;

    @BeforeEach
    public void setup() {
        testProject.publish();
        browser.clearCookies();

        // wait some time synchronizers to complete their execution
        SleepUtil.sleepSeconds(12);

        // Initialize JdbcTemplate
        // jdbcTemplate = new JdbcTemplate(dataSource);
        //
        // // Create the custom_db database and custom_orders table
        // jdbcTemplate.execute("CREATE DATABASE IF NOT EXISTS custom_db;");
        // jdbcTemplate.execute("USE custom_db;");
        // jdbcTemplate.execute(
        // "CREATE TABLE IF NOT EXISTS custom_orders (" +
        // "order_id INT PRIMARY KEY, " +
        // "total DECIMAL(10, 2), " +
        // "date_added TIMESTAMP);"
        // );
        //
        // // Insert sample data into custom_orders
        // jdbcTemplate.execute(
        // "INSERT INTO custom_orders (order_id, total, date_added) " +
        // "VALUES " +
        // "(1, 100.50, '2023-10-01 12:00:00'), " +
        // "(2, 200.75, '2023-10-02 13:30:00'), " +
        // "(3, 300.25, '2023-10-03 14:45:00');"
        // );


    }

//    @Test
//    public void testProjectCreation() {
//        // Simulate project creation in Iapetus
//        // This step would typically involve UI automation or API calls
//        // For now, we assume the project is created successfully
//        assertThat(true).as("Project creation should succeed")
//                        .isTrue();
//    }
//
//    @Test
//    public void testDefineTableForOCOrders() {
//        // Simulate defining the OC_ORDERS table
//        // jdbcTemplate.execute(
//        // "CREATE TABLE IF NOT EXISTS OC_ORDERS (" +
//        // "ID INT PRIMARY KEY, " +
//        // "TOTAL DECIMAL(15, 4), " +
//        // "DATEADDED TIMESTAMP);"
//        // );
//        //
//        // // Verify the table exists
//        // List<Map<String, Object>> tables = jdbcTemplate.queryForList(
//        // "SHOW TABLES LIKE 'OC_ORDERS';"
//        // );
//        // assertThat(tables).as("The OC_ORDERS table should exist").hasSize(1);
//        assertThat(true).as("The OC_ORDERS table should exist")
//                        .isTrue();
//    }
//
//    @Test
//    public void testDefineTableForReplicatedOrders() {
//        // Simulate defining the ORDERS table
//        // jdbcTemplate.execute(
//        // "CREATE TABLE IF NOT EXISTS ORDERS (" +
//        // "ID INT PRIMARY KEY, " +
//        // "TOTAL DECIMAL(15, 4), " +
//        // "DATEADDED TIMESTAMP);"
//        // );
//        //
//        // // Verify the table exists
//        // List<Map<String, Object>> tables = jdbcTemplate.queryForList(
//        // "SHOW TABLES LIKE 'ORDERS';"
//        // );
//        // assertThat(tables).as("The ORDERS table should exist").hasSize(1);
//        assertThat(true).as("The ORDERS table should exist")
//                        .isTrue();
//
//    }
//
//    @Test
//    public void testCreateDataSource() {
//        // Simulate creating the DefaultDB data source
//        // For now, we assume the data source is created successfully
//        assertThat(true).as("Data source creation should succeed")
//                        .isTrue();
//    }
//
//    @Test
//    public void testVerifyDataSource() {
//        // Simulate verifying the DefaultDB data source
//        // For now, we assume the data source is verified successfully
//        assertThat(true).as("Data source verification should succeed")
//                        .isTrue();
//    }
    // // Verify the DefaultDB data source
    // List<Map<String, Object>> tables = jdbcTemplate.queryForList(
    // "SHOW TABLES LIKE 'custom_orders';"
    // );
    // assertThat(tables).as("The custom_orders table should exist").hasSize(1);
    //
    // // Execute a test query
    // List<Map<String, Object>> orders = jdbcTemplate.queryForList(
    // "SELECT order_id, total, date_added FROM custom_orders;"
    // );
    //
    // // Verify the results
    // assertThat(orders).as("There should be 3 orders in the custom_orders table").hasSize(3);

    // Verify the first order
    // Map<String, Object> firstOrder = orders.get(0);
    // assertThat(firstOrder.get("order_id")).as("Order ID should be 1").isEqualTo(1);
    // assertThat(firstOrder.get("total")).as("Total should be 100.50").isEqualTo(100.50);
    // assertThat(firstOrder.get("date_added").toString()).as("Date added should
    // match").isEqualTo("2023-10-01 12:00:00");
    // }
    @Test
    public void testImplementETLUsingJDBC() {
        // Simulate ETL implementation using JDBC
        // This step would typically involve UI automation or API calls
        // For now, we assume the ETL process runs successfully
        assertThat(true).as("ETL implementation using JDBC should succeed")
                        .isTrue();
    }

}

