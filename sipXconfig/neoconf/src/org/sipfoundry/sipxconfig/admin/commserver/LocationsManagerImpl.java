/*
 *
 *
 * Copyright (C) 2008 Pingtel Corp., certain elements licensed under a Contributor Agreement.
 * Contributors retain copyright to elements licensed under a Contributor Agreement.
 * Licensed to the User under the LGPL license.
 *
 *
 */
package org.sipfoundry.sipxconfig.admin.commserver;

import java.util.ArrayList;
import java.util.List;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.sipfoundry.sipxconfig.common.SipxHibernateDaoSupport;
import org.sipfoundry.sipxconfig.common.UserException;
import org.sipfoundry.sipxconfig.nattraversal.NatLocation;
import org.sipfoundry.sipxconfig.service.SipxService;
import org.springframework.orm.hibernate3.HibernateCallback;

import static org.springframework.dao.support.DataAccessUtils.intResult;
import static org.springframework.dao.support.DataAccessUtils.singleResult;

public class LocationsManagerImpl extends SipxHibernateDaoSupport<Location> implements LocationsManager {
    public static final Log LOG = LogFactory.getLog(LocationsManagerImpl.class);
    private static final String LOCATION_PROP_NAME = "fqdn";
    private static final String LOCATION_PROP_PRIMARY = "primary";
    private static final String LOCATION_PROP_IP = "ipAddress";
    private static final String LOCATION_PROP_ID = "locationId";
    private static final String DUPLICATE_FQDN_OR_IP = "&error.duplicateFqdnOrIp";
    private static final String HOST = "localhost";
    private static final int PORT = 27017;
    private static final String DB_NAME = "imdb";
    private static final String DB_COLLECTION_NAME = "node";
    private static final String UNABLE_OPEN_MONGO = "Unable to open mongo connection on: ";
    private static final String COLON = ":";
    private Mongo m_mongoInstance;

    private void initMongo() throws Exception {
        if (m_mongoInstance == null) {
            try {
                m_mongoInstance = new Mongo(HOST, PORT);
            } catch (Exception e) {
                LOG.error(UNABLE_OPEN_MONGO + HOST + COLON + PORT);
                throw (e);
            }
        }
    }

    /** Return the replication URLs, retrieving them on demand */
    public Location[] getLocations() {
        List<Location> locationList = getHibernateTemplate().loadAll(Location.class);
        Location[] locationArray = new Location[locationList.size()];
        locationList.toArray(locationArray);
        return locationArray;
    }

    public Location getLocation(int id) {
        return (Location) getHibernateTemplate().load(Location.class, id);
    }

    public Location getLocationByFqdn(String fqdn) {
        return loadLocationByUniqueProperty(LOCATION_PROP_NAME, fqdn);
    }

    public Location getLocationByAddress(String address) {
        return loadLocationByUniqueProperty("address", address);
    }

    private Location loadLocationByUniqueProperty(String propName, Object propValue) {
        final Criterion expression = Restrictions.eq(propName, propValue);

        HibernateCallback callback = new HibernateCallback() {
            public Object doInHibernate(Session session) {
                Criteria criteria = session.createCriteria(Location.class).add(expression);
                return criteria.list();
            }
        };
        List<Location> locations = getHibernateTemplate().executeFind(callback);
        Location location = (Location) singleResult(locations);

        return location;
    }

    public void storeMigratedLocation(Location location) {
        getHibernateTemplate().saveOrUpdate(location);
    }

    public void saveNatLocation(Location location, NatLocation nat) {
        location.setNat(nat);
        getHibernateTemplate().saveOrUpdate(location);
        // There is a 1-1 relation between Nat and Location
        nat.setLocation(location);
    }

    public void saveServerRoleLocation(Location location, ServerRoleLocation role) {
        location.setServerRoles(role);
        getHibernateTemplate().saveOrUpdate(location);
        role.setLocation(location);
    }

    public void saveLocation(Location location) {
        if (location.isNew()) {
            if (isFqdnOrIpInUseExceptThis(location)) {
                throw new UserException(DUPLICATE_FQDN_OR_IP, location.getFqdn(), location.getAddress());
            }
            getHibernateTemplate().save(location);
        } else {
            if (isFqdnOrIpChanged(location) && isFqdnOrIpInUseExceptThis(location)) {
                throw new UserException(DUPLICATE_FQDN_OR_IP, location.getFqdn(), location.getAddress());
            }
            getHibernateTemplate().update(location);
        }
        // TODO: create a context for mongo
        // TODO: move this code in replicationtrigger or similar;
        try {
            if (location.isRegistered()) {
                initMongo();
                DB datasetDb = m_mongoInstance.getDB(DB_NAME);
                DBCollection nodeCollection = datasetDb.getCollection(DB_COLLECTION_NAME);
                DBObject search = new BasicDBObject();
                search.put("id", location.getId());
                DBCursor cursor = nodeCollection.find(search);
                DBObject node = new BasicDBObject();
                if (cursor.hasNext()) {
                    node = cursor.next();
                }
                node.put("ip", location.getAddress());
                node.put("dsc", location.getName());
                node.put("mstr", location.isPrimary());
                nodeCollection.save(node);
            }
        } catch (Exception e) {
            throw new UserException("Cannot register location in mongo db: " + e);
        }
    }

    /**
     * Need to verify if existing fqdn or ip are about to be changed in order to be in sync with
     * potential situations for versions before 4.1.6 when an user may have at least two locations
     * with the same fqdn or ip. (This situation probably will never appear but we have to be
     * sure). If no ip/fqdn change occurs, no user exception is thrown no matter if there is at
     * least one more location with the same ip or fqdn
     */
    private boolean isFqdnOrIpChanged(Location location) {
        List count = getHibernateTemplate().findByNamedQueryAndNamedParam("sameLocationWithSameFqdnOrIp",
                new String[] {
                    LOCATION_PROP_ID, LOCATION_PROP_NAME, LOCATION_PROP_IP
                }, new Object[] {
                    location.getId(), location.getFqdn(), location.getAddress()
                });

        return intResult(count) == 0;
    }

    private boolean isFqdnOrIpInUseExceptThis(Location location) {
        List count = getHibernateTemplate().findByNamedQueryAndNamedParam(
                "anotherLocationWithSameFqdnOrIpExceptThis", new String[] {
                    LOCATION_PROP_ID, LOCATION_PROP_NAME, LOCATION_PROP_IP
                }, new Object[] {
                    location.getId(), location.getFqdn(), location.getAddress()
                });

        return intResult(count) > 0;
    }

    public void deleteLocation(Location location) {
        if (location.isPrimary()) {
            throw new UserException("&error.delete.primary", location.getFqdn());
        }
        getHibernateTemplate().delete(location);
    }

    public Location getPrimaryLocation() {
        return loadLocationByUniqueProperty(LOCATION_PROP_PRIMARY, true);
    }

    /**
     * Convenience method used only in tests for resetting primary location when needed
     *
     * @see TestPage.resetPrimaryLocation
     */
    public void deletePrimaryLocation() {
        Location location = getPrimaryLocation();
        if (location != null) {
            getHibernateTemplate().delete(location);
        } else {
            return;
        }
    }

    public List<Location> getLocationsForService(SipxService service) {
        List<Location> locations = new ArrayList<Location>();
        for (Location location : getLocations()) {
            if (location.isServiceInstalled(service)) {
                locations.add(location);
            }
        }
        return locations;
    }

    public Location getLocationByBundle(String bundleName) {
        List<Location> locations = getHibernateTemplate().findByNamedQueryAndNamedParam("locationsByBundle",
                "locationBundle", bundleName);
        if (locations.isEmpty()) {
            return null;
        }
        return locations.get(0);
    }
}
