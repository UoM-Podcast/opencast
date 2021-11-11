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
package org.opencastproject.archive.opencast;

import org.opencastproject.archive.api.ArchiveException;
import org.opencastproject.archive.api.UriRewriter;
import org.opencastproject.archive.api.Version;
import org.opencastproject.archive.base.persistence.Episode;
import org.opencastproject.archive.base.storage.ElementStore;
import org.opencastproject.job.api.AbstractJobProducer;
import org.opencastproject.job.api.Job;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.RequireUtil;
import org.opencastproject.util.data.Option;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class OpencastArchiveJobProducer extends AbstractJobProducer {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(OpencastArchiveJobProducer.class);

  public static final String JOB_TYPE = "org.opencastproject.archive";
  public static final Float JOB_LOAD = 0.1f;
  public static final Float NONTERMINAL_JOB_LOAD = 0.1f;

  public enum Operation {
    MoveById, MoveByIdAndVersion, MoveByIdAndDate, MoveByDate
  }

  private static final String OK = "OK";

  private OpencastArchive archive = null;
  private ServiceRegistry serviceRegistry = null;
  private SecurityService securityService = null;
  private UserDirectoryService userDirectoryService = null;
  private OrganizationDirectoryService organizationDirectoryService = null;
  private UriRewriter uriRewriter = null;

  public OpencastArchiveJobProducer() {
    super(JOB_TYPE);
  }

  /**
   * OSGi callback on component activation.
   *
   * @param cc
   *          the component context
   */
  @Override
  public void activate(ComponentContext cc) {
    logger.info("Activating tiered storage assetmanager job service");
    super.activate(cc);
  }

  public void setUriRewriter(UriRewriter rewriter) {
    this.uriRewriter = rewriter;
  }

  public boolean datastoreExists(String storeId) {
    Option<ElementStore> store = archive.getElementStore(storeId);
    return store.isSome();
  }

  @Override
  protected String process(Job job) throws ServiceRegistryException {
    Operation op = null;
    String operation = job.getOperation();
    List<String> arguments = job.getArguments();
    String id;
    String targetStore = arguments.get(0);
    Version version;
    Date start;
    Date end;
    try {
      op = Operation.valueOf(operation);
      switch (op) {
        case MoveById:
          id = arguments.get(1);
          return internalMoveById(id, targetStore);
        case MoveByIdAndVersion:
          id = arguments.get(1);
          version = Version.version(Long.parseLong(arguments.get(2)));
          return internalMoveByIdAndVersion(version, id, targetStore);
        case MoveByDate:
          start = new Date(Long.parseLong(arguments.get(1)));
          end = new Date(Long.parseLong(arguments.get(2)));
          return internalMoveByDate(start, end, targetStore);
        case MoveByIdAndDate:
          id = arguments.get(1);
          start = new Date(Long.parseLong(arguments.get(2)));
          end = new Date(Long.parseLong(arguments.get(3)));
          return internalMoveByIdAndDate(id, start, end, targetStore);
        default:
          throw new IllegalArgumentException("Unknown operation '" + operation + "'");
      }
    } catch (NotFoundException e) {
      throw new ServiceRegistryException("Error running job", e);
    } catch (Exception e) {
      throw new ServiceRegistryException("Error handling operation '" + op + "'", e);
    }
  }

  /**
   * Spawns a job to move a single mediapackage from its current storage to a
   * new target storage location
   *
   * @param version
   *  The {@link Version} to move
   * @param mpId
   *  The mediapackage ID of the mediapackage to move
   * @param targetStorage
   *  The {@link org.opencastproject.archive.base.storage.RemoteElementStore} ID
   *  where the mediapackage should be moved
   * @return
   */
  public Job moveByIdAndVersion(final Version version, final String mpId, final String targetStorage) {
    RequireUtil.notNull(version, "version");
    RequireUtil.notEmpty(mpId, "mpId");
    RequireUtil.notEmpty(targetStorage, "targetStorage");
    List<String> args = new LinkedList<String>();
    args.add(targetStorage);
    args.add(mpId);
    args.add(version.toString());

    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.MoveByIdAndVersion.toString(), args, null, true, JOB_LOAD);
    } catch (ServiceRegistryException e) {
      throw new ArchiveException("Unable to create a job", e);
    }
  }

  /**
   * Triggers the move operation inside the {@link OpencastArchive}
   *
   * @param version
   *  The {@link Version} to move
   * @param mpId
   *  The mediapackage ID of the mediapackage to move
   * @param targetStorage
   *  The {@link org.opencastproject.archive.base.storage.RemoteElementStore} ID
   *  where the mediapackage should be moved
   * @return
   *  The string "OK"
   * @throws NotFoundException
   */
  protected String internalMoveByIdAndVersion(final Version version, final String mpId, final String targetStorage) throws
          NotFoundException {
    archive.moveToStore(version, mpId, targetStorage, uriRewriter);
    return OK;
  }

  /**
   * Spawns a job to move a all archive versions of a mediapackage from their
   * current storage to a new target storage location
   *
   * @param mpId
   *  The mediapackage ID of the snapshot to move
   * @param targetStorage
   *  The {@link org.opencastproject.archive.base.storage.RemoteElementStore} ID 
   *  where the mediapackages should be moved
   * @return
   *  The {@link Job}
   */
  public Job moveById(final String mpId, final String targetStorage) {
    RequireUtil.notEmpty(mpId, "mpId");
    RequireUtil.notEmpty(targetStorage, "targetStorage");
    List<String> args = new LinkedList<String>();
    args.add(targetStorage);
    args.add(mpId);

    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.MoveById.toString(), args, null, true, NONTERMINAL_JOB_LOAD);
    } catch (ServiceRegistryException e) {
      throw new ArchiveException("Unable to create a job", e);
    }
  }

  /**
   * Moves all appropriate archive episodes to their new home
   *
   * @param mpId
   *  The mediapackage ID of the archive episode to move
   * @param targetStorage
   *  The {@link org.opencastproject.archive.base.storage.RemoteElementStore}
   *  ID where the mediapackages should be moved
   * @return
   *  The String containing the number of successful and failed moves
   *  [<> OK ][<> FAILED ]
   */
  protected String internalMoveById(final String mpId, final String targetStorage) {
    List<Episode> episodes = archive.getEpisodesById(mpId);
    String result = moveEpisodes(episodes, targetStorage);
    return result;
  }


  /**
   * Spawns a job to move a all move a all archive versions created between two
   * points from their current storage to a new target storage location
   *
   * @param start
   *  The start {@link Date}
   * @param end
   *  The end {@link Date}
   * @param targetStorage
   *  The {@link org.opencastproject.archive.base.storage.RemoteElementStore} ID
   *  where the mediapackages should be moved
   * @return
   *  The {@link Job}
   */
  public Job moveByDate(final Date start, final Date end, final String targetStorage) {
    RequireUtil.notNull(start, "start");
    RequireUtil.notNull(end, "end");
    RequireUtil.notNull(targetStorage, "targetStorage");
    List<String> args = new LinkedList<String>();
    args.add(targetStorage);
    args.add(Long.toString(start.getTime()));
    args.add(Long.toString(end.getTime()));

    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.MoveByDate.toString(), args, null, true, NONTERMINAL_JOB_LOAD);
    } catch (ServiceRegistryException e) {
      throw new ArchiveException("Unable to create a job", e);
    }
  }

  /**
   * Moves all appropriate archive episodes to their new home
   *
   * @param start
   *  The start {@link Date}
   * @param end
   *  The end {@link Date}
   * @param targetStorage
   *  The {@link org.opencastproject.archive.base.storage.RemoteElementStore} ID
   *  where the mediapackages should be moved
   * @return
   *  The String containing the number of successful and failed moves
   *  [<> OK ][<> FAILED ]
   */
  protected String internalMoveByDate(final Date start, final Date end, final String targetStorage) {
    List<Episode> episodes = archive.getEpisodesByDate(start, end);
    String result = moveEpisodes(episodes, targetStorage);
    return result;
  }

  /**
   * Spawns a job to move a all versions of a given mediapackage taken between 
   * two points from their current storage to a new target storage location
   *
   * @param mpId
   *  The mediapackage ID of the snapshot to move
   * @param start
   *  The start {@link Date}
   * @param end
   *  The end {@link Date}
   * @param targetStorage
   *  The {@link org.opencastproject.archive.base.storage.RemoteElementStore} ID 
   *  where the mediapackages should be moved
   * @return
   *  The {@link Job}
   */
  public Job moveByIdAndDate(final String mpId, final Date start, final Date end, final String targetStorage) {
    RequireUtil.notNull(mpId, "mpId");
    RequireUtil.notNull(start, "start");
    RequireUtil.notNull(end, "end");
    RequireUtil.notNull(targetStorage, "targetStorage");
    List<String> args = new LinkedList<String>();
    args.add(targetStorage);
    args.add(mpId);
    args.add(Long.toString(start.getTime()));
    args.add(Long.toString(end.getTime()));

    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.MoveByIdAndDate.toString(), args, null, true, NONTERMINAL_JOB_LOAD);
    } catch (ServiceRegistryException e) {
      throw new ArchiveException("Unable to create a job", e);
    }
  }

  /**
   * Moves all appropriate archive episodes to their new home
   *
   * @param mpId
   *  The mediapackage ID of the snapshot to move
   * @param start
   *  The start {@link Date}
   * @param end
   *  The end {@link Date}
   * @param targetStorage
   *  The {@link org.opencastproject.archive.base.storage.RemoteElementStore} ID
   *  where the snapshot should be moved
   * @return
   *  The String containing the number of successful and failed moves
   *  [<> OK ][<> FAILED ]
   */
  protected String internalMoveByIdAndDate(final String mpId, final Date start, final Date end, final String targetStorage) {
    List<Episode> episodes = archive.getEpisodesByIdAndDate(mpId, start, end);
    String result = moveEpisodes(episodes, targetStorage);
    return result;
  }

  /**
   * Moves all episodes in list to the targetStore
   *
   * @param episodes
   *  The list of episodes to move to the new target storage
   * @param targetStorage
   *  The {@link org.opencastproject.archive.base.storage.RemoteElementStore} ID where the snapshot should be moved
   * @return
   *  The Status string reporting the number of episodes succeeded and failed
   */
  private String moveEpisodes(final List<Episode> episodes, final String targetStorage) {
    int success = 0;
    int failed = 0;
    for (Episode e : episodes) {
      try {
        internalMoveByIdAndVersion(e.getVersion(), e.getMediaPackage().getIdentifier().toString(), targetStorage);
        success++;
      } catch (NotFoundException ex) {
        failed++;
        logger.warn(ex.getMessage());
      }
    }
    String result = (success > 0) ? Integer.toString(success) + " OK " : "";
    result += (failed > 0) ? Integer.toString(failed) + " FAILED " : "";
    return result;
  }

  protected void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  @Override
  protected ServiceRegistry getServiceRegistry() {
    return this.serviceRegistry;
  }

  protected void setArchive(OpencastArchive archive) {
    this.archive = archive;
  }

  protected void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  @Override
  protected SecurityService getSecurityService() {
    return this.securityService;
  }

  protected void setUserDirectoryService(UserDirectoryService uds) {
    this.userDirectoryService = uds;
  }

  @Override
  protected UserDirectoryService getUserDirectoryService() {
    return this.userDirectoryService;
  }


  protected void setOrganizationDirectoryService(OrganizationDirectoryService os) {
    this.organizationDirectoryService = os;
  }

  @Override
  protected OrganizationDirectoryService getOrganizationDirectoryService() {
    return this.organizationDirectoryService;
  }
}
