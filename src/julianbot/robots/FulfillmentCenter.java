package julianbot.robots;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
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
		
		if(!fulfillmentCenterData.isStableSoupIncomeConfirmed()) confirmStableSoupIncome();
    	if(oughtBuildDrone()) tryBuild(RobotType.DELIVERY_DRONE);
	}

	private boolean oughtBuildDrone() {
		if(senseNumberOfUnits(RobotType.LANDSCAPER, rc.getTeam()) < fulfillmentCenterData.getDronesBuilt() + 1) return false;
		return (fulfillmentCenterData.isStableSoupIncomeConfirmed()) ? rc.getTeamSoup() >= RobotType.DELIVERY_DRONE.cost + 1 : rc.getTeamSoup() >= RobotType.REFINERY.cost + 5;
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
	
}
