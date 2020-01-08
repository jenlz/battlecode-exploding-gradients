package julianbot.utils.pathfinder;

import battlecode.common.Direction;
import battlecode.common.MapLocation;

public class PathfinderData {

	private boolean isRouting;
	
	private final static int MAX_WAYPOINTS = 5;
	private MapLocation[] waypoints;
	private MapLocation destination;
	
	private Direction previousDirection;
	private Direction currentDirection;
	
	public PathfinderData() {
		isRouting = false;
		waypoints = new MapLocation[MAX_WAYPOINTS];
		previousDirection = Direction.CENTER;
		currentDirection = Direction.CENTER;
	}
	
	public boolean isRouting() {
		return isRouting;
	}

	public void setRouting(boolean isRouting) {
		this.isRouting = isRouting;
	}

	public void beginRoutingTo(MapLocation destination) {
		this.destination = destination;
		if(this.destination != null) isRouting = true;
	}
	
	public void stopRouting() {
		isRouting = false;
		destination = null;
		previousDirection = Direction.CENTER;
		currentDirection = Direction.CENTER;
	}
	
	public void addWaypoint(MapLocation waypoint) {
		for(int i = 0; i < MAX_WAYPOINTS - 1; i++) {
			waypoints[i] = waypoints[i + 1];
		}
		
		waypoints[MAX_WAYPOINTS - 1] = waypoint;
	}

	public Direction getPreviousDirection() {
		return previousDirection;
	}

	public void setPreviousDirection(Direction previousDirection) {
		this.previousDirection = previousDirection;
	}

	public Direction getCurrentDirection() {
		return currentDirection;
	}

	public void setCurrentDirection(Direction currentDirection) {
		this.currentDirection = currentDirection;
	}
	
}
