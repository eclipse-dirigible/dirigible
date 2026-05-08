# @aerokit/sdk

[![npm version](https://badge.fury.io/js/%40aerokit%2Fsdk.svg)](https://www.npmjs.com/package/@aerokit/sdk)
[![License](https://img.shields.io/badge/License-EPL--2.0-blue.svg)](https://opensource.org/licenses/EPL-2.0)

> **Accelerate your application and integration development with a unified TypeScript SDK for modern cloud platforms.**

---

## Overview

`@aerokit/sdk` provides a **modular**, **server-side TypeScript** runtime API designed for developing lightweight
applications, automation scripts, and extensions across cloud or on-premise environments.

---

## Core Features

- **TypeScript-first API** – Rich type definitions and IDE autocompletion for productive, safe scripting.
- **Unified platform model** – Common abstractions for HTTP, I/O, Database, Filesystem, Security, Jobs, and Messaging.
- **Built-in decorators** – Simplify development with `@Controller`, `@Entity`, `@Scheduled`, `@Component`, `@Injected`,
  and more.
- **Seamless dependency injection** – Lightweight IoC mechanism for connecting modular business logic.
- **Pluggable persistence** – Work with SQL and NoSQL through the `sdk/sdk/db` API.
- **Enterprise-ready HTTP layer** – Build APIs and web services with `sdk/sdk/http`.
- **Background jobs and listeners** – Schedule recurring tasks using `sdk/sdk/job` or event-driven listeners with
  `sdk/component`.
- **Extensible runtime** – Deploy, extend, and run modules dynamically without redeployment.

---

## Example

### `CountryEntity.ts`

```typescript
import { Entity, Table, Id, Generated, Column, Documentation } from "@aerokit/sdk/db";

@Entity("CountryEntity")
@Table("SAMPLE_COUNTRY")
@Documentation("Sample Country Entity")
export class CountryEntity {

    @Id()
    @Generated("sequence")
    @Column({ name: "COUNTRY_ID", type: "long" })
    @Documentation("My Id")
    public Id?: number;

    @Column({ name: "COUNTRY_NAME", type: "string" })
    @Documentation("My Name")
    public Name?: string;

    @Column({ name: "COUNTRY_CODE2", type: "string" })
    public Code2?: string;

    @Column({ name: "COUNTRY_CODE3", type: "string" })
    public Code3?: string;

    @Column({ name: "COUNTRY_NUMERIC", type: "string" })
    public Numeric?: string;

}
```

### `CountryRepository.ts`

```typescript
import { Repository, EntityConstructor } from "@aerokit/sdk/db";
import { Component } from "@aerokit/sdk/component";
import { CountryEntity } from "./CountryEntity";

@Component('CountryRepository')
export class CountryRepository extends Repository<CountryEntity> {

    constructor() {
        super((CountryEntity as EntityConstructor));
    }

}

CountryRepository;
```

### `CountryController.ts`

```typescript
import { Controller, Get, Documentation } from "@aerokit/sdk/http"
import { HttpUtils } from "@aerokit/sdk/http/utils";
import { Options } from "@aerokit/sdk/db";
import { Injected, Inject } from "@aerokit/sdk/component";
import { CountryEntity } from "./CountryEntity";
import { CountryRepository } from "./CountryRepository";

@Controller
@Documentation("Sample Country Controller")
@Injected()
class CountryController {

    @Inject('CountryRepository')
    private readonly repository!: CountryRepository;

    @Get("/")
    @Documentation("Sample Get All Countries")
    public getAll(): CountryEntity[] {
        try {
            const options: Options = {limit: 20, offset: 0};
            return this.repository.findAll(options);
        } catch (error: any) {
            HttpUtils.sendInternalServerError(error.message);
        }
        return [];
    }

}
```

## SDK Modules

| Module                      | Description                                                                   |
|-----------------------------|-------------------------------------------------------------------------------|
| `@aerokit/sdk/bpm`          | Business process management and workflow automation APIs.                     |
| `@aerokit/sdk/cache`        | In-memory and distributed caching APIs for data reuse and performance.        |
| `@aerokit/sdk/cms`          | Content management utilities and repository access.                           |
| `@aerokit/sdk/component`    | Dependency injection and modular service registration.                        |
| `@aerokit/sdk/core`         | Core runtime utilities such as environment, configuration, and context.       |
| `@aerokit/sdk/db`           | Database access with SQL and NoSQL support, entity mapping, and transactions. |
| `@aerokit/sdk/extensions`   | Mechanisms for runtime extensions and dynamic plugin registration.            |
| `@aerokit/sdk/git`          | Git integration for repository operations (clone, commit, push, etc.).        |
| `@aerokit/sdk/http`         | HTTP server framework for RESTful APIs and web controllers.                   |
| `@aerokit/sdk/indexing`     | Indexing and search API for full-text and structured content.                 |
| `@aerokit/sdk/integrations` | Integration adapters for connecting to external enterprise systems.           |
| `@aerokit/sdk/io`           | File, stream, and storage I/O operations for local or remote sources.         |
| `@aerokit/sdk/job`          | Scheduling and background task execution with decorators.                     |
| `@aerokit/sdk/junit`        | Lightweight testing and runtime validation utilities.                         |
| `@aerokit/sdk/log`          | Centralized logging and monitoring interfaces.                                |
| `@aerokit/sdk/mail`         | Email composition, sending, and notification services.                        |
| `@aerokit/sdk/messaging`    | Messaging layer with queue, topic, and event APIs.                            |
| `@aerokit/sdk/net`          | Networking utilities such as WebSocket, REST, and HTTP client operations.     |
| `@aerokit/sdk/pdf`          | PDF generation and manipulation tools.                                        |
| `@aerokit/sdk/platform`     | Platform lifecycle management, module metadata, and runtime services.         |
| `@aerokit/sdk/security`     | Authentication, authorization, and user context management.                   |
| `@aerokit/sdk/template`     | Templating system for dynamic document and content rendering.                 |
| `@aerokit/sdk/utils`        | Generic utilities for string, date, and object manipulation.                  |

---

## Use Cases

- API-first business logic for internal or customer apps.
- Lightweight integrations between enterprise systems.
- Automation scripts (scheduled or event-driven).
- Custom platform extensions in modular runtimes.

## Philosophy

`@aerokit` follows a code-as-configuration philosophy — instead of XML or JSON descriptors, you define services,
entities, and jobs directly in TypeScript using decorators.
This makes your code self-documenting, testable, and portable across environments.

## Getting Started

```npm install @aerokit/sdk```

Then import only what you need:

```javascript
import { Request, Response } from "@aerokit/sdk/http";
import { Entity, Column } from "@aerokit/sdk/db";
```
