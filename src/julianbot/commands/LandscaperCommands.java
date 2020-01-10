package julianbot.commands;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Transaction;
import julianbot.robotdata.LandscaperData;
import julianbot.utils.NumberMath;
import julianbot.commands.GeneralCommands.Type;

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
			if(message.length != 1 && message[1] == Type.TRANSACTION_HQ_AT_LOC.getVal()) {
				MapLocation landscaperLocation = rc.getLocation();
				MapLocation origin = landscaperLocation.translate(-landscaperLocation.x, -landscaperLocation.y);
				data.setHqLocation(origin.translate(message[2], message[3]));
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
	
	public static void determineApproachCompletion(RobotController rc, LandscaperData data) {
		if(Math.abs(rc.getLocation().x - data.getHqLocation().x) <= 1 && Math.abs(rc.getLocation().y - data.getHqLocation().y) <= 1) {
			data.setCurrentRole(LandscaperData.DEFEND_HQ_FROM_FLOOD);
		}
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
		
		constructDirections = buildPattern[gridY][gridX];
		if(constructDirections.length == 0) {
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
		Direction digDirection = Direction.CENTER;
		
		MapLocation rcLocation = rc.getLocation();
		MapLocation hqLocation = data.getHqLocation();
		
		int dx = rcLocation.x - hqLocation.x;
		int dy = rcLocation.y - hqLocation.y;
		
		int gridX = dx + DIG_PATTERN_ARRAY_SHIFT;
		int gridY = -dy + DIG_PATTERN_ARRAY_SHIFT;
		digDirection = digPattern[gridY][gridX];
		
		digOrMove(rc, data, rcLocation, digDirection);
	}
	
	private static void digOrMove(RobotController rc, LandscaperData data, MapLocation rcLocation, Direction digDirection) throws GameActionException {
		if(rc.senseElevation(rcLocation) - rc.senseElevation(rcLocation.add(digDirection)) < 1) LandscaperCommands.dig(rc, digDirection);
		else GeneralCommands.move(rc, digDirection, data);
	}
	
}
