package com.geekyouup.android.ustopwatch;

import com.google.android.gcm.server.Constants;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.datanucleus.query.JPACursorHelper;
import java.util.logging.Logger;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.Query;

@Api(name = "stopwatchstateendpoint", namespace = @ApiNamespace(ownerDomain = "geekyouup.com", ownerName = "geekyouup.com", packagePath = "android.ustopwatch"))
public class StopWatchStateEndpoint {


    private static final Logger log = Logger.getLogger(StopWatchStateEndpoint.class.getName());

    /**
     * This method lists all the entities inserted in datastore.
     * It uses HTTP GET method and paging support.
     *
     * @return A CollectionResponse class containing the list of all entities
     *         persisted and a cursor to the next page.
     */
    @SuppressWarnings({"unchecked", "unused"})
    @ApiMethod(name = "listStopWatchState")
    public CollectionResponse<StopWatchState> listStopWatchState(
            @Nullable @Named("cursor") String cursorString,
            @Nullable @Named("limit") Integer limit) {

        EntityManager mgr = null;
        List<StopWatchState> execute = null;

        try {
            mgr = getEntityManager();
            Query query = mgr.createQuery("select from StopWatchState as StopWatchState");
            Cursor cursor;
            if (cursorString != null && cursorString.trim().length() > 0) {
                cursor = Cursor.fromWebSafeString(cursorString);
                query.setHint(JPACursorHelper.CURSOR_HINT, cursor);
            }

            if (limit != null) {
                query.setFirstResult(0);
                query.setMaxResults(limit);
            }

            execute = (List<StopWatchState>) query.getResultList();
            cursor = JPACursorHelper.getCursor(execute);
            if (cursor != null) cursorString = cursor.toWebSafeString();

            // Tight loop for fetching all entities from datastore and accomodate
            // for lazy fetch.
            for (StopWatchState obj : execute) ;
        } finally {
            if (mgr != null) {
                mgr.close();
            }
        }

        return CollectionResponse.<StopWatchState>builder()
                .setItems(execute)
                .setNextPageToken(cursorString)
                .build();
    }

    /**
     * This method gets the entity having primary key id. It uses HTTP GET method.
     *
     * @param id the primary key of the java bean, empty for last entry
     * @return The entity with primary key id.
     */
    @ApiMethod(name = "getStopWatchState")
    public StopWatchState getStopWatchState(@Named("id") Long id) {
        EntityManager mgr = getEntityManager();
        StopWatchState stopWatchState = null;
        try {
            if(id == -1) {
                Query query = mgr.createQuery("select from StopWatchState as StopWatchState order by serverTimestamp desc");
                query.setFirstResult(0);
                query.setMaxResults(1);

                List<StopWatchState> execute = (List<StopWatchState>) query.getResultList();
                if(execute != null && execute.size() == 1) {
                    stopWatchState = execute.get(0);
                }
            }
            else {
                stopWatchState = mgr.find(StopWatchState.class, id);
            }
        } finally {
            mgr.close();
        }
        return stopWatchState;
    }

    /**
     * This inserts a new entity into App Engine datastore. If the entity already
     * exists in the datastore, an exception is thrown.
     * It uses HTTP POST method.
     *
     * @param stopWatchState the entity to be inserted.
     * @return The inserted entity.
     */
    @ApiMethod(name = "insertStopWatchState")
    public StopWatchState insertStopWatchState(StopWatchState stopWatchState) throws IOException {
        EntityManager mgr = getEntityManager();
        try {
            if(stopWatchState.getKey() != null) {
                if (containsStopWatchState(stopWatchState)) {
                    throw new EntityExistsException("Object already exists");
                }
            }
            stopWatchState.setServerTimestamp(System.currentTimeMillis());
            mgr.persist(stopWatchState);

            Sender sender = new Sender(API_KEY);
            CollectionResponse<DeviceInfo> response = endpoint.listDeviceInfo(null,
                    10);
            for (DeviceInfo deviceInfo : response.getItems()) {
               // if(!deviceInfo.getDeviceRegistrationID().equals(stopWatchState.getSourceDevice())) {
                  try {
                    log.info("tickling device + " + deviceInfo.getDeviceInformation());
                    doSendViaGcm("tickle", sender, deviceInfo);
                  } catch (Exception e)
                  {
                      log.severe("can't pint device " + deviceInfo + "error " + e.toString());
                  }
               // }
            }
        } finally {
            mgr.close();
        }
        return stopWatchState;
    }

    private static final String API_KEY = "AIzaSyBEKkKCL4NwEG18AIhXtLKI5q8wfmuaaIU";
    private static final DeviceInfoEndpoint endpoint = new DeviceInfoEndpoint();
    private static Result doSendViaGcm(String message, Sender sender,
                                       DeviceInfo deviceInfo) throws IOException {
        // Trim message if needed.
        if (message.length() > 1000) {
            message = message.substring(0, 1000) + "[...]";
        }

        // This message object is a Google Cloud Messaging object, it is NOT
        // related to the MessageData class
        Message msg = new Message.Builder().addData("message", message).build();
        Result result = sender.send(msg, deviceInfo.getDeviceRegistrationID(),
                5);
        if (result.getMessageId() != null) {
            String canonicalRegId = result.getCanonicalRegistrationId();
            if (canonicalRegId != null) {
                endpoint.removeDeviceInfo(deviceInfo.getDeviceRegistrationID());
                deviceInfo.setDeviceRegistrationID(canonicalRegId);
                endpoint.insertDeviceInfo(deviceInfo);
            }
        } else {
            String error = result.getErrorCodeName();
            if (error.equals(Constants.ERROR_NOT_REGISTERED)) {
                endpoint.removeDeviceInfo(deviceInfo.getDeviceRegistrationID());
            }
        }

        return result;
    }
    /**
     * This method is used for updating an existing entity. If the entity does not
     * exist in the datastore, an exception is thrown.
     * It uses HTTP PUT method.
     *
     * @param stopWatchState the entity to be updated.
     * @return The updated entity.
     */
    @ApiMethod(name = "updateStopWatchState")
    public StopWatchState updateStopWatchState(StopWatchState stopWatchState) {
        EntityManager mgr = getEntityManager();
        try {
            if (!containsStopWatchState(stopWatchState)) {
                throw new EntityNotFoundException("Object does not exist");
            }
            mgr.persist(stopWatchState);
        } finally {
            mgr.close();
        }
        return stopWatchState;
    }

    /**
     * This method removes the entity with primary key id.
     * It uses HTTP DELETE method.
     *
     * @param id the primary key of the entity to be deleted.
     * @return The deleted entity.
     */
    @ApiMethod(name = "removeStopWatchState")
    public StopWatchState removeStopWatchState(@Named("id") Long id) {
        EntityManager mgr = getEntityManager();
        StopWatchState stopWatchState = null;
        try {
            stopWatchState = mgr.find(StopWatchState.class, id);
            mgr.remove(stopWatchState);
        } finally {
            mgr.close();
        }
        return stopWatchState;
    }

    private boolean containsStopWatchState(StopWatchState stopWatchState) {
        EntityManager mgr = getEntityManager();
        boolean contains = true;
        try {
            StopWatchState item = mgr.find(StopWatchState.class, stopWatchState.getKey());
            if (item == null) {
                contains = false;
            }
        } finally {
            mgr.close();
        }
        return contains;
    }

    private static EntityManager getEntityManager() {
        return EMF.get().createEntityManager();
    }

}
