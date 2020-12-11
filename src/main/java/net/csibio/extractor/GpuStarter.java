/*
 * Copyright (c) 2020 CSi Biotech
 * Aird and AirdPro are licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package net.csibio.extractor;

import net.csibio.extractor.utils.FileUtil;
import org.jocl.*;

import static org.jocl.CL.*;

public class GpuStarter {

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
        int numPlatformsArray[] = new int[1];
        clGetPlatformIDs(0, null, numPlatformsArray);
        int numPlatforms = numPlatformsArray[0];

        // Obtain a platform ID
        cl_platform_id platforms[] = new cl_platform_id[numPlatforms];
        clGetPlatformIDs(platforms.length, platforms, null);
        cl_platform_id platform = platforms[platformIndex];

        // Initialize the context properties
        cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);

        // Obtain the number of devices for the platform
        int numDevicesArray[] = new int[1];
        clGetDeviceIDs(platform, deviceType, 0, null, numDevicesArray);
        int numDevices = numDevicesArray[0];

        // Obtain a device ID
        cl_device_id devices[] = new cl_device_id[numDevices];
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
