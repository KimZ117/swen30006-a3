/*
 * SWEN30006 Project Part C
 * Group 109: Matt Perrott, Tobias Edwards, Kinsey Reeves
 */
package mycontroller;

import java.util.ArrayList;

import utilities.Coordinate;
import world.WorldSpatial;

/**
 * ExiterStrategy is a ControlStrategy that the controller
 * should use once an exit has been found, it follows
 * a given path from the cars current location to the exit,
 * without traveling on diagonals.
 */
public class ExiterStrategy extends ControlStrategy {

	/** The speed to travel when traversing the path forwards. */
	private static final float FORWARD_SPEED = 0.8f;
	
	/** The speed to travel when traversing the path in reverse. */
	private static final float REVERSE_SPEED = 0.5f;
	
	/** The path to follow, as a list of coordinates. */
	private ArrayList<Coordinate> path;
	
	/**
	 * Whether or not we have initialised with the controller.
	 * Performed when update is called for the first time.
	 */
	private boolean initialisedWithController;
	
	/** The current position of the car. */
	private Coordinate currentPosition;
	
	/** The index of the path that we're currently at. */
	private int pathIndex;
	
	/** Whether the car is backing up before traversing. */
	private boolean backingUp;
	
	/** The tile which the car should back up to. */
	private Coordinate backingUpTarget;
	
	/** The target tile we will be done at. */
	private Coordinate doneTarget;
	
	/** Whether the car has reached the end of the path. */
	private boolean done;
	
	/**
	 * Initiates an ExiterStrategy given a path to follow.
	 * 
	 * @param path the path the car should travel upon
	 */
	public ExiterStrategy(ArrayList<Coordinate> path) {
		this.path = path;
		this.pathIndex = 0;
		this.currentPosition = null;
		this.done = false;
		this.backingUp = false;
		this.initialisedWithController = false;
	}
	
	/**
	 * Makes decisions for the car in order for it to navigate
	 * the path successfully.
	 */
	@Override
	public void update(MyAIController controller) {
		// If we haven't stored the current position yet
		if (!initialisedWithController) {
			initialiseWithController(controller);
		}
		
		// We only want to perform an action if we enter a new tile
		// and we're not done
		if (done || !positionChanged(controller)) {
			return;
		}
		
		if (backingUp) {
			// We're back at the start
			if (backingUpTarget.equals(currentPosition)) {
				controller.toggleReverseMode();
				regulateSpeed(controller);
				backingUp = false;
			}
			
			// Don't want to continue ad potentially turn
			// if we're backing up
			return;
		}
		
		// If we reached the end stop, we're done!
		if (doneTarget.equals(currentPosition)) {
			controller.setSpeedTarget(0);
			done = true;
			return;
		}

		// We've successfully reached the next tile
		if (pathPeek().equals(currentPosition)) {
			pathIndex++;
		} else {
			return;
		}
		
		// Compare the orientation of the car vs the path
		WorldSpatial.Direction currentOrientation =
			controller.getOrientation();
		WorldSpatial.Direction requiredOrientation =
			directionBetween(path.get(pathIndex),
							 path.get(pathIndex + 1));
			
		// If we need to turn
		if (!currentOrientation.equals(requiredOrientation)) {
			WorldSpatial.RelativeDirection turnDir =
				turnDirection(currentOrientation,
							  requiredOrientation);
			controller.performTurn(turnDir);
		}
	}
	
	/**
	 * Initialise when we see the controller for the first time,
	 * this includes deciding whether to back up before we start
	 * and setting the target tile the car should head for.
	 * 
	 * @param controller the controller using this strategy
	 */
	private void initialiseWithController(MyAIController controller) {
		initialisedWithController = true;
		currentPosition = new Coordinate(controller.getPosition());
		doneTarget = path.get(path.size() - 1);
		
		WorldSpatial.Direction currentDir = controller.getOrientation();
		WorldSpatial.Direction startDir = directionBetween(path.get(0),
														   path.get(1));
		
		// If we're not oriented correctly we want to reverse
		// one tile so we can turn, or even reverse the entire
		// path (this isn't so unreasonable since paths should
		// be relatively short).
		if (!currentDir.equals(startDir)) {
			// Tile just before our start tile in our current direction
			backingUpTarget = getReverseTarget(controller);
			
			// Start reversing
			controller.toggleReverseMode();
			
			// We only actually want to be in "backingUp mode" if
			// we have to turn, if the previous tile is on
			// our path then reverse the whole way
			if (!backingUpTarget.equals(path.get(1))) {
				backingUp = true;
			}
		}
		
		regulateSpeed(controller);
		
		// Make sure that the controller is starting at
		// the intended start point.
		if (!currentPosition.equals(path.get(0))) {
			System.out.println("ERROR: Car not starting on path");
		}
	}

	/**
	 * Whether the controller should change strategy, since this strategy
	 * should lead the car to the exit this will always be false.
	 */
	@Override
	public boolean shouldChangeStrategy(MyAIController controller) {
		return false;
	}
	
	/**
	 * Whether or not the position of the car has changed since the
	 * last update, will update currentPosition if this is the case.
	 * 
	 * @param controller controller to check
	 * @return whether the position has changed
	 */
	private boolean positionChanged(MyAIController controller) {
		Coordinate newPosition = new Coordinate(controller.getPosition());
		
		if (!newPosition.equals(currentPosition)) {
			currentPosition = newPosition;
			return true;
		}
		
		return false;
	}
	
	/**
	 * Sets the speed of the controller appropriately depending
	 * on which direction it is driving.
	 * 
	 * @param controller controller to regulate speed of
	 */
	private void regulateSpeed(MyAIController controller) {
		if (controller.getReverseMode()) {
			controller.setSpeedTarget(REVERSE_SPEED);
		} else {
			controller.setSpeedTarget(FORWARD_SPEED);
		}
	}
	
	/**
	 * Returns the coordinate of the next tile in the path,
	 * without changing pathIndex.
	 * 
	 * @return next coordinate in the path
	 */
	private Coordinate pathPeek() {
		return path.get(pathIndex + 1);
	}
	
	/**
	 * Returns the direction between two neighbouring
	 * tiles, i.e. direction from a to b.
	 * 
	 * @param a first coordinate
	 * @param b second coordinate
	 * @return direction from a to b
	 */
	private WorldSpatial.Direction directionBetween(
			Coordinate a, Coordinate b) {
		int dx = b.x - a.x;
		int dy = b.y - a.y;
		
		if (dy == -1 && dx == 0) {
			return WorldSpatial.Direction.SOUTH;
		} else if (dy == 1 && dx == 0) {
			return WorldSpatial.Direction.NORTH;
		} else if (dx == -1 && dy == 0) {
			return WorldSpatial.Direction.WEST;
		} else if (dx == 1 && dy == 0) {
			return WorldSpatial.Direction.EAST;
		}
		
		System.out.println(
				"Shouldn't try get direction between " + a + " and " + b);
		return null;
	}
	
	/**
	 * Returns the direction needed to turn (left/right) to get
	 * from the currentOrientation to the turnOrientation required.
	 * 
	 * @param currentOrientation cars current orientation
	 * @param turnOrientation the orientation required after the turn
	 * @return the relative direction the controller should turn in
	 */
	private WorldSpatial.RelativeDirection turnDirection(
			WorldSpatial.Direction currentOrientation,
			WorldSpatial.Direction turnOrientation) {
		
		
		if (toLeft(currentOrientation).equals(turnOrientation)) {
			return WorldSpatial.RelativeDirection.LEFT;
		} else if (toLeft(turnOrientation).equals(currentOrientation)) {
			return WorldSpatial.RelativeDirection.RIGHT;
		} else {
			String errorString = "Can't turn from " + currentOrientation +
								 " to " + turnOrientation;
			System.out.println(errorString);
			return null;
		}
	}
	
	/**
	 * Returns the cardinal direction to the left of the supplied
	 * direction.
	 * 
	 * @param dir direction in question
	 * @return the direction to the left of supplied direction
	 */
	private WorldSpatial.Direction toLeft(WorldSpatial.Direction dir) {
		switch (dir) {
		case NORTH:
			return WorldSpatial.Direction.WEST;
		case WEST:
			return WorldSpatial.Direction.SOUTH;
		case SOUTH:
			return WorldSpatial.Direction.EAST;
		case EAST:
			return WorldSpatial.Direction.NORTH;
		default:
			return null;
		}
	}
	
	/**
	 * Returns the tile behind the controller, given its current
	 * position and orientation.
	 * 
	 * @param controller controller in question
	 * @return tile behind the controller
	 */
	private Coordinate getReverseTarget(MyAIController controller) {
		Coordinate initial = new Coordinate(controller.getPosition());
		int x = initial.x;
		int y = initial.y;
		
		switch (controller.getOrientation()) {
		case NORTH:
			y -= 1;
			break;
		case EAST:
			x -= 1;
			break;
		case SOUTH:
			y += 1;
			break;
		case WEST:
			x += 1;
			break;
		default:
			break;
		}
		
		return new Coordinate(x, y);
	}

}
