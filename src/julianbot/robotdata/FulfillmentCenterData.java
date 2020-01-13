package julianbot.robotdata;

import battlecode.common.*;

public class FulfillmentCenterData extends RobotData {
		
	private Direction buildDirection;
	private int dronesBuilt;
	
	private boolean stableSoupIncomeConfirmed;
		private int transactionRound;
		private boolean searchedForVaporator;
	
	public FulfillmentCenterData(RobotController rc) {
		super(rc);
		buildDirection = Direction.NORTH;
		transactionRound = 1;
	}
	
	public int getDronesBuilt() {
		return dronesBuilt;
	}

	public void incrementDronesBuilt() {
		dronesBuilt++;
	}
	
	public void setDronesBuilt(int dronesBuilt) {
		this.dronesBuilt = dronesBuilt;
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
