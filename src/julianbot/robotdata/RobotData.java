package julianbot.robotdata;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Team;
import julianbot.commands.GeneralCommands;
import julianbot.robotdata.routedata.RouteData;
import julianbot.utils.pathfinder.MapGraph;

public class RobotData {

	protected final Team team;
	protected final Team opponent;
	protected MapLocation spawnerLocation;
	protected MapGraph mapGraph;
		protected Direction[] path;
		protected int pathProgression;
		
	public RobotData(RobotController rc) {
		team = rc.getTeam();
		opponent = team.opponent();
		setSpawnerLocation(GeneralCommands.getSpawnerLocation(rc));		
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

	public MapGraph getMapGraph() {
		return mapGraph;
	}

	public void setMapGraph(MapGraph mapGraph) {
		this.mapGraph = mapGraph;
	}

	public Direction[] getPath() {
		return path;
	}
	
	public boolean hasPath() {
		return path != null && path.length > 0;
	}

	public void setPath(Direction[] path) {
		this.path = path;
	}
	
	public Direction getNextPathDirection() {
		return path[pathProgression];
	}

	public int getPathProgression() {
		return pathProgression;
	}

	public void incrementPathProgression() {
		pathProgression++;
	}
	
	public void setPathProgression(int pathProgression) {
		this.pathProgression = pathProgression;
	}
	
	public boolean pathCompleted() {
		return pathProgression >= path.length;
	}
	
}
