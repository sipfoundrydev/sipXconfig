/*
 *
 *
 * Copyright (C) 2010 Pingtel Corp., certain elements licensed under a Contributor Agreement.
 * Contributors retain copyright to elements licensed under a Contributor Agreement.
 * Licensed to the User under the LGPL license.
 *
 */
package org.sipfoundry.sipxconfig.setting;

import org.sipfoundry.sipxconfig.IntegrationTestCase;
import org.sipfoundry.sipxconfig.branch.Branch;
import org.sipfoundry.sipxconfig.branch.BranchManager;
import org.sipfoundry.sipxconfig.common.CoreContext;
import org.sipfoundry.sipxconfig.common.User;
import org.sipfoundry.sipxconfig.common.UserException;

public class SettingDaoTestIntegration extends IntegrationTestCase {
    private CoreContext m_coreContext;
    private BranchManager m_branchManager;
    private SettingDao m_settingDao;

    public void testInvalidBranchGroup() throws Exception {
        Branch branch1 = new Branch();
        branch1.setName("branch1");
        Branch branch2 = new Branch();
        branch2.setName("branch2");
        m_branchManager.saveBranch(branch1);
        m_branchManager.saveBranch(branch2);

        Group group = new Group();
        group.setName("group1");

        User user = new User();
        user.setFirstName("First");
        user.setLastName("Last");
        user.setBranch(branch1);
        user.addGroup(group);
        m_coreContext.saveUser(user);

        group.setBranch(branch2);
        try {
            m_settingDao.saveGroup(group);
            //should never be here
            fail();
        } catch (UserException ex) {

        }
    }

    public void setCoreContext(CoreContext coreContext) {
        m_coreContext = coreContext;
    }
    public void setSettingDao(SettingDao settingDao) {
        m_settingDao = settingDao;
    }

    public void setBranchManager(BranchManager branchManager) {
        m_branchManager = branchManager;
    }

}
