/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.openapi.generator;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Parses TypeScript Controller source files and generates an OpenAPI (Swagger) specification JSON.
 */
public class OpenApiSerializer {



    public static String serialize(Map<String, Object> metadata) {
        StringBuilder json = new StringBuilder();
        String controllerName = (String) metadata.get("name");
        List<Map<String, Object>> routes = (List<Map<String, Object>>) metadata.get("routes");

        json.append("{\n");
        json.append("  \"openapi\": \"3.0.0\",\n");
        json.append("  \"info\": {\n");
        json.append("    \"title\": \"")
            .append(controllerName)
            .append(" API\",\n");
        json.append("    \"version\": \"1.0.0\"\n");
        json.append("  },\n");
        json.append("  \"servers\": [\n");
        json.append("    { \"url\": \"/services/ts\" }\n");
        json.append("  ],\n");
        json.append("  \"paths\": {\n");

        Map<String, List<Map<String, Object>>> paths = routes.stream()
                                                             .collect(Collectors.groupingBy(route -> (String) route.get("path")));

        boolean firstPath = true;
        for (Map.Entry<String, List<Map<String, Object>>> entry : paths.entrySet()) {
            if (!firstPath)
                json.append(",\n");
            firstPath = false;

            json.append("    \"")
                .append(entry.getKey())
                .append("\": {\n");

            boolean firstMethod = true;
            for (Map<String, Object> route : entry.getValue()) {
                if (!firstMethod)
                    json.append(",\n");
                firstMethod = false;

                json.append(serializeOperation(route));
            }
            json.append("\n    }");
        }

        json.append("\n  },\n");

        json.append("  \"components\": {\n");
        json.append("    \"schemas\": ");
        json.append(serializeSchemas(OpenApiParser.SCHEMAS));
        json.append("\n  }\n");

        json.append("}\n");

        return json.toString();
    }

    private static String serializeOperation(Map<String, Object> route) {
        StringBuilder json = new StringBuilder();
        String method = (String) route.get("httpMethod");
        String opId = (String) route.get("methodName");
        List<String> tags = (List<String>) route.get("tags");
        String responseType = (String) route.get("responseType");
        String requestBodyRef = (String) route.get("requestBodyRef");
        String documentation = (String) route.get("description");

        json.append("      \"")
            .append(method)
            .append("\": {\n");
        json.append("        \"tags\": [\"")
            .append(tags.get(0))
            .append("\"],\n");
        json.append("        \"operationId\": \"")
            .append(opId)
            .append("\",\n");
        json.append("        \"summary\": \"")
            .append(opId)
            .append(" ")
            .append(tags.get(0))
            .append(" " + documentation + "\",\n");

        // Parameters
        json.append("        \"parameters\": ");
        json.append(serializeParameters((List<Map<String, Object>>) route.get("parameters")))
            .append(",\n");

        // Request Body (for POST/PUT)
        if (requestBodyRef != null) {
            json.append("        \"requestBody\": {\n");
            json.append("          \"required\": true,\n");
            json.append("          \"content\": {\n");
            json.append("            \"application/json\": {\n");
            json.append("              \"schema\": { \"$ref\": \"#/components/schemas/")
                .append(requestBodyRef)
                .append("\" }\n");
            json.append("            }\n");
            json.append("          }\n");
            json.append("        },\n");
        }

        // Responses
        json.append("        \"responses\": {\n");
        json.append("          \"200\": {\n");
        json.append("            \"description\": \"Success\",\n");
        json.append("            \"content\": {\n");
        json.append("              \"application/json\": {\n");

        String schema = serializeResponseSchema(responseType);

        if ("void".equals(schema)) {
            json.append("                \"schema\": {}");
        } else {
            json.append("                \"schema\": ")
                .append(schema);
        }

        json.append("\n                }\n");
        json.append("              }\n");
        json.append("            }\n");
        json.append("          }\n");
        json.append("        }");

        return json.toString();
    }

    private static String serializeParameters(List<Map<String, Object>> parameters) {
        StringBuilder json = new StringBuilder();
        json.append("[");
        boolean firstParam = true;
        for (Map<String, Object> param : parameters) {
            if (!firstParam)
                json.append(",\n");
            firstParam = false;

            json.append("  {");

            boolean firstKey = true;
            for (Map.Entry<String, Object> entry : param.entrySet()) {
                if (!firstKey)
                    json.append(", ");
                firstKey = false;

                json.append("\"")
                    .append(entry.getKey())
                    .append("\": ");

                json.append(serializeValue(entry.getValue()));
            }
            json.append("}");
        }
        json.append("]");
        return json.toString();
    }

    private static String serializeValue(Object value) {
        if (value instanceof String) {
            return "\"" + value + "\"";
        } else if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof Map) {
            // Recurse for nested map structures like 'schema'
            return serializeMap((Map<?, ?>) value);
        } else if (value instanceof List) {
            return "[" + ((List<?>) value).stream()
                                          .map(OpenApiSerializer::serializeValue)
                                          .collect(Collectors.joining(", "))
                    + "]";
        }
        return "null";
    }

    private static String serializeResponseSchema(String responseType) {
        if (responseType.endsWith("[]")) {
            String baseType = responseType.substring(0, responseType.length() - 2);
            return String.format("{ \"type\": \"array\", \"items\": { \"$ref\": \"#/components/schemas/%s\" } }", baseType);
        }
        if (OpenApiParser.SCHEMAS.containsKey(responseType)) {
            return serializeMap(OpenApiParser.SCHEMAS.get(responseType));
        }
        if ("void".equals(responseType)) {
            return responseType;
        }
        return String.format("{ \"$ref\": \"#/components/schemas/%s\" }", responseType);
    }

    private static String serializeSchemas(Map<String, Map<String, Object>> schemas) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        boolean firstSchema = true;
        for (Map.Entry<String, Map<String, Object>> entry : schemas.entrySet()) {
            if (!firstSchema)
                json.append(",\n");
            firstSchema = false;
            json.append("      \"")
                .append(entry.getKey())
                .append("\": ");
            json.append(serializeMap(entry.getValue()));
        }
        json.append("\n    }");
        return json.toString();
    }

    private static String serializeMap(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first)
                sb.append(", ");
            first = false;
            sb.append("\"")
              .append(entry.getKey())
              .append("\": ");

            sb.append(serializeValue(entry.getValue()));
        }
        sb.append("}");
        return sb.toString();
    }

}
