package agents;

import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;

public class DeliberativeAgent implements DeliberativeBehavior {

	enum Algorithm {
		BFS, ASTAR
	}

	/* Environment */
	Topology topology;
	TaskDistribution td;

	/* the properties of the agent */
	Agent agent;
	int capacity;

	/* the planning class */
	Algorithm algorithm;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		this.topology = topology;
		this.td = td;
		this.agent = agent;

		// initialize the planner
		int capacity = agent.vehicles().get(0).capacity();
		String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");

		// Throws IllegalArgumentException if algorithm is unknown
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());

		// ...
	}

	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {

		Plan plan;

		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			// ...
			plan = aStar(vehicle, tasks);
			break;
		case BFS:
			// ...
			plan = BFS(vehicle, tasks);
			break;
		default:
			throw new AssertionError("Should not happen.");
		}
		return plan;

	}

	private Plan aStar(Vehicle vehicle, TaskSet tasks) {

		return null;
	}

	private Plan BFS(Vehicle vehicle, TaskSet tasks) {

		return null;
	}

	@Override
	public void planCancelled(TaskSet carriedTasks) {
		// TODO Auto-generated method stub

	}

}
