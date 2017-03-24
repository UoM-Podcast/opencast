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

package org.opencastproject.pm.impl.scheduling;

import static org.junit.Assert.assertEquals;

import org.opencastproject.util.data.Tuple;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Test cases for class {@link ScheduleFeederRunner}
 */
public class SchedulerFeederRunnerTest {

  private static final ScheduleFeederRunner.Feeder feeder = new ScheduleFeederRunner.Feeder();
  private static final SimpleDateFormat local = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS");

  /** Test method for {@link ScheduleFeederRunner.Feeder#getSyncIntervals(Date, Date)} */
  @Test
  public void testFeederGetSyncIntervals() throws Exception {
    final Date start = local.parse("2014-09-18T08:00:15.000");
    final Date end = local.parse("2014-09-20T23:59:59.999");

    List<Tuple<Date, Date>> syncIntervals = feeder.getSyncIntervals(start, end);

    // 18-20 is equal to 3 days
    assertEquals(3, syncIntervals.size());

    // First interval: Sept 18, 2014, 08:00:15.000 - 23:59:59.999
    assertEquals(start, syncIntervals.get(0).getA());
    assertEquals(local.parse("2014-09-18T23:59:59.999"), syncIntervals.get(0).getB());

    // Second interval Sept 19, 2014, 00:00:00.000 - 23:59:59.999
    assertEquals(local.parse("2014-09-19T00:00:00.000"), syncIntervals.get(1).getA());
    assertEquals(local.parse("2014-09-19T23:59:59.999"), syncIntervals.get(1).getB());

    // Second interval Sept 19, 2014, 00:00:00.000 - 23:59:59.999
    assertEquals(local.parse("2014-09-20T00:00:00.000"), syncIntervals.get(2).getA());
    assertEquals(end, syncIntervals.get(2).getB());
  }

  /** Test method for {@link ScheduleFeederRunner.Feeder#getSyncIntervals(Date, Date)} */
  @Test(expected = IllegalArgumentException.class)
  public void testFeederGetSyncIntervalsWithInvalidDates() {
    final Date now = new Date();
    feeder.getSyncIntervals(new Date(now.getTime() + 1000L), now);
  }

  /** Test method for {@link ScheduleFeederRunner.Feeder#getSyncIntervals(Date, Date)} */
  @Test(expected = IllegalArgumentException.class)
  public void testFeederGetSyncIntervalsWithNullStartDate() {
    feeder.getSyncIntervals(null, new Date());
  }

  /** Test method for {@link ScheduleFeederRunner.Feeder#getSyncIntervals(Date, Date)} */
  @Test(expected = IllegalArgumentException.class)
  public void testFeederGetSyncIntervalsWithNullEndDate() {
    feeder.getSyncIntervals(new Date(), null);
  }

}
