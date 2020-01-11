package julianbot.robotdata;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class MinerData extends RobotData {

	private int currentRole;
	public static final int ROLE_DESIGN_BUILDER = 0;
	public static final int ROLE_SOUP_MINER = 1;
	public static final int ROLE_DEFENSE_BUILDER = 2;
		private boolean northGunBuilt;
		private boolean designSchoolBuilt;
		private boolean fulfillmentCenterBuilt;
		private boolean southGunBuilt;
	public static final int ROLE_SCOUT = 3;
		private RobotInfo targetRobot;
		private RobotInfo previousTarget;
		private int turnsScouted;
	
	private Direction searchDirection;

	/**
	 * Constructs MinerData
	 * Initializes role of miner as
	 * @param rc
	 */
	public MinerData(RobotController rc) {
		super(rc);
		currentRole = ROLE_SCOUT;
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

	public boolean isNorthGunBuilt() {
		return northGunBuilt;
	}

	public void setNorthGunBuilt(boolean northGunBuilt) {
		this.northGunBuilt = northGunBuilt;
	}

	public boolean isDesignSchoolBuilt() {
		return designSchoolBuilt;
	}

	public void setDesignSchoolBuilt(boolean designSchoolBuilt) {
		this.designSchoolBuilt = designSchoolBuilt;
	}

	public boolean isFulfillmentCenterBuilt() {
		return fulfillmentCenterBuilt;
	}

	public void setFulfillmentCenterBuilt(boolean fulfillmentCenterBuilt) {
		this.fulfillmentCenterBuilt = fulfillmentCenterBuilt;
	}

	public boolean isSouthGunBuilt() {
		return southGunBuilt;
	}

	public void setSouthGunBuilt(boolean southGunBuilt) {
		this.southGunBuilt = southGunBuilt;
	}

	// Scout

	/**
	 * Returns target robot
	 * @return Target robot scout is monitoring
	 */
	public RobotInfo getTargetRobot() {
		return this.targetRobot;
	}

	/**
	 * Sets target robot
	 * @param robot Robot to monitor
	 */
	public void setTargetRobot(RobotInfo robot) {
		this.targetRobot = robot;
	}

	/**
	 * Gets target before current target to avoid following again
	 * @return
	 */
	public RobotInfo getPreviousTarget() {
		return this.previousTarget;
	}

	/**
	 * Sets target before current target to avoid following again
	 * @param robot
	 */
	public void setPreviousTarget(RobotInfo robot) {
		this.previousTarget = robot;
	}

	/**
	 * Returns number of turns scouting a specific unit
	 * @return
	 */
	public int getTurnsScouted() {
		return this.turnsScouted;
	}

	/**
	 * Increments turnsScouted by one
	 */
	public void incrementTurnsScouted() {
		turnsScouted++;
	}

	/**
	 * Resets turnsScouted back to 0. Used when changing target to follow
	 */
	public void resetTurnsScouted() {
		this.turnsScouted = 0;
	}

}
