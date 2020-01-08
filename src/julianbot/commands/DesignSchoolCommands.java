package julianbot.commands;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import julianbot.robotdata.DesignSchoolData;

public class DesignSchoolCommands {

	private static final int LANDSCAPER_COST = 150;
	
	public static boolean oughtBuildLandscaper(RobotController rc) {
		return rc.getTeamSoup() >= LANDSCAPER_COST;
	}
	
	/**
     * Attempts to build a given robot in a given direction.
     *
     * @param type The type of the robot to build
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    public static boolean tryBuild(RobotController rc, RobotType type, DesignSchoolData data) throws GameActionException {
    	Direction buildDirection = data.getBuildDirection();
    	
        if (rc.isReady() && rc.canBuildRobot(type, buildDirection)) {
            rc.buildRobot(type, buildDirection);
            data.setBuildDirection(buildDirection.rotateRight());
            return true;
        } 
        
        data.setBuildDirection(buildDirection.rotateRight());
        return false;
    }
}
