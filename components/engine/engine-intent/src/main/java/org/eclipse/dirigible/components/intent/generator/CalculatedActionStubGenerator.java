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

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.RelationIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Scaffolds a {@code CalculatedField} stub under {@code custom/} for every calculated-field
 * <em>action</em> the intent names but the project does not have a class for.
 *
 * <p>
 * A {@code calculatedActionOnCreate} / {@code calculatedActionOnUpdate} is the modeled hand-off to
 * hand-written Java: the generated repository calls
 * {@code Beans.get(<class>.class).calculate(entity)} just before persisting. Until now the
 * declaration produced a repository that referenced a class nobody had written - and because client
 * Java compiles as one batch, that took the whole project's beans down rather than just that field.
 * A named boundary should hand the developer a file to open, exactly as a bare service task does
 * ({@link ServiceTaskHandlerGenerator}).
 *
 * <p>
 * <b>Generate-once, never overwritten</b>, for the same reason as the service-task stub:
 * {@code custom/} is the tier the intent layer does not own.
 *
 * <p>
 * <b>Only a class this project would own is scaffolded.</b> The action may name a class that lives
 * somewhere else - a shared project, another package - in which case the owning entity's
 * {@code imports:} says so. Scaffolding then would not be a convenience but a defect: two
 * compilation units declaring the same binary name fail the whole registry-wide batch. So an action
 * whose simple name is already imported from somewhere is left alone, and a qualified action name
 * is only scaffolded when it is under {@code custom.}.
 */
@Component
@Order(365)
public class CalculatedActionStubGenerator implements IntentTargetGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CalculatedActionStubGenerator.class);

    @Override
    public String name() {
        return "calculated-action-stub";
    }

    @Override
    public void generate(IntentGenerationContext context) {
        IntentModel model = context.getModel();
        for (EntityIntent entity : model.getEntities()) {
            Set<String> scaffolded = new LinkedHashSet<>();
            for (FieldIntent field : entity.getFields()) {
                scaffold(context, entity, field.getName(), valueType(field.getType()), field.getCalculatedActionOnCreate(), scaffolded);
                scaffold(context, entity, field.getName(), valueType(field.getType()), field.getCalculatedActionOnUpdate(), scaffolded);
            }
            for (RelationIntent relation : entity.getRelations()) {
                // A relation's action computes the FK - an integer key, whatever the target looks like.
                scaffold(context, entity, relation.getName(), "Integer", relation.getCalculatedActionOnCreate(), scaffolded);
                scaffold(context, entity, relation.getName(), "Integer", relation.getCalculatedActionOnUpdate(), scaffolded);
            }
        }
    }

    /**
     * Write {@code custom/<Class>.java} when this project owns the class and does not have it yet.
     *
     * @param scaffolded the class names already handled for this entity, so a field calculated on both
     *        create and update is scaffolded once
     */
    private void scaffold(IntentGenerationContext context, EntityIntent entity, String property, String valueType, String action,
            Set<String> scaffolded) {
        if (action == null || action.isBlank() || !scaffolded.add(action)) {
            return;
        }
        String fileName = targetFile(entity, action);
        if (fileName == null) {
            LOGGER.debug("Calculated action [{}] is not this project's to write - not scaffolding it", action);
            return;
        }
        String path = context.getProjectRoot() + "/" + fileName;
        if (context.getRepository()
                   .getResource(path)
                   .exists()) {
            return; // preserve the developer's implementation
        }
        String packageName = fileName.substring(0, fileName.lastIndexOf('/'))
                                     .replace('/', '.');
        String simpleName = action.substring(action.lastIndexOf('.') + 1);
        context.writeModelFile(fileName, stub(packageName, simpleName, entity.getName(), property, valueType));
        LOGGER.info("Scaffolded calculated-field action stub [{}] (implement it - it will not be regenerated)", fileName);
    }

    /**
     * The {@code custom/} file this project would own for the action, or {@code null} when the class
     * belongs to somebody else - a package outside {@code custom.}, or a simple name the entity's
     * authored {@code imports:} already brings in from elsewhere. Scaffolding one of those would not be
     * a convenience: two compilation units declaring the same binary name fail the entire registry-wide
     * client-Java batch, taking every module's beans down with them.
     *
     * @param entity the entity declaring the action
     * @param action the declared action class, simple or qualified
     * @return the project-relative file name, or {@code null} when it must not be scaffolded
     */
    static String targetFile(EntityIntent entity, String action) {
        if (action == null || action.isBlank()) {
            return null;
        }
        if (action.contains(".") && !action.startsWith("custom.")) {
            return null;
        }
        String simpleName = action.substring(action.lastIndexOf('.') + 1);
        if (importsDeclare(entity, simpleName)) {
            return null;
        }
        return "custom/" + action.replace('.', '/')
                                 .replaceFirst("^custom/", "")
                + ".java";
    }

    /** Whether the entity's authored {@code imports:} already bring this simple name in. */
    private static boolean importsDeclare(EntityIntent entity, String simpleName) {
        String imports = entity.getImports();
        if (imports == null || imports.isBlank()) {
            return false;
        }
        for (String line : imports.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("import ") && trimmed.replace(";", "")
                                                        .trim()
                                                        .endsWith("." + simpleName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The Java type of the value the action returns.
     *
     * <p>
     * The entity type it takes is deliberately {@code Object}: the generated entity class is the
     * <em>template engine's</em> output, and an intent generator that named its package would be
     * pinning a path it must stay ignorant of. The developer narrows it to the generated entity - the
     * stub's comment says so, and the entity-level {@code imports:} is what makes that name resolvable.
     */
    static String valueType(String type) {
        if (type == null) {
            return "String";
        }
        return switch (type.toLowerCase()) {
            case "integer", "int" -> "Integer";
            case "long" -> "Long";
            case "decimal" -> "java.math.BigDecimal";
            case "double" -> "Double";
            case "boolean" -> "Boolean";
            case "date" -> "java.time.LocalDate";
            case "timestamp" -> "java.time.Instant";
            default -> "String";
        };
    }

    static String stub(String packageName, String className, String entity, String property, String valueType) {
        return """
                package %s;

                import org.eclipse.dirigible.sdk.component.Component;
                import org.eclipse.dirigible.sdk.db.CalculatedField;

                /**
                 * Calculated value for %s.%s - the intent declares this action, the generated repository calls it
                 * just before persisting.
                 *
                 * Scaffolded once under custom/ - it is yours: implement the real logic here, it is never
                 * regenerated or overwritten.
                 *
                 * TODO narrow the Object parameter to the generated %sEntity (add its import to the entity's
                 * `imports:` in the intent, so the generated repository resolves the same name), then compute
                 * the value from the record being saved.
                 */
                @Component
                public class %s implements CalculatedField<Object, %s> {

                    @Override
                    public %s calculate(Object entity) {
                        return null;
                    }
                }
                """.formatted(packageName, entity, property, entity, className, valueType, valueType);
    }
}
