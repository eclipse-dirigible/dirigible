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
function createSql(graph) {
	let sql = [];
	let parent = graph.getDefaultParent();
	let childCount = graph.model.getChildCount(parent);

	for (let i = 0; i < childCount; i++) {
		let child = graph.model.getChildAt(parent, i);

		if (!graph.model.isEdge(child)) {
			if (child.value.type === 'TABLE') {
				sql.push('CREATE TABLE IF NOT EXISTS ' + child.value.name + ' (');

				let columnCount = graph.model.getChildCount(child);

				if (columnCount > 0) {
					for (let j = 0; j < columnCount; j++) {
						let column = graph.model.getChildAt(child, j).value;

						sql.push('\n    ' + column.name + ' ' + column.type);

						if (column.type === 'VARCHAR' || column.type === 'CHAR') {
							sql.push('(' + column.columnLength + ')');
						}

						if (column.notNull && column.notNull === 'true') {
							sql.push(' NOT NULL');
						}

						if (column.primaryKey && column.primaryKey === 'true') {
							sql.push(' PRIMARY KEY');
						}

						if (column.autoIncrement && column.autoIncrement === 'true') {
							sql.push(' AUTOINCREMENT');
						}

						if (column.unique && column.unique === 'true') {
							sql.push(' UNIQUE');
						}

						if (column.defaultValue != null && column.defaultValue !== '') {
							sql.push(" DEFAULT '" + column.defaultValue + "'");
						}

						sql.push(',');
					}

					sql.splice(sql.length - 1, 1);
					sql.push('\n);');
				}
			} else {
				sql.push('DROP VIEW ' + child.value.name + ';\n');
				sql.push('CREATE VIEW ' + child.value.name + ' AS ');
				let column = graph.model.getChildAt(child, 0).value;
				sql.push(column.name + ';\n');
			}

			sql.push('\n');
		} else {
			sql.push('ALTER TABLE ' + child.source.parent.value.name + '\n');
			sql.push('ADD CONSTRAINT ' + child.source.parent.value.name + '_'
				+ child.target.parent.value.name + '\n');
			sql.push('FOREIGN KEY (' + child.source.value.name + ') REFERENCES '
				+ child.target.parent.value.name + '(' + child.target.value.name + ');\n');
		}
	}

	return sql.join('');
};