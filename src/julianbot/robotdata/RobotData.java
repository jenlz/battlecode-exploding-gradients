package julianbot.robotdata;

import battlecode.common.*;
import julianbot.commands.GeneralCommands;
import julianbot.utils.pathfinder.Pathfinder;

public class RobotData {

	protected final Team team;
	protected final Team opponent;
	protected MapLocation spawnerLocation;
	protected Pathfinder pathfinder;
		protected Direction[] pathToDestination;
		protected boolean hasPath;
		protected int pathProgression;
	
	public RobotData(RobotController rc) {
		team = rc.getTeam();
		opponent = team.opponent();
		setSpawnerLocation(GeneralCommands.getSpawnerLocation(rc));
		this.pathfinder = new Pathfinder();
	}
	
	public Team getTeam() {
		return team;
	}
	
	public Team getOpponent() {
		return opponent;
	}

	public MapLocation getSpawnerLocation() {
		return spawnerLocation;
	}

	public void setSpawnerLocation(MapLocation spawnerLocation) {
		this.spawnerLocation = spawnerLocation;
	}
	
	public void buildMapGraph(RobotController rc) {
		pathfinder.buildGraph(rc);
	}
	
	public void calculatePathTo(MapLocation destination) {
		pathToDestination = pathfinder.getRouteTo(destination);
		hasPath = (pathToDestination != null && pathToDestination.length > 0);
		pathProgression = 0;
	}

	public Direction getCurrentPathDirection() {
		return pathToDestination[pathProgression];
	}
	
	public void incrementPathProgression() {
		pathProgression++;
		if(pathProgression >= pathToDestination.length) {
			pathToDestination = null;
			hasPath = false;
			pathProgression = 0;
		}
	}
	
	public boolean hasPath() {
		return hasPath;
	}
	
}
