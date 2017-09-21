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

import static org.opencastproject.pm.syllabus.impl.SyllabusUtil.toDateMidnightOpt;
import static org.opencastproject.pm.syllabus.impl.SyllabusUtil.toDateTime;
import static org.opencastproject.util.data.Option.option;

import org.opencastproject.pm.syllabus.api.VActivity;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.functions.Strings;
import org.opencastproject.util.persistence.Queries;

import org.joda.time.DateMidnight;
import org.joda.time.DateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

@Entity(name = "VActivity")
@Table(name = "V_ACTIVITY")
@NamedQueries({@NamedQuery(name = "VActivity.findAllSince",
                           query = "select a from VActivity a where a.lastChanged > :since"),
                      @NamedQuery(name = "VActivity.findAll",
                                  query = "select a from VActivity a")})
public final class VActivityDto {
  @Id
  private String id;
  private String name;
  private String hostKey;
  private String description;
  private String activityTypeId;
  private String departmentId;
  private String moduleId;
  @Temporal(TemporalType.TIMESTAMP)
  private Date startDate;
  @Temporal(TemporalType.TIMESTAMP)
  private Date endDate;
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastChanged;
  private boolean isScheduled;

  @Transient
  private static final Logger logger = LoggerFactory.getLogger(VActivity.class);

  private VActivityDto() {
  }

  // -------------------------------------------------------------------------------------------------------------------

  public static final Function<VActivityDto, VActivity> toDomain = new Function<VActivityDto, VActivity>() {
    @Override public VActivity apply(VActivityDto dto) {
      // todo unit test transfer
      final String id = dto.id;
      final String name = dto.name;
      final String hostKey = dto.hostKey;
      final Option<String> description = option(dto.description).bind(Strings.trimToNone);
      final String moduleId = dto.moduleId;
      final String departmentId = dto.departmentId;
      final Option<DateMidnight> startDate = toDateMidnightOpt(dto.startDate);
      final Option<DateMidnight> endDate = toDateMidnightOpt(dto.endDate);
      final DateTime lastChanged;
      {
        DateTime lastChangedInternal = new DateTime(0);
        try {
          lastChangedInternal = toDateTime(dto.lastChanged);
        } catch (NullPointerException e) {
          lastChangedInternal = new DateTime(0);
          logger.error("S+ schema violation, LastChanged == null for activity " + id);
        } finally {
          lastChanged = lastChangedInternal;
        }
      }
      final boolean isScheduled = dto.isScheduled;
      final String activityTypeId = dto.activityTypeId;
      return new VActivity() {
        @Override public String getName() {
          return name;
        }

        @Override public String getHostKey() {
          return hostKey;
        }

        @Override public String getActivityTypeId() {
          return activityTypeId;
        }

        @Override public String getDepartmentId() {
          return departmentId;
        }

        @Override public String getModuleId() {
          return moduleId;
        }

        @Override public Option<String> getDescription() {
          return description;
        }

        @Override public Option<DateMidnight> getStartDate() {
          return startDate;
        }

        @Override public Option<DateMidnight> getEndDate() {
          return endDate;
        }

        @Override public boolean isScheduled() {
          return isScheduled;
        }

        @Override public String getId() {
          return id;
        }

        @Override public DateTime getLastChanged() {
          return lastChanged;
        }
      };
    }
  };

  public abstract static class ActivityFinder<A> extends Finder {
    public ActivityFinder(String entityName) {
      super(entityName);
    }

    /**
    * Get Child Activity Ids that reference a Module identified by UoM courseKey
    @param courseKey
    @return
    */
    public abstract Function<EntityManager, List<String>> findIdsByCourseKey(String courseKey);
  }
  // -------------------------------------------------------------------------------------------------------------------

  public static final Finder<VActivityDto> finder = new ActivityFinder<VActivityDto>("VActivity") {
    @Override
    public Function<EntityManager, List<String>> findIdsByCourseKey(String courseKey) {
      return Queries.sql.findAll(
              "select id from rdowner.V_ACTIVITY as a join rdowner.V_ACTIVITY_MODULE as am on a.id = am.ActivityId "
                      + "where am.moduleid = (select id from rdowner.V_MODULE where userText4 = ?1) "
                      + "and a.IsJtaParent != 1;", courseKey);
    }
  };
}
