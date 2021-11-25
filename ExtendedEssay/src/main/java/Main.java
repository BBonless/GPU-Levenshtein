import Compute.ComputeProgram;
import Compute.GPU;
import com.sun.source.tree.Tree;

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
import static org.lwjgl.opencl.CL10.*;

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

            Levenshtein.CalculateTreeless("the", "penis");

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
            String Cum = "penis";
            IntBuffer SearchBuffer = GPU.Stack.callocInt((Cum).length());
            for (char C : Cum.toCharArray()) {
                SearchBuffer.put(C);
            }
            SearchBuffer.position(0);

            IntBuffer SearchWordLenBuffer = GPU.Stack.callocInt(1);
            SearchWordLenBuffer.put(0, Cum.length());

            int LongestWordLength = 0;
            for (String Word : WordList) {
                if (Word.length() > LongestWordLength) {
                    LongestWordLength = Word.length();
                }
            }

            IntBuffer DistanceMatricesBuffer = GPU.Stack.callocInt(
                    (LongestWordLength+1) * (LongestWordLength+1) //Matrix Dimensions
                    * WordList.length //Multiplied by the number of words that will need a matrix
            );

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

            IntBuffer BaseSizeBuffer = GPU.Stack.callocInt(1);
            BaseSizeBuffer.put(0, WordList.length);

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
            PGR.CreateWriteIntBuffer(3, BaseSizeBuffer, F);
            PGR.CreateWriteIntBuffer(4, LongestWordLenBuffer, F);
            PGR.CreateIntBuffer(5, DistanceMatricesBuffer, F);
            PGR.CreateIntBuffer(6, OutBuffer, F);

            PGR.Dimensions = 2;
            PGR.x = LongestWordLength;
            PGR.y = WordList.length;
            PGR.AutoSetKernelArgs();
            PGR.AutoEnqueue(
                    new int[] {5,6},
                    DistanceMatricesBuffer, OutBuffer
            );

            //int[] Input = Util.IntBuffer2Array(BaseBuffer);
            int[] Result = Util.IntBuffer2Array(OutBuffer);
            int[] DM = Util.IntBuffer2Array(DistanceMatricesBuffer);

            for (int i = 0; i < 10; i++) {
                for (int x = 0; x < LongestWordLength+1; x++) {
                    for (int y = 0; y < LongestWordLength+1; y++) {
                        int n = DM[C3D(x,LongestWordLength+1,y,i,LongestWordLength+1)];
                        if (n < 10 && n >= 0) {
                            System.out.print('0');
                            System.out.print(n);
                        }
                        else {
                            System.out.print(n);
                        }
                        System.out.print(' ');
                    }
                    System.out.println();
                }
                System.out.println();
            }


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

        for(;;) {
            System.out.println("Enter Search String: ");
            String SearchString = "#" + Input.nextLine().toLowerCase();
            System.out.println("Enter Base String: ");
            String BaseString = "#" + Input.nextLine().toLowerCase();
            Levenshtein.CalculateTreeless(SearchString, BaseString);
        }
        //MainLevLoop();
    }

    static int C3D(int X, int XS, int Y, int Z, int ZS) {
        return X + XS * (Y + ZS * Z);
    }

}
