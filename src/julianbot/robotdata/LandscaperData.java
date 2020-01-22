package julianbot.robotdata;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class LandscaperData extends RobotData {

	private int currentRole;
	public static final int TRAVEL_TO_HQ = 0;
	public static final int DEFEND_HQ_FROM_FLOOD = 1;
	public static final int ATTACK = 2;
	
	//HQ DATA
	private MapLocation hqLocation;
		private int hqElevation;
	private MapLocation enemyHQLocation;
		private Direction enemyHQBuryDigDirection;
	
	//WALL DIMENSIONS
	private int wallOffsetXMin, wallOffsetXMax, wallOffsetYMin, wallOffsetYMax;
	
	//CLEARING OBSTRUCTIONS
	private boolean clearingObstruction;
		
	public LandscaperData(RobotController rc, MapLocation spawnerLocation) {
		super(rc, spawnerLocation);
		currentRole = DEFEND_HQ_FROM_FLOOD;
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
	
	public int getHqElevation() {
		return hqElevation;
	}

	public void setHqElevation(int hqElevation) {
		this.hqElevation = hqElevation;
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
	
	public int getWallOffsetXMin() {
		return wallOffsetXMin;
	}

	public int getWallOffsetXMax() {
		return wallOffsetXMax;
	}

	public int getWallOffsetYMin() {
		return wallOffsetYMin;
	}

	public int getWallOffsetYMax() {
		return wallOffsetYMax;
	}
	
	public void setWallOffsetBounds(int wallOffsetXMin, int wallOffsetXMax, int wallOffsetYMin, int wallOffsetYMax) {
		this.wallOffsetXMin = wallOffsetXMin;
		this.wallOffsetXMax = wallOffsetXMax;
		this.wallOffsetYMin = wallOffsetYMin;
		this.wallOffsetYMax = wallOffsetYMax;
	}

	public boolean isClearingObstruction() {
		return clearingObstruction;
	}

	public void setClearingObstruction(boolean clearingObstruction) {
		this.clearingObstruction = clearingObstruction;
	}
	
}
