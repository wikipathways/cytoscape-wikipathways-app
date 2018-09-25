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

import java.util.HashMap;
import java.util.Map;

import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.annotations.AnnotationFactory;
import org.cytoscape.view.presentation.annotations.AnnotationManager;
import org.cytoscape.view.presentation.annotations.ArrowAnnotation;
import org.cytoscape.view.presentation.annotations.ShapeAnnotation;
import org.cytoscape.view.presentation.annotations.TextAnnotation;

/**
 * A wrapper for the kafka-esque annotations API.
 */
public class Annots {
  final AnnotationManager mgr;
  final AnnotationFactory<ArrowAnnotation> arrowFct;
  final AnnotationFactory<ShapeAnnotation> shapeFct;
  final AnnotationFactory<TextAnnotation> textFct;

  public AnnotationManager getAnnotationManager()	{ return mgr;	}
  public Annots(CyServiceRegistrar registrar)
   {
	  mgr =   registrar.getService(AnnotationManager.class);
	  arrowFct =  (AnnotationFactory<ArrowAnnotation>) registrar.getService(AnnotationFactory.class,"(type=ArrowAnnotation.class)");
	  shapeFct =(AnnotationFactory<ShapeAnnotation>)registrar.getService( AnnotationFactory.class,"(type=ShapeAnnotation.class)");
	  textFct = (AnnotationFactory<TextAnnotation>) registrar.getService( AnnotationFactory.class,"(type=TextAnnotation.class)");
  }

  static Map<String,String> ezMap(Object[] elems) {
    final Map<String,String> map = new HashMap<String,String>();
    for (int i = 0; i < elems.length-1; i += 2) {
      map.put(elems[i].toString(), elems[i+1].toString());
    }
    return map;
  }

  public ArrowAnnotation newArrow(final CyNetworkView netView, Object ... args) {
    final ArrowAnnotation annot = arrowFct.createAnnotation(ArrowAnnotation.class, netView, ezMap(args));
//    mgr.addAnnotation(annot);
    return annot;
  }
//
//  public ShapeAnnotation addShape(ShapeAnnotation annot) {
//    mgr.addAnnotation(annot);
//    return annot;
//  }

  public ShapeAnnotation newShape(final CyNetworkView netView, Object ... args) {
    final ShapeAnnotation annot = shapeFct.createAnnotation(ShapeAnnotation.class, netView, ezMap(args));
//    mgr.addAnnotation(annot);
    return annot;
  }

  public TextAnnotation newText(final CyNetworkView netView, Object ... args) {
    final TextAnnotation annot = textFct.createAnnotation(TextAnnotation.class, netView, ezMap(args));
//    mgr.addAnnotation(annot);
    return annot;
  }
}
