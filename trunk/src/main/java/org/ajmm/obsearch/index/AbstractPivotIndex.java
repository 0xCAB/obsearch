package org.ajmm.obsearch.index;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.ajmm.obsearch.Index;
import org.ajmm.obsearch.OB;
import org.ajmm.obsearch.exception.AlreadyFrozenException;
import org.ajmm.obsearch.exception.IllegalIdException;
import org.ajmm.obsearch.exception.NotFrozenException;
import org.ajmm.obsearch.index.pivotselection.PivotSelector;
import org.apache.log4j.Logger;

import com.sleepycat.bind.tuple.IntegerBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/*
 OBSearch: a distributed similarity search engine
 This project is to similarity search what 'bit-torrent' is to downloads.
 Copyright (C)  2007 Arnoldo Jose Muller Molina

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along
 with this program; if not, write to the Free Software Foundation, Inc.,
 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
/**
 * A Pivot index uses n pivots from the database to speed up search. The
 * following outlines the insertion workflow: 1) All insertions are copied into
 * a temporary B-Tree A. 2) User freezes the database. 3) Pivot tuples are
 * calculated for each object and they are copied into a B-tree B. 4) Subclasses
 * can calculate additional values to be used by the index. 5) All the objects
 * are finally re-inserted into the final B-Tree C 6) We will keep using B-Tree
 * A to ease object catching and to reduce the size of the pivot Tree C. Note
 * that B is deleted. Generics are used to make sure that all the inserted
 * objects will be of the same type You should not mix types as the distance
 * function and the objects must be consistent. Also, Inserts are first added to
 * A, and then to C. This guarantees that there will be always objects to match.
 * 
 * @param <O>
 *            The object type to be used
 * @param <D>
 *            The dimension type to be used
 * @author Arnoldo Jose Muller Molina
 * @version %I%, %G%
 * @since 0.0
 */
@XStreamAlias("AbstractPivotIndex")
public abstract class AbstractPivotIndex<O extends OB, D> implements
        Index<O, D> {

    private static final Logger logger = Logger
            .getLogger(AbstractPivotIndex.class);

    private File dbDir;

    private byte pivotsCount;

    private boolean frozen;

    // we should not have to control this property after freezing. This is just
    // used as a safeguard.
    // It allows RandomPivotSelector to be implemented easily
    private transient int maxId;

    private transient PivotSelector pivotSelector;

    protected transient Environment databaseEnvironment;

    protected transient Database aDB;

    protected transient Database pivotsDB;

    protected transient DatabaseConfig dbConfig;

    protected transient List<O> pivots;

    // we keep this in order to be able to create objects of type O
    protected Class<O> type;

    /**
     * Creates a new pivot index. The maximum number of pivots has been
     * arbitrarily hardcoded to 256.
     * 
     * @param databaseDirectory
     *            Where all the databases will be stored.
     * @param pivots
     *            The number of pivots to be used.
     * @param pivotSelector
     *            The object that will choose a hopefully good set of pivots
     *            from the DB.
     * @param type
     *            The type of the object the user will use. Has to match with O.
     *            Do something like YourObj.class
     * @throws DatabaseException
     */
    public AbstractPivotIndex(final File databaseDirectory, final byte pivots,
            Class<O> type) throws DatabaseException {
        this.dbDir = databaseDirectory;
        dbDir.mkdirs(); // create the directory
        this.pivotsCount = pivots;
        frozen = false;
        maxId = 0;
        this.type = type;
        this.pivots = new ArrayList<O>(pivots);
        initDB();
    }

    /**
     * Initialization of all the databases involved
     * 
     * @throws DatabaseException
     */
    private void initDB() throws DatabaseException {
        initBerkeleyDB();
        // way of creating a database
        initA();
        initPivots();
    }

    private Object readResolve() throws DatabaseException, NotFrozenException,
            DatabaseException, IllegalAccessException, InstantiationException {
        if (logger.isDebugEnabled()) {
            logger.debug("Initializing databases after being unserialized");
        }
        initDB();
        loadPivots();
        return this;
    }

    /**
     * Creates database A.
     * 
     * @throws Exception
     */
    private void initA() throws DatabaseException {
        final boolean duplicates = dbConfig.getSortedDuplicates();
        dbConfig.setSortedDuplicates(false);
        aDB = databaseEnvironment.openDatabase(null, "A", dbConfig);
        dbConfig.setSortedDuplicates(duplicates);
    }

    /**
     * Creates database Pivots.
     * 
     * @throws Exception
     */
    private void initPivots() throws DatabaseException {
        final boolean duplicates = dbConfig.getSortedDuplicates();
        dbConfig.setSortedDuplicates(false);
        pivotsDB = databaseEnvironment.openDatabase(null, "Pivots", dbConfig);
        dbConfig.setSortedDuplicates(duplicates);
    }

    /**
     * This method makes sure that all the databases are created with the same
     * settings TODO: Need to tweak these values
     * 
     * @throws DatabaseException
     *             Exception thrown by sleepycat
     */
    private void initBerkeleyDB() throws DatabaseException {
        /* Open a transactional Oracle Berkeley DB Environment. */
        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setAllowCreate(true);
        envConfig.setTransactional(false);
        // envConfig.setCacheSize(8000000);
        // envConfig.setTxnNoSync(true);
        // envConfig.setTxnWriteNoSync(true);
        // envConfig.setLocking(false);
        this.databaseEnvironment = new Environment(dbDir, envConfig);

        dbConfig = new DatabaseConfig();
        dbConfig.setTransactional(false);
        dbConfig.setAllowCreate(true);
        dbConfig.setSortedDuplicates(true);
        // dbConfig.setExclusiveCreate(true);
    }

    /**
     * Utility method to insert data before freezing takes place.
     * 
     * @param object
     *            The object to be inserted
     * @param id
     *            The id to be added
     * @throws IllegalIdException
     *             if the given ID already exists or if isFrozen() = false and
     *             the ID's did not come in sequential order.
     * @throws DatabaseException
     * @return 1 if the object was inserted
     */
    protected byte insertUnFrozen(O object, int id) throws IllegalIdException,
            DatabaseException {
        if (id != (maxId + 1)) {
            throw new IllegalIdException();
        }
        maxId = id;
        insertA(object, id);
        return 1;
    }

    public byte insert(O object, int id) throws IllegalIdException,
            DatabaseException {
        if (isFrozen()) {
            return insertUnFrozen(object, id);
        } else {
            return insertFrozen(object, id);
        }
    }

    /**
     * Inserts in database A the given Object.
     * 
     * @param object
     * @param id
     * @throws DatabaseException
     */
    protected void insertA(final O object, final int id)
            throws DatabaseException {
        insertInDatabase(object, id, aDB);
    }

    /**
     * Inserts the given object with the given Id in the database x
     * 
     * @param object
     *            object to be inserted
     * @param id
     *            id for the object
     * @param x
     *            Database to be used
     * @throws DatabaseException
     */
    protected void insertInDatabase(final O object, final int id, Database x)
            throws DatabaseException {
        final DatabaseEntry keyEntry = new DatabaseEntry();
        final DatabaseEntry dataEntry = new DatabaseEntry();
        // store the object in bytes
        final TupleOutput out = new TupleOutput();
        object.store(out);
        dataEntry.setData(out.getBufferBytes());
        // store the ID
        IntegerBinding.intToEntry(id, keyEntry);
        x.put(null, keyEntry, dataEntry);
    }

    /**
     * Utility method to insert data after freezing. Must be implemented by the
     * subclasses
     * 
     * @param object
     *            The object to be inserted
     * @param id
     *            The id to be added
     * @throws IllegalIdException
     *             if the id already exists
     * @return 1 if successful 0 otherwise
     */
    protected abstract byte insertFrozen(final O object, final int id)
            throws IllegalIdException;

    /**
     * Freezes the index. From this point data can be inserted, searched and
     * deleted The index might deteriorate at some point so every once in a
     * while it is a good idea to rebuild de index.
     * After the method returns, searching is enabled.
     * @param pivotSelector
     *            The pivot selector to be used
     * @throws IOException
     *             if the serialization process fails
     * @throws AlreadyFrozenException
     *             If the index was already frozen and the user attempted to
     *             freeze it again
     */
    public void freeze(PivotSelector pivotSelector) throws IOException,
            AlreadyFrozenException, IllegalIdException, IllegalAccessException,
            InstantiationException, DatabaseException {
        if (isFrozen()) {
            throw new AlreadyFrozenException();
        }

        storePivots();
        
        calculateIndexParameters();    
        
        XStream xstream = new XStream();
        //TODO: make sure this "this" will print the subclass and not the current class
        String xml = xstream.toXML(this);
        FileWriter fout = new FileWriter(this.dbDir.getPath()
                + getSerializedName());
        fout.write(xml);
        fout.close();
    }
    
    /**
     * Children of this class have to implement this method if
     * they want to calculate extra parameters
     */
    protected abstract void calculateIndexParameters();
    
    
    /** 
     * This method calculates the pivots for each element in the database
     * and stores them in database B.
     * Later subclasses of this class can analyze the pivot tables and
     * create additional parameters
     */
    protected void storeTuples() throws NotFrozenException, DatabaseException,
    IllegalAccessException, InstantiationException{
        Cursor cursor = null;
        DatabaseEntry foundKey = new DatabaseEntry();
        DatabaseEntry foundData = new DatabaseEntry();

        if (!isFrozen()) {
            throw new NotFrozenException();
        }
        try {
            int i = 0;

            cursor = aDB.openCursor(null, null);
            O obj = this.instantiateObject();
            D[] tuple = this.instantiateDims(); // create a dimension array
            while (cursor.getNext(foundKey, foundData, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
                assert i == IntegerBinding.entryToInt(foundKey);
                TupleInput in = new TupleInput(foundData.getData());
                
                obj.load(in);
                calculatePivotTuple(obj, tuple);
                insertPivotTupleBDB(i, tuple); // store the tuple
                this.pivots.add(i, obj);
                i++;
            }
            assert this.maxId == i; // pivot count and read # of pivots
            // should be the same
        } finally {
            cursor.close();
        }
    }
    
    /**
     * Calculates the tuple vector for the given object
     * @param obj
     * @param tuple The resulting tuple will be stored here
     */
    protected void calculatePivotTuple(O obj, D[] tuple){
        assert tuple.length == this.pivotsCount;
    }

    /**
     * Stores the pivots selected by the pivot selector in database Pivot. As a
     * side effect leaves the pivots cached in this.pivots
     * 
     * @throws IllegalIdException
     *             If the pivot selector generates invalid ids
     */
    protected void storePivots() throws IllegalIdException,
            IllegalAccessException, InstantiationException, DatabaseException {
        int[] ids = pivotSelector.generatePivots(pivotsCount, maxId);
        assert ids.length == pivots.size() && pivots.size() == this.pivotsCount;

        int i = 0;
        while (i < ids.length) {
            O obj = getObject(ids[i], aDB);
            insertInDatabase(obj, i, pivotsDB); // store in the B-tree
            pivots.set(i, obj); // store in the array for inmediate use
            i++;
        }
    }

    /**
     * Loads the pivots from the database
     * 
     * @throws NotFrozenException
     *             if the freeze method has not been invoqued.
     */
    protected void loadPivots() throws NotFrozenException, DatabaseException,
            IllegalAccessException, InstantiationException {
        Cursor cursor = null;
        DatabaseEntry foundKey = new DatabaseEntry();
        DatabaseEntry foundData = new DatabaseEntry();

        if (!isFrozen()) {
            throw new NotFrozenException();
        }
        try {
            int i = 0;

            cursor = pivotsDB.openCursor(null, null);
            O obj = this.instantiateObject();
            while (cursor.getNext(foundKey, foundData, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
                assert i == IntegerBinding.entryToInt(foundKey);
                TupleInput in = new TupleInput(foundData.getData());                
                obj.load(in);
                this.pivots.add(i, obj);
                i++;
            }
            assert i == pivotsCount; // pivot count and read # of pivots
            // should be the same
        } finally {
            cursor.close();
        }
    }

    /**
     * Generates the name of the file to be used to store the serialized version
     * of this Index.
     * 
     * @return
     */
    public abstract String getSerializedName();

    /**
     * Gets the object with the given id from the database.
     * 
     * @param id
     *            The id to be extracted
     * @param DB
     *            the database to be accessed the object will be returned here
     * @return the object the user asked for
     * @throws DatabaseException
     * @throws IllegalIdException
     *             if the given id does not exist in the database
     */
    public O getObject(int id, Database DB) throws DatabaseException,
            IllegalIdException, IllegalAccessException, InstantiationException {
        // TODO: put these two objects in the class so that they don't have to
        // be created over and over again
        // TODO: add an OB cache
        DatabaseEntry keyEntry = new DatabaseEntry();
        DatabaseEntry dataEntry = new DatabaseEntry();
        IntegerBinding.intToEntry(id, keyEntry);

        O object = instantiateObject();

        if (DB.get(null, keyEntry, dataEntry, null) == OperationStatus.SUCCESS) {
            // TODO: Extend TupleInput so that we don't have to create the
            // object over and over again
            TupleInput in = new TupleInput(dataEntry.getData());
            object.load(in); // load the bytes into the object
        } else {
            throw new IllegalIdException();
        }
        return object;
    }

    public O instantiateObject() throws IllegalAccessException,
            InstantiationException {
        // Find out if java can give us the type information directly from the
        // template parameter. There should be a way...
        return type.newInstance();
    }

    public List<O> getPivots() {
        return this.pivots;
    }

    public byte getPivotsCount() {
        return this.pivotsCount;
    }

}
