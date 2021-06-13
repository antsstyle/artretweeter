/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package antsstyle.artretweeter.datastructures;

/**
 *
 * @author antss
 */
public enum CollectionOperation {
    
    ADD("add"),
    REMOVE("remove"),
    MOVE("move");

    private final String parameterName;

    public String getParameterName() {
        return parameterName;
    }

    private CollectionOperation(String parameterName) {
        this.parameterName = parameterName;
    }
    
    public static boolean isCollectionOperation(String s) {
        String s1 = s.toLowerCase();
        return s1.equals(ADD.parameterName) || s1.equals(REMOVE.parameterName) || s1.equals(MOVE.parameterName);
    }
    
}
