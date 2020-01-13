package julianbot.commands;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Transaction;
import julianbot.commands.GeneralCommands.Type;
import julianbot.robotdata.DroneData;

public class DroneCommands {

	static Direction[] directions = {Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST};
	
	public static void learnHQLocation(RobotController rc, DroneData data) throws GameActionException {
		for(Transaction transaction : rc.getBlock(1)) {
			int[] message = GeneralCommands.decodeTransaction(rc, transaction);
			if(message.length > 1 && message[1] == Type.TRANSACTION_FRIENDLY_HQ_AT_LOC.getVal()) {
				data.setHqLocation(new MapLocation(message[2], message[3]));
				return;
			}
		}
	}
	
	public static void attemptEnemyHQDetection(RobotController rc, DroneData data) {
		RobotInfo enemyHQ = GeneralCommands.senseUnitType(rc, RobotType.HQ, rc.getTeam().opponent());
		if(enemyHQ != null) {
			data.setEnemyHQLocation(enemyHQ.getLocation());
		} else if(rc.canSenseLocation(data.getActiveSearchDestination())){
			data.proceedToNextSearchDestination();
		}
	}
	
	public static boolean pickUpUnit(RobotController rc, DroneData data, RobotType targetType) throws GameActionException {
		RobotInfo info = GeneralCommands.senseUnitType(rc, targetType, rc.getTeam());
		if(info != null) {
			GeneralCommands.waitUntilReady(rc);
			
			if(rc.canPickUpUnit(info.ID)) {
				rc.pickUpUnit(info.ID);
				return true;
			}
		}
		
		return false;
	}
	
	public static boolean dropUnitNextToHQ(RobotController rc, DroneData data) throws GameActionException {
		Direction directionToHQ = rc.getLocation().directionTo(data.getEnemyHQLocation());
		
		GeneralCommands.waitUntilReady(rc);
		if(rc.canDropUnit(directionToHQ.rotateLeft())) {
			rc.dropUnit(directionToHQ.rotateLeft());
			return true;
		} else if(rc.canDropUnit(directionToHQ.rotateRight())) {
			rc.dropUnit(directionToHQ.rotateRight());
			return true;
		}
		
		return false;
	}
	
	public static boolean oughtPickUpUnit(RobotController rc, DroneData data) {
		if(data.getEnemyHQLocation() == null) return true;
		
		MapLocation rcLocation = rc.getLocation();
		return rcLocation.distanceSquaredTo(data.getSpawnerLocation()) < rcLocation.distanceSquaredTo(data.getEnemyHQLocation());
	}
	
	public static void readTransaction(RobotController rc, DroneData data, Transaction[] block) throws GameActionException {

		for (Transaction message : block) {
			int[] decodedMessage = GeneralCommands.decodeTransaction(rc, message);
			if (decodedMessage != new int[] {0}) {
				GeneralCommands.Type category = GeneralCommands.Type.enumOfValue(decodedMessage[1]);
				MapLocation loc = new MapLocation(decodedMessage[2], decodedMessage[3]);

				if (category == null) {
					System.out.println("Something is terribly wrong. enumOfValue returns null");
				}
				//System.out.println("Category of message: " + category);
				switch(category) {
					case TRANSACTION_ENEMY_HQ_AT_LOC:
						data.setEnemyHQLocation(loc);;
						break;
					default:
						break;
				}
			}

		}
	}
}
