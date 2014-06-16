/*
 * Copyright (c) 2014 TIBCO Software Inc. All Rights Reserved.
 *
 * Use is subject to the terms of the TIBCO license terms accompanying the download of this code.
 * In most instances, the license terms are contained in a file named license.txt.
 */
package org.fabrician.enabler;

import java.util.logging.Logger;

import javax.management.ObjectName;

import com.datasynapse.fabric.common.RuntimeContext;
import com.datasynapse.fabric.common.StartCondition;
import com.datasynapse.fabric.container.Container;
import com.datasynapse.fabric.container.ProcessWrapper;
import com.datasynapse.fabric.domain.Domain;
import com.datasynapse.fabric.stats.jmx.MXBeanWrapper;
import com.datasynapse.fabric.util.ContainerUtils;

public class KarafStartCondition implements StartCondition {
    private KarafContainer container;
    private MXBeanWrapper jmxBeanWrapper;
    private transient Logger logger = ContainerUtils.getLogger(this);
    private static final long DEFAULT_POLL_PERIOD = 5000; // 5secs
    private long pollPeriod = DEFAULT_POLL_PERIOD;
    private String karafRootInstanceName;
    private String karafInstanceJmxAttributeName;

    public KarafStartCondition() {}

    public void init(Container c, Domain d, ProcessWrapper p, RuntimeContext ctx) {
        this.container = (KarafContainer) c;
    }

    private Logger getLogger() {
        return this.container.getLogger();
    }

    private String resolveRootKarafInstanceName() throws Exception {
        String instanceName = this.container.getKarafName();
        logger.info("Polling for start condition for karaf root instance : [" + instanceName + "].");
        return instanceName;
    }

    public synchronized boolean hasStarted() throws Exception {
        try {
            if (jmxBeanWrapper == null) {
                try {
                    this.karafRootInstanceName = resolveRootKarafInstanceName();
                    this.karafInstanceJmxAttributeName = "org.apache.karaf:type=instance,name=" + karafRootInstanceName;
                    getLogger().info("Constructing MXBeanWrapper for JMX atttribute '" + this.karafInstanceJmxAttributeName + "'...");
                    this.jmxBeanWrapper = new MXBeanWrapper(this.container.getMBeanServerConnection(), new ObjectName(this.karafInstanceJmxAttributeName));
                    this.jmxBeanWrapper.resolveAttribute("Instances");
                } catch (Exception ex) {
                    throw new RuntimeException("Can't initialize MXBeanWrapper due to : " + ex.getMessage(), ex);
                }
            }
            this.jmxBeanWrapper.refreshAttributes();
            // check if root karaf instance is started...
            String xpath = "/Instances[@name='[" + karafRootInstanceName + "]']/State";
            getLogger().fine("Checking for Karaf root instance at xpath : " + xpath);
            Object value = this.jmxBeanWrapper.getValue("/attributes" + xpath);
            if (value == null) {
                getLogger().info("JMX value associated with xpath is null");
                return false;
            }
            String started = value.toString();
            if (started.equalsIgnoreCase("started")) {
                getLogger().info("JMX value associated with xpath is '" + started + "'.");
                return true;
            }

        } catch (Exception e) {
            logger.warning("While polling to check if " + container.getName() + " has started: " + e);
        }
        return false;
    }

    public void setPollPeriod(long pollPeriod) {
        this.pollPeriod = pollPeriod;
    }

    public long getPollPeriod() {
        return pollPeriod;
    }

}
