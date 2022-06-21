package datacollection;

import java.awt.Graphics;
import java.awt.Rectangle;

public class ObjectLabel {
	String name;
	Rectangle boundingBox;
	
	public ObjectLabel(String name, Rectangle boundingBox) {
		this.name = name;
		this.boundingBox = boundingBox;
	}
	
	public void drawLabel(Graphics g) {
		g.drawRect(boundingBox.x, boundingBox.y, boundingBox.width, boundingBox.height);
		g.drawString(name, boundingBox.x, boundingBox.y);
	}
	
	public Rectangle BoundingBox() {
		return this.boundingBox;
	}
	public String Name() {
		return this.name;
	}
}
