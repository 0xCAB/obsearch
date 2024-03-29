package net.obsearch.example.protein;

import hep.aida.bin.StaticBin1D;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import net.obsearch.ambient.Ambient;

import net.obsearch.ambient.bdb.AmbientBDBJe;

import net.obsearch.ambient.tc.AmbientTC;
import net.obsearch.example.AbstractExampleGeneral;
import net.obsearch.example.AbstractGHSExample;
import net.obsearch.exception.IllegalIdException;
import net.obsearch.exception.NotFrozenException;
import net.obsearch.exception.OBException;
import net.obsearch.exception.OBStorageException;
import net.obsearch.exception.PivotsUnavailableException;
import net.obsearch.index.OBVectorFloat;







import net.obsearch.index.ghs.impl.Sketch64Float;
import net.obsearch.index.utils.Directory;
import net.obsearch.pivots.AcceptAll;
import net.obsearch.pivots.bustos.impl.IncrementalBustosNavarroChavezFloat;
import net.obsearch.pivots.bustos.impl.IncrementalBustosNavarroChavezShort;
import net.obsearch.pivots.perm.impl.IncrementalPermFloat;
import net.obsearch.pivots.random.RandomPivotSelector;
import net.obsearch.pivots.rf03.RF03PivotSelectorFloat;
import net.obsearch.pivots.rf04.RF04PivotSelectorFloat;
import net.obsearch.pivots.rf02.RF02PivotSelectorFloat;
import net.obsearch.pivots.rf02.RF02PivotSelectorShort;
import net.obsearch.pivots.sss.impl.SSSFloat;
import net.obsearch.query.OBQueryFloat;
import net.obsearch.query.OBQueryShort;
import net.obsearch.result.OBPriorityQueueFloat;
import net.obsearch.result.OBPriorityQueueShort;
import net.obsearch.result.OBResultFloat;
import net.obsearch.result.OBResultShort;

public class SwissProtDemo extends AbstractGHSExample {
	
	
	@Option(name = "-k", usage = "Alpha used during search time", aliases = { "--k_nn" })
	protected int k = 1;

	public SwissProtDemo(String args[]) throws IOException, OBStorageException,
			OBException, IllegalAccessException, InstantiationException,
			PivotsUnavailableException {
		super(args);

	}
	
	protected Protein read(BufferedReader qData) throws IOException, OBException{
		String id = qData.readLine();
		if(id == null){return null;}
		String[] parse = id.split("<<:\\)>>");
		return new Protein(parse[0], parse[1]);
		//String protein = qData.readLine();
		//if(protein == null){return null;}		
		//return new Protein(id, protein);
	}

	protected void search() throws OBStorageException, NotFrozenException,
			IllegalAccessException, InstantiationException, OBException,
			IOException {
		BufferedReader qData = new BufferedReader(new FileReader(query));
		Ambient<Protein, Sketch64Float<Protein>> a = new AmbientTC<Protein, Sketch64Float<Protein>>(
				indexFolder);
		Sketch64Float<Protein> index = a.getIndex();
		// now we can match some objects!
		logger.info("Querying the index...");
		int i = 0;

		index.setKAlpha(3f);
		long start = System.currentTimeMillis();
		List<OBPriorityQueueFloat<Protein>> queryResults = new ArrayList<OBPriorityQueueFloat<Protein>>(
				querySize);
		List<Protein> queries = new ArrayList<Protein>(querySize);
		Protein line = read(qData);
		//logger.info("Warming cache...");
		//index.bucketStats();
		index.resetStats(); // reset the stats counter
		logger.info("Search starts!");
		while (line != null && i < querySize) {			
			Protein q = line;
			line = read(qData);
			// query the index with k=1
			OBPriorityQueueFloat<Protein> queue = new OBPriorityQueueFloat<Protein>(
					k);
			// perform a query with r=3000000 and k = 1
			index.searchOB(q, Float.MAX_VALUE, queue);
			logger.info("**** Query: " + q.getId() + " found: \n");
			StringBuilder b = new StringBuilder();
			for(OBResultFloat<Protein> p : queue.getSortedElements()){
				b.append(p.getObject().getId());
				b.append("Dist: ");
				b.append(p.getDistance());
				b.append("\n");
			}
			logger.info(b.toString());
			queryResults.add(queue);
			queries.add(q);

			i++;
		}
		// print the results of the set of queries.
		long elapsed = System.currentTimeMillis() - start;
		logger.info("Time per query: " + elapsed / querySize + " millisec.");

		logger
				.info("Stats follow: (total distances / pivot vectors computed during the experiment)");
		logger.info(index.getStats().toString());

		logger.info("Doing EP validation");
		StaticBin1D ep = new StaticBin1D();
		StaticBin1D rde = new StaticBin1D();
		StaticBin1D precision = new StaticBin1D();
		StaticBin1D compound = new StaticBin1D();
		StaticBin1D adonis = new StaticBin1D();

		Iterator<OBPriorityQueueFloat<Protein>> it1 = queryResults
				.iterator();
		Iterator<Protein> it2 = queries.iterator();
		StaticBin1D seqTime = new StaticBin1D();
		i = 0;
		while (it1.hasNext()) {
			OBPriorityQueueFloat<Protein> qu = it1.next();
			Protein q = it2.next();
			long time = System.currentTimeMillis();
			float[] sortedList = index.fullMatchLite(q, false);
			long el = System.currentTimeMillis() - time;
			seqTime.add(el);
			
			OBQueryFloat<Protein> queryObj = new OBQueryFloat<Protein>(q,
					Float.MAX_VALUE, qu, null);
			ep.add(queryObj.ep(sortedList));
			rde.add(queryObj.rde(sortedList));
			precision.add(queryObj.precision(sortedList));
			double app = queryObj.approx(sortedList);
			adonis.add(app);
			logger.info("Elapsed: " + el + " " + i + " " + adonis + " \n app: "+ app);	
			double comp = queryObj.compound(sortedList);
			if(comp > 1){
				 comp = queryObj.compound(sortedList);
			}
			compound.add(comp);
			i++;
		}
		logger.info("EP");
		logger.info(ep.toString());
		logger.info("RDE");
		logger.info(rde.toString());
		logger.info("Precision: " + precision.mean());
		logger.info("Time per seq query: ");
		logger.info(seqTime.toString());
		
		logger.info("EP: "  + ep.mean());
		logger.info("RDE: " + rde.mean());
		logger.info("Precision: " + precision.mean());
		logger.info("Compound: " + compound.mean());
		logger.info("Adonis: " + adonis);
	}

	protected void intrinsic() throws IllegalIdException,
			IllegalAccessException, InstantiationException, OBException,
			FileNotFoundException, IOException {
		Ambient<Protein, Sketch64Float<Protein>> a = new AmbientTC<Protein, Sketch64Float<Protein>>(
				indexFolder);
		Sketch64Float<Protein> index = a.getIndex();

		logger.info("Intrinsic dim: " + index.intrinsicDimensionality(1000));
	}

	protected void create() throws OBStorageException, OBException,
			IOException, IllegalAccessException, InstantiationException,
			PivotsUnavailableException {
		BufferedReader dbData = new BufferedReader(new FileReader(database));

		// Create a pivot selection strategy
		// new AcceptAll<Protein>());
		/*
		 * RF02PivotSelectorFloat<Protein> sel = new
		 * RF02PivotSelectorFloat<Protein>( new AcceptAll<Protein>());
		 * sel.setDataSample(400); sel.setRepetitions(1000);
		 * sel.setDesiredDistortion(0.30f); sel.setDesiredSpread(0);
		 */
		// IncrementalBustosNavarroChavezFloat<Protein> sel = new
		// IncrementalBustosNavarroChavezFloat<Protein>(new
		// AcceptAll<Protein>(), 400,400);
		// IncrementalPermFloat<Protein> sel = new
		// IncrementalPermFloat<Protein>(new AcceptAll<Protein>(), 50,
		// 50);
		//IncrementalPermFloat<Protein> sel = new IncrementalPermFloat<Protein>(new AcceptAll(), 100, 100);
		/*RandomPivotSelector<Protein> sel = new RandomPivotSelector<Protein>(
				new AcceptAll<Protein>());
		*/

		// make the bit set as short so that m objects can fit in the
		// buckets.
	
			
		//sel.setMaxDistance(9436);
		RF04PivotSelectorFloat<Protein> sel = new RF04PivotSelectorFloat<Protein>(
				new AcceptAll<Protein>());
		sel.setDataSample(400);
		//sel.setRepetitions(100);
		
		Sketch64Float<Protein> index = new Sketch64Float<Protein>(Protein.class,
				sel, 512);
		index.setExpectedError(2f);
		index.setSampleSize(100);
		// select the ks that the user will call.
		index.setMaxK(new int[] { 1, 20 });

		// Create the ambient that will store the index's data.
		Ambient<Protein, Sketch64Float<Protein>> a = new AmbientTC<Protein, Sketch64Float<Protein>>(
				index, indexFolder);

		// Add some random objects to the index:
		logger.info("Adding " + databaseSize + " objects...");
		int i = 0;
		StaticBin1D proteinSizes = new StaticBin1D();
		Protein line = read(dbData);
		while (line != null && i <= databaseSize ) {			
			index.insert(line);		
			proteinSizes.add(line.length());
			line = read(dbData);
			if (i % 100000 == 0) {
				logger.info("Loading: " + i);
			}
			i++;
		}
		logger.info("Protein size stats: " + proteinSizes);

		// prepare the index
		logger.info("Preparing the index... size: " + index.databaseSize());
		a.freeze();
		logger.info(index.getStats().toString());
		// close the index (very important!)
		a.close();
	}

	public static void main(String args[]) throws FileNotFoundException,
			OBStorageException, NotFrozenException, IllegalAccessException,
			InstantiationException, OBException, IOException,
			PivotsUnavailableException {

		SwissProtDemo g = new SwissProtDemo(args);

	}

}
