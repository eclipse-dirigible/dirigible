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
export function getTemplate() {
	return {
		name: 'bpmn-new',
		label: 'Business Process Model',
		extension: 'bpmn',
		data: "<?xml version='1.0' encoding='UTF-8'?>\n<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:flowable=\"http://flowable.org/bpmn\" xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" xmlns:omgdc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:omgdi=\"http://www.omg.org/spec/DD/20100524/DI\" typeLanguage=\"http://www.w3.org/2001/XMLSchema\" expressionLanguage=\"http://www.w3.org/1999/XPath\" targetNamespace=\"http://www.flowable.org/processdef\" exporter=\"Flowable Open Source Modeler\" exporterVersion=\"6.8.1\">\n  <process id=\"myprocess\" name=\"MyProcess\" isExecutable=\"true\">\n    <startEvent id=\"start-event\"/>\n    <serviceTask id=\"my-service-task\" name=\"MyServiceTask\" flowable:async=\"true\" flowable:delegateExpression=\"${JSTask}\">\n      <extensionElements>\n        <flowable:field name=\"handler\">\n          <flowable:string><![CDATA[myproject/my-service-task.ts]]></flowable:string>\n        </flowable:field>\n      </extensionElements>\n    </serviceTask>\n    <sequenceFlow id=\"sid-797626AE-B2F6-4C00-ABEE-FB30ADC177E4\" sourceRef=\"start-event\" targetRef=\"my-service-task\"/>\n    <endEvent id=\"end-event\"/>\n    <sequenceFlow id=\"sid-645847E8-C959-48BD-816B-2E9CC4A2F08A\" sourceRef=\"my-service-task\" targetRef=\"end-event\"/>\n  </process>\n  <bpmndi:BPMNDiagram id=\"BPMNDiagram_myprocess\">\n    <bpmndi:BPMNPlane bpmnElement=\"myprocess\" id=\"BPMNPlane_myprocess\">\n      <bpmndi:BPMNShape bpmnElement=\"start-event\" id=\"BPMNShape_start-event\">\n        <omgdc:Bounds height=\"30.0\" width=\"30.0\" x=\"90.0\" y=\"78.0\"/>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"my-service-task\" id=\"BPMNShape_my-service-task\">\n        <omgdc:Bounds height=\"80.0\" width=\"100.0\" x=\"300.0\" y=\"53.0\"/>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"end-event\" id=\"BPMNShape_end-event\">\n        <omgdc:Bounds height=\"28.0\" width=\"28.0\" x=\"555.0\" y=\"79.0\"/>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNEdge bpmnElement=\"sid-797626AE-B2F6-4C00-ABEE-FB30ADC177E4\" id=\"BPMNEdge_sid-797626AE-B2F6-4C00-ABEE-FB30ADC177E4\" flowable:sourceDockerX=\"15.0\" flowable:sourceDockerY=\"15.0\" flowable:targetDockerX=\"50.0\" flowable:targetDockerY=\"40.0\">\n        <omgdi:waypoint x=\"119.94999969544602\" y=\"93.0\"/>\n        <omgdi:waypoint x=\"299.9999999999858\" y=\"93.0\"/>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"sid-645847E8-C959-48BD-816B-2E9CC4A2F08A\" id=\"BPMNEdge_sid-645847E8-C959-48BD-816B-2E9CC4A2F08A\" flowable:sourceDockerX=\"50.0\" flowable:sourceDockerY=\"40.0\" flowable:targetDockerX=\"14.0\" flowable:targetDockerY=\"14.0\">\n        <omgdi:waypoint x=\"399.95000000000005\" y=\"93.0\"/>\n        <omgdi:waypoint x=\"555.0\" y=\"93.0\"/>\n      </bpmndi:BPMNEdge>\n    </bpmndi:BPMNPlane>\n  </bpmndi:BPMNDiagram>\n</definitions>",
		order: 20
	};
};
