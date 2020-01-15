package julianbot.commands;

import java.util.ArrayList;
import java.util.List;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.Transaction;
import julianbot.robotdata.RobotData;
import julianbot.utils.pathfinder.Pathfinder;

public class GeneralCommands {
	
	static Direction[] directions = {Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST};
	
	public enum Type{
		TRANSACTION_SOS_AT_LOC(-104),
		TRANSACTION_SOUP_AT_LOC(249),
		TRANSACTION_FRIENDLY_HQ_AT_LOC(9),
		TRANSACTION_ENEMY_HQ_AT_LOC(29),
		TRANSACTION_FRIENDLY_REFINERY_AT_LOC(-477),
		TRANSACTION_ENEMY_REFINERY_AT_LOC(-443),
		TRANSACTION_FRIENDLY_DESIGN_SCHOOL_AT_LOC(793), // Not sure if we'd ever need this, but putting it just in case
		TRANSACTION_ENEMY_DESIGN_SCHOOL_AT_LOC(740),
		TRANSACTION_FRIENDLY_FULFILLMENT_CENTER_AT_LOC(117), // Not sure if we'd ever need this, but putting it just in case
		TRANSACTION_ENEMY_FULFILLMENT_CENTER_AT_LOC(177),
		TRANSACTION_ATTACK_AT_LOC(-171);
		
		private int val;
		
		Type(int val){
			this.val = val;
		}
		
		public int getVal() {
			return val;
		}
		
		public static Type enumOfValue(int value) {
		    for (Type e : values()) {
		        if (e.val==value) {
		            return e;
		        }
		    }
			System.out.println("Something is terribly wrong. enumOfValue returns null");
		    return null;
		}
		//Call Type.enumOfValue(plaintxt[1]) to get enum from value
	}
	
	//RECONNAISSANCE
	public static MapLocation getSpawnerLocation(RobotController rc) {
		RobotInfo[] robots = rc.senseNearbyRobots(3, rc.getTeam());
		RobotType targetType = getSpawnerTypeFor(rc.getType());
		
		for(RobotInfo robot : robots) {
			if(robot.type == targetType) return robot.getLocation();
		}
		
		return null;
	}
	
	private static RobotType getSpawnerTypeFor(RobotType type) {
		if(type == RobotType.MINER) return RobotType.HQ;
		if(type == RobotType.REFINERY) return RobotType.MINER;
		if(type == RobotType.LANDSCAPER) return RobotType.DESIGN_SCHOOL;
		return RobotType.HQ;
	}
	
	/**
	 * Senses unit type within sensor radius.
	 * @param rc
	 * @param type
	 * @return First unit of given type. Null if not found
	 */
	public static RobotInfo senseUnitType(RobotController rc, RobotType type) {
		RobotInfo[] robots = rc.senseNearbyRobots();
		for (RobotInfo robot : robots) {
			if (robot.getType() == type) {
				return robot;
			}
		}
		return null;
	}
	
	/**
	 * Senses number of units matching given criteria within sensor radius
	 * @param rc
	 * @param type
	 * @return Number of units of given type
	 */
	public static int senseNumberOfUnits(RobotController rc, RobotType type) {
		int unitCount = 0;
		
		RobotInfo[] robots = rc.senseNearbyRobots();
		for (RobotInfo robot : robots) {
			if (robot.getType() == type) {
				unitCount++;
			}
		}
		
		return unitCount;
	}

	/**
	 * Senses unit type within sensor radius.
	 * @param rc
	 * @param type
	 * @param team
	 * @return First unit of given type and team. Null if not found
	 */
	public static RobotInfo senseUnitType(RobotController rc, RobotType type, Team team) {
		RobotInfo[] robots = rc.senseNearbyRobots(-1, team);
		for (RobotInfo robot : robots) {
			if (robot.getType() == type) {
				return robot;
			}
		}
		return null;
	}
	
	public static RobotInfo[] senseAllUnitsOfType(RobotController rc, RobotType type, Team team) {
		RobotInfo[] robots = rc.senseNearbyRobots(-1, team);
		List<RobotInfo> matchingRobots = new ArrayList<>();
		
		for (RobotInfo robot : robots) {
			if (robot.getType() == type) {
				matchingRobots.add(robot);
			}
		}
		
		RobotInfo[] matchingRobotsArray = new RobotInfo[matchingRobots.size()];
		for(int i = 0; i < matchingRobots.size(); i++) {
			matchingRobotsArray[i] = matchingRobots.get(i);
		}
		
		return matchingRobotsArray;
	}
	
	/**
	 * Senses number of units matching given criteria within sensor radius
	 * @param rc
	 * @param type
	 * @param team
	 * @return Number of units of given type and team
	 */
	public static int senseNumberOfUnits(RobotController rc, RobotType type, Team team) {
		int unitCount = 0;
		
		RobotInfo[] robots = rc.senseNearbyRobots(-1, team);
		for (RobotInfo robot : robots) {
			if (robot.getType() == type) {
				unitCount++;
			}
		}
		
		return unitCount;
	}

	/**
	 * Senses unit type within inputted radius
	 * @param rc
	 * @param type
	 * @param team
	 * @param radius
	 * @return First unit of given type and team. Null if not found.
	 */
	public static RobotInfo senseUnitType(RobotController rc, RobotType type, Team team, int radius) {
		RobotInfo[] robots = rc.senseNearbyRobots(radius, team);
		for (RobotInfo robot : robots) {
			if (robot.getType() == type) {
				return robot;
			}
		}
		return null;
	}
	
	/**
	 * Senses number of units matching given criteria within inputted radius
	 * @param rc
	 * @param type
	 * @param team
	 * @param radius
	 * @return Number of units of given type and team
	 */
	public static int senseNumberOfUnits(RobotController rc, RobotType type, Team team, int radius) {
		int unitCount = 0;
		
		RobotInfo[] robots = rc.senseNearbyRobots(radius, team);
		for (RobotInfo robot : robots) {
			if (robot.getType() == type) {
				unitCount++;
			}
		}
		
		return unitCount;
	}
	
	//TURN MANAGEMENT
	public static void waitUntilReady(RobotController rc) {
		while(!rc.isReady()) {
			Clock.yield();
		}
	}
	
	//MOVEMENT
	public static boolean move(RobotController rc, Direction dir, RobotData data) throws GameActionException {
		stopFollowingPath(data);
		
		waitUntilReady(rc);
		
		if(rc.isReady() && rc.canMove(dir)) {
			data.setPreviousLocation(rc.getLocation());
			rc.move(dir);
			return true;
		}
		
		return false;
	}
	
	private static boolean moveOnPath(RobotController rc, Direction dir, RobotData data) throws GameActionException {
		waitUntilReady(rc);
		
		if(rc.isReady() && rc.canMove(dir)) {
			data.setPreviousLocation(rc.getLocation());
			rc.move(dir);
			return true;
		}
		
		return false;
	}
	
	public static boolean moveAnywhere(RobotController rc, RobotData data) throws GameActionException {
		stopFollowingPath(data);
		
		Direction dir = directions[(int)(Math.random() * directions.length)];
		int rotateLimit = 8;
		
		waitUntilReady(rc);
		
		while (!GeneralCommands.move(rc, dir, data) && rotateLimit > 0) {
			dir.rotateRight();
			rotateLimit--;
		}
		
		if(rotateLimit > 0) data.setPreviousLocation(rc.getLocation().add(dir.opposite()));
		return rotateLimit > 0;
	}	
	
	//TRANSACTIONS
	public static boolean sendTransaction(RobotController rc, int soupBid, Type type, MapLocation loc) throws GameActionException {		
		int transactionTag = (int) (Math.random()*500); //This use of parentheses will prevent truncation of the random number.
		int[] message = new int[]{transactionTag, type.getVal()+transactionTag, loc.x+transactionTag, loc.y+transactionTag, rc.getRoundNum()+transactionTag, 0};
		int odd = 0;
		for (int i : message) {
			if (i%2 == 1)
				odd++;
		}
		message[5] = odd;
		
		if(rc.canSubmitTransaction(message, soupBid)) {
			rc.submitTransaction(message, soupBid);
			return true;
		}
		
		return false;
	}

	/**
	 * Decodes transaction. Returns empty array if message is from enemy team.
	 * @param rc
	 * @param transaction
	 * @return
	 * @throws GameActionException
	 */
	public static int[] decodeTransaction(RobotController rc, Transaction transaction) throws GameActionException {		
		int[] message = transaction.getMessage();
		int transactionTag = message[0];
		int[] plaintxt = new int[6];
		
		int odd = 0;
		for (int i = 0; i<message.length-1; i++) {
			if (message[i]%2 == 1)
				odd++;
		}
		
		if(message.length < 6) return new int[] {0};
		if (odd!=message[5]) return new int[] {0}; //empty means message not from own team
		for (int i = 0; i<message.length-1; i++) {
			plaintxt[i] = message[i] - transactionTag;
		}
		return plaintxt;
	}

	/**
	 * Returns the corresponding Transaction type of locating inputted RobotType
	 * @param rc
	 * @param unitType
	 * @param unitTeam
	 * @return
	 */
	public static Type getLocationType(RobotController rc, RobotType unitType, Team unitTeam) {
		if (unitTeam == rc.getTeam()) {
			switch (unitType) {
				case HQ:
					return Type.TRANSACTION_FRIENDLY_HQ_AT_LOC;
				case REFINERY:
					return Type.TRANSACTION_FRIENDLY_REFINERY_AT_LOC;
				case DESIGN_SCHOOL:
					return Type.TRANSACTION_FRIENDLY_DESIGN_SCHOOL_AT_LOC;
				case FULFILLMENT_CENTER:
					return Type.TRANSACTION_FRIENDLY_FULFILLMENT_CENTER_AT_LOC;
				default:
					return null;
			}
		} else {
			switch (unitType) {
				case HQ:
					return Type.TRANSACTION_ENEMY_HQ_AT_LOC;
				case REFINERY:
					return Type.TRANSACTION_ENEMY_REFINERY_AT_LOC;
				case DESIGN_SCHOOL:
					return Type.TRANSACTION_ENEMY_DESIGN_SCHOOL_AT_LOC;
				case FULFILLMENT_CENTER:
					return Type.TRANSACTION_ENEMY_FULFILLMENT_CENTER_AT_LOC;
				default:
					return null;
			}
		}
	}
	
	public static void sendPendingTransaction(RobotController rc, RobotData data) throws GameActionException {
		if(GeneralCommands.sendTransaction(rc, data.getPendingTransactionSoupBid(), data.getPendingTransactionType(), data.getPendingTransactionLocation())) {
			System.out.println("Submitted pending transaction!");
			data.clearPendingTransaction();
		}
	}
	
	//PATHFINDING
	public static boolean routeTo(MapLocation destination, RobotController rc, RobotData data) throws GameActionException {
		rc.setIndicatorLine(rc.getLocation(), destination, 0, 0, 255);
		//If we're already pathfinding, continue on.
		if(data.hasPath()) {
			return GeneralCommands.pathfind(destination, rc, data);
		}
		
		//Otherwise, simply try to move directly towards the destination.
		MapLocation rcLocation = rc.getLocation();
		Direction initialDirection = rc.getLocation().directionTo(destination);
		if(GeneralCommands.move(rc, initialDirection, data)) return true;
		
		//If this isn't possible, try to move around whatever is blocking us.
		//Directions closer to the destination will be favored.
		Direction initialDirectionLeft = initialDirection.rotateLeft();
		Direction initialDirectionRight = initialDirection.rotateRight();
		
		Direction[] nextDirections = new Direction[4];
		
		if(rcLocation.add(initialDirectionLeft).distanceSquaredTo(destination) < rcLocation.add(initialDirectionRight).distanceSquaredTo(destination)) {
			nextDirections[0] = initialDirectionLeft;
			nextDirections[1] = initialDirectionLeft.rotateLeft();
			nextDirections[2] = initialDirectionRight;
			nextDirections[3] = initialDirectionRight.rotateRight();
		} else {
			nextDirections[0] = initialDirectionRight;
			nextDirections[1] = initialDirectionRight.rotateRight();
			nextDirections[2] = initialDirectionLeft;
			nextDirections[3] = initialDirectionLeft.rotateLeft();
		}
		
		for(Direction direction : nextDirections) {
			if(rcLocation.add(direction).equals(data.getPreviousLocation())) continue;
			if(GeneralCommands.move(rc, direction, data)) return true;
		}
		
		/*
		Direction canMoveDir = initialDirection.rotateRight();
		int rotateLimit = 8;
		while (rotateLimit > 0) {
			if(GeneralCommands.move(rc, canMoveDir, data)) return;
			canMoveDir.rotateRight();
			rotateLimit--;
		}
		*/
		
		//If all of these measures have failed, we'll need to use pathfinding to get around.
		//However, just in case, we will allow for the previous location to be used next turn.
		data.setPreviousLocation(rcLocation);
		return GeneralCommands.pathfind(destination, rc, data);
	}
	
	/**
	 *
	 * @param destination
	 * @param rc
	 * @param data
	 * @return
	 * @throws GameActionException
	 */
	public static boolean pathfind(MapLocation destination, RobotController rc, RobotData data) throws GameActionException {
		if(!data.hasPath() && destination != null) {
			GeneralCommands.calculatePathTo(destination, rc, data);
			return data.hasPath();
		}
		
		return GeneralCommands.proceedAlongPath(rc, data);
	}
	
	private static void calculatePathTo(MapLocation destination, RobotController rc, RobotData data) throws GameActionException {
		data.setMapGraph(Pathfinder.buildMapGraph(rc, rc.getCurrentSensorRadiusSquared()));
		data.setPath(Pathfinder.getRoute(rc.getLocation(), destination, data.getMapGraph()));
		data.setPathProgression(0);
	}
	
	private static boolean proceedAlongPath(RobotController rc, RobotData data) throws GameActionException {
		if(data.hasPath() && GeneralCommands.moveOnPath(rc, data.getNextPathDirection(), data)) {
			data.incrementPathProgression();
			
			if(data.pathCompleted()) {
				stopFollowingPath(data);
			}
			
			return true;
		}
		
		stopFollowingPath(data);
		return false;
	}
	
	public static void stopFollowingPath(RobotData data) {
		data.setPath(null);
		data.setPathProgression(0);
	}

	/**
	 * Returns closest location of array to target location
	 * @param rc
	 * @param locs
	 * @param targetLoc
	 * @return
	 */
	public static MapLocation locateClosestLocation(RobotController rc, ArrayList<MapLocation> locs, MapLocation targetLoc) {
		MapLocation closestLoc = null;
		int closestDistance = Integer.MAX_VALUE;
		for (MapLocation loc : locs) {
			int dist = loc.distanceSquaredTo(targetLoc);
			if (dist < closestDistance) {
				closestLoc = loc;
				closestDistance = dist;
			}
		}
		return closestLoc;
	}
}
