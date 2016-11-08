package model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;

import logist.simulation.Vehicle;
import logist.task.Task;

public class COD {

	public HashMap<Vehicle, LinkedList<VAction>> linkedVehicleTasks;
	public HashMap<Task, Vehicle> taskVehiclePair;

	public COD(List<Vehicle> listOfVehicles) {
		super();
		linkedVehicleTasks = new HashMap<Vehicle, LinkedList<VAction>>();
		for (Vehicle v : listOfVehicles) {
			linkedVehicleTasks.put(v, new LinkedList<VAction>());
		}
		taskVehiclePair = new HashMap<Task, Vehicle>();
	}

	public COD(COD oldCOD) {
		super();
		linkedVehicleTasks = new HashMap<Vehicle, LinkedList<VAction>>();
		for (Entry<Vehicle, LinkedList<VAction>> e : oldCOD.linkedVehicleTasks.entrySet()) {
			LinkedList<VAction> list = new LinkedList<VAction>();
			for (VAction va : e.getValue()) {
				list.add(va.clone());
			}
			linkedVehicleTasks.put(e.getKey(), list);
		}
		taskVehiclePair = new HashMap<Task, Vehicle>();
		taskVehiclePair.putAll(oldCOD.taskVehiclePair);
	}

	@Override
	public COD clone() {
		return new COD(this);
	}

	public VAction nextVAction(VAction va) {
		Vehicle vOrig = taskVehiclePair.get(va.task);
		ListIterator<VAction> i = linkedVehicleTasks.get(vOrig).listIterator();

		while (i.hasNext()) {
			if (va == i.next()) {
				return i.hasNext() ? i.next() : null;
			}
		}
		return null;
	}

	public void setNextVAction(VAction vaOrig, VAction vaNext, Vehicle v) {
		if (vaOrig == null) {
			System.out.println("this needs to be fixed in changing task order bc tpre1 is set to null");
			System.exit(-1);
		}
		if (vaNext == null) {
			// can happen if we call nextTask on a last task. nextTask will then
			// return null and so tNext will be null here
			return;
		}

		if (!taskVehiclePair.get(vaOrig.task).equals(taskVehiclePair.get(vaNext.task))) {
			// shouldnt happen anymore
			System.out.println("taO " + vaOrig.task.id + " - taN " + vaNext.task.id);
			System.exit(-1);
			// taskVehiclePair.put(vaOrig.task, v);
			// taskVehiclePair.put(vaNext.task, v);

		}

		ListIterator<VAction> i = linkedVehicleTasks.get(v).listIterator();

		while (i.hasNext()) {
			if (vaOrig == i.next()) {
				int taskIndex = i.nextIndex();
				linkedVehicleTasks.get(v).add(taskIndex, vaNext);
				break;
			}
		}
	}

	public VAction nextVAction(Vehicle v) {
		if (linkedVehicleTasks.get(v).size() > 0) {
			// System.out.println(this);
			VAction va = linkedVehicleTasks.get(v).getFirst();
			if (!va.isPickup()) {
				System.out.println("first va wasnt pickup!");
				System.exit(-1);
			}
			return va;
		}
		return null;
	}

	public Task nextTask(Vehicle v) {
		if (linkedVehicleTasks.get(v).size() > 0) {
			VAction pickup = linkedVehicleTasks.get(v).getFirst();
			if (!pickup.isPickup()) {
				System.out.println("first va wasnt pickup!");
				System.exit(-1);
			}
			return pickup.task;
		}
		return null;
	}

	public void setNextVAction(Vehicle vOrig, VAction vaNext) {
		if (vaNext != null) {
			linkedVehicleTasks.get(vOrig).add(0, vaNext);
			taskVehiclePair.put(vaNext.task, vOrig);
		}
	}

	// public int time(Task t) {
	// return taskInformation.get(t).time;
	// }

	// public void setTime(Task t, int newTime) {
	// taskInformation.get(t).time = newTime;
	// }

	public Vehicle vehicle(VAction va) {
		return taskVehiclePair.get(va.task);
	}

	public void setVehicle(VAction va, Vehicle v) {
		taskVehiclePair.put(va.task, v);
	}

	public boolean addTask(Vehicle v, Task t) {
		if (v.capacity() >= t.weight) {
			VAction pickup = new VPickupAction(t, v.capacity() - t.weight);
			VAction delivery = new VDeliveryAction(t, v.capacity());
			taskVehiclePair.put(t, v);
			linkedVehicleTasks.get(v).add(0, delivery);
			linkedVehicleTasks.get(v).add(0, pickup);
			return true;
		}
		return false;
	}

	public boolean updateCapacity(Vehicle v, int start) {
		if (linkedVehicleTasks.get(v).isEmpty()) {
			return true;
		}
		Iterator<VAction> i;
		VAction pre = null;
		VAction next = null;
		if (start > 0) {
			i = linkedVehicleTasks.get(v).listIterator(start - 1);
			pre = i.next();
			next = i.next();

			if (!next.updateCapacity(pre.remainingVCapacity)) {
				return false;
			}
		} else {
			i = linkedVehicleTasks.get(v).listIterator(start);
			next = i.next();

			next.updateCapacity(v.capacity());
		}

		while (i.hasNext()) {
			pre = next;
			next = i.next();
			if (!next.updateCapacity(pre.remainingVCapacity)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String toString() {

		StringBuilder s = new StringBuilder();
		Vehicle v;
		for (Entry<Vehicle, LinkedList<VAction>> e : linkedVehicleTasks.entrySet()) {
			v = e.getKey();

			s.append("v" + v.id());
			for (VAction va : e.getValue()) {
				s.append("->" + va);
			}
			s.append("\n");
		}
		return s.toString();
	}
}