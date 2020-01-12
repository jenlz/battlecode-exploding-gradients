package julianbot.commands;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
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
	private static Direction[][] digPattern = new Direction[][]{
		{Direction.EAST, Direction.SOUTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH},
		{Direction.NORTHEAST, Direction.EAST, Direction.NORTHEAST, Direction.SOUTHEAST, Direction.SOUTH},
		{Direction.NORTH, Direction.NORTHEAST, Direction.NORTH, Direction.SOUTHEAST, Direction.SOUTH},
		{Direction.NORTH, Direction.NORTHWEST, Direction.SOUTHWEST, Direction.WEST, Direction.SOUTHWEST},
		{Direction.NORTH, Direction.NORTHWEST, Direction.WEST, Direction.NORTHWEST, Direction.WEST}
	};
	
	private static Direction[][][] buildPattern = new Direction[][][] {
		{{}, {Direction.WEST, Direction.NORTHWEST, Direction.NORTH, Direction.NORTHEAST}, {}, {Direction.NORTHWEST, Direction.NORTH, Direction.NORTHEAST, Direction.EAST}, {}},
		{{Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST, Direction.NORTH}, {}, {}, {}, {Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST}},
		{{Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST}, {}, {}, {}, {Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST}},
		{{Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST}, {}, {}, {}, {Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH}},
		{{}, {Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST}, {}, {Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST}, {}},
	};
	
	public static boolean dig(RobotController rc, Direction dir) throws GameActionException {
		if(rc.isReady() && rc.canDigDirt(dir)) {
			rc.digDirt(dir);
			return true;
		}
		
		return false;
	}
	
	public static boolean depositDirt(RobotController rc, Direction dir) throws GameActionException {
		if(rc.isReady() && rc.canDepositDirt(dir)) {
			rc.depositDirt(dir);
			return true;
		}
		
		return false;
	}
	
	public static void learnHQLocation(RobotController rc, LandscaperData data) throws GameActionException {
		for(Transaction transaction : rc.getBlock(1)) {
			int[] message = GeneralCommands.decodeTransaction(rc, transaction);
			if(message.length > 1 && message[1] == Type.TRANSACTION_HQ_AT_LOC.getVal()) {
				data.setHqLocation(new MapLocation(message[2], message[3]));
				return;
			}
		}
	}
	
	public static void approachHQ(RobotController rc, LandscaperData data) throws GameActionException {
		if(!data.hasPath()) {
			if(GeneralCommands.move(rc, rc.getLocation().directionTo(data.getHqLocation()), data)) return;
			Direction direction = rc.getLocation().directionTo(data.getHqLocation());    		
    		GeneralCommands.pathfind(rc.getLocation().add(direction).add(direction), rc, data);
		} else {
			GeneralCommands.pathfind(null, rc, data);
		}
	}
	
	public static boolean approachComplete(RobotController rc, LandscaperData data) {
		MapLocation rcLocation = rc.getLocation();
		MapLocation hqLocation = data.getHqLocation();
		
		return Math.abs(rcLocation.x - hqLocation.x) <= 2 && Math.abs(rcLocation.y - hqLocation.y) <= 2;
	}
	
	public static void buildHQWall(RobotController rc, LandscaperData data) throws GameActionException {
		System.out.println("---Building wall---");
		if(rc.getDirtCarrying() > 0) constructWallUnits(rc, data);
		else digWallDirt(rc, data);
		System.out.println("=====");
	}
	
	private static void constructWallUnits(RobotController rc, LandscaperData data) throws GameActionException {
		System.out.println("Constructing!");
		Direction[] constructDirections = new Direction[0];
		
		MapLocation rcLocation = rc.getLocation();
		MapLocation hqLocation = data.getHqLocation();
		
		int dx = rcLocation.x - hqLocation.x;
		int dy = rcLocation.y - hqLocation.y;
				
		int gridX = dx + DIG_PATTERN_ARRAY_SHIFT;
		int gridY = -dy + DIG_PATTERN_ARRAY_SHIFT;
		
		constructDirections = buildPattern[gridY][gridX];
		if(constructDirections.length == 0) {
			System.out.println("Nowhere to build!");
			digOrMove(rc, data, rcLocation, digPattern[gridY][gridX]);
			return;
		}
		
		int[] constructElevations = new int[constructDirections.length];
		for(int i = 0; i < constructElevations.length; i++) {
			constructElevations[i] = rc.senseElevation(rcLocation.add(constructDirections[i]));
		}
		
		LandscaperCommands.depositDirt(rc, constructDirections[NumberMath.indexOfLeast(constructElevations)]);
	}
	
	private static void digWallDirt(RobotController rc, LandscaperData data) throws GameActionException {
		System.out.println("Digging!");
		Direction digDirection = Direction.CENTER;
		
		MapLocation rcLocation = rc.getLocation();
		MapLocation hqLocation = data.getHqLocation();
		
		int dx = rcLocation.x - hqLocation.x;
		int dy = rcLocation.y - hqLocation.y;
		
		int gridX = dx + DIG_PATTERN_ARRAY_SHIFT;
		int gridY = -dy + DIG_PATTERN_ARRAY_SHIFT;
		digDirection = digPattern[gridY][gridX];
		
		System.out.println("Continuing " + digDirection);
		digOrMove(rc, data, rcLocation, digDirection);
	}
	
	private static void digOrMove(RobotController rc, LandscaperData data, MapLocation rcLocation, Direction digDirection) throws GameActionException {
		if(rc.senseElevation(rcLocation) - rc.senseElevation(rcLocation.add(digDirection)) < 1) LandscaperCommands.dig(rc, digDirection);
		else GeneralCommands.move(rc, digDirection, data);
	}
	
	public static boolean buryEnemyHQ(RobotController rc, LandscaperData data) throws GameActionException {
		if(data.getEnemyHQLocation() != null) {
			if(rc.getDirtCarrying() > 0) LandscaperCommands.depositDirt(rc, rc.getLocation().directionTo(data.getEnemyHQLocation()));
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
	
}
