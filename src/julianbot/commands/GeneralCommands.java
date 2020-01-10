package julianbot.commands;

import battlecode.common.*;
import julianbot.robotdata.RobotData;

public class GeneralCommands {
	
	public static final int TRANSACTION_PRIME_NUMBER = 8849;
	
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
	 * @param radius First unit of given type and team. Null if not found.
	 * @return
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
	public static boolean move(RobotController rc, Direction dir) throws GameActionException {
		if(rc.isReady() && rc.canMove(dir)) {
			rc.move(dir);
			return true;
		}
		
		return false;
	}
	
	
	//TRANSACTIONS
	public static int getTransactionTag(int roundNumber) {
		return TRANSACTION_PRIME_NUMBER * roundNumber;
	}
	
	public static void reportLocation(RobotController rc, int soupBid) throws GameActionException {
		int transactionTag = getTransactionTag(rc.getRoundNum());
		int[] message = new int[]{transactionTag, rc.getLocation().x, rc.getLocation().y};
		if(rc.canSubmitTransaction(message, soupBid)) rc.submitTransaction(message, soupBid);
	}
	
	//PATHFINDING
	public static void buildMapGraph(RobotController rc, RobotData data) {
		data.buildMapGraph(rc);
	}
	
	public static void calculatePathTo(MapLocation destination, RobotData data) {
		data.calculatePathTo(destination);
	}
	
	public static void proceedAlongPath(RobotController rc, RobotData data) throws GameActionException {
		if(data.hasPath()) {
			if(GeneralCommands.move(rc, data.getCurrentPathDirection())) {
				Clock.yield();
				data.incrementPathProgression();
			}
		}
	}


	
}
