package julianbot.utils.pathfinder;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Pathfinder {

	private MapGraph mapGraph;
	
	
	public Pathfinder() {
		
	}
	
	public void buildGraph(RobotController rc) {
		this.mapGraph = new MapGraph(rc, (int) Math.sqrt(rc.getType().sensorRadiusSquared) - 1);
	}
	
	public Direction[] getRouteTo(MapLocation destination) {
		if(this.mapGraph == null) return new Direction[0];
		return mapGraph.BFS(destination);
	}
	
}
