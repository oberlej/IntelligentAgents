package model;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;

import logist.simulation.Vehicle;
import logist.task.Task;

public class COD {

	public HashMap<Vehicle, LinkedList<Task>> linkedVehicleTasks;
	public HashMap<Task, TaskInformation> taskInformation;

	public COD(List<Vehicle> listOfVehicles) {
		super();
		linkedVehicleTasks = new HashMap<Vehicle, LinkedList<Task>>();
		for (Vehicle v : listOfVehicles) {
			linkedVehicleTasks.put(v, new LinkedList<Task>());
		}
		taskInformation = new HashMap<Task, TaskInformation>();
	}

	public COD(COD oldCOD) {
		super();
		HashMap<Vehicle, LinkedList<Task>> lVT = oldCOD.linkedVehicleTasks;
		HashMap<Task, TaskInformation> tI = oldCOD.taskInformation;
		linkedVehicleTasks = new HashMap<Vehicle, LinkedList<Task>>();
		taskInformation = new HashMap<Task, TaskInformation>();
		Vehicle v;
		for (Entry<Vehicle, LinkedList<Task>> e : lVT.entrySet()) {
			v = e.getKey();
			linkedVehicleTasks.put(v, new LinkedList<Task>());
			linkedVehicleTasks.get(v).addAll(lVT.get(v));
			for (Task t : linkedVehicleTasks.get(v)) {
				taskInformation.put(t, tI.get(t).clone());
			}
		}
	}

	@Override
	public COD clone() {
		return new COD(this);
	}

	public Task nextTask(Task t) {
		Vehicle vOrig = taskInformation.get(t).vehicle;
		ListIterator<Task> i = linkedVehicleTasks.get(vOrig).listIterator();

		while (i.hasNext()) {
			if (t == i.next()) {
				return i.hasNext() ? i.next() : null;
			}
		}
		return null;
	}

	public void setNextTask(Task tOrig, Task tNext, Vehicle v) {
		if (tOrig == null) {
			System.out.println("this needs to be fixed in changing task order bc tpre1 is set to null");
		}
		if (tNext == null) {
			// can happen if we call nextTask on a last task. nextTask will then
			// return null and so tNext will be null here
			return;
		}

		TaskInformation ti1 = taskInformation.get(tOrig);
		TaskInformation ti2 = taskInformation.get(tNext);
		if (!ti1.vehicle.equals(ti2.vehicle)) {
			ti1.vehicle = v;
			ti2.vehicle = v;
		}

		ListIterator<Task> i = linkedVehicleTasks.get(v).listIterator();
		while (i.hasNext()) {
			if (tOrig == i.next()) {
				int taskIndex = i.nextIndex();
				linkedVehicleTasks.get(v).add(taskIndex, tNext);
				break;
			}
		}
	}

	public Task nextTask(Vehicle v) {
		if (linkedVehicleTasks.get(v).size() > 0) {
			return linkedVehicleTasks.get(v).getFirst();
		}
		return null;
	}

	public void setNextTask(Vehicle vOrig, Task tNext) {
		if (tNext != null) {
			linkedVehicleTasks.get(vOrig).add(0, tNext);
			taskInformation.get(tNext).vehicle = vOrig;
		}
	}

	public int time(Task t) {
		return taskInformation.get(t).time;
	}

	public void setTime(Task t, int newTime) {
		taskInformation.get(t).time = newTime;
	}

	public Vehicle vehicle(Task t) {
		return taskInformation.get(t).vehicle;
	}

	public void setVehicle(Task t, Vehicle v) {
		taskInformation.get(t).vehicle = v;
	}

	public boolean addTask(Vehicle v, Task t) {
		if (v.capacity() >= t.weight) {
			if (linkedVehicleTasks.get(v).size() > 0) {
				taskInformation.put(t,
				        new TaskInformation(taskInformation.get(linkedVehicleTasks.get(v).getLast()).time + 1, v));
			} else {
				taskInformation.put(t, new TaskInformation(1, v));
			}
			linkedVehicleTasks.get(v).add(t);
			return true;
		}
		return false;
	}

	@Override
	public String toString() {

		StringBuilder s = new StringBuilder();
		Vehicle v;
		for (Entry<Vehicle, LinkedList<Task>> e : linkedVehicleTasks.entrySet()) {
			v = e.getKey();

			s.append("v" + v.id());
			for (Task t : e.getValue()) {
				// s.append("->t" + t.id);
				s.append("->t" + t.id + "(" + taskInformation.get(t).time + ", v" + taskInformation.get(t).vehicle.id()
				        + ")");
			}
			s.append("\n");
		}
		return s.toString();
	}
}