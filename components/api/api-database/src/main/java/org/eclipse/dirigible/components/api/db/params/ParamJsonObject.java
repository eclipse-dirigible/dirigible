package org.eclipse.dirigible.components.api.db.params;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

class ParamJsonObject {

    private final String name;
    private final String type;
    private final JsonElement valueElement;

    ParamJsonObject(String name, String type, JsonElement valueElement) {
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

    static ParamJsonObject fromJsonElement(JsonElement parameterElement) throws IllegalArgumentException {
        JsonObject jsonObject = parameterElement.getAsJsonObject();
        String name = getName(jsonObject);

        String type = getType(jsonObject);

        JsonElement valueElement = jsonObject.get("value");

        return new ParamJsonObject(name, type, valueElement);
    }

    private static String getName(JsonObject jsonObject) {
        JsonElement nameElement = jsonObject.get("name");
        return null == nameElement ? null
                : nameElement.getAsJsonPrimitive()
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
