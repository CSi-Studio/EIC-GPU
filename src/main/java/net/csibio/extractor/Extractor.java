/*
 * JOCL - Java bindings for OpenCL
 *
 * Copyright 2009-2019 Marco Hutter - http://www.jocl.org/
 */
package net.csibio.extractor;

import net.csibio.extractor.beans.ExtractorParams;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_mem;

import java.util.ArrayList;
import java.util.List;

import static org.jocl.CL.*;

/**
 * A sample showing a simple reduction with JOCL
 */
public class Extractor {


    /**
     * 在多张光谱图中查找多个目标mz
     *
     * @param mzArrayList 多张光谱图中的mz数组列表
     * @param intensityArrayList 多张光谱图中的intensity数组列表
     * @param targets
     * @param mzWindow
     * @return
     */
    public static float[] lowerBound(List<float[]> mzArrayList, List<float[]> intensityArrayList, float[] targets, float mzWindow) {
        if (!GpuStarter.init){
            System.out.println("Init the GpuStart with initialize method before invoke this method");
            return null;
        }
        int countInBatch = mzArrayList.size();
        float[] results = new float[targets.length * countInBatch];

        // Allocate the memory objects for the input- and output data
        cl_mem targetsMem = clCreateBuffer(GpuStarter.context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, Sizeof.cl_float * targets.length, Pointer.to(targets), null);

        List<ExtractorParams> paramsList = new ArrayList<>();
        for (int i = 0; i < countInBatch; i++) {
            float[] mzArray = mzArrayList.get(i);
            if (mzArray.length == 0) {
                paramsList.add(null);
            } else {
                paramsList.add(new ExtractorParams(
                        clCreateBuffer(GpuStarter.context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, Sizeof.cl_float * mzArrayList.get(i).length, Pointer.to(mzArrayList.get(i)), null),
                        clCreateBuffer(GpuStarter.context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, Sizeof.cl_float * intensityArrayList.get(i).length, Pointer.to(intensityArrayList.get(i)), null),
                        mzArrayList.get(i).length));
            }
        }

        cl_mem resultsMem = clCreateBuffer(GpuStarter.context, CL_MEM_WRITE_ONLY, Sizeof.cl_float * targets.length * countInBatch, Pointer.to(results), null);
        lowerBound(paramsList, targetsMem, targets.length, resultsMem, mzWindow);

        // Read the output data
        clEnqueueReadBuffer(GpuStarter.commandQueue, resultsMem, CL_TRUE, 0, targets.length * Sizeof.cl_float, Pointer.to(results), 0, null, null);

        // Release memory objects
        paramsList.forEach(params -> {
            if (params != null) {
                clReleaseMemObject(params.getMzArrayMem());
                clReleaseMemObject(params.getIntArrayMem());
            }
        });
        clReleaseMemObject(targetsMem);
        clReleaseMemObject(resultsMem);
        return results;
    }

    /**
     * LowerBound with GPU
     * @param paramsList
     * @param targetsMem
     * @param targetsLength
     * @param resultsMem
     * @param mzWindow
     */
    private static void lowerBound(List<ExtractorParams> paramsList,
                                   cl_mem targetsMem, int targetsLength,
                                   cl_mem resultsMem, float mzWindow) {
        // Set the arguments for the kernel
        int a = 0;
        clSetKernelArg(GpuStarter.kernel, a++, Sizeof.cl_mem, Pointer.to(targetsMem));
        clSetKernelArg(GpuStarter.kernel, a++, Sizeof.cl_int, Pointer.to(new int[]{targetsLength}));
        for (int i = 0; i < paramsList.size(); i++) {
            if (paramsList.get(i) != null) {
                clSetKernelArg(GpuStarter.kernel, a++, Sizeof.cl_mem, Pointer.to(paramsList.get(i).getMzArrayMem()));
                clSetKernelArg(GpuStarter.kernel, a++, Sizeof.cl_mem, Pointer.to(paramsList.get(i).getIntArrayMem()));
                clSetKernelArg(GpuStarter.kernel, a++, Sizeof.cl_int, Pointer.to(new int[]{paramsList.get(i).getLength()}));
            } else {
                clSetKernelArg(GpuStarter.kernel, a++, Sizeof.cl_mem, null);
                clSetKernelArg(GpuStarter.kernel, a++, Sizeof.cl_mem, null);
                clSetKernelArg(GpuStarter.kernel, a++, Sizeof.cl_int, Pointer.to(new int[]{0}));
            }
        }

        clSetKernelArg(GpuStarter.kernel, a++, Sizeof.cl_float, Pointer.to(new float[]{mzWindow}));
        clSetKernelArg(GpuStarter.kernel, a++, Sizeof.cl_mem, Pointer.to(resultsMem));

        // Execute the kernel
        clEnqueueNDRangeKernel(GpuStarter.commandQueue, GpuStarter.kernel, 1, null,
                new long[]{targetsLength}, null,
                0, null, null);
    }

    /**
     * LowerBound with CPU for multiple targets
     * @param array
     * @param targets
     * @return
     */
    private static int[] lowerBound(float[] array, float[] targets) {
        int[] results = new int[targets.length];
        for (int i = 0; i < targets.length; i++) {
            results[i] = lowerBound(array, targets[i]);
        }
        return results;
    }

    /**
     * LowerBound with CPU for single target mz
     * @param array
     * @param target
     * @return
     */
    public static int lowerBound(float[] array, Float target) {
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
