/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.persistence.processors;

import static java.text.MessageFormat.format;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.database.persistence.IEntityManagerInterceptor;
import org.eclipse.dirigible.database.persistence.PersistenceException;
import org.eclipse.dirigible.database.persistence.model.PersistenceTableColumnModel;
import org.eclipse.dirigible.database.persistence.model.PersistenceTableModel;
import org.eclipse.dirigible.database.persistence.parser.PersistenceAnnotationsParser;
import org.eclipse.dirigible.database.persistence.parser.Serializer;
import org.eclipse.dirigible.database.sql.DataTypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.EnumType;

/**
 * The Abstract Persistence Processor.
 */
public abstract class AbstractPersistenceProcessor implements IPersistenceProcessor {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(AbstractPersistenceProcessor.class);

    /** The entity manager interceptor. */
    private IEntityManagerInterceptor entityManagerInterceptor;

    /**
     * Instantiates a new abstract persistence processor.
     */
    public AbstractPersistenceProcessor() {
        this(null);
    }

    /**
     * Instantiates a new abstract persistence processor.
     *
     * @param entityManagerInterceptor the entity manager interceptor
     */
    protected AbstractPersistenceProcessor(IEntityManagerInterceptor entityManagerInterceptor) {
        this.entityManagerInterceptor = entityManagerInterceptor;
    }

    /**
     * Generate script.
     *
     * @param connection the connection
     * @param tableModel the table model
     * @return the string
     */
    protected abstract String generateScript(Connection connection, PersistenceTableModel tableModel);

    /**
     * Sets the values from pojo.
     *
     * @param tableModel the table model
     * @param pojo the pojo
     * @param preparedStatement the prepared statement
     * @throws SQLException the SQL exception
     * @throws NoSuchFieldException the no such field exception
     * @throws IllegalAccessException the illegal access exception
     */
    protected void setValuesFromPojo(PersistenceTableModel tableModel, Object pojo, PreparedStatement preparedStatement)
            throws SQLException, NoSuchFieldException, IllegalAccessException {
        if (logger.isTraceEnabled()) {
            logger.trace("setValuesFromPojo -> tableModel: " + Serializer.serializeTableModel(tableModel) + ", pojo: "
                    + Serializer.serializePojo(pojo));
        }
        int i = 1;
        for (PersistenceTableColumnModel columnModel : tableModel.getColumns()) {
            if (!shouldSetColumnValue(columnModel)) {
                continue;
            }
            if (columnModel.isIdentity()) {
                continue;
            }
            // Field field = pojo.getClass().getDeclaredField(columnModel.getField());
            Field field = getFieldFromClass(pojo.getClass(), columnModel.getField());
            String dataType = columnModel.getType();
            Object valueObject = null;
            boolean oldAccessible = setAccessible(field);
            try {
                valueObject = field.get(pojo);
            } finally {
                resetAccessible(field, oldAccessible);
            }
            try {
                if ((columnModel.getEnumerated() != null) && (valueObject != null)) {
                    if (EnumType.valueOf(columnModel.getEnumerated())
                                .equals(EnumType.ORDINAL)) {
                        valueObject = ((Enum) valueObject).ordinal();
                    } else {
                        // EnumType.STRING
                        valueObject = ((Enum) valueObject).name();
                    }
                }
                valueObject = truncateValue(columnModel, valueObject);
                setValue(preparedStatement, i++, dataType, valueObject);
            } catch (PersistenceException e) {
                if (logger.isErrorEnabled()) {
                    logger.error(e.getMessage(), e);
                }
                throw new PersistenceException(format("Database type [{0}] not supported (Class: [{1}])", dataType, pojo.getClass()));
            }
        }
    }

    /**
     * Truncate value.
     *
     * @param columnModel the column model
     * @param valueObject the value object
     * @return the object
     */
    protected Object truncateValue(PersistenceTableColumnModel columnModel, Object valueObject) {
        String dataType = columnModel.getType();
        int dataLength = columnModel.getLength();
        if (DataTypeUtils.isVarchar(dataType) || DataTypeUtils.isNvarchar(dataType) || DataTypeUtils.isChar(dataType)) {
            if (valueObject != null && valueObject.toString()
                                                  .length() > dataLength) {
                if (logger.isWarnEnabled()) {
                    logger.warn("String has been truncated to fit to the database column [{}] length [{}].", columnModel.getName(),
                            columnModel.getLength());
                }
                return valueObject.toString()
                                  .substring(0, dataLength - 1);
            }
        }
        return valueObject;
    }

    /**
     * Should set column value.
     *
     * @param columnModel the column model
     * @return true, if successful
     */
    protected boolean shouldSetColumnValue(PersistenceTableColumnModel columnModel) {
        return true;
    }

    /**
     * Reset accessible.
     *
     * @param field the field
     * @param oldAccessible the old accessible
     */
    private void resetAccessible(Field field, boolean oldAccessible) {
        field.setAccessible(oldAccessible);
    }

    /**
     * Sets the accessible.
     *
     * @param field the field
     * @return true, if successful
     */
    private boolean setAccessible(Field field) {
        boolean oldAccessible = field.isAccessible();
        field.setAccessible(true);
        return oldAccessible;
    }

    /**
     * Sets the value primary key.
     *
     * @param tableModel the table model
     * @param id the id
     * @param preparedStatement the prepared statement
     * @throws SQLException the SQL exception
     * @throws NoSuchFieldException the no such field exception
     * @throws IllegalAccessException the illegal access exception
     */
    protected void setValuePrimaryKey(PersistenceTableModel tableModel, Object id, PreparedStatement preparedStatement)
            throws SQLException, NoSuchFieldException, IllegalAccessException {
        if (logger.isTraceEnabled()) {
            logger.trace("setValuePrimaryKey -> tableModel: " + Serializer.serializeTableModel(tableModel) + ", id: " + id);
        }
        for (PersistenceTableColumnModel columnModel : tableModel.getColumns()) {
            if (columnModel.isPrimaryKey()) {
                String dataType = columnModel.getType();
                setValue(preparedStatement, 1, dataType, id);
                break;
            }
        }
    }

    /**
     * Sets the value.
     *
     * @param preparedStatement the prepared statement
     * @param i the i
     * @param value the value
     * @throws SQLException the SQL exception
     */
    protected void setValue(PreparedStatement preparedStatement, int i, Object value) throws SQLException {
        if (logger.isTraceEnabled()) {
            logger.trace("setValue -> i: " + i + ", value: " + value);
        }
        setValue(preparedStatement, i, DataTypeUtils.getDatabaseTypeNameByJavaType(value.getClass()), value);
    }

    /**
     * Sets the value.
     *
     * @param preparedStatement the prepared statement
     * @param i the i
     * @param dataType the data type
     * @param value the value
     * @throws SQLException the SQL exception
     */
    protected void setValue(PreparedStatement preparedStatement, int i, String dataType, Object value) throws SQLException {
        if (logger.isTraceEnabled()) {
            logger.trace("setValue -> i: " + i + ", dataType: " + dataType + ", value: " + value);
        }
        if (getEntityManagerInterceptor() != null) {
            value = getEntityManagerInterceptor().onGetValueBeforeUpdate(i, dataType, value);
        }

        if (value == null) {
            preparedStatement.setNull(i, DataTypeUtils.getSqlTypeByDataType(dataType));
        } else if (DataTypeUtils.isVarchar(dataType)) {
            preparedStatement.setString(i, (String) value);
        } else if (DataTypeUtils.isNvarchar(dataType)) {
            preparedStatement.setString(i, (String) value);
        } else if (DataTypeUtils.isChar(dataType)) {
            if (value instanceof String) {
                preparedStatement.setString(i, (String) value);
            } else if ((value instanceof Character) || char.class.getCanonicalName()
                                                                 .equals(value.getClass()
                                                                              .getCanonicalName())) {
                preparedStatement.setString(i, new String(new char[] {(char) value}));
            } else {
                throw new PersistenceException(format("Database type [{0}] cannot be set as [{1}]", dataType, value.getClass()
                                                                                                                   .getName()));
            }
        } else if (DataTypeUtils.isDate(dataType)) {
            preparedStatement.setDate(i, (Date) value);
        } else if (DataTypeUtils.isTime(dataType)) {
            preparedStatement.setTime(i, (Time) value);
        } else if (DataTypeUtils.isTimestamp(dataType)) {
            preparedStatement.setTimestamp(i, (Timestamp) value);
        } else if (DataTypeUtils.isInteger(dataType)) {
            preparedStatement.setInt(i, (Integer) value);
        } else if (DataTypeUtils.isTinyint(dataType)) {
            preparedStatement.setByte(i, (byte) value);
        } else if (DataTypeUtils.isSmallint(dataType)) {
            preparedStatement.setShort(i, (Short) value);
        } else if (DataTypeUtils.isBigint(dataType)) {
            if (value instanceof Long) {
                preparedStatement.setLong(i, (Long) value);
            } else if (value instanceof BigInteger) {
                preparedStatement.setLong(i, ((BigInteger) value).longValueExact());
            } else {
                throw new PersistenceException(format("Database type [{0}] cannot be set as [{1}]", dataType, value.getClass()
                                                                                                                   .getName()));
            }
        } else if (DataTypeUtils.isReal(dataType)) {
            preparedStatement.setFloat(i, (Float) value);
        } else if (DataTypeUtils.isDouble(dataType)) {
            preparedStatement.setDouble(i, (Double) value);
        } else if (DataTypeUtils.isBoolean(dataType)) {
            preparedStatement.setBoolean(i, (Boolean) value);
        } else if (DataTypeUtils.isDecimal(dataType)) {
            if (value instanceof Double) {
                preparedStatement.setDouble(i, (Double) value);
            } else if (value instanceof BigDecimal) {
                preparedStatement.setBigDecimal(i, ((BigDecimal) value));
            } else {
                throw new PersistenceException(format("Database type [{0}] cannot be set as [{1}]", dataType, value.getClass()
                                                                                                                   .getName()));
            }
        } else if (DataTypeUtils.isBlob(dataType)) {
            byte[] bytes = (byte[]) value;
            preparedStatement.setBinaryStream(i, new ByteArrayInputStream(bytes), bytes.length);
        } else if (DataTypeUtils.isBit(dataType)) {
            if ((value instanceof Boolean) || Boolean.TYPE.isInstance(value)) {
                preparedStatement.setBoolean(i, (Boolean) value);
            } else if (value instanceof Byte || Byte.TYPE.isInstance(value)) {
                preparedStatement.setBoolean(i, ((Byte) value == 1));
            } else if (value instanceof Integer || Integer.TYPE.isInstance(value)) {
                preparedStatement.setBoolean(i, ((Integer) value == 1));
            } else {
                throw new PersistenceException(format("Database type [{0}] cannot be set as [{1}]", dataType, value.getClass()
                                                                                                                   .getName()));
            }
        } else if (DataTypeUtils.isArray(dataType) && value instanceof List) {
            for (Object element : ((List<?>) value)) {
                setValue(preparedStatement, i++, element);
            }
        }

        else {
            throw new PersistenceException(format("Database type [{0}] not supported", dataType));
        }
    }

    /**
     * Sets the value to pojo.
     *
     * @param pojo the pojo
     * @param resultSet the result set
     * @param columnModel the column model
     * @throws NoSuchFieldException the no such field exception
     * @throws SQLException the SQL exception
     * @throws IllegalAccessException the illegal access exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected void setValueToPojo(Object pojo, ResultSet resultSet, PersistenceTableColumnModel columnModel)
            throws NoSuchFieldException, SQLException, IllegalAccessException, IOException {
        Object value = resultSet.getObject(columnModel.getName());
        setValueToPojo(pojo, value, columnModel);
    }

    /**
     * Sets the value to pojo.
     *
     * @param pojo the pojo
     * @param value the value
     * @param columnModel the column model
     * @throws NoSuchFieldException the no such field exception
     * @throws SQLException the SQL exception
     * @throws IllegalAccessException the illegal access exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected void setValueToPojo(Object pojo, Object value, PersistenceTableColumnModel columnModel)
            throws NoSuchFieldException, SQLException, IllegalAccessException, IOException {
        if (logger.isTraceEnabled()) {
            logger.trace("setValueToPojo -> pojo: " + Serializer.serializePojo(pojo) + ", value: " + value + ", columnModel: "
                    + Serializer.serializeColumnModel(columnModel));
        }
        Field field = getFieldFromClass(pojo.getClass(), columnModel.getField());
        boolean oldAccessible = setAccessible(field);
        try {
            if (columnModel.getEnumerated() != null) {
                if (EnumType.valueOf(columnModel.getEnumerated())
                            .equals(EnumType.ORDINAL)
                        && (value instanceof Integer)) {
                    if (field.getType()
                             .isEnum()) {
                        value = field.getType()
                                     .getEnumConstants()[(Integer) value];
                    } else {
                        throw new IllegalStateException(
                                "The annotation @Enumerated is set to a field with a type, which is not an enum type.");
                    }
                } else if (EnumType.valueOf(columnModel.getEnumerated())
                                   .equals(EnumType.STRING)
                        && (value instanceof String)) {
                    if (field.getType()
                             .isEnum()) {
                        value = Enum.valueOf((Class<Enum>) field.getType(), (String) value);
                    } else {
                        throw new IllegalStateException(
                                "The annotation @Enumerated is set to a field with a type, which is not an enum type.");
                    }
                } else if (value != null) {
                    throw new IllegalStateException("The annotation @Enumerated is misused, the value is unknown.");
                }
            }
            value = byteAdaptation(value, field);
            value = intAdaptation(value, field);
            value = blobAdaptation(value);
            value = charAdaptation(value, field);
            value = booleanAdaptation(value, field);
            value = bigIntegerAdaptation(value, field);
            value = shortAdaptation(value, field);
            value = floatAdaptation(value, field);

            if (getEntityManagerInterceptor() != null) {
                value = getEntityManagerInterceptor().onSetValueAfterQuery(pojo, field, value);
            }

            field.set(pojo, value);
        } finally {
            resetAccessible(field, oldAccessible);
        }
    }

    /**
     * Float adaptation.
     *
     * @param value the value
     * @param field the field
     * @return the object
     */
    private Object floatAdaptation(Object value, Field field) {
        if (field.getType()
                 .equals(float.class)
                || field.getType()
                        .equals(Float.class)) {
            if (value instanceof Double) {
                value = ((Double) value).floatValue();
            } else if (value instanceof Float) {
                value = (Float) value;
            }
        }
        return value;
    }

    /**
     * Short adaptation.
     *
     * @param value the value
     * @param field the field
     * @return the object
     */
    private Object shortAdaptation(Object value, Field field) {
        if (field.getType()
                 .equals(short.class)
                || field.getType()
                        .equals(Short.class)) {
            if (value instanceof Long) {
                value = ((Long) value).shortValue();
            } else if (value instanceof Integer) {
                value = ((Integer) value).shortValue();
            } else if (value instanceof Byte) {
                value = ((Byte) value).shortValue();
            }
        }
        return value;
    }

    /**
     * Big integer adaptation.
     *
     * @param value the value
     * @param field the field
     * @return the object
     */
    private Object bigIntegerAdaptation(Object value, Field field) {
        if (field.getType()
                 .equals(BigInteger.class)) {
            if (value instanceof Long) {
                value = BigInteger.valueOf(((Long) value));
            }
        }
        return value;
    }

    /**
     * Boolean adaptation.
     *
     * @param value the value
     * @param field the field
     * @return the object
     */
    private Object booleanAdaptation(Object value, Field field) {
        if (field.getType()
                 .equals(boolean.class)
                || field.getType()
                        .equals(Boolean.class)) {
            if (value instanceof Short) {
                value = Boolean.valueOf(((Short) value) != 0);
            } else if (value instanceof Integer) {
                value = Boolean.valueOf(((Integer) value) != 0);
            } else if (value instanceof Long) {
                value = Boolean.valueOf(((Long) value) != 0);
            }
        }
        return value;
    }

    /**
     * Char adaptation.
     *
     * @param value the value
     * @param field the field
     * @return the object
     */
    private Object charAdaptation(Object value, Field field) {
        if (field.getType()
                 .equals(char.class)
                || field.getType()
                        .equals(Character.class)) {
            if ((value instanceof String) && (((String) value).length() <= 1)) {
                value = new Character(((String) value).charAt(0));
            } else {
                throw new IllegalStateException("Trying to set a multi-character string to a single character field.");
            }
        }
        return value;
    }

    /**
     * Blob adaptation.
     *
     * @param value the value
     * @return the object
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws SQLException the SQL exception
     */
    private Object blobAdaptation(Object value) throws IOException, SQLException {
        if (value instanceof Blob) {
            value = IOUtils.toByteArray(((Blob) value).getBinaryStream());
        }
        return value;
    }

    /**
     * Int adaptation.
     *
     * @param value the value
     * @param field the field
     * @return the object
     */
    private Object intAdaptation(Object value, Field field) {
        if (field.getType()
                 .equals(int.class)
                || field.getType()
                        .equals(Integer.class)) {
            if (value instanceof Long) {
                value = ((Long) value).intValue();
            }
        }
        return value;
    }

    /**
     * Byte adaptation.
     *
     * @param value the value
     * @param field the field
     * @return the object
     */
    private Object byteAdaptation(Object value, Field field) {
        if (field.getType()
                 .equals(byte.class)
                || field.getType()
                        .equals(Byte.class)) {
            if (value instanceof Integer) {
                value = ((Integer) value).byteValue();
            } else if (value instanceof Short) {
                value = ((Short) value).byteValue();
            }
        }
        return value;
    }

    /**
     * Gets the value from pojo.
     *
     * @param pojo the pojo
     * @param columnModel the column model
     * @return the value from pojo
     * @throws NoSuchFieldException the no such field exception
     * @throws SQLException the SQL exception
     * @throws IllegalAccessException the illegal access exception
     */
    protected Object getValueFromPojo(Object pojo, PersistenceTableColumnModel columnModel)
            throws NoSuchFieldException, SQLException, IllegalAccessException {
        if (logger.isTraceEnabled()) {
            logger.trace("getValueFromPojo -> pojo: " + Serializer.serializePojo(pojo) + ", columnModel: "
                    + Serializer.serializeColumnModel(columnModel));
        }
        // Field field = pojo.getClass().getDeclaredField(columnModel.getField());
        Field field = getFieldFromClass(pojo.getClass(), columnModel.getField());
        boolean oldAccessible = setAccessible(field);
        try {
            return field.get(pojo);
        } finally {
            resetAccessible(field, oldAccessible);
        }
    }

    /**
     * Gets the field from class.
     *
     * @param clazz the clazz
     * @param fieldName the field name
     * @return the field from class
     * @throws NoSuchFieldException the no such field exception
     */
    private Field getFieldFromClass(Class clazz, String fieldName) throws NoSuchFieldException {
        Field field = null;
        List<Field> fields = Arrays.asList(PersistenceAnnotationsParser.collectFields(clazz));
        for (Field next : fields) {
            if (next.getName()
                    .equals(fieldName)) {
                field = next;
                break;
            }
        }
        if (field == null) {
            throw new NoSuchFieldException(
                    format("There is no a Field named [{0}] in the POJO of Class [{1}]", fieldName, clazz.getCanonicalName()));
        }
        return field;
    }

    /**
     * Open prepared statement.
     *
     * @param connection the connection
     * @param sql the sql
     * @return the prepared statement
     * @throws SQLException the SQL exception
     */
    protected PreparedStatement openPreparedStatement(Connection connection, String sql) throws SQLException {
        return connection.prepareStatement(sql);
    }

    /**
     * Close prepared statement.
     *
     * @param preparedStatement the prepared statement
     */
    protected void closePreparedStatement(PreparedStatement preparedStatement) {
        try {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    /**
     * Gets the primary key.
     *
     * @param tableModel the table model
     * @return the primary key
     */
    public String getPrimaryKey(PersistenceTableModel tableModel) {
        PersistenceTableColumnModel columnModel = getPrimaryKeyModel(tableModel);
        return columnModel == null ? null : columnModel.getName();
    }

    /**
     * Gets the primary key model.
     *
     * @param tableModel the table model
     * @return the primary key model
     */
    protected PersistenceTableColumnModel getPrimaryKeyModel(PersistenceTableModel tableModel) {
        Optional<PersistenceTableColumnModel> optional = tableModel.getColumns()
                                                                   .stream()
                                                                   .filter(model -> model.isPrimaryKey())
                                                                   .findFirst();
        return optional.orElse(null);
    }

    /**
     * Gets the entity manager interceptor.
     *
     * @return the entity manager interceptor
     */
    public IEntityManagerInterceptor getEntityManagerInterceptor() {
        return entityManagerInterceptor;
    }

    /**
     * Sets the entity manager interceptor.
     *
     * @param entityManagerInterceptor the new entity manager interceptor
     */
    public void setEntityManagerInterceptor(IEntityManagerInterceptor entityManagerInterceptor) {
        this.entityManagerInterceptor = entityManagerInterceptor;
    }

}
