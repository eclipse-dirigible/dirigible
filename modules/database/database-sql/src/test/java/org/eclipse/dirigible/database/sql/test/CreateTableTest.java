/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.test;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.database.sql.DataType;
import org.eclipse.dirigible.database.sql.Modifiers;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * The Class CreateTableTest.
 */
public class CreateTableTest {

	/**
	 * Creates the table generic.
	 */
	@Test
	public void createTableGeneric() {
		String sql = SqlFactory.getDefault().create()
				.table("CUSTOMERS")
				.column("ID", DataType.INTEGER, Modifiers.PRIMARY_KEY, Modifiers.NOT_NULL, Modifiers.NON_UNIQUE)
				.column("FIRST_NAME", DataType.VARCHAR, Modifiers.REGULAR, Modifiers.NOT_NULL, Modifiers.UNIQUE, "(20)")
				.column("LAST_NAME", DataType.VARCHAR, Modifiers.REGULAR, Modifiers.NULLABLE, Modifiers.NON_UNIQUE, "(30)")
				.build();

		assertNotNull(sql);
		assertEquals("CREATE TABLE CUSTOMERS ( ID INTEGER NOT NULL PRIMARY KEY , FIRST_NAME VARCHAR (20) NOT NULL UNIQUE , LAST_NAME VARCHAR (30) )",
				sql);
	}

	/**
	 * Creates the table case sensitive generic.
	 */
	@Test
	public void createTableCaseSensitiveGeneric() {
		Configuration.set("DIRIGIBLE_DATABASE_NAMES_CASE_SENSITIVE", "true");
		try {
			String sql = SqlFactory.getDefault().create()
					.table("CUSTOMERS")
					.column("ID", DataType.INTEGER, Modifiers.PRIMARY_KEY, Modifiers.NOT_NULL, Modifiers.NON_UNIQUE)
					.column("FIRST_NAME", DataType.VARCHAR, Modifiers.REGULAR, Modifiers.NOT_NULL, Modifiers.UNIQUE, "(20)")
					.column("LAST_NAME", DataType.VARCHAR, Modifiers.REGULAR, Modifiers.NULLABLE, Modifiers.NON_UNIQUE, "(30)")
					.build();
			
			assertNotNull(sql);
			assertEquals("CREATE TABLE \"CUSTOMERS\" ( \"ID\" INTEGER NOT NULL PRIMARY KEY , \"FIRST_NAME\" VARCHAR (20) NOT NULL UNIQUE , \"LAST_NAME\" VARCHAR (30) )",
					sql);
		} finally {
			Configuration.set("DIRIGIBLE_DATABASE_NAMES_CASE_SENSITIVE", "false");
		}
	}

	/**
	 * Creates the table type safe.
	 */
	@Test
	public void createTableTypeSafe() {
		String sql = SqlFactory.getDefault().create()
				.table("CUSTOMERS")
				.columnInteger("ID", true, false, false)
				.columnVarchar("FIRST_NAME", 20, false, true, true)
				.columnVarchar("LAST_NAME", 30, false, true, false)
				.build();

		assertNotNull(sql);
		assertEquals("CREATE TABLE CUSTOMERS ( ID INTEGER NOT NULL PRIMARY KEY , FIRST_NAME VARCHAR (20) UNIQUE , LAST_NAME VARCHAR (30) )", sql);
	}

	/**
	 * Creates the table type constraint primary key.
	 */
	@Test
	public void createTableTypeConstraintPrimaryKey() {
		String sql = SqlFactory.getDefault().create().table("CUSTOMERS")
				.column("FIRST_NAME", DataType.VARCHAR, Modifiers.REGULAR, Modifiers.NULLABLE, Modifiers.NON_UNIQUE, "(20)")
				.column("LAST_NAME", DataType.VARCHAR, Modifiers.REGULAR, Modifiers.NULLABLE, Modifiers.NON_UNIQUE, "(30)")
				.primaryKey("PRIMARY_KEY", new String[] { "FIRST_NAME", "LAST_NAME" })
				.build();

		assertNotNull(sql);
		assertEquals(
				"CREATE TABLE CUSTOMERS ( FIRST_NAME VARCHAR (20) , LAST_NAME VARCHAR (30) , CONSTRAINT PRIMARY_KEY PRIMARY KEY ( FIRST_NAME , LAST_NAME ))",
				sql);
	}

	/**
	 * Creates the table type constraint primary key no name.
	 */
	@Test
	public void createTableTypeConstraintPrimaryKeyNoName() {
		String sql = SqlFactory.getDefault().create()
				.table("CUSTOMERS")
				.column("FIRST_NAME", DataType.VARCHAR, Modifiers.REGULAR, Modifiers.NULLABLE, Modifiers.NON_UNIQUE, "(20)")
				.column("LAST_NAME", DataType.VARCHAR, Modifiers.REGULAR, Modifiers.NULLABLE, Modifiers.NON_UNIQUE, "(30)")
				.primaryKey(new String[] { "FIRST_NAME", "LAST_NAME" })
				.build();

		assertNotNull(sql);
		assertEquals("CREATE TABLE CUSTOMERS ( FIRST_NAME VARCHAR (20) , LAST_NAME VARCHAR (30) , PRIMARY KEY ( FIRST_NAME , LAST_NAME ))", sql);
	}

	/**
	 * Creates the table type constraint foregn key.
	 */
	@Test
	public void createTableTypeConstraintForegnKey() {
		String sql = SqlFactory.getDefault().create()
				.table("CUSTOMERS")
				.column("FIRST_NAME", DataType.VARCHAR, Modifiers.REGULAR, Modifiers.NULLABLE, Modifiers.NON_UNIQUE, "(20)")
				.column("LAST_NAME", DataType.VARCHAR, Modifiers.REGULAR, Modifiers.NULLABLE, Modifiers.NON_UNIQUE, "(30)")
				.foreignKey("FOREIGN_KEY", new String[] { "PERSON_ADDRESS_ID" }, "ADDRESSES", new String[] { "ADDRESS_ID" })
				.build();

		assertNotNull(sql);
		assertEquals(
				"CREATE TABLE CUSTOMERS ( FIRST_NAME VARCHAR (20) , LAST_NAME VARCHAR (30) , CONSTRAINT FOREIGN_KEY FOREIGN KEY ( PERSON_ADDRESS_ID ) REFERENCES ADDRESSES( ADDRESS_ID ))",
				sql);
	}

	/**
	 * Creates the table type constraint unique index.
	 */
	@Test
	public void createTableTypeConstraintUniqueIndex() {
		String sql = SqlFactory.getDefault().create()
				.table("CUSTOMERS")
				.column("FIRST_NAME", DataType.VARCHAR, Modifiers.REGULAR, Modifiers.NULLABLE, Modifiers.NON_UNIQUE, "(20)")
				.column("LAST_NAME", DataType.VARCHAR, Modifiers.REGULAR, Modifiers.NULLABLE, Modifiers.NON_UNIQUE, "(30)")
				.unique("LAST_NAME_UNIQUE", new String[] { "LAST_NAME" })
				.build();

		assertNotNull(sql);
		assertEquals("CREATE TABLE CUSTOMERS ( FIRST_NAME VARCHAR (20) , LAST_NAME VARCHAR (30) , CONSTRAINT LAST_NAME_UNIQUE UNIQUE ( LAST_NAME ))",
				sql);
	}
	
	/**
	 * Creates the table type constraint check.
	 */
	@Test
	public void createTableTypeConstraintCheck() {
		String sql = SqlFactory.getDefault().create()
				.table("CUSTOMERS")
				.column("FIRST_NAME", DataType.VARCHAR, Modifiers.REGULAR, Modifiers.NULLABLE, Modifiers.NON_UNIQUE, "(20)")
				.column("LAST_NAME", DataType.VARCHAR, Modifiers.REGULAR, Modifiers.NULLABLE, Modifiers.NON_UNIQUE, "(30)")
				.check("LAST_NAME_CHECK", "LAST_NAME = 'Smith'")
				.build();

		assertNotNull(sql);
		assertEquals("CREATE TABLE CUSTOMERS ( FIRST_NAME VARCHAR (20) , LAST_NAME VARCHAR (30) , CONSTRAINT LAST_NAME_CHECK CHECK (LAST_NAME = 'Smith'))",
				sql);
	}
	
	/**
	 * Creates the table type constraint check.
	 */
	@Test
	public void createTableWithIdentity() {
		String sql = SqlFactory.getDefault().create()
				.table("CUSTOMERS")
				.column("ID", DataType.BIGINT, Modifiers.REGULAR, Modifiers.NULLABLE, Modifiers.NON_UNIQUE, Modifiers.IDENTITY)
				.column("FIRST_NAME", DataType.VARCHAR, Modifiers.REGULAR, Modifiers.NULLABLE, Modifiers.NON_UNIQUE, "(20)")
				.column("LAST_NAME", DataType.VARCHAR, Modifiers.REGULAR, Modifiers.NULLABLE, Modifiers.NON_UNIQUE, "(30)")
				.build();

		assertNotNull(sql);
		assertEquals("CREATE TABLE CUSTOMERS ( ID BIGINT IDENTITY , FIRST_NAME VARCHAR (20) , LAST_NAME VARCHAR (30) )",
				sql);
	}

	@Test
	public void createTableWithEscapedTableName() {
		String sql = SqlFactory.getDefault().create()
				.table("\"CUSTOMER\"")
				.column("FIRST_NAME", DataType.VARCHAR, Modifiers.REGULAR, Modifiers.NULLABLE, Modifiers.NON_UNIQUE, "(20)")
				.column("LAST_NAME", DataType.VARCHAR, Modifiers.REGULAR, Modifiers.NULLABLE, Modifiers.NON_UNIQUE, "(30)")
				.build();

		assertNotNull(sql);
		assertEquals(
				"CREATE TABLE \"CUSTOMER\" ( FIRST_NAME VARCHAR (20) , LAST_NAME VARCHAR (30) )",
				sql);
	}

	@Test
	public void createTableWithEscapedTableNameAndSchema() {
		String sql = SqlFactory.getDefault().create()
				.table("\"DBADMIN\".\"hdbtable-itest::incompatible-column-type-change-hana\"")
				.column("FIRST_NAME", DataType.VARCHAR, Modifiers.REGULAR, Modifiers.NULLABLE, Modifiers.NON_UNIQUE, "(20)")
				.column("LAST_NAME", DataType.VARCHAR, Modifiers.REGULAR, Modifiers.NULLABLE, Modifiers.NON_UNIQUE, "(30)")
				.build();

		assertNotNull(sql);
		assertEquals(
				"CREATE TABLE \"DBADMIN\".\"hdbtable-itest::incompatible-column-type-change-hana\" ( FIRST_NAME VARCHAR (20) , LAST_NAME VARCHAR (30) )",
				sql);
	}

}
