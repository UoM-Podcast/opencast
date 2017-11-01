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
package org.opencastproject.workflow.handler.workflow;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;

import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Test class for {@link AddEpisodeAclWorkflowOperationHandler}
 */
public class AddEpisodeAclWorkflowOperationHandlerTest {

  private MediaPackage mp;

  @Before
  public void setUp() throws Exception {
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    mp = builder.loadFromXml(getClass().getResourceAsStream("/series_mediapackage.xml"));
  }

  @Test
  public void testNoAcl() throws Exception {
    WorkflowOperationResult result = getWorkflowOperationResult(mp, "/dublincore.xml", false);
    Assert.assertEquals(Action.SKIP, result.getAction());
  }

  @Test
  public void testAclFromConfig() throws Exception {
    WorkflowOperationResult result = getWorkflowOperationResult(mp, "/dublincore.xml", true);
    Assert.assertEquals(Action.CONTINUE, result.getAction());
  }

  @Test
  public void testAclAddedFromEpisode() throws Exception {
    WorkflowOperationResult result = getWorkflowOperationResult(mp, "/audience_dublincore.xml", false);
    Assert.assertEquals(Action.CONTINUE, result.getAction());
  }

  @Test
  public void testAclAddedFromEpisodeAndConfig() throws Exception {
    WorkflowOperationResult result = getWorkflowOperationResult(mp, "/audience_dublincore.xml", true);
    Assert.assertEquals(Action.CONTINUE, result.getAction());
  }

  private WorkflowOperationResult getWorkflowOperationResult(MediaPackage mp, String dublincore, boolean config) throws Exception {
    URI uri = getClass().getResource(dublincore).toURI();
    File file = new File(uri);

    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get(EasyMock.anyObject(URI.class))).andReturn(file).anyTimes();
    EasyMock.expect(workspace.read(EasyMock.anyObject(URI.class))).andReturn(file).anyTimes();
    EasyMock.expect(workspace.put(EasyMock.anyString(), EasyMock.anyString(), EasyMock.anyString(),
            EasyMock.anyObject(InputStream.class))).andReturn(uri).anyTimes();
    EasyMock.replay(workspace);
    AuthorizationService authorizationService = EasyMock.createNiceMock(AuthorizationService.class);
    EasyMock.replay(authorizationService);

    AddEpisodeAclWorkflowOperationHandler operationHandler = new AddEpisodeAclWorkflowOperationHandler();
    operationHandler.setWorkspace(workspace);
    operationHandler.setAuthorizationService(authorizationService);

    WorkflowInstanceImpl instance = new WorkflowInstanceImpl();
    List<WorkflowOperationInstance> ops = new ArrayList<>();
    WorkflowOperationInstanceImpl operation = new WorkflowOperationInstanceImpl("test", OperationState.INSTANTIATED);
    ops.add(operation);
    instance.setOperations(ops);
    instance.setMediaPackage(mp);

    if (config) {
      operation.setConfiguration(AddEpisodeAclWorkflowOperationHandler.LIST_OF_ROLES, "Role_2");
    }

    WorkflowOperationResult result = operationHandler.start(instance, null);

    return result;
  }

}
