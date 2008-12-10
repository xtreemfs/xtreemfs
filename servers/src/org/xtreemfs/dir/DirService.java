/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

 This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
 Grid Operating System, see <http://www.xtreemos.eu> for more details.
 The XtreemOS project has been developed with the financial support of the
 European Commission's IST program under contract #FP6-033576.

 XtreemFS is free software: you can redistribute it and/or modify it under
 the terms of the GNU General Public License as published by the Free
 Software Foundation, either version 2 of the License, or (at your option)
 any later version.

 XtreemFS is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Jan Stender (ZIB)
 */

package org.xtreemfs.dir;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.json.JSONString;

/**
 * Implements the functionality provided by the Directory Service.
 *
 * @author stender
 *
 */
public class DirService {

    public enum Attrs {
        uuid, version, lastUpdated, owner, type, name, organization, country, uri, publicKey
    }

    public static final String  TABLE_NAME  = "TABLE";

    public static final String  COL_UUID    = "UUID";

    public static final String  COL_ATTR    = "ATTR";

    public static final String  COL_VAL     = "VAL";

    public static final String  COL_MAPPING = "MAPPING";

    public static final String  COL_OWNER   = "OWNER";

    public static final String  COL_VERSION = "VERSION";

    private DIRRequestListener   requestListener;

    private final Connection    conEntities;

    private final Connection    conMappings;

    private Map<String, Object> timestamps;

    /**
     * Creates a new Directory Service using a database stored at the given path
     * in the local file system tree.
     *
     * @param dbPath
     *            the path to the database directory
     * @throws SQLException
     *             if the database could not be initialized properly
     */
    public DirService(String dbPath) throws SQLException {

        this.timestamps = new HashMap<String, Object>();

        try {
            Class.forName("org.hsqldb.jdbcDriver");
        } catch (Exception e) {
            Logging.logMessage(Logging.LEVEL_ERROR, this,
                "ERROR: failed to load HSQLDB JDBC driver.");
            throw new RuntimeException(e);
        }

        new File(dbPath).mkdirs();

        Properties info = new Properties();
        info.setProperty("shutdown", "true");
        info.setProperty("user", "sa");
        conEntities = DriverManager.getConnection("jdbc:hsqldb:file:" + dbPath + "/ds-entities",
            info);
        conEntities.setAutoCommit(true);

        conMappings = DriverManager.getConnection("jdbc:hsqldb:file:" + dbPath + "/ds-mappings",
            info);
        conMappings.setAutoCommit(true);

        // check whether the entities table exists already
        // if the table does not exist yet, create it
        if (!tableExists(conEntities)) {
            String sql = "CREATE TABLE " + TABLE_NAME + " (" + COL_UUID + " VARCHAR(128) NOT NULL,"
                + COL_ATTR + " VARCHAR(128) NOT NULL," + COL_VAL + " VARCHAR(1024) NOT NULL);";

            Statement statement = conEntities.createStatement();
            statement.execute(sql);
            statement.close();
        }

        // check whether the mappings table exists already
        // if the table does not exist yet, create it
        if (!tableExists(conMappings)) {
            String sql = "CREATE TABLE " + TABLE_NAME + " (" + COL_UUID + " VARCHAR(128) NOT NULL,"
                + COL_OWNER + " VARCHAR(128) NOT NULL," + COL_MAPPING + " VARCHAR(1024) NOT NULL,"
                + COL_VERSION + " INTEGER NOT NULL);";

            Statement statement = conMappings.createStatement();
            statement.execute(sql);
            statement.close();
        }

    }

    public void shutdown() throws SQLException {
        conMappings.createStatement().execute("shutdown");
        conMappings.close();
        conEntities.createStatement().execute("shutdown");
        conEntities.close();
    }

    /**
     * Registers or updates an entity at the Directory Service.
     *
     * <p>
     * First, an authorization check is performed. Access is always granted if
     * no entity with the given UUID exists yet. If an entity already exists,
     * access is only granted if the user ID associated with the request is
     * equal to the user ID associated with the existing entity.
     *
     * <p>
     * If the request is sufficiently authorized and the entity exists already,
     * <tt>oldVersion</tt> is compared to the version which is currently
     * associated with the entity. Unless both version strings are equal,
     * registration fails with an error message indicating that an attempt was
     * made to update an entry with an outdated version.
     *
     * <p>
     * If authorization and version check are successful, all entries given in
     * <tt>data</tt> are atomically updated. This includes a calculation of a
     * new version string, as well as an update of the 'lastUpdated' attribute.
     *
     * @param request
     *            the request context
     * @param uuid
     *            the UUID of the entity
     * @param data
     *            a map containing attribute-value pairs defining the entity
     * @param oldVersion
     *            the former version number of the entry, which the update
     *            refers to
     * @throws SQLException
     *             if an error occured while updating the database
     * @throws UserException
     *             if the operation failed due to an invalid argument
     */
    public void registerEntity(DIRRequest request, String uuid, Map<String, Object> data,
        long oldVersion) throws SQLException, UserException {

        Statement statement = conEntities.createStatement();

        try {

            conEntities.setAutoCommit(false);

            // check if an owner has already been defined for the entry;
            // if so, check if the user requesting the update is authorized to
            // modify the entry
            boolean ownerExists = false;

            String sql = SQLQueryHelper.createQueryStatement(TABLE_NAME, new String[] { COL_VAL },
                COL_UUID + "='" + uuid + "' and " + COL_ATTR + "='" + Attrs.owner + "'");

            ResultSet rs = statement.executeQuery(sql);
            try {
                if (rs.next()) {
                    ownerExists = true;
                    String owner = rs.getString(1);
                    checkAuthorization(owner, request, uuid);
                }
            } finally {
                rs.close();
            }

            // check if the user has the correct version to update
            if (ownerExists && !data.isEmpty()) {
                sql = SQLQueryHelper.createQueryStatement(TABLE_NAME, new String[] { COL_VAL },
                    COL_UUID + "='" + uuid + "' and " + COL_ATTR + "='" + Attrs.version + "'");
                rs = statement.executeQuery(sql);

                try {
                    if (rs.next()) {

                        // check the version
                        long versionInDB = rs.getLong(1);
                        if (versionInDB != oldVersion)
                            throw new UserException(
                                "version mismatch: received update for version '" + oldVersion
                                    + "', latest version is '" + versionInDB + "'");
                    }
                } finally {
                    rs.close();
                }
            }

            long timestamp = System.currentTimeMillis() / 1000;

            // add the owner entry if it does not exist
            if (!ownerExists) {
                // insert the new attribute-value pair
                sql = SQLQueryHelper.createInsertStatement(TABLE_NAME, null, new Object[] { uuid,
                    Attrs.owner.toString(), request.details.userId });
                statement.executeUpdate(sql);
            }

            for (String attr : data.keySet()) {

                if (attr.equals(Attrs.version.toString())
                    || attr.equals(Attrs.lastUpdated.toString()))
                    throw new UserException("invalid attribute name: '" + attr
                        + "' cannot be changed explicitly");

                // delete the former attribute-value pair, if existing
                sql = SQLQueryHelper.createDeleteStatement(TABLE_NAME, COL_UUID + "='" + uuid
                    + "' and " + COL_ATTR + "='" + attr + "'");
                statement.executeUpdate(sql);

                // if a value has been assigned, insert the new attribute-value
                // pair
                String value = (String) data.get(attr);

                if (value != null && value.length() > 0) {

                    sql = SQLQueryHelper.createInsertStatement(TABLE_NAME, null, new Object[] {
                        uuid, attr, value });
                    statement.executeUpdate(sql);
                }

            }

            // calculate the new version number
            long version = oldVersion + 1;

            // delete the former version number
            sql = SQLQueryHelper.createDeleteStatement(TABLE_NAME, COL_UUID + "='" + uuid
                + "' and " + COL_ATTR + "='" + Attrs.version + "'");
            statement.executeUpdate(sql);

            // update the version number
            sql = SQLQueryHelper.createInsertStatement(TABLE_NAME, new String[] { COL_UUID,
                COL_ATTR, COL_VAL }, new Object[] { uuid, Attrs.version.toString(), version });
            statement.executeUpdate(sql);

            // update the timestamp
            timestamps.put(uuid, timestamp);

            // commit the transaction
            conEntities.commit();

            MessageUtils.marshallResponse(request, version);
            this.notifyRequestListener(request);

        } catch (UserException exc) {
            conEntities.rollback();
            throw exc;
        } catch (SQLException exc) {
            conEntities.rollback();
            throw exc;
        } finally {
            statement.close();
        }

    }

    /**
     * Queries the Directory Service.
     *
     * <p>
     * The method returns a set of entities in the form of a mapping from UUIDs
     * to maps containing sets of attribute-value pairs associated with the
     * corresponding UUIDs.
     *
     * <p>
     * The set of entities included in the result set is restricted by the query
     * map. Only such entities are included that match the query map, i.e. that
     * have attribute-value pairs equal to or at least covered by patterns
     * contained in the query map. Entities are only included in the result set
     * if each attribute from the query map is also attached to the entity.
     * Similarly, each value mapped by an attribute in the query map must also
     * be mapped by an attribute attached to the entity, with the exception that
     * an asterisk ('*') indicates that any value is allowed.
     *
     * <p>
     * The attributes included in the result set are restricted by the given
     * list of attributes. If this list is <tt>null</tt> or empty, all
     * attributes of all matching entities are included.
     *
     * @param request
     *            the request context
     * @param queryMap
     *            a mapping defining the query
     * @param attrs
     *            a set of attributes to which all entities included in the
     *            result set are reduced
     *
     * @throws SQLException
     *             if an error occured while querying the database
     */
    public void getEntities(DIRRequest request, Map<String, Object> queryMap, List<String> attrs)
        throws SQLException {

        // TODO: check whether some fancy SQL statement will perform this task
        // more efficiently

        Statement statement = conEntities.createStatement();

        try {

            // first, get a list of potential UUIDs which might belong to the
            // query
            // result; most probably, this will significantly reduce the amount
            // of
            // entries to be checked in the second step
            StringBuffer sb = new StringBuffer();
            for (String key : queryMap.keySet()) {

                String value = (String) queryMap.get(key);

                if (sb.length() != 0)
                    sb.append("OR ");

                if (key.equalsIgnoreCase(Attrs.uuid.toString()) && !value.equals("*")) {
                    sb.append("(");
                    sb.append(COL_UUID);
                    sb.append("='");
                    sb.append(value);
                    sb.append("')");
                    continue;
                }

                if (!key.equalsIgnoreCase(Attrs.lastUpdated.toString())) {

                    sb.append("(");
                    sb.append(COL_ATTR);
                    sb.append("='");
                    sb.append(key);
                    sb.append("'");

                    if (!value.equals("*")) {
                        sb.append(" AND ");
                        sb.append(COL_VAL);
                        sb.append("='");
                        sb.append(value);
                        sb.append("'");
                    }

                    sb.append(")");
                }

            }

            String sql = SQLQueryHelper.createQueryStatement(TABLE_NAME, new String[] { COL_UUID },
                sb.toString());
            ResultSet rs = statement.executeQuery(sql);

            Set<String> uuids = new HashSet<String>();
            try {
                while (rs.next())
                    uuids.add(rs.getString(1));
            } finally {
                rs.close();
            }

            // for each potential entry, check whether all requirements are
            // fulfilled; if so, add the entry to the result set
            Map<String, Map<String, Object>> result = new HashMap<String, Map<String, Object>>();
            for (String uuid : uuids) {

                // get all entities with the given UUID from the database
                sql = SQLQueryHelper.createQueryStatement(TABLE_NAME, new String[] { COL_ATTR,
                    COL_VAL }, COL_UUID + "='" + uuid + "'");
                rs = statement.executeQuery(sql);

                // add all entries from the database
                Map<String, Object> entity = new HashMap<String, Object>();
                entity.put(Attrs.uuid.toString(), uuid);
                try {
                    while (rs.next())
                        entity.put(rs.getString(1), rs.getString(2));
                } finally {
                    rs.close();
                }

                // add the lastUpdated entry from the timestamp map if it exists
                if (timestamps.containsKey(uuid))
                    entity.put(Attrs.lastUpdated.toString(), timestamps.get(uuid).toString());

                // if the entry matches the query map, remove all
                // attribute-value
                // pairs not defined in 'attrs' and add the resulting entry to
                // the
                // result set
                if (matches(entity, queryMap)) {

                    if (attrs == null || attrs.size() == 0)
                        result.put(uuid, entity);

                    else {

                        // prune the result set with the aid of 'attrs'

                        Map<String, Object> prunedEntry = new HashMap<String, Object>();
                        for (String key : attrs) {
                            String value = (String) entity.get(key);
                            if (value != null)
                                prunedEntry.put(key, value);
                        }

                        result.put(uuid, prunedEntry);
                    }
                }
            }

            MessageUtils.marshallResponse(request, result);
            this.notifyRequestListener(request);

        } finally {
            statement.close();
        }
    }

    /**
     * Deregisters an entity from the Directory Service.
     *
     * <p>
     * If an entity with the given UUID exists, a check is performed whether the
     * user ID associated with the request is equal to the user ID associated
     * with the database entry. The deregistration will only be performed if
     * both user IDs match.
     *
     * @param request
     *            the request context
     * @param uuid
     *            the UUID of the entity to remove
     * @throws SQLException
     *             if an error occured while updating the database
     * @throws UserException
     *             if the operation failed due to an invalid argument
     */
    public void deregisterEntity(DIRRequest request, String uuid) throws SQLException, UserException {

        conEntities.setAutoCommit(true);
        Statement statement = conEntities.createStatement();

        // check if an owner has already been defined for the entry;
        // if so, check if the user requesting the deletion is the owner
        String sql = SQLQueryHelper.createQueryStatement(TABLE_NAME, new String[] { COL_VAL },
            COL_UUID + "='" + uuid + "' and " + COL_ATTR + "='" + Attrs.owner + "'");
        ResultSet rs = statement.executeQuery(sql);

        try {

            if (rs.next()) {
                String owner = rs.getString(1);
                checkAuthorization(owner, request, uuid);
            }

            // delete all attribute-value pairs associated with the UUID from
            // the database
            sql = SQLQueryHelper.createDeleteStatement(TABLE_NAME, COL_UUID + "='" + uuid + "'");
            statement.executeUpdate(sql);

            // remove last timestamp from the hash map
            timestamps.remove(uuid);

            MessageUtils.marshallResponse(request, null);
            this.notifyRequestListener(request);

        } finally {
            rs.close();
            statement.close();
        }
    }

    public void registerAddressMapping(DIRRequest request, String uuid,
        List<Map<String, Object>> mapping, long oldVersion) throws SQLException, UserException {

        Statement statement = conMappings.createStatement();

        try {

            conEntities.setAutoCommit(false);

            // First, check whether a mapping has been registered already. If
            // so, check whether the requesting user is authorized to change the
            // mapping.
            String sql = SQLQueryHelper.createQueryStatement(TABLE_NAME, new String[] { COL_OWNER,
                COL_VERSION }, COL_UUID + "='" + uuid + "'");

            ResultSet rs = statement.executeQuery(sql);
            long versionInDB = 0;
            String owner = null;
            try {
                if (rs.next()) {

                    // check whether user is authorized
                    owner = rs.getString(1);
                    checkAuthorization(owner, request, uuid);

                    // check whether version is correct
                    versionInDB = rs.getLong(2);
                    if (versionInDB != oldVersion)
                        throw new UserException("version mismatch: received update for version '"
                            + oldVersion + "', latest version is '" + versionInDB + "'");
                }
            } finally {
                rs.close();
            }
            
            // do not change the owner if the entry exists already
            if(owner == null)
                owner = request.details.userId;
            
            // delete the old mapping, if necessary
            sql = SQLQueryHelper.createDeleteStatement(TABLE_NAME, COL_UUID + "='" + uuid + "'");
            statement.executeUpdate(sql);

            // add the new mapping
            sql = SQLQueryHelper.createInsertStatement(TABLE_NAME, new String[] { COL_UUID,
                COL_OWNER, COL_MAPPING, COL_VERSION }, new Object[] { uuid, owner,
                JSONParser.writeJSON(mapping), oldVersion + 1 });
            statement.executeUpdate(sql);

            // commit the transaction
            conEntities.commit();

            MessageUtils.marshallResponse(request, oldVersion + 1);
            this.notifyRequestListener(request);

        } catch (UserException exc) {
            conEntities.rollback();
            throw exc;
        } catch (SQLException exc) {
            conEntities.rollback();
            throw exc;
        } catch (JSONException exc) {
            conEntities.rollback();
            throw new UserException("cannot convert map to JSON: " + exc);
        } finally {
            statement.close();
        }
    }

    public void getAddressMapping(DIRRequest request, String uuid) throws SQLException,
        JSONException {

        Statement statement = conMappings.createStatement();
        Map<String, List<Object>> results = new HashMap<String, List<Object>>();

        // get all entries
        if (uuid.equals("")) {

            // query all mappings
            String sql = SQLQueryHelper.createQueryStatement(TABLE_NAME, new String[] { COL_UUID,
                COL_VERSION, COL_MAPPING }, null);

            ResultSet rs = statement.executeQuery(sql);
            try {
                while (rs.next()) {
                    List<Object> result = new ArrayList<Object>(2);
                    result.add(rs.getLong(2)); // version
                    result.add(JSONParser.parseJSON(new JSONString(rs.getString(3)))); // mapping

                    results.put(rs.getString(1), result);
                }
            } finally {
                rs.close();
            }

        } else {

            // query the mapping with the given UUID
            String sql = SQLQueryHelper.createQueryStatement(TABLE_NAME, new String[] { COL_UUID,
                COL_VERSION, COL_MAPPING }, COL_UUID + "='" + uuid + "'");
            ResultSet rs = statement.executeQuery(sql);

            try {
                if (rs.next()) {
                    List<Object> result = new ArrayList<Object>(3);
                    result.add(rs.getLong(2)); // version
                    result.add(JSONParser.parseJSON(new JSONString(rs.getString(3)))); // mapping
                                                                                       // /
                                                                                       // /
                                                                                       // mapping

                    results.put(rs.getString(1), result);
                }
            } finally {
                rs.close();
            }
        }

        MessageUtils.marshallResponse(request, results);
        this.notifyRequestListener(request);
    }

    public void deregisterAddressMapping(DIRRequest request, String uuid) throws SQLException,
        UserException {

        conEntities.setAutoCommit(true);

        // First, check whether a mapping has been registered already. If
        // so, check whether the requesting user is authorized to change the
        // mapping.
        Statement statement = conMappings.createStatement();
        String sql = SQLQueryHelper.createQueryStatement(TABLE_NAME, new String[] { COL_OWNER },
            COL_UUID + "='" + uuid + "'");
        ResultSet rs = statement.executeQuery(sql);

        try {
            if (rs.next()) {
                String owner = rs.getString(1);
                checkAuthorization(owner, request, uuid);
            }

            // delete the mapping
            sql = SQLQueryHelper.createDeleteStatement(TABLE_NAME, COL_UUID + "='" + uuid + "'");
            statement.executeUpdate(sql);

            MessageUtils.marshallResponse(request, null);
            this.notifyRequestListener(request);

        } finally {
            rs.close();
            statement.close();
        }
    }

    /**
     * Returns the current system time in milliseconds since 1/1/70
     */
    public void getGlobalTime(DIRRequest request) {
        MessageUtils.marshallResponse(request, System.currentTimeMillis());
        this.notifyRequestListener(request);
    }

    public void setRequestListener(DIRRequestListener listener) {
        requestListener = listener;
    }

    protected void notifyRequestListener(DIRRequest request) {
        if (requestListener != null)
            requestListener.dsRequestDone(request);
        else
            throw new RuntimeException("listener must not be null!");
    }

    protected Map<String, Map<String, String>> getEntityDBDump() throws SQLException {

        Statement statement = conEntities.createStatement();
        String sql = "SELECT * FROM " + TABLE_NAME;
        ResultSet rs = statement.executeQuery(sql);

        Map<String, Map<String, String>> dump = new HashMap<String, Map<String, String>>();

        for (int i = 0; rs.next(); i++) {
            String uuid = rs.getString(1);
            Map<String, String> entry = dump.get(uuid);
            if (entry == null) {
                entry = new HashMap<String, String>();
                dump.put(uuid, entry);
            }

            entry.put(rs.getString(2), rs.getString(3));

            if (!entry.containsKey(Attrs.lastUpdated.toString())) {
                Object timeStamp = timestamps.get(uuid);
                if (timeStamp != null)
                    entry.put(Attrs.lastUpdated.toString(), timeStamp.toString());
            }
        }

        rs.close();

        return dump;
    }

    protected Map<String, Object[]> getMappingDBDump() throws SQLException, JSONException {

        Statement statement = conMappings.createStatement();
        String sql = "SELECT * FROM " + TABLE_NAME;
        ResultSet rs = statement.executeQuery(sql);

        Map<String, Object[]> dump = new HashMap<String, Object[]>();

        for (int i = 0; rs.next(); i++) {
            String uuid = rs.getString(1);
            dump.put(uuid, new Object[] { rs.getString(2),
                JSONParser.parseJSON(new JSONString(rs.getString(3))), rs.getLong(4) + "" });
        }

        rs.close();

        return dump;
    }

    /**
     * Checks whether a given entity matches a given query map.
     *
     * @param entity
     * @param query
     * @return
     */
    private boolean matches(Map<String, Object> entity, Map<String, Object> query) {

        for (String key : query.keySet()) {

            String value = (String) query.get(key);

            if (!entity.containsKey(key)
                || (!value.equals("*") && !value.equals((String) entity.get(key))))
                return false;
        }

        return true;
    }

    private static boolean tableExists(Connection con) throws SQLException {

        boolean exists = false;

        Statement statement = con.createStatement();
        try {
            ResultSet rs = statement.executeQuery("SELECT * FROM " + TABLE_NAME + ";");
            rs.close();
            exists = true;
        } catch (SQLException exc) {
            if (exc.getErrorCode() != -22) // table does not exist
                throw exc;
        } finally {
            statement.close();
        }

        return exists;
    }

    private static void checkAuthorization(String owner, DIRRequest request, String uuid)
        throws UserException {

        if (owner.equals(request.details.userId) || request.details.superUser)
            return;

        throw new UserException("authorization failure: '" + uuid + "' is owned by '" + owner
            + "', but attempted to be modified by '" + request.details.userId
            + "'. Entries may only be modified by their owner or a superuser.");
    }

    // public static void main(String[] args) throws Exception {
    //
    // DirService ds = new DirService("/tmp/dirservice/ds");
    //
    // // register a new entity
    // Map<String, Object> data = new HashMap<String, Object>();
    // data.put("someKey", "someValue");
    // data.put("anotherKey", "anotherValue");
    // DSRequest req = new DSRequest();
    // req.userId = "me";
    // ds.registerEntity(req, "theUUID", data, 0L);
    //
    // data.clear();
    // data.put("someKey", "bla");
    // ds.registerEntity(req, "anotherUUID", data, 0L);
    //
    // // query the Directory Service
    // Map<String, Object> query = new HashMap<String, Object>();
    // List<String> keys = new LinkedList<String>();
    // keys.add("lastUpdated");
    // query.put("someKey", "*");
    // query.put("anotherKey", "anotherValue");
    //
    // // Map<String, Map<String, Object>> result = ds.getEntities(req, query,
    // // keys);
    // // System.out.println(result);
    //
    // // deregister the entity
    // ds.deregisterEntity(req, "theUUID");
    // ds.deregisterEntity(req, "anotherUUID");
    // //
    // // // query again
    // // result = ds.getEntities(req, new HashMap<String, Object>(), keys);
    // // System.out.println(result);
    //
    // ds.shutdown();
    // }

}
