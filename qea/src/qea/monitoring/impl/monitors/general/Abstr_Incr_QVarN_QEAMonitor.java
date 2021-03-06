package qea.monitoring.impl.monitors.general;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import qea.monitoring.impl.GarbageMode;
import qea.monitoring.impl.IncrementalMonitor;
import qea.monitoring.impl.RestartMode;
import qea.structure.impl.other.QBindingImpl;
import qea.structure.impl.other.Transition;
import qea.structure.impl.other.Verdict;
import qea.structure.impl.qeas.Abstr_QVarN_QEA;
import qea.util.OurVeryWeakHashMap;
import qea.util.OurWeakHashMap;

/**
 * 
 * We use the symbol-indexing concept
 * 
 * @author Giles Reger
 */
public abstract class Abstr_Incr_QVarN_QEAMonitor<Q extends Abstr_QVarN_QEA>
		extends IncrementalMonitor<Q> {

	protected static boolean DEBUG = false;

	protected final IncrementalChecker checker;
	protected final Map<Object, QBindingImpl> support_bindings;
	protected final Map<QBindingImpl, String> support_queries;
	private final int qvars;
	private final int freevars;
	private final boolean use_weak;

	protected final QBindingImpl bottom;

	protected final BindingRecord[] empty_paths;
	protected final boolean[] empty_has_q_blanks;// true if event *must* bind a
													// qvar with empty mask
	protected final Map<String, BindingRecord>[] maps;

	protected static final String BLANK = "_";
	// 0 for value
	// 1 for qblank (only qvars)
	// 2 for fblank (some fvars)
	// 3 for nblank (no qvars, only fvars)
	// Important - when creating masks if we have a choice between 1 and 2, pick 2
	// 			   if we have a choice between 2 and 3, pick 3
	// masks should be ordered from specific to general
	public enum Mask { VALUE, QBLANK, FBLANK, NBLANK };
	protected Mask[][][] masks;

	// could_leave[state][eventname] is true if an event with eventname could
	// leave state
	//
	protected final boolean could_leave[][];
	
	protected boolean could_leave(int[] states,int eventName){
		for(int i=0;i<states.length;i++){
			if(could_leave[states[i]][eventName]){
				return true;
			}
		}
		return false;
	}

	public Abstr_Incr_QVarN_QEAMonitor(RestartMode restart,
			GarbageMode garbage, Q qea) {
		super(restart, garbage, qea);
		checker = IncrementalChecker.make(qea.getFullLambda(),qea.isNegated(),
				qea.getFinalStates(), qea.getStrongStates());
		qea.setupMatching();
		qea.isNormal(); // make sure normal is set
		qvars = qea.getFullLambda().length;
		freevars = qea.getFreeVars();
		int num_events = qea.getEventsAlphabet().length + 1;

		// Remember that incremental_checker might contain references
		// to objects if quantification is alternating
		switch (garbage) {
		case NONE:
			support_bindings = null;
			support_queries = null;
			use_weak = false;
			// create a lookup map per event name
			maps = new HashMap[num_events];
			for (int i = 0; i < num_events; i++) {
				maps[i] = new HashMap<String, BindingRecord>();
			}
			break;
		case UNSAFE_LAZY:
			/*
			 * The idea is that the qbinding is pointed to by the objects it
			 * contains. If all of those objects become garbage then that
			 * qbinding can be removed.
			 */
			support_bindings = new OurVeryWeakHashMap<Object, QBindingImpl>();
			support_queries = new OurWeakHashMap<QBindingImpl, String>();
			use_weak = true;
			// create a lookup map per event name
			maps = new OurWeakHashMap[num_events];
			for (int i = 0; i < num_events; i++) {
				maps[i] = new OurWeakHashMap<String, BindingRecord>();
			}
			break;
		default:
			throw new RuntimeException("Garbage mode " + garbage
					+ " not currently supported");
		}

		bottom = qea.newQBinding();
		if (bottom.isTotal()) {
			checker.newBinding(bottom, qea.getInitialState());
		}

		// make empty paths and empty_has_q_blanks
		empty_paths = new BindingRecord[num_events];
		empty_has_q_blanks = new boolean[num_events];
		for (int i = 0; i < num_events; i++) {
			empty_paths[i] = BindingRecord.make(bottom, use_weak);
		}

		masks = new Mask[num_events][][];

		// generate masks
		int num_states = qea.getStates().length + 1;

		for (int e = 0; e < num_events; e++) {

			// get the most general signature
			Mask[] general_args = null;
			boolean every_sig_has_qs = true;
			boolean[] has_q_ever = null;
			for (int s = 1; s < num_states; s++) {
				for (Transition t : getTransitions(s, e)) {
					if (t != null) {
						int[] targs = t.getVariableNames();
						boolean some_q = false;
						for (int v : targs) {
							if (v < 0) {
								some_q = true;
							}
						}
						every_sig_has_qs &= some_q;
						if (general_args == null) {
							general_args = new Mask[targs.length];
							has_q_ever = new boolean[targs.length];
							for (int i = 0; i < targs.length; i++) {
								// Update when we can have values here
								if (targs[i] < 0) {
									general_args[i] = Mask.QBLANK;
									has_q_ever[i] = true;
								} else {
									general_args[i] = Mask.FBLANK;
								}
							}
						} else {
							// take max i.e. max(0,1)=1, max(1,2)=2
							// currently could only be 1 or 2
							for (int i = 0; i < general_args.length; i++) {
								// At the moment only need to set to 2
								// if required
								if (targs[i] > 0) {
									general_args[i] = Mask.FBLANK;
								}
								else{
									has_q_ever[i] = true;
								}
							}
						}

					}
				}
			}
			if (general_args == null) {
				general_args = new Mask[] {};
			}
			else{
				for(int i=0;i<has_q_ever.length;i++){
					if(!has_q_ever[i]){
						general_args[i] = Mask.NBLANK;
					}
				}
			}

			empty_has_q_blanks[e] = every_sig_has_qs;

			// create versions of this signature - not the empty (i.e. all 0)
			// order from most specific (no 0 replacements) to least
			// TODO - we can statically determine some masks are not required -
			// do this!

			// Without this trimming, and assuming no zeros in general_args, the
			// number of
			// masks is 2^args.length -1 (don't consider empty)
			// but will will use a recursive function with lists and then turn
			// it into an array!
			List<List<Mask>> emasks_lists = makeMasks(e, general_args, 0);
			Mask[][] emasks = new Mask[emasks_lists.size()][general_args.length];
			for (int i = 0; i < emasks.length; i++) {
				List<Mask> emasks_list = emasks_lists.get(i);
				for (int j = 0; j < general_args.length; j++) {
					emasks[i][j] = emasks_list.get(j);
				}
			}
			masks[e] = emasks;

		}

		/*
		 * Setup could leave Note: if the QEA is not normal then the whole array
		 * should be true
		 */
		could_leave = new boolean[num_states][num_events];// nums already+1
															// above
		for (int state = 1; state < num_states; state++) {
			for (int event = 1; event < num_events; event++) {
				could_leave[state][event] = qea.can_leave(state, event);
			}
		}

		// for(int state=1;state<num_states;state++)
		// System.err.println(Arrays.toString(could_leave[state]));

	}

	/*
	 * For the det case this will return a singleton array
	 */
	protected abstract Transition[] getTransitions(int s, int e);

	private void makeMasksLevel(Mask[] args, boolean[] used, int level,
			List<List<Mask>> masks, int taken) {
		if (taken == level) {
			// make the mask
			List<Mask> mask = new ArrayList<Mask>();
			boolean all_not_zero = true;
			for (int i = 0; i < args.length; i++) {
				if (used[i]) {
					mask.add(args[i]);
				} else {
					if(args[i] != Mask.NBLANK){
						mask.add(Mask.VALUE);
						all_not_zero = false;
					}
					else{ mask.add(args[i]); }
				}
			}
			// if the mask is empty i.e all 0s
			if (!all_not_zero && !masks.contains(mask)) {				
				masks.add(mask);
			}
			return;
		}
		for (int i = 0; i < args.length; i++) {
			if (!used[i]) {
				used[i] = true;
				makeMasksLevel(args, used, level, masks, taken + 1);
				used[i] = false;
			}
		}
	}

	// level indicates the number of replacements we should make
	// The organisation is somewhat complicated as we want to do breadth-first
	// rather than depth-first
	private List<List<Mask>> makeMasks(int e, Mask[] args, int level) {
		List<List<Mask>> this_level = new ArrayList<List<Mask>>();

		if (args == null || args.length == 0) {
			return this_level;
		}

		if (level == 0) {
			List<Mask> l = new ArrayList<Mask>();
			for (int i=0;i<args.length;i++) {
				if(args[i]!=Mask.NBLANK){
					l.add(Mask.VALUE); // replace x by 0
				}else{
					l.add(args[i]);
				}
			}
			if(!this_level.contains(l)){
				this_level.add(l);
			}
		} else {
			boolean[] used = new boolean[args.length];
			makeMasksLevel(args, used, level, this_level, 0);
		}

		// Stop when level = args.length-1
		if (level < args.length) {
			List<List<Mask>> next_level = makeMasks(e, args, level + 1);
			for(List<Mask> l : next_level){
				if(!this_level.contains(l)){
					this_level.add(l); // append
				}
			}
		}
		return this_level;
	}

	int rep = 0;

	@Override
	public Verdict step(int eventName, Object[] args) {

		//if (rep++ % 1000 == 0) {
		//	DEBUG = true;
		//} else {
		//	DEBUG = false;
		//}
		//if(rep++ == 200){
		//	DEBUG=true;
		//	printEvent(eventName,args);
		//	printMaps();
		//}
		if (DEBUG) {
			System.err
					.println("********************\n\n********************\n=======> "
							+ qea.get_event_name(eventName)
							+ Arrays.toString(args));
		}
		if (DEBUG) {
			//printMaps();
		}

		if (saved != null) {
			if (!restart()) {
				return saved;
			}
		}

		/*
		  for(int e = 1; e<masks.length;e++){ 
			  System.err.println(e); 
			  for(int[] m : masks[e]) System.err.println(Arrays.toString(m)); 
		  }
		  System.err.println(Arrays.toString(empty_has_q_blanks));
		  System.exit(0);
		*/
		 

		// retrieve consistent bindings in order of informativeness
		// do updates and extensions in-place

		// keep track of bindings used
		Set<QBindingImpl> used = new HashSet<QBindingImpl>();

		// Look at the masks, as long as the map is non-empty
		// TODO- is this empty check premature optimisation?
		Map<String, BindingRecord> map = maps[eventName];
		boolean used_full = false;
		if (!map.isEmpty()) {
			Mask[][] eventMasks = masks[eventName];

			for (int i = 0; i < eventMasks.length; i++) {				
				Mask[] mask = eventMasks[i];
				//System.err.println("mask "+Arrays.toString(mask));
				StringBuilder b = new StringBuilder();
				boolean has_q_blanks = false;
				for (int j = 0; j < mask.length; j++) {
					if (mask[j] == Mask.VALUE) {
						b.append(System.identityHashCode(args[j]));
					} else {
						if (mask[j] == Mask.QBLANK) {
							has_q_blanks = true;
						}
						b.append(BLANK);
					}
				}
				String query = b.toString();

				BindingRecord record = map.get(query);

				if (DEBUG){// && record!=null) {
					System.err.println("Query " + query+"\t=>\t"+record);
				}

				if (record != null) {
					process_record(record, eventName, args, used, has_q_blanks);
					if (i == 0 && freevars == 0 && !use_red) {
						// use_full not always viable
						// only allow it if we have no free variables
						// this is oversafe
						used_full = true;
						break; // for full optimisation
					}
				}
			}
		}
		if (!used_full) {
			// now the empty mask
			if (DEBUG) {
				System.err.println("Processing empty");
			}
			BindingRecord record = empty_paths[eventName];
			process_record(record, eventName, args, used,
					empty_has_q_blanks[eventName]);
		}

		Verdict result = checker.verdict(false);
		if (DEBUG) {
			System.err.println("result: " + result);
		}
		if (result.isStrong()) {
			saved = result;
		}

		if (DEBUG) {
			System.err.println("*******************************\n");
			System.err.println(getStatus());
			System.err.println("*******************************\n\n\n\n");
			System.err.println("*******************************\n");
		}

		//System.err.println("done");
		
		return result;
	}

	private void process_record(BindingRecord record, int eventName,
			Object[] args, Set<QBindingImpl> used, boolean has_q_blanks) {

		if (DEBUG) {
			System.err.println("Processing " + record);
		}

		/*
		 * Record might be updated whilst iterating over it But these will be
		 * added to the end, so get num bindings before we begin (although this
		 * shouldn't be needed)
		 */
		int numbindings = record.num_bindings();

		if (numbindings == 0) {
			// This record has become garbage, we should remove
			// the associate entry in maps
			// - can this happen?
			System.err.println("record garbage");
			if (DEBUG) {
				System.err.println(record);
			}
		}

		List<Integer> null_indexes = null;
		for (int j = numbindings - 1; j >= 0; j--) {
			QBindingImpl binding = record.get(j);
			if (binding == null) {
				if (DEBUG) {
					System.err
							.println("record had an empty binding! " + record);
				}
				if (null_indexes == null) {
					null_indexes = new ArrayList<Integer>();
				}
				null_indexes.add(j);
				continue;
			}

			if (DEBUG) {
				System.err.println(j + ":" + binding);
			}

			// If the binding hasn't already been encountered
			if (used.add(binding)) {

				// It should be an invariant that binding is in mapping, check
				// this
				processBinding(eventName, args, has_q_blanks, binding,used);
			}
		}
		// remove null_indexes if they exist
		if (null_indexes != null) {
			record.removeIndexes(null_indexes);
		}

	}

	protected abstract void processBinding(int eventName, Object[] args,
			boolean has_q_blanks, QBindingImpl binding, Set<QBindingImpl> used);

	/*
	 * addSupport for binding b maps v to b in support_bindings for all (x->v)
	 * in b. This is for the purpose of garbage collection
	 */
	protected void addSupport(QBindingImpl binding) {
		if (support_bindings != null) {
			for (int v = 1; v < qvars; v++) {
				Object value = binding.getValue(-v);
				if (value != null) {
					support_bindings.put(value, binding);
				}
			}
		}
	}

	/*
	 * The purpose of add_to_maps is to consistently add a generated binding to
	 * the support maps
	 * 
	 * if add if false then this acts like 'remove' from maps
	 */
	protected void add_to_maps(QBindingImpl ext) {
		add_to_maps(ext, true);
	}

	protected void add_to_maps(QBindingImpl ext, boolean add) {
		if(DEBUG){
			System.err.println("Adding to maps "+ext);
		}
		
		int[][][] sigs = qea.getSigs();
		for (int e = 1; e < sigs.length; e++) {
			Set<String> qs = new HashSet<String>();
			int[][] es = sigs[e];
			for (int s = 0; s < es.length; s++) {
				int[] sig = es[s];
				if(DEBUG){ System.err.println(">> using sig "+Arrays.toString(sig)); }
				StringBuilder b = new StringBuilder();
				boolean empty = true;
				for (int j = 0; j < sig.length; j++) {
					int var = sig[j];
					Object val = var < 0 ? ext.getValue(sig[j]) : null;
					if (val != null) {
						empty = false;
						b.append(System.identityHashCode(val));
					} else {
						b.append(BLANK);
					}
				}
				String q = b.toString();
				if(DEBUG){ System.err.println(">> using query "+q); }
				if (qs.add(q)) {
					if (add && support_queries != null) {
						support_queries.put(ext, q);
					}
					BindingRecord record = empty ? empty_paths[e] : maps[e].get(q);
					if (add && record == null) { // empty must be false
						record = BindingRecord.make(ext, use_weak);
						maps[e].put(q, record);
					} else {
						if (add) {
							record.addBinding(ext);
						} else {
							record.removeBinding(ext);
							if (record.num_bindings() == 0) {
								maps[e].remove(q);
							}
						}
					}
				}
			}
		}
	}

	@Override
	public Verdict end() {
		return checker.verdict(true);
	}

	/*
	 * Bindings are added to the end So we should always iterate from the back
	 * to get the newest (largest) bindings first
	 */
	public static abstract class BindingRecord {

		public static BindingRecord make(QBindingImpl b, boolean weak) {
			if (weak) {
				return new WeakBindingRecord(b);
			}
			return new StrongBindingRecord(b);
		}

		public abstract QBindingImpl get(int j);

		public abstract int num_bindings();

		abstract void addBinding(QBindingImpl b);

		abstract void removeBinding(QBindingImpl b);

		abstract void removeIndexes(List<Integer> is);
	}

	public static class StrongBindingRecord extends BindingRecord {
		QBindingImpl[] bindings;
		int num_bindings = 0;

		@Override
		public String toString() {
			return "record of " + Arrays.toString(bindings);
		}

		StrongBindingRecord(QBindingImpl b) {
			bindings = new QBindingImpl[2];
			num_bindings = 1;
			bindings[0] = b;
		}

		@Override
		void addBinding(QBindingImpl b) {
			if (num_bindings == bindings.length) {
				// extend bindings
				QBindingImpl[] temp = new QBindingImpl[bindings.length * 2];
				System.arraycopy(bindings, 0, temp, 0, bindings.length);
				bindings = temp;
			}
			bindings[num_bindings] = b;
			num_bindings++;
		}

		@Override
		void removeBinding(QBindingImpl b) {
			// find binding
			int index = -1;
			for (int i = 0; i < num_bindings; i++) {
				if (bindings[i].equals(b)) {
					index = i;
					break;
				}
			}
			if (index == -1) {
				return; // not found
			}
			// move everything after index down
			for (int i = index; i + 1 < num_bindings; i++) {
				bindings[i] = bindings[i + 1];
			}
			// but if index is last element then null it
			if (index == bindings.length - 1) {
				bindings[bindings.length - 1] = null;
			}
			num_bindings--;
		}

		// Invariant - indexes is ordered
		@Override
		void removeIndexes(List<Integer> indexes) {
			throw new RuntimeException("Should not be called");
		}

		@Override
		public QBindingImpl get(int j) {
			return bindings[j];
		}

		@Override
		public int num_bindings() {
			return num_bindings;
		}

	}

	public static class WeakBindingRecord extends BindingRecord {
		WeakReference<QBindingImpl>[] bindings;
		int num_bindings = 0;

		@Override
		public String toString() {
			String ret = "record of " + num_bindings + " ";
			ret += Arrays.toString(bindings);
			if (bindings.length > 20) {
				ret += "\n----------------\n";
			}
			return ret;
		}

		WeakBindingRecord(QBindingImpl b) {
			bindings = new WeakReference[2];
			num_bindings = 1;
			bindings[0] = new WeakReference<QBindingImpl>(b);
		}

		@Override
		void addBinding(QBindingImpl b) {
			if (DEBUG) {
				System.err.println("Add " + b + " to " + this);
			}
			if (num_bindings == bindings.length) {
				// extend bindings
				WeakReference<QBindingImpl>[] temp = new WeakReference[bindings.length * 2];
				System.arraycopy(bindings, 0, temp, 0, bindings.length);
				bindings = temp;
			}
			bindings[num_bindings] = new WeakReference<QBindingImpl>(b);
			num_bindings++;
		}

		@Override
		void removeBinding(QBindingImpl b) {
			// find binding
			int index = -1;
			for (int i = 0; i < num_bindings; i++) {
				if (b.equals(bindings[i].get())) { // b is not null, so call on
													// this
					index = i;
					break;
				}
			}
			if (index == -1) {
				System.err.println("Failed removing " + b + " from " + this);
				return; // not found
			}
			// move everything after index down
			// unless we're at the end
			if (index + 1 < num_bindings) {
				// minus 1 from num_bindings as we're dealing with indexes
				int left = num_bindings - 1 - index;
				System.arraycopy(bindings, index + 1, bindings, index, left);
			}

			num_bindings--;
		}

		// Invariant - indexes is ordered *in reverse order*
		@Override
		void removeIndexes(List<Integer> indexes) {
			if (DEBUG) {
				System.err.println("remove " + indexes + " from " + this);
			
				System.err.println("Removing " + indexes.size()+ " garbage in record");
			}
	
			int removed = 0;
			int ep = num_bindings - 1;
			// iterate backwards as order is backwards
			for (int i = indexes.size() - 1; i >= 0; i--) {
				int index = indexes.get(i);
				// The index has been shifted left removed times
				int index_place = index - removed;
				// move everything after index_place left one
				// unless we've reached the end
				if (index_place + 1 < ep) {
					int left = ep - index_place;
					System.arraycopy(bindings, index_place + 1, bindings,
							index_place, left);
					removed++;
					ep--;
				}
			}
			// don't actually need to null anything later or resize
			// as we use num_bindings to iterate over record :)
			// ep will point to the last binding, as we're zero-indexed
			// the actual number is one greater
			num_bindings = ep + 1;
		}

		@Override
		public QBindingImpl get(int j) {
			return bindings[j].get();
		}

		@Override
		public int num_bindings() {
			// trim removed bindings at this point
			// int c = 0;
			// for(int i=0;i<num_bindings;i++){
			// if(bindings[i].get()!=null) c++;
			// }
			// WeakReference<QBindingImpl> [] temp = new WeakReference[c];
			// int p = 0;
			// for(int i=0;i<num_bindings;i++){
			// if(bindings[i].get()!=null) temp[p++] = bindings[i];
			// }
			// bindings=temp;
			// num_bindings=c;

			return num_bindings;
		}

	}

	@Override
	protected boolean restart() {
		switch (restart_mode) {
		case NONE:
			return false;

		case REMOVE:
			// remove the offending binding
			removeStrongBindings();
			return true;
		case ROLLBACK:
			// rollback the offending bindings to initial state
			rollbackStrongBindings();
			return true;
		case IGNORE:
			// set the offending bindings to be ignored in the future
			// ignoreStrongBindings();
			return false;
		}
		return false;
	}

	protected abstract void printMaps();

}
