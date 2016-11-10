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
import model.VAction;
import model.VDeliveryAction;
import model.VPickupAction;

public class CentralizedAgent implements CentralizedBehavior {
	private List<Vehicle> listOfVehicles;
	private TaskSet listOfTasks;

	private final Double LOCAL_CHOICE_P = 0.5;

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
		double sum = 0;
		for (Vehicle v : vehicles) {
			p = new Plan(v.homeCity());
			City currentCity = v.homeCity();
			for (VAction va : bestSolution.linkedVehicleTasks.get(v)) {
				if (!currentCity.equals(va.getCity())) {
					sum += currentCity.distanceTo(va.getCity()) * v.costPerKm();
					createMoves(currentCity, va.getCity(), p);
				}

				p.append(va.getAction());
				currentCity = va.getCity();
			}
			plans.add(p);
		}
		System.out.println("Sum for best sol=" + sum);
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

		System.out.println(N.size());

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
			COD choice = (COD) bestChoicesA.keySet().toArray()[(int) (Math.random() * bestChoicesA.size())];
			System.out.println(choice);
			System.out.println(bestChoicesA.get(choice));
			return choice;
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
		} while (oldA.nextVAction(vRand) == null);

		for (Vehicle v : listOfVehicles) {
			if (!v.equals(vRand)) {
				if (oldA.nextVAction(vRand).task.weight < v.capacity()) {
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
					COD A = changingVActionOrder(oldA, vRand, i, j);
					if (A != null) {
						N.add(A);
					}
				}
			}
		}
		return N;
	}

	private COD changingVehicle(COD A, Vehicle v1, Vehicle v2) {
		// System.out.println("in changing vehicle. from v" + v1.id() + " to v"
		// + v2.id());
		// System.out.println("Before:\n" + A);

		COD A1 = A.clone();

		Task t = A1.nextTask(v1);
		// System.out.println("task t" + t.id);
		VAction delivery = new VDeliveryAction(t, 0);
		VAction pickup = new VPickupAction(t, 0);
		// remove both vaction from v1
		A1.linkedVehicleTasks.get(v1).remove(delivery);
		A1.linkedVehicleTasks.get(v1).remove(pickup);
		// System.out.println("After remove:\n" + A1);
		// add to v2
		A1.addTask(v2, t);

		A1.updateCapacity(v1, 0);

		// System.out.println("After:\n" + A1);
		// System.exit(0);
		return A1.updateCapacity(v2, 0) ? A1 : null;
	}

	private COD changingVActionOrder(COD A, Vehicle v, int i, int j) {

		COD A1 = A.clone();
		// System.out.println("in changing task order " + i + "-" + j);
		//
		// System.out.println("Before:\n" + A);

		VAction va1 = A1.linkedVehicleTasks.get(v).get(i);
		VAction va2 = A1.linkedVehicleTasks.get(v).get(j);
		// System.out.println(va1 + " <-> " + va2);
		if (va1.task.equals(va2.task)) {
			return null;
		}

		// verify that we pickup va1.task before delivering it even after
		// changing the order
		if (va1.isPickup()) {
			VAction partnerVa1 = new VDeliveryAction(va1.task, 0);
			int indexP1 = A1.linkedVehicleTasks.get(v).indexOf(partnerVa1);
			// System.out.println("indexp1=" + indexP1);
			if (indexP1 <= j) {
				return null;
			}
		}

		if (!va2.isPickup()) {
			VAction partnerVa2 = new VPickupAction(va2.task, 0);
			int indexP2 = A1.linkedVehicleTasks.get(v).indexOf(partnerVa2);
			if (indexP2 >= i) {
				return null;
			}
		}

		VAction vaPre1 = A1.linkedVehicleTasks.get(v).get(i > 0 ? i - 1 : 0);
		VAction vaPre2 = A1.linkedVehicleTasks.get(v).get(j - 1);
		VAction vaPost1 = A1.linkedVehicleTasks.get(v).get(i + 1);

		// System.out.println("Setting:\n" + A1);
		if (vaPost1.equals(va2)) {
			A1.linkedVehicleTasks.get(v).remove(va1);
			A1.setNextVAction(va2, va1, v);
		} else {

			A1.linkedVehicleTasks.get(v).remove(va2);
			A1.setNextVAction(vaPre2, va1, v);
			A1.linkedVehicleTasks.get(v).remove(va1);
			if (va1.equals(vaPre1)) {
				// t1 was first in the list
				A1.setNextVAction(v, va2);
			} else {
				A1.setNextVAction(vaPre1, va2, v);
			}
		}
		// System.out.println("After:\n" + A1);
		// System.exit(0);
		return A1.updateCapacity(v, i) ? A1 : null;
	}

	// private void updateTime(COD A, Vehicle v) {
	// // System.out.println("in update time");
	// // System.out.println("Before:\n" + A);
	// Task t = A.nextTask(v);
	// Task t2 = null;
	//
	// if (t != null) {
	// A.setTime(t, 1);
	// do {
	// t2 = A.nextTask(t);
	// // System.out.println(t2.id);
	// if (t2 != null) {
	// A.setTime(t2, A.time(t) + 1);
	// t = t2;
	// }
	// } while (t2 != null);
	// }
	// // System.out.println("After:\n" + A);
	// }

	// public Double dist(VAction va1, VAction va2) {
	//
	// if (va1 == null || va2 == null) {
	// return 0.0;
	// }
	//
	// return va1.getCity().distanceTo(va2.getCity());
	// }
	//
	// public Double dist(Vehicle v, VAction va) {
	//
	// if (v == null || va == null) {
	// return 0.0;
	// }
	//
	// return v.homeCity().distanceTo(va.getCity());
	// }
	//
	// public Double length(VAction task) {
	//
	// if (task == null) {
	// return 0.0;
	// }
	//
	// return task.pickupCity.distanceTo(task.deliveryCity);
	// }

	public Double C(COD cod) {

		Double sum = 0.0;

		for (Vehicle v : listOfVehicles) {
			City currentCity = v.homeCity();
			for (VAction va : cod.linkedVehicleTasks.get(v)) {
				if (!currentCity.equals(va.getCity())) {
					sum += currentCity.distanceTo(va.getCity()) * v.costPerKm();
				}
				currentCity = va.getCity();
			}
		}
		return sum;
	}

}
