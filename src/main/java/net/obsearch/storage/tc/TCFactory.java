package net.obsearch.storage.tc;

import java.io.File;
import java.math.BigInteger;

import tokyocabinet.BDB;
import tokyocabinet.DBM;
import tokyocabinet.FDB;
import tokyocabinet.HDB;

import net.obsearch.asserts.OBAsserts;
import net.obsearch.constants.OBSearchProperties;
import net.obsearch.exception.OBException;
import net.obsearch.exception.OBStorageException;
import net.obsearch.storage.OBStorageConfig;
import net.obsearch.storage.OBStore;
import net.obsearch.storage.OBStoreByte;
import net.obsearch.storage.OBStoreDouble;
import net.obsearch.storage.OBStoreFactory;
import net.obsearch.storage.OBStoreFloat;
import net.obsearch.storage.OBStoreInt;
import net.obsearch.storage.OBStoreLong;
import net.obsearch.storage.OBStoreShort;
import net.obsearch.storage.TupleBytes;
import net.obsearch.storage.OBStorageConfig.IndexType;
import net.obsearch.utils.bytes.ByteConversion;

public class TCFactory implements OBStoreFactory {
	/**
	 * Directory where all the objects will be created.
	 */
	private String directory;
	
	public TCFactory(File directory){
		this.directory = directory.getAbsolutePath();
	}

	@Override
	public void close() throws OBStorageException {

	}

	@Override
	public OBStore<TupleBytes> createOBStore(String name, OBStorageConfig config)
			throws OBStorageException, OBException {

		DBM db = null;
		String path = directory + File.separator + name;
		if (IndexType.HASH == config.getIndexType() || IndexType.DEFAULT == config.getIndexType()) {
			HDB tdb = new HDB();
			tdb.open(path, HDB.OCREAT | HDB.OREADER | HDB.OWRITER);
			OBAsserts.chkAssertStorage(tdb.tune(OBSearchProperties.getLongProperty("tc.expected.db.count") * 4, -1, -1, HDB.TLARGE  ), "Could not set the tuning parameters for the hash table" );
			db = tdb;
		} else if (IndexType.BTREE == config.getIndexType()) {
			BDB tdb = new BDB();
			tdb.open(path, BDB.OCREAT | BDB.OREADER | BDB.OWRITER);
			db = tdb;
		} else if(IndexType.FIXED_RECORD == config.getIndexType()){
			FDB tdb = new FDB();
			tdb.open(path, FDB.OCREAT | FDB.OREADER | FDB.OWRITER);
			OBAsserts.chkAssert(config.getRecordSize() > 0, "Invalid record size");
			OBAsserts.chkAssertStorage(tdb.tune(config.getRecordSize(), OBSearchProperties.getIntProperty("tc.fdb.max.file.size")), "Could not set the tuning parameters for the fixed record device");
			db = tdb;
		}else{
			OBAsserts.fail("Fatal error, invalid index type.");
		}
		
		TCOBStorageBytesArray t = new TCOBStorageBytesArray(name, db, this, config);
		return t;
	}

	@Override
	public OBStoreByte createOBStoreByte(String name, OBStorageConfig config)
			throws OBStorageException, OBException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OBStoreDouble createOBStoreDouble(String name, OBStorageConfig config)
			throws OBStorageException, OBException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OBStoreFloat createOBStoreFloat(String name, OBStorageConfig config)
			throws OBStorageException, OBException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OBStoreInt createOBStoreInt(String name, OBStorageConfig config)
			throws OBStorageException, OBException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OBStoreLong createOBStoreLong(String name, OBStorageConfig config)
			throws OBStorageException, OBException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OBStoreShort createOBStoreShort(String name, OBStorageConfig config)
			throws OBStorageException, OBException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BigInteger deSerializeBigInteger(byte[] value) {		
		return null;
	}

	@Override
	public byte deSerializeByte(byte[] value) {
		return ByteConversion.bytesToByte(value);
	}

	@Override
	public double deSerializeDouble(byte[] value) {
		return ByteConversion.bytesToDouble(value);
	}

	@Override
	public float deSerializeFloat(byte[] value) {
		return ByteConversion.bytesToFloat(value);
	}

	@Override
	public int deSerializeInt(byte[] value) {
		return ByteConversion.bytesToInt(value);
	}

	@Override
	public long deSerializeLong(byte[] value) {
		return ByteConversion.bytesToLong(value);
	}

	@Override
	public short deSerializeShort(byte[] value) {
		return ByteConversion.bytesToShort(value);
	}

	@Override
	public String getFactoryLocation() {
		return this.directory;
	}

	@Override
	public void removeOBStore(OBStore storage) throws OBStorageException {
		storage.deleteAll();
		storage.close();
	}

	@Override
	public byte[] serializeBigInteger(BigInteger value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] serializeByte(byte value) {
		return ByteConversion.byteToBytes(value);
	}

	@Override
	public byte[] serializeDouble(double value) {
		return ByteConversion.doubleToBytes(value);
	}

	@Override
	public byte[] serializeFloat(float value) {
		return ByteConversion.floatToBytes(value);
	}

	@Override
	public byte[] serializeInt(int value) {
		return ByteConversion.intToBytes(value);
	}

	@Override
	public byte[] serializeLong(long value) {
		return ByteConversion.longToBytes(value);
	}

	@Override
	public byte[] serializeShort(short value) {
		return ByteConversion.shortToBytes(value);
	}

	@Override
	public Object stats() throws OBStorageException {
		// TODO Auto-generated method stub
		return null;
	}

}
