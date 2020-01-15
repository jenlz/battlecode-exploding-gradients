package clonebot.robotdata;

import battlecode.common.*;

public class DesignSchoolData extends RobotData {
	
	private Direction buildDirection;
	private int landscapersBuilt;
	
	private boolean stableSoupIncomeConfirmed;
		private int transactionRound;
		private boolean searchedForVaporator;
	
	public DesignSchoolData(RobotController rc) {
		super(rc);
		buildDirection = Direction.WEST;
		transactionRound = 1;
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
