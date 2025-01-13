/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.encryption;

import org.hibernate.event.spi.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * https://github.com/galovics/hibernate-encryption-listener
 *
 */
@Component
public class EncryptionListener implements PreInsertEventListener, PreUpdateEventListener, PreLoadEventListener {

    /** The field encrypter. */
    @Autowired
    private FieldEncrypter fieldEncrypter;

    /** The field decrypter. */
    @Autowired
    private FieldDecrypter fieldDecrypter;

    /**
     * On pre insert.
     *
     * @param event the event
     * @return true, if successful
     */
    @Override
    public boolean onPreInsert(PreInsertEvent event) {
        Object[] state = event.getState();
        String[] propertyNames = event.getPersister()
                                      .getPropertyNames();
        Object entity = event.getEntity();
        fieldEncrypter.encrypt(state, propertyNames, entity);
        return false;
    }

    /**
     * On pre update.
     *
     * @param event the event
     * @return true, if successful
     */
    @Override
    public boolean onPreUpdate(PreUpdateEvent event) {
        Object[] state = event.getState();
        String[] propertyNames = event.getPersister()
                                      .getPropertyNames();
        Object entity = event.getEntity();
        fieldEncrypter.encrypt(state, propertyNames, entity);
        return false;
    }

    /**
     * On pre load.
     *
     * @param event the event
     */
    @Override
    public void onPreLoad(PreLoadEvent event) {
        Object[] state = event.getState();
        String[] propertyNames = event.getPersister()
                                      .getPropertyNames();
        Object entity = event.getEntity();
        fieldDecrypter.decrypt(state, propertyNames, entity);
    }
}
