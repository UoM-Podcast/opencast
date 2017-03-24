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

package org.opencastproject.pm.api.persistence;

import org.opencastproject.pm.api.Course;
import org.opencastproject.pm.api.Course.EmailStatus;
import org.opencastproject.pm.api.Person;
import org.opencastproject.pm.api.Recording;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Business object for a recording view.
 */
public class EmailView {

  private final List<Person> presenters;

  private final String course;

  private final Date startDate;

  private final String room;

  private final EmailStatus emailStatus;

  private final int numberAffected;

  public EmailView(List<Person> presenters, String course, Date start,
          String room, EmailStatus emailStatus, int numberAffected) {
    this.course = course;
    this.presenters = presenters;
    this.startDate = start;
    this.room = room;
    this.emailStatus = emailStatus;
    this.numberAffected = numberAffected;
  }

  public static EmailView fromRecording(Recording recording, int count) {
    final String c;
    if (recording.getCourse().isSome()) {
      c = recording.getCourse().get().getName();
    } else {
      c = "";
    }
    return new EmailView(
            recording.getStaff(),
            c,
            recording.getStart(),
            recording.getRoom().getName(),
            recording.getEmailStatus(),
            count
          );
  }

  public static EmailView fromCourse(Course course) {
    return new EmailView(
            new ArrayList<Person>(),
            course.getName(),
            new Date(),
            "",
            EmailStatus.valueOf(course.getEmailStatus().toString()),
            0
          );
  }

  public List<Person> getPresenters() {
    return presenters;
  }

  public String getCourse() {
    return course;
  }

  public Date getStartDate() {
    return startDate;
  }

  public String getRoom() {
    return room;
  }

  public EmailStatus getEmailStatus() {
    return emailStatus;
  }

  public int getNumberAffected() {
    return numberAffected;
  }
}
