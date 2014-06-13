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

Additionally, the Apache Karaf Enabler requires the Silver Fabric Engine use Java 1.7. 

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
* Build the maven project with the location of the archive, operating system target and optionally the version.  Operating system is typically 'all', 'win32,win64' or 'linux,linux64'. 
Note: For Karaf, you can use 'all' for either apache-karaf-3.0.1.tar.gz or apache-karaf-3.0.1.zip since it is currently 100% Java for both Windows and Linux distributions.

```bash
mvn package -Ddistribution.location=/home/you/Downloads/apache-karaf-3.0.1.tar.gz -Ddistribution.version=3.0.1 -Ddistribution.os=all
```

Overview
--------------------------------------
This enabler is based on the standalone Apache Karaf 3.0.x and provides end user with some basic OOTB(out-of-the-box) features and functionality.
It is meant to be a basic, bare minimal enabler that end user can further enhance and customize with Karaf features specific to their needs.

OOTB, this enabler provides some useful features to help end user kickstart their use of Apache Karaf:

* **Provisioning of instances of Karaf**
    * Only "root" instances are supported
	* Root instances listen on different ports for SSH, JMX, HTTP(s) so that they don't collide
	
* **Container management** - via SSH remotely,JMX remotely, WebConsole
    * manage Apache Karaf features
    * manage OSGi bundles
    * manage the instances
    * manage the confgurations
    * manage the log service
	
* **Http(s) and WebContainer support via PAX Web** - Jetty-based

* **A set of default starter SSL keystore and truststore for Https** - via self-signed certificates

* **Karaf nodes clustering** - via Apache Cellar
    * Currently not working fully due to Apache Cellar bundle issues but the different nodes are being picked up by the cluster
	* Runtime context variables not fully abstracted for clustering yet.

Known issues with Apache Karaf 3.0.1
-------------------------------------
* **JMX-HTTP Bridge** - http://www.jolokia.org/
   * The bridge is not working with latest jolokia-osgi 1.2.1 due to the new Role-Based Access Control(RBAC) to the JMX MBeans and operations in Karaf 3.0.x
   * See a workaround at site http://modio.io/tag/osgi/
   
* **Clustering with Apache Cellar 2.3.x**
   * Apache Cellar bundle is not resolving correctly
   * Apache Cellar is waiting for a release version that is compatible with Apache Karaf 3.0.x; contact Apache Cellar for the status
   
   
Limitations
-----------
There are some important Karaf features that are beyond the scope of this enabler. We briefly outlined here how end user may go about enabling and uses these
advanced Karaf features by customizing this enabler further to suit their needs:

* **Failover**
    * With clustering support, failover is probably redundant...
	* If needed, user can create 2 independent but identical Silver Fabric Karaf components A and B and configure a NTFS file lock or a JDBC driver lock via *${KARAF_ETC}/system.properties*
	See Karaf documentation on how to do this.
	
* **JDBC,LDAP and OGSI-based authentication**
    * Apache Karaf provides a default realm named *karaf*.
     This realm has two login modules: *org.apache.karaf.jaas.modules.properties.PropertiesLoginModule*,*org.apache.karaf.jaas.modules.publickey.PublickeyLoginModule*
	 
    1. *PropertiesLoginModule* uses the *"etc/users.properties"* file as backend for users, groups, roles and password.
       This login module authenticates the users and returns the users' roles.
    2. *PublickeyLoginModule* is especially used by the SSHd. It uses the *"etc/keys.properties"* file. This file contains
       the users and a public key associated to each user.
	* Note: In the OOTB enabler, both the properties login module and the public key login module are enabled. 
	  When JAAS authenticates a user, it tries first of all to authenticate the user with the properties login module. If that fails, it then tries to authenticate the user with the public key login module. 
	  If that module also fails, an error is raised.
	  
	* If needed,user can create a *JAAS Login Module.xml* in the form of *Blueprint* configuration file as stated in http://karaf.apache.org/manual/latest/developers-guide/security-framework.html.
	  using same realm name as "karaf" for simplicity in the *jaas:config* element.
	  Then add it to the *$KARAF_HOME}/deploy* directory as a Silver Fabric grid library *content files* so that module can be automatically activated when the Enabler is provisioned.
	  Use Silver Fabric runtime context variables to configure ports, passwords, server, user, role, etc in the JAAS Login module xml file.
	* Important: If you have several realms defined using same name, you need to redefining one choosen realm with a rank attribute value greater than 0 as the one that you want to use, so that it overrides the standard karaf realm which has the rank 0 or others lower than it.

*  **Deployment**
    * Apache Karaf polls a deployment file from the deploy folder, specified by *${KARAF_DEPLOY_DIR}* and then "delegates" the file handling to a deployer to interprets what to "deploy".
    * By default, Apache Karaf provides a set of deployers:
      **Blueprint deployer is able to handle Blueprint XML files.
	  **Spring deployer is able to handle Spring XML files.
	  **Features deployer is able to handle Apache Karaf features XML files
	  **KAR deployer is able to handle KAR files
	  **Wrap deployer is able to handle non-OSGi jar files and turns it as OSGi bundles "on the fly".
      **WAR deployer is able to handle WAR files.
    * To "install" a Silver Fabric component into Apache Karaf Enabler runtime when after it started, user have 2 options:
	  ** Add a component scripting file  and implement a 
	     `doInstall(ActivationInfo info)` method in script(jython,Rhino script, or jRuby)
	  ** Extend the class *org.fabrician.enabler.KarafContainer* and override the method
	     `doInstall(ActivationInfo info)` in Java and repackage the Apache Karaf gridlib.
	  
Statistics
--------------------------------------
By default, the JMX monitoring service is enabled for Apache Karaf Server, but monitoring is based on  a Role-Based Access Control(RBAC) to the JMX MBeans and operations.
By default, the Karaf "admin" role has access to all beans and operations.

Tracked Statistics:

* **Karaf JVM Used Memory** - Used heap memory(MB).
* **Karaf JVM Committed Memory** - Comitted heap memory(MB).
* **Karaf JVM Thread Count** - Average thread counts.

* Note: End user can add additional tracked statistics by adding one or more Silver Fabric component statistic scripting files.

Runtime Context Variables
--------------------------------------

###Karaf-related launch parameters:###
        
* **KARAF_TITLE** - Window console title
    * Default value: Silver Fabric Karaf
    * Type: Environment
	
* **KARAF_HOME** - Karaf install directory
    * Default value: ${container.getWorkDir()}/apache-karaf
    * Type: Environment
	
* **KARAF_BASE** - Directory containing the configuration and OSGi data specific to the root Karaf instance
    * Default value: ${KARAF_HOME}
    * Type: Environment
    
* **KARAF_DATA** - Karaf server data directory, where the Osgi bundles are resolved and cached
    * Default value: ${KARAF_HOME}/data
    * Type: Environment
    
* **KARAF_ETC** - Karaf server directory, where the various configuration files are located
    * Default value: ${KARAF_HOME}/etc
    * Type: Environment
    
                
###JDK and JVM-related runtimes-related:###
         
* **JDK_NAME** -  The name of the required JDK
    * Default value: j2sdk 
    * Type: String
	
* **JDK_VERSION** - The version of the required JDK
    * Default value: 1.7 
    * Type: String
	
* **JAVA_HOME** -  The Java home directory
    * Default value: ${GRIDLIB_JAVA_HOME} 
    * Type: Environment
	
* **JAVA_MIN_MEM** - Minimum JVM startup memory to use
    * Default value: 128M 
    * Type: Environment
	
* **JAVA_MAX_MEM** -  Maximum JVM startup memory to use
	* Default value: 1024M 
	* Type: Environment
	
* **JAVA_PERM_MEM** - JVM permgen memmory to use  
    * Default value: 128M 
    * Type: Environment
	
* **JAVA_MAX_PERM_MEM** -  Maximum JVM permgen memory to use
    * Default value: 256M 
    * Type: Environment
	
* **KARAF_DEBUG** -  Enabled/disabled JVM debug by setting it to 'true' or ''(false)
    * Default value:
    * Type: Environment
	
* **KARAF_DEBUG_PORT** -  JVM debug port for Karaf instance
    * Default value: 5005 
    * Type: Environment
	
* **JAVA_DEBUG_OPTS** -  Custom JVM debug options to use when launching JVM
    * Default value: -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=${KARAF_DEBUG_PORT} 
    * Type: Environment
	
* **JAVA_OPTS** - Custom JVM options to use when launching JVM, including debug mode
    * Default value:  -server -Xms${JAVA_MIN_MEM} -Xmx${JAVA_MAX_MEM} -Dderby.system.home=${KARAF_DATA}/derby -Dderby.storage.fileSyncTransactionLog=true -Dcom.sun.management.jmxremote  -XX:+UnlockDiagnosticVMOptions -XX:+UnsyncloadClass 
    * Type: Environment 
           
###Karaf configuration customization :###
* Impacts : *${KARAF_ETC}/custom.properties*, this overrides any values in  *${KARAF_ETC}/config.properties* 
         
* **KARAF_FRAMEWORK** -  The OSGI framework to use for Karaf instance, 'felix' or 'equinox'
    * Default value: felix 
    * Type: String
	
* **KARAF_FRAMEWORK_START_LEVEL** -  The OSGI framework start-level to reach when considered active
    * Default value: 100 
    * Type: String 
            
* **KARAF_FEATURE_START_LEVEL** -  OSGI bundle start-level for deployed features
    * Default value: 80 
    * Type: String 
           
* **KARAF_SYSTEM_START_LEVEL** -  OSGI bundles with start-level less than this value is considered system bundles
    * Default value: 50 
    * Type: String 
            
* **KARAF_OBR_REPOSITORIES** -  Space-separated list of OBR URL for bundle repository xmls
    * Default value:
    * Type: String 
            
* **KARAF_CONFIG_FILES_SAVE_ENABLED** -  Specify to flush back in the configuration file any changes performed directly on the configuration service via JMX, SSH, etc
    * Default value: true 
    * Type: String 
            
* **KARAF_CONFIG_FILES_DIR** -  Directory where Apache Karaf is looking for configuration files 
    * Default value: ${KARAF_ETC} 
    * Type: String 
            
* **KARAF_CONFIG_FILES_FILTER** -  File name pattern used to load configuration files
    * Default value=.*\\.cfg 
    * Type: String 
            
* **KARAF_CONFIG_FILES_POLL_INTERVAL** - Polling(loading) interval(in millisecs) for configuration files 
    * Default value: 1000 
    * Type: String 
            
* **KARAF_CONFIG_FILES_POLL_NO_DELAY** -  Delay in polling(loading) configuration files when configuration service starts
    * Default value: true 
    * Type: String 
            
* **KARAF_CONFIG_FILES_SERVICE_LOG_LEVEL** -  Log message verbosity level of the configuration polling(loading) service
    * Default value: 3 
    * Type: String 
            
###Karaf system, OSGI data/cache handling :###
* Impacts : *${KARAF_ETC}/system.properties*
          
* **KARAF_NAME** -  Name of Karaf root instance
    * Default value: root 
    * Type: String 
            
* **KARAF_CLEAN_All** -  Deletes the entire ${KARAF_DATA} directory at every start 
    * Default value: false 
    * Type: Environment 
            
* **KARAF_CLEAN_CACHE** -  Deletes the ${KARAF_DATA}/cache directory at every start
    * Default value: false 
    * Type: String 
            
* **KARAF_ADMIN_ROLE** -  System-wide role name associated with an administrative user
    * Default value: admin 
    * Type: String 
            
###Karaf logging settings :###
* Impacts: *${KARAF_ETC}/org.ops4j.pax.logging.cfg*
        
* **KARAF_LOG_LEVEL** -  The log4J logging level to use : 'INFO','ERROR','WARN' or 'DEBUG'
    * Default value: INFO 
    * Type: String 
            
* **KARAF_LOG_FILE** -  The Karaf log file location
    * Default value: ${KARAF_BASE}/log/karaf.log 
    * Type: String 
           
* **KARAF_MAX_LOG_SIZE** -  The maximum rolling log file size
    * Default value: 50MB 
    * Type: String 
            
* **KARAF_MAX_BACKUP_SIZE** -  The maximum number of logs backup
    * Default value: 100 
    * Type: String 
           
###Admin user :###
* Impacts: *${KARAF_ETC}/user.properties* 
         
* **KARAF_ADMIN_USER** - Karaf Admin user name 
    * Default value: karaf 
    * Type: String 
            
* **KARAF_ADMIN_PWD** -  Karaf Admin user password 
    * Default value: karaf 
    * Type: String 
            
###JAAS module settings :###
* Impacts: *${KARAF_ETC}/org.apache.karaf.jaas.cfg* 
         
* **KARAF_PWD_ENCRYPTION_ENABLED** - When enabled(true), the password are encrypted at the first time an user logs in. If set to false, the password exists as plaintext perpetually 
    * Default value: true 
    * Type: String 
            
* **KARAF_PWD_ENCRYPTION_ALGORITHM** -  Set the encryption message digest algorithm to use in Karaf JAAS login module: 'MD2','MD5','SHA-1','SHA-256','SHA-384' or 'SHA-512'
    * Default value: MD5 
    * Type: String 
            
* **KARAF_PWD_ENCRYPTION_ENCODING** -  Set the encryption encoding to use: 'hexadecimal' or 'base64'
    * Default value: hexadecimal 
    * Type: String 
            
* **KARAF_PWD_ENCRYPTION_NAME** -  The encryption name to use
    * Default value: basic 
    * Type: String 
            
* **KARAF_PWD_ENCRYPTION_PREFIX** -  Prefix to prepend to the encrypted password 
    * Default value: {CRYPT} 
    * Type: String 
            
* **KARAF_PWD_ENCRYPTION_SUFFIX** -  Suffix to append to the encrypted password
    * Default value: {CRYPT} 
    * Type: String 
            
###Karaf access via SSHd :###
* Impacts: *${KARAF_ETC}/org.apache.karaf.shell.cfg* 
          
* **KARAF_SSH_PORT** -  SSH port of Karaf SSHd server
    * Default value: 8101 
    * Type: String 
           
* **KARAF_SSH_IDLE_TIMEOUT** -  Inactivity timeout(milliseconds) before a SSH session is logout
    * Default value: 1800000 
    * Type: String 
            
* **KARAF_SSH_REALM** -  Defines which JAAS domain to use for SSH password authentication
    * Default value: karaf 
    * Type: String 
            
* **KARAF_SSH_KEY_FILE** -  This file stores the public and private key pair of the Karaf instance's SSHd server
    * Default value: ${KARAF_ETC}/host.key 
    * Type: String 
            
* **KARAF_SSH_KEY_ALGORITHM** -  Host key algorithm used by the SSHd server. The possible values are 'DSA' or 'RSA'
    * Default value: DSA 
    * Type: String 
            
* **KARAF_SSH_KEY_SIZE** - Self defined key size in '1024', '2048', '3072', or '4096'-bits 
    * Default value: 1024 
    * Type: String 
           
###SSH public key credentials :###
* Impacts: *{KARAF_ETC}/keys.properties* 
         
* **KARAF_ADMIN_SSH_PUBLIC_KEY** - SSH public key credentials for ${KARAF_ADMIN_USER}.This is the public key part of an SSH key pair (typically found in a user's home directory in ~/.ssh/id_rsa.pub in a UNIX system)   
    * Default value: AAAAB3NzaC1kc3MAAACBAP1/U4EddRIpUt9KnC7s5Of2EbdSPO9EAMMeP4C2USZpRV1AIlH7WT2NWPq/xfW6MPbLm1Vs14E7gB00b/JmYLdrmVClpJ+f6AR7ECLCT7up1/63xhv4O1fnxqimFQ8E+4P208UewwI1VBNaFpEy9nXzrith1yrv8iIDGZ3RSAHHAAAAFQCXYFCPFSMLzLKSuYKi64QL8Fgc9QAAAIEA9+GghdabPd7LvKtcNrhXuXmUr7v6OuqC+VdMCz0HgmdRWVeOutRZT+ZxBxCBgLRJFnEj6EwoFhO3zwkyjMim4TwWeotUfI0o4KOuHiuzpnWRbqN/C/ohNWLx+2J6ASQ7zKTxvqhRkImog9/hWuWfBpKLZl6Ae1UlZAFMO/7PSSoAAACBAKKSU2PFl/qOLxIwmBZPPIcJshVe7bVUpFvyl3BbJDow8rXfskl8wO63OzP/qLmcJM0+JbcRU/53JjTuyk31drV2qxhIOsLDC9dGCWj47Y7TyhPdXh/0dthTRBy6bqGtRPxGa7gJov1xm/UuYYXPIUR/3x9MAZvZ5xvE0kYXO+rx 
    * Type: String 
           
###Karaf access via JMX :###
* Impacts: *${KARAF_ETC}/org.apache.karaf.management.cfg*
         
* **KARAF_JMX_RMI_SERVER_PORT** - The port number of the Karaf JMX RMI server 
    * Default value: 44444 
    * Type: String 
            
* **KARAF_JMX_RMI_REGISTRY_PORT** -  The port number of the Karaf JMX RMI registry 
    * Default value: 1099 
    * Type: String 
           
* **KARAF_JMX_REALM** - Name of the JAAS domain used for JMX authentication 
    * Default value: karaf 
    * Type: String 
            
###Maven repository support to download remote Osgi bundles :###
* Impacts: *${KARAF_ETC}/org.ops4j.pax.url.mvn.cfg*
       
* **KARAF_MVN_REMOTE_REPOSITORIES** - Comma-separated list of maven repositories scanned when resolving an artifact  
    * Default value: http://repo1.maven.org/maven2@id=central,http://repository.springsource.com/maven/bundles/release@id=spring.ebr.release,http://repository.springsource.com/maven/bundles/external@id=spring.ebr.external,file:${karaf.home}/${karaf.default.repository}@id=system.repository,file:${karaf.data}/kar@id=kar.repository@multi 
    * Type: String 
            
###Karaf bootup features :###
* Impacts: *${KARAF_ETC}/org.apache.karaf.features.cfg* 
        
* **KARAF_FEATURES_REPOSITORIES** -  Coma separated list of features repositories (features XML) URLs
    * Default value: mvn:org.apache.karaf.features/standard/3.0.1/xml/features,mvn:org.apache.karaf.features/enterprise/3.0.1/xml/features,mvn:org.ops4j.pax.web/pax-web-features/3.1.0/xml/features,mvn:org.apache.karaf.features/spring/3.0.1/xml/features 
    * Type: String 
            
* **KARAF_BOOTUP_FEATURES** -  Comma-separated list of features to install and start at bootup
    * Default value: config,standard,region,package,kar,ssh,management,obr,war,webconsole 
    * Type: String 
            
###Karaf OBR features resolver settings :###
* Impacts:  *${KARAF_ETC/org.apache.karaf.features.obr.cfg}*
         
* **OBR_RESOLVE_OPTIONAL_IMPORTS** - Specify if features OBR resolver has to resolve optional imports 
    * Default value: false 
    * Type: String 
            
* **OBR_START_RESOLVED** -  Specify if resolved bundles should be started by default
    * Default value: true 
    * Type: String 
            
* **OBR_RESOLVED_START_LEVEL** -  Specify the start level for resolved bundles
    * Default value: 80 
    * Type: String 
            
###Karaf "hot" deploying settings :###
* Impacts: *${KARAF_ETC}/org.apache.felix.fileinstall-deploy.cfg*
      
* **KARAF_DEPLOY_DIR** - Location of the deploy directory where Karaf polls for new hot deploy files 
    * Default value: ${KARAF_BASE}/deploy 
    * Type: String 
            
* **KARAF_DEPLOY_TMPDIR** - Location of a temporary directory where the Karaf deployers store their files
    * Default value: ${KARAF_DATA}/generated-bundles 
    * Type: String 
            
* **KARAF_DEPLOY_POLL_INTERVAL** -  Polling interval(millisecs) for new hot deploys files
    * Default value: 1000 
    * Type: String 
            
* **KARAF_DEPLOY_START_LEVEL** -  Deployed bundles start level; if set to 0, the default framework bundle start level ${KARAF_FEATURE_START_LEVEL} will be used instead
    * Default value: 80 
    * Type: String 
            
* **KARAF_DEPLOY_ACTIVE_LEVEL** -  Skipping scanning for deployed files unless the active framework start level ${KARAF_FRAMEWORK_START_LEVEL}is greater than this value
    * Default value: 80 
    * Type: String 
            
###HTTP,HTTPs feature :###
* Impacts: *${KARAF_ETC}/jetty.xml,org.ops4j.pax.web.cfg*
        
* **HTTP_PORT** - HTTP Port for web console and WARs
    * Default value: 9181 
    * Type: String 
            
* **HTTPS_PORT** -  HTTPS Port for web console and WARs
    * Default value: 9443 
    * Type: String 
            
* **KARAF_KEYSTORE_FILE** -  Key store file path
    * Default value: ${KARAF_HOME}/ssl/karaf.keystore 
    * Type: String 
            
* **KARAF_KEYSTORE_PWD** -  Password for Karaf server key store(plain or OBF password)
    * Default value: changeit 
    * Type: String 
            
* **KARAF_TRUSTSTORE_FILE** -  Trust store file path for validating incoming SSL clients
    * Default value: ${KARAF_HOME}/ssl/karaf.truststore 
    * Type: String 
            
* **KARAF_TRUSTSTORE_PWD** -  Password for the Karaf server trust store(plain or OBF password)
    * Default value: changeit 
    * Type: String 
            
* **KARAF_CLIENT_AUTH_NEEDED** -  Whether client authentication is required for incoming connections to Karaf in the SSL/TLS Connector
    * Default value: false 
    * Type: Environment 
           
###Apache Cellar clustering feature :###
* Impacts: TBD
         
* **CELLAR_GROUP_MEMBERSHIP** -  Comma-separated list of groups that Karaf a node should belongs to
    * Default value: default 
    * Type: String 
           
        
###Others :###
         
* **BIND_ON_ALL_LOCAL_ADDRESSES** -  Specify if all network interfaces should be bounded for the SSHd, JMX server, HTTP(s) connectors
    * Default value: true 
    * Type: String 
            
* **LISTEN_ADDRESS_NET_MASK** -A comma delimited list of net masks in CIDR notation.  The first IP address found that matches one of the net masks is used as the listen address.  Note that BIND_ON_ALL_LOCAL_ADDRESSES overrides this setting.  
    * Default value:
    * Type: Environment 
           
* **DELETE_RUNTIME_DIR_ON_SHUTDOWN** -  Whether to delete the Karaf runtime directory on shutdown. Note: Set to false if you need to review if any configuration is done correctly with runtime context variables.
    * Default value: true 
    * Type: Environment 
            
* **CAPTURE_INCLUDES** -  Directories with settings/configuration files to capture
    * Default value: . 
    * Type: String 
           
* **CAPTURE_EXCLUDES** -  Directories to excludes from capture
    * Default value: .*/instances/.*,.*/tmp/.*,.*/log/.*,.*/work/.*,.*/data/.* 
    * Type: String 
            