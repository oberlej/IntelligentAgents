package model;

import logist.plan.Action;
import logist.plan.Action.Pickup;
import logist.task.Task;
import logist.topology.Topology.City;

public class VPickupAction extends VAction {

	public VPickupAction(Task t, int remainingVCapacity) {
		super(t, remainingVCapacity);
		isPickup = true;
	}

	@Override
	public boolean updateCapacity(int initCapactiy) {
		remainingVCapacity = initCapactiy - task.weight;
		return remainingVCapacity >= 0;

	}

	@Override
	public VAction clone() {
		return new VPickupAction(task, remainingVCapacity);
	}

	@Override
	public boolean isPickup() {
		return true;
	}

	@Override
	public City getCity() {
		return task.pickupCity;
	}

	@Override
	public Action getAction() {
		return new Pickup(task);
	}
}