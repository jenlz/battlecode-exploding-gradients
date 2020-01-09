package julianbot.utils.pathfinder;

import battlecode.common.Direction;
import battlecode.common.MapLocation;

public class PathfinderData {

	private boolean isRouting;
	private MapLocation destination;
	private Direction previousDirection;
	private Direction currentDirection;
	
	public PathfinderData() {
		isRouting = false;
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
