package Compute;

import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL;
import org.lwjgl.opencl.CLCapabilities;
import org.lwjgl.opencl.CLContextCallback;
import org.lwjgl.system.MemoryStack;

import java.io.InputStream;
import java.nio.IntBuffer;
import java.util.HashMap;

import static Compute.InfoUtil.checkCLError;
import static org.lwjgl.opencl.CL10.*;
import static org.lwjgl.opencl.CL11.CL_DEVICE_OPENCL_C_VERSION;
import static org.lwjgl.system.MemoryUtil.*;

public class GPU {

    public static MemoryStack Stack;
    public static IntBuffer ErrorcodeReturn;

    public static long Platform;
    public static long Device;

    private static CLCapabilities PlatformCapabilities;
    private static CLCapabilities DeviceCapabilities;

    protected static long Context;
    private static CLContextCallback ContextCallback;

    public static long CommandQueue;

    public static HashMap<String,ComputeProgram> Programs = new HashMap<String,ComputeProgram>();

    public static void AddProgram(String Name, String Path) {
        ComputeProgram Program = new ComputeProgram(
                Context,
                Path
        );
        Programs.put(Name, Program);
    }

    public static void AddProgram(String Name, InputStream IS) {
        ComputeProgram Program = new ComputeProgram(
                Context,
                IS,
                Name
        );
        Programs.put(Name, Program);
    }


    private static void GetPlatformAndDevice(MemoryStack Stack) {
        IntBuffer PlatformCount = Stack.mallocInt(1);
        PointerBuffer AvailablePlatforms = Stack.mallocPointer(1);

        checkCLError(
                clGetPlatformIDs(AvailablePlatforms, PlatformCount) //2nd Arg nmly (IntBuffer)null
        );
        if (PlatformCount.get(0) == 0) {
            throw new RuntimeException("No OpenCL platforms found.");
        }

        Platform = AvailablePlatforms.get(0);
        PlatformCapabilities = CL.createPlatformCapabilities(Platform);

        IntBuffer DeviceCount = Stack.mallocInt(1);
        PointerBuffer AvailableDevices = Stack.mallocPointer(1);
        checkCLError(
                clGetDeviceIDs(Platform, CL_DEVICE_TYPE_ALL, AvailableDevices, DeviceCount)
        );
        if (DeviceCount.get(0) == 0) {
            throw new RuntimeException("No OpenCL devices found.");
        }

        Device = AvailableDevices.get(0);
        DeviceCapabilities = CL.createDeviceCapabilities(Device, PlatformCapabilities);
    }

    private static PointerBuffer GetContextProperties(MemoryStack Stack) {
        PointerBuffer ContextProperties = Stack.mallocPointer(3);

        ContextProperties
                .put(0, CL_CONTEXT_PLATFORM)
                .put(1, Platform)
                .put(2, 0)
        ;

        return ContextProperties;
    }

    private static void GetContextCallback() {
        ContextCallback = CLContextCallback.create((errinfo, private_info, cb, user_data) -> {
            System.err.println("[LWJGL] cl_context_callback");
            System.err.println("\tInfo: " + memUTF8(errinfo));
        });
    }

    public static void PrintInfo() {
        StringBuilder SB = new StringBuilder();

        SB.append("Compute Device successfully initialized!" + '\n' + '\n');

        SB.append("Compute Device Information:" + '\n');

        SB.append("Name: " + InfoUtil.getDeviceInfoStringUTF8(Device, CL_DEVICE_NAME) + '\n');

        SB.append("Type: ");
        long Type = InfoUtil.getDeviceInfoLong(Device, CL_DEVICE_TYPE);
        switch ((int)Type) {
            case CL_DEVICE_TYPE_GPU:
                SB.append("GPU" + '\n'); break;
            case CL_DEVICE_TYPE_CPU:
                SB.append("CPU" + '\n'); break;
            default:
                SB.append("Unknown" + '\n'); break;
        }

        SB.append("Device Version: " + InfoUtil.getDeviceInfoStringUTF8(Device, CL_DEVICE_VERSION) + '\n');

        SB.append("Language Version: " + InfoUtil.getDeviceInfoStringUTF8(Device, CL_DEVICE_OPENCL_C_VERSION) + '\n');

        SB.append("Maximum Compute Units: " + InfoUtil.getDeviceInfoInt(Device, CL_DEVICE_MAX_COMPUTE_UNITS) + " Units" + '\n');

        SB.append("Maximum Workitem Dimension: " + InfoUtil.getDeviceInfoInt(Device, CL_DEVICE_MAX_WORK_ITEM_DIMENSIONS) + 'D' + '\n');

        SB.append("Maximum Workgroup Size: " + InfoUtil.getDeviceInfoLong(Device, CL_DEVICE_MAX_WORK_GROUP_SIZE) + '\n');

        SB.append("Maximum Clock Frequency: " + InfoUtil.getDeviceInfoInt(Device, CL_DEVICE_MAX_CLOCK_FREQUENCY) + " Hz" + '\n');

        SB.append("Allocated Stack Size: " + Stack.getSize() + " Bytes" + '\n');

        System.out.println(SB);

    }

    public static void Init() {
        Stack = MemoryStack.create(500_000_000); //500 MB

        ErrorcodeReturn = Stack.callocInt(1);

        GetPlatformAndDevice(Stack);

        GetContextCallback();

        Context = clCreateContextFromType(GetContextProperties(Stack), CL_DEVICE_TYPE_GPU, null, 0, ErrorcodeReturn);
        checkCLError(ErrorcodeReturn);

        CommandQueue = clCreateCommandQueue(Context, Device, CL_QUEUE_PROFILING_ENABLE, ErrorcodeReturn);
        checkCLError(ErrorcodeReturn);

        PrintInfo();
    }

    public static void Dispose() {
        Programs.forEach((Name, Program) -> {
            Program.MemoryObjects.forEach((Key, MemObject) -> {
                clReleaseMemObject(MemObject);
            });
            Program.MemoryObjects.clear();

            clReleaseKernel(Program.Kernel);
        });
        Programs.clear();

        clReleaseCommandQueue(CommandQueue);
        clReleaseContext(Context);
        CL.destroy();

        try {
            Stack.close();
        } catch (Exception E) {}
    }
}
