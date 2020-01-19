package julianbot.robotdata;

import battlecode.common.*;

public class DesignSchoolData extends RobotData {
	
	private Direction buildDirection;
	private int landscapersBuilt;
	
	private boolean stableSoupIncomeConfirmed;
		private int transactionRound;
		private boolean searchedForVaporator;
	private int pauseBuildTimer;
	private boolean isAttackSchool;
	
	public DesignSchoolData(RobotController rc, MapLocation spawnerLocation) {
		super(rc, spawnerLocation);
		buildDirection = Direction.WEST;
		transactionRound = 1;
		pauseBuildTimer = 0;
		isAttackSchool = false;
	}

	public boolean getIsAttackSchool() {
		return isAttackSchool;
	}

	public void setIsAttackSchool(boolean bool) {
		isAttackSchool = bool;
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
}
