[fabrician.org](http://fabrician.org/)
==========================================================================
Apache Karaf Server Enabler Guide
==========================================================================

Introduction
--------------------------------------
A Silver Fabric Enabler allows an external application or application platform, such as a 
J2EE application server to run in a TIBCO Silver Fabric software environment. The Apache Karaf 
Server Enabler for Silver Fabric provides integration between Silver Fabric and Apache Karaf. 
The Enabler automatically provisions, orchestrates, controls and manages a Glassfish environment. 

Supported Platforms
--------------------------------------
* Silver Fabric 5.6.x or above
* Windows, Linux

Installation
--------------------------------------
The Apache Karaf Server Enabler consists of an Enabler Runtime Grid Library and a Distribution Grid 
Library. The Enabler Runtime contains information specific to a Silver Fabric version that is 
used to integrate the Enabler, and the Distribution contains the application server or program 
used for the Enabler. Installation of the Apache Karaf Server Enabler involves copying these Grid 
Libraries to the SF_HOME/webapps/livecluster/deploy/resources/gridlib directory on the Silver Fabric Broker. 

Additionally, the GlassFish Server Enabler requires the Silver Fabric Engine use Java 1.7. 

Runtime Grid Library
--------------------------------------
The Enabler Runtime Grid Library is created by building the maven project. The build depends on the SilverFabricSDK jar file that is distributed with TIBCO Silver Fabric. 
The SilverFabricSDK.jar file needs to be referenced in the maven pom.xml or it can be placed in the project root directory.

```bash
mvn package
```
The version of the distribution is defaulted to 3.0.1  However, it can be optionally overridden:
```bash
mvn package -Ddistribution.version=3.0.1
```

Distribution Grid Library
--------------------------------------
The Distribution Grid Library is created by performing the following steps:
* Download the apache-karaf-3.0.1.tar.gz or apache-karaf-3.0.1.zip from http://karaf.apache.org/index/community/download.html
* Build the maven project with the location of the archive, operating system target and optionally the version.  Operating system is typically 'all', 'win32,win64' or 'linux,linux64'. For Karaf, you can use 'all' for either apache-karaf-3.0.1.tar.gz or apache-karaf-3.0.1.zip since it is currently 100% Java.

```bash
mvn package -Ddistribution.location=/home/you/Downloads/apache-karaf-3.0.1.tar.gz -Ddistribution.version=3.0.1 -Ddistribution.os=all
```
Statistics
--------------------------------------
By default, the JMX monitoring service is enabled for Apache Karaf Server, but monitoring is based on  a Role-Based Access Control(RBAC) to the JMX MBeans and operations.
By default, the Karaf "admin" role has access to all beans and operations.



Tracked Statistics:

* **Karaf JVM Used Memory** - Used heap memory(MB).
* **Karaf JVM Committed Memory** - Comitted heap memory(MB).
* **Karaf JVM Thread Count** - Average thread counts.

Runtime Context Variables
--------------------------------------

* **JDK_NAME** - The name of the required JDK.
    * Type: String
    * Default value: j2sdk
* **JDK_VERSION** - The version of the required JDK. 
    * Type: String
    * Default value: 1.7
* **DELETE_RUNTIME_DIR_ON_SHUTDOWN** - Whether to delete the whole Karaf runtime directory on shutdown.  
    * Type: Environment
    * Default value: true
* **KARAF_HOME** - The Karaf home directory.  
    * Type: Environment
    * Default value: ${container.getWorkDir()}/apache-karaf


* **ADMIN_USERID** - Admin Userid.  
    * Type: String
    * Default value: admin
* **ADMIN_PASSWORD** - Admin Password.  
    * Type: String
    * Default value: adminadmin

* **HTTP_PORT** - HTTP listen port.  
    * Type: Environment
    * Default value: 9090
* **HTTPS_PORT** - HTTPS listen port.  
    * Type: Environment
    * Default value: 9190
* **ADMIN_PORT** - Admin port.  
    * Type: Environment
    * Default value: 4848

