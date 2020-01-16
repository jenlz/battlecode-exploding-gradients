package julianbot.robots;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Transaction;
import julianbot.robotdata.DesignSchoolData;

public class DesignSchool extends Robot {
	
	private DesignSchoolData designSchoolData;

	public DesignSchool(RobotController rc) {
		super(rc);
		this.data = new DesignSchoolData(rc, getSpawnerLocation());
		this.designSchoolData = (DesignSchoolData) this.data;
	}

	@Override
	public void run() throws GameActionException {
		super.run();
		
		if(!designSchoolData.isStableSoupIncomeConfirmed()) confirmStableSoupIncome();
    	if(oughtBuildLandscaper()) tryBuild(RobotType.LANDSCAPER);
    	
    	RobotInfo[] enemy = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), designSchoolData.getOpponent());
        
        if(enemy.length > 0) {
	        for(RobotInfo bullseye : enemy) { 
	        	if(bullseye.type.equals(RobotType.DESIGN_SCHOOL)) {
	        		designSchoolData.setBuildDirection(rc.getLocation().directionTo(bullseye.location).rotateLeft());
	        		while(!tryBuild(RobotType.LANDSCAPER)) {
	        			
	        		}
	        	}
	        }
        }
	}
	
	private boolean oughtBuildLandscaper() {
		//Build a landscaper if the fulfillment center has been built but no landscapers are present.
//		int landscapersPresent = GeneralCommands.senseNumberOfUnits(rc, RobotType.LANDSCAPER, rc.getTeam());
		if (designSchoolData.getLandscapersBuilt() == 0) return senseUnitType(RobotType.FULFILLMENT_CENTER, rc.getTeam()) != null;
		return (designSchoolData.isStableSoupIncomeConfirmed()) ? rc.getTeamSoup() >= RobotType.LANDSCAPER.cost : rc.getTeamSoup() >= RobotType.REFINERY.cost + 5;
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
    	Direction buildDirection = designSchoolData.getBuildDirection();
    	
    	waitUntilReady();
        if (rc.isReady() && rc.canBuildRobot(type, buildDirection)) {
            rc.buildRobot(type, buildDirection);
            if(type == RobotType.LANDSCAPER) designSchoolData.incrementLandscapersBuilt();
            return true;
        } 
        return false;
    }
    
    private void confirmStableSoupIncome() throws GameActionException {
    	if(!designSchoolData.searchedForVaporator()) {
    		if(senseUnitType(RobotType.VAPORATOR, rc.getTeam()) != null) {
    			designSchoolData.setStableSoupIncomeConfirmed(true);
    		}
    		
    		designSchoolData.setSearchedForVaporator(true);
    	}
    	
    	for(int i = designSchoolData.getTransactionRound(); i < rc.getRoundNum(); i++) {
    		for(Transaction transaction : rc.getBlock(i)) {
    			int[] message = decodeTransaction(transaction);
    			if(message.length >= 4) {
    				if(message[1] == Robot.Type.TRANSACTION_FRIENDLY_REFINERY_AT_LOC.getVal()) {
    					designSchoolData.setStableSoupIncomeConfirmed(true);
    					System.out.println("Stable soup income confirmed!");
    					return;
    				}
    			}
    		}
    		
    		if(Clock.getBytecodesLeft() <= 200) {
    			designSchoolData.setTransactionRound(i);
    			break;
    		}
    	}
    }
	
}
