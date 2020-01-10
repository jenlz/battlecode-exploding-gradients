package julianbot.robotdata;

import battlecode.common.Direction;
import battlecode.common.RobotController;

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
	
}
