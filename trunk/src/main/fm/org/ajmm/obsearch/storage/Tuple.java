<@pp.dropOutputFile />
<#list types as t>
<#assign type = t.name>
<#assign Type = t.name?cap_first>
<@pp.changeOutputFile name="Tuple"+Type+".java" />
package org.ajmm.obsearch.storage;

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
 * A tuple returned by a range operation of an OBStore
 * implementation. This object holds the key and the value of objects
 * found within a range search. The key is a ${type}, and the value is
 * always an array of bytes.
 * @author Arnoldo Jose Muller Molina
 */

public final class Tuple${Type} {

    private ${type} key;
    
    private byte[] value;

    public Tuple${Type}(${type} key, byte[] value) {
        this.key = key;
        this.value = value;
    }

    /**
     * 
     * @return The key.
     */
    public ${type} getKey() {
        return key;
    }

    /**
     * 
     * @return The value.
     */
    public byte[] getValue() {
        return value;
    }
    
}

</#list>