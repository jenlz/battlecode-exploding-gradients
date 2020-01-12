package julianbot.utils.pathfinder;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Pathfinder {
	
	public static MapGraph buildMapGraph(RobotController rc, int radiusSquared) throws GameActionException {
		return new MapGraph(rc, radiusSquared);
	}
	
	public static Direction[] getRoute(MapLocation start, MapLocation destination, MapGraph mapGraph) {
		if(mapGraph == null) return new Direction[0];
		return mapGraph.BFS(start, destination);
	}
	
}
