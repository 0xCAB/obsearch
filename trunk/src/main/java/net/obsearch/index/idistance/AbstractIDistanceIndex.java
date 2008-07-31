package net.obsearch.index.idistance;

import java.io.IOException;
import java.nio.ByteBuffer;

import net.obsearch.OB;
import net.obsearch.cache.OBCacheLoaderInt;
import net.obsearch.cache.OBCacheLoaderByteArray;
import net.obsearch.exception.AlreadyFrozenException;
import net.obsearch.exception.IllegalIdException;
import net.obsearch.exception.OBException;
import net.obsearch.exception.OBStorageException;
import net.obsearch.exception.OutOfRangeException;
import net.obsearch.index.bucket.AbstractBucketIndex;
import net.obsearch.index.bucket.BucketContainer;
import net.obsearch.index.bucket.BucketObject;
import net.obsearch.pivots.IncrementalPivotSelector;
import net.obsearch.storage.OBStore;
import net.obsearch.storage.OBStoreFactory;
import net.obsearch.storage.TupleBytes;

public abstract class AbstractIDistanceIndex<O extends OB, B extends BucketObject, Q, BC extends BucketContainer<O, B, Q>>
		extends AbstractBucketIndex<O, B, Q, BC> {

	/**
	 * Initializes this abstract class.
	 * 
	 * @param fact
	 *            The factory that will be used to create storage devices.
	 * @param pivotCount
	 *            # of Pivots that will be used.
	 * @param pivotSelector
	 *            The pivot selection mechanism to be used. (64 pivots can
	 *            create 2^64 different buckets, so we have limited the # of
	 *            pivots available. If you need more pivots, then you would have
	 *            to use the Smapped P+Tree (PPTree*).
	 * @param type
	 *            The type that will be stored in this index.
	 * @param nextLevelThreshold
	 *            How many pivots will be used on each level of the hash table.
	 * @throws OBStorageException
	 *             if a storage device could not be created for storing objects.
	 */
	protected AbstractIDistanceIndex(Class<O> type,
			IncrementalPivotSelector<O> pivotSelector, int pivotCount)
			throws OBStorageException, OBException {
		super(type, pivotSelector, pivotCount);
	}

	public void init(OBStoreFactory fact) throws OBStorageException,
			OBException, InstantiationException, IllegalAccessException {
		super.init(fact);
		super.initByteArrayBuckets();
	}

	public void freeze() throws IOException, AlreadyFrozenException,
			IllegalIdException, IllegalAccessException, InstantiationException,
			OutOfRangeException, OBException {
		super.freeze();
		int i = 0;
		while (i < A.size()) {
			O o = getObjectFreeze(i, null);
			insertAux(i, o);
			i++;
		}
		bucketStats();
	}

}