package julianbot.commands;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
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
	
	public static boolean oughtPickUpCow(RobotController rc, DroneData data) {
		//Pick up the unit if we are closer to our own base than our opponent's.
		//This check is just to prevent the drone from moving cows that are already nearer to the opponent's HQ.
		MapLocation rcLocation = rc.getLocation();
		return rcLocation.distanceSquaredTo(data.getSpawnerLocation()) < rcLocation.distanceSquaredTo(data.getEnemyHQLocation());
	}
	
	public static boolean oughtPickUpLandscaper(RobotController rc, DroneData data) {		
		//Pick up the unit if we are closer to our own base than our opponent's.
		//This check is just to prevent the drone from dropping of a landscaper, then immediately detecting it and picking it up again.
		//Also, don't pick up landscapers until there is a surplus so our wall doesn't stop rising.
		MapLocation rcLocation = rc.getLocation();
		return rcLocation.distanceSquaredTo(data.getSpawnerLocation()) < rcLocation.distanceSquaredTo(data.getEnemyHQLocation())
				&& GeneralCommands.senseNumberOfUnits(rc, RobotType.LANDSCAPER, rc.getTeam()) > 1;
	}
	
	public static boolean pickUpUnit(RobotController rc, DroneData data, RobotType targetType) throws GameActionException {
		RobotInfo info = GeneralCommands.senseUnitType(rc, targetType);
		if(info != null) {
			GeneralCommands.waitUntilReady(rc);
			
			if(rc.canPickUpUnit(info.ID)) {
				rc.pickUpUnit(info.ID);
				return true;
			}
		}
		
		return false;
	}
	
	public static boolean pickUpUnit(RobotController rc, DroneData data, RobotType targetType, Team targetTeam) throws GameActionException {
		RobotInfo info = GeneralCommands.senseUnitType(rc, targetType, targetTeam);
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
