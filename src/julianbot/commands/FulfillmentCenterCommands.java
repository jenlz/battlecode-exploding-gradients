package julianbot.commands;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.Transaction;
import julianbot.robotdata.FulfillmentCenterData;

public class FulfillmentCenterCommands {
	
	public static boolean oughtBuildDrone(RobotController rc, FulfillmentCenterData data) {
		if(GeneralCommands.senseNumberOfUnits(rc, RobotType.LANDSCAPER, rc.getTeam()) < data.getDronesBuilt() + 1) return false;
		return (data.isStableSoupIncomeConfirmed()) ? rc.getTeamSoup() >= RobotType.DELIVERY_DRONE.cost + 1 : rc.getTeamSoup() >= RobotType.REFINERY.cost + 5;
	}
	
	/**
     * Attempts to build a given robot in a given direction.
     *
     * @param type The type of the robot to build
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    public static boolean tryBuild(RobotController rc, RobotType type, FulfillmentCenterData data) throws GameActionException {
    	Direction buildDirection = data.getBuildDirection();
    	
    	GeneralCommands.waitUntilReady(rc);
        if (rc.isReady() && rc.canBuildRobot(type, buildDirection)) {
            rc.buildRobot(type, buildDirection);
            if(type == RobotType.DELIVERY_DRONE) data.incrementDronesBuilt();
            return true;
        } 
        
        return false;
    }
    
    public static void confirmStableSoupIncome(RobotController rc, FulfillmentCenterData data) throws GameActionException {
    	if(!data.searchedForVaporator()) {
    		if(GeneralCommands.senseUnitType(rc, RobotType.VAPORATOR, rc.getTeam()) != null) {
    			data.setStableSoupIncomeConfirmed(true);
    		}
    		
    		data.setSearchedForVaporator(true);
    	}
    	
    	for(int i = data.getTransactionRound(); i < rc.getRoundNum(); i++) {
    		for(Transaction transaction : rc.getBlock(i)) {
    			int[] message = GeneralCommands.decodeTransaction(rc, transaction);
    			if(message.length >= 4) {
    				if(message[1] == GeneralCommands.Type.TRANSACTION_FRIENDLY_REFINERY_AT_LOC.getVal()) {
    					data.setStableSoupIncomeConfirmed(true);
    					System.out.println("Stable soup income confirmed!");
    					return;
    				}
    			}
    		}
    		
    		if(Clock.getBytecodesLeft() <= 200) {
    			data.setTransactionRound(i);
    			break;
    		}
    	}
    }
}
