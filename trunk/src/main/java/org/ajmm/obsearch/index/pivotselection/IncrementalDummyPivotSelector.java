package org.ajmm.obsearch.index.pivotselection;

import org.ajmm.obsearch.Index;
import org.ajmm.obsearch.OB;
import org.ajmm.obsearch.exception.OBException;
import org.ajmm.obsearch.exception.OBStorageException;
import org.ajmm.obsearch.exception.PivotsUnavailableException;

import cern.colt.list.IntArrayList;

/*
 OBSearch: a distributed similarity search engine This project is to
 similarity search what 'bit-torrent' is to downloads. 
 Copyright (C) 2008 Arnoldo Jose Muller Molina

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
 * IncrementalDummyPivotSelector is used for testing purposes.
 * @author Arnoldo Jose Muller Molina
 */

public class IncrementalDummyPivotSelector < O extends OB >
        extends AbstractIncrementalPivotSelector < O > {
    
    public IncrementalDummyPivotSelector(){
        super(null);
    }

    @Override
    public int[] generatePivots(short pivotCount, Index<O> index)
            throws OBException, IllegalAccessException, InstantiationException,
            OBStorageException, PivotsUnavailableException {
        // TODO Auto-generated method stub
        return generatePivots(pivotCount, null, index);
    }

    @Override
    public int[] generatePivots(short pivotCount, IntArrayList elements,
            Index<O> index) throws OBException, IllegalAccessException,
            InstantiationException, OBStorageException,
            PivotsUnavailableException {
        int i = 0;
        int[] res = new int[pivotCount];
        while (i < pivotCount) {
            res[i] = super.mapId(i, elements);
            i++;
        }
        return res;
    }

}