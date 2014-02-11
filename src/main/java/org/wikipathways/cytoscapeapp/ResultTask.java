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
