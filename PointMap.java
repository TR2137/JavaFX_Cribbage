//a class used to hold simple coordinates for mouse press processing
public class PointMap {

	private int xCoor;
	private int yCoor;
	
	//constructor - bare
	public PointMap() {
		setX(0);
		setY(0);
	}
	
	//constructor with (x,y)
	public PointMap(int x, int y) {
		setX(x);
		setY(y);
	}

	//getters and setters

	public int getX() {
		return xCoor;
	}

	public void setX(int x) {
		xCoor = x;
	}

	public int getY() {
		return yCoor;
	}

	public void setY(int y) {
		yCoor = y;
	}
	
}
