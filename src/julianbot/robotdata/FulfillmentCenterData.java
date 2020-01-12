package julianbot.robotdata;

import battlecode.common.*;

public class FulfillmentCenterData extends RobotData {

	private Direction buildDirection;
	private int dronesBuilt;
	
	public FulfillmentCenterData(RobotController rc) {
		super(rc);
		buildDirection = Direction.NORTH;
	}
	
	public int getLandscapersBuilt() {
		return dronesBuilt;
	}

	public void incrementLandscapersBuilt() {
		dronesBuilt++;
	}
	
	public void setLandscapersBuilt(int landscapersBuilt) {
		this.dronesBuilt = landscapersBuilt;
	}

	public Direction getBuildDirection() {
		return buildDirection;
	}

	public void setBuildDirection(Direction buildDirection) {
		this.buildDirection = buildDirection;
	}
}
