/*
 * 
 * 
 * Copyright (C) 2007 Pingtel Corp., certain elements licensed under a Contributor Agreement.  
 * Contributors retain copyright to elements licensed under a Contributor Agreement.
 * Licensed to the User under the LGPL license.
 * 
 * $
 */
package org.sipfoundry.sipxconfig.site.user;

import static org.sipfoundry.sipxconfig.site.SiteTestHelper.getBaseUrl;
import junit.framework.Test;
import net.sourceforge.jwebunit.html.Table;
import net.sourceforge.jwebunit.junit.WebTestCase;

import org.sipfoundry.sipxconfig.site.SiteTestHelper;
import org.sipfoundry.sipxconfig.site.TestPage;
import org.sipfoundry.sipxconfig.site.conference.ConferenceTestHelper;

public class UserConferencesTestUi extends WebTestCase {

    private static final String TEST_CONFERENCE_NAME = "200-test-conf";
    
    private ConferenceTestHelper m_helper;
    
    public static Test suite() throws Exception {
        return SiteTestHelper.webTestSuite(UserConferencesTestUi.class);
    }    
    
    protected void setUp() throws Exception {
        m_helper = new ConferenceTestHelper(getTester());
        getTestContext().setBaseUrl(getBaseUrl());
        SiteTestHelper.home(tester);
        SiteTestHelper.setScriptingEnabled(tester, true);
    }    
    
    public void testUserConferences() {
        // First create a test bridge and conference, assigned to the test user.
        m_helper.createBridge("testbridge");
        createTestConference();
        
        // Test that the conference appears under the user's conference list, and that the name and extension
        // are not editable.
        SiteTestHelper.home(tester);
        clickLink("loginFirstTestUser");
        clickLink("EditMyInformation");
        clickLink("link:conferences");
        Table conferencesTable = getTable("conference:list");
        assertEquals(2, conferencesTable.getRowCount()); // there should only be one conference assigned to this user
        assertEquals(TEST_CONFERENCE_NAME, SiteTestHelper.getCellAsText(conferencesTable, 1, 1));
        
        clickLinkWithText(TEST_CONFERENCE_NAME);
        assertElementNotPresent("item:name");
        assertElementNotPresent("item:extension");
        
    }
    
    private void createTestConference() {
        clickLink("link:conferences");
        setWorkingForm("form");
        SiteTestHelper.clickSubmitLink(tester, "conference:add");
        clickButton("assign");
        setTextField("userSearch:query", TestPage.TEST_USER_ALIAS1);
        clickButton("user:search");
        checkCheckbox("checkbox");
        submit("user:select");
        
        // Should be back to the create conference page.
        setTextField("item:name", TEST_CONFERENCE_NAME);
        setTextField("item:extension", "1200");
        submit("form:ok");
    }
    
}
