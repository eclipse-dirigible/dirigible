/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.parser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.dirigible.components.intent.generator.ProcessWaitSupport;
import org.eclipse.dirigible.components.intent.generator.StepEventSupport;

/**
 * Resolves a status referenced by its <b>seeded name</b> to the seed id, everywhere the intent
 * names a status, on the raw YAML tree - before the typed Gson mapping, so every downstream
 * validator, generator and template keeps seeing the plain integers it always saw.
 *
 * <pre>
 * transitions:
 *   - { name: VoidSalesInvoice, forEntity: SalesInvoice, from: [ISSUED, SENT], setStatus: VOIDED }
 * reports:
 *   - { name: OverdueInvoices, source: SalesInvoice, filter: "balance &gt; 0 AND Status != VOIDED" }
 * </pre>
 *
 * Why this exists: a status id is <b>positional</b>. Inserting a status into the middle of a
 * nomenclature shifts every later id, and every guard, filter and transition authored against the
 * old numbering keeps generating well-formed output that now means something else - the failure
 * mode that left a voided invoice on the ledger with no reversing entry (dirigible #6645). A name
 * cannot be silently retargeted, and a mistyped name is a generation error rather than a wrong
 * number.
 *
 * <p>
 * The same holds for any other seeded nomenclature a record points at (a request's {@code type}): a
 * decision {@code if}, a process {@code trigger.when}, a {@code where} condition (#8015), a check's
 * {@code when} and a field's or a composition child's {@code visibleWhen} (#8057) resolve a name
 * compared with {@code ==}/{@code !=} against ANY to-one relation, on that relation's target.
 *
 * <p>
 * Scope: the nomenclature must be seeded <b>in this model</b>. A cross-model status entity is
 * seeded in its owner model, which the parser cannot read (it holds one file, with no repository),
 * so a symbol against it is refused with a message naming the numeric-id fallback instead of being
 * quietly left in place.
 */
final class StatusSymbolResolver {

    /** A comparison inside a guard / filter expression: term, operator, symbolic right-hand side. */
    // Both identifier runs are POSSESSIVE (`*+`): a shorter one would end inside an identifier, where
    // the trailing \b cannot hold, so backtracking into them can never produce a match - it only costs
    // a scan per start position, which is quadratic on a long run of identifier characters and is what
    // CodeQL flags as a polynomial ReDoS now that an authored decision condition reaches here (#7648).
    private static final Pattern COMPARISON =
            Pattern.compile("(\\b[A-Za-z_][A-Za-z0-9_]*+\\b)\\s*(==|!=|<>|<=|>=|=|<|>)\\s*([A-Za-z_][A-Za-z0-9_]*+)\\b");

    /**
     * A {@code forbidWhen} / {@code requiredWhen} one-hop comparison
     * {@code <Relation>.<field> ==|!= <NAME>} - the whole term, so the name on the right resolves
     * against the RELATION TARGET's nomenclature rather than the record's own.
     */
    private static final Pattern RELATION_COMPARISON = Pattern.compile("\\s*(\\w+)\\.(\\w+)\\s*(==|!=)\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*");

    /** The operators for which a status NAME is meaningful - a name has no ordering. */
    private static final Set<String> EQUALITY = Set.of("==", "!=", "<>", "=");

    private static final Pattern INTEGER = Pattern.compile("-?\\d+");

    /** The declarative glue lists carrying an {@code event.when} guard, to the subject naming one. */
    private static final List<Map.Entry<String, String>> GLUE_LISTS = List.of(Map.entry("notifications", "notification"),
            Map.entry("integrations", "integration"), Map.entry("outbound", "outbound"));

    /** Entity name to its raw node. */
    private final Map<String, Map<?, ?>> entities = new LinkedHashMap<>();

    /** Seeded entity name to its rows' {@code name} to {@code id} mapping, in seed order. */
    private final Map<String, Map<String, Integer>> seededIds = new LinkedHashMap<>();

    private final List<String> issues = new ArrayList<>();

    private StatusSymbolResolver() {}

    /**
     * Rewrite every symbolic status reference in the tree to its seed id, in place.
     *
     * @param tree the SnakeYAML-loaded raw tree
     * @throws IntentValidationException naming every symbol that did not resolve
     */
    static void resolve(Object tree) {
        Map<?, ?> root = asMap(tree);
        if (root == null) {
            return;
        }
        StatusSymbolResolver resolver = new StatusSymbolResolver();
        resolver.index(root);
        resolver.rewriteEntities(root);
        resolver.rewriteTransitions(root);
        resolver.rewriteActions(root);
        resolver.rewriteProcesses(root);
        resolver.rewritePostings(root);
        resolver.rewriteGenerates(root);
        resolver.rewriteReversals(root);
        resolver.rewriteSchedules(root);
        resolver.rewriteRollups(root);
        resolver.rewriteGlue(root);
        resolver.rewriteResolves(root);
        resolver.rewriteReports(root);
        if (!resolver.issues.isEmpty()) {
            throw new IntentValidationException(resolver.issues);
        }
    }

    // ----- indexing ---------------------------------------------------------------

    private void index(Map<?, ?> root) {
        for (Object node : asList(root.get("entities"))) {
            Map<?, ?> entity = asMap(node);
            String name = text(entity, "name");
            if (name != null) {
                entities.put(name, entity);
            }
        }
        for (Object node : asList(root.get("seeds"))) {
            Map<?, ?> seed = asMap(node);
            String entityName = text(seed, "entity");
            if (entityName == null || text(seed, "language") != null) {
                continue; // a translation seed carries no base rows
            }
            String idField = idFieldOf(entities.get(entityName));
            Map<String, Integer> ids = seededIds.computeIfAbsent(entityName, key -> new LinkedHashMap<>());
            for (Object rowNode : asList(seed.get("rows"))) {
                Map<?, ?> row = asMap(rowNode);
                String rowName = text(row, "name");
                Integer id = integerOf(row == null ? null : row.get(idField));
                if (rowName != null && id != null) {
                    ids.putIfAbsent(rowName, id);
                }
            }
        }
    }

    /** The field name the entity's seed rows key its primary key by ({@code id} by convention). */
    private static String idFieldOf(Map<?, ?> entity) {
        for (Object node : asList(entity == null ? null : entity.get("fields"))) {
            Map<?, ?> field = asMap(node);
            if (field != null && Boolean.TRUE.equals(field.get("primaryKey")) && text(field, "name") != null) {
                return text(field, "name");
            }
        }
        return "id";
    }

    // ----- sites ------------------------------------------------------------------

    private void rewriteEntities(Map<?, ?> root) {
        for (Object node : asList(root.get("entities"))) {
            Map<?, ?> entity = asMap(node);
            if (entity == null) {
                continue;
            }
            String entityName = text(entity, "name");
            // A to-one relation's `init:` is the FK's DB-level default - the id of a seed row of ITS
            // OWN target, which need not be a status (a payment method, a document kind).
            for (Object relationNode : asList(entity.get("relations"))) {
                Map<?, ?> relation = asMap(relationNode);
                if (relation == null || relation.get("init") == null) {
                    continue;
                }
                Target target = new Target(text(relation, "to"), text(relation, "model"));
                Integer id = resolveScalar(relation.get("init"), target,
                        "entity [" + entityName + "] relation [" + text(relation, "name") + "] init");
                if (id != null) {
                    put(relation, "init", String.valueOf(id));
                }
            }
            Target status = statusOf(entityName);
            String statusRelation = statusRelationName(entityName);
            // The declarative state machine: every node of the graph names a status.
            for (Object edgeNode : asList(asMap(entity.get("lifecycle")) == null ? null : asMap(entity.get("lifecycle")).get("edges"))) {
                Map<?, ?> edge = asMap(edgeNode);
                if (edge == null) {
                    continue;
                }
                String subject = "entity [" + entityName + "] lifecycle edge [" + text(edge, "from") + "]";
                putResolved(edge, "from", status, subject + " from");
                putResolvedList(edge, "to", status, subject + " to");
            }
            // refresh.while (#8375): the statuses the entity's pages poll in. Only the record's own,
            // same-model nomenclature resolves here; the parser refuses refresh: without one, in its
            // own words rather than a status-name message.
            Map<?, ?> refresh = asMap(entity.get("refresh"));
            if (refresh != null && status.entity() != null && status.model() == null) {
                putResolvedList(refresh, "while", status, "entity [" + entityName + "] refresh while");
            }
            if (entity.get("immutableWhen") != null) {
                String rewritten = rewriteExpression(text(entity, "immutableWhen"), statusRelation, status,
                        "entity [" + entityName + "] immutableWhen");
                put(entity, "immutableWhen", rewritten);
            }
            // A period register's closedWhen keys on the REGISTER's own status - and the register is
            // this entity, so the same target resolves it.
            Map<?, ?> period = asMap(entity.get("period"));
            if (period != null && period.get("closedWhen") != null) {
                String rewritten = rewriteExpression(text(period, "closedWhen"), statusRelation, status,
                        "entity [" + entityName + "] period closedWhen");
                put(period, "closedWhen", rewritten);
            }
            for (Object checkNode : asList(entity.get("checks"))) {
                Map<?, ?> check = asMap(checkNode);
                if (check == null) {
                    continue;
                }
                String subject = "entity [" + entityName + "] check [" + text(check, "kind") + "]";
                putResolved(check, "status", status, subject + " status");
                putResolved(check, "setStatus", status, subject + " setStatus");
                // A requiredWhen / forbidWhen condition may be about the status itself ("required once
                // ISSUED"), so it resolves like every other guard - the terms about other properties pass
                // through. Either may also read a status one hop away (`SalesInvoice.Status == PAID`,
                // #7275; `Customer.Status == ACTIVE`, #7495), which resolves against the RELATION
                // TARGET's nomenclature, not the record's - so both are routed to the hop-aware resolver,
                // which falls back to the record-local rewrite for their record-own terms. The plain
                // rewrite would match the `Status == PAID` tail of the path and resolve it against the
                // record's own nomenclature: a wrong id, silently. A record-local term may also name any
                // other to-one relation (`type == CLOSE`, #8057), which resolves against that relation's
                // target, as a trigger guard's does.
                String kind = lower(text(check, "kind"));
                if ("forbidwhen".equals(kind) || "requiredwhen".equals(kind)) {
                    rewriteForbidWhen(entityName, check, subject + " when");
                } else {
                    rewriteRecordWhen(check, "when", entityName, subject + " when");
                }
            }
            // visibleWhen (#7502). On a field it is about the record's own status ("show the paid date
            // once PAID") or another of its to-one relations ("show the closing note for a CLOSE request",
            // #8057), so it resolves like every other record-scoped guard. On the entity it gates the
            // child's panel on its MASTER's page and is written in the master's terms, so its names resolve
            // against the composition master's nomenclatures - resolved against the child's own, a
            // `Status != DRAFT` would name a status of an entity that usually has none.
            for (Object fieldNode : asList(entity.get("fields"))) {
                Map<?, ?> field = asMap(fieldNode);
                if (field != null && field.get("visibleWhen") != null) {
                    rewriteRecordWhen(field, "visibleWhen", entityName,
                            "entity [" + entityName + "] field [" + text(field, "name") + "] visibleWhen");
                }
                // A status stamp (#7874) names the status whose arrival the field records - always one of
                // the record's own, as for the lifecycle graph.
                Map<?, ?> stampOn = field == null ? null : asMap(field.get("stampOn"));
                if (stampOn != null) {
                    putResolved(stampOn, "status", status,
                            "entity [" + entityName + "] field [" + text(field, "name") + "] stampOn status");
                }
            }
            if (entity.get("visibleWhen") != null) {
                String master = compositionMasterOf(entity);
                if (master != null) {
                    rewriteRecordWhen(entity, "visibleWhen", master, "entity [" + entityName + "] visibleWhen");
                }
            }
        }
    }

    private void rewriteTransitions(Map<?, ?> root) {
        for (Object node : asList(root.get("transitions"))) {
            Map<?, ?> transition = asMap(node);
            if (transition == null) {
                continue;
            }
            String subject = "transition [" + text(transition, "name") + "]";
            Target status = statusOf(text(transition, "forEntity"));
            putResolvedList(transition, "from", status, subject + " from");
            putResolved(transition, "setStatus", status, subject + " setStatus");
        }
    }

    /**
     * The statuses a {@code call:} action's button is offered in (issue #8090), on its entity's own
     * nomenclature.
     */
    private void rewriteActions(Map<?, ?> root) {
        for (Object node : asList(root.get("actions"))) {
            Map<?, ?> action = asMap(node);
            if (action != null) {
                putResolvedList(action, "fromStatus", statusOf(text(action, "forEntity")),
                        "action [" + text(action, "name") + "] fromStatus");
            }
        }
    }

    private void rewriteProcesses(Map<?, ?> root) {
        for (Object node : asList(root.get("processes"))) {
            Map<?, ?> process = asMap(node);
            if (process == null) {
                continue;
            }
            String subject = "process [" + text(process, "name") + "]";
            String triggerEntity = triggerEntityOf(process);
            Map<?, ?> abortOn = asMap(process.get("abortOn"));
            if (abortOn != null) {
                putResolvedList(abortOn, "status", statusOf(triggerEntity), subject + " abortOn status");
            }
            // The trigger's own `when` guard. The status axis makes a status name the norm here rather
            // than the exception - "start this process when the record reaches IDENTIFIED" is what an
            // onTransition trigger is FOR - and this was the one `when` in the DSL left unresolved: the
            // name survived into the generated listener as a string compared against the integer FK, so
            // the guard could never hold and the process silently never started (#6862). The same
            // failure hid behind any other to-one relation - "start this flow for a CLOSE request" names
            // a seed of the request-type nomenclature - so the guard then resolves each to-one relation
            // against ITS target, as a decision does (#8015).
            Map<?, ?> trigger = asMap(process.get("trigger"));
            if (trigger != null && trigger.get("when") != null) {
                rewriteTriggerWhen(trigger, triggerEntity, subject + " trigger when");
            }
            for (Object stepNode : asList(process.get("steps"))) {
                Map<?, ?> step = asMap(stepNode);
                Map<?, ?> args = asMap(step == null ? null : step.get("args"));
                if (args == null) {
                    continue;
                }
                String stepSubject = subject + " step [" + text(step, "name") + "]";
                // A `wait` step's guard qualifies the event that RESUMES the parked instance, and it is
                // read against the event entity's own record - not necessarily the trigger entity's
                // (`via:` is exactly the case where the two differ), so the nomenclature is the event
                // entity's. Left unresolved, the name reached the generated listener as a string
                // compared against the integer status FK: never true, and because a wait that matches
                // nothing is a deliberate no-op, the instance simply stayed parked with nothing logged
                // (#6907).
                if ("wait".equals(lower(text(step, "kind"))) && args.get("when") != null) {
                    String eventEntity = waitEventEntityOf(args);
                    rewriteWhen(args, statusRelationName(eventEntity), statusOf(eventEntity), stepSubject + " when");
                }
                // A decision branching on a to-one relation of the trigger entity (#7648): the FK is
                // loaded before the gateway, so the condition compares an id - and a seeded NAME is how
                // an author writes it, exactly as at every other status site. The nomenclature is the
                // one the NAMED relation points at, not the record's own status: a decision may branch
                // on any to-one (a send method, a channel), and resolving against the entity's status
                // would take an id out of the wrong nomenclature.
                if ("decision".equals(lower(text(step, "kind"))) && args.get("if") instanceof String condition) {
                    put(args, "if", rewriteRelationComparisons(condition, triggerEntity, null, this::toOneRelation, stepSubject + " if"));
                }
                // `setRelationField: <Relation>` + `value:` writes an id of THAT relation's target - the
                // status in the canonical case, but the same shape serves any nomenclature FK.
                String relationName = text(args, "setRelationField");
                if (relationName != null && args.get("value") != null) {
                    resolveSetterValue(args, "value", relationName, triggerEntity, false, stepSubject + " setRelationField");
                }
                // A set: map (#8042) writes several members at once: each key naming a to-one relation is
                // resolved exactly as a setRelationField's value; a field key passes through untouched.
                Map<?, ?> set = asMap(args.get("set"));
                if (set != null) {
                    for (Object key : new ArrayList<>(set.keySet())) {
                        if (key instanceof String path && set.get(path) != null) {
                            resolveSetterValue(set, path, path, triggerEntity, true, stepSubject + " set");
                        }
                    }
                }
            }
        }
    }

    /**
     * Resolve the seed name a setter assigns to a to-one relation, in place, against that relation's
     * target - on the trigger record, or one hop away (#7934): {@code order.Status} writes the status
     * of the record {@code order} points at, so its name is a seed of the ORDER's nomenclature. A path
     * that does not reach a local to-one target is the parser's to refuse, with a message about the
     * path.
     *
     * @param mayNameAField whether the path may name a field instead - a {@code set:} key, whose
     *        literal is then left alone
     */
    private void resolveSetterValue(Map<?, ?> owner, String key, String path, String triggerEntity, boolean mayNameAField, String subject) {
        String writtenEntity = triggerEntity;
        String relationName = path;
        int dot = path.indexOf('.');
        if (dot >= 0) {
            Map<?, ?> via = toOneRelation(triggerEntity, path.substring(0, dot));
            if (via == null || text(via, "model") != null || !isToOne(via) || !entities.containsKey(text(via, "to"))) {
                return;
            }
            writtenEntity = text(via, "to");
            relationName = path.substring(dot + 1);
        }
        Map<?, ?> relation = toOneRelation(writtenEntity, relationName);
        if (relation == null && mayNameAField) {
            return; // a field carries a literal, not a seed
        }
        Target target = relation == null ? new Target(null, null) : new Target(text(relation, "to"), text(relation, "model"));
        putResolved(owner, key, target, subject + " [" + relationName + "] value");
    }

    private void rewritePostings(Map<?, ?> root) {
        for (Object node : asList(root.get("postings"))) {
            Map<?, ?> posting = asMap(node);
            Map<?, ?> event = asMap(posting == null ? null : posting.get("event"));
            if (event == null || event.get("when") == null) {
                continue;
            }
            String subject = "posting [" + text(posting, "name") + "] event when";
            Object sourceName = event.get("onTransition") == null ? event.get("onCreate") : event.get("onTransition");
            String source = sourceName == null ? null
                    : String.valueOf(sourceName)
                            .trim();
            // A cross-model source's nomenclature is seeded in its own model; name it as such rather
            // than reporting the entity as unknown.
            Target status = text(event, "model") != null ? new Target(source, text(event, "model")) : statusOf(source);
            if (event.get("when") instanceof String) { // a list `when` is refused by the parser; do not mangle it into a string here
                put(event, "when", rewriteExpression(text(event, "when"), statusRelationName(source), status, subject));
            }
        }
    }

    /**
     * Every status a create-from names, all three on the SOURCE's nomenclature: the {@code event} guard
     * it qualifies on (issue #6711, exactly as a posting's does), the {@code fromStatus} guard the
     * click has to satisfy (issue #7068), the {@code sourceStatus} completion hook it flips to once the
     * target exists, and the {@code sourceStatusOnRetire} the retirement of that target returns it to
     * (issue #6868). The source is {@code from:}, owned by {@code fromUses:} when it is not local.
     */
    private void rewriteGenerates(Map<?, ?> root) {
        for (Object node : asList(root.get("generates"))) {
            Map<?, ?> generate = asMap(node);
            if (generate == null) {
                continue;
            }
            String source = text(generate, "from");
            String subject = "generates [" + text(generate, "name") + "]";
            Target status = text(generate, "fromUses") != null ? new Target(source, text(generate, "fromUses")) : statusOf(source);
            Map<?, ?> event = asMap(generate.get("event"));
            if (event != null && event.get("when") != null) {
                rewriteWhen(event, statusRelationName(source), status, subject + " event when");
            }
            putResolvedList(generate, "fromStatus", status, subject + " fromStatus");
            putResolved(generate, "sourceStatus", status, subject + " sourceStatus");
            putResolved(generate, "sourceStatusOnRetire", status, subject + " sourceStatusOnRetire");
            rewriteGeneratesItemsWhere(generate, subject);
        }
    }

    /**
     * The statuses a reversal (issue #7886) names, all on its entity's own nomenclature: the
     * {@code when} guard it is offered under and every {@code refuse} condition.
     */
    private void rewriteReversals(Map<?, ?> root) {
        for (Object node : asList(root.get("reversals"))) {
            Map<?, ?> reversal = asMap(node);
            if (reversal == null) {
                continue;
            }
            String entity = text(reversal, "forEntity");
            String subject = "reversal [" + text(reversal, "name") + "]";
            Target status = statusOf(entity);
            rewriteWhen(reversal, statusRelationName(entity), status, subject + " when");
            for (Object refusal : asList(reversal.get("refuse"))) {
                Map<?, ?> entry = asMap(refusal);
                if (entry != null) {
                    rewriteWhen(entry, statusRelationName(entity), status, subject + " refuse when");
                }
            }
        }
    }

    /**
     * The source-row rule of a create-from's items block (issue #7091), whose status condition alone is
     * symbolic - and on the ITEM row's own nomenclature, not the header's: the rule selects the rows of
     * the source document, so resolving a name against the document's lifecycle would take an id out of
     * the wrong nomenclature and quietly filter on it.
     *
     * <p>
     * A cross-model source ({@code fromUses:}) owns its items too, so their nomenclature is seeded in
     * that model and unresolvable here - and a LOCAL entity of the same name must not lend its own,
     * whose ids are positional in the wrong nomenclature. The conditions therefore keep the numeric-id
     * form every cross-model status site keeps. WHICH of them names the status is known only to the
     * owner's {@code .model}, so a name there is refused where that model is read: at generation time,
     * by {@code GlueIntentGenerator} (dirigible #7225) - not left in place to render as a string
     * compared against the integer status FK.
     */
    private void rewriteGeneratesItemsWhere(Map<?, ?> generate, String subject) {
        if (text(generate, "fromUses") != null) {
            return;
        }
        Map<?, ?> items = asMap(generate.get("items"));
        String itemEntity = items == null ? null : text(items, "from");
        rewriteConditions(items == null ? null : items.get("where"), itemEntity, subject + " items where");
    }

    /**
     * The row query of a cron schedule (issue #7251) - the same {@code { field, op, value }} triples an
     * items rule carries, and the site a status guard is written at most often: a dunning run, a
     * staleness sweep, a month-end generation all start by naming the status the row must stand in.
     * Left unresolved, the name reached the generated job as a string compared against the integer
     * status FK ({@code .eq("Status", "OVERDUE")}), so the query matched nothing forever and the
     * schedule ticked on doing nothing - the silent failure naming a status exists to remove (#6645). A
     * condition on any other to-one relation names a seed of that relation's own target and resolves
     * the same way (#8015).
     *
     * <p>
     * Same-model source only. A cross-model source ({@code model: <uses alias>}) is not in this file's
     * {@code entities}, so neither its nomenclature nor even WHICH of the conditions names its status
     * is knowable here - and it therefore keeps the numeric-id form, exactly as every other cross-model
     * status site does. A name written there is not left in place to render as a string compared
     * against the integer status FK: it is refused where the owner's {@code .model} is read, at
     * generation time by {@code GlueIntentGenerator} (issue #7288), the same way the sibling
     * cross-model {@code items: where:} rule is (#7225).
     */
    private void rewriteSchedules(Map<?, ?> root) {
        for (Object node : asList(root.get("schedules"))) {
            Map<?, ?> schedule = asMap(node);
            if (schedule == null || text(schedule, "model") != null) {
                continue;
            }
            rewriteConditions(schedule.get("where"), text(schedule, "entity"), "schedule [" + text(schedule, "name") + "] where");
        }
    }

    /**
     * A roll-up's row filter and its capacity-guard gate (issue #7542) - both on the CHILD's own
     * nomenclature, since both are about which of the child's rows count and when the row being written
     * is checked against the parent's capacity.
     *
     * <p>
     * Same-model child only, for the reason every other cross-model status site keeps the numeric form:
     * a foreign child's seeds are declared in its owner model. The parser refuses both keys on that
     * direction outright, so nothing is left here to render against the wrong nomenclature.
     */
    private void rewriteRollups(Map<?, ?> root) {
        for (Object node : asList(root.get("rollups"))) {
            Map<?, ?> rollup = asMap(node);
            if (rollup == null || text(rollup, "model") != null) {
                continue;
            }
            String child = text(rollup, "entity");
            String subject = "rollup [" + text(rollup, "name") + "]";
            rewriteConditions(rollup.get("where"), child, subject + " where");
            putResolved(rollup, "guardAt", statusOf(child), subject + " guardAt");
        }
    }

    /**
     * The {@code event.when} guard of the three declarative glue lists - {@code notifications},
     * {@code integrations} and {@code outbound} (issue #7289) - on the nomenclature of the entity the
     * bound event is about.
     *
     * <p>
     * These bind the same event axis a posting and an event-driven create-from bind, whose
     * {@code event.when} has been symbolic since #6711, and the natural authoring of the construct is
     * the status one: "mail the customer when the invoice reaches ISSUED", "forward the record once it
     * is APPROVED". Left unresolved the name reached the generated listener as a string compared
     * against the integer status FK ({@code Objects.equals(entity.Status, "ISSUED")}) - never true, so
     * the mail never went out and the departure never left, with parse, generation, compile and publish
     * all green.
     *
     * <p>
     * The event entity is always LOCAL here (the axis takes no {@code model:}, and the parser refuses
     * an unknown entity), so the nomenclature is this file's: a guard about anything else passes
     * through untouched, exactly as at every other guard site.
     */
    private void rewriteGlue(Map<?, ?> root) {
        for (Map.Entry<String, String> list : GLUE_LISTS) {
            for (Object node : asList(root.get(list.getKey()))) {
                Map<?, ?> entry = asMap(node);
                Map<?, ?> event = asMap(entry == null ? null : entry.get("event"));
                if (event == null || event.get("when") == null) {
                    continue;
                }
                String entity = glueEventEntityOf(root, event);
                rewriteWhen(event, statusRelationName(entity), statusOf(entity),
                        list.getValue() + " [" + text(entry, "name") + "] event when");
            }
        }
    }

    /**
     * The entity the bound event is about: the one a lifecycle binding names, or - for a process step
     * binding, which names a step rather than a record - the trigger entity of that process, the record
     * the step event is delivered about.
     *
     * <p>
     * This runs on the RAW tree, before the typed mapping, so the kinds are spelled out rather than
     * read off {@code EventBinding}; keep them in step with it. A kind missing here is silent in the
     * usual way: the event entity does not resolve, so a status NAME in that guard cannot be looked up
     * and the model is refused with a message about the nomenclature rather than about the binding.
     */
    private static String glueEventEntityOf(Map<?, ?> root, Map<?, ?> event) {
        for (String kind : List.of("onCreate", "onUpdate", "onDelete", "onTransition", "onPhase", "onNotifyFailed")) {
            String entity = text(event, kind);
            if (entity != null) {
                return entity;
            }
        }
        for (String kind : List.of(StepEventSupport.ON_STEP_REACHED, StepEventSupport.ON_STEP_COMPLETED)) {
            Map<?, ?> binding = asMap(event.get(kind));
            String processName = binding == null ? null : text(binding, "process");
            if (processName == null) {
                continue;
            }
            for (Object node : asList(root.get("processes"))) {
                Map<?, ?> process = asMap(node);
                if (process != null && processName.equals(text(process, "name"))) {
                    return triggerEntityOf(process);
                }
            }
        }
        return null;
    }

    /**
     * Resolve the conditions of a {@code { field, op, value }} where list whose {@code field} names a
     * to-one relation of the queried entity: the {@code function: EntityStatus} one on that entity's
     * own nomenclature, any other (issue #8015) on the nomenclature of the relation's own target - a
     * request's {@code type} names a seed of the request types, not a status.
     *
     * <p>
     * Only those conditions are candidates at all, exactly as a register lookup's static filter is:
     * every other condition compares an ordinary column, whose string value (an {@code op: like} on a
     * name) is just a value and would be reported as an unknown status.
     */
    private void rewriteConditions(Object where, String entityName, String subject) {
        String statusRelation = statusRelationName(entityName);
        for (Object node : asList(where)) {
            Map<?, ?> condition = asMap(node);
            String field = condition == null ? null : text(condition, "field");
            if (field == null) {
                continue;
            }
            if (lower(field).equals(lower(statusRelation))) {
                putResolved(condition, "value", statusOf(entityName), subject + " [" + field + "]");
                continue;
            }
            Map<?, ?> relation = toOneRelationIgnoringCase(entityName, field);
            if (relation != null) {
                rewriteRelationCondition(condition, relation, subject + " [" + field + "]");
            }
        }
    }

    /**
     * Resolve a {@code where} condition on a non-status to-one relation whose value is a seeded name.
     * The foreign key is an integer, so a name is meaningful only for equality: {@code eq}/{@code ne}
     * resolve it, any other operator is refused.
     */
    private void rewriteRelationCondition(Map<?, ?> condition, Map<?, ?> relation, String subject) {
        String name = condition.get("value") instanceof String ? text(condition, "value") : null;
        String op = lower(text(condition, "op"));
        if (name == null || op == null || INTEGER.matcher(name)
                                                 .matches()) {
            return; // nothing symbolic - or no operator, which the parser refuses
        }
        if (!"eq".equals(op) && !"ne".equals(op)) {
            issues.add(subject + " compares the relation to the name [" + name + "] with [" + op
                    + "] - a seeded name has no ordering; use eq/ne");
            return;
        }
        putResolved(condition, "value", new Target(text(relation, "to"), text(relation, "model")), subject);
    }

    /**
     * An effective-dated register lookup routes the record it enriches by status: each of its three
     * outcomes may name one, on the record's own nomenclature.
     */
    private void rewriteResolves(Map<?, ?> root) {
        for (Object node : asList(root.get("resolves"))) {
            Map<?, ?> resolve = asMap(node);
            Map<?, ?> event = asMap(resolve == null ? null : resolve.get("event"));
            if (event == null) {
                continue;
            }
            String record = event.get("onCreate") != null ? text(event, "onCreate") : text(event, "onUpdate");
            Target status = statusOf(record);
            String subject = "resolve [" + text(resolve, "name") + "]";
            if (event.get("when") instanceof String) { // a list `when` is refused by the parser; do not mangle it into a string here
                put(event, "when", rewriteExpression(text(event, "when"), statusRelationName(record), status, subject + " event when"));
            }
            for (String outcome : List.of("found", "notFound", "ambiguous")) {
                Map<?, ?> block = asMap(resolve.get(outcome));
                if (block != null) {
                    putResolved(block, "setStatus", status, subject + " " + outcome + " setStatus");
                }
            }
            rewriteResolveWhere(resolve, subject);
        }
    }

    /**
     * The static register filter of a lookup, whose status pair alone is symbolic.
     *
     * <p>
     * Two things make this narrower than every other site. The filter narrows the REGISTER, so its
     * nomenclature is the register's - resolving on the record's would take an id from the wrong
     * lifecycle and quietly filter on it. And only the pair naming the register's
     * {@code function: EntityStatus} relation is a candidate at all: the other pairs are ordinary
     * columns whose literals are just literals, and handing a string like {@code Kind: PRIMARY} to the
     * symbol resolver would report it as an unknown status rather than leaving it alone.
     */
    private void rewriteResolveWhere(Map<?, ?> resolve, String subject) {
        Map<?, ?> where = asMap(resolve.get("where"));
        String register = text(resolve, "from");
        String statusRelation = statusRelationName(register);
        if (where == null || statusRelation == null) {
            return;
        }
        Target registerStatus = statusOf(register);
        for (Object key : new ArrayList<>(where.keySet())) {
            String name = String.valueOf(key);
            if (lower(name).equals(lower(statusRelation))) {
                putResolved(where, name, registerStatus, subject + " where [" + name + "]");
            }
        }
    }

    private void rewriteReports(Map<?, ?> root) {
        for (Object node : asList(root.get("reports"))) {
            Map<?, ?> report = asMap(node);
            if (report == null || report.get("filter") == null) {
                continue;
            }
            String source = text(report, "source");
            put(report, "filter", rewriteExpression(text(report, "filter"), statusRelationName(source), statusOf(source),
                    "report [" + text(report, "name") + "] filter"));
        }
    }

    /**
     * The entity whose event starts the process - the target of its {@code trigger}. This runs on the
     * RAW tree, before the typed mapping, so it cannot read {@code EventBinding} and the kinds are
     * spelled out; keep them in step with it. Missing one is silent in the worst way: the trigger
     * entity does not resolve, so a status NAME anywhere in that process cannot be looked up and the
     * whole model is refused with a message about the nomenclature rather than the trigger.
     */
    private static String triggerEntityOf(Map<?, ?> process) {
        Map<?, ?> trigger = asMap(process.get("trigger"));
        if (trigger == null) {
            return null;
        }
        // Every kind EventBinding knows, because this runs on the RAW tree before the typed mapping and
        // therefore cannot read EventBinding itself: a kind missing here stops the trigger entity from
        // resolving, so a status NAME anywhere in that process is refused with a message about the
        // nomenclature rather than about the trigger.
        for (String event : List.of("onCreate", "onUpdate", "onDelete", "onTransition", "onNotifyFailed")) {
            String entity = text(trigger, event);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }

    /**
     * The entity whose lifecycle event resumes a {@code wait} step - the target of the one
     * {@code onCreate}/{@code onUpdate}/{@code onTransition} arg it declares. Read through the
     * generator's own list, so the vocabulary a wait accepts and the guard resolved against it cannot
     * drift apart (the parser validates the same list).
     */
    private static String waitEventEntityOf(Map<?, ?> args) {
        for (String event : ProcessWaitSupport.EVENT_KINDS) {
            String entity = text(args, event);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }

    // ----- resolution -------------------------------------------------------------

    /** The status nomenclature an entity's lifecycle is expressed through. */
    private Target statusOf(String entityName) {
        Map<?, ?> relation = statusRelation(entityName);
        return relation == null ? new Target(null, null) : new Target(text(relation, "to"), text(relation, "model"));
    }

    private String statusRelationName(String entityName) {
        Map<?, ?> relation = statusRelation(entityName);
        return relation == null ? null : text(relation, "name");
    }

    private Map<?, ?> statusRelation(String entityName) {
        for (Object node : asList(entities.get(entityName) == null ? null
                : entities.get(entityName)
                          .get("relations"))) {
            Map<?, ?> relation = asMap(node);
            if (relation != null && "entitystatus".equals(lower(text(relation, "function")))) {
                return relation;
            }
        }
        return null;
    }

    /**
     * Resolve every {@code <Relation> ==|!= <NAME>} comparison of a decision condition (issue #7648) or
     * a trigger guard (issue #8015) against the nomenclature THAT relation points at.
     *
     * <p>
     * A term naming anything but a to-one relation of the entity is left exactly as written - a
     * decision compares its own fields, a form-set {@code action} and literals too, and none of those
     * is a status site. An ordering comparison against a name is refused for the reason it is
     * everywhere: names have no order.
     *
     * @param condition the authored condition
     * @param entityName the process's trigger entity
     * @param resolvedRelation a relation an earlier pass over the same condition already resolved - its
     *        comparisons are left to that pass, so a name it refused is not reported twice - or
     *        {@code null}
     * @param relationOf the relation of the entity a term names - entity and term name in, {@code null}
     *        out for a term that is not a nomenclature reference
     * @param subject what to name in an issue
     * @return the condition with each resolvable name replaced by its seed id
     */
    private String rewriteRelationComparisons(String condition, String entityName, String resolvedRelation,
            BiFunction<String, String, Map<?, ?>> relationOf, String subject) {
        if (condition == null || entityName == null) {
            return condition;
        }
        Matcher matcher = COMPARISON.matcher(condition);
        StringBuilder rewritten = new StringBuilder();
        while (matcher.find()) {
            String replacement = matcher.group();
            Map<?, ?> relation = matcher.group(1)
                                        .equalsIgnoreCase(resolvedRelation) ? null : relationOf.apply(entityName, matcher.group(1));
            boolean numeric = INTEGER.matcher(matcher.group(3))
                                     .matches();
            boolean nullTest = "null".equals(matcher.group(3));
            if (relation != null && !numeric && !nullTest) {
                if (!EQUALITY.contains(matcher.group(2))) {
                    issues.add(subject + " compares [" + matcher.group(1) + "] to the name [" + matcher.group(3) + "] with ["
                            + matcher.group(2) + "] - a seeded name has no ordering; use ==/!=");
                } else {
                    Integer id = resolveSymbol(matcher.group(3), new Target(text(relation, "to"), text(relation, "model")), subject);
                    if (id != null) {
                        replacement = matcher.group(1) + " " + matcher.group(2) + " " + id;
                    }
                }
            }
            matcher.appendReplacement(rewritten, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(rewritten);
        return rewritten.toString();
    }

    private Map<?, ?> toOneRelation(String entityName, String relationName) {
        for (Object node : asList(entities.get(entityName) == null ? null
                : entities.get(entityName)
                          .get("relations"))) {
            Map<?, ?> relation = asMap(node);
            if (relation != null && relationName.equals(text(relation, "name"))) {
                return relation;
            }
        }
        return null;
    }

    private Map<?, ?> toOneRelationIgnoringCase(String entityName, String relationName) {
        for (Object node : asList(entities.get(entityName) == null ? null
                : entities.get(entityName)
                          .get("relations"))) {
            Map<?, ?> relation = asMap(node);
            if (relation != null && isToOne(relation) && relationName.equalsIgnoreCase(text(relation, "name"))) {
                return relation;
            }
        }
        return null;
    }

    private static boolean isToOne(Map<?, ?> relation) {
        String kind = lower(text(relation, "kind"));
        return "manytoone".equals(kind) || "onetoone".equals(kind);
    }

    /** Resolve a scalar key in place when it holds a symbol; leave a numeric value untouched. */
    private void putResolved(Map<?, ?> owner, String key, Target target, String subject) {
        if (owner.get(key) == null) {
            return;
        }
        Integer id = resolveScalar(owner.get(key), target, subject);
        if (id != null) {
            put(owner, key, id);
        }
    }

    /** Resolve a key holding either a single status or a list of them, in place. */
    @SuppressWarnings("unchecked")
    private void putResolvedList(Map<?, ?> owner, String key, Target target, String subject) {
        Object value = owner.get(key);
        if (value instanceof List<?> list) {
            List<Object> mutable = (List<Object>) list;
            for (int i = 0; i < mutable.size(); i++) {
                Integer id = resolveScalar(mutable.get(i), target, subject);
                if (id != null) {
                    mutable.set(i, id);
                }
            }
        } else {
            putResolved(owner, key, target, subject);
        }
    }

    /**
     * The seed id a scalar names, or {@code null} when it is already numeric (nothing to do) or did not
     * resolve (an issue is recorded).
     */
    private Integer resolveScalar(Object value, Target target, String subject) {
        if (value instanceof Number || value == null) {
            return null;
        }
        String token = String.valueOf(value)
                             .trim();
        if (token.isEmpty() || INTEGER.matcher(token)
                                      .matches()) {
            return null;
        }
        return resolveSymbol(token, target, subject);
    }

    /**
     * Rewrite a {@code when} guard in place - a scalar comparison string, or a LIST of them (implicit
     * AND, dirigible #6957): each element is rewritten independently, so a status name resolves to its
     * seed id while the elements guarding other properties (a string trace field such as a lookup's
     * {@code outcome:}) pass through untouched.
     */
    private void rewriteWhen(Map<?, ?> owner, String statusRelation, Target target, String subject) {
        rewriteWhen(owner, "when", statusRelation, target, subject);
    }

    /** {@link #rewriteWhen(Map, String, Target, String)} over a condition held under another key. */
    @SuppressWarnings("unchecked")
    private void rewriteWhen(Map<?, ?> owner, String key, String statusRelation, Target target, String subject) {
        Object value = owner.get(key);
        if (value instanceof List<?> list) {
            List<Object> mutable = (List<Object>) list;
            for (int i = 0; i < mutable.size(); i++) {
                if (mutable.get(i) instanceof String term) {
                    mutable.set(i, rewriteExpression(term, statusRelation, target, subject));
                }
            }
        } else if (value instanceof String expression) {
            put(owner, key, rewriteExpression(expression, statusRelation, target, subject));
        }
    }

    /**
     * Rewrite a process trigger's {@code when} guard in place, scalar or list (#6957): each term first
     * through the record's own status, then through every other to-one relation of the trigger entity,
     * each against its own target's nomenclature (#8015). Fields, literals and {@code null} tests pass
     * through untouched.
     */
    @SuppressWarnings("unchecked")
    private void rewriteTriggerWhen(Map<?, ?> trigger, String triggerEntity, String subject) {
        Object value = trigger.get("when");
        if (value instanceof List<?> list) {
            List<Object> mutable = (List<Object>) list;
            for (int i = 0; i < mutable.size(); i++) {
                if (mutable.get(i) instanceof String term) {
                    mutable.set(i, rewriteTriggerTerm(term, triggerEntity, subject));
                }
            }
        } else if (value instanceof String term) {
            put(trigger, "when", rewriteTriggerTerm(term, triggerEntity, subject));
        }
    }

    private String rewriteTriggerTerm(String term, String triggerEntity, String subject) {
        String statusRelation = statusRelationName(triggerEntity);
        String rewritten = rewriteExpression(term, statusRelation, statusOf(triggerEntity), subject);
        return rewriteRelationComparisons(rewritten, triggerEntity, statusRelation, this::toOneRelation, subject);
    }

    /**
     * Rewrite a record-scoped condition in place, scalar or list (#6957) - a check's {@code when} and a
     * field's or a composition child's {@code visibleWhen} (#8057): each term first through the
     * record's own status, then through every other to-one relation of the record, each against its own
     * target's nomenclature, as a trigger guard is (#8015). The relation is matched as the parser's
     * guard validator reads the property ({@code CheckSupport.toOne}: case-insensitively, to-one only).
     * Fields, literals, {@code null} tests and numeric ids pass through untouched.
     *
     * @param owner the node holding the condition
     * @param key the condition's key in it
     * @param entityName the record the condition is read off - for a panel's {@code visibleWhen}, the
     *        composition master
     * @param subject what to name in an issue
     */
    private void rewriteRecordWhen(Map<?, ?> owner, String key, String entityName, String subject) {
        rewriteTerms(owner, key, term -> rewriteRecordTerm(term, entityName, subject));
    }

    private String rewriteRecordTerm(String term, String entityName, String subject) {
        String statusRelation = statusRelationName(entityName);
        String rewritten = rewriteExpression(term, statusRelation, statusOf(entityName), subject);
        return rewriteRelationComparisons(rewritten, entityName, statusRelation, this::toOneRelationIgnoringCase, subject);
    }

    /**
     * Apply {@code rewrite} to a condition held under {@code key}: a scalar, or each string of a list.
     */
    @SuppressWarnings("unchecked")
    private static void rewriteTerms(Map<?, ?> owner, String key, UnaryOperator<String> rewrite) {
        Object value = owner.get(key);
        if (value instanceof List<?> list) {
            List<Object> mutable = (List<Object>) list;
            for (int i = 0; i < mutable.size(); i++) {
                if (mutable.get(i) instanceof String term) {
                    mutable.set(i, rewrite.apply(term));
                }
            }
        } else if (value instanceof String term) {
            put(owner, key, rewrite.apply(term));
        }
    }

    /**
     * The composition master of a raw entity node: the target of its first local
     * {@code composition: true} to-one, as {@code IntentEntities.compositionParents} reads it off the
     * typed model; {@code null} for an entity that is nobody's child.
     */
    private static String compositionMasterOf(Map<?, ?> entity) {
        for (Object node : asList(entity.get("relations"))) {
            Map<?, ?> relation = asMap(node);
            String kind = relation == null ? null : text(relation, "kind");
            if (("manyToOne".equals(kind) || "oneToOne".equals(kind)) && Boolean.TRUE.equals(relation.get("composition"))
                    && text(relation, "to") != null) {
                return text(relation, "to");
            }
        }
        return null;
    }

    /**
     * Rewrite a {@code forbidWhen} / {@code requiredWhen} guard in place - each term either a one-hop
     * {@code <Relation>.<field> ==|!= <NAME>} (dirigible #7275, #7495), whose name resolves against the
     * RELATION TARGET's nomenclature, or a record-local comparison that defers to the ordinary
     * record-scoped rewrite. A cross-model relation's nomenclature is seeded in its owner model, which
     * this parser cannot read, so a name there is refused with the numeric-id fallback - as every
     * cross-model status reference is.
     */
    private void rewriteForbidWhen(String entityName, Map<?, ?> check, String subject) {
        rewriteTerms(check, "when", term -> rewriteForbidWhenTerm(entityName, term, subject));
    }

    private String rewriteForbidWhenTerm(String entityName, String term, String subject) {
        Matcher matcher = RELATION_COMPARISON.matcher(term);
        if (!matcher.matches()) {
            // A record-local term (the child's own status, flag or other to-one): resolve exactly as
            // every other record-scoped guard does.
            return rewriteRecordTerm(term, entityName, subject);
        }
        String relationName = matcher.group(1);
        String field = matcher.group(2);
        String name = matcher.group(4);
        if (INTEGER.matcher(name)
                   .matches()) {
            return term; // already a seed id
        }
        Map<?, ?> relation = toOneRelation(entityName, relationName);
        if (relation == null) {
            return term; // the parser reports the unknown relation
        }
        String targetEntity = text(relation, "to");
        String targetStatusRelation = statusRelationName(targetEntity);
        if (targetStatusRelation == null || !targetStatusRelation.equalsIgnoreCase(field)) {
            return term; // not the target's status field - an ordinary column comparison, left alone
        }
        if (text(relation, "model") != null) {
            issues.add(subject + " names the status [" + name + "] of [" + targetEntity + "], which belongs to model ["
                    + text(relation, "model") + "] and is seeded there - a cross-model status must be referenced by its numeric seed id");
            return term;
        }
        Integer id = resolveSymbol(name, statusOf(targetEntity), subject);
        return id == null ? term : relationName + "." + field + " " + matcher.group(3) + " " + id;
    }

    /**
     * Rewrite `&lt;status relation&gt; == &lt;NAME&gt;` comparisons in a guard / filter expression.
     * When the nomenclature is cross-model its relation name is not resolvable here, so ANY symbolic
     * comparison is reported as the cross-model status reference it almost certainly is - the
     * alternative is leaving it in place and failing later with a message about a malformed guard.
     */
    private String rewriteExpression(String expression, String statusRelation, Target target, String subject) {
        if (expression == null || (statusRelation == null && target.model() == null)) {
            return expression;
        }
        Matcher matcher = COMPARISON.matcher(expression);
        StringBuilder rewritten = new StringBuilder();
        while (matcher.find()) {
            String replacement = matcher.group();
            boolean aboutStatus = statusRelation == null || statusRelation.equalsIgnoreCase(matcher.group(1));
            // `Status != null` tests whether the status is set at all (#7555) - no name to look up
            boolean nullTest = "null".equals(matcher.group(3));
            if (aboutStatus && !nullTest && !INTEGER.matcher(matcher.group(3))
                                                    .matches()) {
                if (!EQUALITY.contains(matcher.group(2))) {
                    issues.add(subject + " compares the status to the name [" + matcher.group(3) + "] with [" + matcher.group(2)
                            + "] - a status name has no ordering; use ==/!= per status, or a report `scope:`");
                } else {
                    Integer id = resolveSymbol(matcher.group(3), target, subject);
                    if (id != null) {
                        replacement = matcher.group(1) + " " + matcher.group(2) + " " + id;
                    }
                }
            }
            matcher.appendReplacement(rewritten, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(rewritten);
        return rewritten.toString();
    }

    /**
     * The seed id the name refers to; records an issue and returns {@code null} when it cannot resolve.
     */
    private Integer resolveSymbol(String name, Target target, String subject) {
        if (target.entity() == null) {
            issues.add(subject + " names the status [" + name
                    + "] but no `function: EntityStatus` relation resolves the nomenclature to look it up in");
            return null;
        }
        if (target.model() != null) {
            issues.add(subject + " names the status [" + name + "] of [" + target.entity() + "], which belongs to model [" + target.model()
                    + "] and is seeded there - a cross-model status must be referenced by its numeric seed id");
            return null;
        }
        Map<String, Integer> ids = seededIds.get(target.entity());
        if (ids == null || ids.isEmpty()) {
            issues.add(subject + " names the status [" + name + "] but [" + target.entity()
                    + "] has no seeded rows in this model - seed the nomenclature or use the numeric id");
            return null;
        }
        Integer id = ids.get(name);
        if (id == null) {
            for (Map.Entry<String, Integer> entry : ids.entrySet()) {
                if (entry.getKey()
                         .equalsIgnoreCase(name)) {
                    id = entry.getValue();
                    break;
                }
            }
        }
        if (id == null) {
            issues.add(
                    subject + " names [" + name + "], which is not a seeded status of [" + target.entity() + "] - known: " + ids.keySet());
        }
        return id;
    }

    /** A status nomenclature: the entity holding it and, when it is not local, the model owning it. */
    private record Target(String entity, String model) {
    }

    // ----- raw-tree helpers -------------------------------------------------------

    private static Map<?, ?> asMap(Object node) {
        return node instanceof Map<?, ?> map ? map : null;
    }

    private static List<?> asList(Object node) {
        return node instanceof List<?> list ? list : List.of();
    }

    private static String text(Map<?, ?> map, String key) {
        Object value = map == null ? null : map.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value)
                            .trim();
        return text.isEmpty() ? null : text;
    }

    private static String lower(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    @SuppressWarnings("unchecked")
    private static void put(Map<?, ?> owner, String key, Object value) {
        ((Map<Object, Object>) owner).put(key, value);
    }

    private static Integer integerOf(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value)
                            .trim();
        return INTEGER.matcher(text)
                      .matches() ? Integer.valueOf(text) : null;
    }
}
