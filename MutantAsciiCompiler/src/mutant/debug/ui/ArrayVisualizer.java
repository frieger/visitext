package mutant.debug.ui;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.List;

import javax.swing.JFrame;

import mutant.ascii.representation.AscChar;
import mutant.ascii.representation.AscClass;
import mutant.ascii.representation.AscEdge;
import mutant.parser.EdgeParser;

public class ArrayVisualizer {
	
	private final static boolean ENABLE_VISUALIZER = true;
	
	private final static boolean COLOR_TEXT = true;
	private final static boolean COLOR_BACKGROUND = true;
	private final static boolean MODIFY_BACKGROUND_COLOR = true;

	private JFrame frame;
	private AscChar[][] arr;
	private EdgeParser ep;
	
	public ArrayVisualizer(AscChar[][] array, List<AscClass> classes, EdgeParser edgeParser) {
		if (!ENABLE_VISUALIZER) {
			return;
		}
		arr = array;
		ep = edgeParser;
		frame = new JFrame();
		frame.setBounds(100,100,900,700);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new GridLayout(1, 1));
		frame.getContentPane().add(new MyCanvas());
		
		frame.setVisible(true);
		
	}
	
	
	
	private class MyCanvas extends Canvas {
		
		private Color[] colors = new Color[]{Color.BLACK, new Color(150, 75, 0), Color.red, Color.ORANGE, Color.yellow, 
												Color.GREEN, Color.BLUE, new Color(127,0,255)};
		
		int xspace = 10;
		int yspace = 15;
		
		int xoff = 20;
		int yoff = 50;
		@Override
		public void paint(Graphics g) {
			// TODO Auto-generated method stub
			super.paint(g);
			
			g.setColor(new Color(192, 192, 192));
			g.fillRect(0, 0, xoff+800, yoff+600);
			Font fnt = Font.decode("Envy Code R-Bold-10");
			g.setFont(fnt);
			
			for (int y = 0; y < arr.length; y++) {
				for (int x = 0; x < arr[0].length; x++) {
					if (arr[y][x].color < 0) {
						g.setColor(Color.WHITE);
					} else if (arr[y][x].color < colors.length) {
						g.setColor(colors[arr[y][x].color]);
					} else {
						g.setColor(Color.cyan);
					}
					if (COLOR_BACKGROUND) {
						Color tmp = g.getColor();
						if (MODIFY_BACKGROUND_COLOR) {
							Color foo = new Color(tmp.darker().getRed(), tmp.darker().getGreen(), tmp.darker().getBlue(), 100);
							g.setColor(foo);
						}
						g.fillRect(xoff + x*xspace - 2, yoff + 5+y*yspace - yspace, xspace, yspace);
						g.setColor(tmp);
					}
					
					if (!COLOR_TEXT) {
						g.setColor(Color.white);
					}
					g.drawString("" + arr[y][x].c, xoff + x*xspace, yoff + y*yspace);

				}
			}
			
			
			int cnt = 0;
			for (AscEdge ae : ep.getEdges()) {
				if (ae.lineColor < 0) {
					g.setColor(Color.WHITE);
				} else if (ae.lineColor < colors.length) {
					g.setColor(colors[ae.lineColor]);
				} else {
					g.setColor(Color.CYAN);
				}
				g.fillRect(700, 50+15*cnt - 10, 10, 10);
				g.setColor(Color.BLACK);
				g.drawString(ae.startMultiplicity.trim() + " " + ae.endMultiplicity.trim() + "  {" + ae.label + "} [" + ae.signalName + "]", 710, 50+15*cnt);
				
				
				cnt++;
			}
			
			
			
	        Graphics2D g2d = (Graphics2D)g;
	        
	        int xmax = arr[0].length * xspace;
	        int ymax = arr.length * yspace;
	        
	 
	        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
	                             RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
			Font fnt2 = Font.decode("Envy Code R-Regular-8");
			g2d.setFont(fnt2);
			for (int x = 0; x < arr[0].length; x++) {
				g2d.setColor(Color.black);
				g2d.drawString("" + x, xoff + x*xspace, yoff - yspace);
				if (x % 2 == 0) {
					g2d.setColor(new Color(0,0,0,80));
					g2d.drawLine(xoff+x*xspace-2, yoff-yspace, xoff+x*xspace-2, yoff+ymax);
				}
			}
			for (int y = 0; y < arr.length; y++) {
				g2d.setColor(Color.black);
				g2d.drawString("" + y, xoff - xspace, yoff + y*yspace);
				if (y%2 == 0) {
					g2d.setColor(new Color(0,0,0,80));
					g2d.drawLine(xoff-xspace, yoff+y*yspace + 5, xoff+xmax, yoff+y*yspace + 5);
				}
			}
			g2d.drawString("20", 40, 500);
			
			/*
			g.setColor(Color.CYAN);
			g.drawString("O", 100, 100);
			g.drawRect(100, 100-10, 10, 10);
			*/
		}
	}
	
}
