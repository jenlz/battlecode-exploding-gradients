package julianbot.robotdata;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Team;

import java.util.ArrayList;

public class DroneData extends ScoutData {
	
	//ROUTING
	private ArrayList<MapLocation> floodedLocs;
	
	//TRANSACTION SEARCHING
	private int transactionRound;
	
	//CARGO
	private Team enemyFrom;
	private boolean holdingEnemy;
	
	//ATTACKS
	private MapLocation attackWaitLocation;
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
	
	//WALL STATUS
	private boolean wallBuildChecked;
	private boolean wallBuildConfirmed;
	private MapLocation nextWallSegment;
	
	public DroneData(RobotController rc, MapLocation spawnerLocation) {
		super(rc, spawnerLocation);
		holdingEnemy = false;
		transactionRound = 1;
		floodedLocs = new ArrayList<MapLocation>();
		nextWallSegment = new MapLocation(0, 0);
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
		if(getHqLocation() != null) attackWaitLocation = getHqLocation().translate(3, 0);
	}

	public void proceedToNextWaitLocation() {
		int gridX = attackWaitLocation.x - getHqLocation().x + (WAIT_LOCATION_GRID_DIMENSION / 2);
		int gridY = getHqLocation().y - attackWaitLocation.y + (WAIT_LOCATION_GRID_DIMENSION / 2);
		attackWaitLocation = attackWaitLocation.add(WAIT_LOCATION_ORDER[gridY][gridX]);
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
	
}
