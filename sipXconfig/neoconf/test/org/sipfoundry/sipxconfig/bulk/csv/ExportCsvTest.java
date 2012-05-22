/*
 *
 *
 * Copyright (C) 2008 Pingtel Corp., certain elements licensed under a Contributor Agreement.
 * Contributors retain copyright to elements licensed under a Contributor Agreement.
 * Licensed to the User under the LGPL license.
 *
 *
 */
package org.sipfoundry.sipxconfig.bulk.csv;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;

import junit.framework.TestCase;

import org.sipfoundry.sipxconfig.common.User;
import org.sipfoundry.sipxconfig.phone.Line;
import org.sipfoundry.sipxconfig.phone.LineInfo;
import org.sipfoundry.sipxconfig.phone.TestPhone;
import org.sipfoundry.sipxconfig.phone.TestPhoneModel;
import org.sipfoundry.sipxconfig.phonebook.Address;
import org.sipfoundry.sipxconfig.phonebook.AddressBookEntry;
import org.sipfoundry.sipxconfig.setting.Group;

public class ExportCsvTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testExportPhoneNoLines() throws Exception {
        ExportCsv exportCsv = new ExportCsv();

        StringWriter writer = new StringWriter();
        SimpleCsvWriter csv = new SimpleCsvWriter(writer);

        TestPhone phone = new TestPhone();
        phone.setModel(new TestPhoneModel());
        phone.setSerialNumber("665544332211");
        phone.setDescription("phone description");
        Collection<String> userIds = exportCsv.exportPhone(csv, phone, "example.org");
        assertEquals(0, userIds.size());
        assertEquals(
                "\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"665544332211\",\"testPhoneModel\",\"\",\"phone description\"," +
                "\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\"\n",
                writer.toString());
    }

    public void testExportPhone() throws Exception {
        ExportCsv exportCsv = new ExportCsv();

        StringWriter writer = new StringWriter();
        SimpleCsvWriter csv = new SimpleCsvWriter(writer);

        // Add 3 user Groups to test export of multiple groups
        Group[] groups = new Group[3];
        groups[0] = new Group();
        groups[0].setName("ug1");
        groups[1] = new Group();
        groups[1].setName("ug2");
        groups[2] = new Group();
        groups[2].setName("ug3");

        User user = new User();
        user.setFirstName("John");
        user.setLastName("Lennon");
        user.setPin("1234");
        user.setSipPassword("sip_pass");
        user.setUserName("jlennon");
        user.setEmailAddress("jlennon@gmail.com");
        user.setImId("imId");
        user.setGroupsAsList(Arrays.asList(groups));

        Line line = new Line();
        line.setUser(user);

        // Add 4 phone Groups to test export of multiple groups
        // Note: phonegroup3, has a comma in the name. This is to make sure the double quotes are
        // around the field
        // and the comma is not taken as a seperator for the csv file.
        Group[] phoneGroups = new Group[4];
        phoneGroups[0] = new Group();
        phoneGroups[0].setName("phonegroup1");
        phoneGroups[1] = new Group();
        phoneGroups[1].setName("phonegroup2");
        phoneGroups[2] = new Group();
        phoneGroups[2].setName("phonegroup3,");
        phoneGroups[3] = new Group();
        phoneGroups[3].setName("phonegroup4");
        TestPhone phone = new TestPhone();
        phone.setModel(new TestPhoneModel());
        phone.setSerialNumber("665544332211");
        phone.setDescription("phone description");
        phone.setGroupsAsList(Arrays.asList(phoneGroups));
        phone.addLine(line);

        Collection<String> userIds = exportCsv.exportPhone(csv, phone, "example.org");
        assertEquals(1, userIds.size());
        assertTrue(userIds.contains("jlennon"));
        assertEquals(
                "\"jlennon\",\"example.org#1234\",\"sip_pass\",\"John\",\"Lennon\",\"\",\"jlennon@gmail.com\",\"ug1 ug2 ug3\",\"665544332211\",\"testPhoneModel\",\"phonegroup1 phonegroup2 phonegroup3, phonegroup4\",\"phone description\",\"imId\"," +
                "\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\"\n",
                writer.toString());
    }

    public void testExportPhoneExternalLine() throws Exception {
        ExportCsv exportCsv = new ExportCsv();

        StringWriter writer = new StringWriter();
        SimpleCsvWriter csv = new SimpleCsvWriter(writer);

        Line line = new Line();

        TestPhone phone = new TestPhone();
        phone.setModel(new TestPhoneModel());
        phone.setSerialNumber("665544332211");
        phone.setDescription("phone description");
        phone.addLine(line);

        LineInfo lineInfo = new LineInfo();
        lineInfo.setDisplayName("dp");
        lineInfo.setExtension("432");
        lineInfo.setRegistrationServer("example.com");
        line.setLineInfo(lineInfo);

        Collection<String> userIds = exportCsv.exportPhone(csv, phone, "example.org");
        assertEquals(0, userIds.size());
        assertEquals(
                "\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"665544332211\",\"testPhoneModel\",\"\",\"phone description\",\"\"" +
                ",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\"\n",
                writer.toString());
    }

    public void testExportUserNoEmail() throws Exception {
        ExportCsv exportCsv = new ExportCsv();

        StringWriter writer = new StringWriter();
        SimpleCsvWriter csv = new SimpleCsvWriter(writer);
        String[] row = Index.newRow();

        User user = new User();
        user.setFirstName("John");
        user.setLastName("Lennon");
        user.setPin("1234");
        user.setSipPassword("sip_pass");
        user.setUserName("jlennon");

        exportCsv.exportUser(csv, row, user, "example.org");
        assertEquals(
                "\"jlennon\",\"example.org#1234\",\"sip_pass\",\"John\",\"Lennon\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\"" +
                ",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\"\n",
                writer.toString());
    }

    public void testExportUserDetails() throws Exception {
        ExportCsv exportCsv = new ExportCsv();

        StringWriter writer = new StringWriter();
        SimpleCsvWriter csv = new SimpleCsvWriter(writer);
        String[] row = Index.newRow();

        User user = new User();
        user.setFirstName("John");
        user.setLastName("Lennon");
        user.setPin("1234");
        user.setSipPassword("sip_pass");
        user.setUserName("jlennon");

        AddressBookEntry addressBookEntry = new AddressBookEntry();
        addressBookEntry.setJobTitle("job title");
        addressBookEntry.setJobDept("job dept");
        addressBookEntry.setCompanyName("company name");
        addressBookEntry.setAssistantName("assistant name");
        addressBookEntry.setCellPhoneNumber("001122");
        addressBookEntry.setHomePhoneNumber("112233");
        addressBookEntry.setAssistantPhoneNumber("223344");
        addressBookEntry.setFaxNumber("33445566");
        addressBookEntry.setAlternateEmailAddress("alternate@gmail.com");
        addressBookEntry.setAlternateImId("alternateImId");
        addressBookEntry.setLocation("location");

        Address homeAddress = new Address();
        homeAddress.setStreet("home street");
        homeAddress.setCity("home city");
        homeAddress.setState("home state");
        homeAddress.setCountry("home country");
        homeAddress.setZip("34001");
        addressBookEntry.setHomeAddress(homeAddress);

        Address officeAddress = new Address();
        officeAddress.setStreet("office street");
        officeAddress.setCity("office city");
        officeAddress.setState("office state");
        officeAddress.setCountry("office country");
        officeAddress.setZip("34342");
        addressBookEntry.setOfficeAddress(officeAddress);

        user.setAddressBookEntry(addressBookEntry);

        exportCsv.exportUser(csv, row, user, "example.org");
        assertEquals(
                "\"jlennon\",\"example.org#1234\",\"sip_pass\",\"John\",\"Lennon\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\"" +
                ",\"job title\",\"job dept\",\"company name\",\"assistant name\"," +
                "\"001122\",\"112233\",\"223344\",\"33445566\",\"alternate@gmail.com\",\"alternateImId\",\"location\"," +
                "\"home street\",\"home city\",\"home state\",\"home country\",\"34001\"," +
                "\"office street\",\"office city\",\"office state\",\"office country\",\"34342\"\n",
                writer.toString());
    }
}
