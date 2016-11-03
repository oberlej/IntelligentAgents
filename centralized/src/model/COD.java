package model;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;

import logist.simulation.Vehicle;
import logist.task.Task;

public class COD {

	public HashMap<Vehicle, LinkedList<Task>> linkedVehicleTasks;
	public HashMap<Task, TaskInformation> taskInformation;
	
	public Task nextTask(Task t){
		
		Vehicle vOrig = taskInformation.get(t).vehicle;
		
		ListIterator<Task> i = linkedVehicleTasks.get(vOrig).listIterator();
		
		while(i.hasNext()){
			if (t == i.next()){
				return i.next();
			}
		}
		
		return null;
	}
	
	public void setNextTask(Task tOrig, Task tNext){
		
		Vehicle vOrig = taskInformation.get(tOrig).vehicle;
		
		ListIterator<Task> i = linkedVehicleTasks.get(vOrig).listIterator();
		
		while(i.hasNext()){
			if (tOrig == i.next()){
				linkedVehicleTasks.get(vOrig).add(i.nextIndex(), tNext);
			}
		}
	}
	
	public Task nextTask(Vehicle v){
		
		return linkedVehicleTasks.get(v).getFirst();
		
	}
	
	public void setNextTask(Vehicle vOrig, Task tNext){
				
		linkedVehicleTasks.get(vOrig).add(1, tNext);
		
	}
	
	public int time(Task t){
		
		return taskInformation.get(t).time;
	}
	
	public void setTime(Task t, int newTime){
		
		taskInformation.get(t).time = newTime;
	}
	
	
	public Vehicle vehicle(Task t){
		
		return taskInformation.get(t).vehicle;
	}
	
	public void setVehicle(Task t, Vehicle v){
		
		taskInformation.get(t).vehicle = v;
	}
	
}