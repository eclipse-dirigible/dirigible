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
// Defines the entity user object
function Entity(name) {
	this.name = name;
}

Entity.prototype.entityType = 'PRIMARY'; // the type of the entity - PRIMARY, DEPENDENT, REPORT
Entity.prototype.dataName = null;
Entity.prototype.dataQuery = null; // database query in case of a report type of the entity
Entity.prototype.dataCount = null; // database query for the count of the entities
Entity.prototype.title = '';
Entity.prototype.caption = '';
Entity.prototype.description = '';
Entity.prototype.tooltip = '';
Entity.prototype.icon = '/services/web/resources/unicons/file.svg';
Entity.prototype.menuLabel = ''; // the visible name of the menu
Entity.prototype.menuIndex = 100;
Entity.prototype.layoutType = 'MANAGE';
Entity.prototype.perspectiveName = 'entities';
Entity.prototype.perspectiveLabel = 'Entities';
Entity.prototype.perspectiveHeader = '';
Entity.prototype.perspectiveNavId = '';
Entity.prototype.perspectiveIcon = '/services/web/resources/unicons/copy.svg';
Entity.prototype.perspectiveOrder = 100;
Entity.prototype.perspectiveRole = '';
Entity.prototype.feedUrl = null;
Entity.prototype.feedUsername = null;
Entity.prototype.feedPassword = null;
Entity.prototype.feedSchedule = null;
Entity.prototype.feedPath = null;
Entity.prototype.roleRead = null;
Entity.prototype.roleWrite = null;
Entity.prototype.generateDefaultRoles = false;
Entity.prototype.projectionReferencedModel = null;
Entity.prototype.projectionReferencedEntity = null;
Entity.prototype.referenceProjections = [];
Entity.prototype.importsCode = null;
Entity.prototype.generateReport = false;
Entity.prototype.multilingual = false;

Entity.prototype.clone = function () {
	return mxUtils.clone(this);
};

// Defines the property user object
function Property(name) {
	this.name = name;
}
Property.prototype.description = '';
Property.prototype.tooltip = '';
Property.prototype.isRequiredProperty = false;
Property.prototype.isCalculatedProperty = false;
Property.prototype.auditType = 'NONE';
Property.prototype.calculatedPropertyExpressionCreate = null;
Property.prototype.calculatedPropertyExpressionUpdate = null;
Property.prototype.dataName = null;
Property.prototype.dataType = 'VARCHAR';
Property.prototype.dataLength = '20';
Property.prototype.dataDefaultValue = null;
Property.prototype.dataPrimaryKey = 'false';
Property.prototype.dataAutoIncrement = 'false';
Property.prototype.dataNotNull = 'false';
Property.prototype.dataUnique = 'false';
Property.prototype.dataPrecision = null;
Property.prototype.dataScale = null;
Property.prototype.relationshipType = null;
Property.prototype.relationshipCardinality = null;
Property.prototype.relationshipName = null;
Property.prototype.relationshipEntityName = null;
Property.prototype.relationshipEntityPerspectiveName = null;
Property.prototype.relationshipEntityPerspectiveLabel = null;
Property.prototype.widgetType = 'TEXTBOX';
Property.prototype.widgetLength = '20';
Property.prototype.widgetPattern = null; // the input validation pattern
Property.prototype.widgetService = null; // the service used to fill in the widget if any
Property.prototype.widgetIsMajor = 'true'; // whether this property will be shown in e.g. a list of entities table
Property.prototype.widgetSection = null; // the name of the grouping section
Property.prototype.widgetLabel = null; // the regular form label
Property.prototype.widgetShortLabel = null; // a short label for limited character places e.g. table headers
Property.prototype.widgetFormat = null; // the format for rendering
Property.prototype.widgetDropDownKey = null; // the key property in drop down case
Property.prototype.widgetDropDownValue = null; // the value property in drop down case
Property.prototype.widgetDependsOnProperty = null; // the property in depends on case
Property.prototype.widgetDependsOnEntity = null; // the entity in depends on case
Property.prototype.widgetDependsOnValueFrom = null; // the value from in depends on case
Property.prototype.widgetDependsOnFilterBy = null; // the filter by in drop down case
Property.prototype.feedPropertyName = null; // the matching property name from the feed
Property.prototype.roleRead = null; // the matching property name from the read role
Property.prototype.roleWrite = null; // the matching property name from the write role

Property.prototype.clone = function () {
	return mxUtils.clone(this);
};

// Defines the connector user object
function Connector(name) {
	this.name = name;
}
