/*
 * SWEN30006 Project Part C
 * Group 109: Matt Perrott, Tobias Edwards, Kinsey Reeves
 */
package mycontroller;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.HashSet;

import utilities.Coordinate;

/**
 * The ExiterStrategyFactory is a singleton used to construct
 * ExiterStrategy classes. It takes an AIMap along with start and
 * end positions and runs a breadth first search to construct
 * a path for the ExiterStrategy to be followed by the controller.
 */
public class ExiterStrategyFactory {
	
	/** The only instance of this singleton class. */
    private static ExiterStrategyFactory instance;
    
    /**
     * Returns the instance of ExiterStrategyFactory. If there hasn't been
     * an instance initialised one will be created.
     * @return
     */
    public static ExiterStrategyFactory getInstance() {
        if (instance == null) {
            instance = new ExiterStrategyFactory();
        }
        
        return instance;
    }
    
    /**
     * Returns a new ExiterStrategy given a controllers current position
     * and an AIMap. It will try to create a path from the cars current
     * position to the exit that avoid traps, but failing this it will include
     * some traps.
     * 
     * @param position cars current position
     * @param map the AIMap we are traversing
     * @return the appropriate ExiterStrategy
     */
    public ExiterStrategy getExiterStrategy(Coordinate position, AIMap map) {
    	Coordinate exit = map.getExit();
        ArrayList<Coordinate> path =
        		performBreadthFirstSearch(position, exit, map);
        
        // If we couldn't build a path without traps
        if (path == null) {
        	path = performBreadthFirstSearchWithTraps(position, exit, map);
        }
        
        // If we still can't build a path then the exit is unreachable at
        // the moment
        if (path == null) {
        	return null;
        }

        return new ExiterStrategy(path);
    }
    
    /**
     * performBreadthFirstSearch performs a BFS on the AIMap to construct
     * a path of coordinates from start to end (inclusive).
     * 
     * @param start the coordinate of the start of the path
     * @param end the coordinate of the end of the path
     * @param map the AIMap that the BFS is performed on
     * @param includeTraps whether or not to count traps as traversable
     * @return path from start to end as a list of coordinates
     */
    private ArrayList<Coordinate> performBreadthFirstSearch(
    		Coordinate start, Coordinate end,
    		AIMap map, boolean includeTraps) {

    	// A set of searched coordinates and the BFS queue
    	HashSet<Coordinate> searched = new HashSet<Coordinate>();
        LinkedList<Coordinate> queue = new LinkedList<Coordinate>();

        // A map from coordinate to its parent, doesn't include start
        HashMap<Coordinate, Coordinate> parents =
        		new HashMap<Coordinate, Coordinate>();

        // The coordinates which are known from the map
        ArrayList<Coordinate> knownCoordinates = map.getKnownCoordinates();
        
        searched.add(start);
        queue.add(start);

        // While the queue is empty we want to take the head of the
        // queue and search its neighbours
        while (!queue.isEmpty()) {
            Coordinate coordinate = queue.remove();
            AITile tile = map.tileAt(coordinate);
            
            if (coordinate.equals(end)) {
            	return pathFromParents(parents, end);
            }

            // If its not a traversable tile we don't want to add
            // its neighbours
            if (!(includeTraps && tile.isTrap()) && tile.blocking()) {
                continue;
            }

            ArrayList<Coordinate> neighbours =
        		getNeighbours(coordinate, knownCoordinates);
            for (Coordinate neighbour : neighbours) {
            	if (!searched.contains(neighbour)) {
            		searched.add(neighbour);
            		queue.add(neighbour);
            		parents.put(neighbour, coordinate);
            	}
            }
        }

        // Error, didn't find exit
        return null;
    }
    
    /**
     * performBreadthFirstSearch performs a BFS on the AIMap to construct
     * a path of coordinates from start to end (inclusive). Doesn't include
     * traps.
     * 
     * @param start the coordinate of the start of the path
     * @param end the coordinate of the end of the path
     * @param map the AIMap that the BFS is performed on
     * @return path from start to end as a list of coordinates
     */
    private ArrayList<Coordinate> performBreadthFirstSearch(
    		Coordinate start, Coordinate end, AIMap map) {
    	return performBreadthFirstSearch(start, end, map, false);
	}

    /**
     * performBreadthFirstSearch performs a BFS on the AIMap to construct
     * a path of coordinates from start to end (inclusive). Traps are
     * included as traversable coordinates.
     * 
     * @param start the coordinate of the start of the path
     * @param end the coordinate of the end of the path
     * @param map the AIMap that the BFS is performed on
     * @return path from start to end as a list of coordinates
     */
    private ArrayList<Coordinate> performBreadthFirstSearchWithTraps(
    		Coordinate start, Coordinate end, AIMap map) {
    	return performBreadthFirstSearch(start, end, map, true);
    }
    
    /**
     * Takes a map of each searched coordinate's parents and constructs
     * a path to a given end point from the start (the only coordinate
     * without a parent).
     * 
     * @param parents hash map mapping coordinate to its parent
     * @param exit coordinate of the end of the path
     * @return the path as a list of coordinates
     */
    private ArrayList<Coordinate> pathFromParents(
    		HashMap<Coordinate, Coordinate> parents, Coordinate exit) {
    	Coordinate current = exit;
        ArrayList<Coordinate> path = new ArrayList<Coordinate>();
        path.add(0, exit);
        
        // Until we find a coordinate without a parent (the start)
        // insert the parent of the current coordinate at the start
        // of the path
        while (parents.containsKey(current)) {
        	current = parents.get(current);
        	path.add(0, current);
        }
        
        return path;
    }

    /**
     * Returns the known coordinates that are adjacent to the current
     * coordinate (no including diagonals).
     * 
     * @param current current coordinate
     * @param knownCoordinates list of all known coordinates
     * @return a list of the neighbours
     */
    public ArrayList<Coordinate> getNeighbours(Coordinate current,
    		ArrayList<Coordinate> knownCoordinates) {
        ArrayList<Coordinate> neighbours = new ArrayList<Coordinate>();

        for (Coordinate candidate : knownCoordinates) {
        	int dx = Math.abs(current.x - candidate.x);
        	int dy = Math.abs(current.y - candidate.y);
            if (dx + dy == 1) {
                neighbours.add(candidate);
            }
        }
        
        return neighbours;
    }
}
