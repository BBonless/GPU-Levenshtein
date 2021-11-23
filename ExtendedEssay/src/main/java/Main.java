import Compute.ComputeProgram;
import Compute.GPU;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static java.lang.Math.*;
import static org.lwjgl.opencl.CL10.CL_MEM_COPY_HOST_PTR;
import static org.lwjgl.opencl.CL10.CL_MEM_READ_ONLY;

public class Main {

    static Scanner Input;
    static String[] WordList;

    private static String[] LoadWordList() {
        ArrayList<String> WordList = new ArrayList<>();
        InputStream IS = Main.class.getClassLoader().getResourceAsStream("10kwordlist.txt");
        try (BufferedReader BR = new BufferedReader( new InputStreamReader(IS, StandardCharsets.UTF_8))) {
            String Line;
            while ((Line = BR.readLine()) != null) {
                WordList.add(Line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        String[] WordListArray = new String[WordList.size()];
        return WordList.toArray(WordListArray);
    }

    public static void MainLevLoop() {
        for(;;) {
            SearchTree LevenshteinTree = new SearchTree(
                    new LevenshteinData("#", 7.5f)
            );

            System.out.println("Enter Search String: ");
            String SearchString = "#" + Input.nextLine().toLowerCase();

            long StartTime = System.currentTimeMillis();

            for (String Base : WordList) {
                Levenshtein.Calculate(Base, SearchString, LevenshteinTree);
            }

            long LevenshteinTime = System.currentTimeMillis() - StartTime;

            LevenshteinTree.PrintInorder(LevenshteinTree, new int[] {0, 5});

            System.out.println("Score Calculation Time Taken: " + LevenshteinTime + "ms");
            System.out.println("Total Time Taken: " + (System.currentTimeMillis() - StartTime) + "ms");
        }
    }

    public static void main(String[] args) {
        Input = new Scanner(System.in);
        WordList = LoadWordList();

        GPU.Init();

        {
            IntBuffer SearchBuffer = GPU.Stack.callocInt(("Penis").length());
            SearchBuffer.put('p');
            SearchBuffer.put('e');
            SearchBuffer.put('n');
            SearchBuffer.put('i');
            SearchBuffer.put('s');
            SearchBuffer.position(0);

            IntBuffer SearchWordLenBuffer = GPU.Stack.callocInt(1);
            SearchWordLenBuffer.put(0, 5);

            int LongestWordLength = 0;
            for (String Word : WordList) {
                if (Word.length() > LongestWordLength) {
                    LongestWordLength = Word.length();
                }
            }

            IntBuffer BaseBuffer = GPU.Stack.callocInt(LongestWordLength * WordList.length);
            for (int i = 0; i < WordList.length; i++) {
                for (int j = 0; j < LongestWordLength; j++) {
                    if (j < WordList[i].length() )  {
                        BaseBuffer.put(WordList[i].charAt(j));
                    }
                    else {
                        BaseBuffer.put(0);
                    }
                }
            }
            BaseBuffer.position(0);

            IntBuffer LongestWordLenBuffer = GPU.Stack.callocInt(1);
            LongestWordLenBuffer.put(0, LongestWordLength);

            IntBuffer OutBuffer = GPU.Stack.callocInt(WordList.length);

            GPU.AddProgram(
                    "Test2D",
                    Main.class.getClassLoader().getResourceAsStream("Test2D.cl")
            );
            ComputeProgram PGR = GPU.Programs.get("Test2D");

            int F = CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR;
            PGR.CreateWriteIntBuffer(0, SearchBuffer, F);
            PGR.CreateWriteIntBuffer(1, SearchWordLenBuffer, F);
            PGR.CreateWriteIntBuffer(2, BaseBuffer, F);
            PGR.CreateWriteIntBuffer(3, LongestWordLenBuffer, F);
            PGR.CreateIntBuffer(4, OutBuffer, F);

            PGR.Dimensions = 2;
            PGR.x = LongestWordLength;
            PGR.y = WordList.length;
            PGR.AutoSetKernelArgs();
            PGR.AutoEnqueue(
                    new int[] {4},
                    OutBuffer
            );

            //int[] Input = Util.IntBuffer2Array(BaseBuffer);
            int[] Result = Util.IntBuffer2Array(OutBuffer);

            /*for (int i = 0; i < Input.length; i++) {
                if (i % LongestWordLength == 0 && i != 0) {
                    System.out.println();
                }
                System.out.print(Input[i]);
                System.out.print(' ');
            }
            System.out.println();
            System.out.println();*/

            for (int i = 0; i < Result.length; i++) {
                if (i % 99999 == 0 && i != 0) {
                    System.out.println();
                }
                System.out.print(Result[i]);
                System.out.print(' ');
            }
            System.out.println();
            System.out.println();

        }

        GPU.Dispose();

        MainLevLoop();
    }

    private static void Test1() {
        GPU.AddProgram(
                "Test",
                Main.class.getClassLoader().getResourceAsStream("Test.cl")
        );

        GPU.Programs.get("Test").GlobalSize = 64;
        GPU.Programs.get("Test").LocalSize = 8;

        IntBuffer Buffer = GPU.Stack.callocInt(64);
        for (int i = 0; i < 64; i++) {
            Buffer.put(i, i);
        }
        IntBuffer Output = GPU.Stack.callocInt(64);

        GPU.Programs.get("Test").CreateIntBuffer(0, Buffer, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR);
        GPU.Programs.get("Test").WriteIntBuffer(0, Buffer);
        GPU.Programs.get("Test").CreateIntBuffer(1, Output, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR);

        GPU.Programs.get("Test").AutoSetKernelArgs();
        GPU.Programs.get("Test").Dimensions = 1;
        GPU.Programs.get("Test").AutoEnqueue(
                new int[] {1},
                Output
        );

        int[] Result = Util.IntBuffer2Array(Output);

        for (int i = 0; i < Result.length; i++) {
            System.out.print(Result[i]);
            System.out.print(' ');
        }
        System.out.println();
    }
}
