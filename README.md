# KnxAutomationDaemon

--- NOTE ---
* DOCUMENTATION AND DEVELOPMENT STILL IN PROGRESS
* Current state is: alpha, kind of instable
* 

## About

KnxAutomationDaemon (KAD) is a small Java based daemon, that helps you to improve your KNX home automation system. Current features:

* Logic engine: Write your own logic scripts with the power of Java. The required API is very small and easy to understand, but enables you to write (if you need to) very complex scripts (including filesystem access, network access, concurrency threading, ...)
* Backend for the CometVisu (http://www.cometvisu.org) with ServerSentEvents (SSE) support

## Planned features

* log data like temperatures, switches, ... to database
* provide stored data put into graphs
* KNX IP Tunneling support: Right no it's hardcoded to IP Routing. But the system is already prepared for IP Tunneling.

## Requirements

* Java JDK 8 (JRE is not enough, it must be a JDK)
* KNX IP Router (IP Tunneling is not yet available, but prepared)
* Any OS that supports Java 8 JDK, but Linux is preferred
* A system that can run your OS. Small low-power systems like RaspberryPi should be efficient enough.
 
### Tested platforms

* Debian Jessie AMD64
* RaspberryPi2 with Raspbian Jessie

## Installation (Linux):

* Download latest distribution archive: http://jenkins.root1.de/job/KnxAutomationDaemon/lastStableBuild/de.root1.kad$kad-distribution/ (zip, tar.gz, tar.bz2, whatever you prefer)
* Extract to ```/opt```
* run startup.sh in ```/opt/kad```
 
### Configuration (Linux):

* Export your KNX project with ETS 4* to ```/opt/kad/conf/knxproject.knxproj``` (* Version 5 should be ok as well, but is not yet tested)
* After first run, pls. check /opt/kad/conf/knxproject.knxproj.user.xml for any groupaddress that has no assigned DataPointType. Fill in missing DPTs like: ```<ga address="1/2/3" dpt="1.001"/>```. You may add the attribute "name" to override the address' name as provided by ETS. Restart KAD afterwards. 


