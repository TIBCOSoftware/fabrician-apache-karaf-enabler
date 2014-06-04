/*
* Copyright (c) 2014 TIBCO Software Inc. All Rights Reserved.
*
* Use is subject to the terms of the TIBCO license terms accompanying the download of this code.
* In most instances, the license terms are contained in a file named license.txt.
*/
package org.fabrician.enabler;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServerConnection;

import com.datasynapse.fabric.common.RuntimeContext;
import com.datasynapse.fabric.container.Container;
import com.datasynapse.fabric.container.ProcessWrapper;
import com.datasynapse.fabric.domain.Domain;
import com.datasynapse.fabric.stats.MXBeanServerRef;
import com.datasynapse.fabric.util.ContainerUtils;

public class KarafMXBeanServerRef implements MXBeanServerRef{
    private MBeanServerConnection mBeanServerConnection;
    private KarafContainer container;
    
    private transient Logger logger = ContainerUtils.getLogger(this);
    
    public synchronized void init(Container enabler, Domain component, ProcessWrapper process, RuntimeContext runtimeContext) {
        try {
            this.container = (KarafContainer)enabler;
            this.mBeanServerConnection = container.getMBeanServerConnection();
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Failed to initialize " + KarafMXBeanServerRef.class, ex);
        }
    }

    public Object getAttribute(String bean, String attr) throws Exception {
        throw new UnsupportedOperationException("This method is unsupported for " + KarafMXBeanServerRef.class);
    }

    public synchronized MBeanServerConnection getConnection() {
        return mBeanServerConnection;
    }
}
