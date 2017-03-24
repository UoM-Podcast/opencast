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

package org.opencastproject.pm.impl.persistence;

import static org.opencastproject.util.RequireUtil.notEmpty;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.Option.some;

import org.opencastproject.pm.api.Course;
import org.opencastproject.pm.api.Course.EmailStatus;
import org.opencastproject.pm.api.SchedulingSource;
import org.opencastproject.pm.api.persistence.EmailView;

import org.opencastproject.util.data.Option;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Entity object for the course.
 */
@Entity(name = "Course")
@Table(name = "mh_pm_course", uniqueConstraints = { @UniqueConstraint(columnNames = { "course" }) })
@NamedQueries({ @NamedQuery(name = "Course.findAll", query = "SELECT c FROM Course c"),
        @NamedQuery(name = "Course.findById", query = "SELECT c FROM Course c WHERE c.courseId = :course"),
        @NamedQuery(name = "Course.findBySeries", query = "SELECT c FROM Course c WHERE c.seriesId = :series"),
        @NamedQuery(name = "Course.clear", query = "DELETE FROM Course"),
        @NamedQuery(name = "Course.countByEmailState", query = "SELECT COUNT(c) FROM Course c WHERE c.emailStatus = :emailState"),
        @NamedQuery(name = "Course.findByEmailState", query = "SELECT c FROM Course c WHERE c.emailStatus = :emailState")})
public class CourseDto {

  @Id
  @GeneratedValue
  @Column(name = "id")
  private long id;

  @Column(name = "course", nullable = false)
  private String courseId;

  @Column(name = "opted_out", nullable = false)
  private boolean optedOut;

  @Column(name = "series")
  private String seriesId;

  @Column(name = "name")
  private String name;

  @Column(name = "description")
  private String description;

  @Column(name = "external_course_key")
  private String externalCourseKey;

  @Column(name = "fingerprint", nullable = true, length = 32)
  private String fingerprint;

  /* requirements are stored as csv */
  @Column(name = "requirements", nullable = true, length = 255)
  private String requirements;

  /* email status for requirement notifications */
  @Enumerated(EnumType.STRING)
  private EmailStatus emailStatus;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "source", referencedColumnName = "id")
  private SchedulingSourceDto schedulingSource;

  /**
   * Default constructor
   */
  public CourseDto() {
  }

  /**
   * Constructor without only courseId
   *
   * @param courseId
   *          the syllabus+ identifier for this course
   */
  public CourseDto(String courseId) {
    this.courseId = notEmpty(courseId, "courseId");
  }

  /**
   * Constructor with all parameters
   *
   * @param courseId
   *          the syllabus+ course identifier
   * @param seriesId
   *          the matterhorn series identifier
   * @param name
   *          the course name
   * @param description
   *          the course description
   * @param externalCourseKey
   *          the external course key
   * @param optedOut
   *          the opted-out status
   * @param fingerprint
   *          the entry's hash value
   * @param requirements
   *          list of requirements
   */
  public CourseDto(String courseId, String seriesId, String name, String description, String externalCourseKey,
          boolean optedOut, String fingerprint, List<String> requirements) {
    this.courseId = notEmpty(courseId, "courseId");
    this.seriesId = seriesId;
    this.name = name;
    this.description = description;
    this.externalCourseKey = externalCourseKey;
    this.optedOut = optedOut;
    this.fingerprint = fingerprint;
    this.emailStatus = Course.EmailStatus.SENT;

    setRequirements(requirements);
  }

  /**
   * Sets the entity identifier
   *
   * @return the entity id
   */
  public void setId(long id) {
    this.id = id;
  }

  /**
   * Gets the entity identifier
   *
   * @return the entity id
   */
  public long getId() {
    return this.id;
  }

  /**
   * Sets the course identifier
   *
   * @param courseId
   *          the course id
   */
  public void setCourseId(String courseId) {
    this.courseId = courseId;
  }

  /**
   * Returns the course identifier
   *
   * @return the course id
   */
  public String getCourseId() {
    return courseId;
  }

  /**
   * Sets the MH series identifier
   *
   * @param seriesId
   *          the series id
   */
  public void setSeriesId(String seriesId) {
    this.seriesId = seriesId;
  }

  /**
   * Returns the MH series identifier
   *
   * @return the series id
   */
  public String getSeriesId() {
    return seriesId;
  }

  /**
   * Returns the course name
   *
   * @return the course name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the course name
   *
   * @param name
   *          the course name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the course description
   *
   * @return the course description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the course description
   *
   * @param description
   *          the course description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Returns the external course key
   *
   * @return the external course key
   */
  public String getExternalCourseKey() {
    return externalCourseKey;
  }

  /**
   * Sets the external course key
   *
   * @param externalCourseKey
   *          the external course key
   */
  public void setExternalCourseKey(String externalCourseKey) {
    this.externalCourseKey = externalCourseKey;
  }

  /**
   * Returns if the course is opted-out.
   *
   * @return true if the course is opted-out
   */
  public boolean isOptedOut() {
    return this.optedOut;
  }

  /**
   * Set the status of the opted-out parameter
   *
   * @param optedOut
   *          the new status of the opted-out parameter
   */
  public void setOptedOut(boolean optedOut) {
    this.optedOut = optedOut;
  }

  /**
   * Returns a fingerprint for this recording.
   *
   * @return the fingerprint
   */
  public Option<String> getFingerprint() {
    return Option.option(fingerprint);
  }

  /**
   * Sets a fingerprint for this course, which is supposed to be a 32 bit hash.
   *
   * @param fingerprint
   *          the fingerprint
   * @throws IllegalArgumentException
   *           if the hash is longer than 32 bit
   */
  public void setFingerprint(String fingerprint) throws IllegalArgumentException {
    if (StringUtils.isNotBlank(fingerprint) && fingerprint.length() > 32)
      throw new IllegalArgumentException("Fingerprint must not be longer than 32 bit");
    this.fingerprint = fingerprint;
  }

  /**
   * Returns the scheduling schedulingSource
   *
   * @return the scheduling schedulingSource
   */
  public Option<SchedulingSourceDto> getSchedulingSource() {
    return Option.option(schedulingSource);
  }

  /**
   * Sets the scheduling schedulingSource
   *
   * @param schedulingSource scheduling schedulingSource
   */
  public void setSchedulingSource(SchedulingSourceDto schedulingSource) {
    this.schedulingSource = schedulingSource;
  }


  public List<String> getRequirements() {
    if (requirements != null) {
      return new ArrayList<String>(Arrays.asList(requirements.split(",")));
    } else {
      return new ArrayList<String>();
    }
  }

  public void setRequirements(List<String> requirements) {
    this.requirements = StringUtils.join(requirements, ",");
  }

  /**
   * Sets the email status
   *
   * @param emailStatus
   *         the email status
   */
  public void setEmailStatus(EmailStatus emailStatus) {
    this.emailStatus = emailStatus;
  }

  /**
   * Returns the email status
   *
   * @return the email status
   */
  public EmailStatus getEmailStatus() {
    return emailStatus;
  }

  /**
   * Returns the business object of this course
   *
   * @return the business object model of this course
   */
  public Course toCourse() {
    Option<SchedulingSource> s = schedulingSource == null ? none(SchedulingSource.class) : some(schedulingSource.toSchedulingSource());
    Course course = new Course(id, courseId, seriesId, name, description, externalCourseKey, option(fingerprint));
    course.setOptedOut(optedOut);
    course.setSchedulingSource(s);
    course.setEmailStatus(emailStatus);

    // convert csv to list
    course.setRequirements(this.getRequirements());

    return course;
  }

  public EmailView toEmailView() {
    return EmailView.fromCourse(toCourse());
  }
}
