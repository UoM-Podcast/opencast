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

import static org.opencastproject.util.data.Collections.last;
import static org.opencastproject.util.data.Collections.list;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Tuple.tuple;
import static org.opencastproject.util.persistence.Queries.sql;

import org.opencastproject.pm.syllabus.api.Activities;
import org.opencastproject.pm.syllabus.api.Occurrence;
import org.opencastproject.pm.syllabus.api.SyllabusService;
import org.opencastproject.pm.syllabus.api.VActivity;
import org.opencastproject.pm.syllabus.api.VActivityDateTime;
import org.opencastproject.pm.syllabus.api.VActivityEquipment;
import org.opencastproject.pm.syllabus.api.VActivityLocation;
import org.opencastproject.pm.syllabus.api.VActivityParents;
import org.opencastproject.pm.syllabus.api.VActivityStaff;
import org.opencastproject.pm.syllabus.api.VActivityStudentSet;
import org.opencastproject.pm.syllabus.api.VCourse;
import org.opencastproject.pm.syllabus.api.VDepartment;
import org.opencastproject.pm.syllabus.api.VEquipment;
import org.opencastproject.pm.syllabus.api.VLocation;
import org.opencastproject.pm.syllabus.api.VLocationSuitability;
import org.opencastproject.pm.syllabus.api.VModule;
import org.opencastproject.pm.syllabus.api.VStaff;
import org.opencastproject.pm.syllabus.api.VStudentSet;
import org.opencastproject.pm.syllabus.api.VZones;
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.persistence.PersistenceEnv;
import org.opencastproject.util.persistence.Queries;
import org.opencastproject.util.persistence.Table;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;

import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;

/** Manchester implementation. */
public abstract class AbstractSyllabusService implements SyllabusService {

  /** Persistence environment dependency. */
  protected abstract PersistenceEnv getPenv();

  protected abstract void closePenv();

  private final String sqlFindAllOccurrence;

  private final String sqlFindStaffActivities = IoSupport.readToString(AbstractSyllabusService.class.getResource("find-staff-activities.sql"),
          "utf-8");

  private final String sqlFindModuleActivities = IoSupport.readToString(AbstractSyllabusService.class.getResource("find-module-activities.sql"),
          "utf-8");

  private final String sqlFindChildActivities = IoSupport.readToString(AbstractSyllabusService.class.getResource("find-child-activities.sql"),
          "utf-8");

  private final String sqlFindParentActivities = IoSupport.readToString(AbstractSyllabusService.class.getResource("find-parent-activity.sql"),
          "utf-8");

  private final String sqlFindLocationActivities = IoSupport.readToString(AbstractSyllabusService.class.getResource("find-activity-by-location.sql"),
          "utf-8");

  protected AbstractSyllabusService() {
    sqlFindAllOccurrence = IoSupport.readToString(AbstractSyllabusService.class.getResource("find-all-occurrence.sql"),
            "utf-8");
  }

  // CHECKSTYLE:OFF
  public static class OccurrenceJoinTable extends Table<OccurrenceJoinTable> {

    public final Col<String> actId = stringCol();
    public final Col<String> actName = stringCol();
    public final Col<DateTime> actLastChanged = dateTimeCol();
    public final Col<DateTime> actDatLastChanged = dateTimeCol();
    public final Col<DateTime> actLocLastChanged = dateTimeCol();
    public final Col<DateTime> locLastChanged = dateTimeCol();
    public final Col<DateTime> actDatStartDateTime = dateTimeCol();
    public final Col<DateTime> actDatEndDateTime = dateTimeCol();
    public final Col<String> locId = stringCol();
    public final Col<String> locName = stringCol();
    public final Col<String> locHostKey = stringCol();

    public OccurrenceJoinTable(Object[] row) {
      super(row);
      init();
    }
  }

  public static class ActivitiesJoinTable extends Table<ActivitiesJoinTable> {

    public final Col<String> actId = stringCol();
    public final Col<String> actName = stringCol();
    public final Col<String> modId = stringCol();
    public final Col<String> modName = stringCol();
    public final Col<String> modDescription = stringCol();
    public final Col<String> courseK = stringCol();
    public final Col<Boolean> actParent = booleanCol();
    public final Col<Boolean> actChild = booleanCol();
    public final Col<Boolean> actVariantParent = booleanCol();
    public final Col<Boolean> actVariantChild = booleanCol();
    public final Col<String> actType = stringCol();
    public final Col<DateTime> actDatStartDateTime = dateTimeCol();
    public final Col<DateTime> actDatEndDateTime = dateTimeCol();

    public ActivitiesJoinTable(Object[] row) {
      super(row);
      init();
    }
  }

  // CHECKSTYLE:ON
  @Override
  public List<Occurrence> findOccurrences(DateTime since, DateTime untilStartDate) {
    return mlist(getPenv().tx(sql.<Object[]>findAll(sqlFindAllOccurrence, since.toDate(), untilStartDate.toDate())))
            .map(rowToOccurrence).value();
  }

  @Override
  public List<VActivity> findActivities(DateTime since) {
    return mlist(getPenv().tx(VActivityDto.finder.findAllSince(since))).map(VActivityDto.toDomain).value();
  }

  @Override
  public List<VActivity> findActivities() {
    return mlist(getPenv().tx(VActivityDto.finder.findAll())).map(VActivityDto.toDomain).value();
  }

  @Override
  public List<VLocation> findLocations(DateTime since) {
    return mlist(getPenv().tx(VLocationDto.finder.findAllSince(since))).map(VLocationDto.toDomain).value();
  }

  @Override
  public List<VLocation> findLocations() {
    return mlist(getPenv().tx(VLocationDto.finder.findAll())).map(VLocationDto.toDomain).value();
  }

  @Override
  public List<VStaff> findStaff(DateTime since) {
    return mlist(getPenv().tx(VStaffDto.finder.findAllSince(since))).map(VStaffDto.toDomain).value();
  }

  @Override
  public List<VStaff> findStaff() {
    return mlist(getPenv().tx(VStaffDto.finder.findAll())).map(VStaffDto.toDomain).value();
  }

  @Override
  public List<VDepartment> findDepartment(DateTime since) {
    return mlist(getPenv().tx(VDepartmentDto.finder.findAllSince(since))).map(VDepartmentDto.toDomain).value();
  }

  @Override
  public List<VDepartment> findDepartment() {
    return mlist(getPenv().tx(VDepartmentDto.finder.findAll())).map(VDepartmentDto.toDomain).value();
  }

  @Override
  public List<VZones> findZones(DateTime since) {
    return mlist(getPenv().tx(VZonesDto.finder.findAllSince(since))).map(VZonesDto.toDomain).value();
  }

  @Override
  public List<VZones> findZones() {
    return mlist(getPenv().tx(VZonesDto.finder.findAll())).map(VZonesDto.toDomain).value();
  }

  @Override
  public List<VActivityDateTime> findActivityDateTime(DateTime since) {
    return mlist(getPenv().tx(VActivityDateTimeDto.finder.findAllSince(since))).map(VActivityDateTimeDto.toDomain)
            .value();
  }

  @Override
  public List<VActivityDateTime> findActivityDateTime() {
    return mlist(getPenv().tx(VActivityDateTimeDto.finder.findAll())).map(VActivityDateTimeDto.toDomain).value();
  }

  @Override
  public List<VActivityDateTime> findActivityDateTime(String activityId) {
    return mlist(getPenv().tx(VActivityDateTimeDto.findByActivityId(activityId))).map(VActivityDateTimeDto.toDomain)
            .value();
  }

  @Override
  public List<VActivityDateTime> findActivityDateTimeByRange(String startActivityId, String endActivityId) {
    return mlist(getPenv().tx(VActivityDateTimeDto.findByActivityIdRange(startActivityId, endActivityId))).map(
            VActivityDateTimeDto.toDomain).value();
  }

  @Override
  public List<VActivityLocation> findActivityLocation(DateTime since) {
    return mlist(getPenv().tx(VActivityLocationDto.finder.findAllSince(since))).map(VActivityLocationDto.toDomain)
            .value();
  }

  @Override
  public List<VActivityLocation> findActivityLocation() {
    return mlist(getPenv().tx(VActivityLocationDto.finder.findAll())).map(VActivityLocationDto.toDomain).value();
  }

  @Override
  public List<VActivityLocation> findActivityLocation(String activityId) {
    return mlist(getPenv().tx(VActivityLocationDto.findByActivityId(activityId))).map(VActivityLocationDto.toDomain)
            .value();
  }

  @Override
  public List<VActivityParents> findActivityParents() {
    return mlist(getPenv().tx(VActivityParentsDto.finder.findAll())).map(VActivityParentsDto.toDomain).value();
  }

  @Override
  public List<VActivityParents> findActivityParents(String activityId) {
    return mlist(getPenv().tx(VActivityParentsDto.findByActivityId(activityId))).map(VActivityParentsDto.toDomain)
            .value();
  }

  @Override
  public List<VLocationSuitability> findLocationSuitability(DateTime since) {
    return mlist(getPenv().tx(VLocationSuitabilityDto.finder.findAllSince(since))).map(VLocationSuitabilityDto.toDomain)
            .value();
  }

  @Override
  public List<VLocationSuitability> findLocationSuitability() {
    return mlist(getPenv().tx(VLocationSuitabilityDto.finder.findAll())).map(VLocationSuitabilityDto.toDomain).value();
  }

  @Override
  public List<VLocationSuitability> findLocationSuitability(String suitabilityId) {
    return mlist(getPenv().tx(VLocationSuitabilityDto.findBySuitabilityId(suitabilityId)))
            .map(VLocationSuitabilityDto.toDomain).value();
  }

  @Override
  public List<VCourse> findCourses(DateTime since) {
    return mlist(getPenv().tx(VCourseDto.finder.findAllSince(since))).map(VCourseDto.toDomain).value();
  }

  @Override
  public List<VCourse> findCourses() {
    return mlist(getPenv().tx(VCourseDto.finder.findAll())).map(VCourseDto.toDomain).value();
  }

  @Override
  public List<VModule> findModules(DateTime since) {
    return mlist(getPenv().tx(VModuleDto.finder.findAllSince(since))).map(VModuleDto.toDomain).value();
  }

  @Override
  public List<VModule> findModules() {
    return mlist(getPenv().tx(VModuleDto.finder.findAll())).map(VModuleDto.toDomain).value();
  }

  @Override
  public List<VStudentSet> findStudentSets(DateTime since) {
    return mlist(getPenv().tx(VStudentSetDto.finder.findAllSince(since))).map(VStudentSetDto.toDomain).value();
  }

  @Override
  public List<VStudentSet> findStudentSets() {
    return mlist(getPenv().tx(VStudentSetDto.finder.findAll())).map(VStudentSetDto.toDomain).value();
  }

  @Override
  public List<VEquipment> findEquipment(DateTime since) {
    return mlist(getPenv().tx(VEquipmentDto.finder.findAllSince(since))).map(VEquipmentDto.toDomain).value();
  }

  @Override
  public List<VEquipment> findEquipment() {
    return mlist(getPenv().tx(VEquipmentDto.finder.findAll())).map(VEquipmentDto.toDomain).value();
  }

  @Override
  public List<VActivityEquipment> findActivityEquipment(DateTime since) {
    return mlist(getPenv().tx(VActivityEquipmentDto.finder.findAllSince(since))).map(VActivityEquipmentDto.toDomain)
            .value();
  }

  @Override
  public List<VActivityEquipment> findActivityEquipment() {
    return mlist(getPenv().tx(VActivityEquipmentDto.finder.findAll())).map(VActivityEquipmentDto.toDomain).value();
  }

  @Override
  public List<VActivityStudentSet> findActivityStudentSet(DateTime since) {
    return mlist(getPenv().tx(VActivityStudentSetDto.finder.findAllSince(since))).map(VActivityStudentSetDto.toDomain)
            .value();
  }

  @Override
  public List<VActivityStudentSet> findActivityStudentSet() {
    return mlist(getPenv().tx(VActivityStudentSetDto.finder.findAll())).map(VActivityStudentSetDto.toDomain).value();
  }

  @Override
  public List<VActivityStaff> findActivityStaff(DateTime since) {
    return mlist(getPenv().tx(VActivityStaffDto.finder.findAllSince(since))).map(VActivityStaffDto.toDomain).value();
  }

  @Override
  public List<VActivityStaff> findActivityStaff() {
    return mlist(getPenv().tx(VActivityStaffDto.finder.findAll())).map(VActivityStaffDto.toDomain).value();
  }

  @Override
  public List<String> findDeletedActivities(List<String> activityIds) {
    // todo
    return null;
  }

  @Override
  public List<String> findModuleActivityIdsByCourseKey(String courseKey) {
    Function<EntityManager, List<String>> f = ((VActivityDto.ActivityFinder) VActivityDto.finder).findIdsByCourseKey(courseKey);
    return getPenv().tx(f);
  }

  @Override
  public DateTimeZone getTimeZone() {
    // todo
    return null;
  }

  @Override
  public VModule getModuleByCourseKey(String courseKey) {
    Option<Object> dto = getPenv().tx(Queries.named.findFirst("VModule.getByCourseKey", tuple("courseKey", courseKey)));
    if (dto.isSome()) {
      return VModuleDto.toDomain.apply((VModuleDto) dto.get());
    } else {
      return null;
    }
  }

  @Override
  public VStaff findStaffById(String spotId) {
    Option<Object> dto = getPenv().tx(Queries.named.findFirst("VStaff.getById", tuple("spotId", spotId)));
    if (dto.isSome()) {
      return VStaffDto.toDomain.apply((VStaffDto) dto.get());
    } else {
      return null;
    }
  }

  @Override
  public List<VActivityLocation> findByActivityLocationId(String activityId) {
    Function<EntityManager, List<VActivityLocation>> f = ((VActivityLocationDto.LocationFinder) VActivityLocationDto.finderLocation).findByActivityLocationId(activityId);
    return getPenv().tx(f);
  }

  @Override
  public List<VLocation> findSuitabilityByLocationId(String locationId) {
    Function<EntityManager, List<VLocation>> f = ((VLocationDto.LocationSuitabilityFinder) VLocationDto.finderSuitability).findSuitabilityByLocationId(locationId);
    return getPenv().tx(f);
  }

  @Override
  public VModule getModuleById(String id) {
    Option<Object> dto = getPenv().tx(Queries.named.findFirst("VModule.getById", tuple("id", id)));
    if (dto.isSome()) {
      return VModuleDto.toDomain.apply((VModuleDto) dto.get());
    } else {
      return null;
    }
  }

  @Override
  public List<VModule> getAllModule() {
    return mlist(getPenv().tx(VModuleDto.finder.findAll())).map(VModuleDto.toDomain).value();
  }

  @Override
  public List<Activities> findStaffActivities(String staffId) {
    return mlist(getPenv().tx(sql.<Object[]>findAll(sqlFindStaffActivities, staffId)))
            .map(rowToStaffActivities).value();
  }

  @Override
  public List<Activities> findModuleActivities(String moduleName) {
    return mlist(getPenv().tx(sql.<Object[]>findAll(sqlFindModuleActivities, moduleName)))
            .map(rowToStaffActivities).value();
  }

  @Override
  public List<Activities> findChildActivities(String activityId) {
    return mlist(getPenv().tx(sql.<Object[]>findAll(sqlFindChildActivities, activityId)))
            .map(rowToStaffActivities).value();
  }

  @Override
  public List<Activities> findParentActivities(String activityId) {
    return mlist(getPenv().tx(sql.<Object[]>findAll(sqlFindParentActivities, activityId)))
            .map(rowToStaffActivities).value();
  }

  @Override
  public List<Activities> findActivitiesByLocation(String location, DateTime since, DateTime untilStartDate) {
    return mlist(getPenv().tx(sql.<Object[]>findAll(sqlFindLocationActivities, location, since.toDate(), untilStartDate.toDate())))
            .map(rowToStaffActivities).value();
  }

  @Override
  public List<VStaff> findStaffByStaffActivityId(String activityId) {
    Function<EntityManager, List<VStaff>> f = ((VStaffDto.StaffFinder) VStaffDto.finderStaff).findStaffByStaffActivityId(activityId);
    return getPenv().tx(f);
  }

  @Override
  public VLocation findLocationByName(String name) {
    Option<Object> dto = getPenv().tx(Queries.named.findFirst("VLocation.findByName", tuple("name", name)));
    if (dto.isSome()) {
      return VLocationDto.toDomain.apply((VLocationDto) dto.get());
    } else {
      return null;
    }
  }

  // could by solved with JPA @javax.persistence.SqlResultSetMapping also
  public static final Function<Object[], Occurrence> rowToOccurrence = new Function<Object[], Occurrence>() {
    @Override
    public Occurrence apply(final Object[] selected) {
      final OccurrenceJoinTable t = new OccurrenceJoinTable(selected);
      final DateTime lastChanged;
      {
        final List<DateTime> lastChanges = list(t.get(t.actLastChanged), t.get(t.actDatLastChanged),
                t.get(t.actLocLastChanged), t.get(t.locLastChanged));
        Collections.sort(lastChanges);
        lastChanged = last(lastChanges).get();
      }
      final VLocationImpl loc = new VLocationImpl(t.get(t.locId), t.get(t.locName), t.get(t.locHostKey),
              t.get(t.locLastChanged), Option.<String> none());
      final Interval time = new Interval(t.get(t.actDatStartDateTime), t.get(t.actDatEndDateTime));
      return new OccurrenceImpl(t.get(t.actId), t.get(t.actName), loc, time, lastChanged);
    }
  };

  public static final Function<Object[], Activities> rowToStaffActivities = new Function<Object[], Activities>() {
    @Override
    public Activities apply(final Object[] selected) {
      final ActivitiesJoinTable t = new ActivitiesJoinTable(selected);
      return new ActivitiesImpl(t.get(t.actId), t.get(t.actName), t.get(t.modId), t.get(t.modName), t.get(t.modDescription), t.get(t.courseK),
              t.get(t.actParent), t.get(t.actChild), t.get(t.actVariantParent), t.get(t.actVariantChild), t.get(t.actType), t.get(t.actDatStartDateTime), t.get(t.actDatEndDateTime));
    }
  };
}
