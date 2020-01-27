package bustedJulianbot.robotdata;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class LandscaperData extends RobotData {

	private int currentRole;
	public static final int TRAVEL_TO_HQ = 0;
	public static final int DEFEND_HQ_FROM_FLOOD = 1;
	public static final int ATTACK = 2;
	public static final int OUTPOST_KEEPER = 3;
		private boolean outpostBuildStarted;
	
	//HQ DATA
	private MapLocation hqLocation;
		private int hqElevation;
	private MapLocation enemyHQLocation;
		private Direction enemyHQBuryDigDirection;
	
	//FLOODING RESPONSE
	private MapLocation[] lastResortBuildLocations;
	
	//WALL BUILD
	private boolean wallBuildHandled;
	
	//CLEARING OBSTRUCTIONS
	private boolean clearingObstruction;
	
	//ATTACK
	private RobotInfo closestEnemyBuilding;
	
	//TRANSACTIONS
	private int transactionRound;
	
	public LandscaperData(RobotController rc, MapLocation spawnerLocation) {
		super(rc, spawnerLocation);
		currentRole = TRAVEL_TO_HQ;
		transactionRound = 1;
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

	public MapLocation[] getLastResortBuildLocations() {
		return lastResortBuildLocations;
	}

	public void setLastResortBuildLocations(MapLocation[] lastResortBuildLocations) {
		this.lastResortBuildLocations = lastResortBuildLocations;
	}

	public boolean isClearingObstruction() {
		return clearingObstruction;
	}

	public void setClearingObstruction(boolean clearingObstruction) {
		this.clearingObstruction = clearingObstruction;
	}

	public RobotInfo getClosestEnemyBuilding() {
		return closestEnemyBuilding;
	}

	public void setClosestEnemyBuilding(RobotInfo closestEnemyBuilding) {
		this.closestEnemyBuilding = closestEnemyBuilding;
	}

	public boolean isWallBuildHandled() {
		return wallBuildHandled;
	}

	public void setWallBuildHandled(boolean wallBuildHandled) {
		this.wallBuildHandled = wallBuildHandled;
	}

	public boolean isOutpostBuildStarted() {
		return outpostBuildStarted;
	}

	public void setOutpostBuildStarted(boolean outpostBuildStarted) {
		this.outpostBuildStarted = outpostBuildStarted;
	}

	public int getTransactionRound() {
		return transactionRound;
	}

	public void setTransactionRound(int transactionRound) {
		this.transactionRound = transactionRound;
	}
	
}
