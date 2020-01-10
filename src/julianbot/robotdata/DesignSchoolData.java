package julianbot.robotdata;

import battlecode.common.Direction;
import battlecode.common.RobotController;

public class DesignSchoolData extends RobotData {

	private Direction buildDirection;
	private int landscapersBuilt;
	
	public DesignSchoolData(RobotController rc) {
		super(rc);
		buildDirection = Direction.NORTH;
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
	
}
