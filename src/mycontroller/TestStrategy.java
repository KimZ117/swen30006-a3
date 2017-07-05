/*
 * SWEN30006 Project Part C
 * Group 109: Matt Perrott, Tobias Edwards, Kinsey Reeves
 */
package mycontroller;

import utilities.Coordinate;

public class TestStrategy extends ControlStrategy {

	private boolean reversing = false;

	@Override
	public void update(MyAIController controller) {
		controller.setSpeedTarget(2f);
		if (at(controller, 4, 17) && !reversing) {
			controller.toggleReverseMode();
			reversing = true;
			System.out.println("Reversing!");
		}
	}

	@Override
	public boolean shouldChangeStrategy(MyAIController controller) {
		return (at(controller, 2, 17) && reversing);
	}

	private boolean at(MyAIController controller, int x, int y) {
		Coordinate current = new Coordinate(controller.getPosition());
		Coordinate target = new Coordinate(x, y);

		return current.equals(target);
	}

}
