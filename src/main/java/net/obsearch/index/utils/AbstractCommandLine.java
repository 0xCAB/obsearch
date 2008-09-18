package net.obsearch.index.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.Properties;

import net.obsearch.Index;
import net.obsearch.OB;
import net.obsearch.ambient.Ambient;
import net.obsearch.asserts.OBAsserts;
import net.obsearch.exception.OBException;
import net.obsearch.exception.OBStorageException;
import net.obsearch.stats.Statistics;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.freehep.util.argv.BooleanOption;
import org.freehep.util.argv.DoubleOption;
import org.freehep.util.argv.IntOption;
import org.freehep.util.argv.StringOption;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import com.sleepycat.je.DatabaseException;

public abstract class AbstractCommandLine<O extends OB, I extends Index<O>, A extends Ambient<O, I>> {

	private static Logger logger = Logger.getLogger(AbstractCommandLine.class);

	/**
	 * Properties that influence this application.
	 */
	protected Properties props;

	enum Mode {
		search, create, add, x
	};
	/**
	 * Output format
	 */
	private DecimalFormat f = new DecimalFormat("00000.0000");

	@Option(name = "-h", usage = "Print help message", aliases = { "--help" })
	private boolean help = false;

	@Option(name = "-v", usage = "Print version information", aliases = { "--version" })
	private boolean version = false;

	@Option(name = "-db", usage = "Database Folder. Path to the folder where the DB is located", aliases = { "--database" })
	private File databaseFolder;

	@Option(name = "-l", usage = "Load data into the DB. (only in create mode)", aliases = { "--load" })
	private File load;

	@Option(name = "-p", usage = "# of pivots to be employed. Used in create mode only", aliases = { "--pivots" })
	protected int pivots = 7;

	@Option(name = "-k", usage = "# of closest objects to be retrieved.")
	protected int k = 1;

	@Option(name = "-m", usage = "Set the mode in search, create(start a new DB), add (add data to an existing database)", aliases = { "--mode" })
	private Mode mode;

	@Option(name = "-q", usage = "Query Filename. (Search mode only)", aliases = { "--query" })
	private File query;

	@Option(name = "-mq", usage = "Maximum number of queries to be executed", aliases = { "--max-queries" })
	protected int maxQueries = 1000;

	@Option(name = "-n", usage = "Name of the experiment", aliases = { "--name" })
	protected String experimentName = "default";

	@Option(name = "-rf", usage = "Experiment result filename", aliases = { "--exp-result" })
	protected String experimentResultFileName = "exp";

	@Option(name = "-b", usage = "Bulk mode is to be employed for create/add", aliases = { "--bulk" })
	protected boolean bulkMode = false;

	@Option(name = "-es", usage = "Experiment set, a colon separated list of  ranges and ks. Just like: r_1,k_1:r_1,k_1:...:r_n,k_n ", aliases = { "--experiment-set" })
	protected String experimentSet;

	@Option(name = "-r", usage = "Range used for retrieval")
	protected double r;
	
	private A ambiente;
	
	private I index;

	/**
	 * Number of queries executed.
	 */
	protected int queries = 0;
	/**
	 * Total ellapsed time during each query.
	 */
	protected long time;

	public void initProperties() throws IOException {

		InputStream is = this.getClass().getResourceAsStream(
				File.separator + "obsearch.properties");
		props = new Properties();
		props.load(is);
		// configure log4j only once too
		String prop = props.getProperty("log4j.file");
		PropertyConfigurator.configure(prop);
	}

	/**
	 * Return the "this" reference, used to access all the command line options.
	 * 
	 * @return The reference of the bottommost class that contains parameters.
	 */
	protected abstract AbstractCommandLine getReference();

	/**
	 * This method must be called by the sons of this class.
	 * 
	 * @param thisReference
	 *            this reference of the subclass.
	 * @param args
	 *            Arguments sent to the application.
	 */
	public void processUserCommands(String args[]) {

		try {
			initProperties();
		} catch (final Exception e) {
			System.err.print("Make sure log4j is configured properly"
					+ e.getMessage());
			e.printStackTrace();
			System.exit(48);
		}

		CmdLineParser parser = new CmdLineParser(getReference());
		try {
			parser.parseArgument(args);
			// arguments have been loaded.
			if (help) {
				parser.printUsage(System.err);
			}
			switch (mode) {
			case create:
				create();
				return;
			case search:
				search();
				return;
			case add:
				add();
				return;
			case x:
				experimentSet();
				return;
			}

		} catch (CmdLineException e) {
			logger.fatal("Error in command line arguments", e);
			parser.printUsage(System.err);
			System.err.println();
			System.exit(32);
		} catch (Exception e) {
			logger.fatal(e);
			e.printStackTrace();
			System.exit(33);
		}

	}

	protected abstract A instantiateNewAmbient(File dbFolder)
			throws OBStorageException, OBException, FileNotFoundException,
			IllegalAccessException, InstantiationException, IOException;

	protected abstract A instantiateAmbient(File dbFolder)
			throws OBStorageException, OBException, FileNotFoundException,
			IllegalAccessException, InstantiationException, IOException;

	/**
	 * Adds objects to the index. Loads the objects from File.
	 * 
	 * @param index
	 *            Index to load the objects into.
	 * @param load
	 *            File to load.
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws OBStorageException
	 * @throws OBException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	protected abstract void addObjects(I index, File load)
			throws FileNotFoundException, IOException, OBStorageException,
			OBException, IllegalAccessException, InstantiationException;

	protected abstract void searchObjects(I index, File load)
			throws IOException, OBException, InstantiationException,
			IllegalAccessException;

	protected void create() throws IOException, OBStorageException,
			OBException, DatabaseException, InstantiationException,
			IllegalAccessException {
		// OBAsserts.chkFileNotExists(databaseFolder);
		OBAsserts.chkFileExists(load);

		A ambiente = instantiateNewAmbient(databaseFolder);
		I index = ambiente.getIndex();

		logger.info("Loading Data...");
		addObjects(index, load);
		/*
		 * logger.info("Closing..."); ambiente.close();
		 * logger.info("Re-opening..."); ambiente =
		 * instantiateNewAmbient(databaseFolder);
		 */
		logger.info("Freezing...");
		ambiente.freeze();

		logger.info(ambiente.getIndex().getStats());
		ambiente.close();
	}

	protected void add() throws IOException, OBStorageException, OBException,
			DatabaseException, InstantiationException, IllegalAccessException {
		OBAsserts.chkFileExists(databaseFolder);
		OBAsserts.chkFileExists(load);

		A ambiente = instantiateAmbient(databaseFolder);
		I index = ambiente.getIndex();

		logger.info("Loading Data...");
		addObjects(index, load);

		logger.info(index.getStats());
		ambiente.close();
	}

	private void experimentSet() throws OBException, IOException,
			DatabaseException, InstantiationException, IllegalAccessException {

		File d = new File(experimentResultFileName + "_dist.txt");
		File s = new File(experimentResultFileName + "_smap.txt");
		File b = new File(experimentResultFileName + "_buckets.txt");
		File ra = new File(experimentResultFileName + "_dataRead.txt");
		File t = new File(experimentResultFileName + "_time.txt");
		File[] allFiles = { d, s, b, ra, t };
		FileWriter distance = new FileWriter(d, true);
		FileWriter smap = new FileWriter(s, true);
		FileWriter bucketsCount = new FileWriter(b, true);
		FileWriter dataRead = new FileWriter(ra, true);
		FileWriter time = new FileWriter(t, true);
		FileWriter[] all = { distance, smap, bucketsCount, dataRead, time };
		writeHeader(allFiles, all);
		writeAll(all, experimentName);
		
		String[] sets = this.experimentSet.split(":");
		openIndex();
		
		for (String set : sets) {
			logger.info("Executing experiment " + this.experimentName + " : "
					+ set);
			String[] rk = set.split(",");
			OBAsserts.chkAssert(rk.length == 2, "Wrong experiment set format");
			r = Double.parseDouble(rk[0]);
			k = Short.parseShort(rk[0]);
			writeAll(all, "\t");
			Statistics stats = searchAux();
			distance.write(p(stats.getDistanceCount()));
			smap.write(p(stats.getSmapCount()));
			bucketsCount.write(p(stats.getBucketsRead()));
			dataRead.write(p(stats.getDataRead()));
			time.write(p(this.time));
			
		}
		writeAll(all, "\n");
		closeAll(all);
		closeIndex();
	}
	
	private String p(double value){
		 return ((double) value/ (double)queries) + "";
	}

	/**
	 * Write header info if the file is empty.
	 * 
	 * @param files
	 * @param str
	 * @throws IOException
	 * @throws OBException
	 */
	protected void writeHeader(File[] files, FileWriter[] all)
			throws IOException, OBException {
		if (files[0].length() == 0) {
			writeAll(all, experimentSet.replace(":", "\t") + "\n");
		} else {
			// confirm that all the files are 0
			for (File f : files) {
				OBAsserts.chkAssert(f.length() != 0,
						"All files should be not zero");
			}
		}
	}

	protected void writeAll(FileWriter[] files, String str) throws IOException {
		for (FileWriter f : files) {
			f.write(str);
			f.flush();
		}
	}

	protected void closeAll(FileWriter[] files) throws IOException {
		for (FileWriter f : files) {
			f.close();
		}
	}
	
	private void openIndex() throws IOException, OBStorageException, OBException, IllegalAccessException, InstantiationException{
		OBAsserts.chkFileExists(databaseFolder);
		OBAsserts.chkFileExists(query);
		ambiente = instantiateAmbient(databaseFolder);
		index = ambiente.getIndex();
	}
	
	private void closeIndex() throws OBException{
		ambiente.close();
	}
	
	
	protected Statistics searchAux() throws IOException, OBStorageException,
	OBException, DatabaseException, InstantiationException,
	IllegalAccessException {
		index.resetStats();
		this.queries = 0;
		this.time = 0;
		logger.info("Searching... ");
		searchObjects(index, query);
		logger.info(index.getStats());
		logger.info("Time per query: " + (double)time / (double)queries);
		return index.getStats();
	}
	

	/**
	 * Perform one search for a given k and r.
	 * 
	 * @throws IOException
	 * @throws OBStorageException
	 * @throws OBException
	 * @throws DatabaseException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	protected void search() throws IOException, OBStorageException,
			OBException, DatabaseException, InstantiationException,
			IllegalAccessException {
		openIndex();
		searchAux();
		closeIndex();
	}

}