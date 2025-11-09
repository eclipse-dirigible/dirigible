"use strict";

/**
 * Supported ORM schema:
 * ```
 * {
 *	 name: <string>,
 *   table: <string>,
 *   properties: [{
 *	   name: <string>,
 *	   column: <string>,
 *	   id: <boolean>,
 *	   required: <boolean>,
 *	   unique: <boolean>,
 *	   dbValue: <function>,
 *	   value: <function>,
 *	   allowedOps: <Array['insert','update']>
 *   }],
 * 	 associations: [{
 *	   name: <string>,
 *     joinKey: <string>,
 *     key: <string>,
 *     type: <ORM.ASSOCIATION_TYPES>,
 *     targetDao: <function|DAO>,
 *     joinDao: <function|DAO>,
 *     defaults: <Object>,
 *   }]
 * }
 * ```
 *
 */

export interface ORMDefinition {
    table: string,
    properties: {
        name: string,
        column: string,
        type: string,
        id?: boolean,
        required?: boolean,
        unique?: boolean,
        autoIncrement?: boolean,
        allowedOps?: ('insert' | 'update')[]
    }[]
}

export function ORM(orm: ORMDefinition) {
	this.orm = orm;
	for (var i in orm) {
		this[i] = orm[i];
	}
};

ORM.prototype.ASSOCIATION_TYPES = Object.freeze({
	"ONE-TO-ONE": "one-to-one",
	"ONE-TO-MANY": "one-to-many",
	"MANY-TO-MANY": "many-to-many",
	"MANY-TO-ONE": "many-to-one"
});
ORM.prototype.ASSOCIATION_TYPES_VALUES = Object.keys(ORM.prototype.ASSOCIATION_TYPES)
	.map(function (key) {
		return ORM.prototype.ASSOCIATION_TYPES[key];
	});

ORM.prototype.getPrimaryKey = function () {
	if (!this.idProperty) {
		if (!this.properties || !this.properties.length)
			throw new Error('Invalid orm configuration - no properties are defined');
		const id = this.properties.filter(function (property) {
			return property.id;
		});
		if (!id.length)
			throw new Error('Invalid orm configuration - no id property is defined');
		this.idProperty = id[0];
	}
	return this.idProperty;
};

ORM.prototype.isAutoIncrementPrimaryKey = function () {
	if (!this.autoIncrementPrimaryKeyProperty) {
		if (!this.properties || !this.properties.length)
			throw new Error('Invalid orm configuration - no properties are defined');
		let isAutoIncrementOldVersion = false;
		const autoIncrementPrimaryKeyProperty = this.properties.filter(function (property) {
			if (property.id && (property.autoIncrement === undefined || property.autoIncrement === null)) {
				isAutoIncrementOldVersion = true;
			}
			return property.id && (property.autoIncrement || isAutoIncrementOldVersion);
		});
		this.autoIncrementPrimaryKeyProperty = autoIncrementPrimaryKeyProperty.length > 0;
	}
	return this.autoIncrementPrimaryKeyProperty;
};

ORM.prototype.getProperty = function (name) {
	if (name === undefined)
		throw new Error('Illegal argument: name[' + name + ']');
	if (!this.properties || !this.properties.length)
		throw new Error('Invalid orm configuration - no properties are defined');
	const property = this.properties.filter(function (property) {
		return property.name === name;
	});
	return property.length > 0 ? property[0] : undefined;
};

ORM.prototype.getMandatoryProperties = function () {
	if (!this.mandatoryProperties) {
		if (!this.properties || !this.properties.length)
			throw new Error('Invalid orm configuration - no properties are defined');
		const mandatories = this.properties.filter(function (property) {
			return property.required;
		});
		this.mandatoryProperties = mandatories;
	}
	return this.mandatoryProperties;
};

ORM.prototype.getOptionalProperties = function () {
	if (!this.optionalProperties) {
		if (!this.properties || !this.properties.length)
			throw new Error('Invalid orm configuration - no properties are defined');
		const mandatories = this.properties.filter(function (property) {
			return !property.required;
		});
		this.optionalProperties = mandatories;
	}
	return this.optionalProperties;
};

ORM.prototype.getUniqueProperties = function () {
	if (!this.uniqueProperties) {
		if (!this.properties || !this.properties.length)
			throw new Error('Invalid orm configuration - no properties are defined');
		const uniques = this.properties.filter(function (property) {
			return property.unique;
		});
		this.uniqueProperties = uniques;
	}
	return this.uniqueProperties;
};

//TODO: remove this or improve by key type.
ORM.prototype.associationKeys = function () {
	let keys = [];
	if (this.associations) {
		keys = this.associations.map(function (assoc) {
			return assoc.joinKey;
		});
	}
	return keys;
};

ORM.prototype.getAssociationNames = function () {
	let names = [];
	if (this.associations) {
		names = this.associations.map(function (assoc) {
			return assoc.name;
		});
	}
	return names;
};

ORM.prototype.getAssociation = function (associationName) {
	if (this.associations) {
		return this.associations
			.filter(function (assoc) {
				return assoc.name === associationName;
			})[0];
	}
	return;
};

ORM.prototype.validate = function () {
	let i;
	if (!this.table)
		throw new Error('Illegal configuration: invalid property table[' + this.table + ']');
	if (!this.properties)
		throw new Error('Illegal configuration: invalid property properties[' + this.properties + ']');
	if (this.properties.constructor !== Array)
		throw new Error("Illegal configuration: property 'properties' type is expected ot be Array. Instead, it was " + (typeof this.properties));
	if (!this.getPrimaryKey())
		throw new Error('Illegal configuration: No primary key specifed');
	for (i = 0; i < this.properties.length; i++) {
		const property = this.properties[i];
		if (!property.name)
			throw new Error('Illegal configuration: invalid property name[' + property.name + ']');
		if (!property.type)
			throw new Error('Illegal configuration: invalid property type[' + property.type + ']');
		if (!property.column)
			throw new Error('Illegal configuration: invalid property column[' + property.column + ']');
		if (property.allowedOps) {
			if (property.allowedOps.constructor !== Array)
				throw new Error("Illegal configuration: property[" + property.name + "]  allowedOps is expected ot be Array. Instead, it was " + (typeof property.allowedOps));
			for (let j = 0; j < property.allowedOps.length; j++) {
				if (['insert', 'update'].indexOf(property.allowedOps[j]) < 0)
					throw new Error("Illegal configuration: property[" + property.name + "] allowedOps[" + property.allowedOps + "] must be an array containing some or all of the following values: ['insert','update']");
			}
		}
	}
	if (this.associations) {
		if (this.associations.constructor !== Array)
			throw new Error("Illegal configuration: property 'associations' type is expected ot be Array. Instead, it was " + (typeof this.associations));
		for (i = 0; i < this.associations.length; i++) {
			const association = this.associations[i];
			if (!association.name)
				throw new Error("Illegal configuration: Association property name[" + association.name + "]");
			if (ORM.prototype.ASSOCIATION_TYPES_VALUES.indexOf(association.type) < 0)
				throw new Error("Illegal configuration: Association " + association.name + " property type[" + association.type + "] must be one of " + ORM.prototype.ASSOCIATION_TYPES_VALUES);
			if (!association.joinKey && 'many-to-one' !== association.type)
				throw new Error('Illegal configuration: invalid association joinKey[' + association.joinKey + ']');
			if (association.targetDao && association.targetDao.constructor !== Function)
				throw new Error('Invalid configuration: Association ' + association.name + ' targetDao property is expected to be function. Instead, it is: ' + (typeof association.targetDao));
			if (association.type === ORM.prototype.ASSOCIATION_TYPES['MANY-TO-MANY']) {
				if (!association.joinDao)
					throw new Error('Illegal configuration: Association ' + association.name + ' joinDao[' + association.joinDao + '] value');
				if (association.joinDao && association.joinDao.constructor !== Function)
					throw new Error('Invalid configuration: Association ' + association.name + ' joinDao property is expected to be function. Instead, it is: ' + (typeof association.joinDao));
			}
		}
	}
};

ORM.prototype.toColumn = function (ormProperty) {
	let column;
	if (ormProperty) {
		column = {
			name: "\"" + ormProperty.column + "\"",
			type: ormProperty.type,
			length: String(ormProperty.length),
			primaryKey: String(ormProperty.id === undefined ? false : ormProperty.id),
			nullable: String(!ormProperty.required),
			defaultValue: ormProperty.defaultValue
		};
	}
	return column;
};

ORM.prototype.toTable = function () {
	let table = {
		name: this.table,
		type: "TABLE",
		columns: null,
		constraints: null,
	};

	const uniqueIndices = [];
	const primaryKeys = [];
	if (this.properties) {
		table.columns = [];
		this.properties.forEach(function (property) {
			var column = this.toColumn(property);
			if (property.unique)
				uniqueIndices.push({
					"columns": column.name
				});
			if (property.id)
				primaryKeys.push({
					"columns": column.name
				});
			//TODO : analyze associations to add FKs if possible
			table.columns.push(column);
		});
	}

	if (primaryKeys.length > 1 || uniqueIndices.length > 0) {
		table["constraints"] = {};
	}

	if (primaryKeys.length > 1) {
		table.columns = table.columns.map(function (column) {
			column.primaryKey = "false";
			return column;
		});
		table.constraints["primarKeys"] = primaryKeys;
	}
	if (uniqueIndices.length > 0)
		table.constraints["uniqueIndices"] = uniqueIndices;

	return table;
};

export function get(orm: ORMDefinition) {
	const _orm = new ORM(orm);
	_orm.validate();
	return _orm;
};
