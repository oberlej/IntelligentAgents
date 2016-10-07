package model;

import logist.topology.Topology.City;

public class AgentAction {
	private City destination;
	private boolean tackingPackage;

	/**
	 * Constructor using fields
	 *
	 * @param destination
	 * @param tackingPackage
	 */
	public AgentAction(City destination, boolean tackingPackage) {
		super();
		this.destination = destination;
		this.tackingPackage = tackingPackage;
	}

	/**
	 * @return the destination
	 */
	public City getDestination() {
		return destination;
	}

	/**
	 * @return the tackingPackage
	 */
	public boolean isTackingPackage() {
		return tackingPackage;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		AgentAction other = (AgentAction) obj;
		if (destination == null) {
			if (other.destination != null) {
				return false;
			}
		} else if (!destination.equals(other.destination)) {
			return false;
		}
		if (tackingPackage != other.tackingPackage) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		if (tackingPackage) {
			return "take P and move to " + destination;
		} else {
			return "don't take P and move to " + destination;
		}
	}
}
