package Compute;

import java.io.*;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.lwjgl.PointerBuffer;

import java.nio.FloatBuffer;
import java.util.HashMap;

import static Compute.GPU.*;
import static Compute.InfoUtil.checkCLError;
import static org.lwjgl.opencl.CL10.*;
import static org.lwjgl.system.MemoryUtil.*;

import org.apache.commons.io.*;

public class ComputeProgram {

    //Programs must have the same filename as the kernel's name!!!!
    public long Kernel;

    private static String LoadSource(String Path) throws IOException {
        return Files.readString(Paths.get(Path));
    }

    public ComputeProgram(long Context, String SourcePath) {
        String Source = null;
        try {
            Source = LoadSource(SourcePath);
        } catch (IOException E) {
            System.err.println("Could not read Program at: " + SourcePath + " !!");
            return;
        }
        String SourceFilename = FilenameUtils.getBaseName(SourcePath);

        long Program = clCreateProgramWithSource(Context, Source, null);

        BuildProgram(Program);

        Kernel = clCreateKernel(Program, SourceFilename, ErrorcodeReturn);
        checkCLError(ErrorcodeReturn);
        clReleaseProgram(Program);
    }

    public ComputeProgram(long Context, InputStream IS, String ProgramName) {
        StringBuilder Source = new StringBuilder();
        try (BufferedReader BR = new BufferedReader( new InputStreamReader(IS, StandardCharsets.UTF_8))) {
            String Line;
            while ((Line = BR.readLine()) != null) {
                Source.append(Line);
            }
        } catch (IOException e) {
            System.err.println("Could not read Program named: " + ProgramName + " !!");
            return;
        }

        long Program = clCreateProgramWithSource(Context, Source.toString(), null);

        BuildProgram(Program);

        Kernel = clCreateKernel(Program, ProgramName, ErrorcodeReturn);
        checkCLError(ErrorcodeReturn);
        clReleaseProgram(Program);
    }

    private void BuildProgram(long Program) {
        if (clBuildProgram(Program, GPU.Device, "", null, NULL) != CL_SUCCESS) {

            ByteBuffer BuildLog = ByteBuffer.allocateDirect(500000);
            PointerBuffer BuildLogSize = PointerBuffer.allocateDirect(1);

            if ( clGetProgramBuildInfo(Program, Device, CL_PROGRAM_BUILD_LOG, BuildLog, BuildLogSize) == CL_SUCCESS ) {
                System.out.println(BuildLogSize.get(0));
                byte[] cock = new byte[(int)BuildLogSize.get(0)]; BuildLog.get(cock);
                try {
                    System.out.println(new String(cock, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    public int GlobalSize = 1;
    public int LocalSize = Integer.MIN_VALUE;
    public int Dimensions = 1;

    public HashMap<Integer,Long> MemoryObjects = new HashMap<Integer,Long>();

    public void CreateFloatBuffer(int ArgumentIndex, FloatBuffer Capacity, int Flags) {
        long MemoryObject = clCreateBuffer(Context, Flags, Capacity, ErrorcodeReturn);
        checkCLError(ErrorcodeReturn);
        MemoryObjects.put(ArgumentIndex, MemoryObject);
    }

    public void WriteFloatBuffer(int ArgumentIndex, FloatBuffer Data) {
        checkCLError(clEnqueueWriteBuffer(CommandQueue, MemoryObjects.get(ArgumentIndex), true, 0, Data, null, null));
    }

    public void CreateIntBuffer(int ArgumentIndex, IntBuffer Capacity, int Flags) {
        long MemoryObject = clCreateBuffer(Context, Flags, Capacity, ErrorcodeReturn);
        checkCLError(ErrorcodeReturn);
        MemoryObjects.put(ArgumentIndex, MemoryObject);
    }

    public void WriteIntBuffer(int ArgumentIndex, IntBuffer Data) {
        checkCLError(clEnqueueWriteBuffer(CommandQueue, MemoryObjects.get(ArgumentIndex), true, 0, Data, null, null));
    }

    public void CreateWriteFloatBuffer(int ArgumentIndex, FloatBuffer InitialData, int Flags) {
        CreateFloatBuffer(ArgumentIndex, InitialData, Flags);
        WriteFloatBuffer(ArgumentIndex, InitialData);
    }

    public void CreateWriteIntBuffer(int ArgumentIndex, IntBuffer InitialData, int Flags) {
        CreateIntBuffer(ArgumentIndex, InitialData, Flags);
        WriteIntBuffer(ArgumentIndex, InitialData);
    }

    public void AutoSetKernelArgs() {
        MemoryObjects.forEach((ArgumentIndex, MemObject) -> {
            //System.out.println(Kernel + " " + ArgumentIndex + " " + MemObject);
            clSetKernelArg1p(Kernel, ArgumentIndex, MemObject);
        });
    }

    public void AutoEnqueue1D() {
        PointerBuffer GlobalWorksizeBuffer = GPU.Stack.callocPointer(1);
        GlobalWorksizeBuffer.put(0, GlobalSize);

        PointerBuffer LocalWorksizeBuffer = GPU.Stack.callocPointer(1);
        LocalWorksizeBuffer.put(0, LocalSize);

        PointerBuffer KernelEvent = GPU.Stack.callocPointer(1);

        clEnqueueNDRangeKernel(
                CommandQueue,
                Kernel,
                Dimensions,
                null,
                GlobalWorksizeBuffer,
                LocalSize == Integer.MIN_VALUE ? null : LocalWorksizeBuffer,
                null,
                KernelEvent
        );

        clWaitForEvents(KernelEvent);
    }

    public void ReadIntBuffer(int OutputArgumentIndex, IntBuffer Buffer) {
        clEnqueueReadBuffer(
                CommandQueue,
                MemoryObjects.get(OutputArgumentIndex),
                true,
                0,
                Buffer,
                null,
                null
        );
    }

    public void ReadFloatBuffer(int OutputArgumentIndex, FloatBuffer Buffer) {
        clEnqueueReadBuffer(
                CommandQueue,
                MemoryObjects.get(OutputArgumentIndex),
                true,
                0,
                Buffer,
                null,
                null
        );
    }
}
