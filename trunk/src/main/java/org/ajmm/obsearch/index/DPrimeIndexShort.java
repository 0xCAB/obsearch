package org.ajmm.obsearch.index;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.ajmm.obsearch.Index;
import org.ajmm.obsearch.OB;
import org.ajmm.obsearch.Result;
import org.ajmm.obsearch.exception.IllegalIdException;
import org.ajmm.obsearch.exception.NotFrozenException;
import org.ajmm.obsearch.exception.OBException;
import org.ajmm.obsearch.exception.OBStorageException;
import org.ajmm.obsearch.exception.OutOfRangeException;
import org.ajmm.obsearch.index.d.BucketContainer;
import org.ajmm.obsearch.index.d.BucketContainerShort;
import org.ajmm.obsearch.index.d.ObjectBucket;
import org.ajmm.obsearch.index.d.ObjectBucketShort;
import org.ajmm.obsearch.index.utils.ShortUtils;
import org.ajmm.obsearch.ob.OBShort;
import org.ajmm.obsearch.query.OBQueryShort;
import org.ajmm.obsearch.result.OBPriorityQueueShort;
import org.ajmm.obsearch.result.OBResultShort;
import org.ajmm.obsearch.storage.OBStoreFactory;
import org.apache.log4j.Logger;

import cern.colt.list.IntArrayList;
import cern.colt.list.ShortArrayList;

import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;
import com.sleepycat.je.DatabaseException;

public final class DPrimeIndexShort < O extends OBShort >
        extends
        AbstractDPrimeIndex < O, ObjectBucketShort, OBQueryShort < O >, BucketContainerShort < O > >
        implements IndexShort < O > {

    /**
     * P parameter that indicates the maximum radius that we will accept.
     */
    private short p;

    /**
     * Median data for each level and pivot.
     */
    private short[] median;

    /**
     * Obtain max distance per pivot... hack. Remove this in the future.
     */
    private short maxDistance;

    /**
     * For each pivot, we have here how many objects fall in distance x. Max:
     * right of the median Min: left of the median
     */
    private int[][] distanceDistributionRight;

    private int[][] distanceDistributionLeft;

    protected float[][] normalizedProbsRight;

    protected float[][] normalizedProbsLeft;

    /**
     * Logger.
     */
    private static final transient Logger logger = Logger
            .getLogger(DPrimeIndexShort.class);

    /**
     * Creates a new DIndex for shorts
     * @param fact
     *                Storage factory to use
     * @param pivotCount
     *                number of pivots to use.
     * @param pivotSelector
     *                Pivot acceptance criteria.
     * @param type
     *                The type of objects to use (needed to create new
     *                instances)
     * @param nextLevelThreshold
     *                threshold used to reduce the number of pivots per level.
     * @param p
     *                P parameter of D-Index.
     * @throws OBStorageException
     *                 If something goes wrong with the storage device.
     * @throws OBException
     *                 If some other exception occurs.
     */
    public DPrimeIndexShort(OBStoreFactory fact, byte pivotCount,
            IncrementalPivotSelector < O > pivotSelector, Class < O > type,
            short p) throws OBStorageException, OBException {
        super(fact, pivotCount, pivotSelector, type);
        this.p = p;
    }

    protected ObjectBucketShort getBucket(O object, short p) throws OBException {

        int i = 0;
        ArrayList < O > piv = super.pivots;
        short[] smapVector = new short[piv.size()];
        long bucketId = 0;
        while (i < piv.size()) {
            short distance = piv.get(i).distance(object);
            smapVector[i] = distance;
            i++;
        }
        ObjectBucketShort res = new ObjectBucketShort(bucketId, 0, smapVector,
                false, -1);
        updateBucket(res);
        return res;
    }

    /**
     * Calculate a new bucket based on the smap vector of the given b Warning,
     * this method destroys the previously available info in the given bucket b.
     * It keeps the smap vector intact.
     * @param b
     *                We will take the smap vector from here.
     * @param level
     *                Level of the hash table
     * @param p
     *                P value to use
     * @throws OBException
     */
    protected void updateBucket(ObjectBucketShort b) throws OBException {
        int i = 0;
        ArrayList < O > piv = super.pivots;
        short[] medians = median;
        short[] smapVector = b.getSmapVector();
        long bucketId = 0;
        while (i < piv.size()) {
            short distance = smapVector[i];
            int r = bps(medians[i], distance);
            if (r == 1) {
                bucketId = bucketId | super.masks[i];
            }
            i++;
        }
        b.setBucket(bucketId);
        b.setExclusionBucket(false);
        b.setLevel(0);

    }

    @Override
    protected ObjectBucketShort getBucket(O object) throws OBException {
        int level = 0;
        ObjectBucketShort res = null;
        while (level < super.pivots.size()) {
            res = getBucket(object, (short) 0);
            if (!res.isExclusionBucket()) {
                break;
            }
            level++;
        }
        assert res != null;
        return res;
    }

    /**
     * Bps function. Returns 0 if d(o,p) <= median - p . Returns 1 if d(o,p) >
     * median +p. Returns 2 otherwise.
     * @param median
     *                Median obtained for the given pivot.
     * @param distance
     *                Distance of the pivot and the object we are processing
     * @return Returns 0 if d(o,p) <= median - p . Returns 1 if d(o,p) > median
     *         +p. Returns 2 otherwise.
     */
    private int bps(short median, short distance) {
        if (distance <= median) {
            return 0;
        } else {
            return 1;
        }
    }

    private int bpsRange(short median, short distance, short range) {
        boolean a = distance - range <= median;
        boolean b = distance + range >= median;
        if (a && b) {
            return 2;
        } else if (a) {
            return 0;
        } else if (b) {
            return 1;
        } else {
            assert false;
            return -1;
        }
    }

    protected void calculateMedians(IntArrayList elementsSource)
            throws OBStorageException, IllegalIdException,
            IllegalAccessException, InstantiationException, DatabaseException,
            OutOfRangeException, OBException {
        int i = 0;
        ArrayList < O > pivots = super.pivots;

        int max;
        if (elementsSource == null) {
            max = (int) A.size();
        } else {
            max = elementsSource.size();
        }

        logger.debug("Calculating medians. max: " + max);
        assert pivots.size() > 0;
        median = new short[pivots.size()];
        while (i < pivots.size()) {
            O p = pivots.get(i);
            int cx = 0;
            ShortArrayList medianData = new ShortArrayList(max);
            // calculate median for pivot p
            while (cx < max) {
                O o = getObjectFreeze(cx, elementsSource);
                short d = p.distance(o);
                if (maxDistance < d) {
                    maxDistance = d;
                }
                medianData.add(d);
                cx++;
            }

            median[i] = median(medianData);
            i++;
        }
        logger.info("max distance: " + maxDistance);
        maxDistance++;
        assert i > 0;
        if (logger.isDebugEnabled()) {
            logger.debug("Found medians: " + Arrays.toString(median));
        }

        assert super.pivots.size() == median.length : "Piv: "
                + super.pivots.size() + " Med: " + median.length;
        this.distanceDistributionRight = new int[pivots.size()][maxDistance];
        this.distanceDistributionLeft = new int[pivots.size()][maxDistance];
    }

    /**
     * Updates probability information.
     * @param b
     */
    protected void updateProbabilities(ObjectBucketShort b) {
        int i = 0;
        while (i < pivots.size()) {
            if (bps(median[i], b.getSmapVector()[i]) == 1) {
                this.distanceDistributionRight[i][b.getSmapVector()[i]]++;
            } else {
                this.distanceDistributionLeft[i][b.getSmapVector()[i]]++;
            }
            i++;
        }
    }

    protected void normalizeProbs() throws OBStorageException {
        normalizedProbsRight = new float[pivots.size()][this.maxDistance];
        normalizedProbsLeft = new float[pivots.size()][this.maxDistance];
        long total = A.size();
        int i = 0;
        while (i < pivots.size()) {
            int cx = 0;
            while (cx < maxDistance) {
                normalizedProbsRight[i][cx] = ((float) distanceDistributionRight[i][cx] / (float) total);
                normalizedProbsLeft[i][cx] = ((float) distanceDistributionLeft[i][cx] / (float) total);
                cx++;
            }
            i++;
        }
    }

    private short median(ShortArrayList medianData) {
        medianData.sort();
        return medianData.get(medianData.size() / 2);
    }

    protected BucketContainerShort < O > instantiateBucketContainer(byte[] data) {
        return new BucketContainerShort < O >(this, data);
    }

    /*
     * (non-Javadoc)
     * @see org.ajmm.obsearch.index.IndexShort#intersectingBoxes(org.ajmm.obsearch.ob.OBShort,
     *      short)
     */
    @Override
    public int[] intersectingBoxes(O object, short r)
            throws NotFrozenException, DatabaseException,
            InstantiationException, IllegalIdException, IllegalAccessException,
            OutOfRangeException, OBException {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * @see org.ajmm.obsearch.index.IndexShort#intersects(org.ajmm.obsearch.ob.OBShort,
     *      short, long)
     */
    @Override
    public boolean intersects(O object, short r, int box)
            throws NotFrozenException, DatabaseException,
            InstantiationException, IllegalIdException, IllegalAccessException,
            OutOfRangeException, OBException {
        throw new UnsupportedOperationException();

    }

    /*
     * (non-Javadoc)
     * @see org.ajmm.obsearch.index.IndexShort#searchOB(org.ajmm.obsearch.ob.OBShort,
     *      short, org.ajmm.obsearch.result.OBPriorityQueueShort, long[])
     */
    @Override
    public void searchOB(O object, short r, OBPriorityQueueShort < O > result,
            int[] boxes) throws NotFrozenException, DatabaseException,
            InstantiationException, IllegalIdException, IllegalAccessException,
            OutOfRangeException, OBException {
        throw new UnsupportedOperationException();

    }

    /*
     * (non-Javadoc)
     * @see org.ajmm.obsearch.index.IndexShort#searchOB(org.ajmm.obsearch.ob.OBShort,
     *      short, org.ajmm.obsearch.result.OBPriorityQueueShort)
     */
    @Override
    /*
     * public void searchOB(O object, short r, OBPriorityQueueShort < O >
     * result) throws NotFrozenException, DatabaseException,
     * InstantiationException, IllegalIdException, IllegalAccessException,
     * OutOfRangeException, OBException { OBQueryShort<O> q = new OBQueryShort<O>(object,r,
     * result); int i = 0; ObjectBucketShort b = null; while(i <
     * pivots.size()){// search through all the levels. b = getBucket(object, i,
     * (short)(p + r)); if(! b.isExclusionBucket()){ BucketContainerShort<O> bc =
     * super.bucketContainerCache.get(super.getBucketStorageId(b));
     * bc.search(q,b); return; } if(r <= p){ this.updateBucket(b, i, (short)(p -
     * r)); if(! b.isExclusionBucket()){ BucketContainerShort<O> bc =
     * super.bucketContainerCache.get(super.getBucketStorageId(b)); bc.search(q,
     * b); } }else{ throw new UnsupportedOperationException("Only supporting
     * ranges < p"); } i++; } // finally, search the exclusion bucket :)
     * BucketContainerShort<O> bc =
     * super.bucketContainerCache.get(super.exclusionBucketId); bc.search(q, b); }
     */
    public void searchOB(O object, short r, OBPriorityQueueShort < O > result)
            throws NotFrozenException, DatabaseException,
            InstantiationException, IllegalIdException, IllegalAccessException,
            OutOfRangeException, OBException {
        OBQueryShort < O > q = new OBQueryShort < O >(object, r, result);

        ObjectBucketShort b = null;
        this.queryCount++;
        b = getBucket(object);

        doIt(b, q, 0, 0);
        /*
         * while (i < pivots.size()) {// search through all the levels. b =
         * getBucket(object, i, (short) 0); assert !b.isExclusionBucket();
         * BucketContainerShort < O > bc = super.bucketContainerCache
         * .get(super.getBucketStorageId(b)); bc.search(q, b); i++; } //
         * finally, search the exclusion bucket :) BucketContainerShort < O > bc =
         * super.bucketContainerCache .get(super.exclusionBucketId);
         * bc.search(q, b);
         */
    }

    /**
     * Does the match for the given index for the given pivot.
     * @param b
     * @param q
     * @param pivotIndex
     */
    private void doIt(ObjectBucketShort b, OBQueryShort < O > q,
            int pivotIndex, long block) throws NotFrozenException,
            DatabaseException, InstantiationException, IllegalIdException,
            IllegalAccessException, OutOfRangeException, OBException {
        if (pivotIndex < super.pivots.size()) {
            int r = bpsRange(median[pivotIndex], b.getSmapVector()[pivotIndex],
                    q.getDistance());
            if (r == 2) { // if we have to do both
                short[] smap = b.getSmapVector();
               if(this.distanceDistributionRight[pivotIndex][smap[pivotIndex]] > this.distanceDistributionLeft[pivotIndex][smap[pivotIndex]]){
                   // do 1 first
                   long newBlock = block | super.masks[pivotIndex];
                   if (super.filter.get(pivotIndex).contains(newBlock)) {
                       doIt(b, q, pivotIndex + 1, newBlock);
                   }
                   r = bpsRange(median[pivotIndex], b.getSmapVector()[pivotIndex],
                           q.getDistance());
                   if ((r== 2 || r == 0) && super.filter.get(pivotIndex).contains(block)) {
                       doIt(b, q, pivotIndex + 1, block);
                   }
                   
               }else{
                   // 0 first
                   if (super.filter.get(pivotIndex).contains(block)) {
                       doIt(b, q, pivotIndex + 1, block);
                   }
                   r = bpsRange(median[pivotIndex], b.getSmapVector()[pivotIndex],
                           q.getDistance());
                   long newBlock = block | super.masks[pivotIndex];
                   if ((r== 2 || r == 1) && super.filter.get(pivotIndex).contains(newBlock)) {
                       
                       doIt(b, q, pivotIndex + 1, newBlock);
                   }
               }

            } else { // only one of the sides is selected
                if (r == 0 && super.filter.get(pivotIndex).contains(block)) {
                    doIt(b, q, pivotIndex + 1, block);
                } else {
                    long newBlock = block | super.masks[pivotIndex];
                    if (super.filter.get(pivotIndex).contains(newBlock)) {
                        doIt(b, q, pivotIndex + 1, newBlock);
                    }
                }
            }

        } else {
            // we have finished
            BucketContainerShort < O > bc = super.bucketContainerCache
                    .get(block);
            super.distanceComputations += bc.search(q, b);
            searchedBoxesTotal++;
            smapRecordsCompared += bc.size();
        }
    }

    @Override
    public Result exists(O object) throws DatabaseException, OBException,
            IllegalAccessException, InstantiationException {
        OBPriorityQueueShort < O > result = new OBPriorityQueueShort < O >(
                (byte) 1);
        searchOB(object, (short) 0, result);
        Result res = new Result();
        res.setStatus(Result.Status.NOT_EXISTS);
        if (result.getSize() == 1) {
            Iterator < OBResultShort < O >> it = result.iterator();
            OBResultShort < O > r = it.next();
            if (r.getObject().equals(object)) {
                res.setId(r.getId());
                res.setStatus(Result.Status.EXISTS);
            }
        }
        return res;
    }

}
