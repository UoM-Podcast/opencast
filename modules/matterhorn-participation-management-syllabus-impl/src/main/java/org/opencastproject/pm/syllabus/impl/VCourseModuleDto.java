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

import static org.opencastproject.pm.syllabus.impl.SyllabusUtil.toDateTime;

import org.opencastproject.pm.syllabus.api.VCourseModule;
import org.opencastproject.pm.syllabus.impl.id.VCourseModuleId;
import org.opencastproject.util.data.Function;

import org.joda.time.DateTime;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity(name = "VCourseModule")
@Table(name = "V_ACTIVITY_LOCATION")
@IdClass(VCourseModuleId.class)
@NamedQueries({@NamedQuery(name = "VCourseModule.findAllSince",
                           query = "select a from VCourseModule a where a.lastChanged > :since"),
                      @NamedQuery(name = "VCourseModule.findAll",
                                  query = "select a from VCourseModule a")})
public final class VCourseModuleDto {
  @Id // not the ID but a foreign key
  private String courseId;
  @Id // not the ID but a foreign key
  private String moduleId;
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastChanged;

  private VCourseModuleDto() {
  }

  // -------------------------------------------------------------------------------------------------------------------

  public static final Function<VCourseModuleDto, VCourseModule> toDomain = new Function<VCourseModuleDto, VCourseModule>() {
    @Override public VCourseModule apply(VCourseModuleDto dto) {
      final String courseId = dto.courseId;
      final String moduleId = dto.moduleId;
      final DateTime lastChanged = toDateTime(dto.lastChanged);
      return new VCourseModule() {
        @Override public String getCourseId() {
          return courseId;
        }

        @Override public String getModuleId() {
          return moduleId;
        }

        @Override public DateTime getLastChanged() {
          return lastChanged;
        }
      };
    }
  };

  // -------------------------------------------------------------------------------------------------------------------

  public static final Finder<VCourseModuleDto> finder = new Finder<VCourseModuleDto>("VCourseModule");
}
