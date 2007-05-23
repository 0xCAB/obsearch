package org.ajmm.obsearch;
import org.ajmm.obsearch.dimension.Dim;
import org.ajmm.obsearch.exceptions.IllegalIdException;
import org.ajmm.obsearch.exceptions.IllegalKException;

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
 * Class: Index
 *
 * An Index stores objects based on a distance function in a hopefully
 * efficient way
 * @author      Arnoldo Jose Muller Molina
 * @version     %I%, %G%
 * @since       0.0
 */
public interface Index {
    /**
     * Searches the Index and returns Result elements (ID and distance only)
     * that are closer to "object".
     * The closest element is at the beginning of the list and the
     * farthest elements is at the end of the list.
     * This condition must hold result.length == k
     *
     * @param object The object that has to be searched
     * @param k The maximum number of objects to be returned
     * @param r the range to be used
     * @param result An array of "Result". A null object terminates the list. Note that if the list contains objects they will be reused saving precious time. This list will contain at most k objects.
     * @throws IllegalKException if k != result.length
     * @since 0.0
     */
    public void searchID(OB object, byte k, Dim r, Result[] result) throws IllegalKException;
    
    /**
     * Searches the Index and returns OBResult (ID, OB and distance) elements
     * that are closer to "object".
     * The closest element is at the beginning of the list and
     * the farthest elements is at the end of the list
     * This condition must hold result.length == k
     *
     * @param object The object that has to be searched
     * @param k The maximum number of objects to be returned
     * @param r the range to be used
     * @param result An array of "OBResult". A null object terminates the list. Note that if the list contains objects they will be reused saving precious time. This list will contain at most k objects.
     * @throws IllegalKException if k != result.length
     * @since 0.0
     */
    public void searchOB(OB object, byte k, Dim r, OBResult[] result) throws IllegalKException;
    
    
    /**
     * Inserts the given object into the index with the given ID
     * If the given ID already exists, the exception IllegalIDException
     * is thrown.
     * @param object The object to be added
     * @param id Identification number of the given object.
     * This number must be responsibly generated by someone
     * @return 0 if the object already existed or 1 if the object was inserted
     * @throws IllegalIdException if the given ID already exists
     * @since 0.0
     */
    //TODO: make sure that the community is ok with 
    //             storing 2,147,483,647 objects
    byte insert(OB object, int id) throws IllegalIdException;
    
    
    /**
     * Deletes the given object form the database
     * @param object The object to be deleted
     * @return 0 if the object was not found in the database or 1 if it
     * was deleted successfully
     * @since 0.0
     */
    public byte delete(OB object);
    
}
