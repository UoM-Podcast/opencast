/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package org.opencastproject.pm.syllabus.impl;

import org.opencastproject.pm.syllabus.api.Activities;

import org.joda.time.DateTime;

import java.io.Serializable;

public class ActivitiesImpl implements Activities, Serializable {

    private String activityId;
    private String activityName;
    private String moduleId;
    private String moduleName;
    private String moduleDescription;
    private String courseKey;
    private Boolean activityJtaParent;
    private Boolean activityJtaChild;
    private Boolean activityVariantParent;
    private Boolean activityVariantChild;
    private String type;
    private DateTime startDate;
    private DateTime endDate;

    public ActivitiesImpl(String activityId, String activityName, String moduleId, String moduleName, String moduleDescription, String courseKey,
                          Boolean activityJtaParent, Boolean activityJtaChild, Boolean activityVariantParent, Boolean activityVariantChild,
                          String type, DateTime startDate, DateTime endDate) {
        this.activityId = activityId;
        this.activityName = activityName;
        this.moduleId = moduleId;
        this.moduleName = moduleName;
        this.moduleDescription = moduleDescription;
        this.courseKey = courseKey;
        this.activityJtaParent = activityJtaParent;
        this.activityJtaChild = activityJtaChild;
        this.activityVariantParent = activityVariantParent;
        this.activityVariantChild = activityVariantChild;
        this.type = type;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    @Override
    public String getActivityId() {
        return activityId;
    }

    @Override
    public String getActivityName() {
        return activityName;
    }

    @Override
    public String getModuleId() {
        return moduleId;
    }

    @Override
    public Boolean getActivityJtaParent() {
        return activityJtaParent;
    }

    @Override
    public Boolean getActivityJtaChild() {
        return activityJtaChild;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public Boolean getActivityVariantParent() {
        return activityVariantParent;
    }

    @Override
    public Boolean getActivityVariantChild() {
        return activityVariantChild;
    }

    @Override
    public String getModuleName() {
        return moduleName;
    }

    public String getModuleDescription() {
        return moduleDescription;
    }

    @Override
    public String getCourseKey() {
        return courseKey;
    }

    @Override
    public DateTime getStartDate() {
        return startDate;
    }

    @Override
    public DateTime getEndDate() {
        return endDate;
    }
}
