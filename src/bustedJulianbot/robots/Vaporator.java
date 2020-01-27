package bustedJulianbot.robots;

import battlecode.common.RobotController;
import bustedJulianbot.robotdata.VaporatorData;

public class Vaporator extends Robot {

	public Vaporator(RobotController rc) {
		super(rc);
		this.data = new VaporatorData(rc, getSpawnerLocation());
	}
	
}
