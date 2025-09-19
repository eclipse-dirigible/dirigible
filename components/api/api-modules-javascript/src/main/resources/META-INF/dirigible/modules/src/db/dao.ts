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
"use strict";

import { ORMDefinition, get as getORM } from "./orm";
import { create as createORMStatements } from "./ormstatements";
import { Sequence } from "./sequence";
import { Database } from "./database";
import { Query } from "sdk/db";
import { Update } from "./update";
import { Insert } from "./insert";
import { Logging } from "sdk/log";
import { configurations, globals } from "sdk/core";


export function DAO(orm: ORMDefinition, logCtxName?: string, dataSourceName?: string) {
	if (orm === undefined)
		throw Error('Illegal argument: orm[' + orm + ']');

	this.orm = getORM(orm);
	this.sequenceName = this.orm.table + '_' + this.orm.getPrimaryKey().name.toUpperCase();
	this.dropIdGenerator = function () {
		return Sequence.drop(this.sequenceName, dataSourceName);
	};
	this.generateId = function () {
		return Sequence.nextval(this.sequenceName, this.orm.table, dataSourceName);
	};

	const conn = Database.getConnection(dataSourceName);
	try {
		this.ormstatements = createORMStatements(this.orm, conn);
	} finally {
		conn.close();
	}

	// setup loggerName
	let loggerName = logCtxName;
	if (!loggerName) {
		loggerName = 'db.dao';
		if (this.orm.table)
			loggerName = 'db.dao.' + (this.orm.table.toLowerCase());
	}
	this.$log = Logging.getLogger(loggerName);

	this.execute = function (sqlBuilder, parameterBindings) {
		const sql = sqlBuilder.build();
		if (sql === undefined || sql.length < 1)
			throw Error("Illegal argument: sql from statement builder is invalid[" + sql + "]");
		this.$log.trace('Executing SQL Statement: {}', sql);

		const parameters = sqlBuilder.parameters && sqlBuilder.parameters();
		const _parameterBindings = [];
		if (parameterBindings?.$filter && parameters && parameters.length > 0) {
			if (parameterBindings.$filter.equals) {
				const propertiesKeys = Object.keys(parameterBindings.$filter.equals);
				const addedPropertiesKeys = [];
				parameters.forEach(e => {
					if (propertiesKeys.includes(e.name) && !addedPropertiesKeys.includes(e.name)) {
						const value = parameterBindings.$filter.equals[e.name];
						if (Array.isArray(value)) {
							value.forEach(v => {
								_parameterBindings.push({
									type: e.type,
									value: parseValue(e.type, v)
								});
							});
						} else {
							_parameterBindings.push({
								type: e.type,
								value: parseValue(e.type, value)
							});
						}
						addedPropertiesKeys.push(e.name);
					}
				});
			}
			if (parameterBindings.$filter.notEquals) {
				const propertiesKeys = Object.keys(parameterBindings.$filter.notEquals);
				const addedPropertiesKeys = [];
				parameters.forEach(e => {
					if (propertiesKeys.includes(e.name) && !addedPropertiesKeys.includes(e.name)) {
						const value = parameterBindings.$filter.notEquals[e.name];
						if (Array.isArray(value)) {
							value.forEach(v => {
								_parameterBindings.push({
									type: e.type,
									value: parseValue(e.type, v)
								});
							});
						} else {
							_parameterBindings.push({
								type: e.type,
								value: parseValue(e.type, value)
							});
						}
						addedPropertiesKeys.push(e.name);
					}
				});
			}
			if (parameterBindings.$filter.contains) {
				const propertiesKeys = Object.keys(parameterBindings.$filter.contains);
				const addedPropertiesKeys = [];
				parameters.forEach(e => {
					if (propertiesKeys.includes(e.name) && !addedPropertiesKeys.includes(e.name)) {
						_parameterBindings.push({
							type: e.type,
							value: `%${parameterBindings.$filter.contains[e.name]}%`
						});
						addedPropertiesKeys.push(e.name);
					}
				});
			}
			if (parameterBindings.$filter.greaterThan) {
				const propertiesKeys = Object.keys(parameterBindings.$filter.greaterThan);
				const addedPropertiesKeys = [];
				parameters.forEach(e => {
					if (propertiesKeys.includes(e.name) && !addedPropertiesKeys.includes(e.name)) {
						_parameterBindings.push({
							type: e.type,
							value: parseValue(e.type, parameterBindings.$filter.greaterThan[e.name])
						});
						addedPropertiesKeys.push(e.name);
					}
				});
			}
			if (parameterBindings.$filter.lessThan) {
				const propertiesKeys = Object.keys(parameterBindings.$filter.lessThan);
				const addedPropertiesKeys = [];
				parameters.forEach(e => {
					if (propertiesKeys.includes(e.name) && !addedPropertiesKeys.includes(e.name)) {
						_parameterBindings.push({
							type: e.type,
							value: parseValue(e.type, parameterBindings.$filter.lessThan[e.name])
						});
						addedPropertiesKeys.push(e.name);
					}
				});
			}
			if (parameterBindings.$filter.greaterThanOrEqual) {
				const propertiesKeys = Object.keys(parameterBindings.$filter.greaterThanOrEqual);
				const addedPropertiesKeys = [];
				parameters.forEach(e => {
					if (propertiesKeys.includes(e.name) && !addedPropertiesKeys.includes(e.name)) {
						_parameterBindings.push({
							type: e.type,
							value: parseValue(e.type, parameterBindings.$filter.greaterThanOrEqual[e.name])
						});
						addedPropertiesKeys.push(e.name);
					}
				});
			}
			if (parameterBindings.$filter.lessThanOrEqual) {
				const propertiesKeys = Object.keys(parameterBindings.$filter.lessThanOrEqual);
				const addedPropertiesKeys = [];
				parameters.forEach(e => {
					if (propertiesKeys.includes(e.name) && !addedPropertiesKeys.includes(e.name)) {
						_parameterBindings.push({
							type: e.type,
							value: parseValue(e.type, parameterBindings.$filter.lessThanOrEqual[e.name])
						});
						addedPropertiesKeys.push(e.name);
					}
				});
			}
		}

		// Left for backward compatibility -> it seems that the find() method uses it, might be removed once refactored
		if (parameters && parameters.length > 0) {
			for (var i = 0; i < parameters.length; i++) {
				var val;
				if (parameterBindings) {
					if (Array.isArray(parameterBindings)) {
						val = parameterBindings[i];
					} else {
						val = parameterBindings[parameters[i].name];
					}
				}
				if ((val === null || val === undefined) && sql.toLowerCase().startsWith('select')) {
					continue;
				}
				const index = i + 1;
				this.$log.trace('Binding to parameter[{}]: {}', index, val);
				_parameterBindings.push({
					"type": parameters[i].type,
					"value": parseValue(parameters[i].type, val)
				});
			}
		}

		let result;

		if (sql.toLowerCase().startsWith('select')) {
			result = Query.execute(sql, _parameterBindings, dataSourceName);
		} else if (sql.toLowerCase().startsWith('insert')) {
			result = Insert.execute(sql, _parameterBindings, dataSourceName);
		} else {
			result = Update.execute(sql, _parameterBindings, dataSourceName);
		}

		return result !== null ? result : [];
	};

	function parseValue(type, value) {
		switch (type.toUpperCase()) {
			case 'INTEGER':
				return parseInt(value);
			case 'DOUBLE':
			case 'FLOAT':
				return parseFloat(value);
			case 'BOOLEAN':
				return value ? 'true' : 'false';
			default:
				return value;
		}
	}
};

DAO.prototype.notify = function (event) {
	const func = this[event];
	if (!this[event])
		return;
	if (typeof func !== 'function')
		throw Error('Illegal argument. Not a function: ' + func);
	const args = [].slice.call(arguments);
	func.apply(this, args.slice(1));
};

/**
 * Prepare a JSON object for insert into DB
 */
DAO.prototype.createSQLEntity = function (entity) {
	let i;
	const persistentItem = {};
	const mandatories = this.orm.getMandatoryProperties();
	for (i = 0; i < mandatories.length; i++) {
		if (mandatories[i].dbValue) {
			persistentItem[mandatories[i].name] = mandatories[i].dbValue.apply(this, [entity[mandatories[i].name], entity]);
		} else {
			persistentItem[mandatories[i].name] = entity[mandatories[i].name];
		}
	}
	const optionals = this.orm.getOptionalProperties();
	for (i = 0; i < optionals.length; i++) {
		if (optionals[i].dbValue !== undefined) {
			persistentItem[optionals[i].name] = optionals[i].dbValue.apply(this, [entity[optionals[i].name], entity]);
		} else {
			persistentItem[optionals[i].name] = entity[optionals[i].name] === undefined ? null : entity[optionals[i].name];
		}
	}
	return persistentItem;
};

/**
 * Create entity as JSON object from ResultSet current Row
 */
DAO.prototype.createEntity = function (resultSetEntry, entityPropertyNames) {
	const entity = {};
	let properties = this.orm.properties;
	if (entityPropertyNames && entityPropertyNames.length > 0) {
		properties = properties.filter(function (prop) {
			return entityPropertyNames.indexOf(prop.name) > -1;
		});
	}
	for (let i = 0; i < properties.length; i++) {
		const prop = properties[i];
		entity[prop.name] = resultSetEntry[prop.columnName];
		if (prop.value) {
			entity[prop.name] = prop.value(entity[prop.name]);
		}
	}
	Object.keys(entity).forEach(function (propertyName) {
		if (entity[propertyName] === null)
			entity[propertyName] = undefined;
	});

	let entitySegment = "";
	if (entity[this.orm.getPrimaryKey().name]) {
		entitySegment = "[" + entity[this.orm.getPrimaryKey().name] + "]";
	}
	// this.$log.trace("Transformation from {} DB JSON object finished", (this.orm.table + entitySegment));
	return entity;
};

DAO.prototype.validateEntity = function (entity, skip) {
	if (entity === undefined || entity === null) {
		throw new Error('Illegal argument: entity is ' + entity);
	}
	if (skip) {
		if (skip.constructor !== Array) {
			skip = [skip];
		}
		for (var j = 0; j < skip.length; j++) {
			skip[j];
		}
	}
	const mandatories = this.orm.getMandatoryProperties();
	for (let i = 0; i < mandatories.length; i++) {
		const propName = mandatories[i].name;
		if ((skip && skip.indexOf(propName) > -1) || mandatories[i].type.toUpperCase() === 'BOOLEAN')
			continue;
		const propValue = entity[propName];
		if (propValue === undefined || propValue === null) {
			throw new Error('Illegal ' + propName + ' attribute value in ' + this.orm.table + ' entity: ' + propValue);
		}
	}
};

DAO.prototype.insert = function (_entity) {

	const ids = [];
	let entities = _entity;
	if (_entity.constructor !== Array) {
		entities = [_entity];
	}

	this.$log.trace('Inserting {} {}', this.orm.table, (entities.length === 1 ? 'entity' : 'entities'));

	for (let i = 0; i < entities.length; i++) {
		let entity = entities[i];
		entity = entities[i];

		this.validateEntity(entity, [this.orm.getPrimaryKey().name]);

		//check for unique constraint violations
		const uniques = this.orm.getUniqueProperties();
		for (let _i = 0; _i < uniques.length; _i++) {
			const prop = uniques[_i];
			const st = this.ormstatements.dialect
				.select(prop.column)
				.from(this.orm.table)
				.where(prop.column + '=?', [prop]);
			const params = {};
			params[prop.name] = entity[prop.name];
			const rs = this.execute(st, params);
			if (rs.length > 0)
				throw Error('Unique constraint violation for ' + prop.name + '[' + entity[prop.name] + ']');
		}

		const dbEntity = this.createSQLEntity(entity);

		try {
			const parametericStatement = this.ormstatements.insert.apply(this.ormstatements);

			if (this.orm.isAutoIncrementPrimaryKey()) {
				const id = this.generateId();
				dbEntity[this.orm.getPrimaryKey().name] = id;
			}

			const updatedRecordCount = this.execute(parametericStatement, dbEntity);

			if (!this.orm.isAutoIncrementPrimaryKey() && isNotEmptyArray(updatedRecordCount)) {
				const id = updatedRecordCount[0];
				dbEntity[this.orm.getPrimaryKey().name] = id;
			}

			this.notify('afterInsert', dbEntity);
			this.notify('beforeInsertAssociationSets', dbEntity);
			if ((updatedRecordCount > 0 || isNotEmptyArray(updatedRecordCount)) && this.orm.associations && Object.keys(this.orm.associations).length) {
				//Insert dependencies if any are provided inline with this entity
				this.$log.trace('Inserting association sets for {}[{}]', this.orm.table, dbEntity[this.orm.getPrimaryKey().name]);
				for (const idx in Object.keys(this.orm.associations)) {
					const association = this.orm.associations[idx];
					const associationName = association['name'];
					if ([this.orm.ASSOCIATION_TYPES['MANY-TO-MANY'], this.orm.ASSOCIATION_TYPES['MANY-TO-ONE']].indexOf(association.type) < 0) {
						if (entity[associationName] && entity[associationName].length > 0) {
							const associationDaoFactoryFunc = association.targetDao || this;
							if (associationDaoFactoryFunc.constructor !== Function)
								throw Error('Invalid ORM: Association ' + associationName + ' dao property is expected to be function. Instead, it is: ' + (typeof associationDaoFactoryFunc))
							const associationDAO = associationDaoFactoryFunc.apply(this);
							this.notify('beforeInsertAssociationSet', entity[associationName], entity);
							this.$log.trace('Inserting {} inline entities into association set {}', entity[associationName].length, associationName);
							for (let j = 0; j < entity[associationName].length; j++) {
								const associatedEntity = entity[associationName][j];
								const associatedEntityJoinKey = association.joinKey;
								const key = association.key || this.orm.getPrimaryKey().name;
								associatedEntity[associatedEntityJoinKey] = entity[key];
								this.notify('beforeInsertAssociationSetEntity', entity[associationName], dbEntity);

								associationDAO.insert.apply(associationDAO, [associatedEntity]);

							}
							this.$log.trace('Inserting {} inline entities into association set {} finsihed', entity[associationName].length, associationName);
							this.notify('afterInsertAssociationSet', entity[associationName], dbEntity);
						}
					}
				}
			}

			if (updatedRecordCount > 0 || isNotEmptyArray(updatedRecordCount)) {
				ids.push(dbEntity[this.orm.getPrimaryKey().name]);
				this.$log.trace('{}[] entity inserted', this.orm.table, dbEntity[this.orm.getPrimaryKey().name]);
			} else {
				this.$log.trace('No changes incurred in {}', this.orm.table);
			}


		} catch (e) {
			this.$log.error("Inserting {} {} failed", this.orm.table, (entities.length === 1 ? 'entity' : 'entities'), e);
			this.$log.trace('Rolling back changes after failed {}[{}] insert. ', this.orm.table, dbEntity[this.orm.getPrimaryKey().name]);
			if (dbEntity[this.orm.getPrimaryKey().name]) {
				try {
					this.remove(dbEntity[this.orm.getPrimaryKey().name]);
				} catch (err) {
					this.$log.error('Could not rollback changes after failed {}[{}] insert. ', this.orm.table, dbEntity[this.orm.getPrimaryKey().name], err);
				}
			}
			throw e;
		}
	}

	if (_entity.constructor !== Array)
		return ids[0];
	else
		return ids;
};

function isNotEmptyArray(array) {
	return Array.isArray(array) && array.length > 0;
}

/**
 * Update entity from a JSON object. Returns the id of the updated entity.
 */
DAO.prototype.update = function (entity) {

	this.$log.trace('Updating {}[{}] entity', this.orm.table, entity !== undefined ? entity[this.orm.getPrimaryKey().name] : entity);

	if (entity === undefined || entity === null) {
		throw new Error('Illegal argument: entity is ' + entity);
	}

	const ignoredProperties = this.orm.getMandatoryProperties()
		.filter(function (property) {
			return property.allowedOps && property.allowedOps.indexOf('update') < 0;
		})
		.map(function (property) {
			return property.name;
		});
	this.validateEntity(entity, ignoredProperties);

	const parametericStatement = this.ormstatements.update.apply(this.ormstatements, [entity]);

	const dbEntity = this.createSQLEntity(entity);

	try {
		this.notify('beforeUpdateEntity', dbEntity);
		const updatedRecordsCount = this.execute(parametericStatement, dbEntity);
		if (updatedRecordsCount > 0)
			this.$log.trace('{}[{}] entity updated', this.orm.table, dbEntity[this.orm.getPrimaryKey().name]);
		else
			this.$log.trace('No changes incurred in {}', this.orm.table);

		return this;

	} catch (e) {
		this.$log.error('Updating {}[{}] failed', this.orm.table, entity !== undefined ? entity[this.orm.getPrimaryKey().name] : entity, e);
		throw e;
	}
};

/**
 * Delete entity by id, or array of ids, or delete all (if not argument is provided)
 */
DAO.prototype.remove = function () {

	let ids = [];
	if (arguments.length === 0) {
		ids = this.list({
			"$select": [this.orm.getPrimaryKey().name]
		}).map(function (ent) {
			return ent[this.orm.getPrimaryKey().name];
		}.bind(this));
	} else {
		if (arguments[0].constructor !== Array) {
			ids = [arguments[0]];
		} else {
			ids = arguments[0];
		}
	}

	this.$log.trace('Deleting ' + this.orm.table + ((ids !== undefined && ids.length === 1) ? '[' + ids[0] + '] entity' : ids.length + ' entities'));

	for (let i = 0; i < ids.length; i++) {

		let id = ids[i];
		//prevent implicit type convertion
		if (this.orm.getPrimaryKey().type.toUpperCase() !== 'VARCHAR')
			id = parseInt(id, 10);

		if (ids.length > 1)
			this.$log.trace('Deleting {}[{}] entity', this.orm.table, id);

		if (id === undefined || id === null) {
			throw new Error('Illegal argument for id parameter:' + id);
		}

		try {

			this.notify('beforeRemoveEntity', id);

			//first we attempt to remove depndents if any
			if (this.orm.associations) {
				//Remove associated dependencies
				for (const idx in Object.keys(this.orm.associations)) {
					const association = this.orm.associations[idx];
					const associationName = association['name'];
					if ([this.orm.ASSOCIATION_TYPES['MANY-TO-MANY'], this.orm.ASSOCIATION_TYPES['MANY-TO-ONE']].indexOf(association.type) < 0) {
						this.$log.trace("Inspecting {}[{}}] entity's dependency '{}' for entities to delete.", this.orm.table, id, associationName);
						const associationDAO = association.targetDao ? association.targetDao() : this;
						const settings = {};
						let joinId = id;
						//check if we are joining on field, other than pk
						if (association.key !== undefined) {
							var ctxEntity = this.find(id);
							joinId = ctxEntity[association.key];
						}
						settings[association.joinKey] = joinId;
						let associatedEntities;
						//associatedEntities = this.expand(associationName, id);
						associatedEntities = associationDAO.list(settings);
						if (associatedEntities && associatedEntities.length > 0) {
							this.$log.trace("Deleting {}[{}] entity's {} dependent {}", this.orm.table, id, associatedEntities.length, associationName);
							this.notify('beforeRemoveAssociationSet', associatedEntities, id);
							for (let j = 0; j < associatedEntities.length; j++) {
								const associatedEntity = associatedEntities[j];
								this.notify('beforeRemoveAssociationSetEntity', associatedEntity, associatedEntities, id);

								associationDAO.remove.apply(associationDAO, [associatedEntity[associationDAO.orm.getPrimaryKey().name]]);

							}
							this.$log.trace("{}[{}] entity's {} dependent {} {} deleted.", this.orm.table, id, associatedEntities.length, associationName, associatedEntities.length > 1 ? 'entities' : 'entity');
						}
					}
				}
			}
			//Delete by primary key value
			const parametericStatement = this.ormstatements["delete"].apply(this.ormstatements, [this.orm.getPrimaryKey().name]);
			let params = {};
			params[this.orm.getPrimaryKey().name] = id;

			const updatedRecordsCount = this.execute(parametericStatement, params);

			if (updatedRecordsCount > 0)
				this.$log.trace('{}[{}] entity deleted', this.orm.table, id);
			else
				this.$log.trace('No changes incurred in {}', this.orm.table);

		} catch (e) {
			this.$log.error('Deleting {}[{}] entity failed',this.orm.table, id, e);
			throw e;
		}

	}

};

DAO.prototype.expand = function (expansionPath, context) {
	let i;
	let settings;
	let key;
	let joinId;
	this.$log.trace('Expanding for association path {} and context entity {}', expansionPath, (typeof arguments[1] !== 'object' ? 'id ' : '') + JSON.stringify(arguments[1]));
	if (!expansionPath || !expansionPath.length) {
		throw new Error('Illegal argument: expansionPath[' + expansionPath + ']');
	}
	if (!context) {
		throw new Error('Illegal argument: context[' + context + ']');
	}
	const associationName = expansionPath.splice ? expansionPath.splice(0, 1)[0] : expansionPath;
	const association = this.orm.getAssociation(associationName);
	if (!associationName || !association)
		throw new Error('Illegal argument: Unknown association for this DAO [' + associationName + ']');
	const joinKey = association.joinKey;

	let contextEntity;
	if (context[this.orm.getPrimaryKey().name] !== undefined) {
		contextEntity = context;
	} else {
		contextEntity = this.find(context);
	}

	if (!contextEntity) {
		throw Error('No record found for context entity [' + context + ']');
	}

	const associationTargetDAO = association.targetDao ? association.targetDao.apply(this) : this;
	if (!associationTargetDAO)
		throw Error('No target association DAO instance available for association ' + associationName);

	let expansion;
	let associationEntities = [];

	if (association.type === this.orm.ASSOCIATION_TYPES['ONE-TO-ONE'] || association.type === this.orm.ASSOCIATION_TYPES['MANY-TO-ONE']) {
		joinId = contextEntity[joinKey];
		this.$log.trace('Expanding association type {} on {}[{}]', association.type, joinKey, joinId);
		if (!association.key || association.key === associationTargetDAO.orm.getPrimaryKey().name)
			expansion = associationTargetDAO.find.apply(associationTargetDAO, [joinId]);
		else {
			let listSettings = {};
			listSettings["$filter"] = association.key;
			listSettings[association.key] = joinId;
			expansion = associationTargetDAO.list.apply(associationTargetDAO, [listSettings])[0];
		}

		if (expansionPath.length > 0) {
			this.expand(expansionPath, expansion);
		}
	} else if (association.type === this.orm.ASSOCIATION_TYPES['ONE-TO-MANY']) {
		settings = {};
		if (association.defaults)
			settings = association.defaults;
		key = association.key || this.orm.getPrimaryKey().name;
		joinId = contextEntity[key];
		this.$log.trace('Expanding association type {} on {}[{}]', association.type, joinKey, joinId);
		settings[joinKey] = joinId;
		associationEntities = associationEntities.concat(associationTargetDAO.list.apply(associationTargetDAO, [settings]));

		if (expansionPath.length > 0) {
			for (i = 0; i < associationEntities.length; i++) {
				this.expand(expansionPath, associationEntities[i]);
			}
		} else {
			expansion = associationEntities;
		}
	} else if (association.type === this.orm.ASSOCIATION_TYPES['MANY-TO-MANY']) {
		const joinDAO = association.joinDao();
		if (!joinDAO)
			throw Error('No join DAO instance available for association ' + associationName);
		if (!joinDAO.listJoins)
			throw Error('No listJoins function in join DAO instance available for association ' + associationName);
		settings = {};
		key = association.key || this.orm.getPrimaryKey().name;
		joinId = contextEntity[key];
		settings[association.joinKey] = joinId;
		associationEntities = associationEntities.concat(joinDAO.listJoins.apply(joinDAO, [settings, { "sourceDao": this, "joinDao": joinDAO, "targetDao": associationTargetDAO }]));
		if (expansionPath.length > 0) {
			for (i = 0; i < associationEntities.length; i++) {
				this.expand(expansionPath, associationEntities[i]);
			}
		} else {
			expansion = associationEntities;
		}
	}
	return expansion;
};

/**
 * Reads a single entity by id, parsed into JSON object.
 * If requested as expanded the returned entity will comprise associated (dependent) entities too. 
 * Expand can be a string tha tis a valid association name defined in this dao orm or an array of such names.
 */
DAO.prototype.find = function (id, expand, select) {
	if (typeof arguments[0] === 'object') {
		id = arguments[0].id;
		expand = arguments[0].$expand || arguments[0].expand;
		select = arguments[0].$select || arguments[0].select;
	}

	this.$log.trace('Finding {}[{}] entity with list parameters expand[{}], select[{}]', this.orm.table, id, expand, select);

	if (id === undefined || id === null) {
		throw new Error('Illegal argument for id parameter:' + id);
	}

	try {
		let entity;
		if (select !== undefined) {
			if (select.constructor !== Array) {
				if (select.constructor === String) {
					select = select.split(',').map(function (sel) {
						if (sel.constructor !== String)
							throw Error('Illegal argument: select array components are expected ot be strings but found ' + (typeof sel));
						return sel.trim();
					});
				} else {
					throw Error('Illegal argument: select is expected to be string or array of strings but was ' + (typeof select));
				}
			}
		}
		//ensure that joinkeys for required expands are available and not filtered by select
		if (select !== undefined && expand !== undefined) {
			select.push(this.orm.getPrimaryKey().name);
			//TODO: checks
			/*for(var i in expand){
				var association = this.orm.associations[expand[i]];
				if(association && select.indexOf(association.joinKey)<1){
					select.push(association.joinKey);
				}
			}*/
		}
		let findQbParams = {
			select: select
		};
		const parametericStatement = this.ormstatements.find.apply(this.ormstatements, [findQbParams]);
		let params = {};

		//prevent implicit type convertion
		if (this.orm.getPrimaryKey().type.toUpperCase() !== 'VARCHAR')
			id = parseInt(id, 10);

		params[this.orm.getPrimaryKey().name] = id;
		const resultSet = this.execute(parametericStatement, params);

		if (resultSet[0]) {
			entity = this.createEntity(resultSet[0], select);
			if (entity) {
				this.$log.trace('{}[{}] entity found', this.orm.table, id);
				this.notify('afterFound', entity);
				if (expand !== undefined) {
					if (expand.constructor !== Array) {
						if (expand.constructor === String) {
							expand = String(expand);
							expand = expand.split(',').map(function (exp) {
								if (exp.constructor !== String)
									throw Error('Illegal argument: expand array components are expected ot be strings but found ' + (typeof exp));
								return exp.trim();
							});
						} else {
							throw Error('Illegal argument: expand is expected to be string or array of strings but was ' + (typeof expand));
						}
					}
					var associationNames = this.orm.getAssociationNames();
					for (var idx in associationNames) {
						var associationName = associationNames[idx];
						if (expand.indexOf(associationName) > -1) {
							entity[associationName] = this.expand([associationName], entity);
						}
					}
				}
			} else {
				this.$log.trace('{}[{}] entity not found', this.orm.table, id);
			}
		}
		return entity;
	} catch (e) {
		this.$log.error("Finding {}[{}] entity failed.", this.orm.table, id, e);
		throw e;
	}
};

DAO.prototype.count = function (settings?) {
	settings = settings || {};

	const parametericStatement = this.ormstatements.count.apply(this.ormstatements, [settings]);
	this.$log.trace('Counting ' + this.orm.table + ' entities');

	let count = 0;
	try {
		const rs = this.execute(parametericStatement, settings);
		if (rs.length > 0) {
			//expectaion is that there is a single object in the result set with a single porperty
			const key = Object.keys(rs[0])[0];
			count = parseInt(rs[0][key], 10);
		}
	} catch (e) {
		this.$log.error('Counting {} entities failed', this.orm.table, e);
		e.errContext = parametericStatement.toString();
		throw e;
	}

	this.$log.trace('{} {} entities counted', String(count), this.orm.table);

	return count;
};

/*
 * list parameters:
 * - $expand
 * - $filter
 * - $select
 * - $sort
 * - $order
 * - $limit
 * - $offset
 */
DAO.prototype.list = function (settings) {

	let key;
	settings = settings || {};

	const expand = settings.$expand || settings.expand;
	if (expand !== undefined) {
		if (expand.constructor !== Array) {
			if (expand.constructor === String) {
				if (expand.indexOf(',') > -1) {
					settings.$expand = expand.split(',').map(function (exp) {
						if (exp.constructor !== String)
							throw Error('Illegal argument: expand array components are expected ot be strings but found ' + (typeof exp));
						return exp.trim();
					});
				} else {
					settings.$expand = [expand];
				}
			} else {
				throw Error('Illegal argument: expand is expected to be string or array of strings but was ' + (typeof expand));
			}
		}
	}

	const select = settings.$select || settings.select;
	if (select !== undefined) {
		if (select.constructor !== Array) {
			if (select.constructor === String) {
				if (select.indexOf(',') > -1) {
					settings.$select = select.split(',').map(function (exp) {
						if (exp.constructor !== String)
							throw Error('Illegal argument: select array components are expected ot be strings but found ' + (typeof exp));
						return exp.trim();
					});
				} else {
					settings.$select = [select];
				}
			} else {
				throw Error('Illegal argument: select is expected to be string or array of strings but was ' + (typeof expand));
			}
		}
	}


	const listArgs = [];
	for (key in settings) {
		listArgs.push(' ' + key + '[' + settings[key] + ']');
	}

	this.$log.trace('Listing {} entity collection with list operators: {}', this.orm.table, listArgs.join(','));

	if (settings.$select !== undefined && expand !== undefined) {
		settings.$select.push(this.orm.getPrimaryKey().name);
	}

	var parametericStatement = this.ormstatements.list.apply(this.ormstatements, [settings]);

	try {
		let entities = [];

		const resultSet = this.execute(parametericStatement, settings);

		resultSet.forEach(function (rsEntry) {
			var entity = this.createEntity(rsEntry, settings.$select);
			if (expand) {
				var associationNames = this.orm.getAssociationNames();
				for (var idx = 0; idx < associationNames.length; idx++) {
					var associationName = associationNames[idx];
					if (expand.indexOf(associationName) > -1) {
						entity[associationName] = this.expand([associationName], entity);
					}
				}
			}
			this.notify('afterFound', entity, settings);
			entities.push(entity);
		}.bind(this));

		this.$log.trace('{} {} entities found', entities.length, this.orm.table);

		return entities;
	} catch (e) {
		this.$log.error("Listing {} entities failed.", this.orm.table, e);
		throw e;
	}
};

DAO.prototype.existsTable = function () {
	this.$log.trace('Check exists table ' + this.orm.table);
	try {
		const parametericStatement = this.ormstatements.count.apply(this.ormstatements);
		const rs = this.execute(parametericStatement);
		return rs.length > 0;
	} catch (e) {
		return false;
	}
};

DAO.prototype.createTable = function () {
	this.$log.trace('Creating table {}', this.orm.table);
	const parametericStatement = this.ormstatements.createTable.apply(this.ormstatements);
	try {
		this.execute(parametericStatement);
		this.$log.trace('{} table created', this.orm.table);
		return this;
	} catch (e) {
		this.$log.error("Create table {} failed", this.orm.table, e);
		throw e;
	}
};

DAO.prototype.dropTable = function (dropIdSequence) {
	this.$log.trace('Dropping table {}.', this.orm.table);
	const parametericStatement = this.ormstatements.dropTable.apply(this.ormstatements);
	try {
		this.execute(parametericStatement);
		this.$log.trace('Table {} dropped.', this.orm.table);
	} catch (e) {
		this.$log.error("Dropping table {} failed.", this.orm.table, e);
		throw e;
	}

	if (dropIdSequence) {
		this.$log.trace('Dropping table {} sequence {}.', this.orm.table, this.sequenceName);
		try {
			this.dropIdGenerator();
			this.$log.trace('Table {} sequence {} dropped.', this.orm.table, this.sequenceName);
		} catch (e) {
			this.$log.error("Dropping table {} sequence {} failed.", this.orm.table, this.sequenceName, e);
			throw e;
		}
	}

	return this;
};


/**
 * oDefinition can be table definition or standard orm definition object. Or it can be a valid path to
 * a .table file, or any other text file contianing a standard dao orm definition.
 */
export function create(oDefinition, logCtxName?: string, dataSourceName?: string) {
	let orm;
	orm = oDefinition;

	if (!dataSourceName || dataSourceName === null) {
		dataSourceName = configurations.get("DIRIGIBLE_DATABASE_DATASOURCE_NAME_DEFAULT", "DefaultDB");
	}

	let productName = globals.get(dataSourceName);
	if (!productName) {
		productName = Database.getProductName(dataSourceName);
		globals.set(dataSourceName, productName);
	}

	//	}
	return new DAO(orm, logCtxName, dataSourceName);
};

export function dao(oDefinition, logCtxName?: string, dataSourceName?: string) {
	return create(oDefinition, logCtxName, dataSourceName)
}
