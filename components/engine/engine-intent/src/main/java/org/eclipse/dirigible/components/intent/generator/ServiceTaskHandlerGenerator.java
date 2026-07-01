/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.ProcessIntent;
import org.eclipse.dirigible.components.intent.model.StepIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Scaffolds a Java {@code JavaDelegate} stub under {@code custom/} for every author-declared
 * service task with <b>no built-in handler</b> - i.e. one that is NOT a {@code call} (TS handler),
 * NOT a {@code setField} / {@code setRelationField} (which the BPMN binds to the generated
 * {@code gen.events.<Process><Step>} delegate), and NOT a {@code delegate} (an author-named client
 * {@code JavaDelegate} the BPMN binds via {@code flowable:class}). The BPMN binds such a bare task
 * to {@code ${JavaTask}} -> {@code custom.<Step>}; this writes {@code custom/<Step>.java} as a
 * minimal logging stub for the developer to implement.
 * <p>
 * <b>Generate-once, never overwritten.</b> {@code custom/} is the escape-hatch tier the intent
 * layer does not own, so the stub is written only when the file is absent (the developer's edits
 * survive regeneration). This is a deliberate one-time scaffold - the same exception the
 * {@code .settings} file makes to the "intent generators emit models, not code" rule - not ongoing
 * code generation. A {@code call} (TS handler) is left to the {@code ${JSTask}} path; a
 * {@code setField}/{@code setRelationField} gets the generated setter delegate - neither gets a
 * stub. (Because {@code custom/} is never scrubbed, a stub scaffolded before such a step gained a
 * {@code setField}/{@code setRelationField} stays orphaned and must be deleted by hand.)
 */
@Component
@Order(360)
public class ServiceTaskHandlerGenerator implements IntentTargetGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceTaskHandlerGenerator.class);

    @Override
    public String name() {
        return "service-task-handler";
    }

    @Override
    public void generate(IntentGenerationContext context) {
        IntentModel model = context.getModel();
        for (ProcessIntent process : model.getProcesses()) {
            for (StepIntent step : process.getSteps()) {
                String kind = step.getKind();
                if (!"serviceTask".equals(kind) && !"script".equals(kind)) {
                    continue;
                }
                if (step.getName() == null || step.getName()
                                                  .isBlank()) {
                    continue;
                }
                Object call = step.getArgs()
                                  .get("call");
                if (call != null && !call.toString()
                                         .isBlank()) {
                    continue; // external (TS) handler - not a Java stub
                }
                // A `delegate` service task is bound by the BPMN to an author-named client JavaDelegate via
                // flowable:class (a reusable, hand-written or cross-project delegate) - not a scaffolded stub.
                Object delegate = step.getArgs()
                                      .get("delegate");
                if (delegate != null && !delegate.toString()
                                                 .isBlank()) {
                    continue;
                }
                // A setField / setRelationField service task is bound by the BPMN to the GENERATED
                // gen.events.<Process><Step> delegate (SetFieldSupport -> setters glue), not a custom stub -
                // scaffolding one here produces an orphaned, never-invoked custom/<Step>.java.
                Object setField = step.getArgs()
                                      .get("setField");
                Object setRelationField = step.getArgs()
                                              .get("setRelationField");
                if ((setField != null && !setField.toString()
                                                  .isBlank())
                        || (setRelationField != null && !setRelationField.toString()
                                                                         .isBlank())) {
                    continue;
                }
                String handler = IntentNaming.pascalCase(step.getName());
                String fileName = "custom/" + handler + ".java";
                if (context.getRepository()
                           .getResource(context.getProjectRoot() + "/" + fileName)
                           .exists()) {
                    continue; // preserve the developer's implementation
                }
                context.writeModelFile(fileName, stub(process.getName(), step.getName(), handler));
                LOGGER.info("Scaffolded service-task handler stub [{}] (implement it - it will not be regenerated)", fileName);
            }
        }
    }

    private static String stub(String process, String step, String handler) {
        return """
                package custom;

                import org.flowable.engine.delegate.DelegateExecution;
                import org.flowable.engine.delegate.JavaDelegate;

                /**
                 * Service-task handler for the '%s' step of the %s process.
                 *
                 * Scaffolded once under custom/ - it is yours: implement the real logic here, it is never
                 * regenerated or overwritten.
                 */
                public class %s implements JavaDelegate {

                    @Override
                    public void execute(DelegateExecution execution) {
                        // TODO implement the '%s' service task.
                        System.out.println("%s: %s service task executed.");
                    }
                }
                """.formatted(step, process, handler, step, process, step);
    }
}
