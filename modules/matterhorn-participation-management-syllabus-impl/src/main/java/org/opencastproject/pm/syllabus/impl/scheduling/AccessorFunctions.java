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

package org.opencastproject.pm.syllabus.impl.scheduling;

import org.opencastproject.pm.api.Recording;
import org.opencastproject.pm.syllabus.api.VActivity;
import org.opencastproject.pm.syllabus.api.VActivityDateTime;
import org.opencastproject.pm.syllabus.api.VActivityLocation;
import org.opencastproject.pm.syllabus.api.VActivityParents;
import org.opencastproject.pm.syllabus.api.VActivityStaff;
import org.opencastproject.pm.syllabus.api.VActivityStudentSet;
import org.opencastproject.pm.syllabus.api.VDepartment;
import org.opencastproject.pm.syllabus.api.VLocation;
import org.opencastproject.pm.syllabus.api.VLocationSuitability;
import org.opencastproject.pm.syllabus.api.VModule;
import org.opencastproject.pm.syllabus.api.VStaff;
import org.opencastproject.pm.syllabus.api.VStudentSet;
import org.opencastproject.pm.syllabus.api.VZones;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;

// CHECKSTYLE:OFF
public final class AccessorFunctions {
  private AccessorFunctions() {
  }

  public static final class VActivityDateTimeF {
    public static final Function<VActivityDateTime, String> getActivityId = new Function<VActivityDateTime, String>() {
      @Override
      public String apply(VActivityDateTime a) {
        return a.getActivityId();
      }
    };
  }

  public static final class VLocationF {
    public static final Function<VLocation, String> getId = new Function<VLocation, String>() {
      @Override
      public String apply(VLocation a) {
        return a.getId();
      }
    };
  }

  public static final class VActivityF {
    public static final Function<VActivity, String> getId = new Function<VActivity, String>() {
      @Override
      public String apply(VActivity a) {
        return a.getId();
      }
    };
  }

  public static final class VLocationSuitabilityF {
    public static final Function<VLocationSuitability, String> getLocationId = new Function<VLocationSuitability, String>() {
      @Override
      public String apply(VLocationSuitability a) {
        return a.getLocationId();
      }
    };
  }

  public static final class VDepartmentF {
    public static final Function<VDepartment, String> getId = new Function<VDepartment, String>() {
      @Override
      public String apply(VDepartment a) {
        return a.getId();
      }
    };
  }

  public static final class VStudentSetF {
    public static final Function<VStudentSet, String> getId = new Function<VStudentSet, String>() {
      @Override
      public String apply(VStudentSet a) {
        return a.getId();
      }
    };
  }

  public static final class RecordingF {
    public static final Function<Recording, Long> getId = new Function<Recording, Long>() {
      @Override
      public Long apply(Recording a) {
        return a.getId().getOrElseNull();
      }
    };
  }

  public static final class VActivityStudentSetF {
    public static final Function<VActivityStudentSet, String> getActivityId = new Function<VActivityStudentSet, String>() {
      @Override
      public String apply(VActivityStudentSet a) {
        return a.getActivityId();
      }
    };
  }

  public static final class VActivityLocationF {
    public static final Function<VActivityLocation, String> getActivityId = new Function<VActivityLocation, String>() {
      @Override
      public String apply(VActivityLocation a) {
        return a.getActivityId();
      }
    };
  }

  public static final class VActivityParentsF {
    public static final Function<VActivityParents, String> getActivityId = new Function<VActivityParents, String>() {
      @Override
      public String apply(VActivityParents a) {
        return a.getActivityId();
      }
    };

    public static final Function<VActivityParents, String> getParentId = new Function<VActivityParents, String>() {
      @Override
      public String apply(VActivityParents a) {
        return a.getParentId();
      }
    };
  }

  public static final class VZonesF {
    public static final Function<VZones, String> getId = new Function<VZones, String>() {
      @Override
      public String apply(VZones a) {
        return a.getId();
      }
    };
  }

  public static final class VStaffF {
    public static final Function<VStaff, String> getId = new Function<VStaff, String>() {
      @Override
      public String apply(VStaff a) {
        return a.getId();
      }
    };
  }

  public static final class VModuleF {
    public static final Function<VModule, String> getId = new Function<VModule, String>() {
      @Override
      public String apply(VModule a) {
        return a.getId();
      }
    };
    public static final Function<VModule, String> getName = new Function<VModule, String>() {
      @Override
      public String apply(VModule a) {
        return a.getName();
      }
    };
    public static final Function<VModule, Option<String>> getDescription = new Function<VModule, Option<String>>() {
      @Override
      public Option<String> apply(VModule a) {
        return a.getDescription();
      }
    };
    public static final Function<VModule, String> getExternalCourseKey = new Function<VModule, String>() {
      @Override
      public String apply(VModule a) {
        return a.getExternalCourseKey().getOrElse("");
      }
    };
  }

  public static final class VActivityStaffF {
    public static final Function<VActivityStaff, String> getActivityId = new Function<VActivityStaff, String>() {
      @Override
      public String apply(VActivityStaff a) {
        return a.getActivityId();
      }
    };
  }
}
