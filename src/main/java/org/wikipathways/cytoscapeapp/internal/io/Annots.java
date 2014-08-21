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

package org.wikipathways.cytoscapeapp.internal.io;

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
    for (int i = 0; i < elems.length; i += 2) {
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
