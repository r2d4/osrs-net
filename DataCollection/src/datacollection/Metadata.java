package datacollection;

import java.util.List;

import org.dreambot.api.methods.input.Camera;

public class Metadata {
	List<ObjectLabel> labels;
	String imageName;
	
	public Metadata(List<ObjectLabel> labels, String imageName) {
		this.labels = labels;
		this.imageName = imageName;
	}
}
