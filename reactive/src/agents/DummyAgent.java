package agents;

import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * A dummy agent that takes every task available and moves to the closest
 * neighbor otherwise.
 * 
 * @author jeremiaoberle
 *
 */
public class DummyAgent implements ReactiveBehavior {
	private int numActions;
	private Agent myAgent;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		numActions = 0;
		myAgent = agent;
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;

		if (availableTask == null) {
			City currentCity = vehicle.getCurrentCity();
			City closestNeigh = null;
			double min = Integer.MAX_VALUE;
			for (City n : currentCity.neighbors()) {
				Plan p = new Plan(currentCity, new Move(n));
				double dist = p.totalDistance();
				if (dist <= min) {
					closestNeigh = n;
					min = dist;
				}
			}
			action = new Move(closestNeigh);
		} else {
			action = new Pickup(availableTask);
		}

		if (numActions >= 1) {
			System.out.println("Dummy: average profit: " + myAgent.getTotalProfit() / (double) numActions + ")");
		}
		numActions++;

		return action;
	}
}
