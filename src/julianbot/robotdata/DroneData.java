package julianbot.robotdata;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Team;

import java.util.ArrayList;

public class DroneData extends RobotData {
	
	//ROUTING
	private MapLocation hqLocation;
	private MapLocation enemyHqLocation;
	private MapLocation[] searchDestinations;
		private int activeSearchDestinationIndex;
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
	
	public MapLocation getHqLocation() {
		return hqLocation;
	}

	public void setHqLocation(MapLocation hqLocation) {
		this.hqLocation = hqLocation;
	}
	
	public MapLocation getEnemyHqLocation() {
		return enemyHqLocation;
	}

	public void setEnemyHqLocation(MapLocation enemyHQLocation) {
		this.enemyHqLocation = enemyHQLocation;
	}

	public boolean searchDestinationsDetermined() {
		return searchDestinations != null;
	}
	
	public void calculateSearchDestinations(RobotController rc) {
		int mapWidth = rc.getMapWidth();
		int mapHeight = rc.getMapHeight();
		
		int centerX = mapWidth / 2;
		int centerY = mapHeight / 2;
		
		MapLocation horizontalSymmetryLocation = new MapLocation(mapWidth - hqLocation.x - 1, hqLocation.y);
		MapLocation verticalSymmetryLocation = new MapLocation(hqLocation.x, mapHeight - hqLocation.y - 1);
		MapLocation rotational90SymmetryLocation = new MapLocation(centerX - (hqLocation.y - centerY), centerY + (hqLocation.x - centerX));
		MapLocation rotational180SymmetryLocation = new MapLocation(centerX - (rotational90SymmetryLocation.y - centerY), centerY + (rotational90SymmetryLocation.x - centerX));
		MapLocation rotational270SymmetryLocation = new MapLocation(centerX - (rotational180SymmetryLocation.y - centerY), centerY + (rotational180SymmetryLocation.x - centerX));
		
		searchDestinations = new MapLocation[] {horizontalSymmetryLocation, verticalSymmetryLocation, rotational180SymmetryLocation, rotational90SymmetryLocation, rotational270SymmetryLocation};
		activeSearchDestinationIndex = 0;
	}
	
	public MapLocation getActiveSearchDestination() {
		return searchDestinations[activeSearchDestinationIndex];
	}
	
	public void proceedToNextSearchDestination() {
		activeSearchDestinationIndex++;
		activeSearchDestinationIndex %= searchDestinations.length;
		System.out.println("Active destination is now " + searchDestinations[activeSearchDestinationIndex]);
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
		if(hqLocation != null) attackWaitLocation = hqLocation.translate(3, 0);
	}

	public void proceedToNextWaitLocation() {
		int gridX = attackWaitLocation.x - hqLocation.x + (WAIT_LOCATION_GRID_DIMENSION / 2);
		int gridY = hqLocation.y - attackWaitLocation.y + (WAIT_LOCATION_GRID_DIMENSION / 2);
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
