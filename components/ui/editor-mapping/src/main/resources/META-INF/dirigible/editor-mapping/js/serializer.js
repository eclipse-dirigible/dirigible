/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
function createMapping(graph) {
	let mapping = [];
	mapping.push('<mapping>\n');
	mapping.push(' <structures>\n');
	let parent = graph.getDefaultParent();
	let childCount = graph.model.getChildCount(parent);

	for (let i = 0; i < childCount; i++) {
		let child = graph.model.getChildAt(parent, i);

		if (!graph.model.isEdge(child)) {
			let table = child.value;
			mapping.push('  <structure name="' + table.name + '" type="' + table.type.toUpperCase() + '">\n');

			let columnCount = table.columns.length;

			if (columnCount > 0) {
				for (let j = 0; j < columnCount; j++) {
					let column = table.columns[j];
					mapping.push('    <column name="' + column.name + '" type="' + column.type + '"');

					if (column.type === 'VARCHAR' || column.type === 'CHAR') {
						mapping.push(' length="' + column.columnLength + '"');
					}
					if (column.notNull === 'true') {
						mapping.push(' nullable="false"');
					}
					if (column.primaryKey === 'true') {
						mapping.push(' primaryKey="true"');
					}
					if (column.autoIncrement === 'true') {
						mapping.push(' identity="true"');
					}
					if (column.unique === 'true') {
						mapping.push(' unique="true"');
					}
					if (column.defaultValue) {
						mapping.push(' defaultValue="' + _.escape(column.defaultValue) + '"');
					}
					if (column.precision) {
						mapping.push(' precision="' + _.escape(column.precision) + '"');
					}
					if (column.scale) {
						mapping.push(' scale="' + _.escape(column.scale) + '"');
					}
					if (column.direct) {
						mapping.push(' direct="' + _.escape(column.direct) + '"');
					}
					if (column.constant) {
						mapping.push(' constant="' + _.escape(column.constant) + '"');
					}
					if (column.formula) {
						mapping.push(' formula="' + _.escape(column.formula) + '"');
					}
					if (column.module) {
						mapping.push(' module="' + _.escape(column.module) + '"');
					}
					if (column.criteria) {
						mapping.push(' criteria="' + _.escape(column.criteria) + '"');
					}

					mapping.push('></column>\n');
				}
			}
			mapping.push('  </structure>\n');
		} else {
			mapping.push('  <structure name="' + child.source.value.name + '_' + child.value.attributes.getNamedItem('sourceColumn').value + '___'
				+ child.target.value.name + '_' + child.value.attributes.getNamedItem('targetColumn').value
				+ '" type="RELATION" ');
			mapping.push('sourceTable="' + child.source.value.name + '" ');
			mapping.push('sourceColumn="' + child.value.attributes.getNamedItem('sourceColumn').value + '" ');
			mapping.push('targetTable="' + child.target.value.name + '" ');
			mapping.push('targetColumn="' + + child.value.attributes.getNamedItem('targetColumn').value + '">\n');
			mapping.push('  </structure>\n');
		}
	}
	mapping.push(' </structures>\n');

	let enc = new mxCodec(mxUtils.createXmlDocument());
	let node = enc.encode(graph.getModel());
	let model = mxUtils.getXml(node);
	mapping.push(' ' + model);
	mapping.push('\n</mapping>');

	return mapping.join('');
}

function createMappingJson(graph) {
	let root = {};
	root.mapping = {};
	root.mapping.structures = [];
	let parent = graph.getDefaultParent();
	let childCount = graph.model.getChildCount(parent);

	for (let i = 0; i < childCount; i++) {
		let child = graph.model.getChildAt(parent, i);
		let structure = {};
		if (!graph.model.isEdge(child)) {
			let table = child.value;
			structure.name = table.name;
			structure.type = table.type.toUpperCase();
			structure.columns = [];

			let columnCount = table.columns.length;
			if (columnCount > 0) {
				for (let j = 0; j < columnCount; j++) {
					let childColumn = table.columns[j];
					let column = {};
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
					if (childColumn.direct) {
						column.direct = childColumn.direct;
					}
					if (childColumn.constant) {
						column.constant = childColumn.constant;
					}
					if (childColumn.formula) {
						column.formula = childColumn.formula;
					}
					if (childColumn.module) {
						column.module = childColumn.module;
					}
					if (childColumn.criteria) {
						column.criteria = childColumn.criteria;
					}
					structure.columns.push(column);
				}
			}
		} else {
			structure.name = child.source.value.name + '_' + child.value.attributes.getNamedItem('sourceColumn').value
				+ '___' + child.target.value.name + '_' + child.value.attributes.getNamedItem('targetColumn').value;
			structure.type = 'RELATION';
			structure.sourceTable = child.source.value.name;
			structure.sourceColumn = child.value.attributes.getNamedItem('sourceColumn').value;
			structure.targetTable = child.target.value.name;
			structure.targetColumn = child.value.attributes.getNamedItem('targetColumn').value;

		}

		root.mapping.structures.push(structure);
	}

	let serialized = JSON.stringify(root, null, 4);

	return serialized;
}