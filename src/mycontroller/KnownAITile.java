/*
 * SWEN30006 Project Part C
 * Group 109: Matt Perrott, Tobias Edwards, Kinsey Reeves
 */
package mycontroller;

import tiles.MapTile;
import tiles.TrapTile;
import tiles.UtilityTile;

/**
 * A KnownAITile is an AITile that the controller has seen
 * and we can be sure of its attributes.
 */
public class KnownAITile extends AITile {

	/** The 4 types of tile we know and care about. */
	private enum KnownAITileType {FREE, WALL, TRAP, EXIT};

	/** A reference to the underlying MapTile. */
	private MapTile tile;
	
	/** The simpler to check tile type. */
	private KnownAITileType type;
	
	/**
	 * Initiates a new known tile.
	 * 
	 * @param tile underlying MapTile
	 */
	public KnownAITile(MapTile tile) {
		this.tile = tile;
		
		if (tile instanceof TrapTile) {
			type = KnownAITileType.TRAP;
		} else if (tile instanceof UtilityTile &&
				  ((UtilityTile) tile).isExit()) {
			type = KnownAITileType.EXIT;
		} else if (tile.getName().equals("Wall")) {
			type = KnownAITileType.WALL;
		} else {
			type = KnownAITileType.FREE;
		}
	}
	
	/**
	 * Returns the underlying MapTile.
	 * 
	 * @return underlying MapTile
	 */
	public MapTile getTile() {
		return tile;
	}
	
	/**
	 * Whether or not the tile is known.
	 * 
	 * @return whether the tile is know
	 */
	@Override
	public boolean known() {
		return true;
	}

	/**
	 * Whether the tile is blocking, i.e. whether it
	 * is FREE or an EXIT.
	 * 
	 * @return whether the tile is blocking
	 */
	@Override
	public boolean blocking() {
		return !(type == KnownAITileType.FREE ||
				 type == KnownAITileType.EXIT);
	}

	/**
	 * Whether or not the tile is a trap.
	 * 
	 * @return whether the tile is a trap
	 */
	@Override
	public boolean isTrap() {
		return (type == KnownAITileType.TRAP);
	}

	/**
	 * Whether or not the tile is an exit.
	 * 
	 * @return whether the tile is an exit
	 */
	@Override
	public boolean isExit() {
		return (type == KnownAITileType.EXIT);
	}

}
