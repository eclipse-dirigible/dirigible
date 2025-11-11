"use strict";

import { Logging } from "@aerokit/sdk/log";
import { CreateTableBuilder, DeleteBuilder, DropTableBuilder, InsertBuilder, SelectBuilder, SQLBuilder, UpdateBuilder } from "./sql";
import { Connection } from "./database";

export function ORMStatements(orm, dialect: SQLBuilder) {
	this.$log = Logging.getLogger('db.dao.ormstatements');
	this.orm = orm;
	this.orm.tableName = this.orm.table;
	this.orm.properties.forEach(function (property) {
		property.columnName = property.column;
	});
	this.dialect = dialect || SQLBuilder.getDialect();
};
ORMStatements.prototype.constructor = ORMStatements;

ORMStatements.prototype.createTable = function (): CreateTableBuilder {
	const builder = this.dialect.create().table(this.orm.table);

	this.orm.properties.forEach(function (property) {
		const column = this.orm.toColumn(property);
		if (property.type.toUpperCase() === 'VARCHAR') {
			if (property.length === undefined)
				property.length = 255;
			builder.columnVarchar(column.name, property.length, column.primaryKey === 'true', column.nullable === 'true', property.unique);
		} else if (property.type.toUpperCase() === 'CHAR') {
			if (property.length === undefined)
				property.length = 1;
			property.length = parseInt(property.length, 10);
			builder.columnChar(column.name, property.length, column.primaryKey === 'true', column.nullable === 'true', property.unique);
		} else {
			builder.column(column.name, column.type, column.primaryKey === 'true', column.nullable === 'true', property.unique);
		}
	}.bind(this));

	return builder;
};

ORMStatements.prototype.dropTable = function (): DropTableBuilder {
	return this.dialect.drop().table(this.orm.table);
};

ORMStatements.prototype.insert = function (): InsertBuilder {
	const builder = this.dialect.insert().into(this.orm.table);
	this.orm.properties.forEach(function (property) {
		builder.column(property.column).value('?', property);
	});
	return builder;
};

ORMStatements.prototype.update = function (entity): UpdateBuilder {
	if (!entity)
		throw Error('Illegal argument: entity[' + entity + ']');

	const builder = this.dialect.update().table(this.orm.table);
	this.orm.properties.filter(function (property) {
		return Object.keys(entity).indexOf(property.name) > -1 && (!property.allowedOps || property.allowedOps.indexOf('update') > -1);
	}).forEach(function (property) {
		if (!property.id)
			builder.set(property.column, '?', property);
	});
	const pkProperty = this.orm.getPrimaryKey();
	builder.where(pkProperty.column + '=?', [pkProperty]);
	return builder;
};

ORMStatements.prototype["delete"] = ORMStatements.prototype.remove = function (): DeleteBuilder {
	const builder = this.dialect.delete().from(this.orm.table);
	if (arguments[0] !== undefined) {
		let filterFieldNames = arguments[0];
		if (filterFieldNames.constructor !== Array)
			filterFieldNames = [filterFieldNames];
		for (let i = 0; i < filterFieldNames.length; i++) {
			const property = this.orm.getProperty(filterFieldNames[i]);
			if (!property)
				throw Error('Unknown property name: ' + filterFieldNames[i] + " in $filter");
			builder.where(property.column + "=?", [property]);
		}
	}
	return builder;
};

ORMStatements.prototype.find = function (params): SelectBuilder {
	let builder = this.dialect.select();
	if (params !== undefined && params.select !== undefined) {
		const selectedFields = params.select.constructor === Array ? params.select : [params.select];
		for (let i = 0; i < selectedFields.length; i++) {
			const property = this.orm.getProperty(selectedFields[i]);
			if (!property)
				throw Error('Unknown field name [' + selectedFields[i] + '] in $select');
			builder = builder.column(property.column);
		}
	}
	builder = builder.from(this.orm.table)
		.where(this.orm.getPrimaryKey().column + "=?", [this.orm.getPrimaryKey()]);
	return builder;
};

ORMStatements.prototype.count = function (settings): SelectBuilder {
	const builder = this.dialect.select().column('COUNT(*)').from(this.orm.table);
	addFilter(this.orm, builder, settings);
	return builder;
};

ORMStatements.prototype.list = function (settings): SelectBuilder {
	let i;
	settings = settings || {};
	const sort = settings.$sort || settings.sort;
	const order = settings.$order || settings.order;
	const selectedFields = settings.$select || settings.select;

	const builder = this.dialect.select().from(this.orm.table);

	//add selected fields if any
	if (selectedFields) {
		for (i = 0; i < selectedFields.length; i++) {
			const property = this.orm.getProperty(selectedFields[i]);
			if (!property)
				throw Error('Unknown field name [' + selectedFields[i] + '] in $select');
			builder.column(property.column);
		}
	}

	//add where clause for any fields
	addFilter(this.orm, builder, settings);

	// TODO: The following code might not be needed anymore
	const propertyDefinitions = this.orm.properties.filter(function (property) {
		for (var settingName in settings) {
			if (settingName === property.name)
				return true;
		}
		return false;
	});
	if (propertyDefinitions.length > 0) {
		for (i = 0; i < propertyDefinitions.length; i++) {
			const def = propertyDefinitions[i];
			if (settings?.$filter && settings.$filter?.indexOf(def.name) > -1) {
				builder.where(def.column + ' LIKE ?', [def]);
			} else {
				const val = settings[def.name];
				if (val === null || val === undefined) {
					builder.where(def.column + ' IS NULL', [def]);
				} else {
					if (val.indexOf && val.indexOf('>') > -1) {
						builder.where(def.column + ' > ?', [def]);
					} else if (val.indexOf && val.indexOf('<') > -1) {
						builder.where(def.column + ' < ?', [def]);
					} else {
						builder.where(def.column + '=?', [def]);
					}
				}
			}
		}
	}

	if (sort !== undefined) {
		var _sort = sort.split(',');
		for (i = 0; i < _sort.length; i++) {
			let _order = true;//ASC
			//TODO: change to be able to order per sort property
			if (order !== undefined) {
				if (['asc', 'desc'].indexOf(String(order).toLowerCase()) > -1) {
					_order = order.toLowerCase() === 'desc' ? false : true;
				}
			}
			if (this.orm.getProperty(_sort[i])) {
				builder.order(this.orm.getProperty(_sort[i]).column, _order);
			} else {
				console.error('Column: ' + _sort[i] + ' not present in ' + JSON.stringify(this.orm));
			}

		}
	}
	addLimit(builder, settings);
	addOffset(builder, settings);
	return builder;
};

function addFilter(orm, builder, settings) {
	//add where clause for any fields
	if (settings?.$filter) {
		const equalsPropertiesKeys = Object.keys(settings.$filter.equals ?? []);
		const notEqualsPropertiesKeys = Object.keys(settings.$filter.notEquals ?? []);
		const containsPropertiesKeys = Object.keys(settings.$filter.contains ?? []);
		const greaterThanPropertiesKeys = Object.keys(settings.$filter.greaterThan ?? []);
		const lessThanPropertiesKeys = Object.keys(settings.$filter.lessThan ?? []);
		const greaterThanOrEqualPropertiesKeys = Object.keys(settings.$filter.greaterThanOrEqual ?? []);
		const lessThanOrEqualPropertiesKeys = Object.keys(settings.$filter.lessThanOrEqual ?? []);

		const equalsProperties = orm.properties.filter(e => equalsPropertiesKeys.includes(e.name));
		const notEqualsProperties = orm.properties.filter(e => notEqualsPropertiesKeys.includes(e.name));
		const containsProperties = orm.properties.filter(e => containsPropertiesKeys.includes(e.name));
		const greaterThanProperties = orm.properties.filter(e => greaterThanPropertiesKeys.includes(e.name));
		const lessThanProperties = orm.properties.filter(e => lessThanPropertiesKeys.includes(e.name));
		const greaterThanOrEqualProperties = orm.properties.filter(e => greaterThanOrEqualPropertiesKeys.includes(e.name));
		const lessThanOrEqualProperties = orm.properties.filter(e => lessThanOrEqualPropertiesKeys.includes(e.name));

		equalsProperties.forEach(e => {
			const value = settings.$filter.equals[e.name];
			if (value === null || value === undefined) {
				builder.where(e.column + ' IS NULL', [e]);
			} else if (Array.isArray(value)) {
				builder.where(`${e.column} IN (${value.map(v => '?').join(', ')})`, [e])
			} else {
				builder.where(`${e.column} = ?`, [e])
			}
		});
		notEqualsProperties.forEach(e => {
			const value = settings.$filter.notEquals[e.name];
			if (value === null || value === undefined) {
				builder.where(e.column + ' IS NOT NULL', [e]);
			} else if (Array.isArray(value)) {
				builder.where(`${e.column} NOT IN (${value.map(v => '?').join(', ')})`, [e])
			} else {
				builder.where(`${e.column} != ?`, [e])
			}
		});
		containsProperties.forEach(e => builder.where(`${e.column} LIKE ?`, [e]));
		greaterThanProperties.forEach(e => builder.where(`${e.column} > ?`, [e]));
		lessThanProperties.forEach(e => builder.where(`${e.column} < ?`, [e]));
		greaterThanOrEqualProperties.forEach(e => builder.where(`${e.column} >= ?`, [e]));
		lessThanOrEqualProperties.forEach(e => builder.where(`${e.column} <= ?`, [e]));
	}
}

function addLimit(builder, settings) {
	const limit = settings.$limit ?? settings.limit;
	if (limit !== undefined) {
		builder.limit(parseInt(limit, 10));
	}
}

function addOffset(builder, settings) {
	const offset = settings.$offset ?? settings.offset;
	if (offset !== undefined) {
		builder.offset(parseInt(offset, 10));
	}
}

export function create(orm, connection?: Connection) {
	let dialect;
	if (connection) {
		dialect = SQLBuilder.getDialect(connection);
	}

	const stmnts = new ORMStatements(orm, dialect);
	return stmnts;
};
