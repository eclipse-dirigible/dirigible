/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests;

import com.icegreen.greenmail.util.GreenMail;
import org.eclipse.dirigible.tests.EdmView;
import org.eclipse.dirigible.tests.IDE;
import org.eclipse.dirigible.tests.IDEFactory;
import org.eclipse.dirigible.tests.util.ProjectUtil;
import org.eclipse.dirigible.tests.util.SecurityUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component
class DeclineLeaveRequestTestProject extends BPMLeaveRequestTestProject {

    DeclineLeaveRequestTestProject(IDE ide, ProjectUtil projectUtil, EdmView edmView, SecurityUtil securityUtil, IDEFactory ideFactory,
            GreenMail greenMail) {
        super(ide, projectUtil, edmView, securityUtil, ideFactory, greenMail);
    }

    @Override
    protected boolean shouldApproveRequest() {
        return false;
    }
}
