/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.root1.kad.knxservice;

/**
 *
 * @author achristian
 */
public class KnxServiceException extends Exception {

    KnxServiceException(String msg, Exception cause) {
        super(msg, cause);
    }

    public KnxServiceException(String message) {
        super(message);
    }
    
}
