package julianbot.commands;

import battlecode.common.*;
import julianbot.robotdata.RobotData;

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
	
	//MOVEMENT
	public static boolean move(RobotController rc, Direction dir) throws GameActionException {
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
