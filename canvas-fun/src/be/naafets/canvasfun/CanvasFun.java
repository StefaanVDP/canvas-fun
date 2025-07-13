package be.naafets.canvasfun;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.Canvas;
import com.google.appinventor.components.runtime.Component;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.Form;
import com.google.appinventor.components.runtime.util.YailList;
//import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import be.naafets.canvasfun.helpers.Acceleration;
import be.naafets.canvasfun.helpers.Directions;
import be.naafets.canvasfun.helpers.Shapes;
import gnu.math.IntNum;
import gnu.math.DFloNum;

@DesignerComponent(
		version = 54,
		versionName = "1.0",
		description = "Developed by Stefaan Vandeputte using Fast.",
		iconName = "bubbles.png"
		)

/** 
 * An extension to show simple graphics moving on a canvas.
 * The core logic is based on code from a demo java applet dating from the early days of java.
 * I changed the core java graphic and draw functionality to the android way.
 * I also added more functionality because the original only drew ascending circles ( bubbles ).
 */
public class CanvasFun extends AndroidNonvisibleComponent 
implements Runnable, Component {

	public CanvasFun(ComponentContainer container) {
		super(container.$form());
//		activity = container.$context();
		form =  container.$form();
	}
//    private final Activity activity;
	private static int colors[] = { Color.BLUE, Color.CYAN, Color.GREEN,
			Color.MAGENTA, Color.RED, Color.YELLOW, Color.LTGRAY, Color.WHITE, Color.BLACK };
	private Object[] customColors;
	private boolean customColorsSet = false;
	private Thread thread;
	private Shape2D[] shapeObjects;  	// location and size of the shapes
	private Random r = new Random();
	private float maxSize;				//  used to limit the maxShapeSize in function of the canvas width
	private float maxSizes[];			//  real max size of every individual shape
	private double esize[];				//  current size of every individual shape
	private float maxShapeSize;			//  theoretical max size of all shapes
	private int sleep = 200;			//  the sleep time of the thread
	private int numberOfShapes = 10;	//  the number of shapes
	private Canvas canvas;
	private int canvasWidth = 200;		//  the canvas width
	private int canvasHeight = 200;		//  the canvas height
	private int moveX = 0;				//  number of pixels to move shape on the X-axis
	private int moveY = -3;				//  number of pixels to move shape on the y-axis
	private String text = "";			//  the text to display ( if shape = TEXT )
	private String stringList[];		//  the texts to display ( if shape = TEXTS )
	private String asset;				//  the name of the asset
	private Bitmap bitmap;				// the underlying bitmap of the asset
	private float xExtend,yExtend,startAngle,sweepAngle; // used in drawArcs
	private boolean useCenter;			// used in drawArcs
	private YailList shapeInstructions ;	// a list with shapes and their attributes
	private Instruction[] instructions;		// above list translated to objects
	private ComplexShape2D[] complexShapes; // above objects translated to shapes and attributes
	private boolean isInstructions = false;
	private int randoms[][];			//  the directions to move the shapes ( if direction = RANDOM )
	private boolean isFilled = false;	// indicates if the shapes should be filled
	private Directions direction = Directions.Up;				// direction the shapes should move
	private Acceleration acceleration = Acceleration.Medium;	// the diff between 2 moves
	private Shapes shape = Shapes.Circle; 						// kind of shape
	private android.graphics.Canvas androidCanvas;
	private View androidView;
	private Form form;
	private YailList textList;	// the given texts to display
								// if e.g. textList contains 3 strings and numShapes is bigger
								// then stringList[] will be filled with random strings from textList

	@SimpleFunction(description = "DrawShapes\n"
			+ "canvas:add a canvas here\n"
			+ "shape:type of shape\n"
			+ "text:text to display if shapetype = TEXT\n"
			+ "count:number of shapes\n"
			+ "speed:refresh rate\n"
			+ "size:max. size of a shape\n"
			+ "filled:fill the shape\n"
			+ "accelaration:size of movement\n"
			+ "direction:direction of movement")
	public void DrawShapes(Canvas canvas, Shapes shape, String text, int count, int speed, int size, boolean filled, Acceleration acceleration, Directions direction) {
		try {
			this.canvas = canvas;
			this.numberOfShapes = count;
			this.sleep = speed;
			this.maxShapeSize = (float) size;
			this.acceleration = acceleration;
			this.direction = direction;
			this.shape = shape;
			this.isFilled = filled;
			this.text = text;
			isInstructions = false;
			init();
		} catch (Exception e) {
			ErrorOccured("DrawShapes: "+e.getMessage()+"\n"+getStackTrace(e));
		}	
	}

	@SuppressWarnings("deprecation")
	@SimpleFunction(description = "DrawText\n"
			+ "canvas:add a canvas here\n"
			+ "texts:a list of texts\n"
			+ "count:number of shapes\n"
			+ "speed:refresh rate\n"
			+ "size:max. size of a shape\n"
			+ "accelaration:size of movement\n"
			+ "direction:direction of movement")
	public void DrawText(Canvas canvas, YailList texts, int count, int speed, int size, Acceleration acceleration, Directions direction) {
		try {
			this.canvas = canvas;
			this.numberOfShapes = count;
			this.sleep = speed;
			this.maxShapeSize = (float) size;
			this.acceleration = acceleration;
			this.direction = direction;
			this.shape = Shapes.Texts;
			this.textList = texts;
			isInstructions = false;
			init();		
		} catch (Exception e) {
			ErrorOccured("DrawText: "+e.getMessage()+"\n"+getStackTrace(e));
		}	
	}

	@SuppressWarnings("deprecation")
	@SimpleFunction(description = "DrawArcs\n"
			+ "canvas:add a canvas here\n"
			+ "text:text to display if shapetype = TEXT\n"
			+ "count:number of shapes\n"
			+ "speed:refresh rate\n"
			+ "size:max. size of a shape\n"
			+ "filled:fill the shape\n"
			+ "accelaration:size of movement\n"
			+ "direction:direction of movement")
	public void DrawArcs(Canvas canvas, float xExtend, float yExtend, float startAngle, float sweepAngle, boolean useCenter, int count, int speed, int size, boolean filled, Acceleration acceleration, Directions direction) {
		try {
			this.canvas = canvas;
			this.numberOfShapes = count;
			this.xExtend = xExtend;
			this.yExtend = yExtend;
			this.startAngle = startAngle;
			this.sweepAngle = sweepAngle;
			this.useCenter = useCenter;
			this.sleep = speed;
			this.maxShapeSize = (float) size;
			this.acceleration = acceleration;
			this.direction = direction;
			this.shape = Shapes.Arc;
			this.isFilled = filled;
			isInstructions = false;
			init();
		} catch (Exception e) {
			ErrorOccured("DrawArcs: "+e.getMessage()+"\n"+getStackTrace(e));
		}	
	}

	@SuppressWarnings("deprecation")
	@SimpleFunction(description = "DrawImage\n"
			+ "canvas:add a canvas here\n"
			+ "asset:an image asset\n"
			+ "count:number of shapes\n"
			+ "speed:refresh rate\n"
			+ "size:max. size of a shape\n"
			+ "accelaration:size of movement\n"
			+ "direction:direction of movement")
	public void DrawImage(Canvas canvas, String asset, int count, int speed, int size, Acceleration acceleration, Directions direction) {
		try {
			this.canvas = canvas;
			this.asset = asset.substring(6);
			this.numberOfShapes = count; 
			this.sleep = speed;
			this.maxShapeSize = (float) size;
			this.acceleration = acceleration;
			this.direction = direction;
			this.shape = Shapes.Image;
			isInstructions = false;
			DebugInfo("assetpath = "+asset);
			init();		
		} catch (Exception e) {
			ErrorOccured("DrawImage: "+e.getMessage()+"\n"+getStackTrace(e));
		}	
	}

	@SuppressWarnings("deprecation")
	@SimpleFunction(description = "DrawComplexShapes\n"
			+ "canvas:add a canvas here\n"
			+ "cShapes:a list of shapes to create\n"
			+ "count:number of shapes\n"
			+ "speed:refresh rate\n"
			+ "size:max. size of the first shape\n"
			+ "filled:fill the shape\n"
			+ "accelaration:size of movement\n"
			+ "direction:direction of movement")
	public void DrawComplexShapes(Canvas canvas, YailList cShapes, int count, int speed, int size, boolean filled, Acceleration acceleration, Directions direction) {
		try {
			this.canvas = canvas;
			this.numberOfShapes = count;
			this.sleep = speed;
			this.maxShapeSize = (float) size;
			this.acceleration = acceleration;
			this.direction = direction;
			this.isFilled = filled;
			this.shapeInstructions = cShapes;
			isInstructions = true;
			this.shape = Shapes.Draw;
			init();
		} catch (Exception e) {
			ErrorOccured("DrawComplexShapes: "+e.getMessage()+"\n"+getStackTrace(e));
		}	
	}

	@SimpleFunction(description = "AddCircle\n"
			+ "xOffset:the relative x of the circle\n"
			+ "yOffset:the relative y of the circle\n"
			+ "relativeSize:the relative size of the circle\n"
			+ "color:thecolor of the circle\n")
	public YailList AddCircle(double xOffset, double yOffset, double relativeSize, int color) {
		YailList instruction=null;
		try {
		List<Object> tempList = new ArrayList<>();
		tempList.add(Shapes.Circle);
		tempList.add(xOffset);
		tempList.add(yOffset);
		tempList.add(relativeSize);
		tempList.add(color);
		instruction = YailList.makeList(tempList);
		//DebugInfo(instruction.toString());

		} catch (Exception e) {
			ErrorOccured("DrawText: "+e.getMessage()+"\n"+getStackTrace(e));
		}	
		return instruction;
	}
	
	@SimpleFunction(description = "AddSquare\n"
			+ "xOffset:the relative x of the square\n"
			+ "yOffset:the relative y of the square\n"
			+ "relativeSize:the relative size of the square\n"
			+ "color:thecolor of the square\n")
	public YailList AddSquare(double xOffset, double yOffset, double relativeSize, int color) {
		YailList instruction=null;
		try {
		List<Object> tempList = new ArrayList<>();
		tempList.add(Shapes.Square);
		tempList.add(xOffset);
		tempList.add(yOffset);
		tempList.add(relativeSize);
		tempList.add(color);
		instruction = YailList.makeList(tempList);
		//DebugInfo(instruction.toString());

		} catch (Exception e) {
			ErrorOccured("DrawText: "+e.getMessage()+"\n"+getStackTrace(e));
		}	
		return instruction;
	}

	@SuppressWarnings("deprecation")
	@SimpleFunction(description = "AddArcs\n"
			+ "xOffset: x position relative to the master shape\n"
			+ "yOffset: y position relative to the master shape\n"
			+ "relativeSize: relative size\n"
			+ "startAngle: startAngle of the arc\n"
			+ "sweepAngle: size of th arc in degrees\n"
			+ "useCenter: uses the center to draw between the edges\n"
			+ "color: color of the arc")
	public YailList AddArcs(double xOffset, double yOffset,  double relativeSize, double startAngle, double sweepAngle, boolean useCenter, int color) {
		YailList instruction=null;
		try {
		List<Object> tempList = new ArrayList<>();
			tempList.add(Shapes.SubArc);
			tempList.add(xOffset);
			tempList.add(yOffset);
			tempList.add(relativeSize);
			tempList.add(startAngle);
			tempList.add(sweepAngle);
			tempList.add(useCenter);
			tempList.add(color);
			instruction = YailList.makeList(tempList);
			DebugInfo(instruction.toString());
		} catch (Exception e) {
			ErrorOccured("AddArcs: "+e.getMessage()+"\n"+getStackTrace(e));
		}	
		return instruction;

	}

	@SuppressWarnings("deprecation")
	@SimpleFunction(description = "AddDoubleRoundRect\n"
			+ "xOffset: x position relative to the master shape\n"
			+ "yOffset: y position relative to the master shape\n"
			+ "relativeSize: relative size\n"
			+ "outerWidth: relative width of the outer rectangle\n"
			+ "outerHeight: relative height of the outer rectangle\n"
			+ "outerRx: x radius of the rounded outer corners\n"
			+ "outerRy: y radius of the rounded outer corners\n"
			+ "innerWidth: relative width of the inner rectangle\n"
			+ "innerHeight: relative height of the inner rectangle\n"
			+ "innerRx: x radius of the rounded inner corners\n"
			+ "innerRy: y radius of the rounded inner corners\n"
			+ "innerxOffset: x offset of the inner rectangle regarding the outer rectangle\n"
			+ "inneryOffset: x offset of the inner rectangle regarding the outer rectangle\n"
			+ "color: color of the rectangles")
	public YailList AddDoubleRoundRect(double xOffset, double yOffset,  double relativeSize, double outerWidth, double outerHeight, double outerRx, double outerRy, double innerWidth, double innerHeight, double innerRx, double innerRy, double innerxOffset, double inneryOffset, int color) {
		YailList instruction=null;
		try {
		List<Object> tempList = new ArrayList<>();
			tempList.add(Shapes.RoundRect);
			tempList.add(xOffset);
			tempList.add(yOffset);
			tempList.add(relativeSize);
			tempList.add(outerWidth);
			tempList.add(outerHeight);
			tempList.add(outerRx);
			tempList.add(outerRy);
			tempList.add(innerWidth);
			tempList.add(innerHeight);
			tempList.add(innerRx);
			tempList.add(innerRy);
			tempList.add(innerxOffset);
			tempList.add(inneryOffset);
			tempList.add(color);
			instruction = YailList.makeList(tempList);
			DebugInfo(instruction.toString());
		} catch (Exception e) {
			ErrorOccured("AddDoubleRoundRect: "+e.getMessage()+"\n"+getStackTrace(e));
		}	
		return instruction;
	}

	@SimpleFunction(description = "stopShapes: stop creating shapes")
	public void StopShapes() {
		stop();
	}

	@SimpleFunction(description = "customColors: use this colors for drawing instead of the default colors")
	public void CustomColors(YailList cColors) {
		this.customColors=cColors.toArray();
		customColorsSet=true;
	}

	@SimpleFunction(description = "customColors: remove the custom colors for drawing")
	public void ClearCustomColors() {
		customColorsSet=false;
	}

	@SimpleEvent(description = "returns the error")
	public void ErrorOccured(String error) {
		EventDispatcher.dispatchEvent(this, "ErrorOccured", error);
	}

	/*
	 * DebugInfo
	 * Use this event if you want to edit/extend this extension
	 * to show relevant info about your changes.
	 */
	@SimpleEvent(description = "returns debug info")
	public void DebugInfo(String message) {
		EventDispatcher.dispatchEvent(this, "DebugInfo", message);
	}
	
	private String getStackTrace(Exception e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		return sw.toString();
	}

	@SuppressWarnings("deprecation")
	private void init() {
		stop();
		canvasWidth = canvas.Width();
		canvasHeight = canvas.Height();
		maxSizes = new float[5];
		maxSize = canvasWidth/5;
		float tmaxSize = Math.min(maxShapeSize, maxSize);
		DebugInfo("tmaxSize:"+tmaxSize);
		for ( int i = 0; i < maxSizes.length-1; i++ ) {
			maxSizes[i] = (float) (tmaxSize * Math.random());
			DebugInfo("debugMaxSize + ["+i+"]="+maxSizes[i]);
		}
		maxSizes[maxSizes.length-1] = tmaxSize;
		// DebugInfo("debugMaxSize:"+debugMaxSize);	
		esize = new double[numberOfShapes];

		if (isInstructions) {
			// DebugInfo("shapeInstructions length: "+shapeInstructions.length());
			instructions = new Instruction[shapeInstructions.length()];
			for (int i = 1; i< shapeInstructions.length();i++) {
				for ( int k = 1 ; k < ((YailList)shapeInstructions.get(i)).length();k++) {
					 DebugInfo("shapeInstructions "+((YailList)shapeInstructions.get(i)).get(k));
				}
				YailList tmp = (YailList) shapeInstructions.get(i);
				Object[] objects = tmp.toArray();
				Object[] values = new Object[objects.length];
				//DebugInfo("objects.length = " + objects.length);
				values[0] = (Shapes) objects[0];
				for ( int k = 1 ; k < objects.length;k++) {
					//DebugInfo("class of "+k+ " : " +objects[k].getClass());
					if (objects[k] instanceof IntNum ) {
						values[k] = ((IntNum)objects[k]).doubleValue();
					} else if (objects[k] instanceof Double ) {
						values[k] = ((Double)objects[k]).doubleValue();
					} else if (objects[k] instanceof Integer ) {
						values[k] = ((Integer)objects[k]).doubleValue();
					} else if (objects[k] instanceof Boolean ) {
						values[k] = objects[k];
					} else if (objects[k] instanceof YailList ) {
						values[k] = (YailList)objects[k];
					} else if (objects[k] instanceof Float ) {
						values[k] = (Float)objects[k];
					} else {
						values[k] = ((DFloNum)objects[k]).doubleValue();
					}
				}
				if ((((Shapes)values[0]).compareTo(Shapes.SubArc)) == 0) {
					//DebugInfo("SUBARC found: "+(double) values[1]+ " "+(double) values[1]+ " "+(double) values[2]+ " "+(double) values[3]+ " "+(double) values[4]+ " "+(double) values[5]);
					instructions[i-1] = new ArcInstruction();
					((ArcInstruction) instructions[i-1]).initialize((Shapes)values[0], (double) values[1], (double) values[2], (double) values[3], (double) values[4], (double) values[5], (boolean) values[6], (int)((double) values[7]));					
				} else if ((((Shapes)values[0]).compareTo(Shapes.RoundRect)) == 0) {
					initAndroidCanvas();
					//DebugInfo("ROUNDRECT found: "+(double) values[1]+ " "+(double) values[1]+ " "+(double) values[2]+ " "+(double) values[3]+ " "+(double) values[4]+ " "+(double) values[5]);
					instructions[i-1] = new DoubleRectInstruction();
					((DoubleRectInstruction) instructions[i-1]).initialize((Shapes)values[0], (double) values[1], (double) values[2], (double) values[3], (double) values[4], (double) values[5], (double) values[6], (double) values[7], (double) values[8], (double) values[9], (double) values[10], (double) values[11], (double) values[12], (double) values[13], (int)((double) values[14]));					
				} else {
					instructions[i-1] = new Instruction();
					instructions[i-1].initialize((Shapes)values[0], (double) values[1], (double) values[2], (double) values[3], (double) values[3], (int)((double) values[4]) );
				}
			}
			complexShapes = new ComplexShape2D[numberOfShapes];
			for (int i = 0; i < numberOfShapes; i++) {
				complexShapes[i] = new ComplexShape2D(instructions);
				// sets location for each shape with the given random size
				setRandomXY(i, (float) (10+(20 * Math.random())));        
			}

		} else {
			// an array of type Shapes2D.Float
			shapeObjects = new Shape2D[numberOfShapes];
			// fills the shapes array with Shape2D.Float objects
			for (int i = 0; i < numberOfShapes; i++) {
				shapeObjects[i] = new Shape2D();
				// sets location for each shape with the given random size
				setRandomXY(i, (float) (10+(20 * Math.random())));        
			}
			if (shape.equals(Shapes.Texts)) {
				stringList = new String[numberOfShapes];
				int limit = 0;
/*				DebugInfo("numberOfShapes:"+numberOfShapes);
				DebugInfo("textList.size:"+textList.size());
				for (int i = 0; i<textList.size(); i++) {
					DebugInfo(i +" =  "+ textList.getString(i));
				}
*/
				if ( textList.size() >= numberOfShapes ) { // there are more or equal texts available than shapes
					for (; limit < numberOfShapes; limit++) {
						// DebugInfo("limitA:"+limit);
						stringList[limit] = textList.getString(limit);
					}    		
				} else { // there are more shapes than texts: first fill shapes with every text and then fill with random text
					for (; limit < textList.size(); limit++) {
						// DebugInfo("limitB:"+limit);
						stringList[limit] = textList.getString(limit);
					}
					for (; limit < numberOfShapes; limit++) {
						// DebugInfo("limitC:"+limit);
						stringList[limit] = textList.getString(r.nextInt(textList.size()));
					}	        
				}
/*				for (int i = 0; i<numberOfShapes; i++) {
					DebugInfo(i +" =  "+ stringList[i]);
				}
*/
			}
			if (shape.equals(Shapes.Image)) {
				initAndroidCanvas();
				BitmapDrawable bd2 = (BitmapDrawable) BitmapDrawable.createFromPath(asset);
				bitmap = bd2.getBitmap();
				DebugInfo("bitmap = "+bitmap);
			}
		}

		switch (direction) {
		case Up:
			moveX = 0;
			moveY = 0-acceleration.toUnderlyingValue();
			break;
		case Down:
			moveX = 0;
			moveY = acceleration.toUnderlyingValue();
			break;
		case Left:
			moveX = 0-acceleration.toUnderlyingValue();
			moveY = 0;
			break;
		case Right:
			moveX = acceleration.toUnderlyingValue();
			moveY = 0;
			break;
		case Wobble:
			// set nothing, calculate movements every step
			break;	
		case Random:
			fillRandoms();
			break;	
		}
		start();
	}

	private void fillRandoms() {
		List<Integer> l = new ArrayList<>();
		l.add(0-acceleration.toUnderlyingValue());
		l.add(acceleration.toUnderlyingValue());
		l.add(0);
		randoms = new int[numberOfShapes][2];
		for (int i = 0; i < numberOfShapes; i++) {
			randoms[i][0] = l.get(r.nextInt(l.size()));
			randoms[i][1] = l.get(r.nextInt(l.size()));
		}	
	}

	/*
	 * sets the bounds of the shape specified by i, using the given
	 * width, height and random size.
	 */
	private void setRandomXY(int i, double size) {
		esize[i] = size;
		double x =  (Math.random() * (canvasWidth-(maxSize/2)));
		double y =  (Math.random() * (canvasHeight-(maxSize/2)));
		if (isInstructions) {
			// DebugInfo("setRandom XY:  x="+x+" y="+y+" size="+size);
			complexShapes[i].initFrame(x, y, size, size);
		} else {
			shapeObjects[i].setFrame(x, y, size, size);
		}
	}

	/*
	 * increase shape size until maxSize
	 */
	private void step() {
		canvas.Clear();
		for (int i = 0; i < numberOfShapes; i++) {
			esize[i]++;
			int mSize = r.nextInt(maxSizes.length);
			if (esize[i] > maxSizes[mSize]) {
				setRandomXY(i, 1);
			} else {
				if (direction.equals(Directions.Wobble)) {
					List<Integer> l = new ArrayList<>();
					l.add(0-acceleration.toUnderlyingValue());
					l.add(acceleration.toUnderlyingValue());
					if (isInstructions) {
						complexShapes[i].setFrame(l.get(r.nextInt(l.size())),l.get(r.nextInt(l.size())),esize[i], esize[i]);
					} else {
						shapeObjects[i].setFrame(shapeObjects[i].getX()+l.get(r.nextInt(l.size())), shapeObjects[i].getY()+l.get(r.nextInt(l.size())),
								esize[i], esize[i]);
					}
				} else if (direction.equals(Directions.Random)) {
					if (isInstructions) {
						complexShapes[i].setFrame(randoms[i][0], randoms[i][1], esize[i], esize[i]);
					} else {        		
						shapeObjects[i].setFrame(shapeObjects[i].getX()+randoms[i][0], shapeObjects[i].getY()+randoms[i][1],esize[i], esize[i]);
					}
				} else {
					if (isInstructions) {
						complexShapes[i].setFrame(moveX,moveY,esize[i], esize[i]); 
					} else {
						shapeObjects[i].setFrame(shapeObjects[i].getX()+moveX, shapeObjects[i].getY()+moveY,
								esize[i], esize[i]);
					}
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	public void draw() {
		// sets the color and stroke size and draws each shape
		step();
		String txt = "";
		for (int i = 0; i < numberOfShapes; i++) {
			switch (shape) {
			case Text:
				txt = text;
				break;
			case Texts:
				txt = stringList[i];
				break;
			default:
				break;
			}
			if (isInstructions ) {
				complexShapes[i].draw(i,isFilled,  txt);
			} else {
				if (customColorsSet) {
					canvas.PaintColor(((IntNum) customColors[i%customColors.length]).intValue());
				} else {
					canvas.PaintColor(colors[i%colors.length]);
				}
				shapeObjects[i].draw(shape,isFilled,  txt);
			}
		}
	}

	private void DrawPacman(int x, int y, int size) {
		canvas.DrawArc(x, y, x+size, y+size, 30, 300, true, isFilled);
		canvas.PaintColor(Color.BLACK);
		canvas.DrawCircle(x+(size/2), y+(size/4), size/10, isFilled);

	}

	private void DrawText(String text,int x, int y, int size) {
		canvas.FontSize(size);
		canvas.DrawText(text, x, y);
	}
	
	private void initAndroidCanvas() {
		if(androidCanvas!=null)return;
		androidView = canvas.getView();
		androidView.requestLayout();
		Field fld = null;
		try {
			fld = (androidView).getClass().getDeclaredField("canvas");
			fld.setAccessible(true);
			androidCanvas = (android.graphics.Canvas) fld.get(androidView);
		} catch (NoSuchFieldException e) {
			//e.printStackTrace();
		} catch (SecurityException e) {
			//e.printStackTrace();
		} catch (IllegalArgumentException e) {
			//e.printStackTrace();
		} catch (IllegalAccessException e) {
			//e.printStackTrace();
		}
	//	DebugInfo("MIt App canvas: "+canvas.Width()+" / "+canvas.Height());
	//	DebugInfo("Android canvas: "+androidCanvas.getWidth()+" / "+androidCanvas.getHeight());

	}
	
	//TODO: $form().deviceDensity()==> resize : density in de init()
	private void DrawAsset(int x, int y, int size) {
		int dd = (int)form.deviceDensity();
		DebugInfo("##############deviceDensity = "+dd);
		Rect dest = new Rect(x*dd,y*dd,(x+size)*dd,(y+size)*dd);
		try {
			androidCanvas.drawBitmap(bitmap, null, dest, null);
			androidView.invalidate();
		} catch (SecurityException e) {
			//e.printStackTrace();
		} catch (IllegalArgumentException e) {
			//e.printStackTrace();
		} 		
	}
	
	private void DrawTexts(String txt,int x, int y, int size) {
		canvas.FontSize(size);
		Optional<String> optionalValue = Optional.of(txt);
		if (optionalValue.isPresent()) {
			canvas.DrawText(optionalValue.get(), x, y);
		} else {
			// DebugInfo("txt is NULL");
		}

		if (txt!=null)
			canvas.DrawText(txt, x, y);
	}

	private void DrawSquare(int x, int y, int size) {
		List<YailList> yailPointsList = new ArrayList<>();
		yailPointsList.add(YailList.makeList(new Integer[] { x , y  }));
		yailPointsList.add(YailList.makeList(new Integer[] { x + size , y  }));
		yailPointsList.add(YailList.makeList(new Integer[] { x + size, y + size }));
		yailPointsList.add(YailList.makeList(new Integer[] { x , y +size }));
		YailList pointList = YailList.makeList(yailPointsList);
		canvas.DrawShape(pointList, isFilled);
	}


	public void start() {
		thread = new Thread(this);
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.start();
	}

	public synchronized void stop() {
		thread = null;
	}

	public void run() {
		Thread me = Thread.currentThread();
		while (thread == me) {
			draw();
			try {
				Thread.sleep(sleep);
			} catch (InterruptedException e) { break; }
		}
		thread = null;
	}

	private class Shape2D {
		double x,y;
		double width,height;

		public void setFrame(double x, double y, double width, double height) {
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;		
		}
		
		public void draw(Shapes shape, int size, boolean filled, String txt, double aStartAngle, double aSweepAngle, boolean aUseCenter) {
			//DebugInfo("left:"+(int)getX()+"  top:"+ (int)getY()+"  right:"+ (int)(getX()+size)+"  bottom:"+ (int)(getY()+size)+"  startAngle:"+ aStartAngle+"  sweepAngle:"+ aStartAngle+"  useCenter:"+ aUseCenter+"  filled:"+ filled);
			canvas.DrawArc((int)getX(), (int)getY(), (int)(getX()+size), (int)(getY()+size), (float)aStartAngle, (float)aSweepAngle, aUseCenter, filled);
		}
		
		private Double getDoubleValue(Object o) {
			if (o instanceof IntNum ) {
				return ((IntNum)o).doubleValue();
			} else if (o instanceof Double ) {
				return ((Double)o).doubleValue();
			} else if (o instanceof Integer ) {
				return ((Integer)o).doubleValue();
			} else if (o instanceof Float ) {
				return ((Float)o).doubleValue();
			} else {
				return ((DFloNum)o).doubleValue();
			}
		}
		
		public void draw(Shapes shape, int aSize, boolean filled, String string, double outerWidth, double outerHeight,
			double outerRx, double outerRy, double innerWidth, double innerHeight, double innerRx, double innerRy,
			double innerOffsetx, double innerOffsety, int color) {
			int dd = (int)form.deviceDensity();
			double rLeft = getDoubleValue(getX());
			double rTop = getDoubleValue(getY());
			double rRight = rLeft + aSize*outerWidth;
			double rBottom = rTop + aSize*outerHeight;
			RectF outerRect = new RectF((float)(rLeft*dd),
										(float)(rTop*dd),
										(float)(rRight*dd),
										(float)(rBottom)*dd);
			//DebugInfo("outerRect: "+outerRect.toShortString());
			rLeft = getDoubleValue(getX())+aSize*innerOffsetx;
			rTop = getDoubleValue(getY())+aSize*innerOffsety;
			rRight = rLeft + aSize*innerWidth;
			rBottom = rTop + aSize*innerHeight;
			RectF innerRect = new RectF((float)(rLeft*dd),
										(float)(rTop*dd),
										(float)(rRight*dd),
										(float)(rBottom)*dd);
			//DebugInfo("innerRect: "+innerRect.toShortString());
			Paint paint = new Paint();	
			if (filled) {
				paint.setStyle(Paint.Style.FILL);
			} else {
				paint.setStyle(Paint.Style.STROKE);
			}
	        paint.setColor(color);
	        paint.setStrokeWidth(5);

			try {
				androidCanvas.drawDoubleRoundRect(outerRect, (float) outerRx, (float)outerRy, innerRect, (float)innerRx, (float)innerRy, paint);
			} catch (SecurityException e) {
				//e.printStackTrace();
			} catch (IllegalArgumentException e) {
				//e.printStackTrace();
			} 		

		}

		@SuppressWarnings("deprecation")
		public void draw(Shapes shape, int size, boolean filled, String txt) {
			 //DebugInfo("draw shape2D: shape="+shape+"  x="+(int)getX());
			switch (shape) {
			case Arc:
				DebugInfo("left:"+(int)getX()+"  top:"+ (int)getY()+"  right:"+ (int)(getX()+size*xExtend)+"  bottom:"+ (int)(getY()+size*yExtend)+"  startAngle:"+ startAngle+"  sweepAngle:"+ sweepAngle+"  useCenter:"+ useCenter+"  filled:"+ filled);
				canvas.DrawArc((int)getX(), (int)getY(), (int)(getX()+size*xExtend), (int)(getY()+size*yExtend), startAngle, sweepAngle, useCenter, filled);
				break;
			case Circle:
				canvas.DrawCircle((int)getX(), (int)getY(), (float) size, filled);
				break;
			case Pacman:
				DrawPacman((int)getX(), (int)getY(), size);
				break;
			case Square:
				DrawSquare((int)getX(), (int)getY(), size);
				break;
			case Text:
				DrawText(txt,(int)getX(), (int)getY(), size);
				break;
			case Image:
				DrawAsset((int)getX(), (int)getY(), size);
				break;
			case Texts:
				DrawTexts(txt,(int)getX(), (int)getY(), (int)size);
				break;
			default:
				break;
			}		
		}
		
		public void draw(Shapes shape, boolean filled, String txt) {
			//DebugInfo("draw shape2D: shape="+shape+"  x="+(int)getX());
			draw(shape,(int)getSize(),filled,txt);
		}

		public double getX() {
			return x;
		}
		public void setX(double x) {
			this.x = x;
		}		
		public double getY() {
			return y;
		}
		public void setY(double y) {
			this.y = y;	
		}
		public double getWidth() {
			return width;
		}
		public double getSize() {
			return width;
		}

		public void setWidth(double width) {
			this.width = width;
		}
		public double getHeight() {
			return height;
		}
		public void setHeight(double height) {
			this.height = height;
		}

	}

	private class ComplexShape2D  {
		Instruction[] instructions;
		Shape2D masterShape;

		public ComplexShape2D(Instruction[] instructions) {
			super();
			this.instructions = instructions;
/*			 DebugInfo("constructor setting instructions");
			for (int j = 0; j < instructions.length-1; j++) {
				 DebugInfo(j + "==> "+instructions[j].toString());
			}	
			DebugInfo("end setting instructions");
*/			
			masterShape = new Shape2D();
		}

		public void initFrame(double x, double y, double size, double size2) {
			masterShape.setFrame(x, y, size, size2);
		}

		public void setFrame(Integer x1, Integer y1, double s1, double s2) {
			masterShape.setFrame(masterShape.getX()+x1, masterShape.getY()+y1, s1, s2);
		}

		public void draw(int seq,boolean filled,String txt) {
			//DebugInfo("drawing complex il="+instructions.length+"   seq="+seq);
			drawOrigin(filled,txt);
			for (int j = 1; j < instructions.length-1; j++) {
				canvas.PaintColor(colors[j%colors.length]);
				//DebugInfo("instruction: "+instructions[j]);
				drawRelative(instructions[j],filled);
			}
		}

		private void drawOrigin(boolean filled,String txt) {
			//DebugInfo("drawing origin: x="+(int)masterShape.getX()+ " y="+(int)masterShape.getY());
			canvas.PaintColor(instructions[0].getColor());
			//DebugInfo("origin paintcolor = "+instructions[0].getColor());
			masterShape.draw(instructions[0].getShape(), (int)masterShape.getHeight(), filled, "");
		}

		private void drawRelative(Instruction instruct, boolean filled) {
			double tx = masterShape.getX() + (masterShape.getWidth()*instruct.getX());
			double ty = masterShape.getY() + (masterShape.getWidth()*instruct.getY());
			double ts = instruct.getSize()*masterShape.getWidth();
			//DebugInfo("drawing relative: x="+(int)tx+" y="+(int)ty+" size="+(int)ts);
			instruct.setShapeFrame(tx, ty, ts, ts);
			canvas.PaintColor(instruct.getColor());
			instruct.draw(ts,filled);
		}


	}

	private class Instruction {	

		Shapes shape = Shapes.Circle; 
		Shape2D relatedShape;
		double x,y,width,height;
		int color;

		public Shapes getShape() {
			return shape;
		}

		public void draw(double aSize, boolean filled) {
			//DebugInfo("******drawing relatedShape******* "+   "color=" + color);
			relatedShape.draw(shape, (int)aSize, filled, "");
			//DebugInfo("******end drawing relatedShape*******");
		}

		public Shape2D getShape2D() {
			return relatedShape;
		}

		public void setShape(Shapes shape) {
			this.shape = shape;
		}

		public void setShape2D(Shape2D aShape2D) {
			this.relatedShape = aShape2D;
		}

		public void initialize(Shapes aShape, double x, double y, double width, double height, int color) {
			relatedShape = new Shape2D();
			shape = aShape;
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
			this.color = color;
		}
		
		public double getY() {
			return y;
		}

		public double getSize() {
			return height;
		}

		public double getX() {
			return x;
		}

		public int getColor() {
			return color;
		}

		public void setShapeFrame(double x, double y, double width, double height) {
			relatedShape.setFrame(x, y, width, height);
		}

		public String toString() {
			return "shape: "+ shape + "x: "+getX() + "y: "+getY() + "size: "+getSize();
		}
	}
	
	private class ArcInstruction extends Instruction {	
		double iStartAngle,iSweepAngle;
		boolean iUseCenter;

		public void initialize(Shapes aShape, double x, double y, double size, double aStartAngle, double aSweepAngle, boolean aUseCenter, int color) {
			relatedShape = new Shape2D();
			shape = aShape;
			this.x = x;
			this.y = y;
//			this.width = size;
			this.height = size;
			this.iStartAngle = aStartAngle;
			this.iSweepAngle = aSweepAngle;
			this.iUseCenter = aUseCenter;
			this.color = color;
			//DebugInfo("initialised: "+this.toString());
		}
		
		public void draw(double aSize, boolean filled) {
			//DebugInfo("******drawing relatedShape*******iStartAngle=" + iStartAngle+"   color="+color);
			relatedShape.draw(shape, (int)aSize, filled, "",iStartAngle,iSweepAngle,iUseCenter);
			//DebugInfo("******end drawing relatedShape*******");
		}
		
		public String toString() {
			return "shape: "+ shape + "x: "+getX() + "y: "+getY() + "size: "+getSize() + "iStartAngle: "+iStartAngle;
		}


	}
	
	private class DoubleRectInstruction extends Instruction {	
		double outerRx,outerRy,innerRx,innerRy,outerWidth,outerHeight,innerWidth,innerHeight,innerOffsetx,innerOffsety;

		public void initialize(Shapes aShape, double x, double y, double size, double outerWidth, double outerHeight, double outerRx, double outerRy, double innerWidth, double innerHeight, double innerRx, double innerRy, double innerOffsetx, double innerOffsety, int color) {
			relatedShape = new Shape2D();
			shape = aShape;
			this.x = x;
			this.y = y;
			this.height = size;
			this.outerWidth = outerWidth;
			this.outerHeight = outerHeight;
			this.outerRx = outerRx;
			this.outerRy = outerRy;
			this.innerWidth = innerWidth;
			this.innerHeight = innerHeight;
			this.innerRx = innerRx;
			this.innerRy = innerRy;
			this.innerOffsetx = innerOffsetx;
			this.innerOffsety = innerOffsety;
			this.color = color;
			DebugInfo("initialised: "+this.toString());
		}
		
		public void draw(double aSize, boolean filled) {
			//DebugInfo("******drawing relatedShape*******iStartAngle=" + iStartAngle+"   color="+color);
			relatedShape.draw(shape, (int)aSize, filled, "",outerWidth,outerHeight,outerRx,outerRy,innerWidth,innerHeight,innerRx,innerRy,innerOffsetx,innerOffsety,color);
			//DebugInfo("******end drawing relatedShape*******");
		}
		
		public String toString() {
			return "shape: "+ shape + "x: "+getX() + "y: "+getY() + "size: "+getSize() + "outerRx: "+outerRx;
		}
	}
}
