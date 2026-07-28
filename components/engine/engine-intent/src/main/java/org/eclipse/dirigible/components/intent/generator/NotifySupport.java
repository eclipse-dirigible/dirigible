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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.generator.print.PrintIntentGenerator;
import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.NotificationIntent;
import org.eclipse.dirigible.components.intent.model.RelationIntent;
import org.eclipse.dirigible.components.intent.model.ProcessIntent;
import org.eclipse.dirigible.components.intent.model.StepIntent;

/**
 * The reusable <b>notify block</b> - "send a message about this record", with the record's rendered
 * document optionally attached. One shape ({@code to} / {@code subject} / {@code body} /
 * {@code attach} / {@code language}, see {@link NotificationIntent}) authored at four call sites:
 *
 * <ul>
 * <li>{@code notifications[]} - on an entity lifecycle event (the original standalone form),</li>
 * <li>{@code schedules[].notify} - per row matched by a cron query,</li>
 * <li>{@code transitions[].notify} - after a guarded on-demand status flip ("on Void, mail the
 * counterparty"),</li>
 * <li>a {@code serviceTask}'s {@code args.notify} - at a step of a process ("after Issue, mail the
 * invoice to its customer").</li>
 * </ul>
 *
 * <p>
 * {@link NotificationSupport} translates the recipient / subject / body paths; this class adds the
 * <b>attachment</b> half and enumerates the process-step senders. {@code attach: print} renders the
 * record's {@code .print} template server-side (the generated {@code <Entity>PrintFeeder} assembles
 * the {@code {document, items}} payload, {@code sdk.print.Print} renders the PDF - the same two
 * steps the snapshot delegate takes) and attaches the result, so mailing a business document needs
 * no hand-written listener around the print engine.
 *
 * <p>
 * Only a <b>document master</b> (an entity with a line-items child, hence a generated feeder and a
 * {@code .print} template) can be attached; {@link #printAttachment} returns {@code null} otherwise
 * and the caller reports the drop rather than sending a mail that claims an attachment it lacks.
 */
public final class NotifySupport {

    /** The only {@code attach} value today: the record's own rendered print template. */
    public static final String ATTACH_PRINT = "print";

    /** The print-template language an {@code attach} defaults to. */
    private static final String DEFAULT_LANGUAGE = "en";

    private NotifySupport() {}

    /**
     * A resolved <b>fan-out</b>: the notify block sends one message per row of a related entity instead
     * of one about the record. The rows are the ones whose {@code fkProperty} points back at the
     * record, and every path in the block resolves against the ROW.
     *
     * @param entity the related entity iterated (e.g. {@code Payslip})
     * @param perspective its resolved perspective (its generated data subfolder)
     * @param fkProperty the row's to-one FK back to the record (PascalCase, e.g. {@code PayrollRun})
     * @param keyProperty the row's own primary-key property
     */
    public record FanOut(String entity, String perspective, String fkProperty, String keyProperty) {
    }

    /**
     * Resolve a notify block's {@code forEach} against the entity the block is about.
     *
     * @param notify the notify block, may be {@code null}
     * @param about the entity the block is attached to (a process's trigger, a transition's forEntity)
     * @param byName all entities by name
     * @param compositionParents composition-parent map (to resolve the row entity's perspective)
     * @return the fan-out, or {@code null} when none was asked for or it cannot be resolved (no such
     *         entity, or no unique to-one relation from it back to {@code about})
     */
    public static FanOut fanOut(NotificationIntent notify, EntityIntent about, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents) {
        if (notify == null || notify.getForEach() == null || notify.getForEach()
                                                                   .isBlank()
                || about == null) {
            return null;
        }
        EntityIntent rows = byName.get(notify.getForEach()
                                             .trim());
        if (rows == null) {
            return null;
        }
        RelationIntent back = backReference(rows, about.getName());
        if (back == null) {
            return null;
        }
        return new FanOut(rows.getName(), IntentEntities.resolvePerspective(rows.getName(), compositionParents),
                IntentNaming.pascalCase(back.getName()), IntentEntities.keyFieldName(rows));
    }

    /**
     * The row entity's single to-one relation back to the record's entity. Exactly one is required:
     * with two (say a document that references the same master twice) the intended collection is
     * ambiguous, and guessing would silently mail the wrong set.
     */
    private static RelationIntent backReference(EntityIntent rows, String about) {
        RelationIntent found = null;
        for (RelationIntent relation : rows.getRelations()) {
            boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
            if (toOne && about.equals(relation.getTo())) {
                if (found != null) {
                    return null; // ambiguous
                }
                found = relation;
            }
        }
        return found;
    }

    /**
     * The glue keys a fan-out contributes, always present so the templates can compare them ({@code
     * forEach} empty = send one message about the record itself).
     *
     * @param fanOut the resolved fan-out, or {@code null}
     * @return the {@code forEach} / {@code forEachPerspective} / {@code forEachFkProperty} /
     *         {@code forEachKeyProperty} keys
     */
    public static Map<String, Object> fanOutFields(FanOut fanOut) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("forEach", fanOut == null ? "" : fanOut.entity());
        fields.put("forEachPerspective", fanOut == null ? "" : fanOut.perspective());
        fields.put("forEachFkProperty", fanOut == null ? "" : fanOut.fkProperty());
        // The row's own PK: the fan-out logs per row, and it does so whether or not a document is
        // attached (attachKeyProperty is empty without an attachment).
        fields.put("forEachKeyProperty", fanOut == null ? "" : fanOut.keyProperty());
        return fields;
    }

    /**
     * A resolved print attachment: everything the generated code needs to render and name the PDF.
     *
     * @param entity the document entity whose print template is rendered
     * @param language the template language code
     * @param fileNameExpression a Java expression for the attachment file name, evaluated against the
     *        loaded record (the document number when the entity has one, else the entity name + id)
     */
    public record PrintAttachment(String entity, String language, String fileNameExpression) {
    }

    /**
     * A process step that sends: the generated {@code <Process><Step>Send} {@code JavaDelegate} the
     * BPMN service task binds, sending about the process's trigger entity.
     *
     * @param process the owning process
     * @param step the {@code serviceTask} step that declared the {@code notify}
     * @param className the generated delegate's simple name
     * @param entity the trigger entity the message is about
     * @param keyProperty the process variable holding the record's PK
     * @param keyAccessor the {@link Number} accessor matching the PK type
     * @param block the authored notify block ({@code notify} is not a legal record component name -
     *        {@code Object.notify()})
     */
    public record Sender(String process, String step, String className, String entity, String keyProperty, String keyAccessor,
            NotificationIntent block) {
    }

    /**
     * @param step a process step
     * @return the step's {@code args.notify} block as a {@link NotificationIntent}, or {@code null}
     *         when the step declares none (or declares it with the wrong shape)
     */
    public static NotificationIntent stepNotify(StepIntent step) {
        return NotificationIntent.fromMap(step == null || step.getArgs() == null ? null
                : step.getArgs()
                      .get("notify"));
    }

    /**
     * Every process-step sender in the model, in declaration order. A sender needs a trigger entity
     * (the record the mail is about is the one the process runs on), so a process without a resolvable
     * trigger contributes none.
     *
     * @param model the parsed intent model
     * @return the senders (possibly empty)
     */
    public static List<Sender> senders(IntentModel model) {
        List<Sender> senders = new ArrayList<>();
        Map<String, EntityIntent> byName = IntentEntities.byName(model);
        for (ProcessIntent process : model.getProcesses()) {
            String triggerEntity = TriggerSupport.triggerEntity(process);
            EntityIntent owner = triggerEntity == null ? null : byName.get(triggerEntity);
            if (owner == null || process.getName() == null) {
                continue;
            }
            for (StepIntent step : process.getSteps()) {
                if (step.getName() == null || !"serviceTask".equals(step.getKind())) {
                    continue;
                }
                NotificationIntent notify = stepNotify(step);
                if (notify == null) {
                    continue;
                }
                senders.add(new Sender(process.getName(), step.getName(), className(process.getName(), step.getName()), triggerEntity,
                        IntentEntities.keyFieldName(owner), keyAccessor(owner), notify));
            }
        }
        return senders;
    }

    /**
     * The generated delegate class name for a sending step - the process name plus the step name, both
     * PascalCase, suffixed {@code Send} (so it cannot collide with the {@code setField} setter class of
     * a step with the same name).
     *
     * @param process the process name
     * @param step the step name
     * @return the simple class name
     */
    public static String className(String process, String step) {
        return IntentNaming.pascalCase(process) + IntentNaming.pascalCase(step) + "Send";
    }

    /**
     * @param notify a notify block, may be {@code null}
     * @return whether it asks for the record's print to be attached
     */
    public static boolean attachesPrint(NotificationIntent notify) {
        return notify != null && notify.getAttach() != null && ATTACH_PRINT.equalsIgnoreCase(notify.getAttach()
                                                                                                   .trim());
    }

    /**
     * Resolve the print attachment of a notify block against the entity it is about.
     *
     * @param notify the notify block
     * @param entity the entity the message is about
     * @param model the parsed intent model (to test the entity is a document master)
     * @return the attachment, or {@code null} when none was asked for or the entity has no printable
     *         document shape (no line-items child, so no generated feeder and no print template)
     */
    public static PrintAttachment printAttachment(NotificationIntent notify, EntityIntent entity, IntentModel model) {
        if (!attachesPrint(notify) || entity == null) {
            return null;
        }
        boolean printable = false;
        for (EntityIntent master : PrintIntentGenerator.documentMasters(model)
                                                       .keySet()) {
            printable = printable || master.getName()
                                           .equals(entity.getName());
        }
        if (!printable) {
            return null;
        }
        String language = notify.getLanguage() == null || notify.getLanguage()
                                                                .isBlank() ? DEFAULT_LANGUAGE
                                                                        : notify.getLanguage()
                                                                                .trim();
        return new PrintAttachment(entity.getName(), language, fileNameExpression(entity));
    }

    /**
     * The attachment file name: the document's own number when the entity declares a {@code number:}
     * field (so the customer receives {@code SI00000042.pdf}, not {@code SalesInvoice 42.pdf}), falling
     * back to the entity name plus the record id. Rendered as a Java expression over the loaded
     * {@code entity} local of the generated code.
     */
    private static String fileNameExpression(EntityIntent entity) {
        String keyProperty = IntentEntities.keyFieldName(entity);
        for (FieldIntent field : entity.getFields()) {
            if (field.getNumber() != null && field.getName() != null) {
                String number = "entity." + IntentNaming.pascalCase(field.getName());
                return "(" + number + " == null || " + number + ".isBlank() ? \"" + entity.getName() + " \" + entity." + keyProperty + " : "
                        + number + ") + \".pdf\"";
            }
        }
        return "\"" + entity.getName() + " \" + entity." + keyProperty + " + \".pdf\"";
    }

    /**
     * The glue keys every notify-bearing descriptor carries, pre-resolved so the Velocity templates
     * hold no expression logic. Always present (empty strings when there is no attachment) - an
     * undefined Velocity variable renders as its own literal name instead of failing, so a template
     * must never depend on a key being absent.
     *
     * @param attachment the resolved attachment, or {@code null} for a plain-text message
     * @return the {@code attach} / {@code attachEntity} / {@code attachLanguage} /
     *         {@code attachFileNameExpression} keys
     */
    public static Map<String, Object> attachmentFields(PrintAttachment attachment) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("attach", attachment == null ? "" : ATTACH_PRINT);
        fields.put("attachEntity", attachment == null ? "" : attachment.entity());
        fields.put("attachLanguage", attachment == null ? "" : attachment.language());
        fields.put("attachFileNameExpression", attachment == null ? "" : attachment.fileNameExpression());
        return fields;
    }

    private static String keyAccessor(EntityIntent owner) {
        FieldIntent pk = IntentEntities.primaryKeyOf(owner);
        String type = pk == null || pk.getType() == null ? "integer" : pk.getType();
        return "long".equals(type) ? "longValue" : "intValue";
    }
}
