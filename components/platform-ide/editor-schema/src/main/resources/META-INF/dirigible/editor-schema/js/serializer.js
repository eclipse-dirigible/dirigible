/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
function createSchema(graph) {
	let schema = [];
	schema.push('<schema>\n');
	schema.push(' <structures>\n');
	let parent = graph.getDefaultParent();
	let childCount = graph.model.getChildCount(parent);

	for (let i = 0; i < childCount; i++) {
		let child = graph.model.getChildAt(parent, i);

		if (!graph.model.isEdge(child)) {
			schema.push('  <structure name="' + child.value.name + '" type="' + child.value.type.toUpperCase() + '">\n');

			let columnCount = graph.model.getChildCount(child);

			if (columnCount > 0) {
				for (let j = 0; j < columnCount; j++) {
					let column = graph.model.getChildAt(child, j).value;

					if (column.isSQL) {
						schema.push('    <query value="' + column.name + '"/>\n');
					} else {
						schema.push('    <column name="' + column.name + '" type="' + column.type + '"');

						if (column.type === 'VARCHAR' || column.type === 'CHAR') {
							schema.push(' length="' + column.columnLength + '"');
						}
						if (column.notNull === 'true') {
							schema.push(' nullable="false"');
						}
						if (column.primaryKey === 'true') {
							schema.push(' primaryKey="true"');
						}
						if (column.autoIncrement === 'true') {
							schema.push(' identity="true"');
						}
						if (column.unique === 'true') {
							schema.push(' unique="true"');
						}
						if (column.defaultValue !== null) {
							schema.push(' defaultValue="' + column.defaultValue + '"');
						}
						if (column.precision !== null) {
							schema.push(' precision="' + column.precision + '"');
						}
						if (column.scale !== null) {
							schema.push(' scale="' + column.scale + '"');
						}
						schema.push('></column>\n');
					}
				}
			}
			schema.push('  </structure>\n');
		} else {
			schema.push('  <structure name="' + child.source.parent.value.name + '_'
				+ child.target.parent.value.name + '" type="foreignKey" ');
			schema.push('table="' + child.source.parent.value.name + '" ');
			schema.push('constraintName="' + child.source.parent.value.name + '_'
				+ child.target.parent.value.name + '" ');
			schema.push('columns="' + child.source.value.name + '" ' +
				'referencedTable="' + child.target.parent.value.name + '" ' +
				'referencedColumns="' + child.target.value.name + '">\n');
			schema.push('  </structure>\n');
		}
	}
	schema.push(' </structures>\n');

	let enc = new mxCodec(mxUtils.createXmlDocument());
	let node = enc.encode(graph.getModel());
	let model = mxUtils.getXml(node);
	schema.push(' ' + model);
	schema.push('\n</schema>');

	return schema.join('');
}

function createSchemaJson(graph) {
	let root = {};
	root.schema = {};
	root.schema.structures = [];
	let parent = graph.getDefaultParent();
	let childCount = graph.model.getChildCount(parent);

	for (let i = 0; i < childCount; i++) {
		let child = graph.model.getChildAt(parent, i);
		let structure = {};
		if (!graph.model.isEdge(child)) {
			structure.name = child.value.name;
			structure.type = child.value.type.toUpperCase();
			structure.columns = [];

			let columnCount = graph.model.getChildCount(child);
			if (columnCount > 0) {
				for (let j = 0; j < columnCount; j++) {
					let childColumn = graph.model.getChildAt(child, j).value;
					let column = {};
					if (childColumn.isSQL) {
						column.query = childColumn.name;
					} else {
						column.name = childColumn.name;
						column.type = childColumn.type;
						column.length = childColumn.columnLength;
						column.nullable = childColumn.notNull === 'true' ? !childColumn.notNull : true;
						column.primaryKey = childColumn.primaryKey === 'true' ? childColumn.primaryKey : false;
						column.identity = childColumn.autoIncrement === 'true' ? childColumn.autoIncrement : false;
						column.unique = childColumn.unique === 'true' ? childColumn.unique : false;
						column.defaultValue = childColumn.defaultValue !== null && childColumn.defaultValue !== '' ? childColumn.defaultValue : null;
						column.precision = childColumn.precision === 'true' ? childColumn.precision : null;
						column.scale = childColumn.scale === 'true' ? childColumn.scale : null;
					}
					structure.columns.push(column);
				}
			}
		} else {
			structure.name = child.source.parent.value.name + '_' + child.target.parent.value.name;
			structure.type = 'foreignKey';
			structure.table = child.source.parent.value.name;
			structure.constraintName = child.source.parent.value.name + '_' + child.target.parent.value.name;
			structure.columns = child.source.value.name;
			structure.referencedTable = child.target.parent.value.name;
			structure.referencedColumns = child.target.value.name;
		}

		root.schema.structures.push(structure);
	}

	let serialized = JSON.stringify(root, null, 4);

	return serialized;
}