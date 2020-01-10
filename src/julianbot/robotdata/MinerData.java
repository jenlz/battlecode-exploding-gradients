package julianbot.robotdata;

import battlecode.common.Direction;
import battlecode.common.RobotController;

public class MinerData extends RobotData {

	private int currentRole;
	public static final int ROLE_DESIGN_BUILDER = 0;
	public static final int ROLE_SOUP_MINER = 1;
	public static final int ROLE_SCOUT = 2;
	
	private Direction searchDirection;

	/**
	 * Constructs MinerData
	 * Initializes role of miner as
	 * @param rc
	 */
	public MinerData(RobotController rc) {
		super(rc);
		currentRole = ROLE_DESIGN_BUILDER;
		searchDirection = spawnerLocation.directionTo(rc.getLocation());
	}
	
	public int getCurrentRole() {
		return currentRole;
	}
	
	public void setCurrentRole(int currentRole) {
		this.currentRole = currentRole;
	}

	public Direction getSearchDirection() {
		return searchDirection;
	}

	public void setSearchDirection(Direction searchDirection) {
		this.searchDirection = searchDirection;
	}
	
}
