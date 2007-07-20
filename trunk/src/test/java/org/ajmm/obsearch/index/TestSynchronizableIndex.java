package org.ajmm.obsearch.index;


import java.io.File;

import junit.framework.TestCase;

import org.ajmm.obsearch.TUtils;
import org.ajmm.obsearch.example.OBSlice;
import org.junit.Before;

public class TestSynchronizableIndex extends TestCase{

	@Before
	public void setUp() throws Exception {
	}
	
	public void testSynchroTree() throws Exception{
		File dbFolder = new File(TUtils.getTestProperties().getProperty("test.db.path") );
    	IndexSmokeTUtil.deleteDB(dbFolder);
   	 	assertTrue(! dbFolder.exists());
   	 	assertTrue(dbFolder.mkdirs());
    	IndexShort<OBSlice> index = new PPTreeShort<OBSlice>(
                dbFolder, (byte) 30, (byte) 2, (short)0, (short) 200);

    	SynchronizableIndexShort<OBSlice> index2 = new SynchronizableIndexShort<OBSlice>(index, dbFolder);
    	IndexSmokeTUtil t = new IndexSmokeTUtil();
    	t.tIndex(index2);
    }

}