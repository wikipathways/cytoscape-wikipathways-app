package org.wikipathways.cytoscapeapp.internal.io;

import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import java.awt.Color;

import org.wikipathways.cytoscapeapp.internal.CyActivator;

public class GpmlVizStyle {
  private static VisualStyle create() {
    final VisualStyle vizStyle = CyActivator.vizStyleFactory.createVisualStyle(CyActivator.vizMapMgr.getDefaultVisualStyle());
    vizStyle.setTitle("WikiPathways");
    vizStyle.setDefaultValue(BasicVisualLexicon.NODE_FILL_COLOR, Color.WHITE);
    CyActivator.vizMapMgr.addVisualStyle(vizStyle);
    return vizStyle;
  }

  private static VisualStyle VIZ_STYLE = null;

  public static VisualStyle get() {
    if (VIZ_STYLE == null)
      VIZ_STYLE = create();
    return VIZ_STYLE;
  }
}
