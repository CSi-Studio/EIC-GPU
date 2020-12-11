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

import net.csibio.extractor.beans.ExtractorParams;
import net.csibio.extractor.utils.FileUtil;
import org.jocl.*;

import java.util.ArrayList;
import java.util.List;

import static org.jocl.CL.*;

public class GPU {

    /**
     * The OpenCL context
     */
    public static cl_context context;

    /**
     * The OpenCL command queue to which the all work will be dispatched
     */
    public static cl_command_queue commandQueue;

    /**
     * The OpenCL program containing the reduction kernel
     */
    public static cl_program program;

    /**
     * The OpenCL kernel that performs the reduction
     */
    public static cl_kernel kernel;

    /**
     * GPU内核是否初始化,如果没有初始化则无法运行GPU相关的代码
     */
    public static boolean init = false;

    /**
     * 默认每一个kernel处理5张光谱图,一共有1,5,10,20,50这5种kernel
     */
    public static int countInBatch = 5;

    /**
     * 在多张光谱图中查找多个目标mz
     *
     * @param mzArrayList 多张光谱图中的mz数组列表
     * @param intensityArrayList 多张光谱图中的intensity数组列表
     * @param targets
     * @param mzWindow
     * @return
     */
    private static float[] lowerBound(List<float[]> mzArrayList, List<float[]> intensityArrayList, float[] targets, float mzWindow) {
        if (!init){
            System.out.println("Init the GpuStart with initialize method before invoke this method");
            return null;
        }
        int countInBatch = mzArrayList.size();
        float[] results = new float[targets.length * countInBatch];

        // Allocate the memory objects for the input- and output data
        cl_mem targetsMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, Sizeof.cl_float * targets.length, Pointer.to(targets), null);

        List<ExtractorParams> paramsList = new ArrayList<>();
        for (int i = 0; i < countInBatch; i++) {
            float[] mzArray = mzArrayList.get(i);
            if (mzArray.length == 0) {
                paramsList.add(null);
            } else {
                paramsList.add(new ExtractorParams(
                        clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, Sizeof.cl_float * mzArrayList.get(i).length, Pointer.to(mzArrayList.get(i)), null),
                        clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, Sizeof.cl_float * intensityArrayList.get(i).length, Pointer.to(intensityArrayList.get(i)), null),
                        mzArrayList.get(i).length));
            }
        }

        cl_mem resultsMem = clCreateBuffer(context, CL_MEM_WRITE_ONLY, Sizeof.cl_float * targets.length * countInBatch, Pointer.to(results), null);
        lowerBound(paramsList, targetsMem, targets.length, resultsMem, mzWindow);

        // Read the output data
        clEnqueueReadBuffer(commandQueue, resultsMem, CL_TRUE, 0, targets.length * Sizeof.cl_float, Pointer.to(results), 0, null, null);

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
        clSetKernelArg(kernel, a++, Sizeof.cl_mem, Pointer.to(targetsMem));
        clSetKernelArg(kernel, a++, Sizeof.cl_int, Pointer.to(new int[]{targetsLength}));
        for (int i = 0; i < paramsList.size(); i++) {
            if (paramsList.get(i) != null) {
                clSetKernelArg(kernel, a++, Sizeof.cl_mem, Pointer.to(paramsList.get(i).getMzArrayMem()));
                clSetKernelArg(kernel, a++, Sizeof.cl_mem, Pointer.to(paramsList.get(i).getIntArrayMem()));
                clSetKernelArg(kernel, a++, Sizeof.cl_int, Pointer.to(new int[]{paramsList.get(i).getLength()}));
            } else {
                clSetKernelArg(kernel, a++, Sizeof.cl_mem, null);
                clSetKernelArg(kernel, a++, Sizeof.cl_mem, null);
                clSetKernelArg(kernel, a++, Sizeof.cl_int, Pointer.to(new int[]{0}));
            }
        }

        clSetKernelArg(kernel, a++, Sizeof.cl_float, Pointer.to(new float[]{mzWindow}));
        clSetKernelArg(kernel, a++, Sizeof.cl_mem, Pointer.to(resultsMem));

        // Execute the kernel
        clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
                new long[]{targetsLength}, null,
                0, null, null);
    }

    /**
     * Initialize a default OpenCL context, command queue, program and kernel
     */
    public static void initialize(int initCountInBatch) {
        // The platform, device type and device number
        // that will be used
        final int platformIndex = 0;
        final long deviceType = CL_DEVICE_TYPE_ALL;
        final int deviceIndex = 0;
        countInBatch = initCountInBatch;
        // Enable exceptions and subsequently omit error checks in this sample
        CL.setExceptionsEnabled(true);

        // Obtain the number of platforms
        int[] numPlatformsArray = new int[1];
        clGetPlatformIDs(0, null, numPlatformsArray);
        int numPlatforms = numPlatformsArray[0];

        // Obtain a platform ID
        cl_platform_id[] platforms = new cl_platform_id[numPlatforms];
        clGetPlatformIDs(platforms.length, platforms, null);
        cl_platform_id platform = platforms[platformIndex];

        // Initialize the context properties
        cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);

        // Obtain the number of devices for the platform
        int[] numDevicesArray = new int[1];
        clGetDeviceIDs(platform, deviceType, 0, null, numDevicesArray);
        int numDevices = numDevicesArray[0];

        // Obtain a device ID
        cl_device_id[] devices = new cl_device_id[numDevices];
        clGetDeviceIDs(platform, deviceType, numDevices, devices, null);
        cl_device_id device = devices[deviceIndex];

        // Create a context for the selected device
        context = clCreateContext(
                contextProperties, 1, new cl_device_id[]{device},
                null, null, null);

        // Create a command-queue for the selected device
        cl_queue_properties properties = new cl_queue_properties();
        commandQueue = clCreateCommandQueueWithProperties(
                context, device, properties, null);

        // Create the program from the source code
        String accCode = FileUtil.readFile("src/main/resources/clkernel/Accumulate.cl");
        String kernelCode = FileUtil.readFile("src/main/resources/clkernel/XICKernel" + countInBatch + ".cpp");
        program = clCreateProgramWithSource(context,
                1, new String[]{accCode + kernelCode}, null, null);

        // Build the program
        clBuildProgram(program, 0, null, null, null, null);

        // Create the kernel
        kernel = clCreateKernel(program, "lowerBound", null);

        init = true;
    }

    /**
     * Shut down and release all resources that have been allocated
     */
    public static void shutdown() {
        clReleaseKernel(kernel);
        clReleaseProgram(program);
        clReleaseCommandQueue(commandQueue);
        clReleaseContext(context);
        reset();
    }

    public static void reset(){
        init = false;
        countInBatch = 5;
    }
}
