package julianbot.robotdata;

import java.util.ArrayList;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
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
	
	//ATTACKS
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
	private int wallOffsetXMin, wallOffsetXMax, wallOffsetYMin, wallOffsetYMax;
	private boolean baseOnEdge;
	
	public DroneData(RobotController rc, MapLocation spawnerLocation) {
		super(rc, spawnerLocation);
		holdingEnemy = false;
		transactionRound = 1;
		floodedLocs = new ArrayList<MapLocation>();
		
		gridXShift = gridYShift = WAIT_LOCATION_GRID_DIMENSION / 2;
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

	public void setEnemyFrom(Team team) {
		enemyFrom = team;
	}
	
	public Team getEnemyFrom() {
		return enemyFrom;
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
