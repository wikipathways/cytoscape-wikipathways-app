package org.wikipathways.cytoscapeapp.internal.guiclient;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.ImageObserver;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JComponent;

class ImagePreview extends JComponent implements ImageObserver {
 	private static final long serialVersionUID = 1L;
 	ImageIcon img = null;

  public void setImage(final String urlPath) {
//    URL url = null;
    try {
//      url =s new URL(urlPath);
      img = new ImageIcon(new URL(urlPath));
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException(e);
    }
    super.repaint();
  }

  public void clearImage() {
    img = null;
    super.repaint();
  }

  protected void paintComponent(final Graphics g) {
    if (img != null && img.getImage() != null) {
      final Graphics2D g2d = (Graphics2D) g;
      g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

      final int cw = super.getWidth();
      final int ch = super.getHeight();
      final double ca = (double) ch / cw;

      final int iw = img.getIconWidth();
      final int ih = img.getIconHeight();
      final double ia = (double) ih / iw;

      if (cw > iw && ch > ih) {
        final int x = (cw - iw) / 2;
        final int y = (ch - ih) / 2;
        g2d.drawImage(img.getImage(), x, y, null);
      } else if (ca > ia) {
        final int dw = cw;
        final int dh = cw * ih / iw;
        final int x = 0;
        final int y = (ch - dh) / 2;
        g.drawImage(img.getImage(), x, y, dw, dh, null);
      } else { // iw <= ih
        final int dw = iw * ch / ih;
        final int dh = ch;
        final int x = (cw - dw) / 2;
        final int y = 0;
        g.drawImage(img.getImage(), x, y, dw, dh, null);
      }
    }
  }
}