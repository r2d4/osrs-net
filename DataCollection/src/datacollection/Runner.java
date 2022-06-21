package datacollection;

import org.dreambot.api.Client;
import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.container.impl.bank.BankLocation;
import org.dreambot.api.methods.input.Camera;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.item.GroundItems;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.walking.impl.Walking;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.wrappers.interactive.Entity;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.NPC;
import org.dreambot.api.wrappers.interactive.Player;
import org.dreambot.api.wrappers.items.GroundItem;
import org.dreambot.core.l;

import com.google.gson.*;

import javax.imageio.ImageIO;


@ScriptManifest(name = "Data Collector for ScapeNet", description = "Creates and labels in-game screenshots based on a path.", author = "Zezima",
                version = 1.0, category = Category.MISC, image = "")
public class Runner extends AbstractScript {
	Instant startTime;
	Gson gson;
	Random rand;
	
	int timesStuck;
	
	Tile[] currentPath;
	
	@Override
	public void onStart() {
		startTime = Instant.now();
		labels = new ArrayList<ObjectLabel>();
		gson = new Gson();
		rand = new Random();
		lastTile = Players.localPlayer().getTile();
		timesStuck = 0;
		
		currentPath = path;
		walkStep = 740;
		currentDest = currentPath[walkStep];
	}
	
	List<Tile> destinations;
	
	Tile currentDest;
	
	Tile lastTile;
	
	List<ObjectLabel> labels;
	
	Rectangle map = new Rectangle(Client.getViewportWidth()-180, 0, 180, 192);
	Rectangle menu = new Rectangle(0, Client.getViewportHeight()-75, Client.getViewportWidth(), 75);
	
	int size = 640;
	
	Rectangle viewport = new Rectangle(Client.getViewportWidth()/2-size/2, Client.getViewportHeight()/2-size/2, size, size);
	
	boolean isSaving = false;
	
	@Override
	public int onLoop() {
		labels = new ArrayList<ObjectLabel>();
		List<ObjectLabel> newLabels = new ArrayList<ObjectLabel>();
		sleep(60);
		
		List<GameObject> gameObjects = GameObjects.all(go -> filterEntity(go));
		List<NPC> npcs = NPCs.all(npc -> filterEntity(npc));
		List<GroundItem> groundItems = GroundItems.all(gi -> filterEntity(gi));

		addLabels(npcs, newLabels);
		addLabels(gameObjects, newLabels);
		addLabels(groundItems, newLabels);
		
		newLabels = newLabels.stream().filter(isInViewport()).collect(Collectors.<ObjectLabel>toList());
		

		isSaving = true;
		saveFrame(newLabels);
		isSaving = false;
		labels = newLabels;

		rotateCamera();
		walk();
		return 1;
	}
	
	public Predicate<ObjectLabel> isInViewport() {
		return l -> viewport.intersects(l.boundingBox);
	}
	
	@Override
	public void onPaint(Graphics g) {
		if (isSaving) return;
		
		g.setColor(Color.white);
		labels.forEach(l -> l.drawLabel(g));
		g.setColor(Color.red);
		g.drawRect(map.x, map.y, map.width, map.height);
		g.drawRect(menu.x, menu.y, menu.width, menu.height);
		
		viewport = new Rectangle(Client.getViewportWidth()/2-size/2, Client.getViewportHeight()/2-size/2, size, size);
		g.drawRect(viewport.x, viewport.y, viewport.width, viewport.height);
		
		g.setColor(Color.white);
		g.fillRect(0, 0, 300, 75);
		g.setColor(Color.black);
		g.drawString(String.format("Destination: %d, %d (step %d)", path[walkStep].getX(), path[walkStep].getY(), walkStep), 20, 20);
		g.drawString(String.format("Screenshots: %d", screenshotCount), 20, 35);
	}
	
	public void addLabels(List<? extends Entity> entities, List<ObjectLabel> labels) {
		for(Entity e: entities) {
			addLabel(e, labels);
		}
	}
	
	// For important mining objects that aren't labeled right.
	public void addLabel(Entity g, List<ObjectLabel> labels) {
		String name = g.getName();
		int id = g.getID();
		if (id == 11391 || id == 11390) name = "Mined Rock";
		if (id == 11370 || id == 11371) name = "Gold Rock";
		if (id == 11360 || id == 11361) name = "Tin Rock";
		if (id == 11363 || id == 11362) name = "Clay Rock";
		if (id == 11161 || id == 10943) name = "Copper Rock";
		if (id == 11364 || id == 11365) name = "Iron Rock";
		if (id == 11366 || id == 11367) name = "Coal Rock";
		if (id == 11372 || id == 11373) name = "Mithril Rock";
		if (id == 11374 || id == 11375) name = "Adamantite Rock";
		if (id == 11376 || id == 11377) name = "Runite Rock";
		labels.add(new ObjectLabel(name, g.getBoundingBox()));
	}
	

	
	public boolean filterEntity(Entity e) {
		 if (e == null) return false;
		if (e.getName() == "null") return false;
		if (e.distance(Players.localPlayer()) >= 25) return false;
		if (!e.isOnScreen()) return false;
		return true;
	}
	
	public void saveFrame(List<ObjectLabel> newLabels) {
		String imageName = String.format("%d", System.currentTimeMillis());
		Metadata m = new Metadata(newLabels, imageName);
		screenshot("images", imageName, newLabels);
		String json = gson.toJson(m);
		try {
			FileWriter fw = new FileWriter("image_labels.txt", true);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(json);
			bw.newLine();
			bw.close();
		} catch(Exception e) {
			log("error writing metadata");
		}
		
		
		
	}
	
	public void rotateCamera() {
		Camera.rotateTo(Calculations.random(2400), 384);
	}
	
	int walkStep = 0;
	
	public void crossWild() {
		if (GameObjects.closest("Wilderness ditch") != null) { 
			GameObjects.closest("Wilderness ditch").interact();
			sleep(2000);
		}
	}

	
	public void walk() {
		if (currentDest.getY() > 3520 && Players.localPlayer().getY() < 3520) {
			crossWild();
		}
		if (currentDest.getY() < 3520 && Players.localPlayer().getY() > 3520) {
			crossWild();
		}
		
		if(Players.localPlayer().distance(currentDest) < 3){
			currentDest = currentPath[walkStep++ % currentPath.length];
			return;
		}
		Walking.walk(currentDest);
		
		if (lastTile.distance(Players.localPlayer().getTile()) == 0) {
			timesStuck++;
		} else {
			timesStuck = 0;
		}
		if (timesStuck > 5) {
			log("stuck: picking new destination");
			currentDest = currentPath[walkStep++ % currentPath.length];
			return;
		}
		lastTile = Players.localPlayer().getTile();
	}
	
	public boolean intersectsOthers(ObjectLabel l, List<ObjectLabel> labels) {
		for (ObjectLabel o : labels) {
			if (o == l) continue;
			if (o.BoundingBox().intersects(l.BoundingBox())) return true;
		}
		return false;
	}
	

	int screenshotCount = 0;
	
	public static BufferedImage copyImage(BufferedImage source){
	    BufferedImage b = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
	    Graphics g = b.getGraphics();
	    g.drawImage(source, 0, 0, null);
	    g.dispose();
	    return b;
	}
	
	public void saveImage(BufferedImage image, String folder, String filename) {
		File file = new File(folder);
		image = image.getSubimage(viewport.x, viewport.y, viewport.width, viewport.height);
		try {
			if (!file.exists() || !file.isDirectory()) {
				log("Creating script folder");
				file.mkdir();
			}
			log("Saving screenshot...");
			ImageIO.write(image, "png",
					new File(String.format("%s/%s.png", folder, filename)));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void screenshot(String folder, String filename, List<ObjectLabel> newLabels) {
		BufferedImage image = Client.getCanvasImage();
		saveImage(image, folder, filename);

		if (screenshotCount % 10 == 0) {
			log("Saving debug screenshot...");
			folder = String.format("%s_debug", folder);
			Graphics2D withLabels = image.createGraphics();
			withLabels.setColor(Color.green);
			newLabels.forEach(l -> l.drawLabel(withLabels));
			withLabels.dispose();
			saveImage(image, folder, filename);
		}
		
		screenshotCount++;
	}
	
	// Random path to walk around. Should take a few hours a then loops.
	// Generated with https://explv.github.io/
	Tile[] path = {
		    new Tile(3225, 3218, 0),
		    new Tile(3218, 3218, 0),
		    new Tile(3218, 3210, 0),
		    new Tile(3214, 3207, 0),
		    new Tile(3202, 3206, 0),
		    new Tile(3202, 3230, 0),
		    new Tile(3216, 3229, 0),
		    new Tile(3225, 3218, 0),
		    new Tile(3235, 3220, 0),
		    new Tile(3240, 3217, 0),
		    new Tile(3248, 3213, 0),
		    new Tile(3256, 3202, 0),
		    new Tile(3242, 3217, 0),
		    new Tile(3236, 3210, 0),
		    new Tile(3235, 3200, 0),
		    new Tile(3225, 3200, 0),
		    new Tile(3242, 3200, 0),
		    new Tile(3244, 3187, 0),
		    new Tile(3243, 3153, 0),
		    new Tile(3229, 3142, 0),
		    new Tile(3150, 3146, 0),
		    new Tile(3145, 3155, 0),
		    new Tile(3143, 3178, 0),
		    new Tile(3166, 3169, 0),
		    new Tile(3203, 3165, 0),
		    new Tile(3232, 3171, 0),
		    new Tile(3227, 3186, 0),
		    new Tile(3200, 3187, 0),
		    new Tile(3173, 3190, 0),
		    new Tile(3148, 3192, 0),
		    new Tile(3147, 3204, 0),
		    new Tile(3135, 3211, 0),
		    new Tile(3114, 3209, 0),
		    new Tile(3114, 3165, 0),
		    new Tile(3117, 3156, 0),
		    new Tile(3106, 3150, 0),
		    new Tile(3099, 3162, 0),
		    new Tile(3104, 3168, 0),
		    new Tile(3110, 3168, 0),
		    new Tile(3112, 3211, 0),
		    new Tile(3088, 3226, 0),
		    new Tile(3070, 3247, 0),
		    new Tile(3073, 3262, 0),
		    new Tile(3080, 3262, 0),
		    new Tile(3080, 3254, 0),
		    new Tile(3087, 3248, 0),
		    new Tile(3093, 3247, 0),
		    new Tile(3093, 3243, 0),
		    new Tile(3096, 3248, 0),
		    new Tile(3111, 3239, 0),
		    new Tile(3127, 3237, 0),
		    new Tile(3133, 3242, 0),
		    new Tile(3128, 3248, 0),
		    new Tile(3119, 3248, 0),
		    new Tile(3112, 3244, 0),
		    new Tile(3103, 3258, 0),
		    new Tile(3103, 3271, 0),
		    new Tile(3091, 3271, 0),
		    new Tile(3083, 3265, 0),
		    new Tile(3078, 3273, 0),
		    new Tile(3094, 3282, 0),
		    new Tile(3103, 3283, 0),
		    new Tile(3106, 3280, 0),
		    new Tile(3109, 3283, 0),
		    new Tile(3111, 3289, 0),
		    new Tile(3109, 3294, 0),
		    new Tile(3109, 3351, 0),
		    new Tile(3127, 3352, 0),
		    new Tile(3127, 3362, 0),
		    new Tile(3121, 3362, 0),
		    new Tile(3121, 3374, 0),
		    new Tile(3094, 3374, 0),
		    new Tile(3089, 3354, 0),
		    new Tile(3098, 3350, 0),
		    new Tile(3071, 3277, 0),
		    new Tile(3034, 3276, 0),
		    new Tile(3019, 3261, 0),
		    new Tile(3019, 3256, 0),
		    new Tile(3042, 3256, 0),
		    new Tile(3048, 3253, 0),
		    new Tile(3053, 3247, 0),
		    new Tile(3042, 3247, 0),
		    new Tile(3041, 3236, 0),
		    new Tile(3050, 3235, 0),
		    new Tile(3027, 3234, 0),
		    new Tile(3027, 3203, 0),
		    new Tile(3049, 3203, 0),
		    new Tile(3049, 3193, 0),
		    new Tile(3022, 3179, 0),
		    new Tile(3019, 3147, 0),
		    new Tile(3002, 3132, 0),
		    new Tile(3000, 3110, 0),
		    new Tile(2984, 3109, 0),
		    new Tile(2987, 3122, 0),
		    new Tile(2996, 3143, 0),
		    new Tile(2998, 3158, 0),
		    new Tile(2988, 3175, 0),
		    new Tile(2993, 3182, 0),
		    new Tile(2999, 3182, 0),
		    new Tile(3005, 3191, 0),
		    new Tile(2989, 3201, 0),
		    new Tile(2967, 3195, 0),
		    new Tile(2966, 3200, 0),
		    new Tile(2956, 3199, 0),
		    new Tile(2945, 3200, 0),
		    new Tile(2944, 3208, 0),
		    new Tile(2945, 3219, 0),
		    new Tile(2952, 3219, 0),
		    new Tile(2953, 3208, 0),
		    new Tile(2960, 3207, 0),
		    new Tile(2961, 3218, 0),
		    new Tile(2972, 3218, 0),
		    new Tile(2972, 3202, 0),
		    new Tile(2985, 3214, 0),
		    new Tile(2983, 3220, 0),
		    new Tile(2970, 3227, 0),
		    new Tile(2974, 3234, 0),
		    new Tile(2983, 3235, 0),
		    new Tile(2985, 3243, 0),
		    new Tile(2982, 3248, 0),
		    new Tile(2975, 3248, 0),
		    new Tile(2968, 3243, 0),
		    new Tile(2971, 3236, 0),
		    new Tile(2977, 3234, 0),
		    new Tile(2984, 3235, 0),
		    new Tile(2985, 3243, 0),
		    new Tile(2977, 3250, 0),
		    new Tile(2974, 3253, 0),
		    new Tile(2958, 3247, 0),
		    new Tile(2950, 3243, 0),
		    new Tile(2942, 3243, 0),
		    new Tile(2941, 3258, 0),
		    new Tile(2921, 3258, 0),
		    new Tile(2921, 3239, 0),
		    new Tile(2924, 3225, 0),
		    new Tile(2908, 3225, 0),
		    new Tile(2923, 3208, 0),
		    new Tile(2932, 3217, 0),
		    new Tile(2939, 3211, 0),
		    new Tile(2940, 3219, 0),
		    new Tile(2932, 3228, 0),
		    new Tile(2936, 3233, 0),
		    new Tile(2947, 3224, 0),
		    new Tile(2955, 3228, 0),
		    new Tile(2953, 3237, 0),
		    new Tile(2970, 3262, 0),
		    new Tile(2951, 3269, 0),
		    new Tile(2929, 3265, 0),
		    new Tile(2915, 3273, 0),
		    new Tile(2906, 3295, 0),
		    new Tile(2941, 3273, 0),
		    new Tile(2945, 3279, 0),
		    new Tile(2945, 3290, 0),
		    new Tile(2939, 3293, 0),
		    new Tile(2933, 3290, 0),
		    new Tile(2923, 3293, 0),
		    new Tile(2915, 3295, 0),
		    new Tile(2912, 3301, 0),
		    new Tile(2909, 3318, 0),
		    new Tile(2912, 3320, 0),
		    new Tile(2912, 3325, 0),
		    new Tile(2922, 3325, 0),
		    new Tile(2922, 3319, 0),
		    new Tile(2917, 3317, 0),
		    new Tile(2933, 3318, 0),
		    new Tile(2939, 3304, 0),
		    new Tile(2966, 3288, 0),
		    new Tile(2993, 3286, 0),
		    new Tile(2999, 3280, 0),
		    new Tile(3004, 3284, 0),
		    new Tile(3000, 3290, 0),
		    new Tile(2993, 3294, 0),
		    new Tile(2995, 3298, 0),
		    new Tile(3001, 3301, 0),
		    new Tile(2999, 3310, 0),
		    new Tile(3015, 3316, 0),
		    new Tile(3019, 3314, 0),
		    new Tile(3024, 3321, 0),
		    new Tile(3034, 3319, 0),
		    new Tile(3042, 3316, 0),
		    new Tile(3048, 3315, 0),
		    new Tile(3046, 3302, 0),
		    new Tile(3053, 3301, 0),
		    new Tile(3062, 3300, 0),
		    new Tile(3066, 3302, 0),
		    new Tile(3066, 3316, 0),
		    new Tile(3064, 3325, 0),
		    new Tile(3007, 3320, 0),
		    new Tile(3002, 3325, 0),
		    new Tile(2986, 3315, 0),
		    new Tile(2983, 3318, 0),
		    new Tile(2976, 3317, 0),
		    new Tile(2973, 3315, 0),
		    new Tile(2967, 3316, 0),
		    new Tile(2966, 3312, 0),
		    new Tile(2943, 3313, 0),
		    new Tile(2942, 3332, 0),
		    new Tile(2942, 3338, 0),
		    new Tile(2945, 3338, 0),
		    new Tile(2945, 3357, 0),
		    new Tile(2940, 3370, 0),
		    new Tile(2943, 3374, 0),
		    new Tile(2946, 3374, 0),
		    new Tile(2944, 3369, 0),
		    new Tile(2940, 3377, 0),
		    new Tile(2950, 3380, 0),
		    new Tile(2953, 3384, 0),
		    new Tile(2959, 3383, 0),
		    new Tile(2962, 3388, 0),
		    new Tile(2956, 3370, 0),
		    new Tile(2959, 3371, 0),
		    new Tile(2956, 3374, 0),
		    new Tile(2963, 3345, 0),
		    new Tile(2968, 3339, 0),
		    new Tile(2976, 3338, 0),
		    new Tile(2980, 3339, 0),
		    new Tile(2974, 3346, 0),
		    new Tile(2965, 3350, 0),
		    new Tile(2968, 3367, 0),
		    new Tile(2968, 3377, 0),
		    new Tile(2979, 3372, 0),
		    new Tile(2978, 3366, 0),
		    new Tile(2985, 3365, 0),
		    new Tile(2987, 3371, 0),
		    new Tile(2995, 3366, 0),
		    new Tile(2995, 3360, 0),
		    new Tile(2994, 3377, 0),
		    new Tile(2984, 3382, 0),
		    new Tile(2983, 3388, 0),
		    new Tile(2990, 3389, 0),
		    new Tile(3002, 3387, 0),
		    new Tile(3009, 3382, 0),
		    new Tile(3016, 3386, 0),
		    new Tile(3023, 3384, 0),
		    new Tile(3023, 3374, 0),
		    new Tile(3014, 3369, 0),
		    new Tile(3006, 3361, 0),
		    new Tile(3028, 3372, 0),
		    new Tile(3033, 3376, 0),
		    new Tile(3034, 3384, 0),
		    new Tile(3036, 3387, 0),
		    new Tile(3057, 3386, 0),
		    new Tile(3057, 3373, 0),
		    new Tile(3063, 3373, 0),
		    new Tile(3058, 3363, 0),
		    new Tile(3057, 3348, 0),
		    new Tile(3057, 3338, 0),
		    new Tile(3056, 3332, 0),
		    new Tile(3035, 3333, 0),
		    new Tile(3029, 3336, 0),
		    new Tile(3018, 3336, 0),
		    new Tile(3015, 3339, 0),
		    new Tile(3022, 3340, 0),
		    new Tile(3031, 3338, 0),
		    new Tile(3038, 3341, 0),
		    new Tile(3043, 3343, 0),
		    new Tile(3043, 3347, 0),
		    new Tile(3047, 3350, 0),
		    new Tile(3053, 3352, 0),
		    new Tile(3052, 3359, 0),
		    new Tile(3045, 3359, 0),
		    new Tile(3043, 3353, 0),
		    new Tile(3035, 3350, 0),
		    new Tile(3028, 3351, 0),
		    new Tile(3022, 3351, 0),
		    new Tile(3006, 3358, 0),
		    new Tile(3012, 3356, 0),
		    new Tile(3031, 3359, 0),
		    new Tile(3033, 3366, 0),
		    new Tile(3042, 3369, 0),
		    new Tile(3043, 3364, 0),
		    new Tile(2987, 3372, 0),
		    new Tile(2965, 3394, 0),
		    new Tile(2947, 3442, 0),
		    new Tile(2948, 3451, 0),
		    new Tile(2949, 3455, 0),
		    new Tile(2952, 3466, 0),
		    new Tile(2950, 3470, 0),
		    new Tile(2950, 3476, 0),
		    new Tile(2954, 3478, 0),
		    new Tile(2956, 3483, 0),
		    new Tile(2955, 3488, 0),
		    new Tile(2956, 3493, 0),
		    new Tile(2954, 3497, 0),
		    new Tile(2953, 3502, 0),
		    new Tile(2955, 3507, 0),
		    new Tile(2961, 3508, 0),
		    new Tile(2958, 3498, 0),
		    new Tile(2951, 3435, 0),
		    new Tile(2986, 3421, 0),
		    new Tile(2988, 3430, 0),
		    new Tile(2966, 3443, 0),
		    new Tile(2961, 3461, 0),
		    new Tile(2968, 3489, 0),
		    new Tile(2977, 3505, 0),
		    new Tile(2976, 3514, 0),
		    new Tile(2985, 3514, 0),
		    new Tile(2986, 3491, 0),
		    new Tile(2980, 3485, 0),
		    new Tile(2977, 3450, 0),
		    new Tile(2984, 3442, 0),
		    new Tile(3018, 3435, 0),
		    new Tile(3035, 3456, 0),
		    new Tile(3027, 3459, 0),
		    new Tile(3015, 3455, 0),
		    new Tile(3005, 3454, 0),
		    new Tile(3005, 3447, 0),
		    new Tile(3016, 3449, 0),
		    new Tile(3015, 3456, 0),
		    new Tile(2983, 3469, 0),
		    new Tile(2992, 3482, 0),
		    new Tile(2993, 3502, 0),
		    new Tile(3006, 3515, 0),
		    new Tile(3008, 3520, 0),
		    new Tile(3021, 3520, 0),
		    new Tile(3032, 3519, 0),
		    new Tile(3036, 3514, 0),
		    new Tile(3021, 3483, 0),
		    new Tile(3033, 3471, 0),
		    new Tile(3043, 3470, 0),
		    new Tile(3051, 3469, 0),
		    new Tile(3051, 3482, 0),
		    new Tile(3048, 3494, 0),
		    new Tile(3047, 3500, 0),
		    new Tile(3056, 3499, 0),
		    new Tile(3056, 3506, 0),
		    new Tile(3049, 3506, 0),
		    new Tile(3049, 3501, 0),
		    new Tile(3054, 3495, 0),
		    new Tile(3053, 3488, 0),
		    new Tile(3053, 3462, 0),
		    new Tile(3052, 3422, 0),
		    new Tile(3052, 3412, 0),
		    new Tile(3070, 3419, 0),
		    new Tile(3073, 3417, 0),
		    new Tile(3074, 3425, 0),
		    new Tile(3079, 3425, 0),
		    new Tile(3079, 3433, 0),
		    new Tile(3074, 3433, 0),
		    new Tile(3074, 3446, 0),
		    new Tile(3084, 3446, 0),
		    new Tile(3083, 3434, 0),
		    new Tile(3086, 3431, 0),
		    new Tile(3081, 3423, 0),
		    new Tile(3076, 3419, 0),
		    new Tile(3078, 3415, 0),
		    new Tile(3082, 3413, 0),
		    new Tile(3087, 3413, 0),
		    new Tile(3089, 3409, 0),
		    new Tile(3088, 3405, 0),
		    new Tile(3081, 3406, 0),
		    new Tile(3080, 3409, 0),
		    new Tile(3086, 3417, 0),
		    new Tile(3085, 3423, 0),
		    new Tile(3088, 3427, 0),
		    new Tile(3093, 3428, 0),
		    new Tile(3099, 3424, 0),
		    new Tile(3102, 3425, 0),
		    new Tile(3103, 3429, 0),
		    new Tile(3107, 3432, 0),
		    new Tile(3104, 3436, 0),
		    new Tile(3095, 3438, 0),
		    new Tile(3091, 3434, 0),
		    new Tile(3088, 3439, 0),
		    new Tile(3089, 3446, 0),
		    new Tile(3096, 3450, 0),
		    new Tile(3096, 3457, 0),
		    new Tile(3086, 3459, 0),
		    new Tile(3075, 3461, 0),
		    new Tile(3075, 3469, 0),
		    new Tile(3074, 3477, 0),
		    new Tile(3073, 3487, 0),
		    new Tile(3074, 3499, 0),
		    new Tile(3074, 3506, 0),
		    new Tile(3074, 3512, 0),
		    new Tile(3076, 3515, 0),
		    new Tile(3073, 3517, 0),
		    new Tile(3073, 3519, 0),
		    new Tile(3085, 3513, 0),
		    new Tile(3085, 3506, 0),
		    new Tile(3081, 3505, 0),
		    new Tile(3077, 3501, 0),
		    new Tile(3084, 3497, 0),
		    new Tile(3084, 3484, 0),
		    new Tile(3079, 3483, 0),
		    new Tile(3091, 3483, 0),
		    new Tile(3085, 3472, 0),
		    new Tile(3091, 3470, 0),
		    new Tile(3095, 3470, 0),
		    new Tile(3094, 3480, 0),
		    new Tile(3096, 3487, 0),
		    new Tile(3092, 3490, 0),
		    new Tile(3094, 3491, 0),
		    new Tile(3093, 3494, 0),
		    new Tile(3097, 3496, 0),
		    new Tile(3094, 3502, 0),
		    new Tile(3089, 3506, 0),
		    new Tile(3089, 3515, 0),
		    new Tile(3103, 3514, 0),
		    new Tile(3101, 3506, 0),
		    new Tile(3103, 3497, 0),
		    new Tile(3112, 3494, 0),
		    new Tile(3111, 3502, 0),
		    new Tile(3105, 3503, 0),
		    new Tile(3103, 3495, 0),
		    new Tile(3106, 3491, 0),
		    new Tile(3111, 3494, 0),
		    new Tile(3112, 3497, 0),
		    new Tile(3112, 3503, 0),
		    new Tile(3114, 3495, 0),
		    new Tile(3110, 3507, 0),
		    new Tile(3106, 3509, 0),
		    new Tile(3105, 3517, 0),
		    new Tile(3111, 3519, 0),
		    new Tile(3116, 3511, 0),
		    new Tile(3131, 3516, 0),
		    new Tile(3134, 3505, 0),
		    new Tile(3124, 3489, 0),
		    new Tile(3116, 3488, 0),
		    new Tile(3115, 3478, 0),
		    new Tile(3124, 3477, 0),
		    new Tile(3130, 3478, 0),
		    new Tile(3134, 3469, 0),
		    new Tile(3117, 3454, 0),
		    new Tile(3110, 3453, 0),
		    new Tile(3111, 3447, 0),
		    new Tile(3120, 3447, 0),
		    new Tile(3138, 3455, 0),
		    new Tile(3147, 3455, 0),
		    new Tile(3150, 3451, 0),
		    new Tile(3150, 3447, 0),
		    new Tile(3142, 3441, 0),
		    new Tile(3138, 3444, 0),
		    new Tile(3136, 3439, 0),
		    new Tile(3123, 3433, 0),
		    new Tile(3122, 3426, 0),
		    new Tile(3115, 3423, 0),
		    new Tile(3113, 3415, 0),
		    new Tile(3117, 3410, 0),
		    new Tile(3128, 3407, 0),
		    new Tile(3137, 3412, 0),
		    new Tile(3136, 3419, 0),
		    new Tile(3147, 3421, 0),
		    new Tile(3150, 3414, 0),
		    new Tile(3159, 3410, 0),
		    new Tile(3157, 3403, 0),
		    new Tile(3152, 3400, 0),
		    new Tile(3144, 3417, 0),
		    new Tile(3154, 3424, 0),
		    new Tile(3166, 3426, 0),
		    new Tile(3166, 3441, 0),
		    new Tile(3162, 3443, 0),
		    new Tile(3171, 3446, 0),
		    new Tile(3174, 3441, 0),
		    new Tile(3178, 3450, 0),
		    new Tile(3183, 3449, 0),
		    new Tile(3182, 3436, 0),
		    new Tile(3182, 3430, 0),
		    new Tile(3187, 3430, 0),
		    new Tile(3182, 3445, 0),
		    new Tile(3185, 3450, 0),
		    new Tile(3175, 3457, 0),
		    new Tile(3165, 3463, 0),
		    new Tile(3165, 3474, 0),
		    new Tile(3165, 3485, 0),
		    new Tile(3161, 3487, 0),
		    new Tile(3161, 3493, 0),
		    new Tile(3165, 3493, 0),
		    new Tile(3168, 3492, 0),
		    new Tile(3169, 3488, 0),
		    new Tile(3166, 3485, 0),
		    new Tile(3177, 3490, 0),
		    new Tile(3183, 3497, 0),
		    new Tile(3179, 3506, 0),
		    new Tile(3182, 3511, 0),
		    new Tile(3186, 3512, 0),
		    new Tile(3188, 3509, 0),
		    new Tile(3182, 3505, 0),
		    new Tile(3149, 3503, 0),
		    new Tile(3143, 3491, 0),
		    new Tile(3152, 3477, 0),
		    new Tile(3170, 3475, 0),
		    new Tile(3176, 3478, 0),
		    new Tile(3175, 3466, 0),
		    new Tile(3173, 3457, 0),
		    new Tile(3185, 3451, 0),
		    new Tile(3191, 3447, 0),
		    new Tile(3196, 3427, 0),
		    new Tile(3182, 3428, 0),
		    new Tile(3184, 3420, 0),
		    new Tile(3198, 3419, 0),
		    new Tile(3192, 3425, 0),
		    new Tile(3198, 3425, 0),
		    new Tile(3199, 3413, 0),
		    new Tile(3198, 3408, 0),
		    new Tile(3191, 3408, 0),
		    new Tile(3190, 3415, 0),
		    new Tile(3181, 3415, 0),
		    new Tile(3183, 3403, 0),
		    new Tile(3184, 3399, 0),
		    new Tile(3191, 3400, 0),
		    new Tile(3200, 3400, 0),
		    new Tile(3200, 3406, 0),
		    new Tile(3207, 3405, 0),
		    new Tile(3210, 3395, 0),
		    new Tile(3202, 3392, 0),
		    new Tile(3198, 3383, 0),
		    new Tile(3210, 3382, 0),
		    new Tile(3214, 3389, 0),
		    new Tile(3223, 3390, 0),
		    new Tile(3228, 3391, 0),
		    new Tile(3235, 3392, 0),
		    new Tile(3231, 3387, 0),
		    new Tile(3237, 3387, 0),
		    new Tile(3243, 3386, 0),
		    new Tile(3247, 3386, 0),
		    new Tile(3254, 3386, 0),
		    new Tile(3254, 3389, 0),
		    new Tile(3253, 3396, 0),
		    new Tile(3253, 3398, 0),
		    new Tile(3256, 3400, 0),
		    new Tile(3256, 3404, 0),
		    new Tile(3253, 3405, 0),
		    new Tile(3251, 3411, 0),
		    new Tile(3245, 3411, 0),
		    new Tile(3245, 3405, 0),
		    new Tile(3245, 3401, 0),
		    new Tile(3249, 3400, 0),
		    new Tile(3238, 3399, 0),
		    new Tile(3235, 3403, 0),
		    new Tile(3234, 3408, 0),
		    new Tile(3233, 3413, 0),
		    new Tile(3235, 3418, 0),
		    new Tile(3240, 3417, 0),
		    new Tile(3244, 3414, 0),
		    new Tile(3261, 3412, 0),
		    new Tile(3267, 3412, 0),
		    new Tile(3270, 3414, 0),
		    new Tile(3270, 3419, 0),
		    new Tile(3266, 3422, 0),
		    new Tile(3265, 3428, 0),
		    new Tile(3255, 3429, 0),
		    new Tile(3250, 3429, 0),
		    new Tile(3247, 3431, 0),
		    new Tile(3245, 3436, 0),
		    new Tile(3246, 3444, 0),
		    new Tile(3250, 3447, 0),
		    new Tile(3251, 3451, 0),
		    new Tile(3244, 3450, 0),
		    new Tile(3244, 3455, 0),
		    new Tile(3241, 3455, 0),
		    new Tile(3237, 3468, 0),
		    new Tile(3237, 3479, 0),
		    new Tile(3243, 3479, 0),
		    new Tile(3242, 3493, 0),
		    new Tile(3246, 3496, 0),
		    new Tile(3250, 3485, 0),
		    new Tile(3246, 3479, 0),
		    new Tile(3252, 3468, 0),
		    new Tile(3246, 3453, 0),
		    new Tile(3264, 3457, 0),
		    new Tile(3268, 3437, 0),
		    new Tile(3272, 3428, 0),
		    new Tile(3282, 3430, 0),
		    new Tile(3286, 3443, 0),
		    new Tile(3283, 3458, 0),
		    new Tile(3268, 3472, 0),
		    new Tile(3270, 3490, 0),
		    new Tile(3272, 3493, 0),
		    new Tile(3272, 3503, 0),
		    new Tile(3274, 3511, 0),
		    new Tile(3271, 3518, 0),
		    new Tile(3285, 3515, 0),
		    new Tile(3290, 3508, 0),
		    new Tile(3294, 3496, 0),
		    new Tile(3298, 3491, 0),
		    new Tile(3305, 3489, 0),
		    new Tile(3312, 3490, 0),
		    new Tile(3326, 3501, 0),
		    new Tile(3324, 3490, 0),
		    new Tile(3304, 3486, 0),
		    new Tile(3298, 3484, 0),
		    new Tile(3280, 3480, 0),
		    new Tile(3291, 3470, 0),
		    new Tile(3301, 3469, 0),
		    new Tile(3306, 3476, 0),
		    new Tile(3306, 3482, 0),
		    new Tile(3310, 3471, 0),
		    new Tile(3316, 3467, 0),
		    new Tile(3309, 3458, 0),
		    new Tile(3296, 3456, 0),
		    new Tile(3290, 3455, 0),
		    new Tile(3292, 3448, 0),
		    new Tile(3294, 3427, 0),
		    new Tile(3294, 3397, 0),
		    new Tile(3294, 3383, 0),
		    new Tile(3293, 3376, 0),
		    new Tile(3288, 3371, 0),
		    new Tile(3279, 3370, 0),
		    new Tile(3275, 3360, 0),
		    new Tile(3283, 3356, 0),
		    new Tile(3290, 3355, 0),
		    new Tile(3297, 3357, 0),
		    new Tile(3309, 3331, 0),
		    new Tile(3287, 3331, 0),
		    new Tile(3282, 3327, 0),
		    new Tile(3292, 3313, 0),
		    new Tile(3287, 3298, 0),
		    new Tile(3288, 3287, 0),
		    new Tile(3287, 3276, 0),
		    new Tile(3294, 3272, 0),
		    new Tile(3301, 3275, 0),
		    new Tile(3299, 3283, 0),
		    new Tile(3298, 3291, 0),
		    new Tile(3295, 3298, 0),
		    new Tile(3301, 3307, 0),
		    new Tile(3300, 3314, 0),
		    new Tile(3302, 3299, 0),
		    new Tile(3301, 3280, 0),
		    new Tile(3304, 3273, 0),
		    new Tile(3293, 3260, 0),
		    new Tile(3281, 3242, 0),
		    new Tile(3277, 3230, 0),
		    new Tile(3279, 3223, 0),
		    new Tile(3279, 3214, 0),
		    new Tile(3284, 3214, 0),
		    new Tile(3289, 3213, 0),
		    new Tile(3292, 3207, 0),
		    new Tile(3298, 3208, 0),
		    new Tile(3303, 3208, 0),
		    new Tile(3315, 3207, 0),
		    new Tile(3318, 3200, 0),
		    new Tile(3315, 3196, 0),
		    new Tile(3316, 3190, 0),
		    new Tile(3325, 3189, 0),
		    new Tile(3326, 3194, 0),
		    new Tile(3319, 3186, 0),
		    new Tile(3320, 3177, 0),
		    new Tile(3320, 3172, 0),
		    new Tile(3312, 3173, 0),
		    new Tile(3310, 3185, 0),
		    new Tile(3305, 3184, 0),
		    new Tile(3301, 3183, 0),
		    new Tile(3295, 3190, 0),
		    new Tile(3288, 3194, 0),
		    new Tile(3283, 3193, 0),
		    new Tile(3276, 3195, 0),
		    new Tile(3269, 3195, 0),
		    new Tile(3271, 3184, 0),
		    new Tile(3269, 3178, 0),
		    new Tile(3275, 3177, 0),
		    new Tile(3278, 3180, 0),
		    new Tile(3274, 3160, 0),
		    new Tile(3269, 3168, 0),
		    new Tile(3267, 3174, 0),
		    new Tile(3264, 3161, 0),
		    new Tile(3268, 3158, 0),
		    new Tile(3275, 3153, 0),
		    new Tile(3268, 3148, 0),
		    new Tile(3273, 3143, 0),
		    new Tile(3279, 3141, 0),
		    new Tile(3284, 3149, 0),
		    new Tile(3292, 3152, 0),
		    new Tile(3301, 3156, 0),
		    new Tile(3307, 3158, 0),
		    new Tile(3305, 3178, 0),
		    new Tile(3298, 3179, 0),
		    new Tile(3297, 3168, 0),
		    new Tile(3287, 3167, 0),
		    new Tile(3287, 3180, 0),
		    new Tile(3287, 3180, 0),
		    new Tile(3281, 3180, 0),
		    new Tile(3291, 3183, 0),
		    new Tile(3295, 3200, 0),
		    new Tile(3288, 3200, 0),
		    new Tile(3287, 3206, 0),
		    new Tile(3271, 3235, 0),
		    new Tile(3309, 3235, 0),
		    new Tile(3310, 3248, 0),
		    new Tile(3276, 3332, 0),
		    new Tile(3239, 3304, 0),
		    new Tile(3238, 3294, 0),
		    new Tile(3238, 3284, 0),
		    new Tile(3242, 3278, 0),
		    new Tile(3247, 3275, 0),
		    new Tile(3250, 3272, 0),
		    new Tile(3251, 3269, 0),
		    new Tile(3251, 3262, 0),
		    new Tile(3251, 3253, 0),
		    new Tile(3249, 3271, 0),
		    new Tile(3238, 3285, 0),
		    new Tile(3238, 3299, 0),
		    new Tile(3234, 3303, 0),
		    new Tile(3238, 3290, 0),
		    new Tile(3239, 3280, 0),
		    new Tile(3251, 3252, 0),
		    new Tile(3239, 3252, 0),
		    new Tile(3241, 3241, 0),
		    new Tile(3247, 3241, 0),
		    new Tile(3255, 3241, 0),
		    new Tile(3257, 3235, 0),
		    new Tile(3249, 3233, 0),
		    new Tile(3249, 3229, 0),
		    new Tile(3260, 3231, 0),
		    new Tile(3262, 3219, 0),
		    new Tile(3257, 3220, 0),
		    new Tile(3235, 3225, 0),
		    new Tile(3233, 3234, 0),
		    new Tile(3228, 3234, 0),
		    new Tile(3224, 3238, 0),
		    new Tile(3224, 3244, 0),
		    new Tile(3231, 3245, 0),
		    new Tile(3232, 3249, 0),
		    new Tile(3227, 3248, 0),
		    new Tile(3218, 3249, 0),
		    new Tile(3218, 3255, 0),
		    new Tile(3219, 3261, 0),
		    new Tile(3225, 3260, 0),
		    new Tile(3228, 3261, 0),
		    new Tile(3225, 3265, 0),
		    new Tile(3214, 3256, 0),
		    new Tile(3214, 3246, 0),
		    new Tile(3209, 3240, 0),
		    new Tile(3205, 3242, 0),
		    new Tile(3198, 3218, 0),
		    new Tile(3197, 3209, 0),
		    new Tile(3183, 3210, 0),
		    new Tile(3161, 3210, 0),
		    new Tile(3147, 3221, 0),
		    new Tile(3148, 3229, 0),
		    new Tile(3153, 3230, 0),
		    new Tile(3163, 3222, 0),
		    new Tile(3168, 3219, 0),
		    new Tile(3185, 3220, 0),
		    new Tile(3186, 3227, 0),
		    new Tile(3193, 3234, 0),
		    new Tile(3195, 3244, 0),
		    new Tile(3188, 3247, 0),
		    new Tile(3180, 3241, 0),
		    new Tile(3165, 3238, 0),
		    new Tile(3156, 3243, 0),
		    new Tile(3165, 3252, 0),
		    new Tile(3174, 3257, 0),
		    new Tile(3180, 3267, 0),
		    new Tile(3180, 3276, 0),
		    new Tile(3173, 3276, 0),
		    new Tile(3167, 3288, 0),
		    new Tile(3167, 3300, 0),
		    new Tile(3172, 3304, 0),
		    new Tile(3172, 3311, 0),
		    new Tile(3165, 3312, 0),
		    new Tile(3161, 3308, 0),
		    new Tile(3162, 3302, 0),
		    new Tile(3166, 3285, 0),
		    new Tile(3161, 3277, 0),
		    new Tile(3148, 3254, 0),
		    new Tile(3134, 3263, 0),
		    new Tile(3132, 3271, 0),
		    new Tile(3132, 3289, 0),
		    new Tile(3134, 3302, 0)
		};
	
}


