package net.obsearch.index.bucket;

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
 * BucketObject holds a bucket number for an object. Subclasses hold also the SMAP
 * vector with the distances of the object to the pivots of a certain level.
 * @author Arnoldo Jose Muller Molina
 */

public abstract class BucketObject {
    

        
    /**
     * Id of the object.
     */
    private long id;

   

    /**
     * Creates a new bucket with the given bucket number and
     * the specified level.
     * @param bucket Bucket number.
     * @param exclusionBucket If true, the corresponding object is in the exclusion zone.
     * @param optional id of the object.
     */
    public BucketObject(long id) {
        super();
        this.id = id;
    }
    
    /**
     * @return the id
     */
    public long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(long id) {
        this.id = id;
    }

   

   
    
    /**
     * Returns the # of pivots.
     * @return
     */
    public abstract int getPivotSize();

}