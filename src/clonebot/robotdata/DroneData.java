package clonebot.robotdata;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Team;

public class DroneData extends RobotData {
	
	static Direction[] directions = {Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST};
	
	private Team enemyFrom;
	private boolean holdingEnemy;
	private MapLocation hqLocation;
	private MapLocation enemyHQLocation;
	private MapLocation[] searchDestinations;
		private int activeSearchDestinationIndex;
	
	public DroneData(RobotController rc) {
		super(rc);
		holdingEnemy = false;
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
	
	public MapLocation getEnemyHQLocation() {
		return enemyHQLocation;
	}

	public void setEnemyHQLocation(MapLocation enemyHQLocation) {
		this.enemyHQLocation = enemyHQLocation;
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
	
}
