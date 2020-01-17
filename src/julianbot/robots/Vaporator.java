package julianbot.robots;

import battlecode.common.RobotController;
import julianbot.robotdata.VaporatorData;

public class Vaporator extends Robot {

	public Vaporator(RobotController rc) {
		super(rc);
		this.data = new VaporatorData(rc, getSpawnerLocation());
	}
	
}
