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

import org.cytoscape.work.Task;
import org.cytoscape.work.TaskMonitor;

/**
 * Specifies a task that can return a result.
 * This class is useful when needing to pass the result of one task
 * to a subsequent task in an iterator.
 */
public abstract class ResultTask<T> implements Task {
  /**
   * Run the task and return its result.
   */
  protected abstract T checkedRun(TaskMonitor monitor) throws Exception;

  protected T result = null;

  public void run(TaskMonitor monitor) throws Exception {
    result = checkedRun(monitor);
  }

  /**
   * Return the result that {@code checkedRun} returned.
   */
  public T get() {
    return result;
  }
}
