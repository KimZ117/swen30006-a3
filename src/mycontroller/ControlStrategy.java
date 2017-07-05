/*
 * SWEN30006 Project Part C
 * Group 109: Matt Perrott, Tobias Edwards, Kinsey Reeves
 */
package mycontroller;

/**
 * A ControlStrategy is a strategy that makes
 * decisions about directions and actions made by the
 * car.
 */
public abstract class ControlStrategy {

	/**
	 * Updates the controller by making decisions about
	 * which action the car should take next.
	 * 
	 * @param controller The car controller being controlled
	 */
	public abstract void update(MyAIController controller);
	
	/**
	 * Whether or not the strategy should change from the current one.
	 * 
	 * @param controller controller asking whether strategy should change
	 * @return whether or not the strategy should be changed
	 */
	public abstract boolean shouldChangeStrategy(MyAIController controller);
	
}
