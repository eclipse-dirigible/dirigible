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
function createModel(graph) {
	let model = [];
	model.push('<model>\n');
	model.push(' <entities>\n');
	let parent = graph.getDefaultParent();
	let childCount = graph.model.getChildCount(parent);

	for (let i = 0; i < childCount; i++) {
		let child = graph.model.getChildAt(parent, i);

		if (!graph.model.isEdge(child)) {
			child.value.dataName = child.value.dataName ? child.value.dataName : JSON.stringify(child.value.name).replace(/\W/g, '').toUpperCase();
			child.value.dataCount = child.value.dataCount ? child.value.dataCount : "SELECT COUNT(*) AS COUNT FROM \"${tablePrefix}" + JSON.stringify(child.value.name).replace(/\W/g, '').toUpperCase() + "\"";
			child.value.title = child.value.title ? child.value.title : child.value.name;
			child.value.caption = child.value.caption ? child.value.caption : "Manage entity " + child.value.name;
			child.value.description = child.value.description ? child.value.description : "Manage entity " + child.value.name;
			child.value.tooltip = child.value.tooltip ? child.value.tooltip : child.value.name;
			child.value.menuKey = child.value.menuKey ? child.value.menuKey : JSON.stringify(child.value.name).replace(/\W/g, '').toLowerCase();
			child.value.menuLabel = child.value.menuLabel ? child.value.menuLabel : child.value.name;
			let entityContent = '  <entity name="' + _.escape(child.value.name) +
				'" dataName="' + _.escape(child.value.dataName) +
				'" dataCount="' + _.escape(child.value.dataCount) +
				'" dataQuery="' + _.escape(child.value.dataQuery) +
				'" type="' + _.escape(child.value.entityType ? child.value.entityType : 'PRIMARY') +
				'" title="' + _.escape(child.value.title) +
				'" caption="' + _.escape(child.value.caption) +
				'" description="' + _.escape(child.value.description) +
				'" tooltip="' + _.escape(child.value.tooltip) +
				'" icon="' + _.escape(child.value.icon) +
				'" menuKey="' + _.escape(child.value.menuKey) +
				'" menuLabel="' + _.escape(child.value.menuLabel) +
				'" menuIndex="' + _.escape(child.value.menuIndex) +
				'" layoutType="' + _.escape(child.value.layoutType) +
				'" perspectiveName="' + _.escape(child.value.perspectiveName) +
				'" perspectiveLabel="' + getPerspectiveLabel(graph, child) +
				'" perspectiveHeader="' + getPerspectiveHeader(graph, child) +
				'" perspectiveIcon="' + getPerspectiveIcon(graph, child) +
				'" perspectiveOrder="' + getPerspectiveOrder(graph, child) +
				'" perspectiveNavId="' + getPerspectiveNavId(graph, child) +
				'" perspectiveRole="' + getPerspectiveRole(graph, child) +
				'" generateReport="' + _.escape(child.value.generateReport) +
				'" multilingual="' + _.escape(child.value.multilingual) +
				'" generateDefaultRoles="' + _.escape(child.value.generateDefaultRoles) + '"';

			if (child.value.feedUrl && child.value.feedUrl !== "") {
				child.value.feedUrl = btoa(child.value.feedUrl);
				entityContent += ' feedUrl="' + child.value.feedUrl + '"';
			}
			if (child.value.feedUsername && child.value.feedUsername !== "") {
				child.value.feedUsername = btoa(child.value.feedUsername);
				entityContent += ' feedUsername="' + child.value.feedUsername + '"';
			}
			if (child.value.feedPassword && child.value.feedPassword !== "") {
				child.value.feedPassword = btoa(child.value.feedPassword);
				entityContent += ' feedPassword="' + child.value.feedPassword + '"';
			}
			if (child.value.feedSchedule && child.value.feedSchedule !== "") {
				entityContent += ' feedSchedule="' + child.value.feedSchedule + '"';
			}
			if (child.value.feedPath && child.value.feedPath !== "") {
				entityContent += ' feedPath="' + child.value.feedPath + '"';
			}
			if (child.value.roleRead && child.value.roleRead !== "") {
				entityContent += ' roleRead="' + child.value.roleRead + '"';
			}
			if (child.value.roleWrite && child.value.roleWrite !== "") {
				entityContent += ' roleWrite="' + child.value.roleWrite + '"';
			}
			if (child.value.projectionReferencedModel && child.value.projectionReferencedModel !== "") {
				entityContent += ' projectionReferencedModel="' + child.value.projectionReferencedModel + '"';
			}
			if (child.value.projectionReferencedEntity && child.value.projectionReferencedEntity !== "") {
				entityContent += ' projectionReferencedEntity="' + child.value.projectionReferencedEntity + '"';
			}
			if (child.value.importsCode && child.value.importsCode !== "") {
				child.value.importsCode = btoa(child.value.importsCode);
				entityContent += ' importsCode="' + child.value.importsCode + '"';
			}

			entityContent += '>\n';
			model.push(entityContent);

			let propertyCount = graph.model.getChildCount(child);
			if (propertyCount > 0) {
				for (let j = 0; j < propertyCount; j++) {
					let property = graph.model.getChildAt(child, j).value;

					property.dataName = property.dataName ? property.dataName : _.escape(child.value.dataName).toUpperCase() + "_" + JSON.stringify(property.name).replace(/\W/g, '').toUpperCase();

					model.push('    <property name="' + _.escape(property.name) +
						'" description="' + _.escape(property.description) +
						'" tooltip="' + _.escape(property.tooltip) +
						'" dataName="' + _.escape(property.dataName) +
						'" dataType="' + _.escape(property.dataType) + '"');
					if (property.dataOrderBy) {
						model.push(' dataOrderBy="' + _.escape(property.dataOrderBy) + '"');
					}
					if (property.dataLength !== null && (property.dataType === 'CHAR' || property.dataType === 'VARCHAR')) {
						model.push(' dataLength="' + _.escape(property.dataLength) + '"');
					}
					if (property.dataNotNull) {
						model.push(' dataNullable="' + (property.dataNotNull == "false") + '"');
					} else {
						model.push(' dataNullable="true"');
					}
					if (property.dataPrimaryKey == "true") {
						model.push(' dataPrimaryKey="true"');
					}
					if (property.dataAutoIncrement == "true") {
						model.push(' dataAutoIncrement="true"');
					}
					if (property.dataUnique == "true") {
						model.push(' dataUnique="true"');
					}
					if (property.dataDefaultValue !== null) {
						model.push(' dataDefaultValue="' + _.escape(property.dataDefaultValue) + '"');
					}
					if (property.dataPrecision !== null && property.dataType === 'DECIMAL') {
						model.push(' dataPrecision="' + _.escape(property.dataPrecision) + '"');
					}
					if (property.dataScale !== null && property.dataType === 'DECIMAL') {
						model.push(' dataScale="' + _.escape(property.dataScale) + '"');
					}
					if (property.dataPrecision !== null && property.dataScale !== null && property.dataType === 'DECIMAL') {
						const dataLength = parseInt(property.dataPrecision);
						model.push(' dataLength="' + dataLength + '"');
					}
					if (property.isRequiredProperty == "true") {
						model.push(' isRequiredProperty="true"');
					}
					if (property.isCalculatedProperty == "true") {
						model.push(' isCalculatedProperty="true"');
					}
					if (property.calculatedPropertyExpressionCreate != null) {
						model.push(' calculatedPropertyExpressionCreate="' + _.escape(property.calculatedPropertyExpressionCreate) + '"');
					}
					if (property.calculatedPropertyExpressionUpdate != null) {
						model.push(' calculatedPropertyExpressionUpdate="' + _.escape(property.calculatedPropertyExpressionUpdate) + '"');
					}
					if (property.relationshipType !== null) {
						model.push(' relationshipType="' + _.escape(property.relationshipType ? property.relationshipType : 'ASSOCIATION') + '"');
					}
					if (property.relationshipCardinality !== null) {
						model.push(' relationshipCardinality="' + _.escape(property.relationshipCardinality ? property.relationshipCardinality : '1_n') + '"');
					}
					if (property.relationshipName !== null) {
						model.push(' relationshipName="' + _.escape(property.relationshipName) + '"');
					}
					if (property.widgetType !== null) {
						model.push(' widgetType="' + _.escape(property.widgetType) + '"');
					}
					if (property.widgetSize !== null) {
						model.push(' widgetSize="' + _.escape(property.widgetSize) + '"');
					}
					if (property.widgetLength !== null) {
						model.push(' widgetLength="' + _.escape(property.widgetLength) + '"');
					}
					if (property.widgetLabel !== null) {
						model.push(' widgetLabel="' + _.escape(property.widgetLabel) + '"');
					}
					if (property.widgetShortLabel !== null) {
						model.push(' widgetShortLabel="' + _.escape(property.widgetShortLabel) + '"');
					}
					if (property.widgetPattern !== null) {
						model.push(' widgetPattern="' + _.escape(property.widgetPattern) + '"');
					}
					if (property.widgetFormat !== null) {
						model.push(' widgetFormat="' + _.escape(property.widgetFormat) + '"');
					}
					if (property.widgetSection !== null) {
						model.push(' widgetSection="' + _.escape(property.widgetSection) + '"');
					}
					if (property.widgetService !== null) {
						model.push(' widgetService="' + _.escape(property.widgetService) + '"');
					}
					if (property.widgetIsMajor) {
						model.push(' widgetIsMajor="' + (property.widgetIsMajor == "true") + '"');
					} else {
						model.push(' widgetIsMajor="false"');
					}
					if (property.feedPropertyName !== null) {
						model.push(' feedPropertyName="' + _.escape(property.feedPropertyName) + '"');
					}
					if (property.roleRead !== null) {
						model.push(' roleRead="' + _.escape(property.roleRead) + '"');
					}
					if (property.roleWrite !== null) {
						model.push(' roleWrite="' + _.escape(property.roleWrite) + '"');
					}
					if (property.widgetDropDownKey !== null) {
						model.push(' widgetDropDownKey="' + _.escape(property.widgetDropDownKey) + '"');
					}
					if (property.widgetDropDownValue !== null) {
						model.push(' widgetDropDownValue="' + _.escape(property.widgetDropDownValue) + '"');
					}
					if (property.widgetDropDownMultiSelect === "true" || property.widgetDropDownMultiSelect === "false") {
						model.push(' widgetDropDownMultiSelect="' + _.escape(property.widgetDropDownMultiSelect) + '"');
					}
					if (property.widgetDependsOnProperty !== null) {
						model.push(' widgetDependsOnProperty="' + _.escape(property.widgetDependsOnProperty) + '"');
					}
					if (property.widgetDependsOnEntity !== null) {
						model.push(' widgetDependsOnEntity="' + _.escape(property.widgetDependsOnEntity) + '"');
					}
					if (property.widgetDependsOnValueFrom !== null) {
						model.push(' widgetDependsOnValueFrom="' + _.escape(property.widgetDependsOnValueFrom) + '"');
					}
					if (property.widgetDependsOnFilterBy !== null) {
						model.push(' widgetDependsOnFilterBy="' + _.escape(property.widgetDependsOnFilterBy) + '"');
					}
					if (property.relationshipEntityPerspectiveName !== null) {
						model.push(' relationshipEntityPerspectiveName="' + _.escape(property.relationshipEntityPerspectiveName) + '"');
					}
					if (property.relationshipEntityPerspectiveLabel !== null) {
						model.push(' relationshipEntityPerspectiveLabel="' + _.escape(property.relationshipEntityPerspectiveLabel) + '"');
					}

					model.push('></property>\n');
				}
			}
			model.push('  </entity>\n');
		} else {
			let relationName = child.name ? child.name : child.source.parent.value.name + '_' + child.target.parent.value.name;
			model.push('  <relation name="' + _.escape(child.source.parent.value.name) + '_'
				+ _.escape(child.target.parent.value.name) + '" type="relation" ');
			model.push('entity="' + _.escape(child.source.parent.value.name) + '" ');
			model.push('relationName="' + _.escape(relationName) + '" ');
			model.push('relationshipEntityPerspectiveName="' + _.escape(child.target.parent.value.perspectiveName) + '" ');
			model.push('relationshipEntityPerspectiveLabel="' + _.escape(child.target.parent.value.perspectiveLabel) + '" ');
			model.push('property="' + _.escape(child.source.value.name) + '" ' +
				'referenced="' + _.escape(child.target.parent.value.name) + '" ' +
				'referencedProperty="' + _.escape(child.target.value.name) + '">\n');
			model.push('  </relation>\n');
		}
	}

	model.push(' </entities>\n');

	if (graph.getModel().perspectives) {
		model.push(' <perspectives>\n');
		for (let i = 0; i < graph.getModel().perspectives.length; i++) {
			let perspective = '  <perspective>';
			perspective += `<name>${_.escape(graph.getModel().perspectives[i].id)}</name>`;
			perspective += `<label>${_.escape(graph.getModel().perspectives[i].label)}</label>`;
			if (graph.getModel().perspectives[i].header) perspective += `<header>${_.escape(graph.getModel().perspectives[i].header)}</header>`;
			if (graph.getModel().perspectives[i].navId) perspective += `<navId>${_.escape(graph.getModel().perspectives[i].navId)}</navId>`;
			if (graph.getModel().perspectives[i].icon) perspective += `<icon>${_.escape(graph.getModel().perspectives[i].icon)}</icon>`;
			if (graph.getModel().perspectives[i].order) perspective += `<order>${_.escape(graph.getModel().perspectives[i].order)}</order>`;
			if (graph.getModel().perspectives[i].role) perspective += `<role>${_.escape(graph.getModel().perspectives[i].role)}</role>`;
			perspective += '</perspective>\n';

			model.push(perspective);
		}
		model.push(' </perspectives>\n');
	}

	if (graph.getModel().navigations) {
		model.push(' <navigations>\n');
		for (let i = 0; i < graph.getModel().navigations.length; i++) {
			let navigation = '  <item>';
			navigation += `<id>${_.escape(graph.getModel().navigations[i].id)}</id>`;
			navigation += `<label>${_.escape(graph.getModel().navigations[i].label)}</label>`;
			if (graph.getModel().navigations[i].header) navigation += `<header>${_.escape(graph.getModel().navigations[i].header)}</header>`;
			if (graph.getModel().navigations[i].expanded !== undefined) navigation += `<expanded>${_.escape(graph.getModel().navigations[i].expanded)}</expanded>`;
			else navigation += `<expanded>true</expanded>`;
			if (graph.getModel().navigations[i].icon) navigation += `<icon>${_.escape(graph.getModel().navigations[i].icon)}</icon>`;
			if (graph.getModel().navigations[i].order) navigation += `<order>${_.escape(graph.getModel().navigations[i].order)}</order>`;
			if (graph.getModel().navigations[i].role) navigation += `<role>${_.escape(graph.getModel().navigations[i].role)}</role>`;
			navigation += '</item>\n'
			model.push(navigation);
		}
		model.push(' </navigations>\n');
	}

	let enc = new mxCodec(mxUtils.createXmlDocument());
	let node = enc.encode(graph.getModel());
	let mxGraph = mxUtils.getXml(node);
	model.push(' ' + mxGraph);
	model.push('\n</model>');

	for (let i = 0; i < childCount; i++) {
		let child = graph.model.getChildAt(parent, i);

		if (!graph.model.isEdge(child)) {

			if (child.value.feedUrl) {
				child.value.feedUrl = atob(child.value.feedUrl);
			}
			if (child.value.feedUsername) {
				child.value.feedUsername = atob(child.value.feedUsername);
			}
			if (child.value.feedPassword) {
				child.value.feedPassword = atob(child.value.feedPassword);
			}
			if (child.value.importsCode) {
				child.value.importsCode = atob(child.value.importsCode);
			}
		}
	}

	return model.join('');
}

function getPerspectiveLabel(graph, child) {
	let perspectiveName = _.escape(child.value.perspectiveName);
	let perspectiveLabel = _.escape(child.value.perspectiveLabel);
	let perspectives = graph.getModel().perspectives || [];
	for (let i = 0; i < perspectives.length; i++) {
		if (perspectiveName === _.escape(perspectives[i].id)) {
			perspectiveLabel = perspectives[i].label;
		}
	}
	return perspectiveLabel;
}

function getPerspectiveHeader(graph, child) {
	let perspectiveName = _.escape(child.value.perspectiveName);
	let perspectiveHeader = _.escape(child.value.perspectiveHeader);
	let perspectives = graph.getModel().perspectives || [];
	for (let i = 0; i < perspectives.length; i++) {
		if (perspectiveName === _.escape(perspectives[i].id)) {
			perspectiveHeader = perspectives[i].header;
		}
	}
	return perspectiveHeader ? perspectiveHeader : '';
}

function getPerspectiveIcon(graph, child) {
	let perspectiveName = _.escape(child.value.perspectiveName);
	let perspectiveIcon = _.escape(child.value.perspectiveIcon);
	let perspectives = graph.getModel().perspectives || [];
	for (let i = 0; i < perspectives.length; i++) {
		if (perspectiveName === _.escape(perspectives[i].id)) {
			perspectiveIcon = perspectives[i].icon;
		}
	}
	return perspectiveIcon ? perspectiveIcon : '';
}

function getPerspectiveNavId(graph, child) {
	let perspectiveName = _.escape(child.value.perspectiveName);
	let perspectiveNavId = _.escape(child.value.perspectiveNavId);
	let perspectives = graph.getModel().perspectives || [];
	for (let i = 0; i < perspectives.length; i++) {
		if (perspectiveName === _.escape(perspectives[i].id)) {
			perspectiveNavId = perspectives[i].navId;
		}
	}
	return perspectiveNavId ? perspectiveNavId : '';
}

function getPerspectiveOrder(graph, child) {
	let perspectiveName = _.escape(child.value.perspectiveName);
	let perspectiveOrder = _.escape(child.value.perspectiveOrder);
	let perspectives = graph.getModel().perspectives || [];
	for (let i = 0; i < perspectives.length; i++) {
		if (perspectiveName === _.escape(perspectives[i].id)) {
			perspectiveOrder = perspectives[i].order;
		}
	}
	return perspectiveOrder;
}

function getPerspectiveRole(graph, child) {
	let perspectiveName = _.escape(child.value.perspectiveName);
	let perspectiveRole = _.escape(child.value.perspectiveRole);
	let perspectives = graph.getModel().perspectives || [];
	for (let i = 0; i < perspectives.length; i++) {
		if (perspectiveName === _.escape(perspectives[i].id)) {
			perspectiveRole = perspectives[i].role;
		}
	}
	return perspectiveRole ? perspectiveRole : '';
}

