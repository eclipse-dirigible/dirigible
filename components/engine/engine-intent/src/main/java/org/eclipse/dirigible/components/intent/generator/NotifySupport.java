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

    /** {@code attach: print} - the rendered print template of the record the block is about. */
    public static final String ATTACH_PRINT = "print";

    /**
     * {@code attach: recordPrint} - the rendered print template of a fan-out's <b>anchor record</b>
     * rather than of the row. The mirror of {@link #ATTACH_PRINT} inside a fan-out: the related rows
     * are only the recipient list (the invited suppliers of a request for quotation, the participants
     * of a meeting) and the document belongs to the record they hang off, so it is rendered ONCE and
     * the same PDF rides on every message.
     */
    public static final String ATTACH_RECORD_PRINT = "recordPrint";

    /**
     * The reserved placeholder scope a fan-out addresses its <b>anchor record</b> with -
     * {@code {record.<field>}} in a subject or body, while every bare path keeps resolving against the
     * ROW. The scope is explicit on purpose: when both records are addressable, an implicit rule is how
     * a message ends up quoting the wrong one, and nothing about the rendered text would show it.
     */
    public static final String RECORD_SCOPE = "record";

    /**
     * The local the generated code holds the fan-out's anchor record in - the name both fan-out
     * templates already give it, so a record-scoped expression pre-rendered here drops straight in.
     */
    public static final String RECORD_LOCAL = "source";

    /** The local the generated code holds the record a message is about in (a fan-out's ROW). */
    static final String ENTITY_LOCAL = "entity";

    /**
     * The run-time language fallback a render defaults to when the notify block declares neither
     * {@code language:} nor {@code languageFrom:} - the first entry of the tenant-resolved application
     * language set, read at send time. Shared with {@link SnapshotSupport}.
     */
    static final String DEFAULT_LANGUAGE_EXPRESSION = "org.eclipse.dirigible.sdk.print.Print.defaultLanguage()";

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
        return new FanOut(rows.getName(),
                IntentEntities.resolvePerspective(rows.getName(), compositionParents, IntentEntities.settingEntities(byName.values())),
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
     * The glue keys the reserved deep-link tokens contribute, always present so the templates can
     * compare them. The two {@code uses*} flags say which link locals the events template must declare;
     * the other two are the only facts the intent layer supplies towards the record link - the entity
     * and its key property. The ROUTE itself is assembled by that template, which is the layer that
     * knows the generated application's URL layout (the path-agnostic rule).
     *
     * <p>
     * In a fan-out {@code about} is the ROW, so {@code recordUrl} links the row - the thing that
     * message is about - exactly like every other bare path.
     *
     * @param plan the translated notify block, or {@code null} when nothing is sent
     * @param about the entity the message is about (a fan-out's row)
     * @return the {@code usesRecordUrl} / {@code usesInboxUrl} / {@code recordUrlEntity} /
     *         {@code recordUrlKeyProperty} keys
     */
    public static Map<String, Object> deepLinkFields(NotificationSupport.Plan plan, EntityIntent about) {
        boolean record = plan != null && plan.usesRecordUrl() && about != null;
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("usesRecordUrl", String.valueOf(record));
        fields.put("usesInboxUrl", String.valueOf(plan != null && plan.usesInboxUrl()));
        fields.put("recordUrlEntity", record ? about.getName() : "");
        fields.put("recordUrlKeyProperty", record ? IntentEntities.keyFieldName(about) : "");
        return fields;
    }

    /**
     * A resolved print attachment: everything the generated code needs to render and name the PDF.
     *
     * @param entity the document entity whose print template is rendered
     * @param languageExpression a Java expression yielding the template language code - a quoted
     *        literal ({@code language:}), a null-safe read off the {@link #languageLoad} local
     *        ({@code languageFrom:}), or the run-time application-language fallback
     * @param fileNameExpression a Java expression for the attachment file name, evaluated against the
     *        loaded record - the authored {@code fileName:} pattern, or, absent one, the document
     *        number when the entity has one, else the entity name + id
     * @param fileNameLoads the one-hop relation loads a {@code fileName:} pattern reads, on top of the
     *        ones the message text already needs - the caller merges them into the block's relation
     *        loads (they share the local named after the relation, so one load serves both)
     * @param languageLoad the {@code languageFrom} relation load backing the expression, or
     *        {@code null} when the language needs no related record
     * @param anchorScoped whether the rendered record is a fan-out's anchor ({@code recordPrint})
     *        instead of the record the block is about - the expressions then read the
     *        {@link #RECORD_LOCAL} local and the render happens once, outside the per-row loop
     */
    public record PrintAttachment(String entity, String languageExpression, String fileNameExpression,
            List<NotificationSupport.RelationLoad> fileNameLoads, LanguageLoad languageLoad, boolean anchorScoped) {

        /** @return the authored {@code attach} value this attachment came from. */
        public String kind() {
            return anchorScoped ? ATTACH_RECORD_PRINT : ATTACH_PRINT;
        }
    }

    /**
     * The related record a {@code languageFrom: relation.field} language is read from - loaded by the
     * generated code into an {@code attachLanguageSource} local off the record's FK.
     *
     * @param fkProperty the record's to-one FK property (PascalCase)
     * @param targetEntity the relation's target entity
     * @param targetPerspective the target's perspective (its generated data subfolder)
     * @param crossModel whether the target is owned by another model
     * @param targetModel the owner model's alias when {@link #crossModel}, else empty
     */
    public record LanguageLoad(String fkProperty, String targetEntity, String targetPerspective, boolean crossModel, String targetModel) {
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
     * @return whether it asks for a rendered document to be attached, of either scope
     */
    public static boolean attachesPrint(NotificationIntent notify) {
        return attaches(notify, ATTACH_PRINT) || attaches(notify, ATTACH_RECORD_PRINT);
    }

    /**
     * @param notify a notify block, may be {@code null}
     * @return whether it attaches the fan-out's ANCHOR record ({@code attach: recordPrint}) rather than
     *         the record the block is about
     */
    public static boolean attachesRecordPrint(NotificationIntent notify) {
        return attaches(notify, ATTACH_RECORD_PRINT);
    }

    /**
     * Whether the block's text addresses the fan-out's anchor record ({@code {record.<field>}}). The
     * generated per-row send method then takes the record as a parameter - it is passed only when a
     * message actually quotes it, so nothing carries an argument it never reads.
     *
     * @param notify a notify block, may be {@code null}
     * @return whether a subject or body placeholder is record-scoped
     */
    public static boolean usesRecordScope(NotificationIntent notify) {
        String marker = "{" + RECORD_SCOPE + ".";
        return notify != null && (notify.getSubject() != null && notify.getSubject()
                                                                       .contains(marker)
                || notify.getBody() != null && notify.getBody()
                                                     .contains(marker));
    }

    private static boolean attaches(NotificationIntent notify, String kind) {
        return notify != null && notify.getAttach() != null && kind.equalsIgnoreCase(notify.getAttach()
                                                                                           .trim());
    }

    /**
     * Resolve the print attachment of a notify block against the entity it is about.
     *
     * @param notify the notify block
     * @param entity the entity the message is about
     * @param model the parsed intent model (to test the entity is a document master)
     * @param byName all LOCAL entities by name (to resolve a same-model {@code languageFrom} target)
     * @param compositionParents composition-parent map (to resolve a target's perspective)
     * @param crossModel resolver for a cross-model {@code languageFrom} relation, or {@code null}
     * @return the attachment, or {@code null} when none was asked for or the entity has no printable
     *         document shape (no line-items child, so no generated feeder and no print template)
     * @throws IllegalArgumentException when a declared {@code languageFrom} path does not resolve - the
     *         caller reports the drop with the precise reason instead of mailing wrong-language copies
     */
    public static PrintAttachment printAttachment(NotificationIntent notify, EntityIntent entity, IntentModel model,
            Map<String, EntityIntent> byName, Map<String, String> compositionParents, NotificationSupport.CrossModelLookup crossModel) {
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
        // `recordPrint` renders the fan-out's anchor, which the generated code holds in its own local -
        // so every expression below is rendered against that local instead of the per-row one.
        boolean anchorScoped = attachesRecordPrint(notify);
        String local = anchorScoped ? RECORD_LOCAL : ENTITY_LOCAL;
        // The file name is resolved once, whatever the language knob turns out to be: it reads the same
        // record through the same local, and an unresolvable pattern must be reported before any of the
        // language shapes below can return an attachment.
        FileName fileName = fileName(notify, entity, local, anchorScoped, byName, compositionParents, crossModel);
        String literal = notify.getLanguage();
        if (literal != null && !literal.isBlank()) {
            return new PrintAttachment(entity.getName(), "\"" + literal.trim() + "\"", fileName.expression(), fileName.loads(), null,
                    anchorScoped);
        }
        String path = notify.getLanguageFrom();
        if (path == null || path.isBlank()) {
            return new PrintAttachment(entity.getName(), DEFAULT_LANGUAGE_EXPRESSION, fileName.expression(), fileName.loads(), null,
                    anchorScoped);
        }
        return languageFromAttachment(path.trim(), entity, byName, compositionParents, crossModel, anchorScoped, local, fileName);
    }

    /**
     * The resolved attachment file name: the Java expression plus the relation loads it reads.
     */
    private record FileName(String expression, List<NotificationSupport.RelationLoad> loads) {
    }

    /**
     * The authored {@code fileName:} pattern, or the default (the document number, else the entity name
     * plus the record id) when the block declares none. A {@code recordPrint} renders the fan-out's
     * anchor ONCE, in its own method, where the per-row relation locals do not exist - so a relation
     * hop is refused there rather than emitting a read of an undeclared local.
     */
    private static FileName fileName(NotificationIntent notify, EntityIntent entity, String local, boolean anchorScoped,
            Map<String, EntityIntent> byName, Map<String, String> compositionParents, NotificationSupport.CrossModelLookup crossModel) {
        FileNameSupport.Site site = new FileNameSupport.Site(entity, local, !anchorScoped, false);
        FileNameSupport.Resolved resolved = FileNameSupport.resolve(notify.getFileName(), site, byName, compositionParents, crossModel);
        if (resolved == null) {
            return new FileName(FileNameSupport.numberOrId(entity, local) + " + \".pdf\"", List.of());
        }
        return new FileName(resolved.expression() + " + \".pdf\"", resolved.loads());
    }

    /**
     * The {@code languageFrom: relation.field} shape: the generated code loads the related record into
     * an {@code attachLanguageSource} local and reads the language off it, falling back to the
     * application language set when the chain is null/blank.
     */
    private static PrintAttachment languageFromAttachment(String path, EntityIntent entity, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents, NotificationSupport.CrossModelLookup crossModel, boolean anchorScoped, String local,
            FileName fileName) {
        int dot = path.indexOf('.');
        if (dot < 0) {
            throw new IllegalArgumentException(
                    "languageFrom [" + path + "] must be a one-hop relation.field path on [" + entity.getName() + "]");
        }
        String relationName = path.substring(0, dot)
                                  .trim();
        String fieldName = path.substring(dot + 1)
                               .trim();
        RelationIntent relation = null;
        for (RelationIntent candidate : entity.getRelations()) {
            boolean toOne = "manyToOne".equals(candidate.getKind()) || "oneToOne".equals(candidate.getKind());
            if (toOne && relationName.equals(candidate.getName()) && candidate.getTo() != null) {
                relation = candidate;
            }
        }
        if (relation == null) {
            throw new IllegalArgumentException(
                    "languageFrom [" + path + "]: [" + relationName + "] is not a to-one relation of [" + entity.getName() + "]");
        }
        String pascalField = IntentNaming.pascalCase(fieldName);
        LanguageLoad load;
        boolean isCrossModel = relation.getModel() != null && !relation.getModel()
                                                                       .isBlank();
        if (isCrossModel) {
            NotificationSupport.CrossModelTarget target = crossModel == null ? null : crossModel.resolve(relation);
            if (target == null || (target.propertyNames() != null && !target.propertyNames()
                                                                            .contains(pascalField))) {
                throw new IllegalArgumentException(
                        "languageFrom [" + path + "]: [" + fieldName + "] could not be resolved on the cross-model target ["
                                + relation.getTo() + "] of model [" + relation.getModel() + "]");
            }
            load = new LanguageLoad(IntentNaming.pascalCase(relationName), relation.getTo(), target.perspectiveName(), true,
                    target.modelAlias());
        } else {
            EntityIntent target = byName.get(relation.getTo());
            if (target == null || fieldOf(target, fieldName) == null) {
                throw new IllegalArgumentException(
                        "languageFrom [" + path + "]: [" + fieldName + "] is not a field of [" + relation.getTo() + "]");
            }
            load = new LanguageLoad(IntentNaming.pascalCase(relationName), relation.getTo(), IntentEntities.resolvePerspective(
                    relation.getTo(), compositionParents, IntentEntities.settingEntities(byName.values())), false, "");
        }
        String expression = "attachLanguageSource == null || attachLanguageSource." + pascalField + " == null || attachLanguageSource."
                + pascalField + ".isBlank() ? " + DEFAULT_LANGUAGE_EXPRESSION + " : attachLanguageSource." + pascalField + ".trim()";
        return new PrintAttachment(entity.getName(), expression, fileName.expression(), fileName.loads(), load, anchorScoped);
    }

    private static FieldIntent fieldOf(EntityIntent entity, String name) {
        for (FieldIntent field : entity.getFields()) {
            if (name.equals(field.getName())) {
                return field;
            }
        }
        return null;
    }

    /**
     * The glue keys every notify-bearing descriptor carries, pre-resolved so the Velocity templates
     * hold no expression logic. Always present (empty strings when there is no attachment) - an
     * undefined Velocity variable renders as its own literal name instead of failing, so a template
     * must never depend on a key being absent.
     *
     * @param attachment the resolved attachment, or {@code null} for a plain-text message
     * @return the {@code attach} / {@code attachEntity} / {@code attachLanguageExpression} /
     *         {@code attachFileNameExpression} keys plus the {@code attachLanguage*} load coordinates
     */
    public static Map<String, Object> attachmentFields(PrintAttachment attachment) {
        Map<String, Object> fields = new LinkedHashMap<>();
        // The authored kind, not a constant: the fan-out templates branch on it to render the anchor
        // record's document once, outside the per-row loop.
        fields.put("attach", attachment == null ? "" : attachment.kind());
        fields.put("attachEntity", attachment == null ? "" : attachment.entity());
        fields.put("attachLanguageExpression", attachment == null ? "" : attachment.languageExpression());
        fields.put("attachFileNameExpression", attachment == null ? "" : attachment.fileNameExpression());
        LanguageLoad load = attachment == null ? null : attachment.languageLoad();
        fields.put("attachLanguageFkProperty", load == null ? "" : load.fkProperty());
        fields.put("attachLanguageTargetEntity", load == null ? "" : load.targetEntity());
        fields.put("attachLanguageTargetPerspective", load == null ? "" : load.targetPerspective());
        fields.put("attachLanguageCrossModel", load != null && load.crossModel());
        fields.put("attachLanguageTargetModel", load == null ? "" : load.targetModel());
        return fields;
    }

    private static String keyAccessor(EntityIntent owner) {
        FieldIntent pk = IntentEntities.primaryKeyOf(owner);
        String type = pk == null || pk.getType() == null ? "integer" : pk.getType();
        return "long".equals(type) ? "longValue" : "intValue";
    }
}
