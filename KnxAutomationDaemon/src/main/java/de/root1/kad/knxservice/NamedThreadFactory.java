/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.root1.kad.knxservice;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author achristian
 */
public class NamedThreadFactory implements ThreadFactory{
    
    private final String name;
    private AtomicInteger i = new AtomicInteger(0);

    public NamedThreadFactory(String name) {
        this.name = name;
    }
    
    @Override
    public Thread newThread(Runnable r) {
        int number = i.incrementAndGet();
        String threadName = name+"#"+number;
        return new Thread(r, threadName);
    }
    
}
