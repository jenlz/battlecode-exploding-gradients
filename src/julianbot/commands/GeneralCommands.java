package julianbot.commands;

import battlecode.common.*;
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
	
	//MOVEMENT
	public static boolean move(RobotController rc, Direction dir, RobotData data) throws GameActionException {
		stopFollowingPath(data);
		
		if(rc.isReady() && rc.canMove(dir)) {
			rc.move(dir);
			return true;
		}
		
		return false;
	}
	
	private static boolean moveOnPath(RobotController rc, Direction dir) throws GameActionException {
		if(rc.isReady() && rc.canMove(dir)) {
			rc.move(dir);
			return true;
		}
		
		return false;
	}
	
	public static boolean moveAnywhere(RobotController rc, RobotData data) throws GameActionException {
		stopFollowingPath(data);
		
		Direction dir = directions[(int)(Math.random() * directions.length)];
		int rotateLimit = 8;
		
		while (!GeneralCommands.move(rc, dir, data) && rotateLimit > 0) {
			dir.rotateRight();
			rotateLimit--;
		}
		
		return rotateLimit > 0;
	}	
	
	//TRANSACTIONS
	public static void sendTransaction(RobotController rc, int soupBid, Type type, MapLocation loc) throws GameActionException {
		int transactionTag = (int) (Math.random()*500); //This use of parentheses will prevent truncation of the random number.
		int[] message = new int[]{transactionTag, type.getVal()+transactionTag, loc.x+transactionTag, loc.y+transactionTag, rc.getRoundNum()+transactionTag, 0};
		int odd = 0;
		for (int i : message) {
			if (i%2 == 1)
				odd++;
		}
		message[5] = odd;
		if(rc.canSubmitTransaction(message, soupBid)) rc.submitTransaction(message, soupBid);
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
		if (odd!=message[5])
			return new int[] {0}; //empty means message not from own team
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

	public static Type getTypeFromVal(RobotController rc, int value) {
		Type[] categories = Type.values();
		for (Type category : categories) {
			if (category.getVal() == value) {
				return category;
			}
		}
		return null;
	}
	
	//PATHFINDING

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
			return false;
		}
		
		return GeneralCommands.proceedAlongPath(rc, data);
	}
	
	private static void calculatePathTo(MapLocation destination, RobotController rc, RobotData data) throws GameActionException {
		data.setMapGraph(Pathfinder.buildMapGraph(rc, rc.getCurrentSensorRadiusSquared()));
		data.setPath(Pathfinder.getRoute(rc.getLocation(), destination, data.getMapGraph()));
		data.setPathProgression(0);
	}
	
	private static boolean proceedAlongPath(RobotController rc, RobotData data) throws GameActionException {
		if(data.hasPath() && GeneralCommands.moveOnPath(rc, data.getNextPathDirection())) {
			data.incrementPathProgression();
			if(data.pathCompleted()) {
				stopFollowingPath(data);
				return true;
			}
		}
		
		return false;
	}
	
	public static void stopFollowingPath(RobotData data) {
		data.setPath(null);
		data.setPathProgression(0);
	}
}
