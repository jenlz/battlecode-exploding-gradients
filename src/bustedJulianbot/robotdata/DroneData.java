package bustedJulianbot.robotdata;

import java.util.ArrayList;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class DroneData extends ScoutData {
	
	//ROUTING
	private ArrayList<MapLocation> floodedLocs;
	
	//TRANSACTION SEARCHING
	private int transactionRound;
	
	//CARGO
	private Team enemyFrom;
	private boolean holdingEnemy;
	private boolean holdingCow;
	private RobotType cargoType;
	
	//ATTACKS
	private boolean preparingToAttack;
		private MapLocation attackWaitLocation;
		private MapLocation baseAttackWaitLocation;
		private MapLocation defaultAttackWaitLocation;
			private int attackLocationCycles;
	private boolean awaitingKillOrder;
	private boolean receivedKillOrder;
		private int killOrderReceptionRound;
	private static final Direction[][] WAIT_LOCATION_ORDER = new Direction[][]{
		{null, Direction.EAST, Direction.EAST, Direction.EAST, Direction.EAST, Direction.SOUTHEAST, null},
		{Direction.NORTHEAST, null, null, null, null, null, Direction.SOUTH},
		{Direction.NORTH, null, null, null, null, null, Direction.SOUTH},
		{Direction.NORTH, null, null, null, null, null, Direction.SOUTH},
		{Direction.NORTH, null, null, null, null, null, Direction.SOUTH},
		{Direction.NORTH, null, null, null, null, null, Direction.SOUTHWEST},
		{null, Direction.NORTHWEST, Direction.WEST, Direction.WEST, Direction.WEST, Direction.WEST, null}};
	private static final int WAIT_LOCATION_GRID_DIMENSION = 7;
		private int gridXShift;
		private int gridYShift;
	
	//WALL STATUS
	private boolean wallBuildChecked;
	private boolean wallBuildConfirmed;
	private MapLocation nextWallSegment;
	
	//LANDMARKS
	private ArrayList<MapLocation> outpostLocs;
		private ArrayList<MapLocation> removedOutpostLocs;
		private MapLocation outpostLocToSearch;
		private int outpostLocToSearchIndex;
		
	public DroneData(RobotController rc, MapLocation spawnerLocation) {
		super(rc, spawnerLocation);
		holdingEnemy = false;
		transactionRound = 1;
		floodedLocs = new ArrayList<MapLocation>();
		
		gridXShift = gridYShift = WAIT_LOCATION_GRID_DIMENSION / 2;
		
		outpostLocs = new ArrayList<MapLocation>();
		removedOutpostLocs = new ArrayList<MapLocation>();
	}

	public ArrayList<MapLocation> getFloodedLocs() {
		return floodedLocs;
	}

	/**
	 * Adds flooded loc if it not too close to already added one or is already added
	 * @param loc
	 */
	public void addFloodedLoc(MapLocation loc) {
		boolean shouldAdd = true;
		for (MapLocation floodedLoc : floodedLocs) {
			if (loc.distanceSquaredTo(floodedLoc) < 16 || loc.equals(floodedLoc)) {
				shouldAdd = false;
			}
		}
		if (shouldAdd) floodedLocs.add(loc);
	}

	public boolean getHoldingEnemy() {
		return holdingEnemy;
	}
	
	public void setHoldingEnemy(boolean enemy) {
		holdingEnemy = enemy;
		if(holdingEnemy) holdingCow = false;
	}

	public boolean getHoldingCow() {
		return holdingCow;
	}

	public void setHoldingCow(boolean holdingCow) {
		this.holdingCow = holdingCow;
		if(holdingCow) holdingEnemy = false;
	}
	
	public RobotType getCargoType() {
		return cargoType;
	}

	public void setCargoType(RobotType cargoType) {
		this.cargoType = cargoType;
	}

	public void setEnemyFrom(Team team) {
		enemyFrom = team;
	}
	
	public Team getEnemyFrom() {
		return enemyFrom;
	}
	
	public boolean isPreparingToAttack() {
		return preparingToAttack;
	}

	public void setPreparingToAttack(boolean preparingToAttack) {
		this.preparingToAttack = preparingToAttack;
	}

	public MapLocation getAttackWaitLocation() {
		return attackWaitLocation;
	}

	public void setAttackWaitLocation(MapLocation attackWaitLocation) {
		this.attackWaitLocation = attackWaitLocation;
	}

	public void calculateInitialAttackWaitLocation() {
		if(getHqLocation() != null) {
			attackWaitLocation = getHqLocation().translate(3, 0);
			baseAttackWaitLocation = getHqLocation().translate(3, 0);
			defaultAttackWaitLocation = getHqLocation().translate(3, 0);
		}
	}

	public void proceedToNextAttackWaitLocation() {
		int maxGrid = WAIT_LOCATION_GRID_DIMENSION - 1;
		
		System.out.println("Proceeding from base location " + baseAttackWaitLocation);
		int gridX = baseAttackWaitLocation.x - getHqLocation().x + gridXShift;
		int gridY = getHqLocation().y - baseAttackWaitLocation.y + gridYShift;
		
		baseAttackWaitLocation = baseAttackWaitLocation.add(WAIT_LOCATION_ORDER[gridY][gridX]);
		attackWaitLocation = new MapLocation(baseAttackWaitLocation.x, baseAttackWaitLocation.y);
		
		if(baseAttackWaitLocation.equals(defaultAttackWaitLocation)) {
			attackLocationCycles++;
		}
		
		Direction cardinalOffsetFromHq = Direction.CENTER;
		if(gridX == 0) cardinalOffsetFromHq = Direction.WEST;
		else if(gridX == maxGrid) cardinalOffsetFromHq = Direction.EAST;
		else if(gridY == 0) cardinalOffsetFromHq = Direction.NORTH;
		else if(gridY == maxGrid) cardinalOffsetFromHq = Direction.SOUTH;
		
		System.out.println("Cardinal Offset = " + cardinalOffsetFromHq);
		
		for(int i = 0; i < attackLocationCycles; i++) {
			attackWaitLocation = attackWaitLocation.add(cardinalOffsetFromHq);
		}
		
		System.out.println("Final attack wait location = " + attackWaitLocation);
	}
	
	public void setGridOffset(int dx, int dy) {
		gridXShift = (WAIT_LOCATION_GRID_DIMENSION / 2) + dx;
		gridYShift = (WAIT_LOCATION_GRID_DIMENSION / 2) + dy;
	}
	
	public int getAttackLocationCycles() {
		return attackLocationCycles;
	}

	public boolean isAwaitingKillOrder() {
		return awaitingKillOrder;
	}

	public void setAwaitingKillOrder(boolean awaitingKillOrder) {
		this.awaitingKillOrder = awaitingKillOrder;
	}

	public boolean receivedKillOrder() {
		return receivedKillOrder;
	}

	public void setReceivedKillOrder(boolean receivedKillOrder) {
		this.receivedKillOrder = receivedKillOrder;
	}
	
	public int getKillOrderReceptionRound() {
		return killOrderReceptionRound;
	}

	public void setKillOrderReceptionRound(int killOrderReceptionRound) {
		this.killOrderReceptionRound = killOrderReceptionRound;
	}

	public int getTransactionRound() {
		return transactionRound;
	}

	public void setTransactionRound(int transactionRound) {
		this.transactionRound = transactionRound;
	}
	
	public boolean isWallBuildChecked() {
		return wallBuildChecked;
	}

	public void setWallBuildChecked(boolean wallBuildChecked) {
		this.wallBuildChecked = wallBuildChecked;
	}

	public boolean isWallBuildConfirmed() {
		return wallBuildConfirmed;
	}

	public void setWallBuildConfirmed(boolean wallBuildConfirmed) {
		this.wallBuildConfirmed = wallBuildConfirmed;
	}

	public MapLocation getNextWallSegment() {
		return nextWallSegment;
	}

	public void setNextWallSegment(MapLocation nextWallSegment) {
		this.nextWallSegment = nextWallSegment;
	}
	
	public boolean addOutpostLoc(MapLocation loc) {
		for (MapLocation outpostLoc : outpostLocs) {
			//21 is default sensor radius besides miner and hq.

			if (/*soupLoc.distanceSquaredTo(loc) < 21 || */outpostLoc.equals(loc)) {
				return false;
			}
		}

		for(MapLocation outpostLoc : removedOutpostLocs) {
			if(outpostLoc.equals(loc)) return false;
		}

		outpostLocs.add(loc);
		if(outpostLocs.size() == 1) {
			outpostLocToSearch = loc;
			outpostLocToSearchIndex = 0;
		}
		
		return true;
	}
	
	public boolean removeOutpostLoc(MapLocation loc) {
		boolean removalSuccessful = outpostLocs.remove(loc);
		if(removalSuccessful) removedOutpostLocs.add(loc);
		return removalSuccessful;
	}
	
	public ArrayList<MapLocation> getOutpostLocs() {
		return outpostLocs;
	}

	public MapLocation getOutpostLocToSearch() {
		return outpostLocToSearch;
	}
	
	public void proceedToNextOutpostLoc() {
		outpostLocToSearchIndex++;
		if(outpostLocToSearchIndex >= outpostLocs.size()) outpostLocToSearch = null;
		else outpostLocToSearch = outpostLocs.get(outpostLocToSearchIndex);
	}
	
}
