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
		
	public LandscaperData(RobotController rc) {
		super(rc);
		currentRole = TRAVEL_TO_HQ;
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
