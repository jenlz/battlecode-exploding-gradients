package julianbot.robotdata;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Team;
import julianbot.commands.GeneralCommands;
import julianbot.utils.pathfinder.MapGraph;

public class RobotData {

	protected final Team team;
	protected final Team opponent;
	protected MapLocation spawnerLocation;
	private MapLocation previousLocation;
	protected MapGraph mapGraph;
		protected Direction[] path;
		protected int pathProgression;
	private boolean hasPendingTransaction;
		private GeneralCommands.Type pendingTransactionType;
		private MapLocation pendingTransactionLocation;
		private int pendingTransactionSoupBid;
	
	public RobotData(RobotController rc) {
		team = rc.getTeam();
		opponent = team.opponent();
		previousLocation = rc.getLocation();
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

	public MapLocation getPreviousLocation() {
		return previousLocation;
	}

	public void setPreviousLocation(MapLocation previousLocation) {
		this.previousLocation = previousLocation;
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
	
	public boolean hasPendingTransaction() {
		return hasPendingTransaction;
	}
	
	public void setPendingTransaction(GeneralCommands.Type transactionType, MapLocation location, int soupBid) {
		this.hasPendingTransaction = true;
		this.pendingTransactionType = transactionType;
		this.pendingTransactionLocation = location;
		this.pendingTransactionSoupBid = soupBid;
	}
	
	public void clearPendingTransaction() {
		this.hasPendingTransaction = false;
	}

	public GeneralCommands.Type getPendingTransactionType() {
		return pendingTransactionType;
	}

	public void setPendingTransactionType(GeneralCommands.Type pendingTransactionType) {
		this.pendingTransactionType = pendingTransactionType;
	}

	public MapLocation getPendingTransactionLocation() {
		return pendingTransactionLocation;
	}

	public void setPendingTransactionLocation(MapLocation pendingTransactionLocation) {
		this.pendingTransactionLocation = pendingTransactionLocation;
	}

	public int getPendingTransactionSoupBid() {
		return pendingTransactionSoupBid;
	}

	public void setPendingTransactionSoupBid(int pendingTransactionSoupBid) {
		this.pendingTransactionSoupBid = pendingTransactionSoupBid;
	}
	
}
