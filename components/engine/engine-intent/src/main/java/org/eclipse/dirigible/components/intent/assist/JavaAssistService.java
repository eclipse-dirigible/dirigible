/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.assist;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.dirigible.components.intent.LoggedValue;
import org.eclipse.dirigible.components.intent.ai.AssistantGuide;
import org.eclipse.dirigible.components.intent.ai.ChatTurn;
import org.eclipse.dirigible.components.intent.ai.ModelClient;
import org.eclipse.dirigible.components.intent.ai.ProposalRepairLoop;
import org.eclipse.dirigible.engine.java.runtime.CompileDiagnostic;
import org.eclipse.dirigible.engine.java.runtime.JavaSourceCompiler;
import org.eclipse.dirigible.engine.java.runtime.JavaSourceParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * The Workbench assistant: help a developer write the hand-written Java the intent boundary hands
 * them - a {@code CalculatedField} action, a {@code JavaDelegate}, a custom controller.
 *
 * <p>
 * It is the same integration standard the Intent Editor's assistant set: the model proposes the
 * <em>complete</em> file through a single {@code propose_java} tool, the proposal is validated
 * server-side before the developer ever sees it, and the developer accepts an explicit diff. This
 * service never writes to the workspace.
 *
 * <p>
 * Validation here is a real compilation. The proposal is compiled with
 * {@link JavaSourceCompiler#compileBatch} <em>together with every other Java source in the
 * project</em> - the generated entities and repositories the class exists to use - because that is
 * how the client-Java runtime compiles it, and a proposal checked alone would resolve none of those
 * references. The compile is side-effect-free: it never touches {@code JavaLoader}, so nothing is
 * loaded, swapped or published.
 */
@Service
class JavaAssistService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaAssistService.class);

    private static final String TOOL_NAME = "propose_java";

    /**
     * Referenced sources embedded in the prompt; beyond this the developer's file is unusually wide.
     */
    private static final int MAX_REFERENCED_SOURCES = 12;

    /** Project types listed in the prompt as an index of what can be imported. */
    private static final int MAX_INDEXED_TYPES = 300;

    private static final String SYSTEM_PROMPT = AssistantGuide.load("/java-assistant-guide.md");

    private static final ModelClient.ToolSpec TOOL = new ModelClient.ToolSpec(TOOL_NAME,
            "Propose the complete, updated Java source of the file being worked on, for the developer to review as a diff.",
            ModelClient.stringSchema(properties()));

    private final ModelClient modelClient;
    private final JavaSourceCompiler compiler;

    JavaAssistService(ModelClient modelClient, JavaSourceCompiler compiler) {
        this.modelClient = modelClient;
        this.compiler = compiler;
    }

    private static Map<String, String> properties() {
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("explanation", "A short, plain explanation of what the class does and what changed.");
        properties.put("source", "The COMPLETE Java source of the file, including the package declaration and every import.");
        return properties;
    }

    /**
     * Run one assistant turn about a Java file.
     *
     * @param context the file, the project's intent and its sibling sources
     * @param message the developer's message
     * @param history the prior transcript
     * @return the reply, any proposed source and the compiler errors still outstanding on it
     */
    AssistReply chat(AssistContext context, String message, List<ChatTurn> history) {
        AtomicReference<List<AssistDiagnostic>> outstanding = new AtomicReference<>(List.of());
        ProposalRepairLoop loop = new ProposalRepairLoop("source", "java", proposal -> {
            List<AssistDiagnostic> diagnostics = compile(proposal, context);
            outstanding.set(diagnostics);
            return diagnostics.stream()
                              .map(JavaAssistService::describe)
                              .toList();
        }, JavaAssistService::repairTurn);

        List<Map<String, Object>> messages = ModelClient.messages(history, buildUserTurn(context, message));
        ProposalRepairLoop.Outcome outcome = loop.run(messages, this::callModel);

        String reply = replyText(outcome);
        List<AssistDiagnostic> diagnostics = outcome.proposal() == null ? List.of() : outstanding.get();
        if (!diagnostics.isEmpty()) {
            reply += "\n\nNote: this proposal does not compile yet:\n" + ProposalRepairLoop.bulleted(outcome.issues());
        }
        return new AssistReply(reply, outcome.proposal(), diagnostics);
    }

    /**
     * One upstream round-trip. Package-visible so tests can substitute a scripted upstream.
     *
     * @param messages the conversation turns to send
     * @return the model's reply
     */
    ModelClient.ModelReply callModel(List<Map<String, Object>> messages) {
        return modelClient.call(SYSTEM_PROMPT, messages, TOOL);
    }

    /**
     * Compile the proposal together with the project's other sources and return the errors attributed
     * to it. Errors in a sibling are the project's pre-existing state, not this proposal's doing, so
     * they are logged and left out of the repair loop - otherwise the assistant would chase somebody
     * else's broken file forever.
     */
    private List<AssistDiagnostic> compile(String proposal, AssistContext context) {
        String fqn;
        try {
            fqn = JavaSourceParser.parse(proposal)
                                  .fqn();
        } catch (JavaSourceParser.JavaSourceParseException ex) {
            LOGGER.debug("Proposed Java has no top-level type declaration", ex);
            return List.of(new AssistDiagnostic(-1, -1, "the proposed source declares no top-level class, interface, record or enum"));
        }

        List<JavaSourceCompiler.SourceUnit> units = new ArrayList<>();
        units.add(new JavaSourceCompiler.SourceUnit(fqn, proposal));
        for (ProjectSource sibling : context.siblings()) {
            units.add(new JavaSourceCompiler.SourceUnit(sibling.fqn(), sibling.source()));
        }

        JavaSourceCompiler.BatchResult result;
        try {
            result = compiler.compileBatch(units);
        } catch (RuntimeException ex) {
            LOGGER.error("Could not compile the proposed Java for [{}]", LoggedValue.of(context.path()), ex);
            return List.of(new AssistDiagnostic(-1, -1, "the proposal could not be compiled: " + ex.getMessage()));
        }
        if (!result.failures()
                   .isEmpty()) {
            LOGGER.debug("Assist compile of [{}] reported failures for [{}]", LoggedValue.of(context.path()),
                    LoggedValue.of(result.failures()
                                         .keySet()));
        }
        List<CompileDiagnostic> diagnostics = result.diagnostics()
                                                    .get(fqn);
        if (diagnostics == null) {
            return result.failures()
                         .containsKey(fqn)
                                 ? List.of(new AssistDiagnostic(-1, -1, result.failures()
                                                                              .get(fqn)))
                                 : List.of();
        }
        return diagnostics.stream()
                          .filter(CompileDiagnostic::error)
                          .map(d -> new AssistDiagnostic(d.line(), d.column(), d.message()))
                          .toList();
    }

    /** The text blocks, else the tool's own explanation, else a neutral stand-in. */
    private static String replyText(ProposalRepairLoop.Outcome outcome) {
        String text = outcome.reply()
                             .text();
        if (StringUtils.isNotBlank(text)) {
            return text;
        }
        String explanation = outcome.reply()
                                    .toolString("explanation");
        if (StringUtils.isNotBlank(explanation)) {
            return explanation;
        }
        return outcome.proposal() != null ? "I've proposed an update to this class." : "(no response)";
    }

    private static String describe(AssistDiagnostic diagnostic) {
        return diagnostic.line() > 0 ? "line " + diagnostic.line() + ": " + diagnostic.message() : diagnostic.message();
    }

    /** The corrective user turn: the compiler's own errors plus the repair instruction. */
    private static String repairTurn(List<String> issues) {
        return "The proposed Java does not compile. javac reported:\n" + ProposalRepairLoop.bulleted(issues)
                + "\nCall propose_java again with the corrected COMPLETE source. Fix only these errors and keep everything else"
                + " exactly as proposed.";
    }

    /**
     * The user turn: the file as the developer's buffer has it, the project's intent (the model this
     * class plugs into), an index of the project's own types, and the sources this file actually
     * references - assembled server-side, because a browser could only send the buffer.
     */
    private static String buildUserTurn(AssistContext context, String message) {
        StringBuilder turn = new StringBuilder();
        turn.append("Project: ")
            .append(context.project())
            .append("\nFile: ")
            .append(context.path())
            .append("\n\nThe file being worked on:\n```java\n")
            .append(StringUtils.defaultString(context.source()))
            .append("\n```\n");

        if (StringUtils.isNotBlank(context.intentYaml())) {
            turn.append("\nThe application model this class plugs into (app.intent):\n```yaml\n")
                .append(context.intentYaml())
                .append("\n```\n");
        }

        List<ProjectSource> referenced = referencedSources(context);
        if (!referenced.isEmpty()) {
            turn.append("\nThe project sources this file references:\n");
            for (ProjectSource source : referenced) {
                turn.append("\n// ")
                    .append(source.path())
                    .append("\n```java\n")
                    .append(source.source())
                    .append("\n```\n");
            }
        }

        appendTypeIndex(turn, context, referenced);

        return turn.append("\nRequest: ")
                   .append(StringUtils.defaultString(message))
                   .toString();
    }

    /**
     * The sources the file names - by import or by simple name. Bounded, because a class that names
     * fifty types would otherwise push the file itself out of the model's attention; the type index
     * below still lists everything else.
     */
    private static List<ProjectSource> referencedSources(AssistContext context) {
        String source = StringUtils.defaultString(context.source());
        List<ProjectSource> referenced = new ArrayList<>();
        for (ProjectSource sibling : context.siblings()) {
            if (source.contains("import " + sibling.fqn() + ";") || containsWord(source, sibling.simpleName())) {
                referenced.add(sibling);
            }
            if (referenced.size() == MAX_REFERENCED_SOURCES) {
                break;
            }
        }
        return referenced;
    }

    /**
     * Every other type in the project, so the model imports one that exists instead of inventing it.
     */
    private static void appendTypeIndex(StringBuilder turn, AssistContext context, List<ProjectSource> referenced) {
        List<String> names = new ArrayList<>();
        for (ProjectSource sibling : context.siblings()) {
            if (!referenced.contains(sibling)) {
                names.add(sibling.fqn());
            }
        }
        if (names.isEmpty()) {
            return;
        }
        turn.append("\nThe other Java types in this project (import them by these exact names):\n");
        for (String name : names.subList(0, Math.min(names.size(), MAX_INDEXED_TYPES))) {
            turn.append("- ")
                .append(name)
                .append('\n');
        }
        if (names.size() > MAX_INDEXED_TYPES) {
            turn.append("- ... and ")
                .append(names.size() - MAX_INDEXED_TYPES)
                .append(" more, not listed\n");
        }
    }

    /** Whole-word containment, so {@code Invoice} does not match {@code SalesInvoiceEntity}. */
    private static boolean containsWord(String source, String word) {
        int from = 0;
        while (true) {
            int at = source.indexOf(word, from);
            if (at < 0) {
                return false;
            }
            boolean beforeOk = at == 0 || !Character.isJavaIdentifierPart(source.charAt(at - 1));
            int after = at + word.length();
            boolean afterOk = after >= source.length() || !Character.isJavaIdentifierPart(source.charAt(after));
            if (beforeOk && afterOk) {
                return true;
            }
            from = at + 1;
        }
    }
}
