/*
 * SWEN30006 Project Part C
 * Group 109: Matt Perrott, Tobias Edwards, Kinsey Reeves
 */
package mycontroller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import tiles.MapTile;
import utilities.Coordinate;
import world.WorldSpatial;

/**
 * A representation of the observed (and unobserved)
 * portions of the map by our controller.
 */
public class AIMap {

	/** Contains a mapping from Coordinates to AITiles. */
	private HashMap<Coordinate, AITile> tiles;

	/** Whether or not the exit has been found by the Car. */
	private boolean exitFound;

	/** The coordinates of the exit. */
	private Coordinate exit;

	/**
	 * Initialises an empty AIMap.
	 */
	public AIMap() {
		tiles = new HashMap<Coordinate, AITile>();
		exitFound = false;
		exit = null;
	}

	/**
	 * Update the map with the current view, integrating the found
	 * tiles into the tiles map.
	 *
	 * @param currentView HashMap mapping coordinates to tiles for 7x7 view
	 */
	public void update(HashMap<Coordinate, MapTile> currentView) {
		for (Map.Entry<Coordinate, MapTile> entry : currentView.entrySet()) {
			if (!tiles.containsKey(entry.getKey())) {
				// Create KnownAITile and insert it into the tiles map
				KnownAITile tile = new KnownAITile(entry.getValue());
				tiles.put(entry.getKey(), tile);

				// Only update the exit if there isn't already an exit
				// tile found
				if (tile.isExit() && !exitFound) {
					exitFound = true;
					exit = entry.getKey();
				}
			}
		}
	}

	/**
	 * Returns the AITile at a given position.
	 *
	 * @param pos Coordinate of requested tile
	 * @return tile at given position
	 */
	public AITile tileAt(Coordinate pos) {
		if (!tiles.containsKey(pos)) {
			tiles.put(pos, new UnknownAITile());
		}

		return tiles.get(pos);
	}

	/**
	 * Returns the AITile at a given position.
	 *
	 * @param x x coordinate of requested tile
	 * @param y y coordinate of requested tile
	 * @return tile at given position
	 */
	public AITile tileAt(int x, int y) {
		return tileAt(new Coordinate(x, y));
	}

	/**
	 * Returns the AITile given a relative position
	 * from the car, with positive y being in front of the car
	 * etc.
	 *
	 * @param carPos position of the car
	 * @param orientation orientation of the car
	 * @param pos relative position
	 * @return requested AITile
	 */
	public AITile tileAtRelative(Coordinate carPos,
								 WorldSpatial.Direction orientation,
								 Coordinate pos) {
		Coordinate rotated = rotateToOrientation(pos, orientation);
		return tileAt(carPos.x + rotated.x, carPos.y + rotated.y);
	}

	/**
	 * Returns the AITile given a relative position
	 * from the car, with positive y being in front of the car
	 * etc.
	 *
	 * @param carPos position of the car
	 * @param orientation orientation of the car
	 * @param x relative x coordinate
	 * @param y relative y coordinate
	 * @return requested AITile
	 */
	public AITile tileAtRelative(Coordinate carPos,
								 WorldSpatial.Direction orientation,
								 int x, int y) {
		return tileAtRelative(carPos, orientation, new Coordinate(x, y));
	}
	
	/**
	 * Returns an array of all of the Coordinates that
	 * we have seen (and thus those that are known).
	 * 
	 * @return a list of all known coordinates
	 */
	public ArrayList<Coordinate> getKnownCoordinates() {	
		ArrayList<Coordinate> knownCoordinates = new ArrayList<Coordinate>();
		
		for (HashMap.Entry<Coordinate, AITile> entry : tiles.entrySet()) {
			// Extract (coordinate, tile) from the entry
			Coordinate coordinate = entry.getKey();
			AITile tile = entry.getValue();
			
			if (tile.known()) {
				knownCoordinates.add(coordinate);
			}
		}
		
		return knownCoordinates;
	}

	/**
	 * Checks whether or not there is a dead end ahead,
	 * since we are following a wall to our left we will
	 * define dead ends by having blocking tiles in the next 2
	 * tiles ahead, and having 2 or less blocks to the left
	 * and 1 block or less to the right.
	 *
	 * @param carPos position of the car
	 * @param orientation orientation of the car
	 * @return whether or not there is a dead end ahead
	 */
	public boolean deadEndAhead(Coordinate carPos,
								WorldSpatial.Direction orientation) {
		AITile oneAhead = tileAtRelative(carPos, orientation, 0, 1);
		AITile twoAhead = tileAtRelative(carPos, orientation, 0, 2);

		// If we don't have anything immediately (within 2) in front of us
		if (!oneAhead.blocking() && !twoAhead.blocking()) {
			return false;
		}

		int roomToLeft =
				spaceInDirection(carPos, orientation,
								 WorldSpatial.RelativeDirection.LEFT);
		int roomToRight =
				spaceInDirection(carPos, orientation,
								 WorldSpatial.RelativeDirection.RIGHT);

		// If we have less than 2 in each direction
		if ((roomToLeft <= 2 && roomToRight < 2)) {
			return true;
		}

		return false;
	}

	/**
	 * Returns how much space there is ahead and to the left/right
	 * of the car, in tiles.
	 *
	 * @param carPos position of the car
	 * @param orientation orientation of the car
	 * @param direction direction to check
	 * @return how many tiles worth of space there is
	 */
	public int spaceInDirection(Coordinate carPos,
								WorldSpatial.Direction orientation,
								WorldSpatial.RelativeDirection direction) {
		int space = 0;
		int step = 0;

		// Find the step we should increment by depending on direction
		switch (direction) {
		case LEFT:
			step = -1;
			break;
		case RIGHT:
			step = 1;
			break;
		default:
			break;
		}

		// While the tile at relative pos (space + step, 1) is not blocking
		while (!tileAtRelative(carPos, orientation, (space + 1) * step, 1)
				.blocking()) {
			space++;
		}

		return space;
	}

	/**
	 * Returns whether or not there is a trap in the following two
	 * tiles.
	 *
	 * @param carPos position of the car
	 * @param orientation orientation of the car
	 * @return whether traps are ahead
	 */
	public boolean trapsAhead(Coordinate carPos,
							  WorldSpatial.Direction orientation) {
		return (tileAtRelative(carPos, orientation, 0, 1).isTrap() ||
				tileAtRelative(carPos, orientation, 0, 2).isTrap());
	}

	/**
	 * Whether or not the traps ahead are traversable, i.e.
	 * there is a free tile immediately on the other side of a trap.
	 *
	 * @param carPos position of the car
	 * @param orientation orientation of the car
	 * @return whether or not the traps are traversable
	 */
	public boolean trapsTraversable(Coordinate carPos,
			  				  		WorldSpatial.Direction orientation) {
		boolean firstTrap = tileAtRelative(carPos, orientation, 0, 1)
							.isTrap();
		boolean secondTrap = tileAtRelative(carPos, orientation, 0, 2)
				             .isTrap();
		boolean thirdTrap = tileAtRelative(carPos, orientation, 0, 3)
							.isTrap();
		return (firstTrap && !secondTrap ||
				!firstTrap && secondTrap && !thirdTrap);
	}

	/**
	 * Returns a coordinate rotated to a certain direction given
	 * in terms of NORTH.
	 *
	 * @param pos position with respect to NORTH
	 * @param orientation orientation to rotate to
	 * @return rotate coordinate
	 */
	private Coordinate rotateToOrientation(
			Coordinate pos,	WorldSpatial.Direction orientation) {
		int x = pos.x;
		int y = pos.y;
		switch (orientation) {
		case NORTH:
			break;
		case EAST:
			x = pos.y;
			y = -pos.x;
			break;
		case SOUTH:
			x = -pos.x;
			y = -pos.y;
			break;
		case WEST:
			x = -pos.y;
			y = pos.x;
			break;
		default:
			break;
		}

		return new Coordinate(x, y);
	}

	/**
	 * Whether or not the exit has been found.
	 *
	 * @return whether or not exit found
	 */
	public boolean exitFound() {
		return exitFound;
	}

	/**
	 * Returns the coordinate of the exit tile.
	 *
	 * @return exit tile's coordinates
	 */
	public Coordinate getExit() {
		return exit;
	}

}
