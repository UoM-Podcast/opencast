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

import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;

import org.opencastproject.util.data.Function2;
import org.opencastproject.util.data.Option;

import org.joda.time.DateMidnight;
import org.joda.time.DateTime;

import java.util.Date;

import javax.annotation.Nullable;

/** Some utility functions. */
public final class SyllabusUtil {
  private SyllabusUtil() {
  }

  public static Option<DateMidnight> toDateMidnightOpt(@Nullable Date date) {
    return date != null ? some(toDateMidnight(date)) : none(DateMidnight.class);
  }

  public static DateMidnight toDateMidnight(Date date) {
    return new DateMidnight(date.getTime());
  }

  public static Option<DateTime> toDateTimeOpt(@Nullable Date date) {
    return date != null ? some(toDateTime(date)) : none(DateTime.class);
  }

  public static DateTime toDateTime(Date date) {
    return new DateTime(date.getTime());
  }

  /** Return the younger date of the two. */
  public static DateTime younger(DateTime a, DateTime b) {
    return a.isAfter(b) ? a : b;
  }

  private static final DateTime BIG_BANG = new DateTime(Long.MIN_VALUE);

  /** Calculate the youngest of all given dates. */
  public static DateTime youngest(DateTime... as) {
    return mlist(as).foldl(BIG_BANG, new Function2<DateTime, DateTime, DateTime>() {
      @Override public DateTime apply(DateTime youngest, DateTime a) {
        return younger(youngest, a);
      }
    });
  }
}
