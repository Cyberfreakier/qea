package monitoring.impl.monitors;

import java.util.IdentityHashMap;

import monitoring.impl.IncrementalMonitor;
import monitoring.impl.configs.NonDetConfig;
import structure.impl.SimpleNonDeterministicQEA;
import structure.impl.Verdict;
import exceptions.ShouldNotHappenException;

/**
 * A small-step monitor for a non-deterministic simple QEA
 * 
 * @author Helena Cuenca
 * @author Giles Reger
 */
public class IncrementalNonDetQEAMonitor extends
		IncrementalMonitor<SimpleNonDeterministicQEA> {

	private IdentityHashMap<Object, NonDetConfig> bindings;

	public IncrementalNonDetQEAMonitor(SimpleNonDeterministicQEA qea) {
		super(qea);
		bindings = new IdentityHashMap<>();
		bindingsInNonFinalStateCount = 0;
		bindingsInFinalStateCount = 0;
	}

	@Override
	public Verdict step(int eventName, Object[] args) {
		if(args.length>1) throw new ShouldNotHappenException("Was only expecting one parameter");
		return step(eventName,args[0]);
	}

	@Override
	public Verdict step(int eventName, Object param1) {

		boolean existingBinding = false;
		NonDetConfig config;

		// Determine if the value received corresponds to an existing binding
		if (bindings.containsKey(param1)) { // Existing quantified variable
											// binding

			// Get current configuration for the binding
			config = bindings.get(param1);

			// Assign flag for counters update
			existingBinding = true;

		} else { // New quantified variable binding

			// Create configuration for the new binding
			config = new NonDetConfig();
		}

		
		// Flag needed to update counters later
		boolean startConfigFinal = qea.containsFinalState(config);

		// Compute next configuration
		config = qea.getNextStates(config, eventName);

		// Flag needed to update counters later
		boolean endConfigFinal = qea.containsFinalState(config);

		// Update/add configuration for the binding
		bindings.put(param1, config);

		// If applicable, update counters
		if (existingBinding) {
			if (startConfigFinal && !endConfigFinal) {
				bindingsInNonFinalStateCount++;
				bindingsInFinalStateCount--;
			} else if (!startConfigFinal && endConfigFinal) {
				bindingsInNonFinalStateCount--;
				bindingsInFinalStateCount++;
			}
		} else {
			if (endConfigFinal) {
				bindingsInFinalStateCount++;
			} else {
				bindingsInNonFinalStateCount++;
			}
		}

		// According to the quantification of the variable, return verdict
		if (qea.isQuantificationUniversal() && allBindingsInFinalState()
				|| !qea.isQuantificationUniversal()
				&& existsOneBindingInFinalState()) {
			return Verdict.WEAK_SUCCESS;
		}
		return Verdict.WEAK_FAILURE;
	}

	@Override
	public Verdict step(int eventName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Verdict end() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * Override toString to print current map to configurations
	 */
	@Override
	public String toString(){
		String ret = "Map:\n";
		for(IdentityHashMap.Entry<Object,NonDetConfig> entry : bindings.entrySet()){
			ret += entry.getKey()+"\t->\t"+entry.getValue()+"\n";
		}
		return ret;
	}
	
}