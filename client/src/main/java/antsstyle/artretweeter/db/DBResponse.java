/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package antsstyle.artretweeter.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashMap;

/**
 * Container class, that wraps all the different parts of a database query result into one object.
 *
 * @author Ant
 * @since 1.0
 */
public class DBResponse {

    private String message;
    private Object returnedObject;
    private ArrayList<HashMap<String, Object>> returnedRows;
    private Integer sqlResult;
    private DBResponseCode statusCode;
    private Exception thrownException;
    
    public Boolean wasSuccessful() {
        return statusCode.equals(DBResponseCode.SUCCESS);
    }

    public Exception getThrownException() {
        return thrownException;
    }

    public DBResponse setThrownException(Exception thrownException) {
        this.thrownException = thrownException;
        return this;
    }
    /**
     * Gets the ArtPoster response message.
     *
     * @return The response message that ArtPoster set.
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * Gets the object returned by the query result this DBResponse holds, if it contains one.
     * @return The object returned by the DB query this DBResponse holds.
     */
    public Object getReturnedObject() {
        return returnedObject;
    }
    /**
     * Gets the rows returned from the database query.
     *
     * @return A list of rows returned from the database query.
     */
    public ArrayList<HashMap<String, Object>> getReturnedRows() {
        return returnedRows;
    }
    public Integer getSQLResult() {
        return sqlResult;
    }

    /**
     * Gets the status code for this query.
     *
     * @return The status code (a success code or error code, as defined in the DBResponseCode class).
     */
    public DBResponseCode getStatusCode() {
        return statusCode;
    }


    /**
     * Sets the ArtPoster response message.
     *
     * @param message The message to set.
     * @return This object.
     */
    public DBResponse setMessage(String message) {
        this.message = message;
        return this;
    }
    /**
     * Sets the object returned by the DB query this DBResponse holds.
     * @param returnedObject The object to set.
     * @return This DBResponse object.
     */
    public DBResponse setReturnedObject(Object returnedObject) {
        this.returnedObject = returnedObject;
        return this;
    }

    /**
     * Sets the rows returned from the database query.
     *
     * @param returnedRows The list to set.
     * @return This object.
     */
    public DBResponse setReturnedRows(ArrayList<HashMap<String, Object>> returnedRows) {
        this.returnedRows = returnedRows;
        return this;
    }
    public DBResponse setSQLResult(Integer sqlResult) {
        this.sqlResult = sqlResult;
        return this;
    }
    /**
     * Sets the status code for this query.
     *
     * @param statusCode The DBResponseCode value to set.
     * @return This object.
     */
    public DBResponse setStatusCode(DBResponseCode statusCode) {
        this.statusCode = statusCode;
        return this;
    }

}
