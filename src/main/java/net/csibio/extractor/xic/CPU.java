/*
 * Copyright (c) 2020 CSi Biotech
 * Aird and AirdPro are licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package net.csibio.extractor.xic;

public class CPU {

    /**
     * 计算 mz在[mzStart, mzEnd]范围对应的intensity的总和
     * Get the Low Bound Index for mzStart and High Bound Index for mzEnd in pairs.mzArray,
     * then accumulate the intensity from the LowBoundIndex to HighBoundIndex
     *
     * @param mzArray        mzArray is an ordered array
     * @param intensityArray intensity array related to mzArray
     * @param mzStart        target mz start
     * @param mzEnd          target mz end
     * @return the intensity sum as extractor result
     */
    public static float accumulation(float[] mzArray, float[] intensityArray, Float mzStart, Float mzEnd) {

        float result = 0f;
        try {
            //Index of first mz bigger than mzStart
            int index = lowerBound(mzArray, mzStart);
            //No element is bigger than mzStart in mzArray
            if (index == -1) {
                return 0f;
            }
            int iterIndex = index;

            //Accumulate when iterIndex in (mzStart, mzEnd). Return 0 if rightIndex's mz is bigger than mzEnd.
            while (mzArray[iterIndex] <= mzEnd) {
                result += intensityArray[iterIndex];
                iterIndex++;
            }
        } catch (Exception e) {
            return result;
        }
        return result;
    }

    /**
     * LowerBound with CPU for multiple targets
     *
     * @param array an ordered array
     * @param targets targets to search
     * @return the low bound indexes of the targets in the ordered array
     */
    public static int[] lowerBound(float[] array, float[] targets) {
        int[] results = new int[targets.length];
        for (int i = 0; i < targets.length; i++) {
            results[i] = lowerBound(array, targets[i]);
        }
        return results;
    }

    /**
     * LowerBound with CPU for single target mz
     *
     * @param array an ordered array
     * @param target target to search
     * @return the low bound index of the target in the ordered array
     */
    public static int lowerBound(float[] array, float target) {
        int rightIndex = array.length - 1;

        if (target <= array[0]) {
            return 0;
        }
        if (target >= array[rightIndex]) {
            return -1;
        }

        int leftIndex = 0;
        while (leftIndex + 1 < rightIndex) {
            int tmp = (leftIndex + rightIndex) >>> 1;
            if (target < array[tmp]) {
                rightIndex = tmp;
            } else if (target > array[tmp]) {
                leftIndex = tmp;
            } else {
                return tmp;
            }
        }

        return rightIndex;
    }
}
