package org.wikipathways.cytoscapeapp.internal.guiclient;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;

class ImagePreview extends JComponent implements ImageObserver {
 	private static final long serialVersionUID = 1L;
 	static ImageIcon imgIcon = null;
 	static String url = "https://www.wikipathways.org//wpi/wpi.php?action=downloadFile&type=png&pwTitle=Pathway:WP1560";
// 	static String url = "https://www.google.com/images/branding/googlelogo/2x/googlelogo_color_272x92dp.png";
 	public static void main(String[] args) {
 		try {
 			System.out.println("main ");
 			ImagePreview preview = new ImagePreview();
 			preview.setImage(url);
 			 BufferedImage image;
 			 try 
 			{
 			    image = ImageIO.read(new URL(url));		// coming back null for wiki request
 			    if (image != null)
 			    {
 			    	if (image.getWidth(preview) < 0)
 	 			    	System.err.println(	"image did not load, check url: " + url);
 			    	imgIcon = new ImageIcon(image);
 			    	System.out.println(	image.getWidth(preview) + " x " + image.getHeight(preview));
 			    }

			} 
 			catch (Exception e) 
 			{
 				e.printStackTrace();
 				System.out.println(	e.getMessage());
 			}
 			JFrame win = new JFrame();
 			win.setSize(600,  400);
 			win.setTitle("My WIndow");
 			win.add(preview);
 			win.setVisible(true);
// 			preview.setBorder(BorderFactory.createDashedBorder(Color.green,3,3,3,false));
 			Thread.sleep(1000);
 			System.out.println(	imgIcon.getIconWidth() + " x " + imgIcon.getIconHeight());
 		} catch (Throwable t) {
 			System.out.println("Should not have seen exception " + t);
 		}
 	}

 	  public void setImage(final String urlPath) {
// 	    URL url = null;
 	    try {
// 	      url =s new URL(urlPath);
 	      imgIcon = new ImageIcon(new URL(urlPath));
 	     System.out.println("getImageLoadStatus: " + imgIcon.getImageLoadStatus());
 	    } catch (Exception e) {
 	 	     System.out.println("MalformedURLException: " + e.getMessage());
	      throw new IllegalArgumentException(e);
 	    }
 	    super.repaint();
 	  }
// 	  
// 	@Override   public boolean imageUpdate(Image img, int infoflags,
//               int x, int y, int width, int height)
// 	{
//		System.out.println("imageUpdate: " + img + x + ", " + y + " Size: " + width + " x " + height);
//		return true;
// 	}

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
    imgIcon = null;
    repaint();
  }

  protected void paintComponent(final Graphics g) {
    if (imgIcon == null || imgIcon.getImage() == null) return;
       final Graphics2D g2d = (Graphics2D) g;
      g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

      final int cw = super.getWidth();
      final int ch = super.getHeight();
     final double ca = (double) ch / cw;
// 	System.out.println("paintComponent: " + imgIcon.getImage() + " "  +cw + " x " + ch);

      final int iw = imgIcon.getIconWidth();
      final int ih = imgIcon.getIconHeight();
      Image image = imgIcon.getImage();
      final double ia = (double) ih / iw;
//      System.out.println("icon size: " + iw + " x " + ih);

      if (cw > iw && ch > ih) {
        final int x = (cw - iw) / 2;
        final int y = (ch - ih) / 2;
				g2d.drawImage(image, x, y, null);
			} else {
				int dw, dh, x=0, y=0;
				if (ca > ia) {
					dw = cw;
					dh = cw * ih / iw;
					y = (ch - dh) / 2;
				} else { // iw <= ih
					dw = iw * ch / ih;
					dh = ch;
					x = (cw - dw) / 2;
				}
				g.drawImage(image, x, y, dw, dh, null);
			}
      }
}