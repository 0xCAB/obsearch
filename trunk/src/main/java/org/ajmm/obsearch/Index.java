package org.ajmm.obsearch;

import java.io.File;
import java.io.IOException;

import org.ajmm.obsearch.exception.AlreadyFrozenException;
import org.ajmm.obsearch.exception.IllegalIdException;
import org.ajmm.obsearch.exception.IllegalKException;
import org.ajmm.obsearch.exception.NotFrozenException;
import org.ajmm.obsearch.exception.OBException;
import org.ajmm.obsearch.exception.OutOfRangeException;
import org.ajmm.obsearch.exception.UndefinedPivotsException;

import com.sleepycat.bind.tuple.TupleInput;
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
 * This the main contract for OBSearch indexes. The normal lifecycle of an index
 * is: 1) Insert many records 2) Perform a freeze A freeze is an operation that
 * calculates different information on the data. The idea is to "learn" how the
 * sample data is distributed to improve performance. You cannot perform
 * searches on an unfrozen index. Note that freezing the index does not mean you
 * cannot insert new data. You can! 3) Insert/Delete new records/ and search.
 * There are indexes that wrap other indexes to provide extended functionality.
 * Please consult the documentation for each index.
 * @param <O>
 *            The object that will be indexed in the database
 * @author Arnoldo Jose Muller Molina
 * @version %I%, %G%
 * @since 1.0
 */
public interface Index < O extends OB > {

    /**
     * Inserts the given object into the index. If the index is not frozen, you
     * are expected to insert objects that are not duplicated OBSearch cannnot
     * efficiently perform those checks for you at this stage (before freezing),
     * so please be careful.
     * @param object
     *            The object to be added Identification number of the given
     *            object. This number must be responsibly generated by someone
     * @return The internal id of the object(>= 0) or -1 if the object exists in
     *         the database
     * @throws DatabaseException
     *             If something goes wrong with the DB
     * @throws OBException
     *             User generated exception
     * @throws IllegalAccessException
     *             If there is a problem when instantiating objects O
     * @throws InstantiationException
     *             If there is a problem when instantiating objects O
     * @since 0.0
     */
    int insert(O object) throws DatabaseException, OBException,
            IllegalAccessException, InstantiationException;

    /**
     * Returns true if the index is frozen.
     * @return true if the index is frozen, false otherwise
     */
    boolean isFrozen();

    /**
     * Returns true if the given object exists in the database. This method was
     * created for OBSearch internal use. If you are realying on this method a
     * lot, isn't it better to use a Hashtable or a B-tree instead of OBSearch?
     * O.equals(...) is used to calculate the equality of two objects.
     * @param object
     *            The object that will be searched.
     * @return true if object exists in the database.
     * @throws DatabaseException
     *             If something goes wrong with the DB
     * @throws OBException
     *             User generated exception
     * @throws IllegalAccessException
     *             If there is a problem when instantiating objects O
     * @throws InstantiationException
     *             If there is a problem when instantiating objects O
     */
    boolean exists(O object) throws DatabaseException, OBException,
            IllegalAccessException, InstantiationException;

    /**
     * Freezes the index. From this point data can be inserted, searched and
     * deleted The index might deteriorate at some point so every once in a
     * while it is a good idea to rebuild de index. A PivotSelector must be
     * executed before calling this method.
     * @throws IOException
     *             if the index serialization process fails
     * @throws AlreadyFrozenException
     *             If the index was already frozen and the user attempted to
     *             freeze it again
     * @throws DatabaseException
     *             If something goes wrong with the DB
     * @throws OBException
     *             User generated exception
     * @throws IllegalAccessException
     *             If there is a problem when instantiating objects O
     * @throws InstantiationException
     *             If there is a problem when instantiating objects O
     * @throws UndefinedPivotsException
     *             If the pivots of the index have not been selected before
     *             calling this method.
     * @throws OutOfRangeException
     *             If the distance of any object to any other object exceeds the
     *             range defined by the user.
     * @throws IllegalIdException
     *             This exception is left as a Debug flag. If you receive this
     *             exception please report the problem to:
     *             http://code.google.com/p/obsearch/issues/list
     */
    void freeze() throws IOException, AlreadyFrozenException,
            IllegalIdException, IllegalAccessException, InstantiationException,
            DatabaseException, OutOfRangeException, OBException,
            UndefinedPivotsException;

    /**
     * Deletes the given object form the database.
     * @param object
     *            The object to be deleted
     * @return >= (the object ID) if the object was deleted or -1 if the object
     *         was not deleted
     * @throws NotFrozenException
     *             if the index has not been frozen.
     * @throws DatabaseException
     *             If something goes wrong with the DB
     * @throws OBException
     *             User generated exception
     * @throws IllegalAccessException
     *             If there is a problem when instantiating objects O
     * @throws InstantiationException
     *             If there is a problem when instantiating objects O
     * @since 0.0
     */
    int delete(O object) throws DatabaseException, OBException,
            IllegalAccessException, InstantiationException, NotFrozenException;

    /**
     * This method returns the object with internal id "i". Mainly used for
     * internal validation purposes, users should not have to use this method
     * directly
     * @param i
     *            The id to be retrieved
     * @return The object associated to the given id. if the index has not been
     *         frozen. was deleted successfully
     * @throws DatabaseException
     *             If something goes wrong with the DB
     * @throws OBException
     *             User generated exception
     * @throws IllegalAccessException
     *             If there is a problem when instantiating objects O
     * @throws InstantiationException
     *             If there is a problem when instantiating objects O
     */
    O getObject(int i) throws DatabaseException, IllegalIdException,
            IllegalAccessException, InstantiationException, OBException;

    /**
     * Closes the database.
     * @throws DatabaseException
     *             If something goes wrong with the DB
     */
    void close() throws DatabaseException;

    /**
     * Returns the total number of boxes this index can hold.
     * @return The total number of boxes the index can eventually support.
     */
    int totalBoxes();

    /**
     * Returns the box where the given object is stored.
     * @param object
     *            The object to be analyzed
     * @return The box that corresponds to object
     * @throws OBException
     *             User generated exception
     */
    int getBox(O object) throws OBException;

    /**
     * Returns the database size.
     * @return Number of elements in the database
     * @throws DatabaseException
     *             If something goes wrong with the DBa
     */
    int databaseSize() throws DatabaseException;

    /**
     * This method *must* be called after de-serializing the database object.
     * Index implementations should store internally the location of the
     * database, but this method allows you to override this. If the given value
     * is null, the internally stored path will be used.
     * @param dbPath
     *            New database path, or null if the default is to be used
     * @throws DatabaseException
     *             If something goes wrong with the DB
     * @throws OBException
     *             User generated exception
     * @throws IllegalAccessException
     *             If there is a problem when instantiating objects O
     * @throws InstantiationException
     *             If there is a problem when instantiating objects O
     * @throws NotFrozenException
     *             if the index has not been frozen.
     * @throws DatabaseException
     *             If something goes wrong with the DB.
     * @throws IOException
     *             if the index serialization process fails
     */
    void relocateInitialize(File dbPath) throws DatabaseException,
            NotFrozenException, IllegalAccessException, InstantiationException,
            OBException, IOException;

    /**
     * Generates an XML representation of this index suitable for serialization.
     * The data itself is not serialized, only the metadata necessary to allow
     * this index to be frozen.
     * @return An String containing an XML file that represents the serializable
     *         fields of this object.
     */
    String toXML();

    /**
     * Instantiates an object from a byte stream. This method should only be
     * used by OBSearch. You normally should not use this method.
     * @param in
     *            Byte Stream from which the object will be loaded
     * @return An object created from the given stream
     * @throws IllegalAccessException
     *             If there is a problem when instantiating objects O
     * @throws InstantiationException
     *             If there is a problem when instantiating objects O
     * @throws OBException
     *             User generated exception
     */
    O readObject(TupleInput in) throws InstantiationException,
            IllegalAccessException, OBException;

}
