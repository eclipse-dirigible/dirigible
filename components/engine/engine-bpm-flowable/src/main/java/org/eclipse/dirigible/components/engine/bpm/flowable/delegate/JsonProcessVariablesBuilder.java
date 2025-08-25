package org.eclipse.dirigible.components.engine.bpm.flowable.delegate;

import org.eclipse.dirigible.components.base.helpers.JsonHelper;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class JsonProcessVariablesBuilder {

    private final Map<String, Object> variables;

    public JsonProcessVariablesBuilder() {
        this.variables = new HashMap<>();
    }

    public Map<String, Object> build() {
        return variables;
    }

    public JsonProcessVariablesBuilder addVariable(String variableName, Object value) {
        String jsonValue = JsonHelper.toJson(value);
        variables.put(variableName, jsonValue);
        return this;
    }
}
