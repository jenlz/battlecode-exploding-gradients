package julianbot.robotdata;

import battlecode.common.*;

public class FulfillmentCenterData extends RobotData {

	private Direction buildDirection;
	private int dronesBuilt;
	
	public FulfillmentCenterData(RobotController rc) {
		super(rc);
		buildDirection = Direction.NORTH;
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
}
