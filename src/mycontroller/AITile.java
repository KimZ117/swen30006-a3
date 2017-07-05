/*
 * SWEN30006 Project Part C
 * Group 109: Matt Perrott, Tobias Edwards, Kinsey Reeves
 */
package mycontroller;

/**
 * The representation of a tile in the AIMap.
 * Will be extended by both KnownAITile and UnknownAITile.
 */
public abstract class AITile {

	/**
	 * Returns whether or not this tile has been
	 * seen by the car.
	 * 
	 * @return whether or not this tile is known
	 */
	public abstract boolean known();
	
	/**
	 * Returns whether or not this tile is a blocking
	 * tile. If it is not blocking that indicates that
	 * we know the car can drive over this tile.
	 * 
	 * @return whether or not this tile is blocking
	 */
	public abstract boolean blocking();
	
	/**
	 * Returns whether or not this tile is a trap.
	 * 
	 * @return whether or not this tile is a trap
	 */
	public abstract boolean isTrap();
	
	/**
	 * Returns whether or not this tile is an exit tile.
	 * 
	 * @return whether or not this tile is an exit tile
	 */
	public abstract boolean isExit();
	
}
