package org.eclipse.dirigible.integration.tests.ui.tests;

import org.eclipse.dirigible.tests.EdmView;
import org.eclipse.dirigible.tests.IDE;
import org.eclipse.dirigible.tests.IDEFactory;
import org.eclipse.dirigible.tests.util.ProjectUtil;
import org.eclipse.dirigible.tests.util.SecurityUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component
class ApproveLeaveRequestTestProject extends BPMLeaveRequestTestProject {

    ApproveLeaveRequestTestProject(IDE ide, ProjectUtil projectUtil, EdmView edmView, SecurityUtil securityUtil, IDEFactory ideFactory) {
        super(ide, projectUtil, edmView, securityUtil, ideFactory);
    }

    @Override
    protected boolean shouldApproveRequest() {
        return true;
    }
}
