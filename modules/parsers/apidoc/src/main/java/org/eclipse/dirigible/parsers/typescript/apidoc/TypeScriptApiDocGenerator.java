/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.parsers.typescript.apidoc;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.parsers.typescript.TypeScriptLexer;
import org.eclipse.dirigible.parsers.typescript.TypeScriptParser;
import org.eclipse.dirigible.parsers.typescript.TypeScriptParserBaseVisitor;

/**
 * Walks a root directory, parses TypeScript files (*.ts) and produces Markdown API docs into the
 * target directory, preserving relative structure.
 *
 * Usage: TypeScriptApiDocGenerator.generate(Paths.get("/path/to/src"), Paths.get("/path/to/docs"));
 */
public class TypeScriptApiDocGenerator {

    private static String root;

    /**
     * Entry convenience method.
     */
    public static void generate(Path rootDir, Path targetDir) throws IOException {
        if (!Files.isDirectory(rootDir)) {
            throw new IllegalArgumentException("rootDir must be a directory");
        }
        if (Files.isDirectory(targetDir)) {
            FileUtils.deleteDirectory(targetDir.toFile());
        }
        Files.createDirectories(targetDir);
        root = rootDir.toString();

        try (var walker = Files.walk(rootDir)) {
            List<Path> tsFiles = walker.filter(p -> Files.isRegularFile(p))
                                       .filter(p -> p.toString()
                                                     .endsWith(".ts"))
                                       .filter(p -> !p.toString()
                                                      .equals("index.ts"))
                                       .collect(Collectors.toList());

            for (Path tsFile : tsFiles) {
                try {
                    String source = Files.readString(tsFile, StandardCharsets.UTF_8);
                    ApiFileModel model = parseFile(tsFile, source);

                    // compute relative target path
                    Path relative = rootDir.relativize(tsFile);
                    Path mdTarget = targetDir.resolve(relative.toString() + ".md");
                    // replace .ts.ts.md if that happens (i.e., avoid double suffix)
                    String mdName = FilenameUtils.removeExtension(relative.toString()) + ".md";
                    mdTarget = targetDir.resolve(mdName);

                    Files.createDirectories(mdTarget.getParent());
                    Files.writeString(mdTarget, renderMarkdown(model), StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING);
                    System.out.println("Generated: " + mdTarget);
                } catch (Exception e) {
                    System.err.println("Failed to parse/generate for: " + tsFile + " -> " + e.getMessage());
                    e.printStackTrace(System.err);
                }
            }
        }
    }

    /**
     * Parse a single TypeScript file and build a model for Markdown rendering.
     */
    public static ApiFileModel parseFile(Path location, String source) {
        CharStream input = CharStreams.fromString(source);
        TypeScriptLexer lexer = new TypeScriptLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        TypeScriptParser parser = new TypeScriptParser(tokens);

        ParseTree tree = parser.program();
        MetadataVisitor visitor = new MetadataVisitor(tokens);
        ApiFileModel model = visitor.visit(tree);
        model.setSourcePath(location.toString());
        return model;
    }

    /**
     * Render the API model to Markdown.
     */
    public static String renderMarkdown(ApiFileModel model) {
        StringBuilder sb = new StringBuilder();
        sb.append("# API: ")
          .append(Optional.ofNullable(model.getModuleName())
                          .orElse(FilenameUtils.getBaseName(model.getSourcePath())))
          .append("\n\n");
        sb.append("> Source: `")
          .append(model.getSourcePath()
                       .substring(root.length() + 1))
          .append("`\n\n");

        if (model.getFileDocumentation() != null) {
            sb.append(model.getFileDocumentation())
              .append("\n\n");
        }

        String sampleFileMaybe = model.getSourcePath()
                                      .replace(".ts", ".sample");
        if (Files.exists(Path.of(sampleFileMaybe))) {
            try (FileInputStream in = new FileInputStream(sampleFileMaybe)) {
                sb.append("## Usage\n")
                  .append("```javascript\n")
                  .append(IOUtils.toString(in, StandardCharsets.UTF_8))
                  .append("\n```\n")
                  .append("\n\n");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        if (!model.getExportedFunctions()
                  .isEmpty()) {
            sb.append("## Exported Functions\n\n");
            for (ApiFunction f : model.getExportedFunctions()) {
                sb.append("### ")
                  .append(f.getName())
                  .append("\n\n");
                if (f.getDocumentation() != null) {
                    sb.append(DocSanitizer.sanitize(f.getDocumentation()))
                      .append("\n\n");
                }
                sb.append("**Signature:** `")
                  .append(f.getSignature())
                  .append("`\n\n");
                if (!f.getDecorators()
                      .isEmpty()) {
                    sb.append("**Decorators:** `")
                      .append(String.join("`, `", f.getDecorators()))
                      .append("`\n\n");
                }
            }
        }

        if (!model.getClasses()
                  .isEmpty()) {
            sb.append("## Classes\n\n");
            for (ApiClass c : model.getClasses()) {
                sb.append("### ")
                  .append(c.getName())
                  .append("\n\n");
                if (c.getDocumentation() != null)
                    sb.append(DocSanitizer.sanitize(c.getDocumentation()))
                      .append("\n\n");
                if (!c.getDecorators()
                      .isEmpty()) {
                    sb.append("**Decorators:** `")
                      .append(String.join("`, `", c.getDecorators()))
                      .append("`\n\n");
                }

                if (!c.getMethods()
                      .isEmpty()) {
                    sb.append("#### Methods\n\n");
                    for (ApiMethod m : c.getMethods()) {
                        sb.append("<hr/>\n\n")
                          .append("#### ")
                          .append(m.getName())
                          .append("\n\n")
                          .append("- `")
                          .append(m.getName())
                          .append(" (")
                          .append(String.join(", ", m.getParameters()))
                          .append(")")
                          .append(m.getReturnType() != null ? m.getReturnType() : "")
                          .append("`")
                          .append("\n\n");
                        if (m.getDocumentation() != null)
                            sb.append("  ")
                              .append(DocSanitizer.sanitize(m.getDocumentation()))
                              .append("\n\n");
                        if (!m.getDecorators()
                              .isEmpty()) {
                            sb.append("  Decorators: `")
                              .append(String.join("`, `", m.getDecorators()))
                              .append("`\n\n");
                        }
                    }
                }
            }
        }

        if (model.getNotes() != null) {
            sb.append("## Notes\n\n")
              .append(model.getNotes())
              .append("\n\n");
        }

        return sb.toString();
    }

    public static final class ApiFileModel {
        private String moduleName;
        private String sourcePath;
        private String fileDocumentation;
        private String notes;
        private final List<ApiClass> classes = new ArrayList<>();
        private final List<ApiFunction> exportedFunctions = new ArrayList<>();

        public String getModuleName() {
            return moduleName;
        }

        public void setModuleName(String moduleName) {
            this.moduleName = moduleName;
        }

        public String getSourcePath() {
            return sourcePath;
        }

        public void setSourcePath(String sourcePath) {
            this.sourcePath = sourcePath;
        }

        public String getFileDocumentation() {
            return fileDocumentation;
        }

        public void setFileDocumentation(String fileDocumentation) {
            this.fileDocumentation = fileDocumentation;
        }

        public List<ApiClass> getClasses() {
            return classes;
        }

        public List<ApiFunction> getExportedFunctions() {
            return exportedFunctions;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }

    public static final class ApiClass {
        private String name;
        private final List<String> decorators = new ArrayList<>();
        private String documentation;
        private final List<ApiMethod> methods = new ArrayList<>();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<String> getDecorators() {
            return decorators;
        }

        public String getDocumentation() {
            return documentation;
        }

        public void setDocumentation(String documentation) {
            this.documentation = documentation;
        }

        public List<ApiMethod> getMethods() {
            return methods;
        }
    }

    public static final class ApiMethod {
        private String name;
        private final List<String> decorators = new ArrayList<>();
        private final List<String> parameters = new ArrayList<>();
        private String returnType;
        private String documentation;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<String> getDecorators() {
            return decorators;
        }

        public List<String> getParameters() {
            return parameters;
        }

        public String getReturnType() {
            return returnType;
        }

        public void setReturnType(String returnType) {
            this.returnType = returnType;
        }

        public String getDocumentation() {
            return documentation;
        }

        public void setDocumentation(String documentation) {
            this.documentation = documentation;
        }
    }

    public static final class ApiFunction {
        private String name;
        private String signature;
        private String documentation;
        private final List<String> decorators = new ArrayList<>();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSignature() {
            return signature;
        }

        public void setSignature(String signature) {
            this.signature = signature;
        }

        public List<String> getDecorators() {
            return decorators;
        }

        public String getDocumentation() {
            return documentation;
        }

        public void setDocumentation(String documentation) {
            this.documentation = documentation;
        }
    }

    private static class MetadataVisitor extends TypeScriptParserBaseVisitor<ApiFileModel> {

        private final ApiFileModel model = new ApiFileModel();
        private final CommonTokenStream tokens;

        MetadataVisitor(CommonTokenStream tokens) {
            this.tokens = tokens;
        }

        @Override
        public ApiFileModel visitProgram(TypeScriptParser.ProgramContext ctx) {
            super.visitProgram(ctx);
            return model;
        }

        @Override
        public ApiFileModel visitSourceElement(TypeScriptParser.SourceElementContext ctx) {
            if (model.getFileDocumentation() == null && ctx.getStart() != null) {
                String doc = extractLeadingJsDoc(ctx.getStart()
                                                    .getTokenIndex());
                if (doc != null)
                    model.setFileDocumentation(doc);
            }
            return super.visitSourceElement(ctx);
        }

        @Override
        public ApiFileModel visitFunctionDeclaration(TypeScriptParser.FunctionDeclarationContext ctx) {
            boolean exported = ctx.getParent() != null && ctx.getParent()
                                                             .getText()
                                                             .contains("export");
            ApiFunction f = new ApiFunction();
            String name = ctx.getText() != null ? ctx.getText() : "<anonymous>";
            f.setName(name);

            String signature = ctx.getText();
            int paramsStart = signature.indexOf('(');
            if (paramsStart != -1) {
                int paramsEnd = signature.indexOf(')', paramsStart);
                String params = paramsEnd != -1 ? signature.substring(paramsStart, paramsEnd + 1) : signature.substring(paramsStart);
                f.setSignature(name + params);
            } else {
                f.setSignature(signature);
            }

            List<String> decs = extractDecoratorsFromCtx(ctx);
            f.getDecorators()
             .addAll(decs);

            String doc = extractLeadingJsDoc(ctx.getStart()
                                                .getTokenIndex());
            f.setDocumentation(doc);

            if (exported)
                model.getExportedFunctions()
                     .add(f);
            return super.visitFunctionDeclaration(ctx);
        }

        @Override
        public ApiFileModel visitClassDeclaration(TypeScriptParser.ClassDeclarationContext ctx) {
            ApiClass c = new ApiClass();
            if (ctx.identifier() != null && ctx.identifier()
                                               .Identifier() != null) {
                c.setName(ctx.identifier()
                             .Identifier()
                             .getText());
            } else {
                c.setName("<anonymous-class>");
            }

            List<String> classDecorators = extractDecoratorsText(ctx);
            c.getDecorators()
             .addAll(classDecorators);

            String doc = extractLeadingJsDoc(ctx.getStart()
                                                .getTokenIndex());
            if (doc != null)
                c.setDocumentation(doc);

            if (ctx.classTail() != null && ctx.classTail()
                                              .classElement() != null) {
                for (TypeScriptParser.ClassElementContext elt : ctx.classTail()
                                                                   .classElement()) {
                    for (int i = 0; i < elt.getChildCount(); i++) {
                        ParseTree child = elt.getChild(i);
                        if (child instanceof TypeScriptParser.MethodDeclarationExpressionContext) {
                            TypeScriptParser.MethodDeclarationExpressionContext mctx =
                                    (TypeScriptParser.MethodDeclarationExpressionContext) child;
                            ApiMethod m = new ApiMethod();
                            if (mctx.propertyName() != null) {
                                m.setName(mctx.propertyName()
                                              .getText());
                            } else {
                                m.setName("<unknown>");
                            }
                            String mt = mctx.getText();
                            int ps = mt.indexOf('(');
                            int pe = mt.indexOf(')');
                            if (ps != -1 && pe != -1 && pe > ps) {
                                String paramsRaw = mt.substring(ps + 1, pe)
                                                     .trim();
                                if (!paramsRaw.isEmpty()) {
                                    String[] parts = paramsRaw.split(",");
                                    for (String p : parts)
                                        m.getParameters()
                                         .add(p.trim());
                                }
                            }
                            if (mctx.callSignature() != null) {
                                String returnStatement = mctx.callSignature()
                                                             .getText();
                                m.setReturnType(
                                        returnStatement.lastIndexOf(":") > 0 ? returnStatement.substring(returnStatement.lastIndexOf(":"))
                                                : "void");
                            }
                            m.getDecorators()
                             .addAll(extractDecoratorsFromCtx(mctx));
                            String mdoc = extractLeadingJsDoc(mctx.getStart()
                                                                  .getTokenIndex());
                            if (mdoc != null)
                                m.setDocumentation(mdoc);

                            c.getMethods()
                             .add(m);
                        } else if (child instanceof TypeScriptParser.PropertyMemberDeclarationContext) {
                            // optional: skip or add as simple methodless property
                        } else if (child instanceof TypeScriptParser.ConstructSignatureContext) {
                            // skip
                        }
                    }
                }
            }

            model.getClasses()
                 .add(c);
            return super.visitClassDeclaration(ctx);
        }

        /**
         * Extract decorator textual representation attached to a context
         */
        private List<String> extractDecoratorsText(ParserRuleContext ctx) {
            List<String> result = new ArrayList<>();
            try {
                // Many context types have a decoratorList() child - reflectively attempt to obtain it
                for (int i = 0; i < ctx.getChildCount(); i++) {
                    ParseTree child = ctx.getChild(i);
                    if (child instanceof TypeScriptParser.DecoratorListContext) {
                        TypeScriptParser.DecoratorListContext dl = (TypeScriptParser.DecoratorListContext) child;
                        for (TypeScriptParser.DecoratorContext d : dl.decorator()) {
                            result.add(d.getText());
                        }
                    } else if (child instanceof TypeScriptParser.DecoratorContext) {
                        result.add(child.getText());
                    }
                }
            } catch (Exception e) {
                // ignore and return what we have
            }
            return result;
        }

        /**
         * Extract decorators by searching up the tree for decoratorList child.
         */
        private List<String> extractDecoratorsFromCtx(ParserRuleContext ctx) {
            List<String> res = new ArrayList<>();
            ParserRuleContext p = ctx;
            while (p != null) {
                res.addAll(extractDecoratorsText(p));
                p = p.getParent();
            }
            return res;
        }

        /**
         * Heuristic: extracts a JSDoc-style comment immediately preceding the token index. This looks
         * backwards over tokens until it finds a LINE_COMMENT or BLOCK_COMMENT token that looks like JSDoc.
         */
        private String extractLeadingJsDoc(int tokenIndex) {
            try {
                for (int i = tokenIndex - 1; i >= Math.max(0, tokenIndex - 50); i--) {
                    Token t = tokens.get(i);
                    if (t == null)
                        continue;
                    int type = t.getType();
                    String text = t.getText();
                    // In your lexer the comment token type may differ; check for "/*" style
                    if (text != null && (text.startsWith("/**") || text.startsWith("/*"))) {
                        // Clean up comment markers
                        String cleaned = cleanComment(text);
                        return cleaned;
                    }
                    // stop if we find a non-trivia token (e.g. a semicolon or keyword) to avoid scanning too far
                    if (text != null && !text.startsWith("//") && !text.startsWith("/*") && !text.trim()
                                                                                                 .isEmpty()) {
                        // continue scanning a short distance, but don't go too far
                    }
                }
            } catch (Exception e) {
                // ignore
            }
            return null;
        }

        private String cleanComment(String commentText) {
            String s = commentText;
            if (s.startsWith("/**"))
                s = s.substring(3);
            else if (s.startsWith("/*"))
                s = s.substring(2);
            if (s.endsWith("*/"))
                s = s.substring(0, s.length() - 2);
            // remove leading * on lines
            String[] lines = s.split("\\r?\\n");
            StringBuilder sb = new StringBuilder();
            for (String line : lines) {
                String l = line.trim();
                if (l.startsWith("*"))
                    l = l.substring(1)
                         .trim();
                sb.append(l)
                  .append("\n");
            }
            return escapeMd(sb.toString()
                              .trim());
        }
    }

    private static String escapeMd(String text) {
        return text.replace("{", "\\{")
                   .replace("}", "\\}");
    }

    public static void main(String[] args) throws IOException {
        Path src = Paths.get("../../../components/api/api-modules-javascript/src/main/resources/META-INF/dirigible/modules/src");
        Path out = Paths.get("./dist/api");
        TypeScriptApiDocGenerator.generate(src, out);
        MarkdownIndexGenerator.generateMarkdownIndexes(out);
        ApiIndexGenerator.generateApiIndexes(out);
    }
}
