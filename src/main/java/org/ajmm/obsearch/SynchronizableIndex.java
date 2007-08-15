package org.ajmm.obsearch;

import java.util.Date;
import java.util.Iterator;

import org.ajmm.obsearch.exception.IllegalIdException;
import org.ajmm.obsearch.exception.OBException;

import com.sleepycat.je.DatabaseException;

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
 A SynchronizableIndex can be used to perform syncrhonizations with other
 indexes. We use timestamps as the base for this process.
 Someone has to guarantee that the clocks are somewhat syncrhonized if 
 the indexes were generated in different computers.
 
 @author      Arnoldo Jose Muller Molina    
 @version     %I%, %G%
 @since       0.0
 */

/**
 * A boxed index is an index that divides the data into boxes. In this way boxes
 * can be distributed among different computers. For n boxes, the box id is in
 * the range [0 , n-1].
 * 
 * @param <O>
 *                The object that will be indexed
 */
public interface SynchronizableIndex<O extends OB> extends Index<O> {

    /**
         * Returns the total # of boxes this index can potentially hold
         * 
         * @return
         */
    int totalBoxes();

    /**
         * Returns a list of the currently held boxes for this index
         * 
         * @return
         */
    int[] currentBoxes() throws DatabaseException, OBException;

    /**
         * Returns the most recent insert /delete date for the given box The
         * resulting long is actually a date as returned by
         * System.currentTimeMillis() Returns -1 if no data is found for the
         * given box
         * 
         * @param box
         * @return Latest inserted time in System.currentTimeMillis() format.
         */
    long latestModification(int box) throws DatabaseException, OBException;

    /**
         * Returns the # of objects per box
         * 
         * @param box
         * @return
         * @throws DatabaseException
         * @throws OBException
         */
    int elementsPerBox(int box) throws DatabaseException, OBException;

    /**
         * Returns an iterator with all the inserted or deleted elements newer
         * than the given date
         * 
         * @param x
         *                date in the format returned by
         *                System.currentTimeMillis()
         * @param Box
         *                to search
         * @return Iterator
         */
    Iterator<TimeStampResult<O>> elementsNewerThan(int box, long x)
	    throws DatabaseException, OBException;

    /**
         * Return the internal index. A synchronized index uses a "standard"
         * index
         * 
         * @return
         */
    Index<O> getIndex();

    /**
         * Inserts the given object into the index. Forces the object to have
         * the given timestamp. This method is intended to be used internally by
         * OBSearch.
         * 
         * @param object
         *                The object to be added
         * @param id
         *                Identification number of the given object. This number
         *                must be responsibly generated by someone
         * @param time
         *                Timestamp to be used.
         * @return The internal id of the object(>= 0) or -1 if the object
         *         exists in the database
         * @throws IllegalIdException
         *                 if the given ID already exists or if isFrozen() =
         *                 false and the ID's did not come in sequential order
         * @throws DatabaseException
         *                 If something goes wrong with the DB
         * @return The internal id of the object or -1 if the object was not
         *         inserted because it already exists
         * @since 0.0
         */
    int insert(O object, long time) throws DatabaseException, OBException,
	    IllegalAccessException, InstantiationException;

    /**
         * Deletes the given object into the index. Forces the object to be
         * deleted in the given timestamp. This method is intended to be used
         * internally by OBSearch.
         * 
         * @param object
         *                Object to insert
         * @param time
         *                The time where it should be inserted
         * @return The internal id of the object(>= 0) or -1 if the object
         *         exists in the database *
         * @throws IllegalIdException
         *                 if the given ID already exists or if isFrozen() =
         *                 false and the ID's did not come in sequential order
         * @throws DatabaseException
         *                 If something goes wrong with the DB
         * @return The internal id of the object or -1 if the object was not
         *         inserted because it already exists
         * 
         */
    int delete(O object, long time) throws DatabaseException, OBException,
	    IllegalAccessException, InstantiationException;
}
