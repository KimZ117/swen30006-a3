/*
 * SWEN30006 Project Part C
 * Group 109: Matt Perrott, Tobias Edwards, Kinsey Reeves
 */
package mycontroller;

import java.util.Stack;

import controller.CarController;
import utilities.Coordinate;
import world.Car;
import world.WorldSpatial;

/**
 * Subclass of CarController which encapsulates
 * all of the movement logic, as well as delegating
 * to a ControlStrategy for decision logic.
 */
public class MyAIController extends CarController{

	/** The maximum speed at which the car should go. */
	private static final float MAX_SPEED = 2.0f;

	/** The speed at which we are considered full speed during a turn. */
	private static final float SPEED_MULTIPLE = 0.8f;

	/** Alternative speed for full speed considerations. */
	private static final float SPEED_BUFFER = 0.3f;

	/** The speed at which a u-turn should be carried out. */
	private static final float UTURN_SPEED = 0.8f;

	/** The speed at which a spin should be carried out. */
	private static final float SPIN_SPEED = 0.1f;

	/** The speed at which a three point turn should be carried out. */
	private static final float THREE_POINT_SPEED = 0.4f;
	
	/** The number of degrees the car can be mis-aligned. */
	private static final float MISALIGNED_THRESHOLD = 1;

	/** The actions that MyAIController can carry out. */
	public enum AIAction {STOP, GO, TURN, UTURN, THREE_POINT};

	/** The current ControlStratefy making decisions for the controller. */
	private ControlStrategy strategy;

	/** Whether or not the car is currently in reverse mode. */
	private boolean reversing;

	/** The stack of previous actions, helps for action and composition. */
	private Stack<AIAction> actionStack;

	/** The current action being carried out. */
	private AIAction currentAction;

	/** The stage of the action being carried out, starts at 0 each time. */
	private int actionStage;

	/** The previous max speed before the last change. */
	private float previousMaxSpeed;

	/** The current maximum speed for the car. */
	private float currentMaxSpeed;

	/** The orientation of the car when the last turn started. */
	private WorldSpatial.Direction previousOrientation;

	/** The direction (left/right) of the current action. */
	private WorldSpatial.RelativeDirection actionDirection;

	/**
	 * Initialises a new MyAIController, controlling a given car.
	 * @param car the car that MyAIController with control
	 */
	public MyAIController(Car car) {
		super(car);
		strategy = new ExplorerStrategy();
		reversing = false;

		// Initialise actions and action queue;
		currentAction = AIAction.GO;
		actionStack = new Stack<AIAction>();
		actionStage = 0;

		currentMaxSpeed = MAX_SPEED;
		previousMaxSpeed = MAX_SPEED;
	}

	/**
	 * Update delegates the decision logic to the current control strategy,
	 * deals with switching strategies when appropriate to do so, and
	 * carries out any current actions.
	 */
	@Override
	public void update(float delta) {
		// We want to delegate to the ControlStrategy when we aren't
		// in the middle of performing an action.
		if (currentAction == AIAction.STOP || currentAction == AIAction.GO) {
			strategy.update(this);
			
			if (strategy.shouldChangeStrategy(this)) {
				AIMap map = ((ExplorerStrategy) strategy).getMap();
				ExiterStrategy exiter = ExiterStrategyFactory
						.getInstance()
						.getExiterStrategy(
								new Coordinate(getPosition()), map);
			
				// If exiter was null that would indicate that even though
				// we have found the exit it is unreachable
				if (exiter != null) {
					strategy = exiter;
				}
			}
		}

		switch (currentAction) {
		case STOP:
			updateStop(delta);
			break;
		case GO:
			updateGo(delta);
			break;
		case TURN:
			updateTurn(delta);
			break;
		case UTURN:
			updateUTurn(delta);
			break;
		case THREE_POINT:
			updateThreePointTurn(delta);
		default:
			break;
		}
	}

	/**
	 * Encapsulates the movement logic for the STOP state.
	 *
	 * @param delta seconds since last update
	 */
	private void updateStop(float delta) {
		readjust(delta);
		if (getVelocity() > 0) {
			applyBrake();
		}
	}

	/**
	 * Encapsulates the movement logic for the GO state.
	 *
	 * @param delta seconds since last update
	 */
	private void updateGo(float delta) {
		readjust(delta);
		if (getVelocity() < currentMaxSpeed) {
		    accelerate();
		} else {
			applyBrake();
		}

	}

	/**
	 * Encapsulates the movement logic for the TURN state.
	 *
	 * @param delta seconds since last update
	 */
	private void updateTurn(float delta) {
		if (getVelocity() < currentMaxSpeed) {
			accelerate();
		}
		if (getOrientation() != previousOrientation) {
			actionDone();
		}
		turn(actionDirection, delta);
	}

	/**
	 * Encapsulates the movement logic for the UTURN state.
	 *
	 * @param delta seconds since last update
	 */
	private void updateUTurn(float delta) {
		switch (actionStage) {
		case 0:
			if (getVelocity() > currentMaxSpeed) {
				applyBrake();
			} else {
				actionStage++;
			}
			break;
		case 1:
			actionStage++;
			performTurn(actionDirection);
			break;
		case 2:
			actionStage++;
			performTurn(actionDirection);
			break;
		case 3:
			resetSpeedTarget();
			actionDone();
			break;
		default:
			break;
		}
	}

	/**
	 * Encapsulates the movement logic for the THREE_POINT state.
	 *
	 * @param delta seconds since last update
	 */
	private void updateThreePointTurn(float delta) {
		switch (actionStage) {
		case 0:
			// Currently Reversing
			if (getVelocity() == 0) actionStage++;
			accelerate();
			break;
		case 1:
			// When we've reversed far enough (speed is easier to
			// measure than distance)
			if (getVelocity() > currentMaxSpeed - SPEED_BUFFER) actionStage++;
			accelerate();
			break;
		case 2:
			// Get up to the necessary speed before starting the turn
			if (getVelocity() > currentMaxSpeed) {
				applyBrake();
			} else if (getVelocity() < currentMaxSpeed * SPEED_MULTIPLE) {
				accelerate();
			} else {
				actionStage++;
				performTurn(actionDirection);
			}
		case 3:
			// Stop and then start reversing
			if (getVelocity() > 0) {
				applyBrake();
			} else {
				actionStage++;
				toggleReverseMode();
			}
			break;
		case 4:
			// Get back up to speed before turning again
			if (getVelocity() < currentMaxSpeed) {
				accelerate();
			} else {
				actionStage++;
			}
			break;
		case 5:
			// Turn again
			actionStage++;
			performTurn(actionDirection);
			break;
		case 6:
			resetSpeedTarget();
			actionDone();
			break;
		default:
			break;
		}
	}

	/**
	 * Indicates that the controller should perform a 90 degree
	 * turn in a given direction.
	 *
	 * @param direction the relative direction of the turn
	 */
	public void performTurn(WorldSpatial.RelativeDirection direction) {
		previousOrientation = getOrientation();
		actionDirection = direction;
		setAction(AIAction.TURN);
	}

	/**
	 * Indicates that the controller should perform a 180 degree
	 * u-turn in a given direction.
	 *
	 * @param direction the relative direction of the u-turn
	 */
	public void performUTurn(WorldSpatial.RelativeDirection direction) {
		previousOrientation = getOrientation();
		actionDirection = direction;
		setAction(AIAction.UTURN);

		setSpeedTarget(UTURN_SPEED);

		actionStage = 0;
	}

	/**
	 * Indicates that the controller should perform a 180 degree
	 * spin on the spot. This is just a slower u-turn.
	 *
	 * @param direction the relative direction to spin
	 */
	public void performSpin(WorldSpatial.RelativeDirection direction) {
		previousOrientation = getOrientation();
		actionDirection = direction;
		setAction(AIAction.UTURN);

		setSpeedTarget(SPIN_SPEED);

		actionStage = 0;
	}

	/**
	 * Indicates that the controller should perform a 180 degree
	 * three point turn in a given direction.
	 *
	 * @param direction the relative direction of the turn
	 */
	public void performThreePointTurn(
			WorldSpatial.RelativeDirection direction) {
		previousOrientation = getOrientation();

		actionDirection = direction;

		setAction(AIAction.THREE_POINT);
		actionStage = 0;

		setSpeedTarget(THREE_POINT_SPEED);

		toggleReverseMode();
	}

	/**
	 * Toggle whether or not the controller is in reverse mode.
	 */
	public void toggleReverseMode() {
		reversing = !reversing;
	}
	
	/**
	 * Returns whether or not the controller is in reverse mode.
	 * 
	 * @return whether in reverse mode or not
	 */
	public boolean getReverseMode() {
		return reversing;
	}

	/**
	 * Set the maximum speed target for the car.
	 *
	 * @param speed maximum speed target
	 */
	public void setSpeedTarget(float speed) {
		previousMaxSpeed = currentMaxSpeed;
		currentMaxSpeed = Math.min(speed, MAX_SPEED);
	}

	/**
	 * Reset the speed target to the previous value.
	 */
	public void resetSpeedTarget() {
		currentMaxSpeed = previousMaxSpeed;
	}

	/**
	 * Returns the orientation of the car, overloaded so
	 * that if the car is reversing the opposite direction
	 * is given.
	 */
	@Override
	public WorldSpatial.Direction getOrientation() {
		WorldSpatial.Direction direction = super.getOrientation();

		if (reversing) {
			return oppositeDirection(direction);
		} else {
			return direction;
		}
	}

	/**
	 * Returns the opposite direction to that given.
	 *
	 * @param direction direction to reverse
	 * @return the opposite of direction
	 */
	private WorldSpatial.Direction oppositeDirection(
		WorldSpatial.Direction direction) {

		switch (direction) {
		case NORTH:
			return WorldSpatial.Direction.SOUTH;
		case EAST:
			return WorldSpatial.Direction.WEST;
		case SOUTH:
			return WorldSpatial.Direction.NORTH;
		case WEST:
			return WorldSpatial.Direction.EAST;
		default:
			// There is no sensible default for this case
			// as we will never reach this point
			return WorldSpatial.Direction.NORTH;
		}
	}

	/**
	 * Called when an action is complete,
	 * the current action will be set to the previous
	 * action on the actionStack.
	 */
	private void actionDone() {
		if (actionStack.isEmpty()) {
			currentAction = AIAction.STOP;
		} else {
			currentAction = actionStack.pop();
		}
	}

	/**
	 * Sets a new current action and pushes the old current action
	 * onto the action stack.
	 *
	 * @param newAction new action to be carried out
	 */
	private void setAction(AIAction newAction) {
		actionStack.push(currentAction);
		currentAction = newAction;
	}

	/**
	 * Readjust the orientation of the car so it is as close to
	 * straight in the direction it is headed as possible.
	 *
	 * @param delta seconds since last update
	 */
	private void readjust(float delta) {
		float misaligned = degreesMisaligned();

		if (misaligned < MISALIGNED_THRESHOLD) {
			turnRight(delta);
		} else if (misaligned > MISALIGNED_THRESHOLD) {
			turnLeft(delta);
		}
	}

	/**
	 * Returns how many degrees (and in which direction, left given
	 * by negative, the car is mis-aligned from one of the
	 * cardinal directions.
	 *
	 * @return number of degrees mis-aligned, with magnitude and direction
	 */
	private float degreesMisaligned() {
		float current = getAngle();
		float misaligned = WorldSpatial.EAST_DEGREE_MAX;
		float cardinals[] = {
				WorldSpatial.EAST_DEGREE_MIN,
				WorldSpatial.NORTH_DEGREE,
				WorldSpatial.WEST_DEGREE,
				WorldSpatial.SOUTH_DEGREE,
				WorldSpatial.EAST_DEGREE_MAX
			};

		for (float cardinal : cardinals) {
			if (Math.abs(cardinal - current) < Math.abs(misaligned)) {
				misaligned = cardinal - current;
			}
		}

		return misaligned;
	}

	/**
	 * Performs a turn in a given direction.
	 *
	 * @param direction direction to turn in
	 * @param delta seconds since last update
	 */
	private void turn(WorldSpatial.RelativeDirection direction, float delta) {
		switch (direction) {
		case LEFT:
			turnLeft(delta);
			break;
		case RIGHT:
			turnRight(delta);
			break;
		default:
			break;
		}
	}

	/**
	 * Accelerate in the direction the car is going.
	 */
	private void accelerate() {
		if (!reversing) {
			applyForwardAcceleration();
		} else {
			applyReverseAcceleration();
		}
	}

}
