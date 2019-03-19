package org.wikipathways.cytoscapeapp.impl;

import java.io.File;
import java.io.Reader;

import org.cytoscape.io.read.CyNetworkReader;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.TaskIterator;
import org.wikipathways.cytoscapeapp.WPClient;

/**
 * Creates {@link org.cytoscape.work.TaskIterator}s for reading a GPML document
 * and converting it to a Cytoscape network.
 *
 * <p>
 * This interface is an alternative to {@code CyNetworkReader}.
 * This app exports a {@link org.cytoscape.io.read.InputStreamTaskFactory}
 * implementation that can read a GPML document. If this app is loaded,
 * an instance of {@code CyNetworkReader}
 * that reads a GPML document can be retrieved from {@code CyNetworkReaderManager}.
 * However, the {@code CyNetworkReader} instance
 * has some limitations:
 * <ul>
 * <li>
 * {@code CyNetworkReaderManager.getReader(URI, String)}
 * is the only way to retrieve a GPML reader instance.
 * {@code CyNetworkReaderManager.getReader(InputStream, String)}
 * will not work even if the {@code InputStream} contains
 * a valid GPML document.
 * </li>
 * <li>
 * It's difficult to correctly implement the method {@link CyNetworkReader#buildCyNetworkView}.
 * It does not allow for executing a task iterator that performs
 * crucial steps in building the network view, like running a layout algorithm.
 * The implementation could inject a {@code SynchronousTaskManager} and use it to
 * run a task iterator, but this would not provide crucial user feedback.
 * </li>
 * <li>
 * {@code GpmlReaderFactory} uses {@link java.io.Reader},
 * whereas {@code CyNetworkReader} uses
 * {@link java.io.InputStream}. This is because {@code Reader}s include
 * the character encoding with the underlying bytestream, which is crucial
 * for correctly reading XML text streams across platforms.
 * </li>
 * </ul>
 * </p>
 *
 * <p>
 * This interface aims to address these limitations. It also allows for
 * separating stages of building the {@code CyNetwork} from
 * building the {@code CyNetworkView}.
 * </p>
 *
 * <p>
 * If your Cytoscape app needs to read GPML files,
 * get the {@code GpmlReaderFactory} service through OSGi.
 * This can be done in your {@code CyActivator} class as follows:
 * <pre>
 * {@code 
 * final GpmlReaderFactory gpmlReaderFactory = super.getService(GpmlReaderFactory.class);
 * }
 * </pre>
 * </p>
 */
public interface GpmlReaderFactory {
  /**
   * Creates a task iterator that reads the GPML pathway from {@code gpmlContents} and builds the
   * converted GPML pathway in the given {@code network}.
   *
   * <p>
   * The caller is required to pass in a {@code CyNetwork} instance. Callers can obtain
   * an instance from {@link org.cytoscape.model.CyNetworkFactory} and register the instance with
   * {@link org.cytoscape.model.CyNetworkManager}.
   * </p>
   *
   * @param gpmlContents A {@link java.io.Reader} that contains the GPML XML document; this can be a
   * {@link java.io.FileReader} instance for GPML files on disk or the result of {@link WPClient#newGPMLContentsTask}.
   *
   * @param network The {@link org.cytoscape.model.CyNetwork} instance that will store the converted GPML pathway.
   *
   * @param method The method by which the GPML pathway is converted to a Cytoscape network.
   */
  public TaskIterator createReader(
		  final String id, final Reader gpmlContents,
		  final CyNetwork network, final GpmlConversionMethod method, final File f);

  /**
   * Creates a task iterator that builds the network view for a network that contains a GPML pathway.
   *
   * <p>
   * The caller is required to pass in a {@code CyNetworkView} instance. Callers can obtain
   * an instance from {@link org.cytoscape.view.model.CyNetworkViewFactory} and register the instance with
   * {@link org.cytoscape.view.model.CyNetworkViewManager}.
   * </p>
   *
   * @param gpmlNetwork This <em>must</em> be the same {@code CyNetwork} as was passed in {@link #createReader}.
   *
   * @throws IllegalArgumentException if {@code gpmlNetwork} is a {@code CyNetwork} that doesn't have a GPML pathway.
   */
  public TaskIterator createViewBuilder( final String id, final CyNetwork gpmlNetwork, final CyNetworkView networkView);

  /**
   * Creates a task iterator that reads the GPML pathway from {@code gpmlContents}, builds the converted
   * GPML pathway, and builds the network view.
   *
   * <p>
   * This method is a convenience that combines the {@link org.cytoscape.work.Task}s from {@link #createReader}
   * and {@link #createViewBuilder} into a single {@link org.cytoscape.work.TaskIterator}.
   * </p>
   *
   * <p>
   * The caller is required to pass in a {@code CyNetworkView} instance. Callers can obtain
   * an instance from {@link org.cytoscape.view.model.CyNetworkViewFactory} and register the instance with
   * {@link org.cytoscape.view.model.CyNetworkViewManager}.
   * </p>
   *
   * @param gpmlContents A {@link java.io.Reader} that contains the GPML XML document; this can be a
   * {@link java.io.FileReader} instance for GPML files on disk or the result of {@link WPClient#newGPMLContentsTask}.
   *
   * @param conversionMethod The method by which the GPML pathway is converted to a Cytoscape network.
   *
   * @param setNetworkName If true, this method will update the {@code networkView}'s name to the pathway's name specified in {@code gpmlContents}.
   */
//  public TaskIterator createReaderAndViewBuilder(
//			 final String id, final Reader gpmlContents, final CyNetworkView networkView,
//			 final GpmlConversionMethod method);
//
  /**
   * Creates a task iterator that reads the GPML pathway from {@code gpmlContents}, builds the converted
   * GPML pathway, and builds the network view.
   *
   * <p>
   * This method is a convenience that creates a new {@code CyNetworkView} then
   * combines the {@link org.cytoscape.work.Task}s from {@link #createReader}
   * and {@link #createViewBuilder} into a single {@link org.cytoscape.work.TaskIterator}.
   * </p>
   *
   * @param gpmlContents A {@link java.io.Reader} that contains the GPML XML document; this can be a
   * {@link java.io.FileReader} instance for GPML files on disk or the result of {@link WPClient#newGPMLContentsTask}.
   *
   * @param conversionMethod The method by which the GPML pathway is converted to a Cytoscape network.
   */
  public TaskIterator createReaderAndViewBuilder( final String id, final Reader rdr, final GpmlConversionMethod method, File f);
  public WPManager getWPManager();

  public void setClient(WPClient client);
  public WPClient getClient();
//
//public void setSemaphore();
//public void clearSemaphore();
}
