/*
 * Copyright (c) 2014 TIBCO Software Inc. All Rights Reserved.
 *
 * Use is subject to the terms of the TIBCO license terms accompanying the download of this code.
 * In most instances, the license terms are contained in a file named license.txt.
 */
package org.fabrician.enabler;

import com.datasynapse.commons.beans.AbstractBean;
import com.datasynapse.fabric.domain.Domain;
import com.datasynapse.fabric.domain.featureinfo.DisplayName;
import com.datasynapse.fabric.domain.featureinfo.DisplayOrder;
import com.datasynapse.fabric.domain.featureinfo.FeatureInfo;

/**
 * This <code>FeatureInfo</code> implementation indicates that the Domain supports
 * clustering.  The only required parameter is the <code>groupMembership</code>.
 */
public class KarafClusteringInfo extends AbstractBean implements FeatureInfo {
    public static final String FEATURE_NAME="Apache Cellar Clustering Support";
    private static final long serialVersionUID = 1L;
    public static final String GROUP_MEMBERSHIP_PROPERTY ="groupMembership";
    
    private String groupMembership="default";
    
    public void init(Domain domain) {}
    
    @DisplayOrder(0)
    public String getGroupMembership() {
        return this.groupMembership;
    }

    @DisplayName("Karaf node group membership")
    public void setGroupMembership(String groupMembership) {
        String old = this.groupMembership;
        this.groupMembership=groupMembership;
        propertyChange(GROUP_MEMBERSHIP_PROPERTY, old, groupMembership);
    }
}
