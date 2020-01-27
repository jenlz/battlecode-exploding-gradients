package bustedJulianbot.utils.pathfinder;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Pathfinder {
	
	public static MapGraph buildMapGraph(RobotController rc, int radiusSquared) throws GameActionException {
		MapGraph mapGraph = new MapGraph(radiusSquared);
		mapGraph.connectEdges(rc);
		return mapGraph;
	}
	
	public static Direction[] getRoute(MapLocation start, MapLocation destination, MapGraph mapGraph) {
		if(mapGraph == null) return new Direction[0];
		return mapGraph.BFS(start, destination);
	}
	
}
