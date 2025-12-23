/**
 * * ECMAScript 2025-compliant ORM decorator implementation
 * Compatible with GraalJS runtime.
 * * Features:
 * - Uses context.addInitializer for stable decorator timing
 * - Stores metadata in a global WeakMap cache
 * - Finalizes entity metadata once per class
 * - Defers finalization via microtask (Promise.resolve().then)
 */

// --- Decorator Context Types (based on ECMAScript Decorators proposal) ---
type ClassFieldDecoratorContext = {
  kind: "field";
  name: string | symbol;
  static: boolean;
  private: boolean;
  addInitializer(fn: () => void): void;
};

type ClassDecoratorContext = {
  kind: "class";
  name?: string | symbol;
  addInitializer(fn: () => void): void;
};

export type ColumnTypes =
  // Numeric types
  | 'integer' | 'long' | 'short' | 'byte' | 'float' | 'double' | 'big_integer' | 'big_decimal'
  // String types
  | 'string' | 'char' | 'text' | 'nstring' | 'ntext'
  // Date/Time types
  | 'date' | 'time' | 'timestamp' | 'calendar' | 'calendar_date' | 'instant'
  // Boolean types
  | 'boolean' | 'true_false' | 'yes_no' | 'numeric_boolean' 
  // Binary types
  | 'binary' | 'blob' | 'clob' | 'materialized_blob' | 'materialized_clob'
  // Other types
  | 'serializable' | 'any' | 'object' | 'uuid-char' | 'uuid-binary' | 'json' | 'jsonb' | 'xml';

// --- Metadata Models ---
export interface ColumnOptions {
  name?: string;
  type?: ColumnTypes | (string & {});
  length?: number;
  nullable?: boolean;
  defaultValue?: string;
  precision?: number;
  scale?: number;
}

export interface OneToManyOptions {
  table?: string;
  joinColumn: string;
  cascade?: "all" | "none" | "persist" | "merge" | "remove";
  inverse?: boolean;
  lazy?: boolean;
  fetch?: "select" | "join";
  joinColumnNotNull?: boolean;
}

export interface ManyToOneOptions {
  joinColumn?: string;
  cascade?: "all" | "none" | "persist" | "merge" | "remove";
  nullable?: boolean;
  lazy?: boolean;
  fetch?: "select" | "join";
}

interface PropertyMetadata {
  propertyName: string;
  isId: boolean;
  isGenerated: boolean;
  documentation?: string;
  columnOptions?: ColumnOptions;
  oneToManyOptions?: { type: Function; options: OneToManyOptions };
  manyToOneOptions?: { type: Function; options: ManyToOneOptions };
}

export interface EntityConstructor extends Function {
  new(...args: any[]): any;
  $entity_name: string;
  $table_name: string;
  $id_name: string;
  $id_column: string;
  $initialized?: boolean;
  $documentation?: string;
}

// --- Global Metadata Cache ---
const globalCache: WeakMap<Function, PropertyMetadata[]> =
  (globalThis as any).__decorator_metadata_cache__ ||
  ((globalThis as any).__decorator_metadata_cache__ = new WeakMap());

function getMetadataArray(constructor: Function): PropertyMetadata[] {
  if (!globalCache.has(constructor)) {
    globalCache.set(constructor, []);
  }
  return globalCache.get(constructor)!;
}

// --- Defer Helper (GraalJS-safe microtask) ---
function defer(fn: () => void): void {
  // Uses Promise microtask queue for deferred execution
  Promise.resolve().then(fn);
}

// --- Core Property Decorator Factory ---
function createPropertyDecorator(
  kind: "column" | "id" | "generated",
  options?: ColumnOptions
) {
  return function (_: any, context: ClassFieldDecoratorContext) {
    if (context.kind !== "field") {
      throw new Error(`@${kind} must apply to fields`);
    }

    context.addInitializer(function () {
      const ctor = (this as any).constructor as EntityConstructor;
      const propertyName = context.name.toString();
      const metadataArray = getMetadataArray(ctor);

      let metadata = metadataArray.find(
        (m) => m.propertyName === propertyName
      );

      if (!metadata) {
        metadata = {
          propertyName: propertyName,
          isId: false,
          isGenerated: false,
        };
        metadataArray.push(metadata);
      }

      if (kind === "id") metadata.isId = true;
      if (kind === "generated") metadata.isGenerated = true;
      if (kind === "column") metadata.columnOptions = options;
    });
  };
}

// --- @Documentation Decorator (Dual-Purpose) ---
/**
 * Adds documentation metadata to a class or a field.
 */
export function Documentation(description: string) {
  return function (
    value: Function | any,
    context: ClassDecoratorContext | ClassFieldDecoratorContext
  ) {
    if (context.kind === "class") {
      context.addInitializer(function () {
        (value as EntityConstructor).$documentation = description;
      });
      return value;
    } else if (context.kind === "field") {
      context.addInitializer(function () {
        const ctor = (this as any).constructor as EntityConstructor;
        const propertyName = context.name.toString();
        const metadataArray = getMetadataArray(ctor);

        let metadata = metadataArray.find(
          (m) => m.propertyName === propertyName
        );

        if (!metadata) {
          metadata = {
            propertyName: propertyName,
            isId: false,
            isGenerated: false,
          };
          metadataArray.push(metadata);
        }

        metadata.documentation = description;
      });
    }
  };
}


// --- @Entity Decorator ---
/**
 * Marks a class as an entity and initiates metadata finalization.
 * @param entityName The name of the entity (defaults to class name).
 */
export function Entity(entityName?: string) {
  return function (value: Function, context: ClassDecoratorContext) {
    context.addInitializer(function () {
      // Defer execution to ensure all field decorators have run
      defer(() => {
        const ctor = value as EntityConstructor;

        // Prevent duplicate registration (idempotency)
        if (ctor.$initialized) return;
        ctor.$initialized = true;

        ctor.$entity_name = entityName || ctor.name;

        const metadataArray = getMetadataArray(ctor);
        const idMetadata = metadataArray.find((m) => m.isId);

        if (idMetadata) {
          ctor.$id_name = idMetadata.propertyName;
          // Determine ID column name: use explicit name or convert property name to upper case
          ctor.$id_column =
            idMetadata.columnOptions?.name ||
            idMetadata.propertyName.toUpperCase();
        }

        // Future: Logic to map all properties to columns/relations goes here
      });
    });
  };
}

// --- @Table Decorator ---
/**
 * Specifies the database table name for the entity.
 * @param tableName The table name (defaults to uppercase class name).
 */
export function Table(tableName?: string) {
  return function <T extends EntityConstructor>(
    value: T,
    context: ClassDecoratorContext
  ) {
    context.addInitializer(function () {
      (value as EntityConstructor).$table_name =
        tableName || (context.name?.toString() ?? value.name.toUpperCase());
    });

    return value;
  };
}

// --- Exported Property Decorators ---
/**
 * Marks a property as a standard database column.
 */
export const Column = (options?: ColumnOptions) =>
  createPropertyDecorator("column", options);

/**
 * Marks a property as the entity's primary key.
 */
export const Id = () => createPropertyDecorator("id");

/**
 * Marks a property as a generated value (e.g., auto-increment).
 * @param strategy The generation strategy (e.g., "IDENTITY"). Parameter is currently unused in logic.
 */
export const Generated = (strategy: string) =>
  createPropertyDecorator("generated");

/**
 * Defines a one-to-many relationship.
 */
export function OneToMany(
  typeFunction: () => Function,
  options: OneToManyOptions
) {
  return function (_: any, context: ClassFieldDecoratorContext) {
    if (context.kind !== "field") return;

    context.addInitializer(function () {
      const ctor = (this as any).constructor as EntityConstructor;
      const propertyName = context.name.toString();
      const metadataArray = getMetadataArray(ctor);

      let metadata = metadataArray.find(
        (m) => m.propertyName === propertyName
      );

      if (!metadata) {
        metadata = {
          propertyName: propertyName,
          isId: false,
          isGenerated: false,
        };
        metadataArray.push(metadata);
      }

      metadata.oneToManyOptions = { type: typeFunction(), options };
    });
  };
}

/**
 * Defines a many-to-one relationship.
 */
export function ManyToOne(
  typeFunction: () => Function,
  options: ManyToOneOptions = {}
) {
  return function (_: any, context: ClassFieldDecoratorContext) {
    if (context.kind !== "field") return;

    context.addInitializer(function () {
      const ctor = (this as any).constructor as EntityConstructor;
      const propertyName = context.name.toString();
      const metadataArray = getMetadataArray(ctor);

      let metadata = metadataArray.find(
        (m) => m.propertyName === propertyName
      );

      if (!metadata) {
        metadata = {
          propertyName: propertyName,
          isId: false,
          isGenerated: false,
        };
        metadataArray.push(metadata);
      }

      metadata.manyToOneOptions = { type: typeFunction(), options };
    });
  };
}