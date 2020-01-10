package julianbot.utils.pathfinder;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Pathfinder {
	
	public static MapGraph buildMapGraph(RobotController rc) {
		return new MapGraph(rc, (int) Math.sqrt(rc.getType().sensorRadiusSquared) - 1);
	}
	
	public static Direction[] getRoute(MapLocation start, MapLocation destination, MapGraph mapGraph) {
		if(mapGraph == null) return new Direction[0];
		return mapGraph.BFS(start, destination);
	}
	
}
