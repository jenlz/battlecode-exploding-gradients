package bustedJulianbot.robots;

import battlecode.common.RobotController;
import bustedJulianbot.robotdata.RefineryData;

public class Refinery extends Robot {

	public Refinery(RobotController rc) {
		super(rc);
		this.data = new RefineryData(rc, getSpawnerLocation());
	}
	
}
