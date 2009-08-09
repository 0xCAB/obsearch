<@pp.dropOutputFile />
<#include "/@inc/ob.ftl">
<#include "/@inc/index.ftl">
<#list types as t>
<@type_info t=t/>
<@pp.changeOutputFile name="Sketch64${Type}.java" />
package net.obsearch.index.ghs.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import net.obsearch.index.ghs.CBitVector;
import net.obsearch.index.Commons${Type};
import net.obsearch.AbstractOBResult;
import net.obsearch.asserts.OBAsserts;
import net.obsearch.constants.ByteConstants;
import net.obsearch.exception.IllegalIdException;
import net.obsearch.exception.NotFrozenException;
import net.obsearch.exception.OBException;
import net.obsearch.exception.OBStorageException;
import net.obsearch.exception.OutOfRangeException;
import net.obsearch.filter.Filter;
import net.obsearch.filter.FilterNonEquals;
import net.obsearch.index.Index${Type};
import net.obsearch.index.bucket.impl.BucketObject${Type};
import net.obsearch.index.bucket.sleek.SleekBucket${Type};
import net.obsearch.index.ghs.AbstractSketch64;
import net.obsearch.ob.OB${Type};
import net.obsearch.pivots.IncrementalPairPivotSelector;
import net.obsearch.pivots.IncrementalPivotSelector;
import net.obsearch.query.AbstractOBQuery;
import net.obsearch.query.OBQuery${Type};
import net.obsearch.result.OBPriorityQueue${Type};
import net.obsearch.result.OBResultInvertedByte;
import net.obsearch.result.OBResultInverted${Type};
import net.obsearch.result.OBResult${Type};
import net.obsearch.index.ghs.SketchProjection;

public final class Sketch64${Type}<O extends OB${Type}> extends AbstractSketch64<O, BucketObject${Type}<O>, OBQuery${Type}<O>, SleekBucket${Type}<O>>
implements Index${Type}<O> {
	
	private boolean DEBUG = true;
	
	private static final transient Logger logger = Logger
	.getLogger(Sketch64${Type}.class.getName());
	
	/**
	 * Create a new Sketch64${Type} with m bytes. 
	 * @param type Type of object that will be stored
	 * @param pivotSelector Pivot selection strategy to be employed.
	 * @param m The number of bits
	 * @param bucketPivotCount Number of pivots per bucket
	 * @throws OBStorageException
	 * @throws OBException
	 * @throws IOException
	 */
	public Sketch64${Type}(Class<O> type,
			IncrementalPairPivotSelector<O> pivotSelector, int m, int bucketPivotCount
			)												
			throws OBStorageException, OBException, IOException {
		
		super(type, pivotSelector,  m, bucketPivotCount);
		
	}
	
	@Override
	public BucketObject${Type}<O> getBucket(O object) throws OBException,
			InstantiationException, IllegalAccessException {
		BucketObject${Type}<O> res = new BucketObject${Type}<O>(null, -1L, object);
		return res;
	}

	
	/**
	 * Compute the sketch for the given object.
	 */
	@Override
	public SketchProjection getProjection(BucketObject${Type}<O> bucket) throws OBException {
			//OBAsserts.chkAssert(m <= Byte.MAX_VALUE, "Cannot address more than Byte.MAX_VALUE");
		int i = 0;
		CBitVector res = new CBitVector(m);
		double[] lowerBounds = new double[m];
		while(i < m){
				${type} distanceA = pivotGrid[i][0].distance(bucket.getObject());
				${type} distanceB = pivotGrid[i][1].distance(bucket.getObject());
			if(distanceA > distanceB ){
					res.set(i);
				distortionStats[i][1]++;
			}else{
				distortionStats[i][0]++;
			}
			// update the lower bounds:
			lowerBounds[i] = Math.max(((double)Math.abs(distanceA - distanceB)) / 2, 0);
			i++;
		}

		// calculate distances that are farther to the hyperplane
		double[] lowerBoundsSorted = new double[m];
		System.arraycopy(lowerBounds,0,lowerBoundsSorted,0,m);
		Arrays.sort(lowerBoundsSorted);
		byte[] ordering = new byte[m];
		i = 0;
		while(i < m){
				
				ordering[i] = (byte)Arrays.binarySearch(lowerBoundsSorted, lowerBounds[i]);
				i++;
		}
		
		SketchProjection result = new SketchProjection(ordering, res, -1, lowerBounds);
		return result;
	}

	
	@Override
	protected SleekBucket${Type}<O> instantiateBucketContainer(
			byte[] data, byte[] address) throws InstantiationException, IllegalAccessException, OBException {
		if(data == null){
			return new SleekBucket${Type}<O>( type, bucketPivotCount);
		}else{
			try{
				return new SleekBucket${Type}<O>( type, bucketPivotCount, data);
			}catch(IOException e){
				throw new OBException(e);
			}
		
		}
	}
	
	@Override
	protected int primitiveDataTypeSize() {
		return ByteConstants.${Type}.getSize();
	}
	
	
	
	
	//TODO: raise this method one level up and change all the interfaces.
	@Override
	public void searchOB(O object, ${type} r, OBPriorityQueue${Type}<O> result)
			throws NotFrozenException, InstantiationException,
			IllegalIdException, IllegalAccessException, OutOfRangeException,
			OBException {
		searchOB(object, r,null, result);
		
				
	}
	
	@Override
	protected AbstractOBQuery<O> getKQuery(O object, int k) throws OBException, InstantiationException, IllegalAccessException {
		BucketObject${Type}<O> b = getBucket(object);
		OBPriorityQueue${Type}<O> result = new OBPriorityQueue${Type}<O>(k);
		OBQuery${Type}<O> q = new OBQuery${Type}<O>(object, ${ClassType}.MAX_VALUE, result, b
				.getSmapVector());
		return q;
	}


	@Override
	public void searchOB(O object, ${type} r, Filter<O> filter,
			OBPriorityQueue${Type}<O> result) throws NotFrozenException,
			InstantiationException, IllegalIdException, IllegalAccessException,
			OutOfRangeException, OBException {
		BucketObject${Type}<O> b = getBucket(object);

		OBQuery${Type}<O> q = new OBQuery${Type}<O>(object, r, result, b
				.getSmapVector());
		SketchProjection query = this.getProjection(b);
		
		// we only use the k estimation once, at the beginning of the search process.
		int kEstimation = estimateK(result.getK());
		stats.addExtraStats("K-estimation", kEstimation);
		long time = System.currentTimeMillis();
		List<SketchProjection> sortedBuckets = searchBuckets(query, kEstimation);
		getStats().addExtraStats("Buckets_search_time", System.currentTimeMillis() - time);
		for(SketchProjection bucket: sortedBuckets){
				SleekBucket${Type}<O> container = this.bucketCache.get(bucket.getAddress());
			stats.incBucketsRead();
			container.search(q, b, filter, getStats());															
		}
	}
	

	
	/**
	 * This method returns a list of all the distances of the query against  the DB.
	 * This helps to calculate EP values in a cheaper way. results that are equal to the original object are added
	 * as ${Type}.MAX_VALUE
	 * @param query
	 * @param filterSame if True we do not return objects o such that query.equals(o)
	 * @return
	 * @throws OBException 
	 * @throws InstantiationException 
	 * @throws IllegalAccessException 
	 */
	public ${type}[] fullMatchLite(O query, boolean filterSame) throws OBException, IllegalAccessException, InstantiationException{
			return Commons${Type}.fullMatchLite((OB${Type})query, filterSame, this);
	}
	
	
	/**
	 * Get the kMax closest objects. Count how many different bucket ids are
	 * there for each k and fill in accordingly the tables.
	 * 
	 * @param object
	 * @throws OBException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	protected void maxKEstimationAux(O object)
			throws OBException, InstantiationException, IllegalAccessException {
		// we calculate first the list of elements against the DB.
		BucketObject${Type}<O> b = getBucket(object);
		SketchProjection longAddr = getProjection(b);
		byte[] addr = longAddr.getAddress();
		
		

		${type} [] sortedList = fullMatchLite( object, true);
		//`calculate the AVG distance between the sample and the DB.
		for(${type} t : sortedList){
				getStats().addExtraStats("GENERAL_DISTANCE", t);
		}
		// we now calculate the buckets and we sort them
		// according to the distance of the query.
		
		if(sketchSet == null){
			loadMasks();
		}
		long time = System.currentTimeMillis();
		OBAsserts.chkAssert(Buckets.size() <= Integer.MAX_VALUE, "Capacity exceeded");
		List<SketchProjection> sortedBuckets = searchBuckets(longAddr, (int)Buckets.size());
		
		//List<OBResult${Type}<O>> sortedBuckets2 = fullMatch(object);
		logger.info("Time searching sketches: " + (System.currentTimeMillis() - time));

		// now we have to calculate the EP for each k up to maxK
		// and for each k we calculate how many buckets must be read in order
		// to obtain ep less than
		int i = 0;
		// container
		// used to
		// read
		// data.
		FilterNonEquals<O> fne = new FilterNonEquals<O>();
		while (i < getMaxK().length) {
			double ep = 1;
			int goodK = 0;
			// get the query for the
			AbstractOBQuery<O> queryAbst = getKQuery(object, userK[i]);
			OBQuery${Type}<O> query = (OBQuery${Type}<O>) queryAbst;
			
			for (SketchProjection result : sortedBuckets) {
				
				SleekBucket${Type}<O> container = this.bucketCache.get(result.getAddress());
				// search the objects
				assert container != null : "Problem while loading: " + result.getSketch();
				container.search(query, b, fne, getStats());
				// calculate the ep of the query and the DB.
				if (query.isFull()) { // only if the query is full of items.
					ep = query.ep(sortedList);
					//double epOld = query.ep((List)sortedBuckets2);
					//OBAsserts.chkAssert(Math.abs(ep - epOld)<= 1f / sortedList.length, "oops: new: " + ep + " old: " + epOld);					
				}
				goodK++;
				if (ep <= this.getExpectedEP()) {
					// add the information to the stats:
					// goodK buckets required to retrieve with k==i.
						logger.info("Found result after reading: " + goodK + " buckets ");
					kEstimators[i].add(goodK);
					// store the distance of the best object and the real-best object
					${type} difference = (${type})Math.abs(sortedList[0] - query.getResult().getSortedElements().get(0).getDistance());
					getStats().addExtraStats("SEXY_DISTANCE", difference);
					break; // we are done.
				}
			}
			i++;
		}

	}
	
	
	protected double distance(O a, O b) throws OBException{
		return a.distance(b);
	}



	@Override
	protected SketchProjection calculateDistance(SketchProjection query,
			CBitVector proj) {
		
			return query.distance(proj);
	}

	@Override
	protected int getCPSize() {
			return Math.max((int)Math.ceil(m  /  ByteConstants.Byte.getBits() ), ByteConstants.Long.getSize());
	}

	@Override
	protected Class<CBitVector> getPInstance() {
			return CBitVector.class;
	}

	
}
</#list>