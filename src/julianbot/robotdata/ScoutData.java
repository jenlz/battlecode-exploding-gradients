package julianbot.robotdata;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class ScoutData extends RobotData {

    private MapLocation hqLocation;
    private MapLocation enemyHqLocation;
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

        int centerX = mapWidth / 2;
        int centerY = mapHeight / 2;

        MapLocation horizontalSymmetryLocation = new MapLocation(mapWidth - hqLocation.x - 1, hqLocation.y);
        MapLocation verticalSymmetryLocation = new MapLocation(hqLocation.x, mapHeight - hqLocation.y - 1);
        MapLocation rotational90SymmetryLocation = new MapLocation(centerX - (hqLocation.y - centerY), centerY + (hqLocation.x - centerX));
        MapLocation rotational180SymmetryLocation = new MapLocation(centerX - (rotational90SymmetryLocation.y - centerY), centerY + (rotational90SymmetryLocation.x - centerX));
        MapLocation rotational270SymmetryLocation = new MapLocation(centerX - (rotational180SymmetryLocation.y - centerY), centerY + (rotational180SymmetryLocation.x - centerX));

        searchDestinations = new MapLocation[] {horizontalSymmetryLocation, rotational180SymmetryLocation, verticalSymmetryLocation, rotational90SymmetryLocation, rotational270SymmetryLocation};
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
