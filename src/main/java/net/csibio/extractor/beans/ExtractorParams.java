/*
 * Copyright (c) 2020 CSi Biotech
 * Aird and AirdPro are licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package net.csibio.extractor.beans;

import org.jocl.cl_mem;

public class ExtractorParams {

    private cl_mem mzArrayMem;
    private cl_mem intArrayMem;
    private int length;

    public ExtractorParams() {
    }

    public ExtractorParams(cl_mem mzArrayMem, cl_mem intArrayMem, int length) {
        this.setMzArrayMem(mzArrayMem);
        this.setIntArrayMem(intArrayMem);
        this.setLength(length);
    }


    public cl_mem getIntArrayMem() {
        return intArrayMem;
    }

    public void setIntArrayMem(cl_mem intArrayMem) {
        this.intArrayMem = intArrayMem;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public cl_mem getMzArrayMem() {
        return mzArrayMem;
    }

    public void setMzArrayMem(cl_mem mzArrayMem) {
        this.mzArrayMem = mzArrayMem;
    }
}
