/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.persistence.test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The Class All Data Types.
 */
@Table(name = "DT")
public class AllDataTypes {

    /** The varchar. */
    @Id
    @Column(name = "ALL_VARCHAR", columnDefinition = "VARCHAR", nullable = false, length = 512)
    private String _varchar;

    /** The char. */
    @Column(name = "ALL_CHAR", columnDefinition = "CHAR", nullable = false, length = 10)
    private String _char;

    /** The date. */
    @Column(name = "ALL_DATE", columnDefinition = "DATE", nullable = false)
    private Date _date;

    /** The time. */
    @Column(name = "ALL_TIME", columnDefinition = "TIME", nullable = false)
    private Time _time;

    /** The timestamp. */
    @Column(name = "ALL_TIMESTAMP", columnDefinition = "TIMESTAMP", nullable = false)
    private Timestamp _timestamp;

    /** The integer. */
    @Column(name = "ALL_INTEGER", columnDefinition = "INTEGER", nullable = false)
    private int _integer;

    /** The tinyint. */
    @Column(name = "ALL_TINYINT", columnDefinition = "TINYINT", nullable = false)
    private byte _tinyint;

    /** The bigint. */
    @Column(name = "ALL_BIGINT", columnDefinition = "BIGINT", nullable = false)
    private BigInteger _bigint;

    /** The smallint. */
    @Column(name = "ALL_SMALLINT", columnDefinition = "SMALLINT", nullable = false)
    private short _smallint;

    /** The real. */
    @Column(name = "ALL_REAL", columnDefinition = "REAL", nullable = false)
    private float _real;

    /** The double. */
    @Column(name = "ALL_DOUBLE", columnDefinition = "DOUBLE", nullable = false)
    private double _double;

    /** The boolean. */
    @Column(name = "ALL_BOOLEAN", columnDefinition = "BOOLEAN", nullable = false)
    private boolean _boolean;

    /** The blob. */
    @Column(name = "ALL_BLOB", columnDefinition = "BLOB", nullable = false)
    private byte[] _blob;

    /** The decimal. */
    @Column(name = "ALL_DECIMAL", columnDefinition = "DECIMAL", nullable = false, length = 10, scale = 2)
    private BigDecimal _decimal;

    /** The bit. */
    @Column(name = "ALL_BIT", columnDefinition = "BIT", nullable = false)
    private boolean _bit;

    /**
     * Gets the varchar.
     *
     * @return the varchar
     */
    public String get_varchar() {
        return _varchar;
    }

    /**
     * Sets the varchar.
     *
     * @param _varchar the new varchar
     */
    public void set_varchar(String _varchar) {
        this._varchar = _varchar;
    }

    /**
     * Gets the char.
     *
     * @return the char
     */
    public String get_char() {
        return _char;
    }

    /**
     * Sets the char.
     *
     * @param _char the new char
     */
    public void set_char(String _char) {
        this._char = _char;
    }

    /**
     * Gets the date.
     *
     * @return the date
     */
    public Date get_date() {
        return _date;
    }

    /**
     * Sets the date.
     *
     * @param _date the new date
     */
    public void set_date(Date _date) {
        this._date = _date;
    }

    /**
     * Gets the time.
     *
     * @return the time
     */
    public Time get_time() {
        return _time;
    }

    /**
     * Sets the time.
     *
     * @param _time the new time
     */
    public void set_time(Time _time) {
        this._time = _time;
    }

    /**
     * Gets the timestamp.
     *
     * @return the timestamp
     */
    public Timestamp get_timestamp() {
        return _timestamp;
    }

    /**
     * Sets the timestamp.
     *
     * @param _timestamp the new timestamp
     */
    public void set_timestamp(Timestamp _timestamp) {
        this._timestamp = _timestamp;
    }

    /**
     * Gets the integer.
     *
     * @return the integer
     */
    public int get_integer() {
        return _integer;
    }

    /**
     * Sets the integer.
     *
     * @param _integer the new integer
     */
    public void set_integer(int _integer) {
        this._integer = _integer;
    }

    /**
     * Gets the tinyint.
     *
     * @return the tinyint
     */
    public byte get_tinyint() {
        return _tinyint;
    }

    /**
     * Sets the tinyint.
     *
     * @param _tinyint the new tinyint
     */
    public void set_tinyint(byte _tinyint) {
        this._tinyint = _tinyint;
    }

    /**
     * Gets the bigint.
     *
     * @return the bigint
     */
    public BigInteger get_bigint() {
        return _bigint;
    }

    /**
     * Sets the bigint.
     *
     * @param _bigint the new bigint
     */
    public void set_bigint(BigInteger _bigint) {
        this._bigint = _bigint;
    }

    /**
     * Gets the smallint.
     *
     * @return the smallint
     */
    public short get_smallint() {
        return _smallint;
    }

    /**
     * Sets the smallint.
     *
     * @param _smallint the new smallint
     */
    public void set_smallint(short _smallint) {
        this._smallint = _smallint;
    }

    /**
     * Gets the real.
     *
     * @return the real
     */
    public float get_real() {
        return _real;
    }

    /**
     * Sets the real.
     *
     * @param _real the new real
     */
    public void set_real(float _real) {
        this._real = _real;
    }

    /**
     * Gets the double.
     *
     * @return the double
     */
    public double get_double() {
        return _double;
    }

    /**
     * Sets the double.
     *
     * @param _double the new double
     */
    public void set_double(double _double) {
        this._double = _double;
    }

    /**
     * Checks if is boolean.
     *
     * @return true, if is boolean
     */
    public boolean is_boolean() {
        return _boolean;
    }

    /**
     * Sets the boolean.
     *
     * @param _boolean the new boolean
     */
    public void set_boolean(boolean _boolean) {
        this._boolean = _boolean;
    }

    /**
     * Gets the blob.
     *
     * @return the blob
     */
    public byte[] get_blob() {
        return _blob;
    }

    /**
     * Sets the blob.
     *
     * @param _blob the new blob
     */
    public void set_blob(byte[] _blob) {
        this._blob = _blob;
    }

    /**
     * Gets the decimal.
     *
     * @return the decimal
     */
    public BigDecimal get_decimal() {
        return _decimal;
    }

    /**
     * Sets the decimal.
     *
     * @param _decimal the new decimal
     */
    public void set_decimal(BigDecimal _decimal) {
        this._decimal = _decimal;
    }

    /**
     * Checks if is bit.
     *
     * @return true, if is bit
     */
    public boolean is_bit() {
        return _bit;
    }

    /**
     * Sets the bit.
     *
     * @param _bit the new bit
     */
    public void set_bit(boolean _bit) {
        this._bit = _bit;
    }



}
