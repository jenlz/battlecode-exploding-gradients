package julianbot.commands;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import julianbot.robotdata.FulfillmentCenterData;

public class FulfillmentCenterCommands {
	
	public static boolean oughtBuildDrone(RobotController rc, FulfillmentCenterData data) {
		return rc.getTeamSoup() >= RobotType.DELIVERY_DRONE.cost + data.getDronesBuilt() * Math.abs(RobotType.LANDSCAPER.cost - RobotType.DELIVERY_DRONE.cost) + 1;
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
}
