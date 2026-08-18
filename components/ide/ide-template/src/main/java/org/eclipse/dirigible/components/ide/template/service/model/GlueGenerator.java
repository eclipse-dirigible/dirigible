/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.template.service.model;

import org.eclipse.dirigible.commons.api.helpers.NamingHelper;
import org.eclipse.dirigible.components.ide.template.domain.GenerationTemplateMetadataSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.dirigible.components.ide.template.service.model.ModelValues.asMaps;
import static org.eclipse.dirigible.components.ide.template.service.model.ModelValues.str;
import static org.eclipse.dirigible.components.ide.template.service.model.ModelValues.strOr;
import static org.eclipse.dirigible.components.ide.template.service.model.ModelValues.truthy;

/**
 * Generates the glue artefacts of a model - the listeners, delegates, jobs and controllers that
 * wire an application's processes, notifications, roll-ups and postings together.
 *
 * <p>
 * Unlike the entity collections, a glue collection is not entity-shaped: each entry is a descriptor
 * the intent layer already resolved, so almost everything here is marshalling one descriptor into
 * one template context. What is genuinely computed is confined to a handful of places - the roll-up
 * coalescing, the aggregate variants, the period step of an expansion, and the fully-qualified
 * class names that only the generation layer knows the package layout for.
 *
 * <p>
 * Two conventions run through every case. A Java package segment is the <em>sanitized</em>
 * perspective, while an event topic keeps the <em>raw</em> perspective, because that is what the
 * repository publishes to. And a cross-model reference resolves against the owner model's
 * generation folder rather than this project's.
 */
class GlueGenerator {

    /** The names of the collections this generator handles. */
    private static final List<String> COLLECTIONS = List.of("triggers", "resolvers", "fieldLoaders", "assignees", "timerLoaders", "waits",
            "aborts", "setters", "writers", "notifications", "schedules", "integrations", "inbound", "inboundMessages", "inboundFiles",
            "stepEvents", "rollups", "expansions", "settlements", "generates", "generateEvents", "transitions", "sends", "posts",
            "aggregates", "postings", "printFeeders", "snapshots", "numbering", "resolves");

    /** The renderer. */
    private final ModelTemplateRenderer renderer;

    /**
     * Instantiates a new glue generator.
     *
     * @param renderer the renderer
     */
    GlueGenerator(ModelTemplateRenderer renderer) {
        this.renderer = renderer;
    }

    /**
     * Tests whether a collection is a glue collection.
     *
     * @param collection the collection name
     * @return true when this generator handles it
     */
    static boolean handles(String collection) {
        return COLLECTIONS.contains(collection);
    }

    /**
     * Generates one glue collection.
     *
     * @param collection the collection name
     * @param source the template source
     * @param content the template content
     * @param model the model
     * @param parameters the generation parameters
     * @return the generated files, empty when the model declares nothing for this collection
     * @throws IOException when rendering fails
     */
    List<GeneratedFile> generate(String collection, GenerationTemplateMetadataSource source, String content, Map<String, Object> model,
            Map<String, Object> parameters) throws IOException {
        return switch (collection) {
            case "triggers" -> each(collection, source, content, model, parameters, GlueGenerator::bindTrigger);
            case "resolvers" -> each(collection, source, content, model, parameters, GlueGenerator::bindResolver);
            case "fieldLoaders" -> each(collection, source, content, model, parameters, GlueGenerator::bindFieldLoader);
            case "assignees" -> each(collection, source, content, model, parameters, GlueGenerator::bindAssignee);
            case "timerLoaders" -> each(collection, source, content, model, parameters, GlueGenerator::bindTimerLoader);
            case "waits" -> each(collection, source, content, model, parameters, GlueGenerator::bindWait);
            case "aborts" -> each(collection, source, content, model, parameters, GlueGenerator::bindAbort);
            case "setters" -> each(collection, source, content, model, parameters, GlueGenerator::bindSetter);
            case "writers" -> each(collection, source, content, model, parameters, GlueGenerator::bindWriter);
            case "notifications" -> each(collection, source, content, model, parameters, GlueGenerator::bindNotification);
            case "schedules" -> each(collection, source, content, model, parameters, GlueGenerator::bindSchedule);
            case "integrations" -> each(collection, source, content, model, parameters, GlueGenerator::bindIntegration);
            case "inbound" -> each(collection, source, content, model, parameters, GlueGenerator::bindInbound);
            case "inboundMessages" -> each(collection, source, content, model, parameters, GlueGenerator::bindInboundMessage);
            case "inboundFiles" -> each(collection, source, content, model, parameters, GlueGenerator::bindInboundFile);
            case "stepEvents" -> each(collection, source, content, model, parameters, GlueGenerator::bindStepEvent);
            case "expansions" -> each(collection, source, content, model, parameters, GlueGenerator::bindExpansion);
            case "settlements" -> each(collection, source, content, model, parameters, GlueGenerator::bindSettlement);
            // Both collections carry the SAME create-from descriptors (generateEvents is the
            // event-driven subset), so they share one binding - the listener and the create-from it
            // calls cannot be rendered from divergent data.
            case "generates", "generateEvents" -> each(collection, source, content, model, parameters, GlueGenerator::bindGenerate);
            case "transitions" -> each(collection, source, content, model, parameters, GlueGenerator::bindTransition);
            case "sends" -> each(collection, source, content, model, parameters, GlueGenerator::bindSend);
            case "posts" -> each(collection, source, content, model, parameters, GlueGenerator::bindPost);
            case "postings" -> each(collection, source, content, model, parameters, GlueGenerator::bindPosting);
            case "printFeeders" -> each(collection, source, content, model, parameters, GlueGenerator::bindPrintFeeder);
            case "snapshots" -> each(collection, source, content, model, parameters, GlueGenerator::bindSnapshot);
            case "numbering" -> each(collection, source, content, model, parameters, GlueGenerator::bindNumbering);
            case "resolves" -> each(collection, source, content, model, parameters, GlueGenerator::bindResolve);
            case "rollups" -> rollups(source, content, model, parameters);
            case "aggregates" -> aggregates(source, content, model, parameters);
            default -> List.of();
        };
    }

    /**
     * Binds and renders one file per entry of a collection.
     *
     * @param collection the collection name
     * @param source the template source
     * @param content the template content
     * @param model the model
     * @param parameters the generation parameters
     * @param binder the per-entry binding
     * @return the generated files
     * @throws IOException when rendering fails
     */
    private List<GeneratedFile> each(String collection, GenerationTemplateMetadataSource source, String content, Map<String, Object> model,
            Map<String, Object> parameters, Binder binder) throws IOException {
        List<GeneratedFile> files = new ArrayList<>();
        for (Map<String, Object> item : asMaps(model.get(collection))) {
            Map<String, Object> context = ModelValues.copy(parameters);
            binder.bind(item, context, parameters);
            files.add(render(source, content, ModelValues.cleaned(context)));
        }
        return files;
    }

    /**
     * Renders one file.
     *
     * @param source the template source
     * @param content the template content
     * @param context the template context
     * @return the generated file
     * @throws IOException when rendering fails
     */
    private GeneratedFile render(GenerationTemplateMetadataSource source, String content, Map<String, Object> context) throws IOException {
        return new GeneratedFile(source.getLocation(), renderer.renderPath(source.getLocation(), source.getRename(), context),
                renderer.render(source, content, context));
    }

    /**
     * Binds a process trigger - the listener that starts a process when a record is created.
     *
     * @param item the descriptor
     * @param context the template context
     * @param parameters the generation parameters
     */
    private static void bindTrigger(Map<String, Object> item, Map<String, Object> context, Map<String, Object> parameters) {
        copy(context, item, "process", "entity", "perspective", "keyProperty", "businessKeyProperty", "generateBusinessKey", "topicSuffix",
                "guardExpression", "personalFkProperty", "personalIdentityProperty");
        context.put("javaPerspective", sanitize(item, "perspective"));
        // The identity repository the listener resolves a personal task assignee through. A
        // cross-model target resolves against the owner model's generation folder.
        if (truthy(item, "personalFkProperty")) {
            String genFolder =
                    truthy(item, "personalCrossModel") ? sanitize(item, "personalTargetModel") : str(parameters, "javaGenFolderName");
            context.put("personalIdentityRepositoryClass", "gen." + genFolder + ".data." + sanitize(item, "personalTargetPerspective") + "."
                    + str(item, "personalTargetEntity") + "Repository");
        } else {
            context.remove("personalIdentityRepositoryClass");
        }
        context.put("relationLinks", relationLinks(item.get("relationLinks"), parameters));
    }

    /**
     * Assembles the target controller URL of each to-one relation the trigger's task list links to. The
     * intent layer carries only logical names; the path layout is known here.
     *
     * @param raw the declared links
     * @param parameters the generation parameters
     * @return the resolved links
     */
    private static List<Object> relationLinks(Object raw, Map<String, Object> parameters) {
        List<Object> links = new ArrayList<>();
        for (Map<String, Object> link : asMaps(raw)) {
            boolean crossModel = truthy(link, "crossModel");
            String targetGenFolder = crossModel ? sanitize(link, "targetModel") : str(parameters, "javaGenFolderName");
            String targetProject = crossModel ? str(link, "targetProject") : str(parameters, "projectName");
            Map<String, Object> resolved = new LinkedHashMap<>();
            resolved.put("fkProperty", link.get("fkProperty"));
            resolved.put("labelField", link.get("labelField"));
            resolved.put("url", "/services/java/" + targetProject + "/gen/" + targetGenFolder + "/api/"
                    + sanitize(link, "targetPerspective") + "/" + str(link, "targetEntity") + "Controller");
            links.add(resolved);
        }
        return links;
    }

    /**
     * Binds a decision resolver - the delegate that publishes a referenced field before a gateway.
     *
     * @param item the descriptor
     * @param context the template context
     * @param parameters the generation parameters
     */
    private static void bindResolver(Map<String, Object> item, Map<String, Object> context, Map<String, Object> parameters) {
        copy(context, item, "process", "handler", "fkProperty", "targetEntity", "targetPerspective", "targetField", "targetIdAccessor",
                "variable", "ownerEntity", "ownerPerspective", "ownerKeyProperty", "ownerKeyAccessor");
        context.put("javaTargetPerspective", sanitize(item, "targetPerspective"));
        context.put("javaOwnerPerspective", sanitize(item, "ownerPerspective"));
    }

    /**
     * Binds an own-field decision loader - the delegate that loads the trigger record and publishes the
     * fields a gateway branches on.
     *
     * @param item the descriptor
     * @param context the template context
     * @param parameters the generation parameters
     */
    private static void bindFieldLoader(Map<String, Object> item, Map<String, Object> context, Map<String, Object> parameters) {
        copy(context, item, "process", "handler", "ownerEntity", "ownerPerspective", "ownerKeyProperty", "ownerKeyAccessor", "fields");
        context.put("javaOwnerPerspective", sanitize(item, "ownerPerspective"));
    }

    /**
     * Binds a user-task assignee resolver - the delegate that walks the trigger record's relations to
     * the person a task belongs to and publishes their login.
     *
     * @param item the descriptor
     * @param context the template context
     * @param parameters the generation parameters
     */
    private static void bindAssignee(Map<String, Object> item, Map<String, Object> context, Map<String, Object> parameters) {
        copy(context, item, "process", "step", "handler", "variable", "path", "ownerEntity", "ownerPerspective", "ownerKeyProperty",
                "ownerKeyAccessor", "firstFkProperty", "identityLocal", "identityProperty");
        context.put("javaOwnerPerspective", sanitize(item, "ownerPerspective"));
        context.put("hops", assigneeHops(item.get("hops"), parameters));
    }

    /**
     * Resolves each hop of an assignee walk to the generated classes it loads. The intent layer carries
     * only logical names; the package layout is known here, and a cross-model hop resolves against the
     * owner model's generation folder rather than this project's.
     *
     * @param raw the declared hops
     * @param parameters the generation parameters
     * @return the resolved hops
     */
    private static List<Object> assigneeHops(Object raw, Map<String, Object> parameters) {
        List<Object> hops = new ArrayList<>();
        for (Map<String, Object> hop : asMaps(raw)) {
            String genFolder = truthy(hop, "crossModel") ? sanitize(hop, "targetModel") : str(parameters, "javaGenFolderName");
            String qualified = "gen." + genFolder + ".data." + sanitize(hop, "perspective") + "." + str(hop, "entity");
            Map<String, Object> resolved = new LinkedHashMap<>();
            resolved.put("local", hop.get("local"));
            resolved.put("nextFkProperty", strOr(hop, "nextFkProperty", ""));
            resolved.put("entityClass", qualified + "Entity");
            resolved.put("repositoryClass", qualified + "Repository");
            hops.add(resolved);
        }
        return hops;
    }

    /**
     * Binds an expiry loader - the delegate that publishes the date a task's boundary timer binds to.
     *
     * @param item the descriptor
     * @param context the template context
     * @param parameters the generation parameters
     */
    private static void bindTimerLoader(Map<String, Object> item, Map<String, Object> context, Map<String, Object> parameters) {
        copy(context, item, "process", "handler", "ownerEntity", "ownerPerspective", "ownerKeyProperty", "ownerKeyAccessor", "variable",
                "dueExpression");
        context.put("javaOwnerPerspective", sanitize(item, "ownerPerspective"));
    }

    /**
     * Binds a wait listener - the handler that correlates a parked catch event with the event that
     * resumes it.
     *
     * @param item the descriptor
     * @param context the template context
     * @param parameters the generation parameters
     */
    private static void bindWait(Map<String, Object> item, Map<String, Object> context, Map<String, Object> parameters) {
        copy(context, item, "process", "className", "messageName", "eventEntity", "eventPerspective", "eventKeyProperty", "topicSuffix",
                "guardExpression", "viaFkProperty", "parentEntity", "parentPerspective");
        context.put("javaEventPerspective", sanitize(item, "eventPerspective"));
        context.put("javaParentPerspective", sanitize(item, "parentPerspective"));
    }

    /**
     * Binds an abort listener - the handler that cancels an instance when the record reaches a matching
     * status.
     *
     * @param item the descriptor
     * @param context the template context
     * @param parameters the generation parameters
     */
    private static void bindAbort(Map<String, Object> item, Map<String, Object> context, Map<String, Object> parameters) {
        copy(context, item, "process", "entity", "perspective", "messageName", "statusMatchExpression");
        context.put("javaPerspective", sanitize(item, "perspective"));
    }

    /**
     * Binds a field setter - the delegate that writes a literal, or a seed relation, onto the record.
     *
     * @param item the descriptor
     * @param context the template context
     * @param parameters the generation parameters
     */
    private static void bindSetter(Map<String, Object> item, Map<String, Object> context, Map<String, Object> parameters) {
        copy(context, item, "process", "className", "entity", "perspective", "keyProperty", "keyAccessor", "field", "value", "relation",
                "errorMessage");
        context.put("javaPerspective", sanitize(item, "perspective"));
    }

    /**
     * Binds a writer - the delegate that writes a reviewer's edits back onto the record after a task.
     *
     * @param item the descriptor
     * @param context the template context
     * @param parameters the generation parameters
     */
    private static void bindWriter(Map<String, Object> item, Map<String, Object> context, Map<String, Object> parameters) {
        copy(context, item, "process", "userTask", "className", "entity", "perspective", "keyProperty", "keyAccessor", "fields");
        context.put("javaPerspective", sanitize(item, "perspective"));
    }

    /**
     * Binds a notification - the listener that mails on an entity event.
     *
     * @param item the descriptor
     * @param context the template context
     * @param parameters the generation parameters
     */
    private static void bindNotification(Map<String, Object> item, Map<String, Object> context, Map<String, Object> parameters) {
        copy(context, item, "name", "className", "entity", "perspective", "topicSuffix", "guardExpression", "toExpression",
                "subjectExpression", "bodyExpression", "attachKeyProperty", "attach", "attachEntity", "attachLanguageExpression",
                "attachLanguageFkProperty", "attachLanguageTargetEntity", "attachFileNameExpression");
        context.put("javaPerspective", sanitize(item, "perspective"));
        context.put("relationLoads", relationLoads(item.get("relationLoads"), parameters));
        bindDeepLinks(item, context);
        bindAttachLanguage(item, context, parameters);
    }

    /**
     * Binds a schedule - the job that queries an entity and either notifies about, or generates a
     * record from, every matching row.
     *
     * @param item the descriptor
     * @param context the template context
     * @param parameters the generation parameters
     */
    private static void bindSchedule(Map<String, Object> item, Map<String, Object> context, Map<String, Object> parameters) {
        boolean generates = "generate".equals(str(item, "action"));
        copy(context, item, "name", "className", "cron", "entity", "perspective", "criteriaExpression", "toExpression", "subjectExpression",
                "bodyExpression", "attachKeyProperty", "attach", "attachEntity", "attachLanguageExpression", "attachLanguageFkProperty",
                "attachLanguageTargetEntity", "attachFileNameExpression", "genToEntity", "genToPk", "genFieldAssignments");
        context.put("javaPerspective", sanitize(item, "perspective"));
        // The source's generation folder is the owner model's when the source is cross-model, else
        // this project's - always supplied, so a local source stays unchanged.
        context.put("sourceGenFolder",
                truthy(item, "sourceCrossModel") ? sanitize(item, "sourceModel") : str(parameters, "javaGenFolderName"));
        context.put("action", strOr(item, "action", "notify"));
        context.put("relationLoads", relationLoads(item.get("relationLoads"), parameters));
        bindDeepLinks(item, context);
        bindAttachLanguage(item, context, parameters);
        context.put("genToGenFolder",
                generates ? (truthy(item, "genCrossModel") ? sanitize(item, "genToModel") : str(parameters, "javaGenFolderName")) : "");
        context.put("genToJavaPerspective", generates ? sanitize(item, "genToPerspective") : "");
        context.put("genChildren", scheduleChildren(item.get("genChildren"), parameters));
    }

    /**
     * Resolves the fully-qualified classes of a schedule's collection-driven children, recursively. The
     * child target lives in the generation target's model, while the iterated collection is local
     * unless the child named another model.
     *
     * @param raw the declared children
     * @param parameters the generation parameters
     * @return the resolved children
     */
    private static List<Object> scheduleChildren(Object raw, Map<String, Object> parameters) {
        List<Object> children = new ArrayList<>();
        for (Map<String, Object> child : asMaps(raw)) {
            Map<String, Object> resolved = ModelValues.copy(child);
            String childGenFolder = truthy(child, "toCrossModel") ? sanitize(child, "toModel") : str(parameters, "javaGenFolderName");
            String childPackage = "gen." + childGenFolder + ".data." + sanitize(child, "toPerspective") + ".";
            String forEachGenFolder =
                    truthy(child, "forEachCrossModel") ? sanitize(child, "forEachModel") : str(parameters, "javaGenFolderName");
            String forEachPackage = "gen." + forEachGenFolder + ".data." + sanitize(child, "forEachPerspective") + ".";
            resolved.put("toEntityClass", childPackage + str(child, "toEntity") + "Entity");
            resolved.put("toRepositoryClass", childPackage + str(child, "toEntity") + "Repository");
            if (truthy(child, "forEachEntity")) {
                resolved.put("forEachEntityClass", forEachPackage + str(child, "forEachEntity") + "Entity");
                resolved.put("forEachRepositoryClass", forEachPackage + str(child, "forEachEntity") + "Repository");
            } else {
                resolved.remove("forEachEntityClass");
                resolved.remove("forEachRepositoryClass");
            }
            resolved.put("children", scheduleChildren(child.get("children"), parameters));
            children.add(resolved);
        }
        return children;
    }

    /**
     * Binds an outbound integration - the listener that forwards an entity event to an external
     * endpoint.
     *
     * @param item the descriptor
     * @param context the template context
     * @param parameters the generation parameters
     */
    private static void bindIntegration(Map<String, Object> item, Map<String, Object> context, Map<String, Object> parameters) {
        copy(context, item, "name", "className", "entity", "perspective", "topicSuffix", "clientMethod", "hasBody", "urlExpression",
                "hasPayload", "payloadFields");
        // The declared payload reads the record and, for a one-hop value, a related record - the same
        // loads a notification performs, so the same package resolution applies.
        context.put("javaPerspective", sanitize(item, "perspective"));
        context.put("relationLoads", relationLoads(item.get("relationLoads"), parameters));
    }

    /**
     * Binds an inbound webhook - the controller that ingests a posted payload as a record.
     *
     * @param item the descriptor
     * @param context the template context
     * @param parameters the generation parameters
     */
    private static void bindInbound(Map<String, Object> item, Map<String, Object> context, Map<String, Object> parameters) {
        copy(context, item, "name", "className", "entity", "perspective", "path");
        context.put("javaPerspective", sanitize(item, "perspective"));
    }

    /**
     * Binds an inbound message ingest - the listener that ingests every record arriving on a queue or a
     * topic.
     *
     * @param item the descriptor
     * @param context the template context
     * @param parameters the generation parameters
     */
    private static void bindInboundMessage(Map<String, Object> item, Map<String, Object> context, Map<String, Object> parameters) {
        copy(context, item, "name", "className", "entity", "perspective", "destination", "listenerKind");
        context.put("javaPerspective", sanitize(item, "perspective"));
    }

    /**
     * Binds an inbound file ingest - the job that polls a drop folder and ingests every file that
     * arrived.
     *
     * @param item the descriptor
     * @param context the template context
     * @param parameters the generation parameters
     */
    private static void bindInboundFile(Map<String, Object> item, Map<String, Object> context, Map<String, Object> parameters) {
        copy(context, item, "name", "className", "entity", "perspective", "folder", "cron");
        context.put("javaPerspective", sanitize(item, "perspective"));
    }

    /**
     * Binds a process-step event emitter - the delegate that publishes the process's record when the
     * execution reaches or completes a step.
     *
     * @param item the descriptor
     * @param context the template context
     * @param parameters the generation parameters
     */
    private static void bindStepEvent(Map<String, Object> item, Map<String, Object> context, Map<String, Object> parameters) {
        copy(context, item, "name", "className", "process", "step", "entity", "perspective", "keyProperty", "keyAccessor", "topicSuffix");
        context.put("javaPerspective", sanitize(item, "perspective"));
    }

    /**
     * Binds a period expansion - the handler that regenerates a master's child rows across a date span.
     * Only the period step is derived here; the type-dependent pieces arrive pre-rendered.
     *
     * @param item the descriptor
     * @param context the template context
     * @param parameters the generation parameters
     */
    private static void bindExpansion(Map<String, Object> item, Map<String, Object> context, Map<String, Object> parameters) {
        copy(context, item, "className", "masterEntity", "masterPerspective", "masterPk", "childEntity", "fkProperty", "startProperty",
                "endProperty", "mapProperty", "unit", "criteriaExpression");
        context.put("javaMasterPerspective", sanitize(item, "masterPerspective"));
        context.put("javaChildPerspective", sanitize(item, "childPerspective"));
        String unit = str(item, "unit");
        context.put("periodStep", "month".equals(unit) ? "d.plusMonths(1)" : ("week".equals(unit) ? "d.plusWeeks(1)" : "d.plusDays(1)"));
        context.put("skipDays", strOr(item, "skipDays", ""));
        context.put("defaultsBlock", strOr(item, "defaultsBlock", ""));
        context.put("spreadTotalProperty", strOr(item, "spreadTotalProperty", ""));
        context.put("spreadIntoProperty", strOr(item, "spreadIntoProperty", ""));
        context.put("spreadRound", strOr(item, "spreadRound", "2"));
        context.put("countProperty", strOr(item, "countProperty", ""));
        context.put("countValue", strOr(item, "countValue", ""));
        context.put("topicSuffix", strOr(item, "topicSuffix", ""));
    }

    /**
     * Binds an auto-settlement - the listener and delegate pair that applies a payment to an invoice
     * through their junction.
     *
     * @param item the descriptor
     * @param context the template context
     * @param parameters the generation parameters
     */
    private static void bindSettlement(Map<String, Object> item, Map<String, Object> context, Map<String, Object> parameters) {
        copy(context, item, "name", "match", "order", "invoiceEntity", "invoicePk", "invoiceTotal", "invoicePaid", "invoiceStatus",
                "payableCondition", "junctionEntity", "junctionFkInvoice", "junctionFkPayment", "junctionAmount", "paymentEntity",
                "paymentPk", "paymentPot", "paymentTopic");
        context.put("invoiceJavaPerspective", sanitize(item, "invoicePerspective"));
        context.put("junctionJavaPerspective", sanitize(item, "junctionPerspective"));
        context.put("paymentGenFolder", truthy(item, "crossModel") ? sanitize(item, "paymentModel") : str(parameters, "javaGenFolderName"));
        context.put("paymentJavaPerspective", sanitize(item, "paymentPerspective"));
    }

    /**
     * Binds a create-from action - the controller that clones a source record into a fresh target.
     *
     * @param item the descriptor
     * @param context the template context
     * @param parameters the generation parameters
     */
    private static void bindGenerate(Map<String, Object> item, Map<String, Object> context, Map<String, Object> parameters) {
        copy(context, item, "name", "className", "fromEntity", "toEntity", "toPk", "fieldAssignments", "hasItems", "hasItemLines",
                "itemLines", "fromItemEntity", "toItemEntity", "srcFkProperty", "toFkProperty", "itemFieldAssignments", "fromPerspective",
                "sourceStatusProperty", "sourceStatusValue",
                // The event half (issue #6711): the trigger kind, the status guard and the back-reference
                // the at-most-once check reads, plus whether a button is contributed at all.
                "fromPk", "eventOnly", "hasEvent", "isCreate", "guardProperty", "guardValue", "backRefProperty",
                // The declared input form (issue #6685): the prompted target properties with their
                // pre-rendered value conversions - the template renders one block per entry.
                "hasPrompt", "promptFields");
        context.put("fromJavaPerspective", sanitize(item, "fromPerspective"));
        // The SOURCE's gen folder / owning project: this project unless the source belongs to another
        // model (intent `fromUses:`). That is what lets a create-from be authored on the module owning
        // the TARGET and keeps the two modules' generated Java acyclic - only one side references the
        // other.
        context.put("fromGenFolder", truthy(item, "crossModelSource") ? sanitize(item, "fromModel") : str(parameters, "javaGenFolderName"));
        context.put("fromProjectName", truthy(item, "crossModelSource") ? str(item, "fromProject") : str(parameters, "projectName"));
        context.put("toGenFolder", truthy(item, "crossModel") ? sanitize(item, "toModel") : str(parameters, "javaGenFolderName"));
        context.put("toJavaPerspective", sanitize(item, "toPerspective"));
        // A primary source item that is not a composition child lives outside the source document's
        // perspective, which the intent layer resolves; for the common case it is the same one.
        context.put("fromItemJavaPerspective",
                NamingHelper.sanitizeJavaIdentifier(strOr(item, "fromItemPerspective", str(item, "fromPerspective"))));
    }

    /**
     * Binds an on-demand transition - the controller that validates the guards and flips only the
     * status column, optionally notifying afterwards.
     *
     * @param item the descriptor
     * @param context the template context
     * @param parameters the generation parameters
     */
    private static void bindTransition(Map<String, Object> item, Map<String, Object> context, Map<String, Object> parameters) {
        copy(context, item, "name", "className", "entity", "perspective", "attachKeyProperty", "statusProperty", "setStatus", "allowedExpr",
                "fromStatuses", "guardExpr", "guardText", "notify", "forEach", "forEachFkProperty", "forEachKeyProperty",
                "notifyToExpression", "notifySubjectExpression", "notifyBodyExpression", "notifyRecordScoped", "attach", "attachEntity",
                "attachLanguageExpression", "attachLanguageFkProperty", "attachLanguageTargetEntity", "attachFileNameExpression");
        context.put("javaPerspective", sanitize(item, "perspective"));
        context.put("notifyRelationLoads", relationLoads(item.get("notifyRelationLoads"), parameters));
        context.put("javaForEachPerspective", NamingHelper.sanitizeJavaIdentifier(strOr(item, "forEachPerspective", "")));
        bindDeepLinks(item, context);
        bindAttachLanguage(item, context, parameters);
    }

    /**
     * Binds a sending step - the delegate that re-loads the process's record and mails it.
     *
     * @param item the descriptor
     * @param context the template context
     * @param parameters the generation parameters
     */
    private static void bindSend(Map<String, Object> item, Map<String, Object> context, Map<String, Object> parameters) {
        copy(context, item, "process", "step", "className", "entity", "perspective", "keyProperty", "keyAccessor", "forEach",
                "forEachFkProperty", "forEachKeyProperty", "notifyToExpression", "notifySubjectExpression", "notifyBodyExpression",
                "notifyRecordScoped", "attachKeyProperty", "attach", "attachEntity", "attachLanguageExpression", "attachLanguageFkProperty",
                "attachLanguageTargetEntity", "attachFileNameExpression");
        context.put("javaPerspective", sanitize(item, "perspective"));
        context.put("notifyRelationLoads", relationLoads(item.get("notifyRelationLoads"), parameters));
        context.put("javaForEachPerspective", NamingHelper.sanitizeJavaIdentifier(strOr(item, "forEachPerspective", "")));
        bindDeepLinks(item, context);
        bindAttachLanguage(item, context, parameters);
    }

    /**
     * Binds an event-driven posting rule - the handler that emits mapped rows into a target, keyed by
     * the back reference so it stays idempotent.
     *
     * @param item the descriptor
     * @param context the template context
     * @param parameters the generation parameters
     */
    private static void bindPost(Map<String, Object> item, Map<String, Object> context, Map<String, Object> parameters) {
        copy(context, item, "name", "className", "sourcePerspective", "sourceKeyField", "isCreate", "event", "statusProperty",
                "statusValue", "perItem", "itemsEntity", "itemsFk", "itemsPerspective", "into", "targetPerspective", "targetPk", "backRef",
                "assigns");
        context.put("sourceEntity", item.get("entity"));
        context.put("sourceJavaPerspective", sanitize(item, "sourcePerspective"));
        context.put("itemsJavaPerspective", sanitize(item, "itemsPerspective"));
        context.put("targetJavaPerspective", sanitize(item, "targetPerspective"));
    }

    /**
     * Binds a declarative posting - the handler that creates a document and its computed items from a
     * source record's transition.
     *
     * @param item the descriptor
     * @param context the template context
     * @param parameters the generation parameters
     */
    private static void bindPosting(Map<String, Object> item, Map<String, Object> context, Map<String, Object> parameters) {
        copy(context, item, "name", "className", "isCreate", "sourcePerspective", "sourceEntity", "sourceKeyField", "guardProperty",
                "guardValue", "targetEntity", "targetPk", "itemsEntity", "itemsFk", "backRefProperty", "stornoProperty",
                "stornoFilterProperty", "hasRule", "ruleEntity", "ruleMatchProperty", "ruleMatchValueJava", "usedRuleColumns",
                "conditionalRuleGuards", "headerAssignments", "itemRows");
        boolean crossModel = truthy(item, "crossModel");
        context.put("sourceTopicProject", crossModel ? item.get("sourceProject") : parameters.get("projectName"));
        context.put("sourceJavaGenFolder", crossModel ? sanitize(item, "sourceGenFolder") : str(parameters, "javaGenFolderName"));
        context.put("sourceJavaPerspective", sanitize(item, "sourcePerspective"));
        context.put("targetJavaPerspective", sanitize(item, "targetPerspective"));
        context.put("itemsJavaPerspective", sanitize(item, "itemsPerspective"));
        context.put("ruleJavaPerspective", truthy(item, "rulePerspective") ? sanitize(item, "rulePerspective") : "");
    }

    /**
     * Binds a print feeder - the controller that assembles the nested payload a print template binds. A
     * cross-model node's fields are deliberately not enumerated: the template copies the loaded record
     * reflectively, so this project's generated output survives the owner retiring a field.
     *
     * @param item the descriptor
     * @param context the template context
     * @param parameters the generation parameters
     */
    private static void bindPrintFeeder(Map<String, Object> item, Map<String, Object> context, Map<String, Object> parameters) {
        copy(context, item, "className", "entity", "rootScalars", "itemsEntity", "itemsFkProperty", "itemScalars");
        context.put("javaPerspective", sanitize(item, "perspective"));
        context.put("itemsJavaPerspective", sanitize(item, "itemsPerspective"));
        context.put("hasCrossModel", Boolean.TRUE.equals(item.get("hasCrossModel")));
        context.put("itemNodes", feederNodes(item.get("itemNodes"), parameters, false));
        context.put("nodes", feederNodes(item.get("nodes"), parameters, true));
    }

    /**
     * Resolves the relation nodes of a print feeder.
     *
     * @param raw the declared nodes
     * @param parameters the generation parameters
     * @param withParent whether the nodes carry a parent reference
     * @return the resolved nodes
     */
    private static List<Object> feederNodes(Object raw, Map<String, Object> parameters, boolean withParent) {
        List<Object> nodes = new ArrayList<>();
        for (Map<String, Object> node : asMaps(raw)) {
            boolean crossModel = Boolean.TRUE.equals(node.get("crossModel"));
            Map<String, Object> resolved = new LinkedHashMap<>();
            resolved.put("entityVar", node.get("entityVar"));
            resolved.put("mapVar", node.get("mapVar"));
            if (withParent) {
                resolved.put("parentEntityVar", node.get("parentEntityVar"));
                resolved.put("parentMapVar", node.get("parentMapVar"));
            }
            resolved.put("fkProperty", node.get("fkProperty"));
            resolved.put("keyInParent", node.get("keyInParent"));
            resolved.put("entity", node.get("entity"));
            resolved.put("crossModel", crossModel);
            resolved.put("model", node.get("model"));
            resolved.put("genFolder", crossModel ? sanitize(node, "model") : str(parameters, "javaGenFolderName"));
            resolved.put("javaPerspective", sanitize(node, "perspective"));
            resolved.put("labelField", node.get("labelField"));
            resolved.put("scalars", node.get("scalars"));
            nodes.add(resolved);
        }
        return nodes;
    }

    /**
     * Binds a snapshot generator - the delegate that renders a document through its print feeder and
     * stores the result as an attachment.
     *
     * @param item the descriptor
     * @param context the template context
     * @param parameters the generation parameters
     */
    private static void bindSnapshot(Map<String, Object> item, Map<String, Object> context, Map<String, Object> parameters) {
        copy(context, item, "master", "masterPk", "languageExpression", "languageFkProperty", "languageTargetEntity", "snapshotEntity",
                "snapshotMasterFk");
        context.put("masterJavaPerspective", NamingHelper.sanitizeJavaIdentifier(strOr(item, "masterPerspective", "")));
        context.put("languageTargetJavaPerspective", NamingHelper.sanitizeJavaIdentifier(strOr(item, "languageTargetPerspective", "")));
        context.put("languageTargetJavaGenFolder",
                truthy(item, "languageTargetModel") ? sanitize(item, "languageTargetModel") : str(parameters, "javaGenFolderName"));
        context.put("snapshotJavaPerspective", sanitize(item, "snapshotPerspective"));
    }

    /**
     * Binds a numbering stamp - the delegate that allocates the real document number at the issue step.
     *
     * @param item the descriptor
     * @param context the template context
     * @param parameters the generation parameters
     */
    private static void bindNumbering(Map<String, Object> item, Map<String, Object> context, Map<String, Object> parameters) {
        copy(context, item, "entity", "masterPk", "field", "series", "per");
        context.put("javaPerspective", sanitize(item, "perspective"));
    }

    /**
     * Binds an effective-dated register lookup - the listener that fills a to-one from the register row
     * valid on the record's date.
     *
     * @param item the descriptor
     * @param context the template context
     * @param parameters the generation parameters
     */
    private static void bindResolve(Map<String, Object> item, Map<String, Object> context, Map<String, Object> parameters) {
        copy(context, item, "name", "className", "entity", "perspective", "keyProperty", "topicSuffix", "guardExpression", "setProperty",
                "registerEntity", "registerPerspective", "registerValueProperty", "matches", "matchSummary", "startProperty", "endProperty",
                "valueProperty", "outcomeProperty", "statusProperty", "foundStatus", "notFoundStatus", "ambiguousStatus", "writesStatus");
        context.put("javaPerspective", sanitize(item, "perspective"));
        context.put("javaRegisterPerspective", sanitize(item, "registerPerspective"));
    }

    /**
     * Generates the roll-up handlers, coalescing every roll-up that shares a child entity, a parent
     * relation and an event into ONE handler.
     *
     * <p>
     * The coalescing is what keeps the totals correct: separate handlers would each persist the whole
     * parent row and clobber each other's fields. Grouping by the event as well as the relation is what
     * makes each event's aggregate set right - a count roll-up contributes no update entry, so the
     * update handler ends up sum-only.
     *
     * @param source the template source
     * @param content the template content
     * @param model the model
     * @param parameters the generation parameters
     * @return the generated files
     * @throws IOException when rendering fails
     */
    private List<GeneratedFile> rollups(GenerationTemplateMetadataSource source, String content, Map<String, Object> model,
            Map<String, Object> parameters) throws IOException {
        Map<String, List<Map<String, Object>>> groups = new LinkedHashMap<>();
        for (Map<String, Object> rollup : asMaps(model.get("rollups"))) {
            String key = str(rollup, "childEntity") + "|" + str(rollup, "fkProperty") + "|" + strOr(rollup, "topicSuffix", "");
            groups.computeIfAbsent(key, ignored -> new ArrayList<>())
                  .add(rollup);
        }
        List<GeneratedFile> files = new ArrayList<>();
        for (List<Map<String, Object>> group : groups.values()) {
            Map<String, Object> first = group.get(0);
            StringBuilder aggregateBlock = new StringBuilder();
            for (Map<String, Object> rollup : group) {
                aggregateBlock.append(RollupAggregates.render(rollup));
            }
            Map<String, Object> context = ModelValues.copy(parameters);
            copy(context, first, "className", "childEntity", "childPerspective", "parentEntity", "fkProperty", "criteriaExpression");
            context.put("javaChildPerspective", sanitize(first, "childPerspective"));
            context.put("javaParentPerspective", sanitize(first, "parentPerspective"));
            // A cross-model roll-up writes into the owner model's generated package.
            context.put("parentGenFolder",
                    truthy(first, "parentCrossModel") ? sanitize(first, "parentModel") : str(parameters, "javaGenFolderName"));
            context.put("topicSuffix", strOr(first, "topicSuffix", ""));
            context.put("aggregateBlock", aggregateBlock.toString());
            files.add(render(source, content, ModelValues.cleaned(context)));
        }
        return files;
    }

    /**
     * Generates the keyed aggregate handlers. Each declared aggregate expands into four handlers - one
     * per source event - because every one upserts the incoming row's key tuple and recomputes the
     * total from the store, which makes them idempotent and self-healing.
     *
     * <p>
     * The fourth, re-keyed, variant is the same handler fed the previous row: the repository publishes
     * that event only when a grouping key actually moved, and recomputing the former tuple - which no
     * longer contains the row - drops the total it left behind. Without it, editing a grouping key
     * leaves a stale aggregate on the tuple the row moved out of.
     *
     * @param source the template source
     * @param content the template content
     * @param model the model
     * @param parameters the generation parameters
     * @return the generated files
     * @throws IOException when rendering fails
     */
    private List<GeneratedFile> aggregates(GenerationTemplateMetadataSource source, String content, Map<String, Object> model,
            Map<String, Object> parameters) throws IOException {
        List<GeneratedFile> files = new ArrayList<>();
        for (Map<String, Object> aggregate : asMaps(model.get("aggregates"))) {
            List<Map<String, Object>> keys = asMaps(aggregate.get("keys"));
            StringBuilder keyNullGuards = new StringBuilder();
            StringBuilder keyCriteria = new StringBuilder("Criteria.create()");
            StringBuilder keyAssigns = new StringBuilder();
            List<String> keyNames = new ArrayList<>(keys.size());
            for (Map<String, Object> keyEntry : keys) {
                String key = str(keyEntry, "key");
                keyNames.add(key);
                keyNullGuards.append(" || row.")
                             .append(key)
                             .append(" == null");
                keyCriteria.append(".eq(\"")
                           .append(key)
                           .append("\", row.")
                           .append(key)
                           .append(")");
                keyAssigns.append("        target.")
                          .append(key)
                          .append(" = row.")
                          .append(key)
                          .append(";\n");
            }
            String sumField = str(aggregate, "sumField");
            String aggregateStep = "count".equals(str(aggregate, "op")) ? "agg = agg.add(java.math.BigDecimal.ONE);"
                    : "if (r." + sumField + " != null) { agg = agg.add(r." + sumField + "); }";
            for (AggregateVariant variant : AggregateVariant.values()) {
                Map<String, Object> context = ModelValues.copy(parameters);
                copy(context, aggregate, "op", "sourceEntity", "sourcePerspective", "targetEntity", "targetPerspective", "targetField",
                        "targetPk", "sumField");
                context.put("className", str(aggregate, "className") + "Aggregate" + variant.classSuffix());
                context.put("topicSuffix", variant.topicSuffix());
                context.put("sourceJavaPerspective", sanitize(aggregate, "sourcePerspective"));
                context.put("targetJavaPerspective", sanitize(aggregate, "targetPerspective"));
                context.put("keyList", String.join(", ", keyNames));
                context.put("keyNullGuards", keyNullGuards.toString());
                context.put("keyCriteria", keyCriteria.toString());
                context.put("keyAssigns", keyAssigns.toString());
                context.put("aggregateStep", aggregateStep);
                files.add(render(source, content, ModelValues.cleaned(context)));
            }
        }
        return files;
    }

    /**
     * Binds the coordinates a print attachment's render language is resolved through.
     *
     * @param item the descriptor
     * @param context the template context
     * @param parameters the generation parameters
     */
    private static void bindAttachLanguage(Map<String, Object> item, Map<String, Object> context, Map<String, Object> parameters) {
        context.put("attachLanguageJavaTargetPerspective",
                NamingHelper.sanitizeJavaIdentifier(strOr(item, "attachLanguageTargetPerspective", "")));
        context.put("attachLanguageJavaGenFolder", truthy(item, "attachLanguageCrossModel") ? sanitize(item, "attachLanguageTargetModel")
                : str(parameters, "javaGenFolderName"));
    }

    /**
     * Binds the deep-link keys every notify call site carries: which of the two reserved link locals
     * ({@code recordUrl} / {@code inboxUrl}) the message references, plus the entity and key property
     * the record link is built from. The route itself is assembled in the template, which is the layer
     * that knows the generated application's URL layout.
     *
     * @param item the descriptor
     * @param context the template context
     */
    private static void bindDeepLinks(Map<String, Object> item, Map<String, Object> context) {
        copy(context, item, "usesRecordUrl", "usesInboxUrl", "recordUrlEntity", "recordUrlKeyProperty");
    }

    /**
     * Resolves the Java package of each one-hop relation load. A cross-model load imports from the
     * owner model's generation folder; a same-model one from this project's.
     *
     * @param raw the declared loads
     * @param parameters the generation parameters
     * @return the resolved loads
     */
    private static List<Object> relationLoads(Object raw, Map<String, Object> parameters) {
        List<Object> loads = new ArrayList<>();
        for (Map<String, Object> load : asMaps(raw)) {
            Map<String, Object> resolved = ModelValues.copy(load);
            resolved.put("javaTargetPerspective", sanitize(load, "targetPerspective"));
            resolved.put("javaGenFolder",
                    truthy(load, "crossModel") ? sanitize(load, "targetModel") : str(parameters, "javaGenFolderName"));
            loads.add(resolved);
        }
        return loads;
    }

    /**
     * Copies keys from a descriptor onto a template context. A key the descriptor does not carry is
     * <em>removed</em> from the context rather than set to null, because the context starts as a copy
     * of the generation parameters and may already hold a same-named value.
     *
     * @param target the template context
     * @param source the descriptor
     * @param keys the keys to copy
     */
    private static void copy(Map<String, Object> target, Map<String, Object> source, String... keys) {
        for (String key : keys) {
            if (source.containsKey(key)) {
                target.put(key, source.get(key));
            } else {
                target.remove(key);
            }
        }
    }

    /**
     * Sanitizes a descriptor's value into a Java identifier.
     *
     * @param source the descriptor
     * @param key the key
     * @return the sanitized identifier
     */
    private static String sanitize(Map<String, Object> source, String key) {
        return NamingHelper.sanitizeJavaIdentifier(str(source, key));
    }

    /**
     * The four handlers one declared aggregate expands into.
     */
    private enum AggregateVariant {

        /** Maintains the total when a source row is created. */
        ON_CREATE("", "OnCreate"),

        /** Maintains the total when a source row changes. */
        ON_UPDATE("-updated", "OnUpdate"),

        /** Maintains the total when a source row is deleted. */
        ON_DELETE("-deleted", "OnDelete"),

        /** Drops the total a source row left behind when its grouping key moved. */
        ON_REKEY("-rekeyed", "OnRekey");

        /** The event topic suffix. */
        private final String topicSuffix;

        /** The suffix of the generated handler's class name. */
        private final String classSuffix;

        /**
         * Instantiates a new aggregate variant.
         *
         * @param topicSuffix the event topic suffix
         * @param classSuffix the class name suffix
         */
        AggregateVariant(String topicSuffix, String classSuffix) {
            this.topicSuffix = topicSuffix;
            this.classSuffix = classSuffix;
        }

        /**
         * Gets the event topic suffix.
         *
         * @return the topic suffix
         */
        String topicSuffix() {
            return topicSuffix;
        }

        /**
         * Gets the class name suffix.
         *
         * @return the class suffix
         */
        String classSuffix() {
            return classSuffix;
        }
    }

    /**
     * Binds one descriptor onto a template context.
     */
    @FunctionalInterface
    private interface Binder {

        /**
         * Binds a descriptor.
         *
         * @param item the descriptor
         * @param context the template context
         * @param parameters the generation parameters
         */
        void bind(Map<String, Object> item, Map<String, Object> context, Map<String, Object> parameters);
    }

}
