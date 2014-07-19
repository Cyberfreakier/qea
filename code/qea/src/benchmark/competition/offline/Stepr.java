package benchmark.competition.offline;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import monitoring.impl.XMLFileMonitorSAX;
import monitoring.impl.translators.OfflineTranslator;

import org.xml.sax.SAXException;

import properties.competition.translators.SteprTranslators;
import structure.impl.other.Verdict;
import structure.intf.QEA;
import exceptions.XMLFailureException;

public class Stepr {

	static properties.competition.Stepr s = new properties.competition.Stepr();
	static SteprTranslators tm = new SteprTranslators();

	public static void main(String[] args) throws IOException,
			ParserConfigurationException, SAXException {
		// runOne();
		// runTwo();
		// runThree();
		runFour();
	}

	static String trace = "traces/Team6/log.xml";

	public static void runOne() throws IOException,
			ParserConfigurationException, SAXException {

		long startTime = System.currentTimeMillis();
		QEA qea = s.makeOne();
		OfflineTranslator t = tm.makeOne();
		XMLFileMonitorSAX m = new XMLFileMonitorSAX(trace, qea, t);
		System.err.println("Running with " + m.getMonitorClass());
		long beforeMonitoring = System.currentTimeMillis();

		try {
			Verdict v = m.monitor();
			System.err.println("STePr 1: Verdict was " + v);
			long endTime = System.currentTimeMillis();
			System.err.println(">>Execution time without creation: "
					+ (endTime - beforeMonitoring));
			System.err.println(">>Total execution time : "
					+ (endTime - startTime));
		} catch (XMLFailureException e) {
			System.err.println("STePr 1: " + e.getMessage());
			long endTime = System.currentTimeMillis();
			System.err.println(">>Execution time without creation: "
					+ (endTime - beforeMonitoring));
			System.err.println(">>Total execution time : "
					+ (endTime - startTime));
		}

	}

	public static void runTwo() throws IOException,
			ParserConfigurationException, SAXException {

		long startTime = System.currentTimeMillis();
		QEA qea = s.makeTwo();
		OfflineTranslator t = tm.makeTwo();
		XMLFileMonitorSAX m = new XMLFileMonitorSAX(trace, qea, t);
		System.err.println("Running with " + m.getMonitorClass());
		long beforeMonitoring = System.currentTimeMillis();

		try {
			Verdict v = m.monitor();
			System.err.println("STePr 2: Verdict was " + v);
			long endTime = System.currentTimeMillis();
			System.err.println(">>Execution time without creation: "
					+ (endTime - beforeMonitoring));
			System.err.println(">>Total execution time : "
					+ (endTime - startTime));
		} catch (XMLFailureException e) {
			System.err.println("STePr 2: " + e.getMessage());
			long endTime = System.currentTimeMillis();
			System.err.println(">>Execution time without creation: "
					+ (endTime - beforeMonitoring));
			System.err.println(">>Total execution time : "
					+ (endTime - startTime));
		}
	}

	public static void runThree() throws IOException,
			ParserConfigurationException, SAXException {

		long startTime = System.currentTimeMillis();
		QEA qea = s.makeThree();
		OfflineTranslator t = tm.makeThree();
		XMLFileMonitorSAX m = new XMLFileMonitorSAX(trace, qea, t);
		System.err.println("Running with " + m.getMonitorClass());
		long beforeMonitoring = System.currentTimeMillis();

		try {
			Verdict v = m.monitor();
			System.err.println("STePr 3: Verdict was " + v);
			long endTime = System.currentTimeMillis();
			System.err.println(">>Execution time without creation: "
					+ (endTime - beforeMonitoring));
			System.err.println(">>Total execution time : "
					+ (endTime - startTime));
		} catch (XMLFailureException e) {
			System.err.println("STePr 3: " + e.getMessage());
			long endTime = System.currentTimeMillis();
			System.err.println(">>Execution time without creation: "
					+ (endTime - beforeMonitoring));
			System.err.println(">>Total execution time : "
					+ (endTime - startTime));
		}
	}

	public static void runFour() throws IOException,
			ParserConfigurationException, SAXException {

		long startTime = System.currentTimeMillis();
		QEA qea = s.makeFour();
		OfflineTranslator t = tm.makeFour();
		XMLFileMonitorSAX m = new XMLFileMonitorSAX(trace, qea, t);
		System.err.println("Running with " + m.getMonitorClass());
		long beforeMonitoring = System.currentTimeMillis();

		try {
			Verdict v = m.monitor();
			System.err.println("STePr 4: Verdict was " + v);
			long endTime = System.currentTimeMillis();
			System.err.println(">>Execution time without creation: "
					+ (endTime - beforeMonitoring));
			System.err.println(">>Total execution time : "
					+ (endTime - startTime));
		} catch (XMLFailureException e) {
			System.err.println("STePr 4: " + e.getMessage());
			long endTime = System.currentTimeMillis();
			System.err.println(">>Execution time without creation: "
					+ (endTime - beforeMonitoring));
			System.err.println(">>Total execution time : "
					+ (endTime - startTime));
		}
	}
}
