package agents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Action;
import logist.plan.Action.Delivery;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;
import model.State;

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

	public static double costPerKm;

	/* the planning class */
	Algorithm algorithm;

	boolean isJumboTaskSet = false;

	TaskSet pickedUpTasks = null;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		this.topology = topology;
		this.td = td;
		this.agent = agent;
		costPerKm = agent.vehicles().get(0).costPerKm();

		String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());
	}

	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		System.out.println("Agent " + (agent.id() + 1) + ": " + agent.name());
		Plan plan;
		// System.out.println("tasks carried:" + vehicle.getCurrentTasks());
		// System.out.println("tasks available:" + tasks);

		switch (algorithm) {
		case ASTAR:
			plan = aStar(vehicle, tasks);
			break;
		case BFS:
			plan = BFS(vehicle, tasks);
			break;
		default:
			throw new AssertionError("Should not happen.");
		}
		return plan;

	}

	private Plan aStar(Vehicle vehicle, TaskSet tasks) {

		List<State> open = new ArrayList<State>();
		// add root
		open.add(new State(tasks, vehicle.getCurrentTasks(), vehicle.getCurrentCity(), 0, vehicle.capacity(),
		        new State(null, null, null, 0, 0, null)));

		State current;
		int nbStates = 1;
		State newNeighbor;

		current = open.get(0);
		open.remove(0);

		while (!current.getAvailableTasks().isEmpty() || !current.getPickedUpTasks().isEmpty()) {
			// System.out.println(current);
			// add new neighbor states

			// pick up a new task if possible
			for (Task t : current.getAvailableTasks()) {
				newNeighbor = current.clone();
				if (newNeighbor.pickUpTask(t)) {
					// task was picked up
					newNeighbor.setParentState(current);
					newNeighbor.setFcost(newNeighbor.getCost() + h(newNeighbor));
					open.add(newNeighbor);
					nbStates++;
				}
			}

			// deliver one of our picked up tasks
			for (Task t : current.getPickedUpTasks()) {
				newNeighbor = current.clone();
				if (newNeighbor.deliverTask(t)) {
					// task was delivered
					newNeighbor.setParentState(current);
					newNeighbor.setFcost(newNeighbor.getCost() + h(newNeighbor));
					open.add(newNeighbor);
					nbStates++;
				}
			}

			// System.out.println("before sort");
			// for (State s : open) {
			// System.out.println(s);
			// }
			Collections.sort(open);
			// System.out.println("after sort");
			// for (State s : open) {
			// System.out.println(s);
			// }
			current = open.get(0);
			open.remove(0);
		}

		System.out.println("While loop done after " + nbStates);
		// System.out.println("final state:" + current);
		// create actions
		List<Action> actions = new ArrayList<Action>();
		do {
			Task t;
			if (current.hasPickedUpTaskInCurrentCity()) {
				// find task that has been picked up in the current.city
				t = (Task) TaskSet
				        .intersectComplement(current.getParentState().getAvailableTasks(), current.getAvailableTasks())
				        .toArray()[0];
				actions.add(new Pickup(t));
			} else {
				// find task that has been delivered in the current.city
				t = (Task) TaskSet
				        .intersectComplement(current.getParentState().getPickedUpTasks(), current.getPickedUpTasks())
				        .toArray()[0];
				actions.add(new Delivery(t));
			}
			actions.addAll(createReverseMoves(current.getParentState().getCurrentCity(), current.getCurrentCity()));

			current = current.getParentState();
		} while (current != null && current.getParentState() != null);
		Collections.reverse(actions);
		Plan p = new Plan(vehicle.getCurrentCity(), actions);
		// System.out.println("Agent" + agent.id() + "\n" + p);
		return p;
	}

	private double h(State s) {
		double cost = 0;
		for (Task t : s.getAvailableTasks()) {
			cost += t.pickupCity.distanceTo(t.deliveryCity) * costPerKm;
		}
		double avCost = 0;
		for (Task t : s.getPickedUpTasks()) {
			// System.out.println(s.getCurrentCity().distanceTo(t.deliveryCity));
			// cost += s.getCurrentCity().distanceTo(t.deliveryCity) *
			// costPerKm;
			// avCost += s.getCurrentCity().distanceTo(t.deliveryCity);
			cost += 800;
		}
		// cost += avCost > 0 ? avCost / s.getPickedUpTasks().size() : 0;
		return cost;
	}

	private Plan BFS(Vehicle vehicle, TaskSet tasks) {
		List<State> queue = new ArrayList<State>();
		// add root
		queue.add(new State(tasks, vehicle.getCurrentTasks(), vehicle.getCurrentCity(), 0, vehicle.capacity(), null));

		State bestFinalState = new State(null, null, null, Integer.MAX_VALUE, 0, null);

		int nbStates = 1;
		State newNeighbor;
		State current;
		while (!queue.isEmpty() && bestFinalState.getCost() == Integer.MAX_VALUE) {
			// System.out.println("in while");
			current = queue.get(0);
			queue.remove(0);
			// add new neighbor states

			// pick up a new task if possible
			for (Task t : current.getAvailableTasks()) {
				newNeighbor = current.clone();
				if (newNeighbor.pickUpTask(t)) {
					// task was picked up
					if (newNeighbor.getCost() < bestFinalState.getCost()) {
						newNeighbor.setParentState(current);
						queue.add(newNeighbor);
						nbStates++;
					}
				}
			}
			// deliver one of our picked up tasks
			for (Task t : current.getPickedUpTasks()) {
				newNeighbor = current.clone();
				if (newNeighbor.deliverTask(t)) {
					// task was delivered
					if (newNeighbor.getCost() < bestFinalState.getCost()) {
						newNeighbor.setParentState(current);
						if (newNeighbor.getAvailableTasks().isEmpty() && newNeighbor.getPickedUpTasks().isEmpty()) {
							// we are in a final state s we don't need to add it
							// to
							// the queue
							// System.out.println("final state");
							if (newNeighbor.getCost() < bestFinalState.getCost()) {
								bestFinalState = newNeighbor;
							}
						} else {
							queue.add(newNeighbor);
							nbStates++;
						}
					}
				}
			}
		}
		// System.out.println("While loop done after " + nbStates);
		current = bestFinalState;
		// create actions
		List<Action> actions = new ArrayList<Action>();
		do {
			Task t;
			if (current.hasPickedUpTaskInCurrentCity()) {
				// find task that has been picked up in the current.city
				t = (Task) TaskSet
				        .intersectComplement(current.getParentState().getAvailableTasks(), current.getAvailableTasks())
				        .toArray()[0];
				actions.add(new Pickup(t));
			} else {
				// find task that has been delivered in the current.city
				t = (Task) TaskSet
				        .intersectComplement(current.getParentState().getPickedUpTasks(), current.getPickedUpTasks())
				        .toArray()[0];
				actions.add(new Delivery(t));
			}
			actions.addAll(createReverseMoves(current.getParentState().getCurrentCity(), current.getCurrentCity()));

			current = current.getParentState();
		} while (current != null && current.getParentState() != null);
		Collections.reverse(actions);
		return new Plan(vehicle.getCurrentCity(), actions);
	}

	private List<Action> createReverseMoves(City source, City destination) {
		List<Action> moves = new ArrayList<Action>();
		if (!source.equals(destination)) {
			for (City city : source.pathTo(destination)) {
				moves.add(new Move(city));
			}
			Collections.reverse(moves);
		}
		return moves;
	}

	@Override
	public void planCancelled(TaskSet carriedTasks) {
	}

}
