import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class Util {
    public static float[] FloatBuffer2Array(FloatBuffer Buffer) {
        float[] Result = new float[Buffer.capacity()];
        for (int i = 0; i < Result.length; i++) {
            Result[i] = Buffer.get(i);
        }
        return Result;
    }

    public static int[] IntBuffer2Array(IntBuffer Buffer) {
        int[] Result = new int[Buffer.capacity()];
        for (int i = 0; i < Result.length; i++) {
            Result[i] = Buffer.get(i);
        }
        return Result;
    }
}
