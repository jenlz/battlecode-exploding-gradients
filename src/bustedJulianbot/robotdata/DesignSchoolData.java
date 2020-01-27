package bustedJulianbot.robotdata;

import battlecode.common.*;

public class DesignSchoolData extends RobotData {
	
	private MapLocation hqLocation;
	private MapLocation enemyHqLocation;
	
	private Direction buildDirection;
		private Direction defaultBuildDirection;
		private Direction defaultAttackBuildDirection;
	private boolean buildSitesBlocked;
	
	private int landscapersBuilt;
	
	private boolean stableSoupIncomeConfirmed;
		private int transactionRound;
		private boolean searchedForVaporator;
	
	private boolean waitingOnRefinery;
	private boolean refineryBuilt;
	
	private int pauseBuildTimer;
	private boolean isAttackSchool;
	
	private int currentRole;
	public static final int ROLE_WALL_BUILDER = 0;
	public static final int ROLE_OBSTRUCTION_CLEARER = 1;
	public static final int ROLE_ATTACKER = 2;
	
	public DesignSchoolData(RobotController rc, MapLocation spawnerLocation) {
		super(rc, spawnerLocation);
		buildDirection = Direction.WEST;
			defaultBuildDirection = Direction.WEST;
			defaultAttackBuildDirection = Direction.NORTH;
		
		waitingOnRefinery = true;
		
		transactionRound = 1;
		
		pauseBuildTimer = 0;
		isAttackSchool = false;
	}
	
	public MapLocation getHqLocation() {
		return hqLocation;
	}

	public void setHqLocation(MapLocation hqLocation) {
		this.hqLocation = hqLocation;
	}

	public MapLocation getEnemyHqLocation() {
		return enemyHqLocation;
	}

	public void setEnemyHqLocation(MapLocation enemyHqLocation) {
		this.enemyHqLocation = enemyHqLocation;
	}

	public int getCurrentRole() {
		return currentRole;
	}

	public void setCurrentRole(int currentRole) {
		this.currentRole = currentRole;
	}

	public boolean getIsAttackSchool() {
		return isAttackSchool;
	}

	public void setIsAttackSchool(boolean isAttackSchool) {
		this.isAttackSchool = isAttackSchool;
	}

	public void setPauseBuildTimer(int turns) {
		pauseBuildTimer = turns;
	}

	public int getPauseBuildTimer() {
		return pauseBuildTimer;
	}
	
	public int getLandscapersBuilt() {
		return landscapersBuilt;
	}

	public void incrementLandscapersBuilt() {
		landscapersBuilt++;
	}
	
	public void setLandscapersBuilt(int landscapersBuilt) {
		this.landscapersBuilt = landscapersBuilt;
	}

	public Direction getBuildDirection() {
		return buildDirection;
	}

	public void setBuildDirection(Direction buildDirection) {
		this.buildDirection = buildDirection;
	}
	
	public Direction getDefaultBuildDirection() {
		return defaultBuildDirection;
	}

	public void setDefaultBuildDirection(Direction defaultBuildDirection) {
		this.defaultBuildDirection = defaultBuildDirection;
	}

	public Direction getDefaultAttackBuildDirection() {
		return defaultAttackBuildDirection;
	}

	public void setDefaultAttackBuildDirection(Direction defaultAttackBuildDirection) {
		this.defaultAttackBuildDirection = defaultAttackBuildDirection;
	}
	
	public boolean getBuildSitesBlocked() {
		return buildSitesBlocked;
	}

	public void setBuildSitesBlocked(boolean buildSitesBlocked) {
		this.buildSitesBlocked = buildSitesBlocked;
	}

	public boolean isStableSoupIncomeConfirmed() {
		return stableSoupIncomeConfirmed;
	}

	public void setStableSoupIncomeConfirmed(boolean stableSoupIncomeConfirmed) {
		this.stableSoupIncomeConfirmed = stableSoupIncomeConfirmed;
	}
	
	public int getTransactionRound() {
		return transactionRound;
	}

	public void setTransactionRound(int transactionRound) {
		this.transactionRound = transactionRound;
	}

	public boolean searchedForVaporator() {
		return searchedForVaporator;
	}

	public void setSearchedForVaporator(boolean searchedForVaporator) {
		this.searchedForVaporator = searchedForVaporator;
	}
	
	public boolean isWaitingOnRefinery() {
		return waitingOnRefinery;
	}

	public void setWaitingOnRefinery(boolean waitingOnRefinery) {
		this.waitingOnRefinery = waitingOnRefinery;
	}

	public boolean isRefineryBuilt() {
		return refineryBuilt;
	}

	public void setRefineryBuilt(boolean refineryBuilt) {
		this.refineryBuilt = refineryBuilt;
	}
}
