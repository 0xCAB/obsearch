package org.ajmm.obsearch.index;

import java.io.File;

import junit.framework.TestCase;

import org.ajmm.obsearch.example.OBSlice;
import org.ajmm.obsearch.example.OBSliceFactory;
import org.ajmm.obsearch.index.pivotselection.DummyPivotSelector;
import org.ajmm.obsearch.index.utils.Directory;
import org.ajmm.obsearch.index.utils.IndexSmokeTUtil;
import org.ajmm.obsearch.index.utils.TUtils;
import org.junit.Before;

/*
 OBSearch: a distributed similarity search engine
 This project is to similarity search what 'bit-torrent' is to downloads.
 Copyright (C)  2007 Arnoldo Jose Muller Molina

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
/**
 * Test SynchronizableIndex.
 * @author Arnoldo Jose Muller Molina
 * @since 0.7
 */

public class TestSynchronizableIndex
        extends TestCase {

    /**
     * Call IndexSmokeTUtil and test a SynchronizableIndex.
     * @throws Exception
     *             If something goes really bad.
     */
    public void testSyncTree() throws Exception {
        File dbFolder = new File(TUtils.getTestProperties().getProperty(
                "test.db.path"));
        File dbFolderSync = new File(TUtils.getTestProperties().getProperty(
        "test.db.path")+ File.separator + "sync");
        
        Directory.deleteDirectory(dbFolder);
        assertTrue(!dbFolder.exists());
        assertTrue(dbFolder.mkdirs());
        assertTrue(dbFolderSync.mkdirs());
        DummyPivotSelector ps = new DummyPivotSelector();
        IndexShort < OBSlice > index = new PPTreeShort < OBSlice >(dbFolder,
                (byte) 30, (byte) 2, (short) 0, (short) (OBSliceFactory.maxSliceSize * 2), ps, OBSlice.class);
        SynchronizableIndexShort < OBSlice > index2 = new SynchronizableIndexShort < OBSlice >(
                index, dbFolderSync );
        IndexSmokeTUtil<OBSlice> t = new IndexSmokeTUtil<OBSlice>(new OBSliceFactory());
        t.tIndex(index2);
        
        
    }

}