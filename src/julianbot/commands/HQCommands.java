package julianbot.commands;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.Transaction;
import julianbot.robotdata.HQData;

public class HQCommands {

	private static final int MINER_COST = 70;
	
	public static void makeInitialReport(RobotController rc) throws GameActionException {
		//Since there can be seven transactions per round, we can be guaranteed to get one message through if that message is sent with a bid of one more than a seventh of the inital soup cost.
		GeneralCommands.reportLocation(rc, (GameConstants.INITIAL_SOUP / 7) + 1);
	}
	
	public static boolean oughtReportLocation(RobotController rc) {
		return rc.getRoundNum() == 1;
	}
	
	public static boolean oughtBuildMiner(RobotController rc) {
		return rc.getTeamSoup() >= GameConstants.INITIAL_SOUP + MINER_COST || rc.getRoundNum() == 1;
	}
	
	/**
     * Attempts to build a given robot in a given direction.
     *
     * @param type The type of the robot to build
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    public static boolean tryBuild(RobotController rc, RobotType type, HQData data) throws GameActionException {
    	Direction buildDirection = data.getBuildDirection();
    	
        if (rc.isReady() && rc.canBuildRobot(type, buildDirection)) {
            rc.buildRobot(type, buildDirection);
            data.setBuildDirection(buildDirection.rotateRight());
            return true;
        } 
        
        data.setBuildDirection(buildDirection.rotateRight());
        return false;
    }
	
    public static void storeForeignTransactions(RobotController rc, HQData data) throws GameActionException {
    	if(rc.getRoundNum() > 1) {
    		Transaction[] block = rc.getBlock(rc.getRoundNum() - 1);
	    	for(Transaction transaction : block) {
	    		if(transaction.getMessage().length > 0) {
	    			data.addForeignTransaction(transaction);
	    		}
	    	}
    	}
    }
    
    public static void repeatForeignTransaction(RobotController rc, HQData data) throws GameActionException {
    	Transaction interceptedTransaction = data.getRandomForeignTransaction();
    	if(interceptedTransaction == null || interceptedTransaction.getMessage().length == 0) return;
    	
    	System.out.println("Attempting to wreak havoc.");
    	
    	if(rc.canSubmitTransaction(interceptedTransaction.getMessage(), 10)) {
    		rc.submitTransaction(interceptedTransaction.getMessage(), 10);
    	}
    }
}
