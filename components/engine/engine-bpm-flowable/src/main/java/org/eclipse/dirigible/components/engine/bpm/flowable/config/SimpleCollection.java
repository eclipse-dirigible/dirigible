package org.eclipse.dirigible.components.engine.bpm.flowable.config;

class SimpleCollection {

    private final CollectionType collectionType;
    private final ElementType elementType;
    private final String jsonValue;

    public SimpleCollection(CollectionType collectionType, ElementType elementType, String jsonValue) {
        this.collectionType = collectionType;
        this.elementType = elementType;
        this.jsonValue = jsonValue;
    }

    enum CollectionType {
        SET, LIST, QUEUE
    }


    enum ElementType {
        BYTE, SHORT, INT, LONG, DOUBLE, FLOAT, BOOLEAN, STRING
    }

    public String getJsonValue() {
        return jsonValue;
    }

    public CollectionType getCollectionType() {
        return collectionType;
    }

    public ElementType getElementType() {
        return elementType;
    }

    @Override
    public String toString() {
        return "SimpleCollection{" + "collectionType=" + collectionType + ", elementType=" + elementType + ", jsonValue='" + jsonValue
                + '\'' + '}';
    }
}
