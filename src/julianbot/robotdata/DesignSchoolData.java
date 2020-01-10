package julianbot.robotdata;

import battlecode.common.*;

public class DesignSchoolData extends RobotData {

	private Direction buildDirection;
	
	public DesignSchoolData(RobotController rc) {
		super(rc);
		buildDirection = Direction.NORTH;
	}

	public Direction getBuildDirection() {
		return buildDirection;
	}

	public void setBuildDirection(Direction buildDirection) {
		this.buildDirection = buildDirection;
	}
}
