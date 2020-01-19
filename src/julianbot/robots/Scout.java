package julianbot.robots;

import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import julianbot.robotdata.ScoutData;

public class Scout extends Robot {

    protected ScoutData scoutData;

    public Scout(RobotController rc) {
        super(rc);
    }

    public void attemptEnemyHQDetection() {
        RobotInfo enemyHQ = senseUnitType(RobotType.HQ, rc.getTeam().opponent());
        if(enemyHQ != null) {
            scoutData.setEnemyHqLocation(enemyHQ.getLocation());
        } else if(rc.canSenseLocation(scoutData.getActiveSearchDestination())){
            scoutData.proceedToNextSearchDestination();
        }
    }
}
