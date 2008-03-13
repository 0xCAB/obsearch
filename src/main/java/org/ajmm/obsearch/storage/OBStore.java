package org.ajmm.obsearch.storage;

import java.util.Iterator;

import org.ajmm.obsearch.Result;
import org.ajmm.obsearch.exception.OBStorageException;

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
 * OBStore abstracts a generic storage system. The purpose of this class is to
 * allow OBSearch to run on top of different storage systems (distributed,
 * local, file system based, etc). The keys can be sorted, and range queries are
 * possible. The base interface only allows operations on keys of arrays of
 * bytes. Subclasses of this interface will provide specialized methods for
 * Java's primitive types.
 * @author Arnoldo Jose Muller Molina
 */

public interface OBStore {

    /**
     * Get the name of this storage system.
     * @return the name of this storage system.
     */
    String getName();

    /**
     * Returns the associated value for the given key. If the underlying storage
     * system can hold multiple keys, then an IllegalArgumentException is
     * thrown.
     * @param key
     *                The key that will be searched.
     * @return the associated value for the given key or null if the key could
     *         not be found.
     * @throws IllegalArgumentException
     *                 If the underlying storage system can hold multiple keys (
     *                 {@link #allowsDuplicatedData()} == true).
     * @throws OBStorageException
     *                 If an exception occurs at the underlying storage system.
     *                 You can query the exception to see more details regarding
     *                 the nature of the error.
     */
    byte[] getValue(byte[] key) throws IllegalArgumentException,
            OBStorageException;

    /**
     * Inserts the key value pair. If the key existed, it will be overwritten.
     * @param key
     *                Key to insert
     * @param value
     *                The value that the key will hold after this operation
     *                completes.
     * @throws OBStorageException
     *                 If an exception occurs at the underlying storage system.
     *                 You can query the exception to see more details regarding
     *                 the nature of the error.
     * @return {@link org.ajmm.obsearch.Result.Status#OK} the record was inserted/updated successfully.
     *               {@link org.ajmm.obsearch.Result.Status#ERROR} if the record could not be updated.
     */
    Result put(byte[] key, byte[] value) throws OBStorageException;

   

    /**
     * Deletes the given key and its corresponding value from the database.
     * @param key
     *                The key that will be deleted.
     * @throws OBStorageException
     *                 If an exception occurs at the underlying storage system.
     *                 You can query the exception to see more details regarding
     *                 the nature of the error.
     * @return {@link org.ajmm.obsearch.Result.Status#OK} if the key was found,
     *         otherwise, {@link org.ajmm.obsearch.Result.Status#NOT_EXISTS}.
     */
    Result delete(byte[] key) throws OBStorageException;

    /**
     * Closes the storage system.
     * @throws OBStorageException
     *                 If an exception occurs at the underlying storage system.
     *                 You can query the exception to see more details regarding
     *                 the nature of the error.
     */
    void close() throws OBStorageException;

    /**
     * Deletes all the items in the storage system. Use with care!
     * @throws OBStorageException
     *                 If an exception occurs at the underlying storage system.
     *                 You can query the exception to see more details regarding
     *                 the nature of the error.
     */
    void deleteAll() throws OBStorageException;

    /**
     * Returns the number of elements in the database. 
     * @return The number of elements in the database. 
     * @throws OBStorageException
     *                 If an exception occurs at the underlying storage system.
     *                 You can query the exception to see more details regarding
     *                 the nature of the error.
     */
    long size() throws OBStorageException;
}
