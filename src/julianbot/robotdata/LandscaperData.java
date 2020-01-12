package julianbot.robotdata;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class LandscaperData extends RobotData {

	private int currentRole;
	public static final int TRAVEL_TO_HQ = 0;
	public static final int DEFEND_HQ_FROM_FLOOD = 1;
	
	private MapLocation hqLocation;
	private MapLocation enemyHQLocation;
		private Direction enemyHQBuryDigDirection;
	
	private Direction searchDirection;
	
	public LandscaperData(RobotController rc) {
		super(rc);
		currentRole = TRAVEL_TO_HQ;
		searchDirection = spawnerLocation.directionTo(rc.getLocation());
	}
	
	public int getCurrentRole() {
		return currentRole;
	}
	
	public void setCurrentRole(int currentRole) {
		this.currentRole = currentRole;
	}

	public MapLocation getHqLocation() {
		return hqLocation;
	}

	public void setHqLocation(MapLocation hqLocation) {
		this.hqLocation = hqLocation;
	}

	public Direction getSearchDirection() {
		return searchDirection;
	}

	public void setSearchDirection(Direction searchDirection) {
		this.searchDirection = searchDirection;
	}

	public MapLocation getEnemyHQLocation() {
		return enemyHQLocation;
	}

	public void setEnemyHQLocation(MapLocation enemyHQLocation) {
		this.enemyHQLocation = enemyHQLocation;
	}

	public Direction getEnemyHQBuryDigDirection() {
		return enemyHQBuryDigDirection;
	}

	public void setEnemyHQBuryDigDirection(Direction enemyHQBuryDigDirection) {
		this.enemyHQBuryDigDirection = enemyHQBuryDigDirection;
	}
	
}
