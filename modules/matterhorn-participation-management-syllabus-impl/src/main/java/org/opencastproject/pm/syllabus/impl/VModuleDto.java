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
import static org.opencastproject.util.data.Option.option;

import org.opencastproject.pm.syllabus.api.VModule;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.functions.Strings;

import org.joda.time.DateTime;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity(name = "VModule")
@Table(name = "V_MODULE")
@NamedQueries({
        @NamedQuery(name = "VModule.findAllSince", query = "select a from VModule a where a.lastChanged > :since"),
        @NamedQuery(name = "VModule.findAll", query = "select a from VModule a") })
public final class VModuleDto {
  @Id
  private String id;
  private String name;
  private String hostKey;
  private String departmentId;
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastChanged;
  private String description;
  private String userText4;

  private VModuleDto() {
  }

  // -------------------------------------------------------------------------------------------------------------------

  public static final Function<VModuleDto, VModule> toDomain = new Function<VModuleDto, VModule>() {
    @Override
    public VModule apply(VModuleDto dto) {
      final String name = dto.name;
      final String id = dto.id;
      final String hostKey = dto.hostKey;
      final Option<String> externalCourseKey = option(dto.userText4).bind(Strings.trimToNone);
      final Option<String> description = option(dto.description).bind(Strings.trimToNone);
      final String departmentId = dto.departmentId;
      final DateTime lastChanged = toDateTime(dto.lastChanged);
      return new VModule() {
        @Override
        public String getDepartmentId() {
          return departmentId;
        }

        @Override
        public Option<String> getDescription() {
          return description;
        }

        @Override
        public String getName() {
          return name;
        }

        @Override
        public String getHostKey() {
          return hostKey;
        }

        @Override
        public String getId() {
          return id;
        }

        @Override
        public DateTime getLastChanged() {
          return lastChanged;
        }

        @Override
        public Option<String> getExternalCourseKey() {
          return externalCourseKey;
        }
      };
    }
  };

  // -------------------------------------------------------------------------------------------------------------------

  public static final Finder<VModuleDto> finder = new Finder<VModuleDto>("VModule");
}
