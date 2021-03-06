package qea.monitoring.impl.monitors;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import qea.monitoring.impl.GarbageMode;
import qea.monitoring.impl.RestartMode;
import qea.monitoring.impl.configs.NonDetConfig;
import qea.structure.impl.other.Verdict;
import qea.structure.impl.qeas.QVar1_FVar_NonDet_FixedQVar_QEA;
import qea.util.EagerGarbageHashMap;
import qea.util.IgnoreIdentityWrapper;
import qea.util.IgnoreWrapper;
import qea.util.WeakIdentityHashMap;

/**
 * 
 * @author Helena Cuenca
 * @author Giles Reger
 */
public class Incr_QVar1_FVar_NonDet_FixedQVar_QEAMonitor extends
		Abstr_Incr_QVar1_FVar_QEAMonitor<QVar1_FVar_NonDet_FixedQVar_QEA> {

	private Map<Object, NonDetConfig> bindings;
	private final HashSet<Object> strong;

	public Incr_QVar1_FVar_NonDet_FixedQVar_QEAMonitor(RestartMode restart,
			GarbageMode garbage, QVar1_FVar_NonDet_FixedQVar_QEA qea) {
		super(restart, garbage, qea);
		switch (garbage) {
		case UNSAFE_LAZY:
		case OVERSAFE_LAZY:
		case LAZY:
			bindings = new WeakIdentityHashMap<Object,NonDetConfig>();
			break;
		case EAGER:
			bindings = new EagerGarbageHashMap<NonDetConfig>();
			break;
		case NONE:
			bindings = new IdentityHashMap<Object,NonDetConfig>();
		}
		if (restart == RestartMode.IGNORE && garbage != GarbageMode.EAGER) {
			bindings = new IgnoreIdentityWrapper<Object,NonDetConfig>(bindings);
		}
		strong = new HashSet<Object>();
	}

	@Override
	public Verdict step(int eventName, Object[] args) {

		if (saved != null) {
			if (!restart()) {
				return saved;
			}
		}

		//printEvent(eventName,args);
		
		boolean existingBinding = false;
		boolean startConfigFinal = false;
		NonDetConfig config;

		// Obtain the value for the quantified variable
		Object qVarValue = args[0];

		// Determine if the value received corresponds to an existing binding
		if (bindings.containsKey(qVarValue)) { // Existing quantified
												// variable binding
			// Get current configuration for the binding
			config = bindings.get(qVarValue);
			// if config=null it means the object is ignored
			// we should stop processing it here
			if (config == null) {
				return computeVerdict(false);
			}

			// Assign flags for counters update
			existingBinding = true;
			startConfigFinal = qea.containsFinalState(config);

		} else { // New quantified variable binding

			// If the global guard is false then we can return now
			// as we can ignore this binding
			if(!qea.checkGlobalGuard(qVarValue)) return computeVerdict(false);
			
			// If we're using the IGNORE restart strategy make sure we're not ignoring
			if(restart_mode==RestartMode.IGNORE){
				if(((IgnoreWrapper) bindings).isIgnored(qVarValue)){
					return computeVerdict(false);
				}
			}			
			
			// Create configuration for the new binding
			config = new NonDetConfig(qea.getInitialState(), qea.newBinding(),null);
		}

		// Compute next configuration
		config = qea.getNextConfig(config, eventName, args, qVarValue);

		//System.err.println(config);
		
		// Update/add configuration for the binding
		bindings.put(qVarValue, config);

		// Determine if there is a final/non-final strong state
		boolean endConfigFinal = checkFinalAndStrongStates(config, qVarValue);

		// Update counters
		updateCounters(existingBinding, startConfigFinal, endConfigFinal);

		return computeVerdict(false);
	}

	private static final Object[] emptyArgs = new Object[] {};

	@Override
	public Verdict step(int eventName) {
		return step(eventName, emptyArgs);
	}

	@Override
	public String getStatus() {
		String ret = "Map:\n";
		Set<Map.Entry<Object, NonDetConfig>> entryset = null;
		if (bindings instanceof EagerGarbageHashMap) {
			entryset = ((EagerGarbageHashMap) bindings).storeEntrySet();
		} else {
			entryset = bindings.entrySet();
		}
		for (Map.Entry<Object, NonDetConfig> entry : entryset) {
			ret += entry.getKey() + "\t->\t" + entry.getValue() + "\n";
		}
		return ret;
	}

	@Override
	protected int removeStrongBindings() {
		int removed = 0;
		for (Object o : strong) {
			NonDetConfig c = bindings.get(o);
			boolean is_final = false;
			for (int s : c.getStates()) {
				is_final |= qea.isStateFinal(s);
			}
			if (is_final == finalStrongState) {
				bindings.remove(o);
				removed++;
			}
		}
		strong.clear();
		return removed;
	}

	@Override
	protected int rollbackStrongBindings() {
		int rolled = 0;
		for (Object o : strong) {
			NonDetConfig c = bindings.get(o);
			boolean is_final = false;
			for (int s : c.getStates()) {
				is_final |= qea.isStateFinal(s);
			}
			if (is_final == finalStrongState) {
				bindings.put(
						o,
						new NonDetConfig(qea.getInitialState(), qea
								.newBinding(),null));
				rolled++;
			}
		}
		strong.clear();
		return rolled;
	}

	@Override
	protected int ignoreStrongBindings() {
		int ignored = 0;
		for (Object o : strong) {
			NonDetConfig c = bindings.get(o);
			boolean is_final = false;
			for (int s : c.getStates()) {
				is_final |= qea.isStateFinal(s);
			}
			if (is_final == finalStrongState) {
				((IgnoreWrapper) bindings).ignore(o);
				ignored++;
			}
		}
		strong.clear();
		return ignored;
	}

}
