package julianbot.commands;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Transaction;
import julianbot.commands.GeneralCommands.Type;
import julianbot.robotdata.LandscaperData;
import julianbot.utils.NumberMath;

public class LandscaperCommands {
	
	private static final int DIG_PATTERN_ARRAY_SHIFT = 2;
	private static Direction[][] movePattern = new Direction[][]{
		{Direction.EAST, Direction.EAST, Direction.EAST, Direction.EAST, Direction.SOUTH},
		{Direction.NORTH, Direction.NORTH, Direction.NORTH, Direction.EAST, Direction.SOUTH},
		{Direction.NORTH, Direction.WEST, Direction.NORTH, Direction.EAST, Direction.SOUTH},
		{Direction.NORTH, Direction.NORTH, Direction.SOUTH, Direction.SOUTH, Direction.SOUTH},
		{Direction.NORTH, Direction.WEST, Direction.WEST, Direction.WEST, Direction.WEST}
	};
	
	private static Direction[][] digPattern = new Direction[][]{
		{Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTH, Direction.SOUTH, Direction.SOUTHWEST},
		{Direction.EAST, null, null, null, Direction.WEST},
		{Direction.NORTHEAST, null, null, null, Direction.SOUTHWEST},
		{Direction.EAST, null, null, null, Direction.WEST},
		{Direction.NORTHEAST, Direction.NORTH, Direction.NORTH, Direction.NORTH, Direction.NORTHWEST}
	};
	
	private static Direction[][][] buildPattern = new Direction[][][] {
		{{Direction.CENTER}, {Direction.CENTER}, {Direction.CENTER}, {Direction.CENTER}, {Direction.CENTER}},
		{{Direction.CENTER}, {}, {}, {}, {Direction.CENTER}},
		{{Direction.CENTER}, {}, {}, {}, {Direction.CENTER}},
		{{Direction.CENTER}, {}, {}, {}, {Direction.CENTER}},
		{{Direction.CENTER}, {Direction.CENTER}, {Direction.CENTER}, {Direction.CENTER}, {Direction.CENTER}},
	};
	
	public static boolean dig(RobotController rc, Direction dir) throws GameActionException {
		GeneralCommands.waitUntilReady(rc);
		if(rc.isReady() && rc.canDigDirt(dir)) {
			rc.digDirt(dir);
			return true;
		}
		
		return false;
	}
	
	public static boolean depositDirt(RobotController rc, Direction dir) throws GameActionException {
		GeneralCommands.waitUntilReady(rc);
		if(rc.isReady() && rc.canDepositDirt(dir)) {
			rc.depositDirt(dir);
			return true;
		}
		
		return false;
	}
	
	public static void learnHQLocation(RobotController rc, LandscaperData data) throws GameActionException {
		for(Transaction transaction : rc.getBlock(1)) {
			int[] message = GeneralCommands.decodeTransaction(rc, transaction);
			if(message.length > 1 && message[1] == Type.TRANSACTION_FRIENDLY_HQ_AT_LOC.getVal()) {
				data.setHqLocation(new MapLocation(message[2], message[3]));
				return;
			}
		}
	}
	
	public static boolean approachComplete(RobotController rc, LandscaperData data) {
		MapLocation rcLocation = rc.getLocation();
		MapLocation hqLocation = data.getHqLocation();
		
		return Math.abs(rcLocation.x - hqLocation.x) <= 2 && Math.abs(rcLocation.y - hqLocation.y) <= 2;
	}
	
	public static void buildHQWall(RobotController rc, LandscaperData data) throws GameActionException {
		if(rc.getDirtCarrying() > 0) constructWallUnits(rc, data);
		else digWallDirt(rc, data);
	}
	
	private static void constructWallUnits(RobotController rc, LandscaperData data) throws GameActionException {
		Direction[] constructDirections = new Direction[0];
		
		MapLocation rcLocation = rc.getLocation();
		MapLocation hqLocation = data.getHqLocation();
		
		int dx = rcLocation.x - hqLocation.x;
		int dy = rcLocation.y - hqLocation.y;
				
		int gridX = dx + DIG_PATTERN_ARRAY_SHIFT;
		int gridY = -dy + DIG_PATTERN_ARRAY_SHIFT;
		
		//If where we're going is too low, deposit dirt there.
		if(rc.senseElevation(rcLocation) - rc.senseElevation(rcLocation.add(movePattern[gridY][gridX])) > GameConstants.MAX_DIRT_DIFFERENCE) {
			LandscaperCommands.depositDirt(rc, movePattern[gridY][gridX]);
			return;
		}
		
		constructDirections = buildPattern[gridY][gridX];
		if(constructDirections.length == 0) {
			GeneralCommands.move(rc, movePattern[gridY][gridX], data);
			return;
		}
		
		int[] constructElevations = new int[constructDirections.length];
		for(int i = 0; i < constructElevations.length; i++) {
			constructElevations[i] = rc.senseElevation(rcLocation.add(constructDirections[i]));
		}
		
		LandscaperCommands.depositDirt(rc, constructDirections[NumberMath.indexOfLeast(constructElevations)]);
	}
	
	private static void digWallDirt(RobotController rc, LandscaperData data) throws GameActionException {
		Direction digDirection = null;
		
		MapLocation rcLocation = rc.getLocation();
		MapLocation hqLocation = data.getHqLocation();
		
		int dx = rcLocation.x - hqLocation.x;
		int dy = rcLocation.y - hqLocation.y;
		
		int gridX = dx + DIG_PATTERN_ARRAY_SHIFT;
		int gridY = -dy + DIG_PATTERN_ARRAY_SHIFT;
		
		//If where we're going is too high, dig from there.
		if(rc.senseElevation(rcLocation.add(movePattern[gridY][gridX])) - rc.senseElevation(rcLocation) > GameConstants.MAX_DIRT_DIFFERENCE) {
			LandscaperCommands.dig(rc, movePattern[gridY][gridX]);
			return;
		}
		
		digDirection = digPattern[gridY][gridX];
		
		if(digDirection != null) LandscaperCommands.dig(rc, digDirection);
		GeneralCommands.move(rc, movePattern[gridY][gridX], data);
	}
	
	public static boolean buryEnemyHQ(RobotController rc, LandscaperData data) throws GameActionException {
		if(data.getEnemyHQLocation() != null) {
			
			if(!rc.getLocation().isWithinDistanceSquared(data.getEnemyHQLocation(), 3)) GeneralCommands.routeTo(data.getEnemyHQLocation(), rc, data);
			else if(rc.getDirtCarrying() > 0) LandscaperCommands.depositDirt(rc, rc.getLocation().directionTo(data.getEnemyHQLocation()));
			else LandscaperCommands.dig(rc, data.getEnemyHQBuryDigDirection());
			
			return true;
		}
		
		RobotInfo enemyHQ = GeneralCommands.senseUnitType(rc, RobotType.HQ, rc.getTeam().opponent());
		if(enemyHQ != null) {
			data.setEnemyHQLocation(enemyHQ.getLocation());
			determineDigDirection(rc, data);
			return true;
		}
		
		return false;
	}
	
	private static void determineDigDirection(RobotController rc, LandscaperData data) {
		Direction enemyHQDirection = rc.getLocation().directionTo(data.getEnemyHQLocation());
		if(rc.canDigDirt(enemyHQDirection.rotateLeft())) data.setEnemyHQBuryDigDirection(enemyHQDirection.rotateLeft());
		else if(rc.canDigDirt(enemyHQDirection.rotateRight())) data.setEnemyHQBuryDigDirection(enemyHQDirection.rotateRight());
		else if(rc.canDigDirt(enemyHQDirection.rotateLeft().rotateLeft())) data.setEnemyHQBuryDigDirection(enemyHQDirection.rotateLeft().rotateLeft());
		else if(rc.canDigDirt(enemyHQDirection.rotateRight().rotateRight())) data.setEnemyHQBuryDigDirection(enemyHQDirection.rotateRight().rotateRight());
		else data.setEnemyHQBuryDigDirection(enemyHQDirection);
	}

	public static void buryEnemyDesign(RobotController rc, LandscaperData data, RobotInfo enemy) throws GameActionException {
		if(rc.getDirtCarrying() > 0) LandscaperCommands.depositDirt(rc, rc.getLocation().directionTo(enemy.location));
		else LandscaperCommands.dig(rc, rc.getLocation().directionTo(data.getHqLocation()));
	}
	
}
