package model;

import logist.plan.Action;
import logist.plan.Action.Delivery;
import logist.task.Task;
import logist.topology.Topology.City;

public class VDeliveryAction extends VAction {

	public VDeliveryAction(Task t, int remainingVCapacity) {
		super(t, remainingVCapacity);
		isPickup = false;
	}

	@Override
	public boolean updateCapacity(int initCapactiy) {
		remainingVCapacity = initCapactiy + task.weight;
		return true;
	}

	@Override
	public VAction clone() {
		return new VDeliveryAction(task, remainingVCapacity);
	}

	@Override
	public boolean isPickup() {
		return false;
	}

	@Override
	public City getCity() {
		return task.deliveryCity;
	}

	@Override
	public Action getAction() {
		return new Delivery(task);
	}
}