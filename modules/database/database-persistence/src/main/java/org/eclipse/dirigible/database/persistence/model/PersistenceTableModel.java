/**
 * Copyright (c) 2010-2018 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   SAP - initial API and implementation
 */
package org.eclipse.dirigible.database.persistence.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The Persistence Table Model transport object.
 */
public class PersistenceTableModel {

	private String className;

	private String tableName;

	private String schemaName;

	private List<PersistenceTableColumnModel> columns = new ArrayList<>();

	private List<PersistenceTableRelationModel> relations = new ArrayList<>();

	public PersistenceTableModel(String tableName, List<PersistenceTableColumnModel> columns, List<PersistenceTableRelationModel> relations) {
		this.tableName = tableName;
		this.columns = columns;
		this.relations = relations;
	}

	public PersistenceTableModel() {
	}

	/**
	 * Gets the class name.
	 *
	 * @return the class name
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * Sets the class name.
	 *
	 * @param className
	 *            the new class name
	 */
	public void setClassName(String className) {
		this.className = className;
	}

	/**
	 * Gets the table name.
	 *
	 * @return the table name
	 */
	public String getTableName() {
		return tableName;
	}

	/**
	 * Sets the table name.
	 *
	 * @param tableName
	 *            the new table name
	 */
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	/**
	 * Gets the schema name.
	 *
	 * @return the schema name
	 */
	public String getSchemaName() {
		return schemaName;
	}

	/**
	 * Sets the schema name.
	 *
	 * @param schemaName
	 *            the new schema name
	 */
	public void setSchemaName(String schemaName) {
		this.schemaName = schemaName;
	}

	/**
	 * Gets the columns.
	 *
	 * @return the columns
	 */
	public List<PersistenceTableColumnModel> getColumns() {
		return columns;
	}

	/**
	 * Sets the columns.
	 *
	 * @param columns
	 *            the new columns
	 */
	public void setColumns(List<PersistenceTableColumnModel> columns) {
		this.columns = columns;
	}

	/**
	 * gets the relations.
	 *
	 *            the new columns
	 */
	public List<PersistenceTableRelationModel> getRelations() {
		return relations;
	}

	/**
	 * Sets the relations.
	 *
	 * @param relations
	 *            the new columns
	 */
	public void setRelations(List<PersistenceTableRelationModel> relations) {
		this.relations = relations;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		PersistenceTableModel that = (PersistenceTableModel) o;
		return Objects.equals(className, that.className) &&
				tableName.equals(that.tableName) &&
				Objects.equals(schemaName, that.schemaName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(className, tableName, schemaName);
	}
}
