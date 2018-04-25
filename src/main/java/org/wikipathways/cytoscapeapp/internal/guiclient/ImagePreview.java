package org.wikipathways.cytoscapeapp.internal.guiclient;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.ImageObserver;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JComponent;

class ImagePreview extends JComponent implements ImageObserver {
 	private static final long serialVersionUID = 1L;
 	static ImageIcon img = null;

 	public static void main(String[] args) {
 		try {
 			System.out.println("main ");
 			ImagePreview preview = new ImagePreview();
 			preview.setImage("http://www.wikipathways.org//wpi/wpi.php?action=downloadFile&type=png&pwTitle=Pathway:WP1560");
 			System.out.println("img: " + img);
 			Image image = img.getImage();
 			
 			Thread.sleep(1000);
 			System.out.println(	image.getWidth(preview) + " x " + image.getHeight(preview));
 		} catch (Throwable t) {
 			System.out.println("Should not have seen exception " + t);
 		}
 	}

 	  public void setImage(final String urlPath) {
// 	    URL url = null;
 	    try {
// 	      url =s new URL(urlPath);
 	      img = new ImageIcon(new URL(urlPath));
 	    } catch (MalformedURLException e) {
 	      throw new IllegalArgumentException(e);
 	    }
 	    super.repaint();
 	  }
//
// 	  public void setImage(final String urlPath) {
//	URL url = null;
//    try {
//		System.out.println("url: " + urlPath);
//		url = new URL(urlPath);
////		 Iterator<ImageReader> readers = ImageIO.getImageReadersBySuffix("png");
////		 System.out.println("readers");
////		 while (readers.hasNext())
////		 {
////			 ImageReader reader = readers.next();
////			 System.out.println("reader: " + reader);
////		 }
//		 BufferedImage buffimg = ImageIO.read(url);
//		
//		if (buffimg != null)
//		{
//			System.out.println("buffimg: " + buffimg + ": " + buffimg.getWidth() + " x " + buffimg.getHeight());
//			img = new ImageIcon(buffimg);
//		}
//		else 
//	      img = new ImageIcon(new URL(urlPath));
////    img = new ImageIcon(url);
//    } 
//    catch (MalformedURLException e) 
//    {
//    		System.err.println("Malformed URL: " + urlPath);
//    		e.printStackTrace();
//    		throw new IllegalArgumentException(e);
//    } 
//    catch (IOException e) 
//    {
//    		System.err.println("IOException: " + urlPath);
//		e.printStackTrace();
//	}
//    repaint();
//  
//}
  public void clearImage() {
    img = null;
    repaint();
  }

  protected void paintComponent(final Graphics g) {
    if (img != null && img.getImage() != null) {
      final Graphics2D g2d = (Graphics2D) g;
      g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

      final int cw = super.getWidth();
      final int ch = super.getHeight();
     final double ca = (double) ch / cw;
 	System.out.println("paintComponent: " + img.getImage() + " "  +cw + " x " + ch);

      final int iw = img.getIconWidth();
      final int ih = img.getIconHeight();
      final double ia = (double) ih / iw;
    	System.out.println("icon size: " + iw + " x " + ih);

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