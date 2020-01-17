package julianbot.robots;

import battlecode.common.RobotController;
import julianbot.robotdata.RefineryData;

public class Refinery extends Robot {

	public Refinery(RobotController rc) {
		super(rc);
		this.data = new RefineryData(rc, getSpawnerLocation());
	}
	
}
