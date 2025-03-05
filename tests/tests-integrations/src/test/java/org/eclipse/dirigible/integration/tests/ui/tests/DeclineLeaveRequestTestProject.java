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
