package julianbot.robotdata;

import battlecode.common.*;

public class FulfillmentCenterData extends RobotData {
		
	private Direction buildDirection;
	private int dronesBuilt;
	
	private boolean stableSoupIncomeConfirmed;
		private int transactionRound;
		private boolean searchedForVaporator;
	
	private MapLocation hqLocation;
	private boolean enemyHqLocated;
	
	private int wallOffsetXMin, wallOffsetXMax, wallOffsetYMin, wallOffsetYMax;
	private boolean baseOnEdge;
	
	public FulfillmentCenterData(RobotController rc, MapLocation spawnerLocation) {
		super(rc, spawnerLocation);
		buildDirection = Direction.NORTH;
		transactionRound = 1;
	}
	
	public int getDronesBuilt() {
		return dronesBuilt;
	}

	public void incrementDronesBuilt() {
		dronesBuilt++;
	}
	
	public void setDronesBuilt(int dronesBuilt) {
		this.dronesBuilt = dronesBuilt;
	}

	public Direction getBuildDirection() {
		return buildDirection;
	}

	public void setBuildDirection(Direction buildDirection) {
		this.buildDirection = buildDirection;
	}

	public boolean isStableSoupIncomeConfirmed() {
		return stableSoupIncomeConfirmed;
	}

	public void setStableSoupIncomeConfirmed(boolean stableSoupIncomeConfirmed) {
		this.stableSoupIncomeConfirmed = stableSoupIncomeConfirmed;
	}

	public int getTransactionRound() {
		return transactionRound;
	}

	public void setTransactionRound(int transactionRound) {
		this.transactionRound = transactionRound;
	}

	public boolean searchedForVaporator() {
		return searchedForVaporator;
	}

	public void setSearchedForVaporator(boolean searchedForVaporator) {
		this.searchedForVaporator = searchedForVaporator;
	}
	
	public MapLocation getHqLocation() {
		return hqLocation;
	}

	public void setHqLocation(MapLocation hqLocation) {
		this.hqLocation = hqLocation;
	}

	public boolean isEnemyHqLocated() {
		return enemyHqLocated;
	}

	public void setEnemyHqLocated(boolean enemyHqLocated) {
		this.enemyHqLocated = enemyHqLocated;
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
	
	public boolean isBaseOnEdge() {
		return baseOnEdge;
	}

	public void setBaseOnEdge(boolean baseOnEdge) {
		this.baseOnEdge = baseOnEdge;
	}
}
