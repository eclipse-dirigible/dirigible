/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
import { BookRepository } from "../gen/ts-books/dao/Books/BookRepository";
import { Controller, Get } from "sdk/http";

@Controller
class BooksReport {

    @Get("/count")
    public count() {
        return { count: new BookRepository().count() };
    }
}
