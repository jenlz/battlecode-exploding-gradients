package bustedJulianbot.robots;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import bustedJulianbot.robotdata.NetGunData;

public class NetGun extends Robot {

	private NetGunData netGunData;
	
	public NetGun(RobotController rc) {
		super(rc);
		this.data = new NetGunData(rc, getSpawnerLocation());
		this.netGunData = (NetGunData) this.data;
	}

	@Override
	public void run() throws GameActionException {
		super.run();
		
    	RobotInfo[] enemy = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), netGunData.getOpponent());
    	if(enemy.length > 0) {
    		for (RobotInfo target : enemy) {
    			if(rc.canShootUnit(target.getID())) {
    				rc.shootUnit(target.getID());
    			}
    		}
    	}
	}
	
}
