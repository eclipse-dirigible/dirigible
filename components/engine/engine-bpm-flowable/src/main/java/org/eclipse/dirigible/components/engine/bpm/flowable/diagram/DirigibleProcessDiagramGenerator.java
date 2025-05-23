/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.diagram;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.AdhocSubProcess;
import org.flowable.bpmn.model.Artifact;
import org.flowable.bpmn.model.Association;
import org.flowable.bpmn.model.AssociationDirection;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.BusinessRuleTask;
import org.flowable.bpmn.model.CallActivity;
import org.flowable.bpmn.model.CaseServiceTask;
import org.flowable.bpmn.model.CompensateEventDefinition;
import org.flowable.bpmn.model.ConditionalEventDefinition;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.ErrorEventDefinition;
import org.flowable.bpmn.model.EscalationEventDefinition;
import org.flowable.bpmn.model.Event;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.EventGateway;
import org.flowable.bpmn.model.EventSubProcess;
import org.flowable.bpmn.model.ExclusiveGateway;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.ExternalWorkerServiceTask;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowElementsContainer;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.Gateway;
import org.flowable.bpmn.model.GraphicInfo;
import org.flowable.bpmn.model.HttpServiceTask;
import org.flowable.bpmn.model.InclusiveGateway;
import org.flowable.bpmn.model.IntermediateCatchEvent;
import org.flowable.bpmn.model.Lane;
import org.flowable.bpmn.model.ManualTask;
import org.flowable.bpmn.model.MessageEventDefinition;
import org.flowable.bpmn.model.MultiInstanceLoopCharacteristics;
import org.flowable.bpmn.model.ParallelGateway;
import org.flowable.bpmn.model.Pool;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.ReceiveTask;
import org.flowable.bpmn.model.ScriptTask;
import org.flowable.bpmn.model.SendEventServiceTask;
import org.flowable.bpmn.model.SendTask;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.bpmn.model.SignalEventDefinition;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.Task;
import org.flowable.bpmn.model.TextAnnotation;
import org.flowable.bpmn.model.ThrowEvent;
import org.flowable.bpmn.model.TimerEventDefinition;
import org.flowable.bpmn.model.Transaction;
import org.flowable.bpmn.model.UserTask;
import org.flowable.image.ProcessDiagramGenerator;

public class DirigibleProcessDiagramGenerator implements ProcessDiagramGenerator {

    protected Map<Class<? extends BaseElement>, ActivityDrawInstruction> activityDrawInstructions = new HashMap<>();
    protected Map<Class<? extends BaseElement>, ArtifactDrawInstruction> artifactDrawInstructions = new HashMap<>();

    public DirigibleProcessDiagramGenerator() {
        this(1.0);
    }

    // The instructions on how to draw a certain construct is
    // created statically and stored in a map for performance.
    public DirigibleProcessDiagramGenerator(final double scaleFactor) {
        // start event
        activityDrawInstructions.put(StartEvent.class, new ActivityDrawInstruction() {

            @Override
            public void draw(DirigibleProcessDiagramCanvas processDiagramCanvas, BpmnModel bpmnModel, FlowNode flowNode) {
                GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(flowNode.getId());
                StartEvent startEvent = (StartEvent) flowNode;
                if (startEvent.getEventDefinitions() != null && !startEvent.getEventDefinitions()
                                                                           .isEmpty()) {
                    EventDefinition eventDefinition = startEvent.getEventDefinitions()
                                                                .get(0);
                    if (eventDefinition instanceof TimerEventDefinition) {
                        processDiagramCanvas.drawTimerStartEvent(graphicInfo, scaleFactor);
                    } else if (eventDefinition instanceof ErrorEventDefinition) {
                        processDiagramCanvas.drawErrorStartEvent(graphicInfo, scaleFactor);
                    } else if (eventDefinition instanceof EscalationEventDefinition) {
                        processDiagramCanvas.drawEscalationStartEvent(graphicInfo, scaleFactor);
                    } else if (eventDefinition instanceof ConditionalEventDefinition) {
                        processDiagramCanvas.drawConditionalStartEvent(graphicInfo, scaleFactor);
                    } else if (eventDefinition instanceof SignalEventDefinition) {
                        processDiagramCanvas.drawSignalStartEvent(graphicInfo, scaleFactor);
                    } else if (eventDefinition instanceof MessageEventDefinition) {
                        processDiagramCanvas.drawMessageStartEvent(graphicInfo, scaleFactor);
                    } else {
                        processDiagramCanvas.drawNoneStartEvent(graphicInfo);
                    }
                } else {
                    List<ExtensionElement> eventTypeElements = startEvent.getExtensionElements()
                                                                         .get("eventType");
                    if (eventTypeElements != null && eventTypeElements.size() > 0) {
                        processDiagramCanvas.drawEventRegistryStartEvent(graphicInfo, scaleFactor);

                    } else {
                        processDiagramCanvas.drawNoneStartEvent(graphicInfo);
                    }
                }
            }
        });

        // signal catch
        activityDrawInstructions.put(IntermediateCatchEvent.class, new ActivityDrawInstruction() {

            @Override
            public void draw(DirigibleProcessDiagramCanvas processDiagramCanvas, BpmnModel bpmnModel, FlowNode flowNode) {
                GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(flowNode.getId());
                IntermediateCatchEvent intermediateCatchEvent = (IntermediateCatchEvent) flowNode;
                if (intermediateCatchEvent.getEventDefinitions() != null && !intermediateCatchEvent.getEventDefinitions()
                                                                                                   .isEmpty()) {

                    if (intermediateCatchEvent.getEventDefinitions()
                                              .get(0) instanceof SignalEventDefinition) {
                        processDiagramCanvas.drawCatchingSignalEvent(flowNode.getName(), graphicInfo, true, scaleFactor);
                    } else if (intermediateCatchEvent.getEventDefinitions()
                                                     .get(0) instanceof TimerEventDefinition) {
                        processDiagramCanvas.drawCatchingTimerEvent(flowNode.getName(), graphicInfo, true, scaleFactor);
                    } else if (intermediateCatchEvent.getEventDefinitions()
                                                     .get(0) instanceof MessageEventDefinition) {
                        processDiagramCanvas.drawCatchingMessageEvent(flowNode.getName(), graphicInfo, true, scaleFactor);
                    } else if (intermediateCatchEvent.getEventDefinitions()
                                                     .get(0) instanceof ConditionalEventDefinition) {
                        processDiagramCanvas.drawCatchingConditionalEvent(flowNode.getName(), graphicInfo, true, scaleFactor);
                    }
                }
            }
        });

        // signal throw
        activityDrawInstructions.put(ThrowEvent.class, new ActivityDrawInstruction() {

            @Override
            public void draw(DirigibleProcessDiagramCanvas processDiagramCanvas, BpmnModel bpmnModel, FlowNode flowNode) {
                GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(flowNode.getId());
                ThrowEvent throwEvent = (ThrowEvent) flowNode;
                if (throwEvent.getEventDefinitions() != null && !throwEvent.getEventDefinitions()
                                                                           .isEmpty()) {
                    if (throwEvent.getEventDefinitions()
                                  .get(0) instanceof SignalEventDefinition) {
                        processDiagramCanvas.drawThrowingSignalEvent(graphicInfo, scaleFactor);
                    } else if (throwEvent.getEventDefinitions()
                                         .get(0) instanceof EscalationEventDefinition) {
                        processDiagramCanvas.drawThrowingEscalationEvent(graphicInfo, scaleFactor);
                    } else if (throwEvent.getEventDefinitions()
                                         .get(0) instanceof CompensateEventDefinition) {
                        processDiagramCanvas.drawThrowingCompensateEvent(graphicInfo, scaleFactor);
                    } else {
                        processDiagramCanvas.drawThrowingNoneEvent(graphicInfo, scaleFactor);
                    }
                } else {
                    processDiagramCanvas.drawThrowingNoneEvent(graphicInfo, scaleFactor);
                }
            }
        });

        // end event
        activityDrawInstructions.put(EndEvent.class, new ActivityDrawInstruction() {

            @Override
            public void draw(DirigibleProcessDiagramCanvas processDiagramCanvas, BpmnModel bpmnModel, FlowNode flowNode) {
                GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(flowNode.getId());
                EndEvent endEvent = (EndEvent) flowNode;
                if (endEvent.getEventDefinitions() != null && !endEvent.getEventDefinitions()
                                                                       .isEmpty()) {
                    if (endEvent.getEventDefinitions()
                                .get(0) instanceof ErrorEventDefinition) {
                        processDiagramCanvas.drawErrorEndEvent(flowNode.getName(), graphicInfo, scaleFactor);
                    } else if (endEvent.getEventDefinitions()
                                       .get(0) instanceof EscalationEventDefinition) {
                        processDiagramCanvas.drawEscalationEndEvent(flowNode.getName(), graphicInfo, scaleFactor);
                    } else {
                        processDiagramCanvas.drawNoneEndEvent(graphicInfo, scaleFactor);
                    }
                } else {
                    processDiagramCanvas.drawNoneEndEvent(graphicInfo, scaleFactor);
                }
            }
        });

        // task
        activityDrawInstructions.put(Task.class, new ActivityDrawInstruction() {

            @Override
            public void draw(DirigibleProcessDiagramCanvas processDiagramCanvas, BpmnModel bpmnModel, FlowNode flowNode) {
                GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(flowNode.getId());
                processDiagramCanvas.drawTask(flowNode.getName(), graphicInfo, scaleFactor);
            }
        });

        // user task
        activityDrawInstructions.put(UserTask.class, new ActivityDrawInstruction() {

            @Override
            public void draw(DirigibleProcessDiagramCanvas processDiagramCanvas, BpmnModel bpmnModel, FlowNode flowNode) {
                GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(flowNode.getId());
                processDiagramCanvas.drawUserTask(flowNode.getName(), graphicInfo, scaleFactor);
            }
        });

        // script task
        activityDrawInstructions.put(ScriptTask.class, new ActivityDrawInstruction() {

            @Override
            public void draw(DirigibleProcessDiagramCanvas processDiagramCanvas, BpmnModel bpmnModel, FlowNode flowNode) {
                GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(flowNode.getId());
                processDiagramCanvas.drawScriptTask(flowNode.getName(), graphicInfo, scaleFactor);
            }
        });

        // service task
        activityDrawInstructions.put(ServiceTask.class, new ActivityDrawInstruction() {

            @Override
            public void draw(DirigibleProcessDiagramCanvas processDiagramCanvas, BpmnModel bpmnModel, FlowNode flowNode) {
                GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(flowNode.getId());
                ServiceTask serviceTask = (ServiceTask) flowNode;
                if ("camel".equalsIgnoreCase(serviceTask.getType())) {
                    processDiagramCanvas.drawCamelTask(serviceTask.getName(), graphicInfo, scaleFactor);
                } else if (ServiceTask.HTTP_TASK.equalsIgnoreCase(serviceTask.getType())) {
                    processDiagramCanvas.drawHttpTask(serviceTask.getName(), graphicInfo, scaleFactor);
                } else if (ServiceTask.DMN_TASK.equalsIgnoreCase(serviceTask.getType())) {
                    processDiagramCanvas.drawDMNTask(serviceTask.getName(), graphicInfo, scaleFactor);
                } else if (ServiceTask.SHELL_TASK.equalsIgnoreCase(serviceTask.getType())) {
                    processDiagramCanvas.drawShellTask(serviceTask.getName(), graphicInfo, scaleFactor);
                } else {
                    processDiagramCanvas.drawServiceTask(serviceTask.getName(), graphicInfo, scaleFactor);
                }
            }
        });

        // http service task
        activityDrawInstructions.put(HttpServiceTask.class, new ActivityDrawInstruction() {

            @Override
            public void draw(DirigibleProcessDiagramCanvas processDiagramCanvas, BpmnModel bpmnModel, FlowNode flowNode) {
                GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(flowNode.getId());
                processDiagramCanvas.drawHttpTask(flowNode.getName(), graphicInfo, scaleFactor);
            }
        });

        // receive task
        activityDrawInstructions.put(ReceiveTask.class, new ActivityDrawInstruction() {

            @Override
            public void draw(DirigibleProcessDiagramCanvas processDiagramCanvas, BpmnModel bpmnModel, FlowNode flowNode) {
                GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(flowNode.getId());
                processDiagramCanvas.drawReceiveTask(flowNode.getName(), graphicInfo, scaleFactor);
            }
        });

        // send task
        activityDrawInstructions.put(SendTask.class, new ActivityDrawInstruction() {

            @Override
            public void draw(DirigibleProcessDiagramCanvas processDiagramCanvas, BpmnModel bpmnModel, FlowNode flowNode) {
                GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(flowNode.getId());
                processDiagramCanvas.drawSendTask(flowNode.getName(), graphicInfo, scaleFactor);
            }
        });

        // manual task
        activityDrawInstructions.put(ManualTask.class, new ActivityDrawInstruction() {

            @Override
            public void draw(DirigibleProcessDiagramCanvas processDiagramCanvas, BpmnModel bpmnModel, FlowNode flowNode) {
                GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(flowNode.getId());
                processDiagramCanvas.drawManualTask(flowNode.getName(), graphicInfo, scaleFactor);
            }
        });

        // send event service task
        activityDrawInstructions.put(SendEventServiceTask.class, new ActivityDrawInstruction() {

            @Override
            public void draw(DirigibleProcessDiagramCanvas processDiagramCanvas, BpmnModel bpmnModel, FlowNode flowNode) {
                GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(flowNode.getId());
                processDiagramCanvas.drawSendEventServiceTask(flowNode.getName(), graphicInfo, scaleFactor);
            }
        });

        // external worker service task
        activityDrawInstructions.put(ExternalWorkerServiceTask.class, new ActivityDrawInstruction() {

            @Override
            public void draw(DirigibleProcessDiagramCanvas processDiagramCanvas, BpmnModel bpmnModel, FlowNode flowNode) {
                GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(flowNode.getId());
                ServiceTask serviceTask = (ServiceTask) flowNode;
                processDiagramCanvas.drawServiceTask(serviceTask.getName(), graphicInfo, scaleFactor);
            }
        });

        // case service task
        activityDrawInstructions.put(CaseServiceTask.class, new ActivityDrawInstruction() {

            @Override
            public void draw(DirigibleProcessDiagramCanvas processDiagramCanvas, BpmnModel bpmnModel, FlowNode flowNode) {
                GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(flowNode.getId());
                processDiagramCanvas.drawCaseServiceTask(flowNode.getName(), graphicInfo, scaleFactor);
            }
        });

        // businessRuleTask task
        activityDrawInstructions.put(BusinessRuleTask.class, new ActivityDrawInstruction() {

            @Override
            public void draw(DirigibleProcessDiagramCanvas processDiagramCanvas, BpmnModel bpmnModel, FlowNode flowNode) {
                GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(flowNode.getId());
                processDiagramCanvas.drawBusinessRuleTask(flowNode.getName(), graphicInfo, scaleFactor);
            }
        });

        // exclusive gateway
        activityDrawInstructions.put(ExclusiveGateway.class, new ActivityDrawInstruction() {

            @Override
            public void draw(DirigibleProcessDiagramCanvas processDiagramCanvas, BpmnModel bpmnModel, FlowNode flowNode) {
                GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(flowNode.getId());
                processDiagramCanvas.drawExclusiveGateway(graphicInfo, scaleFactor);
            }
        });

        // inclusive gateway
        activityDrawInstructions.put(InclusiveGateway.class, new ActivityDrawInstruction() {

            @Override
            public void draw(DirigibleProcessDiagramCanvas processDiagramCanvas, BpmnModel bpmnModel, FlowNode flowNode) {
                GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(flowNode.getId());
                processDiagramCanvas.drawInclusiveGateway(graphicInfo, scaleFactor);
            }
        });

        // parallel gateway
        activityDrawInstructions.put(ParallelGateway.class, new ActivityDrawInstruction() {

            @Override
            public void draw(DirigibleProcessDiagramCanvas processDiagramCanvas, BpmnModel bpmnModel, FlowNode flowNode) {
                GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(flowNode.getId());
                processDiagramCanvas.drawParallelGateway(graphicInfo, scaleFactor);
            }
        });

        // event based gateway
        activityDrawInstructions.put(EventGateway.class, new ActivityDrawInstruction() {

            @Override
            public void draw(DirigibleProcessDiagramCanvas processDiagramCanvas, BpmnModel bpmnModel, FlowNode flowNode) {
                GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(flowNode.getId());
                processDiagramCanvas.drawEventBasedGateway(graphicInfo, scaleFactor);
            }
        });

        // Boundary timer
        activityDrawInstructions.put(BoundaryEvent.class, new ActivityDrawInstruction() {

            @Override
            public void draw(DirigibleProcessDiagramCanvas processDiagramCanvas, BpmnModel bpmnModel, FlowNode flowNode) {
                GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(flowNode.getId());
                BoundaryEvent boundaryEvent = (BoundaryEvent) flowNode;
                if (boundaryEvent.getEventDefinitions() != null && !boundaryEvent.getEventDefinitions()
                                                                                 .isEmpty()) {
                    EventDefinition eventDefinition = boundaryEvent.getEventDefinitions()
                                                                   .get(0);
                    if (eventDefinition instanceof TimerEventDefinition) {
                        processDiagramCanvas.drawCatchingTimerEvent(flowNode.getName(), graphicInfo, boundaryEvent.isCancelActivity(),
                                scaleFactor);

                    } else if (eventDefinition instanceof ConditionalEventDefinition) {
                        processDiagramCanvas.drawCatchingConditionalEvent(graphicInfo, boundaryEvent.isCancelActivity(), scaleFactor);

                    } else if (eventDefinition instanceof ErrorEventDefinition) {
                        processDiagramCanvas.drawCatchingErrorEvent(graphicInfo, boundaryEvent.isCancelActivity(), scaleFactor);

                    } else if (eventDefinition instanceof EscalationEventDefinition) {
                        processDiagramCanvas.drawCatchingEscalationEvent(graphicInfo, boundaryEvent.isCancelActivity(), scaleFactor);

                    } else if (eventDefinition instanceof SignalEventDefinition) {
                        processDiagramCanvas.drawCatchingSignalEvent(flowNode.getName(), graphicInfo, boundaryEvent.isCancelActivity(),
                                scaleFactor);

                    } else if (eventDefinition instanceof MessageEventDefinition) {
                        processDiagramCanvas.drawCatchingMessageEvent(flowNode.getName(), graphicInfo, boundaryEvent.isCancelActivity(),
                                scaleFactor);

                    } else if (eventDefinition instanceof CompensateEventDefinition) {
                        processDiagramCanvas.drawCatchingCompensateEvent(graphicInfo, boundaryEvent.isCancelActivity(), scaleFactor);
                    }

                } else {
                    List<ExtensionElement> eventTypeElements = boundaryEvent.getExtensionElements()
                                                                            .get("eventType");
                    if (eventTypeElements != null && eventTypeElements.size() > 0) {
                        processDiagramCanvas.drawCatchingEventRegistryEvent(flowNode.getName(), graphicInfo,
                                boundaryEvent.isCancelActivity(), scaleFactor);
                    }
                }
            }
        });

        // subprocess
        activityDrawInstructions.put(SubProcess.class, new ActivityDrawInstruction() {

            @Override
            public void draw(DirigibleProcessDiagramCanvas processDiagramCanvas, BpmnModel bpmnModel, FlowNode flowNode) {
                GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(flowNode.getId());
                if (graphicInfo.getExpanded() != null && !graphicInfo.getExpanded()) {
                    processDiagramCanvas.drawCollapsedSubProcess(flowNode.getName(), graphicInfo, false, scaleFactor);
                } else {
                    processDiagramCanvas.drawExpandedSubProcess(flowNode.getName(), graphicInfo, false, scaleFactor);
                }
            }
        });

        // transaction
        activityDrawInstructions.put(Transaction.class, new ActivityDrawInstruction() {

            @Override
            public void draw(DirigibleProcessDiagramCanvas processDiagramCanvas, BpmnModel bpmnModel, FlowNode flowNode) {
                GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(flowNode.getId());
                if (graphicInfo.getExpanded() != null && !graphicInfo.getExpanded()) {
                    processDiagramCanvas.drawCollapsedSubProcess(flowNode.getName(), graphicInfo, false, scaleFactor);
                } else {
                    processDiagramCanvas.drawExpandedTransaction(flowNode.getName(), graphicInfo, scaleFactor);
                }
            }
        });

        // Event subprocess
        activityDrawInstructions.put(EventSubProcess.class, new ActivityDrawInstruction() {

            @Override
            public void draw(DirigibleProcessDiagramCanvas processDiagramCanvas, BpmnModel bpmnModel, FlowNode flowNode) {
                GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(flowNode.getId());
                if (graphicInfo.getExpanded() != null && !graphicInfo.getExpanded()) {
                    processDiagramCanvas.drawCollapsedSubProcess(flowNode.getName(), graphicInfo, true, scaleFactor);
                } else {
                    processDiagramCanvas.drawExpandedSubProcess(flowNode.getName(), graphicInfo, true, scaleFactor);
                }
            }
        });

        // Adhoc subprocess
        activityDrawInstructions.put(AdhocSubProcess.class, new ActivityDrawInstruction() {

            @Override
            public void draw(DirigibleProcessDiagramCanvas processDiagramCanvas, BpmnModel bpmnModel, FlowNode flowNode) {
                GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(flowNode.getId());
                if (graphicInfo.getExpanded() != null && !graphicInfo.getExpanded()) {
                    processDiagramCanvas.drawCollapsedSubProcess(flowNode.getName(), graphicInfo, false, scaleFactor);
                } else {
                    processDiagramCanvas.drawExpandedSubProcess(flowNode.getName(), graphicInfo, false, scaleFactor);
                }
            }
        });

        // call activity
        activityDrawInstructions.put(CallActivity.class, new ActivityDrawInstruction() {

            @Override
            public void draw(DirigibleProcessDiagramCanvas processDiagramCanvas, BpmnModel bpmnModel, FlowNode flowNode) {
                GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(flowNode.getId());
                processDiagramCanvas.drawCollapsedCallActivity(flowNode.getName(), graphicInfo, scaleFactor);
            }
        });

        // text annotation
        artifactDrawInstructions.put(TextAnnotation.class, new ArtifactDrawInstruction() {

            @Override
            public void draw(DirigibleProcessDiagramCanvas processDiagramCanvas, BpmnModel bpmnModel, Artifact artifact) {
                GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(artifact.getId());
                TextAnnotation textAnnotation = (TextAnnotation) artifact;
                processDiagramCanvas.drawTextAnnotation(textAnnotation.getText(), graphicInfo, scaleFactor);
            }
        });

        // association
        artifactDrawInstructions.put(Association.class, new ArtifactDrawInstruction() {

            @Override
            public void draw(DirigibleProcessDiagramCanvas processDiagramCanvas, BpmnModel bpmnModel, Artifact artifact) {
                Association association = (Association) artifact;
                String sourceRef = association.getSourceRef();
                String targetRef = association.getTargetRef();

                // source and target can be instance of FlowElement or Artifact
                BaseElement sourceElement = bpmnModel.getFlowElement(sourceRef);
                BaseElement targetElement = bpmnModel.getFlowElement(targetRef);
                if (sourceElement == null) {
                    sourceElement = bpmnModel.getArtifact(sourceRef);
                }
                if (targetElement == null) {
                    targetElement = bpmnModel.getArtifact(targetRef);
                }
                List<GraphicInfo> graphicInfoList = bpmnModel.getFlowLocationGraphicInfo(artifact.getId());
                graphicInfoList = connectionPerfectionizer(processDiagramCanvas, bpmnModel, sourceElement, targetElement, graphicInfoList);

                if (graphicInfoList != null) {
                    int[] xPoints = new int[graphicInfoList.size()];
                    int[] yPoints = new int[graphicInfoList.size()];
                    for (int i = 1; i < graphicInfoList.size(); i++) {
                        GraphicInfo graphicInfo = graphicInfoList.get(i);
                        GraphicInfo previousGraphicInfo = graphicInfoList.get(i - 1);

                        if (i == 1) {
                            xPoints[0] = (int) previousGraphicInfo.getX();
                            yPoints[0] = (int) previousGraphicInfo.getY();
                        }
                        xPoints[i] = (int) graphicInfo.getX();
                        yPoints[i] = (int) graphicInfo.getY();
                    }

                    AssociationDirection associationDirection = association.getAssociationDirection();
                    processDiagramCanvas.drawAssociation(xPoints, yPoints, associationDirection, false, scaleFactor);
                }
            }
        });
    }

    @Override
    public InputStream generateDiagram(BpmnModel bpmnModel, String imageType, List<String> highLightedActivities,
            List<String> highLightedFlows, String activityFontName, String labelFontName, String annotationFontName,
            ClassLoader customClassLoader, double scaleFactor, boolean drawSequenceFlowNameWithNoLabelDI) {

        return generateProcessDiagram(bpmnModel, imageType, highLightedActivities, highLightedFlows, activityFontName, labelFontName,
                annotationFontName, customClassLoader, scaleFactor, drawSequenceFlowNameWithNoLabelDI).generateImage(imageType);
    }

    @Override
    public InputStream generateDiagram(BpmnModel bpmnModel, String imageType, List<String> highLightedActivities,
            List<String> highLightedFlows, boolean drawSequenceFlowNameWithNoLabelDI) {
        return generateDiagram(bpmnModel, imageType, highLightedActivities, highLightedFlows, null, null, null, null, 1.0,
                drawSequenceFlowNameWithNoLabelDI);
    }

    @Override
    public InputStream generateDiagram(BpmnModel bpmnModel, String imageType, List<String> highLightedActivities,
            List<String> highLightedFlows, double scaleFactor, boolean drawSequenceFlowNameWithNoLabelDI) {
        return generateDiagram(bpmnModel, imageType, highLightedActivities, highLightedFlows, null, null, null, null, scaleFactor,
                drawSequenceFlowNameWithNoLabelDI);
    }

    @Override
    public InputStream generateDiagram(BpmnModel bpmnModel, String imageType, List<String> highLightedActivities,
            boolean drawSequenceFlowNameWithNoLabelDI) {
        return generateDiagram(bpmnModel, imageType, highLightedActivities, Collections.emptyList(), drawSequenceFlowNameWithNoLabelDI);
    }

    @Override
    public InputStream generateDiagram(BpmnModel bpmnModel, String imageType, List<String> highLightedActivities, double scaleFactor,
            boolean drawSequenceFlowNameWithNoLabelDI) {
        return generateDiagram(bpmnModel, imageType, highLightedActivities, Collections.emptyList(), scaleFactor,
                drawSequenceFlowNameWithNoLabelDI);
    }

    @Override
    public InputStream generateDiagram(BpmnModel bpmnModel, String imageType, String activityFontName, String labelFontName,
            String annotationFontName, ClassLoader customClassLoader, boolean drawSequenceFlowNameWithNoLabelDI) {

        return generateDiagram(bpmnModel, imageType, Collections.emptyList(), Collections.emptyList(), activityFontName, labelFontName,
                annotationFontName, customClassLoader, 1.0, drawSequenceFlowNameWithNoLabelDI);
    }

    @Override
    public InputStream generateDiagram(BpmnModel bpmnModel, String imageType, String activityFontName, String labelFontName,
            String annotationFontName, ClassLoader customClassLoader, double scaleFactor, boolean drawSequenceFlowNameWithNoLabelDI) {

        return generateDiagram(bpmnModel, imageType, Collections.emptyList(), Collections.emptyList(), activityFontName, labelFontName,
                annotationFontName, customClassLoader, scaleFactor, drawSequenceFlowNameWithNoLabelDI);
    }

    @Override
    public InputStream generatePngDiagram(BpmnModel bpmnModel, boolean drawSequenceFlowNameWithNoLabelDI) {
        return generatePngDiagram(bpmnModel, 1.0, drawSequenceFlowNameWithNoLabelDI);
    }

    @Override
    public InputStream generatePngDiagram(BpmnModel bpmnModel, double scaleFactor, boolean drawSequenceFlowNameWithNoLabelDI) {
        return generateDiagram(bpmnModel, "png", Collections.emptyList(), Collections.emptyList(), scaleFactor,
                drawSequenceFlowNameWithNoLabelDI);
    }

    @Override
    public InputStream generateJpgDiagram(BpmnModel bpmnModel) {
        return generateJpgDiagram(bpmnModel, 1.0, false);
    }

    @Override
    public InputStream generateJpgDiagram(BpmnModel bpmnModel, double scaleFactor, boolean drawSequenceFlowNameWithNoLabelDI) {
        return generateDiagram(bpmnModel, "jpg", Collections.emptyList(), Collections.emptyList(), drawSequenceFlowNameWithNoLabelDI);
    }

    public BufferedImage generateImage(BpmnModel bpmnModel, String imageType, List<String> highLightedActivities,
            List<String> highLightedFlows, String activityFontName, String labelFontName, String annotationFontName,
            ClassLoader customClassLoader, double scaleFactor, boolean drawSequenceFlowNameWithNoLabelDI) {

        return generateProcessDiagram(bpmnModel, imageType, highLightedActivities, highLightedFlows, activityFontName, labelFontName,
                annotationFontName, customClassLoader, scaleFactor, drawSequenceFlowNameWithNoLabelDI).generateBufferedImage(imageType);
    }

    public BufferedImage generateImage(BpmnModel bpmnModel, String imageType, List<String> highLightedActivities,
            List<String> highLightedFlows, double scaleFactor, boolean drawSequenceFlowNameWithNoLabelDI) {

        return generateImage(bpmnModel, imageType, highLightedActivities, highLightedFlows, null, null, null, null, scaleFactor,
                drawSequenceFlowNameWithNoLabelDI);
    }

    @Override
    public BufferedImage generatePngImage(BpmnModel bpmnModel, double scaleFactor) {
        return generateImage(bpmnModel, "png", Collections.emptyList(), Collections.emptyList(), scaleFactor, false);
    }

    protected DirigibleProcessDiagramCanvas generateProcessDiagram(BpmnModel bpmnModel, String imageType,
            List<String> highLightedActivities, List<String> highLightedFlows, String activityFontName, String labelFontName,
            String annotationFontName, ClassLoader customClassLoader, double scaleFactor, boolean drawSequenceFlowNameWithNoLabelDI) {

        prepareBpmnModel(bpmnModel);

        DirigibleProcessDiagramCanvas processDiagramCanvas =
                initProcessDiagramCanvas(bpmnModel, imageType, activityFontName, labelFontName, annotationFontName, customClassLoader);

        // Draw pool shape, if process is participant in collaboration
        for (Pool pool : bpmnModel.getPools()) {
            GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(pool.getId());
            processDiagramCanvas.drawPoolOrLane(pool.getName(), graphicInfo, scaleFactor);
        }

        // Draw lanes
        for (Process process : bpmnModel.getProcesses()) {
            for (Lane lane : process.getLanes()) {
                GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(lane.getId());
                processDiagramCanvas.drawPoolOrLane(lane.getName(), graphicInfo, scaleFactor);
            }
        }

        // Draw activities and their sequence-flows
        for (Process process : bpmnModel.getProcesses()) {
            for (FlowNode flowNode : process.findFlowElementsOfType(FlowNode.class)) {
                if (!isPartOfCollapsedSubProcess(flowNode, bpmnModel)) {
                    drawActivity(processDiagramCanvas, bpmnModel, flowNode, highLightedActivities, highLightedFlows, scaleFactor,
                            drawSequenceFlowNameWithNoLabelDI);
                }
            }
        }

        // Draw artifacts
        for (Process process : bpmnModel.getProcesses()) {

            for (Artifact artifact : process.getArtifacts()) {
                drawArtifact(processDiagramCanvas, bpmnModel, artifact);
            }

            List<SubProcess> subProcesses = process.findFlowElementsOfType(SubProcess.class, true);
            if (subProcesses != null) {
                for (SubProcess subProcess : subProcesses) {

                    GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(subProcess.getId());
                    if (graphicInfo != null && graphicInfo.getExpanded() != null && !graphicInfo.getExpanded()) {
                        continue;
                    }

                    if (!isPartOfCollapsedSubProcess(subProcess, bpmnModel)) {
                        for (Artifact subProcessArtifact : subProcess.getArtifacts()) {
                            drawArtifact(processDiagramCanvas, bpmnModel, subProcessArtifact);
                        }
                    }
                }
            }
        }

        return processDiagramCanvas;
    }

    protected void prepareBpmnModel(BpmnModel bpmnModel) {

        // Need to make sure all elements have positive x and y.
        // Check all graphicInfo and update the elements accordingly

        List<GraphicInfo> allGraphicInfos = new ArrayList<>();
        if (bpmnModel.getLocationMap() != null) {
            allGraphicInfos.addAll(bpmnModel.getLocationMap()
                                            .values());
        }
        if (bpmnModel.getLabelLocationMap() != null) {
            allGraphicInfos.addAll(bpmnModel.getLabelLocationMap()
                                            .values());
        }
        if (bpmnModel.getFlowLocationMap() != null) {
            for (List<GraphicInfo> flowGraphicInfos : bpmnModel.getFlowLocationMap()
                                                               .values()) {
                allGraphicInfos.addAll(flowGraphicInfos);
            }
        }

        if (allGraphicInfos.size() > 0) {

            boolean needsTranslationX = false;
            boolean needsTranslationY = false;

            double lowestX = 0.0;
            double lowestY = 0.0;

            // Collect lowest x and y
            for (GraphicInfo graphicInfo : allGraphicInfos) {

                double x = graphicInfo.getX();
                double y = graphicInfo.getY();

                if (x < lowestX) {
                    needsTranslationX = true;
                    lowestX = x;
                }
                if (y < lowestY) {
                    needsTranslationY = true;
                    lowestY = y;
                }

            }

            // Update all graphicInfo objects
            if (needsTranslationX || needsTranslationY) {

                double translationX = Math.abs(lowestX);
                double translationY = Math.abs(lowestY);

                for (GraphicInfo graphicInfo : allGraphicInfos) {
                    if (needsTranslationX) {
                        graphicInfo.setX(graphicInfo.getX() + translationX);
                    }
                    if (needsTranslationY) {
                        graphicInfo.setY(graphicInfo.getY() + translationY);
                    }
                }
            }

        }

    }

    protected void drawActivity(DirigibleProcessDiagramCanvas processDiagramCanvas, BpmnModel bpmnModel, FlowNode flowNode,
            List<String> highLightedActivities, List<String> highLightedFlows, double scaleFactor,
            Boolean drawSequenceFlowNameWithNoLabelDI) {

        ActivityDrawInstruction drawInstruction = activityDrawInstructions.get(flowNode.getClass());
        if (drawInstruction != null) {

            drawInstruction.draw(processDiagramCanvas, bpmnModel, flowNode);

            // Gather info on the multi instance marker
            boolean multiInstanceSequential = false;
            boolean multiInstanceParallel = false;
            boolean collapsed = false;
            if (flowNode instanceof Activity) {
                Activity activity = (Activity) flowNode;
                MultiInstanceLoopCharacteristics multiInstanceLoopCharacteristics = activity.getLoopCharacteristics();
                if (multiInstanceLoopCharacteristics != null) {
                    multiInstanceSequential = multiInstanceLoopCharacteristics.isSequential();
                    multiInstanceParallel = !multiInstanceSequential;
                }
            }

            // Gather info on the collapsed marker
            GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(flowNode.getId());
            if (flowNode instanceof SubProcess) {
                collapsed = graphicInfo.getExpanded() != null && !graphicInfo.getExpanded();
            } else if (flowNode instanceof CallActivity) {
                collapsed = true;
            }

            if (scaleFactor == 1.0) {
                // Actually draw the markers
                processDiagramCanvas.drawActivityMarkers((int) graphicInfo.getX(), (int) graphicInfo.getY(), (int) graphicInfo.getWidth(),
                        (int) graphicInfo.getHeight(), multiInstanceSequential, multiInstanceParallel, collapsed);
            }

            // Draw highlighted activities
            if (highLightedActivities.contains(flowNode.getId())) {
                drawHighLight(processDiagramCanvas, bpmnModel.getGraphicInfo(flowNode.getId()));
            }

        } else if (flowNode instanceof Task) {
            activityDrawInstructions.get(Task.class)
                                    .draw(processDiagramCanvas, bpmnModel, flowNode);

            if (highLightedActivities.contains(flowNode.getId())) {
                drawHighLight(processDiagramCanvas, bpmnModel.getGraphicInfo(flowNode.getId()));
            }
        }

        // Outgoing transitions of activity
        for (SequenceFlow sequenceFlow : flowNode.getOutgoingFlows()) {
            boolean highLighted = (highLightedFlows.contains(sequenceFlow.getId()));
            String defaultFlow = null;
            if (flowNode instanceof Activity) {
                defaultFlow = ((Activity) flowNode).getDefaultFlow();
            } else if (flowNode instanceof Gateway) {
                defaultFlow = ((Gateway) flowNode).getDefaultFlow();
            }

            boolean isDefault = false;
            if (defaultFlow != null && defaultFlow.equalsIgnoreCase(sequenceFlow.getId())) {
                isDefault = true;
            }
            boolean drawConditionalIndicator = sequenceFlow.getConditionExpression() != null && sequenceFlow.getConditionExpression()
                                                                                                            .trim()
                                                                                                            .length() > 0
                    && !(flowNode instanceof Gateway);

            String sourceRef = sequenceFlow.getSourceRef();
            String targetRef = sequenceFlow.getTargetRef();
            FlowElement sourceElement = bpmnModel.getFlowElement(sourceRef);
            FlowElement targetElement = bpmnModel.getFlowElement(targetRef);
            List<GraphicInfo> graphicInfoList = bpmnModel.getFlowLocationGraphicInfo(sequenceFlow.getId());
            if (graphicInfoList != null && graphicInfoList.size() > 0) {
                graphicInfoList = connectionPerfectionizer(processDiagramCanvas, bpmnModel, sourceElement, targetElement, graphicInfoList);
                int[] xPoints = new int[graphicInfoList.size()];
                int[] yPoints = new int[graphicInfoList.size()];

                for (int i = 1; i < graphicInfoList.size(); i++) {
                    GraphicInfo graphicInfo = graphicInfoList.get(i);
                    GraphicInfo previousGraphicInfo = graphicInfoList.get(i - 1);

                    if (i == 1) {
                        xPoints[0] = (int) previousGraphicInfo.getX();
                        yPoints[0] = (int) previousGraphicInfo.getY();
                    }
                    xPoints[i] = (int) graphicInfo.getX();
                    yPoints[i] = (int) graphicInfo.getY();

                }

                processDiagramCanvas.drawSequenceflow(xPoints, yPoints, drawConditionalIndicator, isDefault, highLighted, scaleFactor);

                // Draw sequenceflow label
                GraphicInfo labelGraphicInfo = bpmnModel.getLabelGraphicInfo(sequenceFlow.getId());
                if (labelGraphicInfo != null) {
                    processDiagramCanvas.drawLabel(sequenceFlow.getName(), labelGraphicInfo, false);
                } else {
                    if (drawSequenceFlowNameWithNoLabelDI) {
                        GraphicInfo lineCenter = getLineCenter(graphicInfoList);
                        processDiagramCanvas.drawLabel(sequenceFlow.getName(), lineCenter, false);
                    }

                }
            }
        }

        // Nested elements
        if (flowNode instanceof FlowElementsContainer) {
            for (FlowElement nestedFlowElement : ((FlowElementsContainer) flowNode).getFlowElements()) {
                if (nestedFlowElement instanceof FlowNode && !isPartOfCollapsedSubProcess(nestedFlowElement, bpmnModel)) {
                    drawActivity(processDiagramCanvas, bpmnModel, (FlowNode) nestedFlowElement, highLightedActivities, highLightedFlows,
                            scaleFactor, drawSequenceFlowNameWithNoLabelDI);
                }
            }
        }
    }

    /**
     * This method makes coordinates of connection flow better.
     *
     * @param processDiagramCanvas
     * @param bpmnModel
     * @param sourceElement
     * @param targetElement
     * @param graphicInfoList
     * @return
     */
    protected static List<GraphicInfo> connectionPerfectionizer(DirigibleProcessDiagramCanvas processDiagramCanvas, BpmnModel bpmnModel,
            BaseElement sourceElement, BaseElement targetElement, List<GraphicInfo> graphicInfoList) {
        GraphicInfo sourceGraphicInfo = bpmnModel.getGraphicInfo(sourceElement.getId());
        GraphicInfo targetGraphicInfo = bpmnModel.getGraphicInfo(targetElement.getId());

        DirigibleProcessDiagramCanvas.SHAPE_TYPE sourceShapeType = getShapeType(sourceElement);
        DirigibleProcessDiagramCanvas.SHAPE_TYPE targetShapeType = getShapeType(targetElement);

        return processDiagramCanvas.connectionPerfectionizer(sourceShapeType, targetShapeType, sourceGraphicInfo, targetGraphicInfo,
                graphicInfoList);
    }

    /**
     * This method returns shape type of base element.<br>
     * Each element can be presented as rectangle, rhombus, or ellipse.
     *
     * @param baseElement
     * @return DirigibleProcessDiagramCanvas.SHAPE_TYPE
     */
    protected static DirigibleProcessDiagramCanvas.SHAPE_TYPE getShapeType(BaseElement baseElement) {
        if (baseElement instanceof Task || baseElement instanceof Activity || baseElement instanceof TextAnnotation) {
            return DirigibleProcessDiagramCanvas.SHAPE_TYPE.Rectangle;
        } else if (baseElement instanceof Gateway) {
            return DirigibleProcessDiagramCanvas.SHAPE_TYPE.Rhombus;
        } else if (baseElement instanceof Event) {
            return DirigibleProcessDiagramCanvas.SHAPE_TYPE.Ellipse;
        } else {
            // unknown source element, just do not correct coordinates
        }
        return null;
    }

    protected static GraphicInfo getLineCenter(List<GraphicInfo> graphicInfoList) {
        GraphicInfo gi = new GraphicInfo();

        int[] xPoints = new int[graphicInfoList.size()];
        int[] yPoints = new int[graphicInfoList.size()];

        double length = 0;
        double[] lengths = new double[graphicInfoList.size()];
        lengths[0] = 0;
        double m;
        for (int i = 1; i < graphicInfoList.size(); i++) {
            GraphicInfo graphicInfo = graphicInfoList.get(i);
            GraphicInfo previousGraphicInfo = graphicInfoList.get(i - 1);

            if (i == 1) {
                xPoints[0] = (int) previousGraphicInfo.getX();
                yPoints[0] = (int) previousGraphicInfo.getY();
            }
            xPoints[i] = (int) graphicInfo.getX();
            yPoints[i] = (int) graphicInfo.getY();

            length += Math.sqrt(Math.pow((int) graphicInfo.getX() - (int) previousGraphicInfo.getX(), 2)
                    + Math.pow((int) graphicInfo.getY() - (int) previousGraphicInfo.getY(), 2));
            lengths[i] = length;
        }
        m = length / 2;
        int p1 = 0;
        int p2 = 1;
        for (int i = 1; i < lengths.length; i++) {
            double len = lengths[i];
            p1 = i - 1;
            p2 = i;
            if (len > m) {
                break;
            }
        }

        GraphicInfo graphicInfo1 = graphicInfoList.get(p1);
        GraphicInfo graphicInfo2 = graphicInfoList.get(p2);

        double AB = (int) graphicInfo2.getX() - (int) graphicInfo1.getX();
        double OA = (int) graphicInfo2.getY() - (int) graphicInfo1.getY();
        double OB = lengths[p2] - lengths[p1];
        double ob = m - lengths[p1];
        double ab = AB * ob / OB;
        double oa = OA * ob / OB;

        double mx = graphicInfo1.getX() + ab;
        double my = graphicInfo1.getY() + oa;

        gi.setX(mx);
        gi.setY(my);
        return gi;
    }

    protected void drawArtifact(DirigibleProcessDiagramCanvas processDiagramCanvas, BpmnModel bpmnModel, Artifact artifact) {

        ArtifactDrawInstruction drawInstruction = artifactDrawInstructions.get(artifact.getClass());
        if (drawInstruction != null) {
            drawInstruction.draw(processDiagramCanvas, bpmnModel, artifact);
        }
    }

    private static void drawHighLight(DirigibleProcessDiagramCanvas processDiagramCanvas, GraphicInfo graphicInfo) {
        processDiagramCanvas.drawHighLight((int) graphicInfo.getX(), (int) graphicInfo.getY(), (int) graphicInfo.getWidth(),
                (int) graphicInfo.getHeight());

    }

    protected static DirigibleProcessDiagramCanvas initProcessDiagramCanvas(BpmnModel bpmnModel, String imageType, String activityFontName,
            String labelFontName, String annotationFontName, ClassLoader customClassLoader) {

        // We need to calculate maximum values to know how big the image will be in its entirety
        double minX = Double.MAX_VALUE;
        double maxX = 0;
        double minY = Double.MAX_VALUE;
        double maxY = 0;

        for (Pool pool : bpmnModel.getPools()) {
            GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(pool.getId());
            minX = graphicInfo.getX();
            maxX = graphicInfo.getX() + graphicInfo.getWidth();
            minY = graphicInfo.getY();
            maxY = graphicInfo.getY() + graphicInfo.getHeight();
        }

        List<FlowNode> flowNodes = gatherAllFlowNodes(bpmnModel);
        for (FlowNode flowNode : flowNodes) {

            GraphicInfo flowNodeGraphicInfo = bpmnModel.getGraphicInfo(flowNode.getId());

            // width
            if (flowNodeGraphicInfo.getX() + flowNodeGraphicInfo.getWidth() > maxX) {
                maxX = flowNodeGraphicInfo.getX() + flowNodeGraphicInfo.getWidth();
            }
            if (flowNodeGraphicInfo.getX() < minX) {
                minX = flowNodeGraphicInfo.getX();
            }
            // height
            if (flowNodeGraphicInfo.getY() + flowNodeGraphicInfo.getHeight() > maxY) {
                maxY = flowNodeGraphicInfo.getY() + flowNodeGraphicInfo.getHeight();
            }
            if (flowNodeGraphicInfo.getY() < minY) {
                minY = flowNodeGraphicInfo.getY();
            }

            for (SequenceFlow sequenceFlow : flowNode.getOutgoingFlows()) {
                List<GraphicInfo> graphicInfoList = bpmnModel.getFlowLocationGraphicInfo(sequenceFlow.getId());
                if (graphicInfoList != null) {
                    for (GraphicInfo graphicInfo : graphicInfoList) {
                        // width
                        if (graphicInfo.getX() > maxX) {
                            maxX = graphicInfo.getX();
                        }
                        if (graphicInfo.getX() < minX) {
                            minX = graphicInfo.getX();
                        }
                        // height
                        if (graphicInfo.getY() > maxY) {
                            maxY = graphicInfo.getY();
                        }
                        if (graphicInfo.getY() < minY) {
                            minY = graphicInfo.getY();
                        }
                    }
                }
            }
        }

        List<Artifact> artifacts = gatherAllArtifacts(bpmnModel);
        for (Artifact artifact : artifacts) {

            GraphicInfo artifactGraphicInfo = bpmnModel.getGraphicInfo(artifact.getId());

            if (artifactGraphicInfo != null) {
                // width
                if (artifactGraphicInfo.getX() + artifactGraphicInfo.getWidth() > maxX) {
                    maxX = artifactGraphicInfo.getX() + artifactGraphicInfo.getWidth();
                }
                if (artifactGraphicInfo.getX() < minX) {
                    minX = artifactGraphicInfo.getX();
                }
                // height
                if (artifactGraphicInfo.getY() + artifactGraphicInfo.getHeight() > maxY) {
                    maxY = artifactGraphicInfo.getY() + artifactGraphicInfo.getHeight();
                }
                if (artifactGraphicInfo.getY() < minY) {
                    minY = artifactGraphicInfo.getY();
                }
            }

            List<GraphicInfo> graphicInfoList = bpmnModel.getFlowLocationGraphicInfo(artifact.getId());
            if (graphicInfoList != null) {
                for (GraphicInfo graphicInfo : graphicInfoList) {
                    // width
                    if (graphicInfo.getX() > maxX) {
                        maxX = graphicInfo.getX();
                    }
                    if (graphicInfo.getX() < minX) {
                        minX = graphicInfo.getX();
                    }
                    // height
                    if (graphicInfo.getY() > maxY) {
                        maxY = graphicInfo.getY();
                    }
                    if (graphicInfo.getY() < minY) {
                        minY = graphicInfo.getY();
                    }
                }
            }
        }

        int nrOfLanes = 0;
        for (Process process : bpmnModel.getProcesses()) {
            for (Lane l : process.getLanes()) {

                nrOfLanes++;

                GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(l.getId());
                // // width
                if (graphicInfo.getX() + graphicInfo.getWidth() > maxX) {
                    maxX = graphicInfo.getX() + graphicInfo.getWidth();
                }
                if (graphicInfo.getX() < minX) {
                    minX = graphicInfo.getX();
                }
                // height
                if (graphicInfo.getY() + graphicInfo.getHeight() > maxY) {
                    maxY = graphicInfo.getY() + graphicInfo.getHeight();
                }
                if (graphicInfo.getY() < minY) {
                    minY = graphicInfo.getY();
                }
            }
        }

        // Special case, see https://activiti.atlassian.net/browse/ACT-1431
        if (flowNodes.isEmpty() && bpmnModel.getPools()
                                            .isEmpty()
                && nrOfLanes == 0) {
            // Nothing to show
            minX = 0;
            minY = 0;
        }

        return new DirigibleProcessDiagramCanvas((int) maxX + 10, (int) maxY + 10, (int) minX, (int) minY, imageType, activityFontName,
                labelFontName, annotationFontName, customClassLoader);
    }

    protected static List<Artifact> gatherAllArtifacts(BpmnModel bpmnModel) {
        List<Artifact> artifacts = new ArrayList<>();
        for (Process process : bpmnModel.getProcesses()) {
            artifacts.addAll(process.getArtifacts());
        }
        return artifacts;
    }

    protected static List<FlowNode> gatherAllFlowNodes(BpmnModel bpmnModel) {
        List<FlowNode> flowNodes = new ArrayList<>();
        for (Process process : bpmnModel.getProcesses()) {
            flowNodes.addAll(gatherAllFlowNodes(process));
        }
        return flowNodes;
    }

    protected static List<FlowNode> gatherAllFlowNodes(FlowElementsContainer flowElementsContainer) {
        List<FlowNode> flowNodes = new ArrayList<>();
        for (FlowElement flowElement : flowElementsContainer.getFlowElements()) {
            if (flowElement instanceof FlowNode) {
                flowNodes.add((FlowNode) flowElement);
            }
            if (flowElement instanceof FlowElementsContainer) {
                flowNodes.addAll(gatherAllFlowNodes((FlowElementsContainer) flowElement));
            }
        }
        return flowNodes;
    }

    protected boolean isPartOfCollapsedSubProcess(FlowElement flowElement, BpmnModel model) {
        SubProcess subProcess = flowElement.getSubProcess();
        if (subProcess != null) {
            GraphicInfo graphicInfo = model.getGraphicInfo(subProcess.getId());
            if (graphicInfo != null && graphicInfo.getExpanded() != null && !graphicInfo.getExpanded()) {
                return true;
            }

            return isPartOfCollapsedSubProcess(subProcess, model);
        }

        return false;
    }

    public Map<Class<? extends BaseElement>, ActivityDrawInstruction> getActivityDrawInstructions() {
        return activityDrawInstructions;
    }

    public void setActivityDrawInstructions(Map<Class<? extends BaseElement>, ActivityDrawInstruction> activityDrawInstructions) {
        this.activityDrawInstructions = activityDrawInstructions;
    }

    public Map<Class<? extends BaseElement>, ArtifactDrawInstruction> getArtifactDrawInstructions() {
        return artifactDrawInstructions;
    }

    public void setArtifactDrawInstructions(Map<Class<? extends BaseElement>, ArtifactDrawInstruction> artifactDrawInstructions) {
        this.artifactDrawInstructions = artifactDrawInstructions;
    }

    protected interface ActivityDrawInstruction {
        void draw(DirigibleProcessDiagramCanvas processDiagramCanvas, BpmnModel bpmnModel, FlowNode flowNode);
    }

    protected interface ArtifactDrawInstruction {
        void draw(DirigibleProcessDiagramCanvas processDiagramCanvas, BpmnModel bpmnModel, Artifact artifact);
    }

}
