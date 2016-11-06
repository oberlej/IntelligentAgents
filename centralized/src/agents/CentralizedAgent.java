package agents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import logist.LogistSettings;
import logist.agent.Agent;
import logist.behavior.CentralizedBehavior;
import logist.config.Parsers;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;
import model.COD;

public class CentralizedAgent implements CentralizedBehavior {
	private List<Vehicle> listOfVehicles;
	private TaskSet listOfTasks;

	private final Double LOCAL_CHOICE_P = 0.3;

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private long timeout_setup;
	private long timeout_plan;

	@Override
	public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
		// this code is used to get the timeouts
		LogistSettings ls = null;
		try {
			ls = Parsers.parseSettings("config/settings_default.xml");
		} catch (Exception exc) {
			System.out.println("There was a problem loading the configuration file.");
		}

		// the setup method cannot last more than timeout_setup milliseconds
		timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
		// the plan method cannot execute more than timeout_plan milliseconds
		timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		listOfVehicles = vehicles;
		listOfTasks = tasks;
		List<Plan> plans = new ArrayList<Plan>();

		COD bestSolution = SLS();

		Plan p;
		for (Vehicle v : vehicles) {
			p = new Plan(v.homeCity());
			City currentCity = v.homeCity();
			for (Task t : bestSolution.linkedVehicleTasks.get(v)) {
				if (!currentCity.equals(t.pickupCity)) {
					createMoves(currentCity, t.pickupCity, p);
				}
				p.appendPickup(t);
				createMoves(t.pickupCity, t.deliveryCity, p);
				p.appendDelivery(t);
				currentCity = t.deliveryCity;
			}
			plans.add(p);
		}
		return plans;
	}

	private void createMoves(City source, City destination, Plan p) {
		for (City city : source.pathTo(destination)) {
			p.appendMove(city);
		}
	}

	private COD SLS() {
		COD A = selectInitialSolution();
		// System.out.println("Nb tasks: " + listOfTasks.size());
		// System.out.println(A);
		List<COD> N;

		long start_time = System.currentTimeMillis();
		int i = 0;
		do {
			i++;
			// System.out.println("in sls");
			COD oldA = A;
			N = chooseNeighbors(oldA);
			A = localChoice(N, oldA);
			if (i % 10 == 0) {
			}
		} while (start_time + timeout_plan - 300 > System.currentTimeMillis());

		return A;
	}

	private COD selectInitialSolution() {
		COD initA = new COD(listOfVehicles);
		// random distribution
		int lastVehicle = 0;
		for (Task t : listOfTasks) {
			int nbTry = 1;
			while (!initA.addTask(listOfVehicles.get(lastVehicle++), t)) {
				if (nbTry == listOfVehicles.size()) {
					System.out.println("Impossible to give task " + t + " to any vehicle");
					System.exit(-1);
				}
				nbTry++;
				if (lastVehicle == listOfVehicles.size()) {
					lastVehicle = 0;
				}
			}
			if (lastVehicle == listOfVehicles.size()) {
				lastVehicle = 0;
			}
		}

		return initA;

	}

	private COD localChoice(List<COD> N, COD oldA) {
		// System.out.println("in local choice");
		HashMap<COD, Double> bestChoicesA = new HashMap<COD, Double>();

		COD choiceA = oldA;

		bestChoicesA.put(choiceA, C(choiceA));

		for (COD A : N) {

			Double costA = C(A);

			if (costA <= bestChoicesA.get(choiceA)) {

				// equally good so add to the list
				if (costA < bestChoicesA.get(choiceA)) {
					bestChoicesA.clear();
				}
				choiceA = A;
				bestChoicesA.put(A, costA);
			}
		}

		if (Math.random() < LOCAL_CHOICE_P) {
			return (COD) bestChoicesA.keySet().toArray()[(int) (Math.random() * bestChoicesA.size())];
		}

		return oldA;
	}

	private List<COD> chooseNeighbors(COD oldA) {
		// System.out.println("in choose neighb");
		// neighbor solutions
		List<COD> N = new ArrayList<COD>();

		// change vehicle
		Vehicle vRand;
		do {
			vRand = listOfVehicles.get((int) (Math.random() * (listOfVehicles.size() - 1)));
		} while (oldA.nextTask(vRand) == null);

		for (Vehicle v : listOfVehicles) {
			if (!v.equals(vRand)) {
				if (oldA.nextTask(vRand).weight < v.capacity()) {
					COD A = changingVehicle(oldA, vRand, v);
					if (A != null) {
						N.add(A);
					}
				}
			}
		}

		// change tasks
		int length = oldA.linkedVehicleTasks.get(vRand).size();

		if (length >= 2) {
			for (int i = 0; i < length; i++) {
				for (int j = i + 1; j < length; j++) {
					// System.out.println("call changing task order");
					COD A = changingTaskOrder(oldA, vRand, i, j);
					N.add(A);
				}
			}
		}
		return N;
	}

	private COD changingVehicle(COD A, Vehicle v1, Vehicle v2) {
		// System.out.println("in changing vehicle. v1: " + v1.id() + " v2: " +
		// v2.id());
		// System.out.println("Before:\n" + A);

		COD A1 = A.clone();

		Task t = A1.nextTask(v1);
		// remove t from v1
		A1.linkedVehicleTasks.get(A1.taskInformation.get(t).vehicle).remove(t);

		// add to v2
		A1.setNextTask(v2, t);

		updateTime(A1, v1);
		updateTime(A1, v2);

		// System.out.println("After:\n" + A1);
		return A1;
	}

	private COD changingTaskOrder(COD A, Vehicle v, int i, int j) {

		COD A1 = A.clone();
		// System.out.println("in changing task order");
		// System.out.println("Before:\n" + A);

		Task t1 = A1.linkedVehicleTasks.get(v).get(i);
		Task t2 = A1.linkedVehicleTasks.get(v).get(j);

		Task tPre1 = A1.linkedVehicleTasks.get(v).get(i > 0 ? i - 1 : 0);
		Task tPre2 = A1.linkedVehicleTasks.get(v).get(j - 1);
		Task tPost1 = A1.linkedVehicleTasks.get(v).get(i + 1);

		// System.out.println("Setting:\n" + A1);
		if (tPost1 == t2) {
			A1.linkedVehicleTasks.get(v).remove(t1);
			A1.setNextTask(t2, t1, v);
		} else {
			A1.linkedVehicleTasks.get(v).remove(t2);
			A1.setNextTask(tPre2, t1, v);
			A1.linkedVehicleTasks.get(v).remove(t1);
			if (t1.equals(tPre1)) {
				// t1 was first in the list
				A1.setNextTask(v, t2);
			} else {
				A1.setNextTask(tPre1, t2, v);
			}
		}
		// System.out.println("After:\n" + A1);
		updateTime(A1, v);

		return A1;
	}

	private void updateTime(COD A, Vehicle v) {
		// System.out.println("in update time");
		// System.out.println("Before:\n" + A);
		Task t = A.nextTask(v);
		Task t2 = null;

		if (t != null) {
			A.setTime(t, 1);
			do {
				t2 = A.nextTask(t);
				// System.out.println(t2.id);
				if (t2 != null) {
					A.setTime(t2, A.time(t) + 1);
					t = t2;
				}
			} while (t2 != null);
		}
		// System.out.println("After:\n" + A);
	}

	public Double dist(Task t1, Task t2) {

		if (t1 == null || t2 == null) {
			return 0.0;
		}

		return t1.deliveryCity.distanceTo(t2.pickupCity);
	}

	public Double dist(Vehicle v, Task t) {

		if (v == null || t == null) {
			return 0.0;
		}

		return v.homeCity().distanceTo(t.pickupCity);
	}

	public Double length(Task task) {

		if (task == null) {
			return 0.0;
		}

		return task.pickupCity.distanceTo(task.deliveryCity);
	}

	public Double C(COD cod) {

		Double sum = 0.0;

		for (Vehicle v : cod.linkedVehicleTasks.keySet()) {
			Task nt = cod.nextTask(v);
			sum += (dist(v, nt) + length(nt)) * v.costPerKm();
		}

		for (Task t : cod.taskInformation.keySet()) {
			Task nt = cod.nextTask(t);
			sum += (dist(t, nt) + length(nt)) * cod.vehicle(t).costPerKm();
		}

		return sum;
	}

}
