package org.eclipse.dirigible.components.data.store.hbm;

import org.eclipse.dirigible.components.data.store.hbm.HbmXmlDescriptor.HbmIdDescriptor;
import org.eclipse.dirigible.components.data.store.model.EntityFieldMetadata;
import org.eclipse.dirigible.components.data.store.model.EntityFieldMetadata.ColumnDetails;
import org.eclipse.dirigible.components.data.store.model.EntityMetadata;

public class EntityToHbmMapper {

    /**
     * Converts EntityMetadata (typically parsed from TypeScript) into an HbmXmlDescriptor.
     */
    public static HbmXmlDescriptor map(EntityMetadata entityMetadata) {

        EntityFieldMetadata idField = entityMetadata.getFields()
                                                    .stream()
                                                    .filter(EntityFieldMetadata::isIdentifier)
                                                    .findFirst()
                                                    .orElseThrow(() -> new IllegalArgumentException("Entity must have an @Id field."));

        HbmIdDescriptor idDesc = new HbmIdDescriptor(idField.getPropertyName(),
                // Use ColumnDetails name, falling back to property name if null
                idField.getColumnDetails()
                       .getColumnName() != null ? idField.getColumnDetails()
                                                         .getColumnName()
                               : idField.getPropertyName()
                                        .toUpperCase(),
                mapGenerationStrategy(idField.getGenerationStrategy()));

        String className = entityMetadata.getClassName();
        String tableName = entityMetadata.getTableName();

        // Fallback for tableName
        if (tableName == null || tableName.isEmpty()) {
            tableName = entityMetadata.getEntityName() != null ? entityMetadata.getEntityName()
                                                                               .toUpperCase()
                    : className.toUpperCase();
        }

        HbmXmlDescriptor hbmDesc = new HbmXmlDescriptor(className, tableName, idDesc);

        entityMetadata.getFields()
                      .stream()
                      .filter(f -> !f.isIdentifier()) // Exclude the ID field, as it's already mapped
                      .forEach(field -> {
                          ColumnDetails cd = field.getColumnDetails();

                          String mappedColumnName = cd.getColumnName() != null ? cd.getColumnName()
                                  : field.getPropertyName()
                                         .toUpperCase();

                          HbmXmlDescriptor.HbmPropertyDescriptor propDesc =
                                  new HbmXmlDescriptor.HbmPropertyDescriptor(field.getPropertyName(), mappedColumnName,
                                          mapType(field.getTypeScriptType(), cd.getDatabaseType()), cd.getLength());
                          hbmDesc.addProperty(propDesc);
                      });

        return hbmDesc;
    }

    /**
     * Mapping from TypeScript types and Database type hints to Hibernate HBM types.
     */
    private static String mapType(String tsType, String dbType) {
        // Clean up the TypeScript type (remove '| null' for easier matching)
        String cleanTsType = tsType.toLowerCase()
                                   .replace(" | null", "")
                                   .trim();
        String cleanDbType = (dbType != null) ? dbType.toLowerCase()
                                                      .trim()
                : "";

        if (!cleanDbType.isEmpty()) {
            return switch (cleanDbType) {
                // String/Text Types
                case "varchar", "nvarchar", "char", "text", "ntext" -> "string";
                // Integer Types
                case "tinyint", "smallint" -> "short";
                case "int", "integer" -> "integer";
                case "bigint", "long" -> "long";
                // Floating Point/Decimal Types
                case "float", "real" -> "float";
                case "double", "double precision" -> "double";
                case "numeric", "decimal", "money", "currency" -> "big_decimal";
                // Boolean Type
                case "boolean", "bit" -> "boolean";
                // Date/Time Types
                case "date" -> "date";
                case "time" -> "time";
                case "datetime", "timestamp", "datetime2" -> "timestamp";
                // Binary/LOB Types
                case "blob", "binary", "varbinary" -> "binary";
                case "clob" -> "clob";
                // UUID
                case "uuid" -> "uuid-char"; // Assuming string representation
                // Default for explicit DB type that isn't fully matched
                default -> cleanDbType;
            };
        }

        // Fallback mapping based on TypeScript type
        return switch (cleanTsType) {
            case "number" -> "long"; // Default number to a generic long/integer type
            case "string" -> "string";
            case "boolean" -> "boolean";
            case "date" -> "timestamp";
            default -> "string"; // Fallback for any unknown object or complex type
        };
    }

    /** Simple mapping from common generation strategies to Hibernate generator class. */
    private static String mapGenerationStrategy(String strategy) {
        if (strategy == null)
            return "assigned";
        return switch (strategy.toLowerCase()) {
            case "sequence", "uuid", "increment", "auto" -> "native"; // native is often used for db-specific sequence/identity
            case "identity" -> "identity";
            default -> "assigned";
        };
    }

}
