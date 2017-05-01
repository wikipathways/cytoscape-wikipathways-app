package org.wikipathways.cytoscapeapp.internal.cmd;

import java.io.Reader;
import java.util.regex.Pattern;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.wikipathways.cytoscapeapp.GpmlConversionMethod;
import org.wikipathways.cytoscapeapp.GpmlReaderFactory;
import org.wikipathways.cytoscapeapp.ResultTask;
import org.wikipathways.cytoscapeapp.WPClient;
import org.wikipathways.cytoscapeapp.WPPathway;

public class WPImportCmdTask extends AbstractTask {
  static final Pattern WP_ID_REGEX = Pattern.compile("WP\\d+");

  @Tunable
  public String id;

  final WPClient client;
  final GpmlReaderFactory factory;
  final GpmlConversionMethod method;

  public WPImportCmdTask(
      final WPClient client,
      final GpmlReaderFactory factory,
      final GpmlConversionMethod method
    ) {
    this.client = client;
    this.factory = factory;
    this.method = method;
  }

  public void run(TaskMonitor monitor) {
    if (id == null || id.length() == 0) {
      throw new IllegalArgumentException("id must be specified");
    }

    if (!WP_ID_REGEX.matcher(id).matches()) {
      throw new IllegalArgumentException("id must follow this regular format: " + WP_ID_REGEX.pattern());
    }

    final ResultTask<WPPathway> infoTask = client.newPathwayInfoTask(id);
    super.insertTasksAfterCurrentTask(
      infoTask, new AbstractTask() {
        public void run(TaskMonitor monitor) {
          if (infoTask.get() == null) {
            throw new IllegalArgumentException("No such pathway with ID: " + id);
          }
          
          final ResultTask<Reader> gpmlTask = client.newGPMLContentsTask(infoTask.get());
          super.insertTasksAfterCurrentTask(gpmlTask, new AbstractTask() {
            public void run(TaskMonitor monitor) {
              super.insertTasksAfterCurrentTask(factory.createReaderAndViewBuilder(gpmlTask.get(), method));
            }
          });
        }
      });
  }
}
