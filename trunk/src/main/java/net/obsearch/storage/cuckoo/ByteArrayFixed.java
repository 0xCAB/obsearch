package net.obsearch.storage.cuckoo;

import hep.aida.bin.StaticBin1D;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Iterator;
import java.util.NoSuchElementException;

import net.obsearch.asserts.OBAsserts;
import net.obsearch.constants.ByteConstants;
import net.obsearch.exception.OBException;
import net.obsearch.exception.OBStorageException;
import net.obsearch.storage.CloseIterator;
import net.obsearch.storage.TupleLong;

/**
 * An byte array in which each element has a fixed size.
 *
 */
public class ByteArrayFixed implements ByteArray{
	
	private int tupleSize;
	private int recordSize;
	
	private RandomAccessFile file;
	
	/**
	 * Size of the little header we append in front of the record
	 * to indicate if the record is available or not.
	 */
	private int ID_SIZE = ByteConstants.Byte.getSize();
	
	public ByteArrayFixed(int tupleSize, File in) throws  OBException{
		OBAsserts.chkAssert(tupleSize > 0, "Must store objects of size > 0");
		this.tupleSize = tupleSize + ID_SIZE;
		this.recordSize = tupleSize;
		try{
		file = new RandomAccessFile(new File(in, "main.az"),"rw");
		}catch(IOException e){
			throw new OBStorageException(e);
		}
	}

	@Override
	public long add(byte[] data) throws IOException, OBException {
		long res =  size();
		put(res, data );
		return res;
	}

	@Override
	public boolean delete(long i) throws OBException, IOException {
		boolean result = get(i) != null;
		file.seek(i * tupleSize);
		file.write(0); // erased the record.
		return result;
	}
	
	public void deleteAll() throws IOException{
		file.setLength(0);
	}

	@Override
	public byte[] get(long i) throws OBException, IOException {
		
		file.seek(i * tupleSize);
		int status = file.read();
		if(status == 0){
			return null;
		}else{
			assert status == 1;
			byte[] res = new byte[recordSize];
			file.read(res);
			return res;
		}
	}

	@Override
	public CloseIterator<TupleLong> iterator() {
		return new ByteArrayStorageIterator(0);
	}
	
	
	public class ByteArrayStorageIterator implements CloseIterator<TupleLong> {

		private long nextPointer;
		private TupleLong next;

		/**
		 * Create an iterator starting from "startPosition"
		 * 
		 * @param startPosition
		 */
		public ByteArrayStorageIterator(long startPosition) {
			nextPointer = startPosition;
			calculateNext();
		}
		// only returns non-null objects.
		private void calculateNext() {
			try {
				byte[] obj = null;
				long loadedPointer = -1;
				while(obj == null && nextPointer < size()) {
					obj = get(nextPointer);
					loadedPointer = nextPointer;
					nextPointer++; // leaves the pointer ready for the next
									// iteration.
				}
				if (obj == null) {
					next = null; // end everything
				} else {
					next = new TupleLong(loadedPointer, obj);
				}
			} catch (IOException e) {
				throw new NoSuchElementException(e.toString());
			} catch (OBException e) {
				throw new NoSuchElementException(e.toString());
			}
		}

		@Override
		public boolean hasNext() {
			return next != null;
			
		}

		@Override
		public TupleLong next() {			
			TupleLong toReturn = next;
			calculateNext();
			return toReturn;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();			
		}
		@Override
		public void closeCursor() throws OBException {
			// TODO Auto-generated method stub
			
		}
		
		

	}

	@Override
	public void put(long id, byte[] data) throws OBException, IOException {
		OBAsserts.chkAssert(data.length == recordSize, "Data does not have the correct size");
		long offset = id * tupleSize;
		file.seek(offset);
		file.write(1);// write the header.
		file.write(data);		
	}

	@Override
	public long size() throws IOException {
		return file.length() / tupleSize;
	}

	@Override
	public StaticBin1D fragmentationReport() throws IOException, OBException {		
		return null;
	}
	
	public void close() throws IOException {
		file.close();
	}

}