package julianbot.utils.pathfinder;

import java.util.Iterator;
import java.util.LinkedList;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class MapGraph {

	private static Direction[] directions = {Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST};
	
	private int chainLength;
	private int dimension;
	
	private int nodeCount;
	private LinkedList<Integer> adj[];
	private int[] parentCodes;
	
	private MapLocation sourceLocation;
	
	public MapGraph(RobotController rc, int chainLength) {
		int dimension = 1 + 2 * chainLength;
		
		this.chainLength = chainLength;
		this.dimension = dimension;
		
		this.nodeCount = dimension * dimension;
		this.adj = new LinkedList[nodeCount];
		this.parentCodes = new int[nodeCount];
		
		this.sourceLocation = rc.getLocation();
		
		for(int i = 0; i < nodeCount; ++i) {
			this.adj[i] = new LinkedList<>();
		}
		
		for(int i = -chainLength + 1; i <= chainLength - 1; i++) {
			for(int j = -chainLength + 1; j <= chainLength - 1; j++) {
				MapLocation mapLocation = sourceLocation.translate(i, j);
				connectAllAdjacencies(rc, mapLocation);
			}
		}
	}
	
	private int getLocationCode(MapLocation testLocation) {
		int graphX = testLocation.x - sourceLocation.x + chainLength;
		int graphY = testLocation.y - sourceLocation.y + chainLength;
		return graphY * dimension + graphX;
	}
	
	private MapLocation getLocation(int locationCode) {
		int graphX = locationCode % dimension;
		int graphY = locationCode / dimension;
		return sourceLocation.translate(graphX - chainLength, graphY - chainLength);
	}
	
	private void connectAllAdjacencies(RobotController rc, MapLocation location) {	
		int locationCode = getLocationCode(location);
		
		try {
			for(Direction direction : directions) {
				if(rc.onTheMap(location.add(direction)) && !rc.isLocationOccupied(location.add(direction))) {
					addEdge(locationCode, getLocationCode(location.add(direction)));
				}
			}
		} catch(GameActionException e) {
			e.printStackTrace();
		}
	}
	
	private void addEdge(int v, int w) {
		adj[v].add(w);
	}
	
	public Direction[] BFS(MapLocation mapDestination) {
		int source = getLocationCode(sourceLocation);
		int destination = getLocationCode(mapDestination);
		
		boolean visited[] = new boolean[nodeCount];
		
		LinkedList<Integer> queue = new LinkedList<Integer>(); 
		  
        // Mark the current node as visited and enqueue it 
        visited[source]=true;
        queue.add(source);
  
        
        int node = source;
        boolean foundSolution = false;
        while (queue.size() != 0) 
        { 
            node = queue.poll(); 
            
            Iterator<Integer> i = adj[node].listIterator(); 
            while (i.hasNext()) 
            { 
                int n = i.next(); 
                if (!visited[n]) 
                { 
                    visited[n] = true; 
                    queue.add(n); 
                    parentCodes[n] = node;
                    if(n == destination) {
                    	foundSolution = true;
                    	break;
                    }
                } 
            }
        }
        
        if(!foundSolution) return new Direction[0];
        
        int depth = 1;
        int vertex = destination;
        while(vertex != source) {
        	vertex = parentCodes[vertex];
        	depth++;
        }
        
        Direction[] path = new Direction[depth - 1];
        vertex = destination;
        for(int i = path.length - 1; i >= 0; i--) {
        	MapLocation b = getLocation(vertex);
        	MapLocation a = getLocation((vertex = parentCodes[vertex]));
        	path[i] = a.directionTo(b);
        }
        
        return path;
	}
	
}
