package julianbot.utils.pathfinder;

import battlecode.common.MapLocation;

public class MapGraphNode {

	private MapLocation location;
	private MapGraphNode[] adjacencies;
	
	public MapGraphNode(MapLocation location) {
		this.location = location;
		this.adjacencies = new MapGraphNode[8];
	}
	
}
