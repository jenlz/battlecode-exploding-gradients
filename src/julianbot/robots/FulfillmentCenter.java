package julianbot.robots;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Transaction;
import julianbot.robotdata.FulfillmentCenterData;

public class FulfillmentCenter extends Robot {

	private FulfillmentCenterData fulfillmentCenterData;
	
	public FulfillmentCenter(RobotController rc) {
		super(rc);
		this.data = new FulfillmentCenterData(rc, getSpawnerLocation());
		this.fulfillmentCenterData = (FulfillmentCenterData) this.data;
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
		
		if(!fulfillmentCenterData.isStableSoupIncomeConfirmed()) confirmStableSoupIncome();
    	if(oughtBuildDrone()) tryBuild(RobotType.DELIVERY_DRONE);
	}
	
	private void learnHQLocation() throws GameActionException {
		for(Transaction transaction : rc.getBlock(1)) {
			int[] message = decodeTransaction(transaction);
			if(message.length > 1 && message[1] == Type.TRANSACTION_FRIENDLY_HQ_AT_LOC.getVal()) {
				fulfillmentCenterData.setHqLocation(new MapLocation(message[2], message[3]));
				return;
			}
		}		
	}

	private boolean oughtBuildDrone() {
		if(fulfillmentCenterData.isStableSoupIncomeConfirmed()) {
			RobotInfo[] robots = rc.senseNearbyRobots(fulfillmentCenterData.getHqLocation(), 3, rc.getTeam());
			for(RobotInfo robot : robots) {
				//A landscaper right next to the HQ is an attack landscaper.
				if(robot.getType() == RobotType.LANDSCAPER) return true;
			}
			
			//Otherwise, we can still produce scouting drones if we need to.
			return (!fulfillmentCenterData.isEnemyHqLocated()) ? rc.getTeamSoup() >= RobotType.DELIVERY_DRONE.cost + 1 : false;
		} else {
			//If a stable soup income is not confirmed, give the miners time to build a refinery before allocating soup to drones.
			return rc.getTeamSoup() >= RobotType.REFINERY.cost + 5;
		}
	}
	
	/**
     * Attempts to build a given robot in a given direction.
     *
     * @param type The type of the robot to build
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    private boolean tryBuild(RobotType type) throws GameActionException {
    	Direction buildDirection = fulfillmentCenterData.getBuildDirection();
    	
    	waitUntilReady();
        if (rc.isReady() && rc.canBuildRobot(type, buildDirection)) {
            rc.buildRobot(type, buildDirection);
            if(type == RobotType.DELIVERY_DRONE) fulfillmentCenterData.incrementDronesBuilt();
            return true;
        } 
        
        return false;
    }
    
    private void confirmStableSoupIncome() throws GameActionException {
    	if(!fulfillmentCenterData.searchedForVaporator()) {
    		if(senseUnitType(RobotType.VAPORATOR, rc.getTeam()) != null) {
    			fulfillmentCenterData.setStableSoupIncomeConfirmed(true);
    		}
    		
    		fulfillmentCenterData.setSearchedForVaporator(true);
    	}
    	
    	for(int i = fulfillmentCenterData.getTransactionRound(); i < rc.getRoundNum(); i++) {
    		for(Transaction transaction : rc.getBlock(i)) {
    			int[] message = decodeTransaction(transaction);
    			if(message.length >= 4) {
    				if(message[1] == Robot.Type.TRANSACTION_FRIENDLY_REFINERY_AT_LOC.getVal()) {
    					fulfillmentCenterData.setStableSoupIncomeConfirmed(true);
    					System.out.println("Stable soup income confirmed!");
    					return;
    				}
    			}
    		}
    		
    		if(Clock.getBytecodesLeft() <= 200) {
    			fulfillmentCenterData.setTransactionRound(i);
    			break;
    		}
    	}
    }
    
    private void readTransaction(Transaction[] block) throws GameActionException {

		for (Transaction message : block) {
			int[] decodedMessage = decodeTransaction(message);
			if (decodedMessage.length == GameConstants.NUMBER_OF_TRANSACTIONS_PER_BLOCK) {
				Robot.Type category = Robot.Type.enumOfValue(decodedMessage[1]);
				MapLocation loc = new MapLocation(decodedMessage[2], decodedMessage[3]);

				//System.out.println("Category of message: " + category);
				switch(category) {
					case TRANSACTION_ENEMY_HQ_AT_LOC:
						fulfillmentCenterData.setEnemyHqLocated(true);
						break;
					default:
						break;
				}
			}

		}
	}
	
}
