/*
 * SWEN30006 Project Part C
 * Group 109: Matt Perrott, Tobias Edwards, Kinsey Reeves
 */
package mycontroller;

/**
 * An AITile that hasn't been discovered by the controller
 * yet.
 */
public class UnknownAITile extends AITile {

	/**
	 * Whether or not this tile is known. Will
	 * always return false.
	 * 
	 * @return whether this is known, false
	 */
	@Override
	public boolean known() {
		return false;
	}

	/**
	 * Whether or not this tile is blocking, we assume
	 * that it is.
	 * 
	 * @return whether this tile is blocking, true
	 */
	@Override
	public boolean blocking() {
		return true;
	}

	/**
	 * Returns whether this tile is a trap, we assume
	 * it is not.
	 * 
	 * @return whether this tile is a trap
	 */
	@Override
	public boolean isTrap() {
		return false;
	}

	/**
	 * Returns whether this tile is an exit, we assume
	 * it is not.
	 * 
	 * @whether this tile is an exit.
	 */
	@Override
	public boolean isExit() {
		return false;
	}

}
