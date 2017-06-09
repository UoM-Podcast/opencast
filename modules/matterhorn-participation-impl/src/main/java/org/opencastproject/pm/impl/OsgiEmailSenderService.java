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

import static java.lang.String.format;
import static org.opencastproject.util.OsgiUtil.getCfg;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.functions.Misc.ifThen;

import org.opencastproject.messages.MailService;
import org.opencastproject.messages.TemplateType;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase;
import org.opencastproject.pm.api.util.Security;
import org.opencastproject.util.Crypt;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.functions.Strings;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.List;

/**
 * OSGi bound implementation of {@link org.opencastproject.pm.impl.AbstractEmailSenderService} using Freemarker for
 * template rendering.
 */
public class OsgiEmailSenderService extends AbstractEmailSenderService implements ManagedService {
  /** Logging utilities */
  private static final Logger logger = LoggerFactory.getLogger(AbstractEmailSenderService.class);

  private Mode mode = new Mode.TestFileMode("/dev/null", 0, false);
  private String optOutLink = "";

  private MailService mailService;
  private ParticipationManagementDatabase db;
  private final FreemarkerTemplateRenderer renderer;

  public OsgiEmailSenderService() {
    this.renderer = new FreemarkerTemplateRenderer();
  }

  @SuppressWarnings("rawtypes")
  @Override
  public synchronized void updated(Dictionary d) throws ConfigurationException {
    if (d != null) {
      optOutLink = getCfg(d, "opt-out-link");
      mode = parseMode(getCfg(d, "mode")).orError(new ConfigurationException("mode", "Cannot parse mode string")).get();
      logger.info(format("Running EmailSenderService in mode %s. Updating recordings: %s", mode,
              mode.isUpdateRecordings()));
    }
  }

  public static Option<Mode> parseMode(String m) {
    if (m.startsWith("smtp")) {
      return Option.<Mode> some(new Mode.SmtpMode());
    } else {
      final List<String> as = mlist(m.split(":")).bind(Strings.trimToNone).value();
      if (as.size() == 4) {
        final String mode = as.get(0);
        final boolean updateRecording = Boolean.parseBoolean(as.get(3));
        for (int limit : parseOptNumber(as.get(2))) {
          if ("test-smtp".equals(mode)) {
            return Option.<Mode> some(new Mode.TestSmtpMode(as.get(1), limit, updateRecording));
          } else if ("test-file".equals(mode)) {
            return Option.<Mode> some(new Mode.TestFileMode(as.get(1), limit, updateRecording));
          } else {
            return none();
          }
        }
        return none();
      } else {
        return none();
      }
    }
  }

  private static Option<Integer> parseOptNumber(String number) {
    return some(number).map(ifThen("none", "-1")).bind(Strings.toInt);
  }

  @Override
  protected String renderInvitationBody(String template, TemplateType.Invitation.Data data) {
    return renderer.render(template, data);
  }

  @Override
  protected Mode getMode() {
    return mode;
  }

  @Override
  protected String getOptOutLink(String teacherId) {
    return format(optOutLink, Crypt.encrypt(Security.TEACHER_EMAIL_KEY, teacherId));
  }

  @Override
  protected ParticipationManagementDatabase getDb() {
    return db;
  }

  @Override
  protected MailService getMailService() {
    return mailService;
  }

  /** OSGi DI. */
  public void setDb(ParticipationManagementDatabase db) {
    this.db = db;
  }

  /** OSGi DI. */
  public void setMailService(MailService mailService) {
    this.mailService = mailService;
  }
}
