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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.ProcessIntent;
import org.eclipse.dirigible.components.intent.model.RelationIntent;
import org.eclipse.dirigible.components.intent.model.StepIntent;

/**
 * Routes a user task to the person a <b>relation walk off the trigger record</b> names - the
 * requester's manager, the customer's account manager - rather than to a role, or to the record's
 * own owner ({@code assignee: personal}).
 *
 * <pre>
 * - name: approve
 *   kind: userTask
 *   args:
 *     assignee: { path: employee.manager, fallback: manager }
 *     form: ApproveRequest
 * </pre>
 *
 * Each segment of {@code path} is a <b>to-one relation</b> - the first of the trigger entity, each
 * further one of the previous target - and the walk ends at an entity that declares
 * {@code identity}, the same field the personal surfaces map a login to. A cross-model relation may
 * only be the LAST segment: a projection carries the target's own properties (so its identity is
 * known) but not its relations, so there is nothing to walk on from there.
 *
 * <p>
 * The login is resolved <b>at task entry</b>, not at process start: the BPMN generator inserts the
 * generated delegate as a service task right before the task, so a relation an earlier step of the
 * process itself set is already visible - the same latest-moment discipline as
 * {@link ProcessResolverSupport} and {@link ProcessFieldLoadSupport}, and the reason this is not
 * folded into the start-time {@code __personalUser} seeding the trigger listener does. The delegate
 * publishes the resolved login into {@link #variable(String)} and the user task binds
 * {@code flowable:assignee="${...}"} to it.
 *
 * <p>
 * {@code fallback} is <b>required</b>, and names the candidate group the task stays claimable by.
 * It is what makes the unresolvable case total: a null hop, a missing record or a blank identity
 * leaves the variable null, Flowable creates the task unassigned, and the group picks it up - never
 * a task nobody can see.
 */
public final class ProcessAssigneeSupport {

    /** The {@code assignee} arg key carrying the relation walk. */
    private static final String PATH_KEY = "path";

    /** The {@code assignee} arg key carrying the candidate group an unresolved walk falls back to. */
    private static final String FALLBACK_KEY = "fallback";

    /** The only keys a map-shaped {@code assignee} may declare. */
    private static final Set<String> KNOWN_KEYS = Set.of(PATH_KEY, FALLBACK_KEY);

    /** The identity property assumed for a cross-model target the owner's model does not name. */
    private static final String DEFAULT_IDENTITY_PROPERTY = "Email";

    private ProcessAssigneeSupport() {}

    /**
     * Resolves a cross-model relation target's facts, so the last hop of a walk may leave this model.
     * The lookup performs the IO (it reads the owner's {@code .model}); this class stays free of
     * Spring/IO so its path logic remains directly unit-testable. Returns {@code null} when the
     * relation is not a resolvable cross-model target.
     */
    @FunctionalInterface
    public interface CrossModelLookup {
        CrossModelTarget resolve(RelationIntent relation);
    }

    /**
     * The facts a cross-model terminal hop needs: where the owner's generated Entity/Repository live,
     * and which of its properties carries the login.
     *
     * @param perspectiveName the target's perspective in the owner model (its gen data subfolder)
     * @param project the owner project
     * @param modelAlias the owner model alias
     * @param identityProperty the PascalCase identity property, or null when the owner model does not
     *        declare one
     */
    public record CrossModelTarget(String perspectiveName, String project, String modelAlias, String identityProperty) {
    }

    /**
     * The map-shaped {@code assignee} of a user task: the relation walk plus the candidate group an
     * unresolved walk falls back to. Raw author text - neither is validated here.
     *
     * @param path the authored relation walk (e.g. {@code employee.manager})
     * @param fallback the authored candidate group
     */
    public record PathAssignee(String path, String fallback) {
    }

    /**
     * One hop of a resolved walk: the record reached by traversing a to-one relation.
     *
     * @param local the delegate's local variable holding the loaded record ({@code hop0}, {@code hop1},
     *        ...)
     * @param entity the entity reached
     * @param perspective the reached entity's perspective (its gen data subfolder)
     * @param nextFkProperty the PascalCase FK property read off this record to reach the next hop,
     *        empty for the last hop
     * @param crossModel whether the traversed relation points at an entity owned by another model
     * @param targetModel the owner model alias when {@code crossModel}, else empty
     * @param targetProject the owner project when {@code crossModel}, else empty
     */
    public record Hop(String local, String entity, String perspective, String nextFkProperty, boolean crossModel, String targetModel,
            String targetProject) {
    }

    /**
     * One user-task assignee resolver to generate.
     *
     * @param process the owning process
     * @param step the user task whose assignee it resolves (the delegate is inserted before it)
     * @param handler the generated handler class simple name (e.g.
     *        {@code ResolveExpenseApprovalApproveAssignee})
     * @param variable the process variable the delegate publishes the login into
     * @param path the authored walk, for the generated class's documentation
     * @param ownerEntity the trigger entity, loaded by its id to start the walk
     * @param ownerPerspective the trigger entity's perspective
     * @param ownerKeyProperty the process variable holding the trigger record's PK
     * @param ownerKeyAccessor the {@link Number} accessor matching that PK's type
     * @param firstFkProperty the PascalCase FK property read off the trigger record to reach the first
     *        hop
     * @param hops the walk, in order; never empty
     * @param identityLocal the local variable of the last hop - the record the identity is read off
     * @param identityProperty the PascalCase identity property read off the last hop
     */
    public record Assignee(String process, String step, String handler, String variable, String path, String ownerEntity,
            String ownerPerspective, String ownerKeyProperty, String ownerKeyAccessor, String firstFkProperty, List<Hop> hops,
            String identityLocal, String identityProperty) {
    }

    /**
     * A resolved walk, or the reason it did not resolve.
     *
     * @param firstFkProperty the FK property read off the walked entity to reach the first hop
     * @param hops the walk, in order
     * @param identityProperty the PascalCase identity property of the last hop
     * @param failure the reason the walk did not resolve, or null when it did
     */
    public record Walk(String firstFkProperty, List<Hop> hops, String identityProperty, String failure) {

        private static Walk failed(String reason) {
            return new Walk("", List.of(), "", reason);
        }

        /**
         * @return whether the walk resolved
         */
        public boolean resolved() {
            return failure == null;
        }
    }

    /**
     * The map-shaped {@code assignee} of a step, or {@code null} when the step declares none (no
     * assignee at all, or the plain role / {@code personal} literal).
     *
     * @param step the step
     * @return the declared walk and fallback, or null
     */
    public static PathAssignee pathAssignee(StepIntent step) {
        if (!"userTask".equals(step.getKind()) || step.getArgs() == null) {
            return null;
        }
        if (!(step.getArgs()
                  .get("assignee") instanceof Map<?, ?> map)) {
            return null;
        }
        return new PathAssignee(trimmed(map.get(PATH_KEY)), trimmed(map.get(FALLBACK_KEY)));
    }

    /**
     * The keys a map-shaped {@code assignee} declares beyond {@code path} / {@code fallback} - the
     * parser rejects them rather than silently ignoring a misspelling.
     *
     * @param step the step
     * @return the unknown keys, in declaration order
     */
    public static List<String> unknownAssigneeKeys(StepIntent step) {
        List<String> unknown = new ArrayList<>();
        if (!"userTask".equals(step.getKind()) || step.getArgs() == null || !(step.getArgs()
                                                                                  .get("assignee") instanceof Map<?, ?> map)) {
            return unknown;
        }
        for (Object key : map.keySet()) {
            if (key != null && !KNOWN_KEYS.contains(key.toString())) {
                unknown.add(key.toString());
            }
        }
        return unknown;
    }

    /**
     * Walks an assignee path off an entity, reporting the first segment that does not resolve. The
     * message is written to read after {@code "process [P] step [S] "}.
     *
     * @param path the authored walk
     * @param owner the entity the first segment is a relation of
     * @param byName all LOCAL entities by name
     * @param compositionParents composition-parent map (to resolve a hop's perspective)
     * @param settingEntities the setting entities (same)
     * @param crossModel resolver for a cross-model terminal hop's owner facts, or {@code null} to keep
     *        the local-only behavior (unit tests, and the parser, which has no project context)
     * @return the resolved walk, or a failure carrying the reason
     */
    public static Walk walk(String path, EntityIntent owner, Map<String, EntityIntent> byName, Map<String, String> compositionParents,
            Set<String> settingEntities, CrossModelLookup crossModel) {
        if (path == null || path.isBlank()) {
            return Walk.failed("declares an assignee with no path");
        }
        List<Hop> hops = new ArrayList<>();
        String firstFkProperty = null;
        EntityIntent current = owner;
        CrossModelTarget crossModelTerminal = null;
        String[] segments = path.split("\\.", -1);
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];
            if (segment.isBlank()) {
                return Walk.failed("declares assignee path [" + path + "] with an empty segment");
            }
            if (current == null) {
                // The previous hop left this model; a projection carries the target's own properties but
                // not its relations, so the walk has nothing left to traverse.
                return Walk.failed("declares assignee path [" + path + "] but segment [" + segment
                        + "] walks on past a cross-model relation - a cross-model hop can only be the last one");
            }
            RelationIntent relation = toOneRelation(current, segment);
            if (relation == null) {
                return Walk.failed(
                        "declares assignee path [" + path + "] but [" + current.getName() + "] has no to-one relation [" + segment + "]");
            }
            // The FK that reaches this segment is read off the PREVIOUS record, so it is recorded there
            // - on the walked entity for the first segment, back-filled onto the preceding hop after.
            if (i == 0) {
                firstFkProperty = IntentNaming.pascalCase(segment);
            } else {
                Hop previous = hops.get(i - 1);
                hops.set(i - 1, new Hop(previous.local(), previous.entity(), previous.perspective(), IntentNaming.pascalCase(segment),
                        previous.crossModel(), previous.targetModel(), previous.targetProject()));
            }
            boolean isCrossModel = relation.getModel() != null && !relation.getModel()
                                                                           .isBlank();
            CrossModelTarget target = isCrossModel && crossModel != null ? crossModel.resolve(relation) : null;
            EntityIntent local = isCrossModel ? null : byName.get(relation.getTo());
            if (!isCrossModel && local == null) {
                return Walk.failed("declares assignee path [" + path + "] but relation [" + segment + "] targets unknown entity ["
                        + relation.getTo() + "]");
            }
            hops.add(new Hop("hop" + i, relation.getTo(),
                    isCrossModel ? crossModelPerspective(relation, target)
                            : IntentEntities.resolvePerspective(relation.getTo(), compositionParents, settingEntities),
                    "", isCrossModel, isCrossModel ? relation.getModel() : "", target == null ? "" : target.project()));
            current = local;
            crossModelTerminal = isCrossModel ? target : null;
        }
        if (current != null) {
            if (current.getIdentity() == null || current.getIdentity()
                                                        .isBlank()) {
                return Walk.failed("declares assignee path [" + path + "] but its target [" + current.getName()
                        + "] declares no identity - there is no login to assign the task to");
            }
            return new Walk(firstFkProperty, hops, IntentNaming.pascalCase(current.getIdentity()), null);
        }
        // A cross-model target's identity lives in the owner's .model - resolved through the lookup at
        // generation time, exactly like a cross-model personal owner relation, and never at parse time.
        String identityProperty = crossModelTerminal == null ? null : crossModelTerminal.identityProperty();
        return new Walk(firstFkProperty, hops,
                identityProperty == null || identityProperty.isBlank() ? DEFAULT_IDENTITY_PROPERTY : identityProperty, null);
    }

    /**
     * Every user-task assignee resolver across every process in the model. A step whose walk does not
     * resolve is skipped: the parser has already reported it as an issue, so Generate must not also
     * emit a delegate that cannot compile.
     *
     * @param model the intent
     * @param crossModel resolver for a cross-model terminal hop's owner facts, or {@code null}
     * @return the resolvers, in declaration order
     */
    public static List<Assignee> assignees(IntentModel model, CrossModelLookup crossModel) {
        List<Assignee> assignees = new ArrayList<>();
        Map<String, EntityIntent> byName = IntentEntities.byName(model);
        Map<String, String> compositionParents = IntentEntities.compositionParents(model);
        Set<String> settingEntities = IntentEntities.settingEntities(byName.values());
        Set<String> seen = new LinkedHashSet<>();
        for (ProcessIntent process : model.getProcesses()) {
            String triggerEntity = TriggerSupport.triggerEntity(process);
            EntityIntent owner = triggerEntity == null ? null : byName.get(triggerEntity);
            if (owner == null) {
                continue; // no trigger entity -> no record to walk off
            }
            for (StepIntent step : process.getSteps()) {
                PathAssignee declared = pathAssignee(step);
                if (declared == null || step.getName() == null) {
                    continue;
                }
                Walk walk = walk(declared.path(), owner, byName, compositionParents, settingEntities, crossModel);
                if (!walk.resolved()) {
                    continue;
                }
                String handler = handler(process.getName(), step.getName());
                if (!seen.add(handler)) {
                    continue; // a duplicate step name in one process - the parser reports that
                }
                assignees.add(new Assignee(process.getName(), step.getName(), handler, variable(step.getName()), declared.path(),
                        owner.getName(), IntentEntities.resolvePerspective(owner.getName(), compositionParents, settingEntities),
                        IntentEntities.keyFieldName(owner), idAccessor(IntentEntities.primaryKeyOf(owner)), walk.firstFkProperty(),
                        walk.hops(), walk.hops()
                                         .get(walk.hops()
                                                  .size()
                                                 - 1)
                                         .local(),
                        walk.identityProperty()));
            }
        }
        return assignees;
    }

    /**
     * The process variable a step's resolved login is published into, and which the user task's
     * {@code flowable:assignee} expression reads.
     *
     * @param stepName the user task's name
     * @return the variable name
     */
    public static String variable(String stepName) {
        return "__assignee_" + IntentNaming.camelCase(stepName);
    }

    /**
     * The generated delegate's class simple name.
     *
     * @param processName the owning process
     * @param stepName the user task
     * @return the class simple name
     */
    public static String handler(String processName, String stepName) {
        // Named into the Resolve* delegate family (ResolveCustomerCreditLimit, ...) rather than Assign*,
        // which the effective-dated `resolves` lookup already owns for its listeners.
        return "Resolve" + IntentNaming.pascalCase(processName) + IntentNaming.pascalCase(stepName) + "Assignee";
    }

    private static String crossModelPerspective(RelationIntent relation, CrossModelTarget target) {
        return target != null && target.perspectiveName() != null ? target.perspectiveName() : relation.getTo();
    }

    private static RelationIntent toOneRelation(EntityIntent owner, String name) {
        for (RelationIntent relation : owner.getRelations()) {
            boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
            if (toOne && name.equals(relation.getName())) {
                return relation;
            }
        }
        return null;
    }

    private static String idAccessor(FieldIntent pk) {
        String type = pk == null || pk.getType() == null ? "integer" : pk.getType();
        return "long".equals(type) ? "longValue" : "intValue";
    }

    private static String trimmed(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString()
                           .trim();
        return text.isEmpty() ? null : text;
    }
}
