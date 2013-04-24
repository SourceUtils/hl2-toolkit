package com.timepath.steam.net;

/**
 *
 * @author timepath
 */
public interface ServerListener {
    
    public static ServerListener DUMMY = new ServerListener() {

        public void inform(String update) {
        }
        
    };
    
    /**
     *
     * @param update
     */
    public void inform(String update);
    
}
