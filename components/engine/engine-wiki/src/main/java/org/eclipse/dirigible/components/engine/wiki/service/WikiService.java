/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.wiki.service;

import java.io.StringWriter;
import java.nio.file.Paths;
import java.util.Arrays;

import org.eclipse.dirigible.components.registry.accessor.RegistryAccessor;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.mylyn.wikitext.confluence.ConfluenceLanguage;
import org.eclipse.mylyn.wikitext.parser.MarkupParser;
import org.eclipse.mylyn.wikitext.parser.builder.HtmlDocumentBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;

/**
 * The Class WebService.
 */
@Service
public class WikiService {

    /** The Constant FILE_EXTENSION_MD. */
    public static final String FILE_EXTENSION_MD = ".md";

    /** The Constant FILE_EXTENSION_MARKDOWN. */
    public static final String FILE_EXTENSION_MARKDOWN = ".markdown";

    /** The Constant FILE_EXTENSION_CONFLUENCE. */
    public static final String FILE_EXTENSION_CONFLUENCE = ".confluence";

    /** The Constant FILE_EXTENSION_HTML. */
    public static final String FILE_EXTENSION_HTML = ".html";

    /** The registry accessor. */
    @Autowired
    private RegistryAccessor registryAccessor;

    /**
     * Exist resource.
     *
     * @param path the requested resource location
     * @return if the {@link IResource}
     */
    public boolean existResource(String path) {
        return registryAccessor.existResource(path);
    }

    /**
     * Gets the resource.
     *
     * @param path the requested resource location
     * @return the {@link IResource} instance
     */
    public IResource getResource(String path) {
        return registryAccessor.getResource(path);
    }

    /**
     * Gets the resource content.
     *
     * @param path the requested resource location
     * @return the {@link IResource} content as a byte array
     */
    public byte[] getResourceContent(String path) {
        return registryAccessor.getRegistryContent(path);
    }

    /**
     * Render content.
     *
     * @param path the path
     * @param content the content
     * @return the string
     */
    public String renderContent(String path, String content) {
        if (path.endsWith(FILE_EXTENSION_MD) || path.endsWith(FILE_EXTENSION_MARKDOWN)) {
            return renderMarkdown(content);
        } else if (path.endsWith(FILE_EXTENSION_CONFLUENCE)) {
            return renderConfluence(content);
        }
        return "File extension is uknown for Wiki engine: " + path;
    }

    /**
     * Render and store content.
     *
     * @param path the path
     * @param content the content
     */
    public void generateContent(String path, String content) {
        String html = renderContent(path, content);
        storeGenerated(path, html);
    }

    /**
     * Render markdown.
     *
     * @param content the content
     * @return the string
     */
    private String renderMarkdown(String content) {
        MutableDataSet options = new MutableDataSet();

        // uncomment to set optional extensions
        options.set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create(), StrikethroughExtension.create()));

        // uncomment to convert soft-breaks to hard breaks
        options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");

        Parser parser = Parser.builder(options)
                              .build();
        HtmlRenderer renderer = HtmlRenderer.builder(options)
                                            .build();

        // You can re-use parser and renderer instances
        Node document = parser.parse(content);
        String html = renderer.render(document);
        return html;
    }

    /**
     * Render confluence.
     *
     * @param content the content
     * @return the string
     */
    private String renderConfluence(String content) {
        StringWriter writer = new StringWriter();
        HtmlDocumentBuilder builder = new HtmlDocumentBuilder(writer);
        builder.setEmitAsDocument(false);
        MarkupParser markupParser = new MarkupParser();
        markupParser.setBuilder(builder);
        markupParser.setMarkupLanguage(new ConfluenceLanguage());
        markupParser.parse(content);
        String htmlContent = writer.toString();
        return htmlContent;
    }

    /**
     * Store generated.
     *
     * @param path the path
     * @param html the html
     */
    private void storeGenerated(String path, String html) {
        String target = generatePath(path);
        registryAccessor.getRepository()
                        .createResource(target, html.getBytes());
    }

    /**
     * Generate path.
     *
     * @param path the path
     * @return the string
     */
    private String generatePath(String path) {
        String target = path;
        if (target.endsWith(FILE_EXTENSION_MD)) {
            target = target.substring(0, target.length() - FILE_EXTENSION_MD.length());
            target += FILE_EXTENSION_HTML;
        } else if (target.endsWith(FILE_EXTENSION_MARKDOWN)) {
            target = target.substring(0, target.length() - FILE_EXTENSION_MARKDOWN.length());
            target += FILE_EXTENSION_HTML;
        } else if (target.endsWith(FILE_EXTENSION_CONFLUENCE)) {
            target = target.substring(0, target.length() - FILE_EXTENSION_CONFLUENCE.length());
            target += FILE_EXTENSION_HTML;
        }
        return IRepositoryStructure.PATH_REGISTRY_PUBLIC + target;
    }

    /**
     * Removes the generated.
     *
     * @param path the path
     */
    public void removeGenerated(String path) {
        String target = generatePath(path);
        IResource resource = registryAccessor.getRepository()
                                             .getResource(target);
        if (resource.exists()) {
            resource.delete();
        }
    }

}
