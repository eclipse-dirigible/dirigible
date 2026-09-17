/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.builders.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.eclipse.dirigible.database.sql.DataType;
import org.eclipse.dirigible.database.sql.Modifiers;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.junit.Test;

/**
 * The Class CreateTableTest.
 */
public class AlterTableTest {

    /**
     * Alter the table generic.
     */
    @Test
    public void alterAddTableGeneric() {
        String sql = SqlFactory.getDefault()
                               .alter()
                               .table("CUSTOMERS")
                               .add()
                               .column("FIRST_NAME", DataType.VARCHAR, Modifiers.REGULAR, Modifiers.NULLABLE, Modifiers.NON_UNIQUE, "(20)")
                               .build();

        assertNotNull(sql);
        assertEquals("ALTER TABLE \"CUSTOMERS\" ADD \"FIRST_NAME\" VARCHAR (20) ;", sql);
    }

    /**
     * Alter the table type safe.
     */
    @Test
    public void alterAddTableTypeSafe() {
        String sql = SqlFactory.getDefault()
                               .alter()
                               .table("CUSTOMERS")
                               .add()
                               .columnVarchar("FIRST_NAME", 20, false, true, false)
                               .build();

        assertNotNull(sql);
        assertEquals("ALTER TABLE \"CUSTOMERS\" ADD \"FIRST_NAME\" VARCHAR (20) ;", sql);
    }

    /**
     * Alter the table generic.
     */
    @Test
    public void alerDropTableGeneric() {
        String sql = SqlFactory.getDefault()
                               .alter()
                               .table("CUSTOMERS")
                               .drop()
                               .column("FIRST_NAME", DataType.VARCHAR, Modifiers.REGULAR, Modifiers.NOT_NULL, Modifiers.UNIQUE, "(20)")
                               .build();

        assertNotNull(sql);
        assertEquals("ALTER TABLE \"CUSTOMERS\" DROP COLUMN \"FIRST_NAME\" ;", sql);
    }

    /**
     * Alter the table type safe.
     */
    @Test
    public void alterDropTableTypeSafe() {
        String sql = SqlFactory.getDefault()
                               .alter()
                               .table("CUSTOMERS")
                               .drop()
                               .columnVarchar("FIRST_NAME", 20, false, true, true)
                               .build();

        assertNotNull(sql);
        assertEquals("ALTER TABLE \"CUSTOMERS\" DROP COLUMN \"FIRST_NAME\" ;", sql);
    }

    /**
     * Alter table add foreign key.
     */
    @Test
    public void alterAddForeignKey() {
        String sql = SqlFactory.getDefault()
                               .alter()
                               .table("ORDERS")
                               .add()
                               .foreignKey("FK1", new String[] {"ORDER_CUSTOMER_ID"}, "CUSTOMERS", new String[] {"CUSTOMER_ID"})
                               .build();

        assertNotNull(sql);
        assertEquals(
                "ALTER TABLE \"ORDERS\" ADD CONSTRAINT \"FK1\" FOREIGN KEY ( \"ORDER_CUSTOMER_ID\" ) REFERENCES \"CUSTOMERS\" ( \"CUSTOMER_ID\" );",
                sql);
    }

    /**
     * Alter table drop foreign key.
     */
    @Test
    public void alterDropForeignKey() {
        String sql = SqlFactory.getDefault()
                               .alter()
                               .table("ORDERS")
                               .drop()
                               .foreignKey("FK1", new String[] {"ORDER_CUSTOMER_ID"}, "CUSTOMERS", new String[] {"CUSTOMER_ID"})
                               .build();

        assertNotNull(sql);
        assertEquals("ALTER TABLE \"ORDERS\" DROP CONSTRAINT FK1;", sql);
    }

    /** Alter table add a unique key (the table alter processor's reconciliation form). */
    @Test
    public void alterTableAddUnique() {
        String sql = SqlFactory.getDefault()
                               .alter()
                               .table("SALES_INVOICE")
                               .add()
                               .unique("SalesInvoice_Company_Number", new String[] {"SALES_INVOICE_COMPANY", "SALES_INVOICE_NUMBER"})
                               .build();

        assertNotNull(sql);
        assertEquals(
                "ALTER TABLE \"SALES_INVOICE\" ADD CONSTRAINT \"SalesInvoice_Company_Number\" UNIQUE ( \"SALES_INVOICE_COMPANY\" , \"SALES_INVOICE_NUMBER\" );",
                sql);
    }

    /** Alter table drop a unique key - the name quoted exactly as the ADD created it. */
    @Test
    public void alterTableDropUnique() {
        String sql = SqlFactory.getDefault()
                               .alter()
                               .table("SALES_INVOICE")
                               .drop()
                               .unique("SalesInvoice_Company_Number", new String[] {"SALES_INVOICE_COMPANY", "SALES_INVOICE_NUMBER"})
                               .build();

        assertNotNull(sql);
        assertEquals("ALTER TABLE \"SALES_INVOICE\" DROP CONSTRAINT \"SalesInvoice_Company_Number\";", sql);
    }

}
