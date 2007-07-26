/*
 * 
 * 
 * Copyright (C) 2007 Pingtel Corp., certain elements licensed under a Contributor Agreement.  
 * Contributors retain copyright to elements licensed under a Contributor Agreement.
 * Licensed to the User under the LGPL license.
 * 
 * $
 */
package org.sipfoundry.sipxconfig.admin.commserver;

import java.util.List;

import org.sipfoundry.sipxconfig.admin.commserver.imdb.RegistrationItem;

public interface RegistrationContext {

    public abstract List<RegistrationItem> getRegistrations();

}
