package org.wikipathways.cytoscapeapp;

import java.util.Map;
import java.util.HashMap;

import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.annotations.ArrowAnnotation;
import org.cytoscape.view.presentation.annotations.ShapeAnnotation;
import org.cytoscape.view.presentation.annotations.TextAnnotation;
import org.cytoscape.view.presentation.annotations.AnnotationFactory;
import org.cytoscape.view.presentation.annotations.AnnotationManager;

/**
 * A wrapper for the kafka-esque annotations API.
 */
public class Annots {
  final AnnotationManager mgr;
  final AnnotationFactory<ArrowAnnotation> arrowFct;
  final AnnotationFactory<ShapeAnnotation> shapeFct;
  final AnnotationFactory<TextAnnotation> textFct;

  public Annots(
      final AnnotationManager mgr,
      final AnnotationFactory<ArrowAnnotation> arrowFct,
      final AnnotationFactory<ShapeAnnotation> shapeFct,
      final AnnotationFactory<TextAnnotation> textFct) {
    this.mgr = mgr;
    this.arrowFct = arrowFct;
    this.shapeFct = shapeFct;
    this.textFct = textFct;
  }

  static Map<String,String> ezMap(Object[] elems) {
    final Map<String,String> map = new HashMap<String,String>();
    for (int i = 0; i < elems.length-1; i += 2) {
      map.put(elems[i].toString(), elems[i+1].toString());
    }
    System.out.println("ezMap: " + map.toString());
    return map;
  }

  public ArrowAnnotation newArrow(final CyNetworkView netView, Object ... args) {
    final ArrowAnnotation annot = arrowFct.createAnnotation(ArrowAnnotation.class, netView, ezMap(args));
    mgr.addAnnotation(annot);
    return annot;
  }

  public ShapeAnnotation newShape(final CyNetworkView netView, Object ... args) {
    final ShapeAnnotation annot = shapeFct.createAnnotation(ShapeAnnotation.class, netView, ezMap(args));
    mgr.addAnnotation(annot);
    return annot;
  }

  public TextAnnotation newText(final CyNetworkView netView, Object ... args) {
    final TextAnnotation annot = textFct.createAnnotation(TextAnnotation.class, netView, ezMap(args));
    mgr.addAnnotation(annot);
    return annot;
  }
}
