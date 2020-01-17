package julianbot.robots;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.Transaction;
import julianbot.robotdata.DroneData;

public class Drone extends Robot {

	private DroneData droneData;
	
	public Drone(RobotController rc) {
		super(rc);
		this.data = new DroneData(rc, getSpawnerLocation());
		this.droneData = (DroneData) data;
	}

	@Override
	public void run() throws GameActionException {
		super.run();
		
		if(turnCount == 1) learnHQLocation();
    	
    	if(turnCount < GameConstants.INITIAL_COOLDOWN_TURNS) {
    		for(int i = (rc.getRoundNum() > 100) ? rc.getRoundNum() - 100 : 1; i < rc.getRoundNum(); i++)
    		readTransaction(rc.getBlock(i));
    	}

    	readTransaction(rc.getBlock(rc.getRoundNum() - 1));
    	
    	if(droneData.getEnemyHQLocation() != null) {
    		
    		if(!rc.isCurrentlyHoldingUnit()) {
        		if(senseUnitType(RobotType.LANDSCAPER,data.getOpponent())!=null && (senseUnitType(RobotType.HQ,data.getTeam())!=null || senseUnitType(RobotType.HQ,data.getOpponent())!=null)) {
        			if(!pickUpUnit(RobotType.LANDSCAPER, data.getOpponent())) {
        				if(senseUnitType(RobotType.HQ, data.getTeam())!=null)
        					routeTo(droneData.getHqLocation());
        				else
        					routeTo(droneData.getEnemyHQLocation());
        			}
        			else {
        				droneData.setHoldingEnemy(true);
        				if(senseUnitType(RobotType.HQ, data.getTeam())!=null)
        					droneData.setEnemyFrom(data.getTeam());
        				else
        					droneData.setEnemyFrom(data.getOpponent());
        			}
        		}
        	}
        	
        	if(droneData.getHoldingEnemy()) {
        		for(Direction d : Direction.allDirections()) {
        			if(rc.senseFlooding(rc.adjacentLocation(d))) {
        				rc.dropUnit(d);
        				droneData.setHoldingEnemy(false);
        				break;
        			}
        		}
        		if(droneData.getHoldingEnemy()) {
        			if(senseUnitType(RobotType.HQ, data.getTeam())!=null)
        				droneData.setEnemyFrom(droneData.getTeam());
					else
						droneData.setEnemyFrom(data.getOpponent());
        			if(droneData.getEnemyFrom().equals(data.getTeam()))
        				routeTo(droneData.getEnemyHQLocation());
        			else
        				routeTo(droneData.getHqLocation());
        		}
        	}
    		
    		if(rc.isCurrentlyHoldingUnit() && !droneData.getHoldingEnemy()) {
    			if(rc.getLocation().isWithinDistanceSquared(droneData.getEnemyHQLocation(), 3)) {
    				dropUnitNextToHQ();
    			} else {
    				routeTo(droneData.getEnemyHQLocation());
    			}
    		} else if (!rc.isCurrentlyHoldingUnit()){
    			boolean oughtPickUpCow = oughtPickUpCow();
    			boolean oughtPickUpLandscaper = oughtPickUpLandscaper();
    			
    			if(senseUnitType(RobotType.COW) != null && oughtPickUpCow) {
	    			if(!pickUpUnit(RobotType.COW)) {
	    				routeTo(droneData.getHqLocation());
	    			}
	    		} else if(oughtPickUpLandscaper) {
	    			if(!pickUpUnit(RobotType.LANDSCAPER, rc.getTeam())) {
	    				routeTo(droneData.getHqLocation());
	    			}
	    		} else if(!rc.getLocation().isWithinDistanceSquared(droneData.getHqLocation(), 3)) {
	    			routeTo(droneData.getSpawnerLocation());
	    		} else {
	    			routeTo(droneData.getEnemyHQLocation());
	    		}
    		}
    	} else {
    		if(!droneData.searchDestinationsDetermined()) {
    			droneData.calculateSearchDestinations(rc);
    		}
    		
    		routeTo(droneData.getActiveSearchDestination());
    		attemptEnemyHQDetection();
    		if(droneData.getEnemyHQLocation() != null) {
    			sendTransaction(10, Robot.Type.TRANSACTION_ENEMY_HQ_AT_LOC, droneData.getEnemyHQLocation());
    		}
    	}
	}
	
	private void learnHQLocation() throws GameActionException {
		for(Transaction transaction : rc.getBlock(1)) {
			int[] message = decodeTransaction(transaction);
			if(message.length > 1 && message[1] == Type.TRANSACTION_FRIENDLY_HQ_AT_LOC.getVal()) {
				droneData.setHqLocation(new MapLocation(message[2], message[3]));
				return;
			}
		}
	}
	
	private void attemptEnemyHQDetection() {
		RobotInfo enemyHQ = senseUnitType(RobotType.HQ, rc.getTeam().opponent());
		if(enemyHQ != null) {
			droneData.setEnemyHQLocation(enemyHQ.getLocation());
		} else if(rc.canSenseLocation(droneData.getActiveSearchDestination())){
			droneData.proceedToNextSearchDestination();
		}
	}
	
	private boolean oughtPickUpCow() {
		//Pick up the unit if we are closer to our own base than our opponent's.
		//This check is just to prevent the drone from moving cows that are already nearer to the opponent's HQ.
		MapLocation rcLocation = rc.getLocation();
		return rcLocation.distanceSquaredTo(droneData.getSpawnerLocation()) < rcLocation.distanceSquaredTo(droneData.getEnemyHQLocation());
	}
	
	private boolean oughtPickUpLandscaper() {		
		//Pick up the unit if we are closer to our own base than our opponent's.
		//This check is just to prevent the drone from dropping of a landscaper, then immediately detecting it and picking it up again.
		//Also, don't pick up landscapers until there is a surplus so our wall doesn't stop rising.
		MapLocation rcLocation = rc.getLocation();
		return rcLocation.distanceSquaredTo(data.getSpawnerLocation()) < rcLocation.distanceSquaredTo(droneData.getEnemyHQLocation())
				&& senseNumberOfUnits(RobotType.LANDSCAPER, rc.getTeam()) > 2;
	}
	
	private boolean pickUpUnit(RobotType targetType) throws GameActionException {
		RobotInfo info = senseUnitType(targetType);
		if(info != null) {
			waitUntilReady();
			
			if(rc.canPickUpUnit(info.ID)) {
				rc.pickUpUnit(info.ID);
				return true;
			}
		}
		
		return false;
	}
	
	private boolean pickUpUnit(RobotType targetType, Team targetTeam) throws GameActionException {
		RobotInfo info = senseUnitType(targetType, targetTeam);
		
		if(info != null) {
			waitUntilReady();
			
			if(rc.canPickUpUnit(info.ID)) {
				rc.pickUpUnit(info.ID);
				return true;
			}
		}
		
		return false;
	}
	
	private boolean dropUnitNextToHQ() throws GameActionException {
		Direction directionToHQ = rc.getLocation().directionTo(droneData.getEnemyHQLocation());
		
		waitUntilReady();
		if(rc.canDropUnit(directionToHQ.rotateLeft())) {
			rc.dropUnit(directionToHQ.rotateLeft());
			return true;
		} else if(rc.canDropUnit(directionToHQ.rotateRight())) {
			rc.dropUnit(directionToHQ.rotateRight());
			return true;
		} else if(rc.canDropUnit(directionToHQ.rotateLeft().rotateLeft())) {
			rc.dropUnit(directionToHQ.rotateLeft().rotateLeft());
			return true;
		} else if(rc.canDropUnit(directionToHQ.rotateRight().rotateRight())) {
			rc.dropUnit(directionToHQ.rotateRight().rotateRight());
			return true;
		}
		
		return false;
	}
	
	private void readTransaction(Transaction[] block) throws GameActionException {

		for (Transaction message : block) {
			int[] decodedMessage = decodeTransaction(message);
			if (!decodedMessage.equals(new int[] {0})) {
				Robot.Type category = Robot.Type.enumOfValue(decodedMessage[1]);
				MapLocation loc = new MapLocation(decodedMessage[2], decodedMessage[3]);

				//System.out.println("Category of message: " + category);
				switch(category) {
					case TRANSACTION_ENEMY_HQ_AT_LOC:
						droneData.setEnemyHQLocation(loc);
						break;
					default:
						break;
				}
			}

		}
	}
	
}
