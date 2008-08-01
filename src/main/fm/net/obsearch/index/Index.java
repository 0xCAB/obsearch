<@pp.dropOutputFile />
<#list types as t>
<#assign type = t.name>
<#assign Type = t.name?cap_first>
<@pp.changeOutputFile name="Index"+Type+".java" />

package net.obsearch.index;
import net.obsearch.Index;
import net.obsearch.ob.OB${Type};
import net.obsearch.result.OBPriorityQueue${Type};
import net.obsearch.exception.IllegalIdException;
import net.obsearch.exception.NotFrozenException;
import net.obsearch.exception.OBException;
import net.obsearch.exception.OutOfRangeException;
import java.util.Iterator;


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
 * An Index interface for distance functions that return ${Type}s.
 * @param <O>
 *            An object of type OB${Type} that will be stored in the index.
 * @author Arnoldo Jose Muller Molina
 * @since 0.7
 */
public interface Index${Type}<O extends OB${Type}> extends Index<O> {
	/**
         * Searches the Index and returns OBResult (ID, OB and distance)
         * elements that are closer to "object". The closest element is at the
         * beginning of the list and the farthest elements is at the end of the
         * list. You can control the size of the resulting set when you create
         * the object "result". This becomes the k parameter of the search.
         * @param object
         *            The object that has to be searched
         * @param r
         *            The range to be used
         * @param result
         *            A priority queue that will hold the result
         * @throws NotFrozenException
         *             if the index has not been frozen.
         * @throws OBException
         *             User generated exception
         * @throws IllegalAccessException
         *             If there is a problem when instantiating objects O
         * @throws InstantiationException
         *             If there is a problem when instantiating objects O
         * @throws IllegalIdException
         *             This exception is left as a Debug flag. If you receive
         *             this exception please report the problem to:
         *             http://code.google.com/p/obsearch/issues/list
         * @throws OutOfRangeException
         *             If the distance of any object to any other object exceeds
         *             the range defined by the user.
         */
    
    void searchOB(O object, ${type} r, OBPriorityQueue${Type}<O> result)
            throws NotFrozenException,
            InstantiationException, IllegalIdException, IllegalAccessException, OutOfRangeException, OBException;

	/**
         * Returns true if the given object intersects the given box when range
         * r is used. <b>You should normally use this method. This is intended
         * to be used internally by OBSearch </b>
         * @param object
         *            The object that has to be compared
         * @param r
         *            Range to be used
         * @param box
         *            The box to be searched
         * @return True if the object intersects the given box
         * @throws NotFrozenException
         *             if the index has not been frozen.
         * @throws OBException
         *             User generated exception
         * @throws IllegalAccessException
         *             If there is a problem when instantiating objects O
         * @throws InstantiationException
         *             If there is a problem when instantiating objects O
         * @throws IllegalIdException
         *             This exception is left as a Debug flag. If you receive
         *             this exception please report the problem to:
         *             http://code.google.com/p/obsearch/issues/list
         * @throws OutOfRangeException
         *             If the distance of any object to any other object exceeds
         *             the range defined by the user.
         */
		boolean intersects(O object, ${type} r, int box)throws NotFrozenException, InstantiationException, IllegalIdException, IllegalAccessException,
			OutOfRangeException, OBException ;

	/**
         * For an object, it returns the boxes that have to be searched based on
         * a certain range. Some index implementations will return the boxes in
         * a special order to optimize matching performance. Do not change the
         * order of the returned boxes. <b>You should normally not use this method.
         * This is intended to be used by OBSearch indexes</b> This is an optional 
         * operation, not all indexes need to support this.
         * @param object
         *            The object that will be analyzed
         * @param r
         *            The range
         * @return An array which holds all the boxes that have to be searched
         *         for the given object and range
         * @throws NotFrozenException
         *             if the index has not been frozen.
         * @throws OBException
         *             User generated exception
         * @throws IllegalAccessException
         *             If there is a problem when instantiating objects O
         * @throws InstantiationException
         *             If there is a problem when instantiating objects O
         * @throws IllegalIdException
         *             This exception is left as a Debug flag. If you receive
         *             this exception please report the problem to:
         *             http://code.google.com/p/obsearch/issues/list
         * @throws OutOfRangeException
         *             If the distance of any object to any other object exceeds
         *             the range defined by the user.
         */
		Iterator<Long> intersectingBoxes(O object, ${type} r)throws NotFrozenException, InstantiationException, IllegalIdException, IllegalAccessException,
			OutOfRangeException, OBException ;


	    /**
             * Searches the Index and returns OBResult (ID, OB and distance)
             * elements that are closer to "object". The closest element is at
             * the beginning of the list and the farthest elements is at the end
             * of the list. You can control the size of the resulting set when
             * you create the object "result". This becomes the k parameter of
             * the search. This search method only searches the object in the
             * given boxes. Implementations will search the given boxes from
             * left to right. This is because some indexes return the boxes in
             * order as an optimization <b>You should normally use this method.
             * This is intended to be used by OBSearch indexes</b>
             * @param object
             *            The object that has to be searched
             * @param r
             *            The range to be used
             * @param result
             *            A priority queue that will hold the result
             * @param boxes
             *            An array of box ids that holds the boxes that are to
             *            be searched
             * @throws NotFrozenException
             *             if the index has not been frozen.
             * @throws OBException
             *             User generated exception
             * @throws IllegalAccessException
             *             If there is a problem when instantiating objects O
             * @throws InstantiationException
             *             If there is a problem when instantiating objects O
             * @throws IllegalIdException
             *             This exception is left as a Debug flag. If you
             *             receive this exception please report the problem to:
             *             http://code.google.com/p/obsearch/issues/list
             * @throws OutOfRangeException
             *             If the distance of any object to any other object
             *             exceeds the range defined by the user.
             * @since 0.0
             */    
    void searchOB(O object, ${type} r, OBPriorityQueue${Type}<O> result, int[] boxes)
				throws NotFrozenException,
            InstantiationException, IllegalIdException, IllegalAccessException, OutOfRangeException, OBException;
		
}

</#list>