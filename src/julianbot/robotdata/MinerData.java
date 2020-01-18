package julianbot.robotdata;

import java.util.ArrayList;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class MinerData extends RobotData {

	private int currentRole;
	public static final int ROLE_DESIGN_BUILDER = 0;
	public static final int ROLE_FULFILLMENT_BUILDER = 1;
	public static final int ROLE_SOUP_MINER = 2;
	public static final int ROLE_REFINERY_BUILDER = 3;
	public static final int ROLE_DEFENSE_BUILDER = 4;
	public static final int ROLE_BLOCK = 5;
		private boolean northGunBuilt;
		private boolean designSchoolBuilt;
		private boolean fulfillmentCenterBuilt;
		private boolean southGunBuilt;
	public static final int ROLE_SCOUT = 6;
		private RobotInfo targetRobot;
		private RobotInfo previousTarget;
		private int turnsScouted;
	

	private ArrayList<MapLocation> soupLocs;
		private ArrayList<MapLocation> removedSoupLocs;
	private ArrayList<MapLocation> refineryLocs;

	/**
	 * Constructs MinerData
	 * Initializes role of miner as
	 * @param rc
	 */
	public MinerData(RobotController rc, MapLocation spawnerLocation) {
		super(rc, spawnerLocation);
		currentRole = ROLE_SOUP_MINER;
		soupLocs = new ArrayList<MapLocation>();
			removedSoupLocs = new ArrayList<MapLocation>();
		refineryLocs = new ArrayList<MapLocation>();
		refineryLocs.add(spawnerLocation);
	}
	
	public int getCurrentRole() {
		return currentRole;
	}
	
	public void setCurrentRole(int currentRole) {
		this.currentRole = currentRole;
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

	// Storing Locations

	/**
	 * Returns known locations for soup
	 * @return
	 */
	public ArrayList<MapLocation> getSoupLocs() {
		return soupLocs;
	}

	/**
	 * Returns known locations of allied refineries
	 * @return
	 */
	public ArrayList<MapLocation> getRefineryLocs() {
		return refineryLocs;
	}

	/**
	 * Adds Soup Location if there is no other close Soup Location and it hasn't been added before
	 * @param loc
	 * @return Whether soupLoc is added
	 */
	public boolean addSoupLoc(MapLocation loc) {
		for (MapLocation soupLoc : soupLocs) {
			//21 is default sensor radius besides miner and hq.
			
			if (/*soupLoc.distanceSquaredTo(loc) < 21 || */soupLoc.equals(loc)) {
				return false;
			}
		}
		
		for(MapLocation soupLoc : removedSoupLocs) {
			if(soupLoc.equals(loc)) return false;
		}
		
		soupLocs.add(loc);
		return true;
	}
	
	/**
	 * Adds Refinery Location if not added before
	 * @param loc
	 * @return Whether location successfully sent
	 */
	public boolean addRefineryLoc(MapLocation loc) {
		for (MapLocation refineryLoc : refineryLocs) {
			if (refineryLoc.equals(loc)) {
				return false;
			}
		}
		refineryLocs.add(loc);
		return true;
	}

	/**
	 * Removes specified location from soupLocs if found
	 * @param loc
	 * @return Whether location removed
	 */
	public boolean removeSoupLoc(MapLocation loc) {
		boolean removalSuccessful = soupLocs.remove(loc);
		if(removalSuccessful) removedSoupLocs.add(loc);
		return removalSuccessful;
	}

	/**
	 * Removes specified location from refineryLocs if found
	 * @param loc
	 * @return Whether location removed
	 */
	public boolean removeRefineryLoc(MapLocation loc) {
		return refineryLocs.remove(loc);
	}

}
