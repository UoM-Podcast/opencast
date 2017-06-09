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

package org.opencastproject.pm.impl;

import static org.junit.Assert.assertEquals;
import static org.opencastproject.pm.impl.OsgiEmailSenderService.parseMode;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;

import org.opencastproject.pm.impl.AbstractEmailSenderService.Mode;

import org.junit.Test;

public class OsgiEmailSenderServiceTest {
  @Test
  public void testParseMode() {
    assertEquals(none(Mode.class), parseMode(""));
    assertEquals(none(Mode.class), parseMode("bla"));
    assertEquals(none(Mode.class), parseMode("test-smtp"));
    assertEquals(none(Mode.class), parseMode("test-smtp:::"));
    assertEquals(none(Mode.class), parseMode("test-smtp:::"));
    assertEquals(none(Mode.class), parseMode("test-smtp:root@localhost::"));
    assertEquals(none(Mode.class), parseMode("test-smtp:root@localhost:100a:"));
    assertEquals(none(Mode.class), parseMode("test-smtp:root@localhost:100a:false"));
    assertEquals(some(new Mode.TestSmtpMode("root@localhost", 100, false)),
            parseMode("test-smtp:root@localhost:100:false"));
    assertEquals(some(new Mode.SmtpMode()), parseMode("smtp"));
    assertEquals(some(new Mode.TestFileMode("/dev/null", -1, true)), parseMode("test-file:/dev/null:none:true"));
  }
}
