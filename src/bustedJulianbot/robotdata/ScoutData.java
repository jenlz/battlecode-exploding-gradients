package bustedJulianbot.robotdata;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class ScoutData extends RobotData {

    protected MapLocation hqLocation;
    protected MapLocation enemyHqLocation;
    private MapLocation[] searchDestinations;
    private int activeSearchDestinationIndex;
    
    public ScoutData(RobotController rc, MapLocation spawnerLocation) {
        super(rc, spawnerLocation);
    }

    public MapLocation getHqLocation() {
        return hqLocation;
    }

    public void setHqLocation(MapLocation hqLocation) {
        this.hqLocation = hqLocation;
    }

    public MapLocation getEnemyHqLocation() {
        return enemyHqLocation;
    }

    public void setEnemyHqLocation(MapLocation enemyHQLocation) {
        this.enemyHqLocation = enemyHQLocation;
    }

    public boolean searchDestinationsDetermined() {
        return searchDestinations != null;
    }

    public void calculateSearchDestinations(RobotController rc) {
        int mapWidth = rc.getMapWidth();
        int mapHeight = rc.getMapHeight();

        MapLocation horizontalSymmetryLocation = new MapLocation(mapWidth - hqLocation.x - 1, hqLocation.y);
        MapLocation verticalSymmetryLocation = new MapLocation(hqLocation.x, mapHeight - hqLocation.y - 1);
        MapLocation rotationalSymmetryLocation = new MapLocation(mapWidth - hqLocation.x - 1, mapHeight - hqLocation.y - 1);

        searchDestinations = new MapLocation[] {horizontalSymmetryLocation, rotationalSymmetryLocation, verticalSymmetryLocation};
        activeSearchDestinationIndex = 0;
    }

    public MapLocation getActiveSearchDestination() {
        return searchDestinations[activeSearchDestinationIndex];
    }

    public void proceedToNextSearchDestination() {
        activeSearchDestinationIndex++;
        activeSearchDestinationIndex %= searchDestinations.length;
        System.out.println("Active destination is now " + searchDestinations[activeSearchDestinationIndex]);
    }

}
