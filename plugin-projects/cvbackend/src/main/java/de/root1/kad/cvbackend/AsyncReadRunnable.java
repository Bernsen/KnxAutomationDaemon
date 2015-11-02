/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.root1.kad.cvbackend;

import de.root1.kad.knxservice.KnxService;
import de.root1.kad.knxservice.KnxServiceException;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author achristian
 */
public class AsyncReadRunnable implements Runnable {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final NanoHttpdSSE.SseResponse sse;
    private final KnxService knx;
    private final String address;

    public AsyncReadRunnable(NanoHttpdSSE.SseResponse sse, KnxService knx, String address) {
        this.sse = sse;
        this.knx = knx;
        this.address = address;
    }

    @Override
    public void run() {
        try {
            String value = knx.read(address);
            JSONObject response = new JSONObject();
            if (value != null) {
                JSONObject jsonResponse = new JSONObject();
                JSONObject jsonData = new JSONObject();
                jsonResponse.put("d", jsonData);
                jsonResponse.put("i", "1");
                log.info("async response: {}", jsonResponse.toJSONString());
                sse.sendMessage(null, null, jsonResponse.toJSONString());
            } else {
                log.warn("Cannot read '" + address + "' async. Timeout?");
            }
        } catch (KnxServiceException ex) {
            log.error("Error reading data from '"+address+"'", ex);
        }
    }

}
