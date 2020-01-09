package julianbot.robotdata;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import julianbot.commands.GeneralCommands;
import julianbot.utils.pathfinder.Pathfinder;

public class RobotData {

	protected MapLocation spawnerLocation;
	protected Pathfinder pathfinder;
	protected Direction[] pathToDestination;
	protected boolean hasPath;
	protected int pathProgression;
	
	public RobotData(RobotController rc) {
		setSpawnerLocation(GeneralCommands.getSpawnerLocation(rc));
		this.pathfinder = new Pathfinder();
	}
	
	public void buildMapGraph(RobotController rc) {
		pathfinder.buildGraph(rc);
	}
	
	public void calculatePathTo(MapLocation destination) {
		pathToDestination = pathfinder.getRouteTo(destination);
		hasPath = (pathToDestination != null && pathToDestination.length > 0);
		pathProgression = 0;
	}
	
	public void incrementPathProgression() {
		pathProgression++;
		if(pathProgression >= pathToDestination.length) {
			pathToDestination = null;
			hasPath = false;
			pathProgression = 0;
		}
	}
	public void setSpawnerLocation(MapLocation spawnerLocation) {
		this.spawnerLocation = spawnerLocation;
	}

	public MapLocation getSpawnerLocation() {
		return spawnerLocation;
	}

	public Direction getCurrentPathDirection() {
		return pathToDestination[pathProgression];
	}

	public boolean hasPath() {
		return hasPath;
	}
	
}
