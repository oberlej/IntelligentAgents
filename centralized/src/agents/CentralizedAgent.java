package agents;

import java.util.ArrayList;
import java.util.List;

import logist.agent.Agent;
import logist.behavior.CentralizedBehavior;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import model.COD;

public class CentralizedAgent implements CentralizedBehavior {
	List<Vehicle> listOfVehicles;
	
	
	@Override
	public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		listOfVehicles = vehicles;
		
		COD bestSolution = SLS();
		
		// TODO Auto-generated method stub
		return null;
	}

	private COD SLS(){
		COD A = selectInitialSolution();
		List<COD> N = new ArrayList<COD>();
		
		while (true) {
			COD oldA = A;
			N = chooseNeighbors(oldA);
			A = localChoice(N);
			break;
		}
		
		return A;
	}
	
	private COD selectInitialSolution(){
		COD initA = new COD();
		
		return initA;
	}
	
	private COD localChoice(List<COD> N){
		COD choiceA = null;
		
		return choiceA;
	}
	
	private List<COD> chooseNeighbors(COD oldA){
//		neighbor solutions
		List<COD> N = new ArrayList<COD>();
		
//		change vehicle
		Vehicle vRand = listOfVehicles.get((int)(Math.random()*listOfVehicles.size()-1));
		
		for (Vehicle v: listOfVehicles) {
			Task t = oldA.nextTask(v);
			
			if (t.weight < v.capacity()){
				COD A = changingVehicle(oldA, vRand, v);
				N.add(A);
			}
		}
		
//		change tasks		
		int length = oldA.linkedVehicleTasks.get(vRand).size();
		
		if (length >= 2) {
			for (int i = 0; i < length; i++){
				for (int j = i+1; j < length; j++){
					COD A = changingTaskOrder(oldA, vRand, i, j);
					N.add(A);
				}
			}
		}
		return N;
	}
	
	private COD changingVehicle(COD A, Vehicle v1, Vehicle v2){
		COD A1 = A;
		Task t = A.nextTask(v1);
		A1.setNextTask(v1, A1.nextTask(t));
		A1.setNextTask(t, A1.nextTask(v2));
		A1.setNextTask(v2, t);
		
		updateTime(A1, v1);
		updateTime(A1, v2);
		A1.setVehicle(t, v2);
		
		return A1;
	}
	private COD changingTaskOrder(COD A, Vehicle v, int i, int j){
		COD A1 = A;
		
		Task tPre1 = null;
		Task t1 = A1.nextTask(v);
		int count = 1;
		
		while (count < i){
			tPre1 = t1;
			t1 = A1.nextTask(t1);
			count++;
		}
		
		Task tPost1 = A1.nextTask(t1);
		Task tPre2 = t1;
		Task t2 = A1.nextTask(tPre2);
		count++;
		
		while(count < j){
			tPre2 = t2;
			t2 = A1.nextTask(t2);
			count++;
		}
		
		Task tPost2 = A1.nextTask(t2);
		
		if (tPost1 == t2) {
			A1.setNextTask(tPre1, t2);
			A1.setNextTask(t2, t1);
			A1.setNextTask(t1, tPost2);
		} else {
			A1.setNextTask(tPre1, t2);
			A1.setNextTask(tPre2, t1);
			A1.setNextTask(t2, tPost1);
			A1.setNextTask(t1, tPost2);
		}
		
		updateTime(A1, v);
		
		return A1;
	}
	
	private void updateTime(COD A, Vehicle v){
		
		Task t = A.nextTask(v);
		Task t2 = null;
		
		if (t != null){
			A.setTime(t, 1);
			
			while (t != null){
				t2 = A.nextTask(t);
				if (t2 != null) {
					A.setTime(t2, A.time(t) + 1);
					t2 = t;
				}
			}
		}
	}
	
	public Double dist(Task t1, Task t2){
		
		if (t1 == null || t2 == null){
			return 0.0;
		}
		
		return t1.deliveryCity.distanceTo(t2.pickupCity);
	}
	
	public Double dist(Vehicle v, Task t){
		
		if (v == null || t == null){
			return 0.0;
		}
		
		return v.homeCity().distanceTo(t.pickupCity);
	}
	
	public Double length(Task task){
		
		if (task == null){
			return 0.0;
		}
		
		return task.pickupCity.distanceTo(task.deliveryCity);
	}
	
	public int C(COD cod){
		
		int sum = 0;
		
		for (Vehicle v: cod.linkedVehicleTasks.keySet()) {
			Task nt = cod.nextTask(v); 
			sum += (dist(v, nt) + length(nt)) * v.costPerKm();
		}
		
		for (Task t: cod.taskInformation.keySet()) {
			Task nt = cod.nextTask(t); 
			sum += (dist(t, nt) + length(nt)) * cod.vehicle(t).costPerKm();
		}
		
		return sum;
	}
	
	
}
