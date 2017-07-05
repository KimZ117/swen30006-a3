/*
 * SWEN30006 Project Part C
 * Group 109: Matt Perrott, Tobias Edwards, Kinsey Reeves
 */
package mycontroller;

import utilities.Coordinate;
import world.WorldSpatial;

/**
 * Subclass of ControlStrategy to handle decision
 * logic while the car is in 'explorer' mode
 * i.e. yet to find the exit.
 */
public class ExplorerStrategy extends ControlStrategy {

	/** Distance in tiles between car and wall before applying decision logic. */
	private static final int WALL_THRESHOLD = 2;

	/** The states the car can be in while exploring. */
	private enum ExplorerState {NORMAL, WALL_FOLLOWING, JUST_TURNED_LEFT, PASSING_TRAP};

	/** If the car has just reversed. */
	private boolean justReversed = false;

	/** The state of the car, initialised to NORMAL state. */
	private ExplorerState state = ExplorerState.NORMAL;
	
	/** The map the car is exploring on. */
	private AIMap map;
	
	/** The last position the car was at. */
	private String previousPosition;

	/**
	 * Initialises the ExplorerStrategy, creating a new AIMap. 
	 */
	public ExplorerStrategy() {
		map = new AIMap();
	}

	/**
	 * Applies decision logic for the car based on it's current
	 * state. Switches between states responding to the type of AI tiles 
	 * on the AIMap, and their position relative to the car.
	 */
	@Override
	public void update(MyAIController controller) {
		// Update Map
		map.update(controller.getView());
		WorldSpatial.Direction orientation = controller.getOrientation();
		Coordinate pos = new Coordinate(controller.getPosition());

		System.out.println(state);

		switch (state) {
		case NORMAL:
			updateNormal(controller, pos, orientation);
			break;
		case WALL_FOLLOWING:
			updateWallFollowing(controller, pos, orientation);
			break;
		// Checks car has moved tiles before going back to WALL_FOLLOWING state
		case JUST_TURNED_LEFT:
			updateJustTurnedLeft(controller, pos, orientation);
			break;
		case PASSING_TRAP:
			updatePassingTrap(controller, pos, orientation);
			break;
		}
	}

	/** 
	 * Whether or not the controller should change strategy to
	 * ExiterStrategy.
	 */
	@Override
	public boolean shouldChangeStrategy(MyAIController controller) {
		return map.exitFound();
	}
	
	/**
	 * Returns the AIMap.
	 * 
	 * @returns the map this strategy is traversing
	 */
	public AIMap getMap() {
		return map;
	}

	/** 
	 * Checks to see if there are tiles blocking in a specific
	 * direction, in range of WALL_THRESHOLD.
	 * 
	 * @param carPos the coordinate the car is on
	 * @param direction the direction to check
	 * @return if there are 'blocking' tiles in direction
	 */
	private boolean checkInDirection(Coordinate carPos,
			                         WorldSpatial.Direction direction) {
		for (int i = 1; i <= WALL_THRESHOLD; i++) {
			if (map.tileAtRelative(carPos, direction, 0, i).blocking()) {
				return true;
			}
		}
		return false;
	}

	/** 
	 * Check if the car is following a wall.
	 * 
	 * @param carPos the coordinate the car is on
	 * @param orientation the direction to check
	 * @return if the car is following a wall
	 */
	private boolean checkFollowingWall(Coordinate carPos,
										   WorldSpatial.Direction orientation) {
		// If it's a traversable trap return false
		if (map.tileAtRelative(carPos, orientation, -1, 0).isTrap() &&
			!map.tileAtRelative(carPos, orientation, -2, 0).blocking()) {
			return false;
		}

		// Return true if there are any blocking tiles in specific range ahead
		for (int i = 1; i <= WALL_THRESHOLD; i++) {
			if (map.tileAtRelative(carPos, orientation, -i, 0).blocking()) {
				return true;
			}
		}
		return false;
	}

	/** 
	 * Perform an action depending on how much space there is in
	 * the deadend. Possible actions are u-turn, 3 point turn, or
	 * reverse. 
	 * 
	 * @param controller the MyAIController being used
	 */
	private void dealWithDeadEnd(MyAIController controller) {
		WorldSpatial.Direction orientation = controller.getOrientation();
		Coordinate pos = new Coordinate(controller.getPosition());

		// Check the space on left and right of the car
		int spaceOnRight = map.spaceInDirection(pos, orientation,
				WorldSpatial.RelativeDirection.RIGHT);
		int spaceOnLeft = map.spaceInDirection(pos, orientation,
				WorldSpatial.RelativeDirection.LEFT);

		// Perform turning actions based on space available
		if (spaceOnRight > 1) {
			controller.performUTurn(WorldSpatial.RelativeDirection.RIGHT);
			state = ExplorerState.JUST_TURNED_LEFT;
		} else if (spaceOnRight >= 0 && spaceOnLeft >= 1) {
			controller.performThreePointTurn(WorldSpatial.RelativeDirection.RIGHT);
			state = ExplorerState.JUST_TURNED_LEFT;
		} else {
			justReversed = true;
			controller.toggleReverseMode();
		}
	}
	
	/**
	 * Decision logic to apply when car is in NORMAL state and
	 * update() is called. Car can only move into WALL_FOLLOWING
	 * once it has found a wall to follow.
	 * 
	 * @param controller the MyAIController used
	 * @param pos the Coordinate the car is on
	 * @param orientation the orientation of the car
	 */
	private void updateNormal(MyAIController controller, Coordinate pos, WorldSpatial.Direction orientation) {
		if (checkInDirection(pos, WorldSpatial.Direction.NORTH)) {
			if (!orientation.equals(WorldSpatial.Direction.EAST)) {
				controller.performTurn(WorldSpatial.RelativeDirection.RIGHT);
			} else {
				state = ExplorerState.WALL_FOLLOWING;
			}
		} else if (!orientation.equals(WorldSpatial.Direction.NORTH)) {
			controller.performTurn(WorldSpatial.RelativeDirection.LEFT);
		}
	}

	/**
	 * Decision logic to apply when car is in WALL_FOLLOWING state and
	 * update() is called. Car can move into PASSING_TRAP or
	 * JUST_TURNED_LEFT.
	 * 
	 * @param controller the MyAIController used
	 * @param pos the Coordinate the car is on
	 * @param orientation the orientation of the car
	 */
	private void updateWallFollowing(MyAIController controller, Coordinate pos, WorldSpatial.Direction orientation) {
		if (checkFollowingWall(pos, orientation)) {
			if (map.trapsAhead(pos, orientation) && map.trapsTraversable(pos, orientation)) {
				state = ExplorerState.PASSING_TRAP;
			} else if (map.deadEndAhead(pos, orientation)) {
				dealWithDeadEnd(controller);
			} else if (checkInDirection(pos, orientation)) {
				controller.performTurn(WorldSpatial.RelativeDirection.RIGHT);
			}
		} else if (justReversed) {
			controller.performSpin(WorldSpatial.RelativeDirection.LEFT);
			justReversed = false;
			controller.toggleReverseMode();
		} else {
			controller.performTurn(WorldSpatial.RelativeDirection.LEFT);
			state = ExplorerState.JUST_TURNED_LEFT;
			previousPosition = controller.getPosition();
		}
	}
	
	/**
	 * Decision logic to apply when car is in JUST_TURNED_LEFT state and
	 * update() is called. Car can only move into WALL_FOLLOWING state
	 * when it has moved tile positions.
	 * 
	 * @param controller the MyAIController used
	 * @param pos the Coordinate the car is on
	 * @param orientation the orientation of the car
	 */
	private void updateJustTurnedLeft(MyAIController controller, Coordinate pos, WorldSpatial.Direction orientation) {
		// Check car has moved positions
		if (controller.getPosition() != previousPosition) {
			if (checkFollowingWall(pos, orientation)) {
				state = ExplorerState.WALL_FOLLOWING;
			}
		}
	}
	
	/**
	 * Decision logic to apply when car is in PASSING_TRAP state and
	 * update() is called. Car can only move into WALL_FOLLOWING state
	 * once it has passed the trap and there are no traps ahead.
	 * 
	 * @param controller the MyAIController used
	 * @param pos the Coordinate the car is on
	 * @param orientation the orientation of the car
	 */
	private void updatePassingTrap(MyAIController controller, Coordinate pos, WorldSpatial.Direction orientation) {
		if (!map.trapsAhead(pos, orientation) && !map.tileAt(pos).isTrap()) {
			controller.resetSpeedTarget();
			state = ExplorerState.WALL_FOLLOWING;
		} else {
			controller.setSpeedTarget(3);
		}
	}
}
