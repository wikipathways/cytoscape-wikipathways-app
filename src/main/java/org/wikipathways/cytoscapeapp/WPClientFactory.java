// WikiPathways App for Cytoscape
//
// Copyright 2013-2014 WikiPathways
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.wikipathways.cytoscapeapp;

/**
 * Used as an OSGi service to create new WikiPathways clients.
 *
 * If your Cytoscape app needs to use the WikiPathways client,
 * get the {@code WPClientFactory} service through OSGi.
 * This can be done in your {@code CyActivator} class as follows:
 * <p><pre>
 * {@code 
 * final WPClientFactory wpClientFactory = super.getService(WPClientFactory.class);
 * final WPClient wpClient = wpClientFactory.create();
 * }
 * </pre></p>
 */
public interface WPClientFactory {
  /**
   * Create a new WikiPathways client.
   */
  WPClient create();
}
