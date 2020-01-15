package julianbot.robots;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Transaction;
import julianbot.robotdata.HQData;

public class HQ extends Robot {

	private HQData hqData;
	
	public HQ(RobotController rc) {
		super(rc);
		this.data = new HQData(rc, getSpawnerLocation());
		this.hqData = (HQData) this.data;
	}

	@Override
	public void run() throws GameActionException {    	
    	if(rc.getRoundNum() == 1) {
    		makeInitialReport();
    		setBuildDirectionTowardsSoup();
    	}
    	    	
        if(oughtBuildMiner()) {        	
        	tryBuild(RobotType.MINER);
        }
                
        storeForeignTransactions();
        if(rc.getRoundNum() % 100 == 0) repeatForeignTransaction();        
        
        RobotInfo[] enemy = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), hqData.getOpponent());
                
        if(enemy.length > 0) {
        	if(enemy.length > 10) {
	        	sendSOS();
	        }
	        for(RobotInfo bullseye : enemy) { 
	        	if(bullseye.type.equals(RobotType.DESIGN_SCHOOL)) {
	        		//HQCommands.sendSOS(rc);
	        		//GeneralCommands.sendTransaction(rc, 10, Type.TRANSACTION_ENEMY_DESIGN_SCHOOL_AT_LOC, bullseye.location);
	        		hqData.setBuildDirection(rc.getLocation().directionTo(bullseye.location).rotateLeft());
	        		while(!tryBuild(RobotType.MINER)) {
	        			
	        		}
	        	}
	        	
		    	if(rc.canShootUnit(bullseye.getID())) {
		    		shootUnit(bullseye.getID());
		    	}
	        }
        }
	}
	
	private void makeInitialReport() throws GameActionException {
		//Since there can be seven transactions per round, we can be guaranteed to get one message through on the first round if that message is sent with a bid of one more than a seventh of the inital soup cost.
		sendTransaction((GameConstants.INITIAL_SOUP / 7) + 1, Type.TRANSACTION_FRIENDLY_HQ_AT_LOC, rc.getLocation());
	}
	
	private boolean oughtBuildMiner() {
		if(senseUnitType(RobotType.DESIGN_SCHOOL, rc.getTeam()) != null) return false;
		
		return rc.getTeamSoup() >= RobotType.MINER.cost + hqData.getMinersBuilt() * 30 || rc.getRoundNum() == 1;
	}
	
	//DEFENSE
	private void sendSOS() throws GameActionException {
		//Since there can be seven transactions per round, we can be guaranteed to get one message through if that message is sent with a bid of one more than a seventh of the inital soup cost.
		sendTransaction(10, Type.TRANSACTION_SOS_AT_LOC, rc.getLocation());
	}
	
	private void shootUnit(int robotID) throws GameActionException {
		waitUntilReady();
		
		if(rc.canShootUnit(robotID)) {
			rc.shootUnit(robotID);
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
    	Direction buildDirection = hqData.getBuildDirection();
    	
        if (rc.isReady() && rc.canBuildRobot(type, buildDirection)) {
            rc.buildRobot(type, buildDirection);
            hqData.setBuildDirection(buildDirection.rotateRight());
            if(type == RobotType.MINER) hqData.incrementMinersBuilt();
            return true;
        } 
        
        hqData.setBuildDirection(buildDirection.rotateRight());
        return false;
    }
    
    private void setBuildDirectionTowardsSoup() throws GameActionException {
    	MapLocation rcLocation = rc.getLocation();
    	int dimension = (int) Math.ceil(Math.sqrt(RobotType.HQ.sensorRadiusSquared));
    	for(int dx = -dimension; dx <= dimension; dx++) {
    		for(int dy = -dimension; dy <= dimension; dy++) {
    			MapLocation searchLocation = rcLocation.translate(dx, dy);
    			if(rc.canSenseLocation(searchLocation)) {
    				if(rc.senseSoup(searchLocation) > 0) {
    					hqData.setBuildDirection(rcLocation.directionTo(searchLocation));
    					return;
    				}
    			}
    		}
    	}
    }
	
    private void storeForeignTransactions() throws GameActionException {
    	if(rc.getRoundNum() > 1) {
    		Transaction[] block = rc.getBlock(rc.getRoundNum() - 1);
	    	for(Transaction transaction : block) {
	    		if(transaction.getMessage().length > 0) {
	    			hqData.addForeignTransaction(transaction);
	    		}
	    	}
    	}
    }
    
    private void repeatForeignTransaction() throws GameActionException {
    	Transaction interceptedTransaction = hqData.getRandomForeignTransaction();
    	if(interceptedTransaction == null || interceptedTransaction.getMessage().length == 0) return;
    	    	
    	if(rc.canSubmitTransaction(interceptedTransaction.getMessage(), 1)) {
    		rc.submitTransaction(interceptedTransaction.getMessage(), 1);
    	}
    }
	
}
