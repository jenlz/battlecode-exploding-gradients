package julianbot.commands;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import julianbot.robotdata.RobotData;
import julianbot.utils.pathfinder.Pathfinder;

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
	public static int getTransactionTag(int roundNumber) {
		return TRANSACTION_PRIME_NUMBER * roundNumber;
	}
	
	public static void reportLocation(RobotController rc, int soupBid) throws GameActionException {
		int transactionTag = getTransactionTag(rc.getRoundNum());
		int[] message = new int[]{transactionTag, rc.getLocation().x, rc.getLocation().y};
		if(rc.canSubmitTransaction(message, soupBid)) rc.submitTransaction(message, soupBid);
	}
	
	//PATHFINDING
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
