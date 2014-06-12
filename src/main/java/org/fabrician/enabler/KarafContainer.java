/*
 * Copyright (c) 2014 TIBCO Software Inc. All Rights Reserved.
 *
 * Use is subject to the terms of the TIBCO license terms accompanying the download of this code.
 * In most instances, the license terms are contained in a file named license.txt.
 */
package org.fabrician.enabler;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import com.datasynapse.commons.util.HostUtils;
import com.datasynapse.fabric.common.ActivationInfo;
import com.datasynapse.fabric.common.RuntimeContextVariable;
import com.datasynapse.fabric.container.ExecContainer;
import com.datasynapse.fabric.container.Feature;
import com.datasynapse.fabric.domain.featureinfo.HttpFeatureInfo;
import com.datasynapse.fabric.util.ContainerUtils;
import com.datasynapse.fabric.util.DynamicVarsUtils;

public class KarafContainer extends ExecContainer {
    private static final long serialVersionUID = 4555259893922948569L;
    private static final String KARAF_NAME = "KARAF_NAME";
    private static final String KARAF_ADMIN_USER = "KARAF_ADMIN_USER";
    private static final String KARAF_ADMIN_PWD = "KARAF_ADMIN_PWD";
    private static final String KARAF_JMX_RMI_SERVER_PORT = "KARAF_JMX_RMI_SERVER_PORT";
    private static final String KARAF_JMX_RMI_REGISTRY_PORT = "KARAF_JMX_RMI_REGISTRY_PORT";
    private static final String KARAF_SSH_PORT = "KARAF_SSH_PORT";
    private static final String KARAF_JMX_SERVICE_URL = "KARAF_JMX_SERVICE_URL";
    private static final String KARAF_DEBUG = "KARAF_DEBUG";
    private static final String KARAF_DEBUG_PORT = "KARAF_DEBUG_PORT";
    private static final String KARAF_WEBCONSOLE_URL = "KARAF_WEBCONSOLE_URL";
    private static final String BIND_ON_ALL_LOCAL_ADDRESSES = "BIND_ON_ALL_LOCAL_ADDRESSES";
    private static final String KARAF_BIND_ADDRESS = "KARAF_BIND_ADDRESS";
    private static final String CELLAR_CLUSTERING_ENABLED = "CELLAR_CLUSTERING_ENABLED";
    private static final String CELLAR_GROUP_MEMBERSHIP = "CELLAR_GROUP_MEMBERSHIP";
    private static final int UNDEFINED_PORT = -1;
    private JMXConnector jmxc = null;
    private MBeanServerConnection mBeanServer = null;
    private HttpFeatureInfo httpFeatureInfo = null;
    private KarafClusteringInfo clusterFeatureInfo = null;
    
    public KarafContainer() {
        super();
    }

    public synchronized MBeanServerConnection getMBeanServerConnection() throws Exception {
        if (this.mBeanServer == null) {
            Map<String, String[]> environment = new HashMap<String, String[]>();
            String user = getAdminName();
            String pwd = getAdminPassword();
            String jmxurl = getJmxClientUrl();
            String[] credentials = new String[] {user, pwd};
            environment.put(JMXConnector.CREDENTIALS, credentials);
            JMXServiceURL url = null;
            try {
                url = new JMXServiceURL(jmxurl);
                getEngineLogger().info("Establishing JMX connection to URL : [" + jmxurl + "]...");
                this.jmxc = JMXConnectorFactory.connect(url, environment);
                this.mBeanServer = jmxc.getMBeanServerConnection();
                getEngineLogger().info("JMX connection established.");
            } catch (Exception ex) {
                getEngineLogger().warning("[" + jmxurl + "] : " + ex.getMessage());
                throw ex;
            }
        }
        return this.mBeanServer;
    }

    public Logger getLogger() {
        return getEngineLogger();
    }

    @Override
    protected void doInit(List<RuntimeContextVariable> additionalVariables) throws Exception {
        getEngineLogger().fine("doInit invoked");
        clusterFeatureInfo = (KarafClusteringInfo) ContainerUtils.getFeatureInfo(KarafClusteringInfo.FEATURE_NAME, this, getCurrentDomain());
        httpFeatureInfo = (HttpFeatureInfo) ContainerUtils.getFeatureInfo(Feature.HTTP_FEATURE_NAME, this, getCurrentDomain());
        
        boolean httpEnabled = isHttpEnabled();
        boolean httpsEnabled = isHttpsEnabled();
        if (!httpEnabled && !httpsEnabled) {
            throw new Exception("HTTP or HTTPS must be enabled in the Domain");
        }
        if (httpEnabled && !DynamicVarsUtils.validateIntegerVariable(this,HttpFeatureInfo.HTTP_PORT_VAR)) {
            throw new Exception("HTTP is enabled but the " + HttpFeatureInfo.HTTP_PORT_VAR + " runtime context variable is not set");
        }
        if (httpsEnabled && !DynamicVarsUtils.validateIntegerVariable(this, HttpFeatureInfo.HTTPS_PORT_VAR)) {
            throw new Exception("HTTPS is enabled but the " + HttpFeatureInfo.HTTPS_PORT_VAR + " runtime context variable is not set");
        }
        
        if (DynamicVarsUtils.variableHasValue(KARAF_DEBUG, "true") && !DynamicVarsUtils.validateIntegerVariable(this, KARAF_DEBUG_PORT)) {
            throw new Exception(KARAF_DEBUG_PORT + " runtime context variable is not set properly");
        }
        if (!DynamicVarsUtils.validateIntegerVariable(this, KARAF_JMX_RMI_SERVER_PORT)) {
            throw new Exception(KARAF_JMX_RMI_SERVER_PORT + " runtime context variable is not set properly");
        }

        if (!DynamicVarsUtils.validateIntegerVariable(this, KARAF_JMX_RMI_REGISTRY_PORT)) {
            throw new Exception(KARAF_JMX_RMI_REGISTRY_PORT + " runtime context variable is not set properly");
        }

        if (!DynamicVarsUtils.validateIntegerVariable(this, KARAF_SSH_PORT)) {
            throw new Exception(KARAF_SSH_PORT + " runtime context variable is not set properly");
        }
        
        String bindAllStr = getStringVariableValue(BIND_ON_ALL_LOCAL_ADDRESSES, "true");
        boolean bindAll = BooleanUtils.toBoolean(bindAllStr);
        String karafBindAddress = bindAll ? "0.0.0.0" : getStringVariableValue(LISTEN_ADDRESS_VAR);

        additionalVariables.add(new RuntimeContextVariable(KARAF_BIND_ADDRESS, karafBindAddress, RuntimeContextVariable.STRING_TYPE));
        additionalVariables.add(new RuntimeContextVariable(KARAF_JMX_SERVICE_URL, getJmxServiceUrl(), RuntimeContextVariable.STRING_TYPE, "Karaf JMX service url"));
        if (clusterFeatureInfo == null) {
            additionalVariables.add(new RuntimeContextVariable(CELLAR_CLUSTERING_ENABLED, "false", RuntimeContextVariable.STRING_TYPE));
        } else {
            String group_membership = StringUtils.trimToEmpty(resolveVariables(clusterFeatureInfo.getGroupMembership()));
            group_membership = StringUtils.isBlank(group_membership) ? "default" : group_membership;
            String[] groups = StringUtils.splitByWholeSeparator(group_membership, ",");
            for (String g : groups) {
                if (StringUtils.trimToEmpty(g).isEmpty()) {
                    throw new Exception("'Cellar Group Membership' value specified in the Clustering feature is not set properly.");
                }
            }
            group_membership=StringUtils.join(groups,","); // get rid of some misplaced ","
            additionalVariables.add(new RuntimeContextVariable(CELLAR_CLUSTERING_ENABLED, "true", RuntimeContextVariable.STRING_TYPE));
            additionalVariables.add(new RuntimeContextVariable(CELLAR_GROUP_MEMBERSHIP, group_membership, RuntimeContextVariable.STRING_TYPE));
        }
        super.doInit(additionalVariables);
    }

    @Override
    protected void doStart() throws Exception {
        getEngineLogger().fine("Invoking doStart...");
        // check sshd, jmx ports for collisions with ports already in use
        if (portsConflict()) {
            getEngineLogger().severe("Unable to activate Container due to port conflicts.");
            throw new Exception("Port conflicts detected.");
        }
        super.doStart();
        getEngineLogger().fine("doStart invoked");
    }

    @Override
    protected void doInstall(ActivationInfo info) throws Exception {
        getEngineLogger().fine("Invoking doInstall...");
        super.doInstall(info);
        info.setProperty(KARAF_SSH_PORT, getStringVariableValue(KARAF_SSH_PORT));
        info.setProperty(KARAF_JMX_SERVICE_URL, getJmxClientUrl());
        if (getKarafDebugPort() != UNDEFINED_PORT) {
            info.setProperty(KARAF_DEBUG_PORT, String.valueOf(getKarafDebugPort()));
        }
        if(isHttpEnabled()){
        info.setProperty(HttpFeatureInfo.HTTP_PORT_VAR, String.valueOf(getHttpPort()));
        }
        if(isHttpEnabled()){
            info.setProperty(HttpFeatureInfo.HTTPS_PORT_VAR, String.valueOf(getHttpsPort()));
        }
        info.setProperty(KARAF_WEBCONSOLE_URL, getWebConsoleUrl());
        getEngineLogger().fine("doInstall invoked");
    }

    @Override
    protected void doUninstall() throws Exception {
        getEngineLogger().fine("Invoking doUnInstall...");
        super.doUninstall();
        getEngineLogger().fine("doUnInstall invoked");
    }

    @Override
    protected void doShutdown() throws Exception {
        getEngineLogger().fine("Invoking doShutdown...");
        long shutdownStart = System.currentTimeMillis();
        if ((getProcess() != null) && (getProcess().isRunning())) {
            super.doShutdown();
            waitForShutdown(shutdownStart);
        }

        // stop monitoring for unexpected container crash now.
        setRunCrashMonitor(false);
        try {
            jmxc.close();
        } catch (Exception ex) {
            // we don't normally care about these exceptions but we'll log just the same
            // we want the JMXConnection closed and the exception handled here so it doesn't
            // crop up later unexpectedly during a garbage collection
            getEngineLogger().finer("JMXConnection close had exception <safe to ignore>" + ex.getMessage());
            try {
                jmxc.close();
            } catch (Exception e) {
                getEngineLogger().finer("JMXConnection close had another exception <safe to ignore>" + e.getMessage());
            }
        }
        getEngineLogger().fine("doShutdown invoked");
    }

    protected void checkAndSaveProperty(Properties toBeSavedProps, String name) {
        String value = System.getProperty(name);
        if (!isNullOrEmpty(value)) {
            toBeSavedProps.put(name, value);
        }
    }

    protected void revertEnvironment(Properties envSaved) {
        for (Iterator itr = envSaved.keySet().iterator(); itr.hasNext();) {
            String key = (String) itr.next();
            String val = envSaved.getProperty(key);
            if (val != null) {
                System.setProperty(key, val);
            }
        }
    }

    public boolean isHttpEnabled() {
        return (httpFeatureInfo != null ? httpFeatureInfo.isHttpEnabled() : false);
    }

    public boolean isHttpsEnabled() {
        return (httpFeatureInfo != null ? httpFeatureInfo.isHttpsEnabled() : false);
    }

    public String getKarafName() throws Exception {
        return getStringVariableValue(KARAF_NAME, DynamicVarsUtils.incrementVariableValue("root", RuntimeContextVariable.STRING_APPEND_INCREMENT));
    }

    public String getJmxServiceUrl() throws Exception {
        return "service:jmx:rmi://" + getKarafBindAddress() + ":" + getJmxRmiServerPort() + "/jndi/rmi://" + getKarafBindAddress() + ":" + getJmxRmiRegistryPort() + "/karaf-"
                + getKarafName();
    }

    public String getWebConsoleUrl() throws Exception {
        if(isHttpEnabled() && isHttpsEnabled()){
           return "http://" + getKarafListenAddress() + ":" + getHttpPort() + "/system/console," +
                  "https://" + getKarafListenAddress() + ":" + getHttpsPort() + "/system/console";
        }else if(isHttpEnabled()){
            return "http://" + getKarafListenAddress() + ":" + getHttpPort() + "/system/console";
        }else if(isHttpsEnabled()){
            return "https://" + getKarafListenAddress() + ":" + getHttpsPort() + "/system/console";
        }else{
            return "";
        }
    }

    public String getJmxClientUrl() throws Exception {
        return "service:jmx:rmi:///jndi/rmi://" + getKarafListenAddress() + ":" + getJmxRmiRegistryPort() + "/karaf-" + getKarafName();
    }

    private String getAdminName() throws Exception {
        return getStringVariableValue(KARAF_ADMIN_USER, "karaf");
    }

    private String getAdminPassword() throws Exception {
        return getStringVariableValue(KARAF_ADMIN_PWD, "karaf");
    }

    public String getKarafBindAddress() throws Exception {
        return getStringVariableValue(KARAF_BIND_ADDRESS, "0.0.0.0");
    }

    private String getKarafListenAddress() throws Exception {
        return getStringVariableValue(LISTEN_ADDRESS_VAR, HostUtils.getFQHostname());
    }

    private int getJmxRmiServerPort() throws Exception {
        return NumberUtils.createInteger(getStringVariableValue(KARAF_JMX_RMI_SERVER_PORT, null));
    }

    private int getJmxRmiRegistryPort() throws Exception {
        return NumberUtils.createInteger(getStringVariableValue(KARAF_JMX_RMI_REGISTRY_PORT, null));
    }

    private int getSSHdPort() throws Exception {
        return NumberUtils.createInteger(getStringVariableValue(KARAF_SSH_PORT, null));
    }

    private int getKarafDebugPort() throws Exception {
        if (DynamicVarsUtils.variableHasValue(KARAF_DEBUG, "true")) {
            return NumberUtils.createInteger(getStringVariableValue(KARAF_DEBUG_PORT, null));
        }
        return UNDEFINED_PORT;
    }

    private int getHttpPort() throws Exception {
        return NumberUtils.createInteger(getStringVariableValue(HttpFeatureInfo.HTTP_PORT_VAR, null));
    }

    private int getHttpsPort() throws Exception {
        return NumberUtils.createInteger(getStringVariableValue(HttpFeatureInfo.HTTPS_PORT_VAR, null));
    }

    // try and open a server socket. If we can then close it and return false
    // otherwise assume its in use and return true
    private boolean serverPortInUse(int port) {
        if (port == UNDEFINED_PORT) {
            return false;
        }
        ServerSocket srv = null;
        boolean inUse = false;
        try {
            srv = new ServerSocket(port);
            srv.close();
        } catch (Exception e) {
            getEngineLogger().finest("serverPortInUse: debug exception: " + e);
            inUse = true;
        }
        return inUse;
    }

    // check to see if a given socket port is in use by some other server. This
    // way we can throw an exception and kill the activation
    // with an appropriate error rather than waiting for Karaf to try and start
    // and then fail (since its hard to detect at that point)
    private boolean portsConflict() {
        boolean conflicts = false;
        try {

            if (serverPortInUse(getKarafDebugPort())) {
                conflicts = true;
                getEngineLogger().severe("Port conflict : Karaf debug port <" + getKarafDebugPort() + ">" + " is already in use.");
            }

            if (serverPortInUse(getJmxRmiServerPort())) {
                conflicts = true;
                getEngineLogger().severe("Port conflict : JMX RMI Server port <" + getJmxRmiServerPort() + ">" + " is already in use.");
            }
            if (serverPortInUse(getJmxRmiRegistryPort())) {
                conflicts = true;
                getEngineLogger().severe("Port conflict : JMX RMI Registry port <" + getJmxRmiRegistryPort() + ">" + " is already in use.");
            }

            if (serverPortInUse(getSSHdPort())) {
                conflicts = true;
                getEngineLogger().severe("Port conflict : SSH daemon port <" + getSSHdPort() + ">" + " is already in use.");
            }

            if (isHttpEnabled() && serverPortInUse(getHttpPort())) {
                conflicts = true;
                getEngineLogger().severe("Port conflict : Http port <" + getHttpPort() + ">" + " is already in use.");
            }
            if (isHttpsEnabled() && serverPortInUse(getHttpsPort())) {
                conflicts = true;
                getEngineLogger().severe("Port conflict : Https port <" + getHttpsPort() + ">" + " is already in use.");
            }

        } catch (Exception e) {
            getEngineLogger().log(Level.SEVERE, "Exception while checking if ports are in use", e);
            conflicts = true;
        }
        return conflicts;
    }

    private static boolean isNullOrEmpty(String string) {
        return string == null || string.trim().length() == 0;
    }

    private static String getHostName() throws Exception {
        return InetAddress.getLocalHost().getHostName();
    }

    private static void copyFile(File from, File to) throws IOException {
        Files.copy(from.toPath(), to.toPath());
    }

    private static String stripExtension(String fileName) {
        int idx = fileName.lastIndexOf('.');
        return (idx != -1) ? fileName.substring(0, idx) : fileName;
    }

}
