package org.eclipse.dirigible.components.engine.bpm.flowable.config;

import com.google.gson.reflect.TypeToken;
import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.components.engine.bpm.flowable.delegate.TypesUtil;
import org.flowable.variable.api.types.ValueFields;
import org.flowable.variable.api.types.VariableType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class SimpleCollectionVariableType implements VariableType {

    public static final String TYPE_NAME = "simpleCollection";
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleCollectionVariableType.class);

    @Override
    public String getTypeName() {
        return TYPE_NAME;
    }

    @Override
    public boolean isCachable() {
        return true;
    }

    @Override
    public Object getValue(ValueFields valueFields) {
        String simpleCollectionJson = valueFields.getTextValue();
        if (null == simpleCollectionJson) {
            return null;
        }

        SimpleCollection simpleCollection = GsonHelper.fromJson(simpleCollectionJson, SimpleCollection.class);

        SimpleCollection.CollectionType collectionType = simpleCollection.getCollectionType();
        SimpleCollection.ElementType elementType = simpleCollection.getElementType();
        String jsonValue = simpleCollection.getJsonValue();

        if (collectionType == SimpleCollection.CollectionType.SET) {

            if (elementType == SimpleCollection.ElementType.BYTE) {
                TypeToken<Set<Byte>> typeToken = new TypeToken<>() {};
                return GsonHelper.fromJson(jsonValue, typeToken);
            }

            if (elementType == SimpleCollection.ElementType.SHORT) {
                TypeToken<Set<Short>> typeToken = new TypeToken<>() {};
                return GsonHelper.fromJson(jsonValue, typeToken);
            }

            if (elementType == SimpleCollection.ElementType.INT) {
                TypeToken<Set<Integer>> typeToken = new TypeToken<>() {};
                return GsonHelper.fromJson(jsonValue, typeToken);
            }

            if (elementType == SimpleCollection.ElementType.LONG) {
                TypeToken<Set<Long>> typeToken = new TypeToken<>() {};
                return GsonHelper.fromJson(jsonValue, typeToken);
            }

            if (elementType == SimpleCollection.ElementType.DOUBLE) {
                TypeToken<Set<Double>> typeToken = new TypeToken<>() {};
                return GsonHelper.fromJson(jsonValue, typeToken);
            }

            if (elementType == SimpleCollection.ElementType.FLOAT) {
                TypeToken<Set<Float>> typeToken = new TypeToken<>() {};
                return GsonHelper.fromJson(jsonValue, typeToken);
            }

            if (elementType == SimpleCollection.ElementType.BOOLEAN) {
                TypeToken<Set<Boolean>> typeToken = new TypeToken<>() {};
                return GsonHelper.fromJson(jsonValue, typeToken);
            }

            if (elementType == SimpleCollection.ElementType.STRING) {
                TypeToken<Set<String>> typeToken = new TypeToken<>() {};
                return GsonHelper.fromJson(jsonValue, typeToken);
            }
        }

        if (collectionType == SimpleCollection.CollectionType.LIST) {

            if (elementType == SimpleCollection.ElementType.BYTE) {
                TypeToken<List<Byte>> typeToken = new TypeToken<>() {};
                return GsonHelper.fromJson(jsonValue, typeToken);
            }

            if (elementType == SimpleCollection.ElementType.SHORT) {
                TypeToken<List<Short>> typeToken = new TypeToken<>() {};
                return GsonHelper.fromJson(jsonValue, typeToken);
            }

            if (elementType == SimpleCollection.ElementType.INT) {
                TypeToken<List<Integer>> typeToken = new TypeToken<>() {};
                return GsonHelper.fromJson(jsonValue, typeToken);
            }

            if (elementType == SimpleCollection.ElementType.LONG) {
                TypeToken<List<Long>> typeToken = new TypeToken<>() {};
                return GsonHelper.fromJson(jsonValue, typeToken);
            }

            if (elementType == SimpleCollection.ElementType.DOUBLE) {
                TypeToken<List<Double>> typeToken = new TypeToken<>() {};
                return GsonHelper.fromJson(jsonValue, typeToken);
            }

            if (elementType == SimpleCollection.ElementType.FLOAT) {
                TypeToken<List<Float>> typeToken = new TypeToken<>() {};
                return GsonHelper.fromJson(jsonValue, typeToken);
            }

            if (elementType == SimpleCollection.ElementType.BOOLEAN) {
                TypeToken<List<Boolean>> typeToken = new TypeToken<>() {};
                return GsonHelper.fromJson(jsonValue, typeToken);
            }

            if (elementType == SimpleCollection.ElementType.STRING) {
                TypeToken<List<String>> typeToken = new TypeToken<>() {};
                return GsonHelper.fromJson(jsonValue, typeToken);
            }
        }

        if (collectionType == SimpleCollection.CollectionType.QUEUE) {

            if (elementType == SimpleCollection.ElementType.BYTE) {
                TypeToken<Queue<Byte>> typeToken = new TypeToken<>() {};
                return GsonHelper.fromJson(jsonValue, typeToken);
            }

            if (elementType == SimpleCollection.ElementType.SHORT) {
                TypeToken<Queue<Short>> typeToken = new TypeToken<>() {};
                return GsonHelper.fromJson(jsonValue, typeToken);
            }

            if (elementType == SimpleCollection.ElementType.INT) {
                TypeToken<Queue<Integer>> typeToken = new TypeToken<>() {};
                return GsonHelper.fromJson(jsonValue, typeToken);
            }

            if (elementType == SimpleCollection.ElementType.LONG) {
                TypeToken<Queue<Long>> typeToken = new TypeToken<>() {};
                return GsonHelper.fromJson(jsonValue, typeToken);
            }

            if (elementType == SimpleCollection.ElementType.DOUBLE) {
                TypeToken<Queue<Double>> typeToken = new TypeToken<>() {};
                return GsonHelper.fromJson(jsonValue, typeToken);
            }

            if (elementType == SimpleCollection.ElementType.FLOAT) {
                TypeToken<Queue<Float>> typeToken = new TypeToken<>() {};
                return GsonHelper.fromJson(jsonValue, typeToken);
            }

            if (elementType == SimpleCollection.ElementType.BOOLEAN) {
                TypeToken<Queue<Boolean>> typeToken = new TypeToken<>() {};
                return GsonHelper.fromJson(jsonValue, typeToken);
            }

            if (elementType == SimpleCollection.ElementType.STRING) {
                TypeToken<Queue<String>> typeToken = new TypeToken<>() {};
                return GsonHelper.fromJson(jsonValue, typeToken);
            }
        }

        throw new IllegalStateException("Cannot deserialize value from " + simpleCollection);
    }

    @Override
    public void setValue(Object value, ValueFields valueFields) {
        if (value == null) {
            valueFields.setTextValue(null);
        } else {

            SimpleCollection.CollectionType collectionType = determineCollectionType(value);
            SimpleCollection.ElementType elementType = determineElementType(value);
            String jsonValue = GsonHelper.toJson(value);

            SimpleCollection simpleCollection = new SimpleCollection(collectionType, elementType, jsonValue);
            String variableValue = GsonHelper.toJson(simpleCollection);
            valueFields.setTextValue(variableValue);
        }
    }

    private SimpleCollection.CollectionType determineCollectionType(Object value) {
        if (value instanceof Set) {
            return SimpleCollection.CollectionType.SET;
        }

        if (value instanceof List) {
            return SimpleCollection.CollectionType.LIST;
        }

        if (value instanceof Queue) {
            return SimpleCollection.CollectionType.QUEUE;
        }

        throw new IllegalStateException("Unsupported collection type " + value.getClass());
    }

    private SimpleCollection.ElementType determineElementType(Object value) {
        if (!(value instanceof Collection<?> collection)) {
            throw new IllegalStateException("Provided value [" + value + " is not a collection");
        }

        if (TypesUtil.isBytesCollection(collection)) {
            return SimpleCollection.ElementType.BYTE;
        }

        if (TypesUtil.isShortsCollection(collection)) {
            return SimpleCollection.ElementType.SHORT;
        }

        if (TypesUtil.isIntsCollection(collection)) {
            return SimpleCollection.ElementType.INT;
        }

        if (TypesUtil.isLongsCollection(collection)) {
            return SimpleCollection.ElementType.LONG;
        }

        if (TypesUtil.isBooleansCollection(collection)) {
            return SimpleCollection.ElementType.BOOLEAN;
        }

        if (TypesUtil.isStringsCollection(collection)) {
            return SimpleCollection.ElementType.STRING;
        }

        if (TypesUtil.isDoublesCollection(collection)) {
            return SimpleCollection.ElementType.DOUBLE;
        }

        if (TypesUtil.isFloatsCollection(collection)) {
            return SimpleCollection.ElementType.FLOAT;
        }

        throw new IllegalStateException("Unsupported elements type for collection of type " + value.getClass() + " . Value: " + value);
    }

    @Override
    public boolean isAbleToStore(Object value) {
        if (!TypesUtil.isPrimitiveWrapperOrStringCollection(value)) {
            return false;
        }

        if (value instanceof Set || value instanceof List || value instanceof Queue) {
            return true;
        }

        LOGGER.debug("Unsupported collection type {}", value.getClass());
        return false;

    }
}
