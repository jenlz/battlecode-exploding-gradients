package julianbot.commands;

import battlecode.common.*;
import julianbot.robotdata.RobotData;
import julianbot.utils.pathfinder.Pathfinder;

public class GeneralCommands {
	
	public enum Type{
		TRANSACTION_SOS_AT_LOC(-104),
		TRANSACTION_SOUP_AT_LOC(249),
		TRANSACTION_HQ_AT_LOC(9),
		TRANSACTION_REFINERY_AT_LOC(-477),
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
	
	
	//TRANSACTIONS
	public static void sendTransaction(RobotController rc, int soupBid, Type type, MapLocation loc) throws GameActionException {
		int transactionTag = (int) Math.random()*500;
		int[] message = new int[]{transactionTag, type.getVal()+transactionTag, loc.x+transactionTag, loc.y+transactionTag, rc.getRoundNum()+transactionTag, 0};
		int odd = 0;
		for (int i : message) {
			if (i%2 == 1)
				odd++;
		}
		message[5] = odd;
		if(rc.canSubmitTransaction(message, soupBid)) rc.submitTransaction(message, soupBid);
	}
	
	public static int[] decodeTransaction(RobotController rc, Transaction transaction) throws GameActionException {
		int[] message = transaction.getMessage();
		int transactionTag = message[0];
		int[] plaintxt = new int[6];
		int odd = 0;
		for (int i = 0; i<message.length-1; i++) {
			if (i%2 == 1)
				odd++;
		}
		if (odd!=message[5])
			return new int[] {0}; //empty means message not from own team
		for (int i = 0; i<message.length-1; i++) {
			plaintxt[i] = message[i] - transactionTag;
		}
		return plaintxt;
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
	
	private static void calculatePathTo(MapLocation destination, RobotController rc, RobotData data) {
		data.setMapGraph(Pathfinder.buildMapGraph(rc));
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
