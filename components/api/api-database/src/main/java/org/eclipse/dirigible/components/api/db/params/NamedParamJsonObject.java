package org.eclipse.dirigible.components.api.db.params;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

class NamedParamJsonObject {

    private final String name;
    private final String type;
    private final JsonElement valueElement;

    NamedParamJsonObject(String name, String type, JsonElement valueElement) {
        this.name = name;
        this.type = type;
        this.valueElement = valueElement;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public JsonElement getValueElement() {
        return valueElement;
    }

    static NamedParamJsonObject fromJsonElement(JsonElement parameterElement) throws IllegalArgumentException {
        JsonObject jsonObject = parameterElement.getAsJsonObject();
        String name = getName(jsonObject);
        String type = getType(jsonObject);

        JsonElement valueElement = jsonObject.get("value");

        return new NamedParamJsonObject(name, type, valueElement);
    }

    private static String getName(JsonObject jsonObject) {
        JsonElement nameElement = jsonObject.get("name");
        if (null == nameElement) {
            throw new IllegalArgumentException("Missing name member in " + jsonObject);
        }
        if (!nameElement.isJsonPrimitive()) {
            throw new IllegalArgumentException("Invalid name member in " + jsonObject);
        }
        return nameElement.getAsJsonPrimitive()
                          .getAsString();
    }

    private static String getType(JsonObject jsonObject) {
        JsonElement typeElement = jsonObject.get("type");
        if (!typeElement.isJsonPrimitive() || !typeElement.getAsJsonPrimitive()
                                                          .isString()) {
            throw new IllegalArgumentException("Parameter 'type' must be a string representing the database type name");
        }
        return typeElement.getAsJsonPrimitive()
                          .getAsString();
    }
}
