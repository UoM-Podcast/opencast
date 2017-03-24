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
import static org.opencastproject.util.data.Collections.list;
import static org.opencastproject.util.data.Collections.map;
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.messages.TemplateType.Invitation;
import org.opencastproject.util.IoSupport;

import org.junit.Test;

public class FreemarkerTemplateRendererTest {
  @Test
  public void testRenderInvitationMap() throws Exception {
    final FreemarkerTemplateRenderer r = new FreemarkerTemplateRenderer();
    final String template = IoSupport.loadTxtFromClassPath("mail-template-invitation.ftl", getClass()).get();
    final String rendered = r.render(
            template,
            map(tuple("staff", "Dr. Lurch"),
                    tuple("modules",
                            list(map(tuple("name", "Module 1"), tuple("description", "This is module no. 1")),
                                    map(tuple("name", "Module 2"), tuple("description", "This is module no. 2")))),
                    tuple("optOutLink", "http://manchester.uk.ac/pmm/optout")));

    assertEquals(IoSupport.loadTxtFromClassPath("mail-template-invitation-rendered.txt", getClass()).get(), rendered);
  }

  @Test
  public void testRenderInvitationData() throws Exception {
    final FreemarkerTemplateRenderer r = new FreemarkerTemplateRenderer();
    final String template = IoSupport.loadTxtFromClassPath("mail-template-invitation.ftl", getClass()).get();
    final String rendered = r.render(
            template,
            Invitation.data(
                    "Dr. Lurch",
                    "http://manchester.uk.ac/pmm/optout",
                    list(Invitation.module("Module 1", "This is module no. 1"),
                            Invitation.module("Module 2", "This is module no. 2"))));

    assertEquals(IoSupport.loadTxtFromClassPath("mail-template-invitation-rendered.txt", getClass()).get(), rendered);
  }

  @Test
  public void testRenderInvitationDataDescriptionMissing() throws Exception {
    final FreemarkerTemplateRenderer r = new FreemarkerTemplateRenderer();
    final String template = IoSupport.loadTxtFromClassPath("mail-template-invitation.ftl", getClass()).get();
    final String rendered = r.render(
            template,
            Invitation.data("Dr. Lurch", "http://manchester.uk.ac/pmm/optout",
                    list(Invitation.module("Module 1", "This is module no. 1"), Invitation.module("Module 2", null))));
    assertEquals(IoSupport
            .loadTxtFromClassPath("mail-template-invitation-rendered-description-missing.txt", getClass()).get(),
            rendered);
  }

  @Test
  public void testRenderInvitationDataDescriptionEmpty() throws Exception {
    final FreemarkerTemplateRenderer r = new FreemarkerTemplateRenderer();
    final String template = IoSupport.loadTxtFromClassPath("mail-template-invitation.ftl", getClass()).get();
    final String rendered = r.render(
            template,
            Invitation.data("Dr. Lurch", "http://manchester.uk.ac/pmm/optout",
                    list(Invitation.module("Module 1", "This is module no. 1"), Invitation.module("Module 2", ""))));
    assertEquals(IoSupport
            .loadTxtFromClassPath("mail-template-invitation-rendered-description-missing.txt", getClass()).get(),
            rendered);
  }
}
