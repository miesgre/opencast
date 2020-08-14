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

package org.opencastproject.adminui.endpoint;

import static org.apache.http.HttpStatus.SC_OK;

import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.google.gson.Gson;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/")
@RestService(name = "VersionService", title = "Version service",
  abstractText = "Provides latest opencast version",
  notes = { "This service offers the GET method to retrieve the latest opencast version from https://api.github.com ."})
@Component(
  immediate = true,
  service = VersionEndpoint.class,
  property = {
    "service.description=Admin UI - Latest Version Endpoint",
    "opencast.service.type=org.opencastproject.adminui.endpoint.VersionEndpoint",
    "opencast.service.path=/admin-ng/oc-version"
  }
)
public class VersionEndpoint {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(VersionEndpoint.class);

  /** The version */
  private String version = "";

  /** GitHub URL */
  private String url = "https://api.github.com/repos/opencast/opencast/releases/latest";

  /** The date */
  private long lastUpdated = 0;

  private static final Gson gson = new Gson();

  /** OSGi callback. */
  @Activate
  protected void activate() {
    logger.info("Activate the Admin ui - Latest version endpoint");
  }

  @GET
  @Path("version.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "latestversion", description = "Returns the latest Opencast version",
          returnDescription = "Returns the latest Opencast version retrieved from GitHub",
          responses = { @RestResponse(responseCode = SC_OK, description = "The latest Opencast version.") })
  public String getVersion() {
    if (System.currentTimeMillis() / 1000L - lastUpdated >= 3600) {
      updateVersion();
    }
    return gson.toJson(version);
  }

  private synchronized void updateVersion() {
    if (System.currentTimeMillis() / 1000L - lastUpdated < 3600) {
      return;
    }
    try {
      HttpClient client = HttpClientBuilder.create().build();
      HttpGet request = new HttpGet(url);
      HttpResponse response = client.execute(request);
      String responseString = IOUtils.toString(response.getEntity().getContent(), "utf-8");
      Map data = gson.fromJson(responseString, Map.class);
      version = (String) data.get("tag_name");
      lastUpdated = System.currentTimeMillis() / 1000L;
    } catch (Exception e) {
      logger.warn("Error while parsing the Opencast version from GitHub", e);
    }
  }
}
