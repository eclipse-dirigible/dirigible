/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
/**
 * Helpers for the Projects view "New -> Java" submenu: name parsing/validation and strong-interface
 * skeleton generation. Pure functions, no AngularJS or DOM dependency, so they are trivially unit
 * testable and keep all Java-specific knowledge out of projects.js. Exposed as the global JavaNew.
 */
const JavaNew = (function () {

    const IDENTIFIER = /^[A-Za-z_$][A-Za-z0-9_$]*$/;

    // Java reserved words (and literals) that cannot be used as identifiers.
    const RESERVED = new Set([
        'abstract', 'assert', 'boolean', 'break', 'byte', 'case', 'catch', 'char', 'class', 'const',
        'continue', 'default', 'do', 'double', 'else', 'enum', 'extends', 'final', 'finally', 'float',
        'for', 'goto', 'if', 'implements', 'import', 'instanceof', 'int', 'interface', 'long', 'native',
        'new', 'package', 'private', 'protected', 'public', 'return', 'short', 'static', 'strictfp',
        'super', 'switch', 'synchronized', 'this', 'throw', 'throws', 'transient', 'try', 'void',
        'volatile', 'while', 'true', 'false', 'null', '_',
    ]);

    /** Menu descriptors, in display order. Ids are namespaced so projects.js can route them. */
    const menuItems = [
        { id: 'java:package', kind: 'package', label: 'Package', leftIconClass: 'sap-icon--folder-blank' },
        { id: 'java:class', kind: 'class', label: 'Class', leftIconClass: 'sap-icon--syntax', separator: true },
        { id: 'java:interface', kind: 'interface', label: 'Interface', leftIconClass: 'sap-icon--syntax' },
        { id: 'java:enum', kind: 'enum', label: 'Enum', leftIconClass: 'sap-icon--syntax' },
        { id: 'java:annotation', kind: 'annotation', label: 'Annotation', leftIconClass: 'sap-icon--syntax' },
        { id: 'java:record', kind: 'record', label: 'Record', leftIconClass: 'sap-icon--syntax' },
        { id: 'java:exception', kind: 'exception', label: 'Exception', leftIconClass: 'sap-icon--syntax' },
        { id: 'java:controller', kind: 'controller', label: 'Controller', leftIconClass: 'sap-icon--source-code', separator: true },
        { id: 'java:job', kind: 'job', label: 'Job', leftIconClass: 'sap-icon--source-code' },
        { id: 'java:listener', kind: 'listener', label: 'Listener', leftIconClass: 'sap-icon--source-code' },
        { id: 'java:websocket', kind: 'websocket', label: 'WebSocket', leftIconClass: 'sap-icon--source-code' },
        { id: 'java:repository', kind: 'repository', label: 'Repository', leftIconClass: 'sap-icon--source-code' },
    ];

    function isValidIdentifier(segment) {
        return IDENTIFIER.test(segment) && !RESERVED.has(segment);
    }

    /** The context-menu submenu group object consumed by projects.js. */
    function submenu() {
        return {
            id: 'java',
            label: 'Java',
            iconClass: 'sap-icon--syntax',
            items: menuItems.map((i) => ({ ...i })),
        };
    }

    function isJavaMenuId(id) {
        return typeof id === 'string' && id.startsWith('java:');
    }

    function itemById(id) {
        return menuItems.find((i) => i.id === id);
    }

    function placeholderFor(kind) {
        if (kind === 'package') return 'com.example.app';
        if (kind === 'controller') return 'com.example.MyController';
        if (kind === 'repository') return 'com.example.CountryRepository';
        return 'com.example.MyType';
    }

    /**
     * Parses a package name like {@code com.example.app}.
     * @returns {{ok: boolean, error?: string, segments?: string[]}}
     */
    function parsePackage(input) {
        const value = (input || '').trim();
        if (!value) return { ok: false, error: 'Provide a package name, e.g. com.example.app' };
        const segments = value.split('.');
        for (const segment of segments) {
            if (!isValidIdentifier(segment)) {
                return { ok: false, error: `'${segment}' is not a valid Java package segment` };
            }
        }
        return { ok: true, segments };
    }

    /**
     * Parses a (possibly qualified) type name like {@code com.example.MyClass} or {@code MyClass}.
     * @returns {{ok: boolean, error?: string, packageSegments?: string[], typeName?: string}}
     */
    function parseType(input) {
        const value = (input || '').trim();
        if (!value) return { ok: false, error: 'Provide a type name, e.g. MyClass or com.example.MyClass' };
        const segments = value.split('.');
        const typeName = segments.pop();
        if (!isValidIdentifier(typeName)) {
            return { ok: false, error: `'${typeName}' is not a valid Java type name` };
        }
        for (const segment of segments) {
            if (!isValidIdentifier(segment)) {
                return { ok: false, error: `'${segment}' is not a valid Java package segment` };
            }
        }
        return { ok: true, packageSegments: segments, typeName };
    }

    function header(packageName) {
        return packageName ? `package ${packageName};\n\n` : '';
    }

    function body(kind, name, entity) {
        switch (kind) {
            case 'class':
                return `public class ${name} {\n\n}\n`;
            case 'interface':
                return `public interface ${name} {\n\n}\n`;
            case 'enum':
                return `public enum ${name} {\n\n}\n`;
            case 'annotation':
                return `public @interface ${name} {\n\n}\n`;
            case 'record':
                return `public record ${name}() {\n}\n`;
            case 'exception':
                return `public class ${name} extends Exception {\n\n`
                    + `    public ${name}(String message) {\n`
                    + `        super(message);\n`
                    + `    }\n\n`
                    + `    public ${name}(String message, Throwable cause) {\n`
                    + `        super(message, cause);\n`
                    + `    }\n}\n`;
            case 'controller':
                return `import org.eclipse.dirigible.sdk.http.Controller;\n`
                    + `import org.eclipse.dirigible.sdk.http.Get;\n\n`
                    + `@Controller\n`
                    + `public class ${name} {\n\n`
                    + `    @Get("/hello")\n`
                    + `    public String hello() {\n`
                    + `        return "Hello from ${name}";\n`
                    + `    }\n}\n`;
            case 'job':
                return `import org.eclipse.dirigible.sdk.component.Component;\n`
                    + `import org.eclipse.dirigible.sdk.job.JobHandler;\n\n`
                    + `@Component\n`
                    + `public class ${name} implements JobHandler {\n\n`
                    + `    @Override\n`
                    + `    public String cron() {\n`
                    + `        return "0/30 * * * * ?";\n`
                    + `    }\n\n`
                    + `    @Override\n`
                    + `    public void run() {\n`
                    + `        // TODO implement the scheduled work\n`
                    + `    }\n}\n`;
            case 'listener':
                return `import org.eclipse.dirigible.sdk.component.Component;\n`
                    + `import org.eclipse.dirigible.sdk.messaging.ListenerKind;\n`
                    + `import org.eclipse.dirigible.sdk.messaging.MessageHandler;\n\n`
                    + `@Component\n`
                    + `public class ${name} implements MessageHandler {\n\n`
                    + `    @Override\n`
                    + `    public String destination() {\n`
                    + `        return "${queueName(name)}";\n`
                    + `    }\n\n`
                    + `    @Override\n`
                    + `    public ListenerKind kind() {\n`
                    + `        return ListenerKind.QUEUE;\n`
                    + `    }\n\n`
                    + `    @Override\n`
                    + `    public void onMessage(String message) {\n`
                    + `        // TODO handle the message\n`
                    + `    }\n}\n`;
            case 'websocket':
                return `import org.eclipse.dirigible.sdk.component.Component;\n`
                    + `import org.eclipse.dirigible.sdk.net.WebsocketHandler;\n\n`
                    + `@Component\n`
                    + `public class ${name} implements WebsocketHandler {\n\n`
                    + `    @Override\n`
                    + `    public String endpoint() {\n`
                    + `        return "${endpointName(name)}";\n`
                    + `    }\n\n`
                    + `    @Override\n`
                    + `    public void onMessage(String message, String from) {\n`
                    + `        // TODO handle the inbound frame\n`
                    + `    }\n}\n`;
            case 'repository': {
                // The managed entity is supplied by the developer; a fully-qualified name adds an import.
                const provided = (entity || '').trim() || 'Entity';
                const dot = provided.lastIndexOf('.');
                const entitySimple = dot >= 0 ? provided.substring(dot + 1) : provided;
                const entityImport = dot >= 0 ? `import ${provided};\n` : '';
                return entityImport + `import org.eclipse.dirigible.components.data.store.java.repository.JavaRepository;\n`
                    + `import org.eclipse.dirigible.sdk.component.Repository;\n\n`
                    + `@Repository\n`
                    + `public class ${name} extends JavaRepository<${entitySimple}> {\n\n`
                    + `    public ${name}() {\n`
                    + `        super(${entitySimple}.class);\n`
                    + `    }\n}\n`;
            }
            default:
                return `public class ${name} {\n\n}\n`;
        }
    }

    /** Derives a sensible destination name from a listener class name. */
    function queueName(name) {
        return name.replace(/Listener$/, '').toLowerCase() || 'queue';
    }

    /** Derives a sensible endpoint suffix from a websocket handler class name. */
    function endpointName(name) {
        return name.replace(/(Socket|Handler|Websocket)$/i, '').toLowerCase() || 'endpoint';
    }

    /**
     * Generates the skeleton source for the given kind. {@code entity} is only used for the repository
     * kind, where it is the developer-supplied entity type the repository manages.
     * @returns {string}
     */
    function generate(kind, packageName, typeName, entity) {
        return header(packageName) + body(kind, typeName, entity);
    }

    return {
        menuItems,
        submenu,
        isJavaMenuId,
        itemById,
        placeholderFor,
        parsePackage,
        parseType,
        generate,
        isValidIdentifier,
    };
})();
