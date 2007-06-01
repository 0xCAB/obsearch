package org.ajmm.obsearch;

import org.ajmm.obsearch.exception.OutOfRangeException;
import org.apache.log4j.Logger;

import com.sleepycat.bind.tuple.TupleInput;

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
 * An interface used to store dimension units. Internally, the user defines the
 * minimum and maximum value that the dimension will hold. Any constructor is
 * free to throw OutOfRangeException if the given input value is out of range.
 * Before freezing this exception is fatal. After freezing records are simply
 * ignored.
 * Classes implementing this interface must provide the default constructor
 * 
 * @author Arnoldo Jose Muller Molina
 * @version %I%, %G%
 * @since 1.0
 */

public interface Dim extends Storable {

    
    /**
     * This method should read one dimension a from in and do: t = Math.abs(this -
     * a) if (t > r) return false; if (t > max) { max = t; return true;
     * 
     * @param in
     *            the input where the dimension will be parsed
     * @param r
     *            the range to be used
     * @param max
     *            the current maximum value (not to be confused with the maximum
     *            acceptable value)
     * @return true if we should continue calculating linf, false otherwise
     */
    boolean lInfiniteOneStep(TupleInput in, Dim r, Dim max);

    /**
     * Normalizes the current value of the dimension
     * 
     * @see http://people.revoledu.com/kardi/tutorial/Similarity/Normalization.html
     *      For a list of normalization strategies A typical formula to
     *      normalize d is (d - min) / (max - min) where min is the minimum
     *      acceptable value and max is the maximum acceptable value
     * @param min
     *            Minimum value to be outputted by the distance function
     * @param max
     *            Maximum value to be outputted by the distance function
     * @throws OutOfRangeException
     *             if the current dimension is outside the interval [min, max]
     * @return A number between 1 and 0
     */
    // TODO try to remove this method. But for now it is ok as we only plan to
    // do pyramids. At some point it needs to be refactored.
    float normalize(Dim min, Dim max) throws OutOfRangeException;

    /**
     * Returns true if this Dimension's value is less than X
     * 
     * @param x
     * @return
     */
    boolean lt(Dim x);

    /**
     * Returns true if this Dimension's value is greater than X
     * 
     * @param x
     * @return
     */
    boolean gt(Dim x);

    /**
     * Returns true if this Dimension's value is greater or equal than x
     * 
     * @param x
     * @return
     */
    boolean ge(Dim x);

    /**
     * Returns true if this Dimension's value is lower or equal than x
     * 
     * @param x
     * @return
     */
    boolean le(Dim x);

    /**
     * Sets this dimension to the smallest possible value
     */
    void setToAbsoluteSmall();
    
    /**
     * Adds the value of x into this. Will not go above maxInput
     * @param x
     * @return a new object with the new value
     */
    Dim add(Dim x, Dim maxInput);
    
    /**
     * Substracts the value of x from this.  will not go below minInput
     * @param x
     * @return a new object the the new value
     */
    Dim sub(Dim x, Dim minInput);


    /**
     * Assigns the value of x to this object
     * 
     * @param x
     *            the value from where to update
     */
    void update(Dim x);
}
