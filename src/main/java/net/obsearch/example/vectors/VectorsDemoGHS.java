package net.obsearch.example.vectors;

import hep.aida.bin.StaticBin1D;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.obsearch.ambient.Ambient;
import net.obsearch.ambient.bdb.AmbientBDBDb;
import net.obsearch.ambient.bdb.AmbientBDBJe;
import net.obsearch.ambient.my.AmbientMy;
import net.obsearch.ambient.tc.AmbientTC;
import net.obsearch.exception.NotFrozenException;
import net.obsearch.exception.OBException;
import net.obsearch.exception.OBStorageException;
import net.obsearch.exception.PivotsUnavailableException;
import net.obsearch.index.ghs.impl.Sketch64Short;
import net.obsearch.index.utils.Directory;
import net.obsearch.pivots.AcceptAll;
import net.obsearch.pivots.bustos.impl.IncrementalBustosNavarroChavezShort;
import net.obsearch.pivots.rf02.RF02PivotSelectorShort;
import net.obsearch.query.OBQueryShort;
import net.obsearch.result.OBPriorityQueueShort;
import net.obsearch.result.OBResultShort;

public class VectorsDemoGHS extends VectorsDemo {
	
	
	public static void main(String args[]) throws FileNotFoundException, OBStorageException, NotFrozenException, IllegalAccessException, InstantiationException, OBException, IOException, PivotsUnavailableException {
		
		init();
		
		// Delete the directory of the index just in case.
		Directory.deleteDirectory(INDEX_FOLDER);
		
		
		// Create a pivot selection strategy for L1 distance
		 //IncrementalMullerRosaShort<L1> sel = new IncrementalMullerRosaShort<L1>(
	 	//			new AcceptAll<L1>(), 4000, 1000, (short) Short.MAX_VALUE);
		
		RF02PivotSelectorShort<L1> sel = new RF02PivotSelectorShort<L1>(new AcceptAll<L1>());
		sel.setDataSample(1000);
		sel.setRepetitions(1000);
		// make the bit set as short so that m objects can fit in the buckets.
	    Sketch64Short<L1> index = new Sketch64Short<L1>(L1.class, sel, 64, 0);
	    index.setExpectedEP(0.0001);
	    index.setSampleSize(100);
	    
	    // select the ks that the user will call.
	    index.setMaxK(new int[]{1});	    
	    index.setFixedRecord(true);
    	index.setFixedRecord(VEC_SIZE*2);
		// Create the ambient that will store the index's data. (NOTE: folder name is hardcoded)
		//Ambient<L1, Sketch64Short<L1>> a =  new AmbientBDBDb<L1, Sketch64Short<L1>>( index, INDEX_FOLDER );
	    //Ambient<L1, Sketch64Short<L1>> a =  new AmbientMy<L1, Sketch64Short<L1>>( index, INDEX_FOLDER );
    	Ambient<L1, Sketch64Short<L1>> a =  new AmbientTC<L1, Sketch64Short<L1>>( index, INDEX_FOLDER );
		
		// Add some random objects to the index:	
		logger.info("Adding " + DB_SIZE + " objects...");
		int i = 0;		
		while(i < DB_SIZE){
			index.insert(generateVector());
			if(i % 100000 == 0){
				logger.info("Loading: " + i);
			}
			i++;
		}
		
		// prepare the index
		logger.info("Preparing the index...");
		a.freeze();
		// add the rest of the objects
		/*while(i < DB_SIZE){
			index.insert(generateVector());
			if(i % 100000 == 0){
				logger.info("Loading: " + i);
			}
			i++;
		}*/
		
		// now we can match some objects!		
		logger.info("Querying the index...");
		i = 0;
		index.resetStats(); // reset the stats counter
		long start = System.currentTimeMillis();
		List<OBPriorityQueueShort<L1>> queryResults = new ArrayList<OBPriorityQueueShort<L1>>(QUERY_SIZE);
		List<L1> queries = new ArrayList<L1>(QUERY_SIZE);
		while(i < QUERY_SIZE){
			L1 q = 	generateVector();	
			// query the index with k=1			
			OBPriorityQueueShort<L1> queue = new OBPriorityQueueShort<L1>(1);			
			// perform a query with r=3000000 and k = 1 
			index.searchOB(q, Short.MAX_VALUE, queue);
			queryResults.add(queue);
			queries.add(q);
			
			i++;
		}
		// print the results of the set of queries. 
		long elapsed = System.currentTimeMillis() - start;
		logger.info("Time per query: " + elapsed / QUERY_SIZE + " millisec.");
		
		logger.info("Stats follow: (total distances / pivot vectors computed during the experiment)");
		logger.info(index.getStats().toString());

		/*
		logger.info("Doing EP validation");
		StaticBin1D ep = new StaticBin1D();
		

		Iterator<OBPriorityQueueShort<L1>> it1 = queryResults.iterator();
		Iterator<L1> it2 = queries.iterator();
		StaticBin1D seqTime = new StaticBin1D();
		i = 0;
		while(it1.hasNext()){
			OBPriorityQueueShort<L1> qu = it1.next();
			L1 q = it2.next();
			long time = System.currentTimeMillis();
			short[] sortedList = index.fullMatchLite(q, false);
			long el = System.currentTimeMillis() - time;
			seqTime.add(el);
			logger.info("Elapsed: " + el + " "  + i);
			OBQueryShort<L1> queryObj = new OBQueryShort<L1>(q, Short.MAX_VALUE, qu, null);
			ep.add(queryObj.ep(sortedList));
			i++;
		}
		
		logger.info(ep.toString());
		logger.info("Time per seq query: ");
		logger.info(seqTime.toString());
		*/
	}

}
