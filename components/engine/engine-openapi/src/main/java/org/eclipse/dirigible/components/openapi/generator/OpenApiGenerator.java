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

import java.util.Map;

/**
 * Parses TypeScript Controller source files and generates an OpenAPI (Swagger) specification JSON.
 */
public class OpenApiGenerator {

    /**
     * Entry point for generating the OpenAPI specification.
     *
     * @param location The actual source location
     * @param source The TypeScript source code string of the controller.
     * @return The OpenAPI specification as a formatted JSON string.
     */
    public static String generate(String location, String source) {
        Map<String, Object> controllerMetadata = OpenApiParser.parse(location, source);
        return OpenApiSerializer.serialize(controllerMetadata);
    }

    /**
     * Mock main method to demonstrate the generator's output, including the extracted imports.
     */
    public static void main(String[] args) {
        String source = """
                import { Controller, Get, Post, Put, Delete } from "@dirigible/core";
                import { CountryEntity } from "../../data/Settings/CountryEntity";
                import { FilterModel as F } from "../data/models/FilterModel";

                @Controller
                class CountryController {

                    @Get("/")
                    public getAll(_: any, ctx: any): CountryEntity[] {
                //...
                    }

                    @Post("/")
                    public create(entity: CountryEntity): CountryEntity {
                //...
                    }

                    @Get("/count")
                    public count(): number {
                //...
                    }

                    @Post("/search")
                    public search(filter: F) {
                //...
                    }

                    @Get("/:id")
                    public getById(id: number, ctx: any): CountryEntity[] {
                //...
                    }

                    @Put("/:id")
                    public update(entity: CountryEntity, ctx: any): CountryEntity {
                //...
                    }

                    @Delete("/:id")
                    public deleteById(_: any, ctx: any): void {
                //...
                    }
                }
                """;

        // Output the generated OpenAPI JSON to the console (or file)
        // The generate method now prints the extracted imports before the JSON.
        System.out.println(
                new OpenApiGenerator().generate("/codbex-countries/gen/codbex-countries/api/Settings/CountryController.ts", source));
    }
}
