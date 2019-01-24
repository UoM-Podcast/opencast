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
package org.opencastproject.waveform.endpoint;

import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.rest.AbstractJobProducerEndpoint;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.opencastproject.waveform.api.WaveformService;
import org.opencastproject.waveform.api.WaveformServiceException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
@RestService(name = "WaveformServiceEndpoint", title = "Waveform Service REST Endpoint",
        abstractText = "The Waveform Service generates a waveform image from a media file with at least one audio channel.",
        notes = {"All paths above are relative to the REST endpoint base (something like http://your.server/waveform)"})
public class WaveformServiceEndpoint extends AbstractJobProducerEndpoint {
  private static final Logger logger = LoggerFactory.getLogger(WaveformServiceEndpoint.class);

  private ServiceRegistry serviceRegistry = null;
  private WaveformService waveformService = null;

  @POST
  @Path("/create")
  @Produces({MediaType.APPLICATION_XML})
  @RestQuery(name = "create", description = "Create a waveform image from the given track",
          returnDescription = "Media package attachment for the generated waveform.",
          restParameters = {
            @RestParameter(name = "track", type = RestParameter.Type.TEXT,
                    description = "Track with at least one audio channel.", isRequired = true),
            @RestParameter(name = "color", type = RestParameter.Type.STRING, defaultValue = "black",
                    description = "Waveform image color, see https://www.ffmpeg.org/ffmpeg-all.html#Color .", isRequired = false),
            @RestParameter(name = "height", type = RestParameter.Type.STRING, defaultValue = "500",
                    description = "Waveform image height in pixels.", isRequired = false),
            @RestParameter(name = "minWidth", type = RestParameter.Type.STRING, defaultValue = "5000",
                    description = "Waveform image minimum width in pixels.", isRequired = false),
            @RestParameter(name = "maxWidth", type = RestParameter.Type.STRING, defaultValue = "20000",
                    description = "Waveform image maximum width in pixels.", isRequired = false),
            @RestParameter(name = "widthPPM", type = RestParameter.Type.STRING, defaultValue = "200",
                    description = "Waveform image width in pixels per minute of video duration.", isRequired = false),
            @RestParameter(name = "scale", type = RestParameter.Type.STRING, defaultValue = "lin",
                    description = "Waveform image scale can be lin or log.", isRequired = false)
          },
          reponses = {
            @RestResponse(description = "Waveform generation job successfully created.",
                    responseCode = HttpServletResponse.SC_OK),
            @RestResponse(description = "The given track can't be parsed.",
                    responseCode = HttpServletResponse.SC_BAD_REQUEST),
            @RestResponse(description = "Internal server error.",
                    responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
          })
  public Response createWaveformImage(@FormParam("track") String track,
          @FormParam("color") String color,
          @FormParam("height") String height,
          @FormParam("minWidth") String minWidth,
          @FormParam("maxWidth") String maxWidth,
          @FormParam("widthPPM") String widthPPM,
          @FormParam("scale") String scale) {
    try {
      MediaPackageElement sourceTrack = MediaPackageElementParser.getFromXml(track);
      if (!Track.TYPE.equals(sourceTrack.getElementType())) {
        return Response.status(Response.Status.BAD_REQUEST).entity("Track element must be of type track").build();
      }

      Map<String, String> filterHash = new HashMap<>();
      if (!StringUtils.isBlank(color)) {
        filterHash.put("waveformColor", color);
      }
      if (!StringUtils.isBlank(height)) {
        filterHash.put("waveformImageHeight", height);
      }
      if (!StringUtils.isBlank(minWidth)) {
        filterHash.put("waveformImageWidthMin", minWidth);
      }
      if (!StringUtils.isBlank(maxWidth)) {
        filterHash.put("waveformImageWidthMax", maxWidth);
      }
      if (!StringUtils.isBlank(widthPPM)) {
        filterHash.put("waveformImageWidthPPM", widthPPM);
      }
      if (!StringUtils.isBlank(scale)) {
        filterHash.put("waveformScale", scale);
      }
      Job job = waveformService.createWaveformImage((Track) sourceTrack, filterHash);
      return Response.ok().entity(new JaxbJob(job)).build();
    } catch (WaveformServiceException ex) {
      logger.error("Creating waveform job for track {} failed: {}", track, ExceptionUtils.getStackTrace(ex));
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    } catch (MediaPackageException ex) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Track element parsing failure").build();
    }
  }

  @Override
  public JobProducer getService() {
    if (waveformService instanceof JobProducer) {
      return (JobProducer) waveformService;
    } else {
      return null;
    }
  }

  @Override
  public ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  public void setWaveformService(WaveformService waveformService) {
    this.waveformService = waveformService;
  }
}
