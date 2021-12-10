import Compute.ComputeProgram;
import Compute.GPU;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Scanner;

import static org.lwjgl.opencl.CL10.*;

public class Main {

    static Scanner Input;
    static String[] WordList;

    static int LongestWordLength;
    static IntBuffer DistanceMatricesBuffer;
    static IntBuffer BaseBuffer;
    static IntBuffer BaseSizeBuffer;
    static IntBuffer LongestWordLenBuffer;

    static boolean QueryGPU = true;
    static boolean QueryCPU = true;
    static boolean PrintGPU = false;
    static boolean PrintCPU = false;
    static boolean HeadrGPU = false;
    static boolean HeadrCPU = false;
    static boolean TimesGPU = false;
    static boolean TimesCPU = false;
    static boolean Manual = false;

    private static String[] LoadWordList(String List) {
        long FunctionStartTimer = System.nanoTime();
        ArrayList<String> WordList = new ArrayList<>();
        InputStream IS = Main.class.getClassLoader().getResourceAsStream(List);
        try (BufferedReader BR = new BufferedReader( new InputStreamReader(IS, StandardCharsets.UTF_8))) {
            String Line;
            while ((Line = BR.readLine()) != null) {
                WordList.add(Line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        String[] WordListArray = new String[WordList.size()];

        for (String Word : WordList) {
            if (Word.length() > LongestWordLength) {
                LongestWordLength = Word.length();
            }
        }
        long FunctionTimeTaken1 = (System.nanoTime() - FunctionStartTimer);
        System.out.printf("Load Time (Word List + GPU Buffer): %dns / %dms %n", FunctionTimeTaken1, FunctionTimeTaken1 / 1000000);

        DistanceMatricesBuffer = GPU.Stack.callocInt(
                (LongestWordLength+1) * (LongestWordLength+1) //Matrix Dimensions
                     * WordList.size() //Multiplied by the number of words that will need a matrix
        );

        BaseBuffer = GPU.Stack.callocInt(LongestWordLength * WordList.size());
        for (String s : WordList) {
            for (int j = 0; j < LongestWordLength; j++) {
                if (j < s.length()) {
                    BaseBuffer.put(s.charAt(j));
                } else {
                    BaseBuffer.put(0);
                }
            }
        }
        BaseBuffer.position(0);

        BaseSizeBuffer = GPU.Stack.callocInt(1);
        BaseSizeBuffer.put(0, WordList.size());

        LongestWordLenBuffer = GPU.Stack.callocInt(1);
        LongestWordLenBuffer.put(0, LongestWordLength);

        long FunctionTimeTaken2 = (System.nanoTime() - FunctionStartTimer);
        System.out.printf("Load Time (Word List + GPU Buffer): %dns / %dms %n", FunctionTimeTaken2, FunctionTimeTaken2 / 1000000);
        return WordList.toArray(WordListArray);
    }

    public static void QueryCPULev(String SearchTerm) {
        long FunctionStartTimer = System.nanoTime();
        SearchTree LevenshteinTree = new SearchTree(
                new LevenshteinData("#", 7.5f)
        );

        SearchTerm = "#" + SearchTerm.toLowerCase();

        for (String Base : WordList) {
            Levenshtein.Calculate(Base, SearchTerm, LevenshteinTree);
        }

        long FunctionTimeTaken1 = System.nanoTime() - FunctionStartTimer;
        if (TimesCPU) System.out.printf("%nCPU Query: %dns / %dms %n%n", FunctionTimeTaken1, FunctionTimeTaken1 / 1000000);
        if (PrintCPU) {
            LevenshteinTree.PrintInorder(LevenshteinTree, new int[] {0, 5});
        }
    }

    public static void QueryGPULev(String SearchTerm) {
        long FunctionStartTimer = System.nanoTime();

        SearchTerm = SearchTerm.toLowerCase();

        //region Search Term
        IntBuffer SearchTermBuffer = GPU.Stack.callocInt((SearchTerm).length());
        for (char CurrentChar : SearchTerm.toCharArray()) {
            SearchTermBuffer.put(CurrentChar);
        }
        SearchTermBuffer.position(0);
        //endregion

        //region Search Term Length
        IntBuffer SearchTermLengthBuffer = GPU.Stack.callocInt(1);
        SearchTermLengthBuffer.put(0, SearchTerm.length());
        //endregion

        FloatBuffer OutBuffer = GPU.Stack.callocFloat(WordList.length);
        
        ComputeProgram SolverProgram = GPU.Programs.get("LevenshteinSolver");

        int Flags = CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR;
        SolverProgram.CreateWriteIntBuffer(0, SearchTermBuffer, Flags);
        SolverProgram.CreateWriteIntBuffer(1, SearchTermLengthBuffer, Flags);
        SolverProgram.CreateWriteIntBuffer(2, BaseBuffer, Flags);
        SolverProgram.CreateWriteIntBuffer(3, BaseSizeBuffer, Flags);
        SolverProgram.CreateWriteIntBuffer(4, LongestWordLenBuffer, Flags);
        
        SolverProgram.CreateIntBuffer(5, DistanceMatricesBuffer, Flags);
        SolverProgram.CreateFloatBuffer(6, OutBuffer, Flags);

        SolverProgram.Dimensions = 1;
        SolverProgram.GlobalSize = WordList.length;

        long FunctionTimeTaken1 = System.nanoTime() - FunctionStartTimer;
        if (TimesGPU) System.out.printf("%nGPU Query (Preprocessing): %dns / %dms %n", FunctionTimeTaken1, FunctionTimeTaken1 / 1000000);

        SolverProgram.AutoSetKernelArgs();
        SolverProgram.AutoEnqueue1D();
        SolverProgram.ReadFloatBuffer(6, OutBuffer);
        long FunctionTimeTaken2 = System.nanoTime() - FunctionStartTimer;
        if (TimesGPU) System.out.printf("GPU Query (Processing): %dns / %dms %n", FunctionTimeTaken2 - FunctionTimeTaken1, (FunctionTimeTaken2 - FunctionTimeTaken1) / 1000000);

        SearchTree LevenshteinTree = new SearchTree(
                new LevenshteinData("#", 7.5f)
        );

        int TermIndex = 0;
        while (OutBuffer.hasRemaining()) {
            LevenshteinTree.Insert(new SearchTree(
                    new LevenshteinData(WordList[TermIndex++], OutBuffer.get())
            ));
        }
        long FunctionTimeTaken3 = System.nanoTime() - FunctionStartTimer;
        if (TimesGPU) System.out.printf("GPU Query (Postprocessing): %dns / %dms %n", FunctionTimeTaken3 - FunctionTimeTaken2, (FunctionTimeTaken3 - FunctionTimeTaken2) / 1000000);
        if (TimesGPU) System.out.printf("GPU Query (Total): %dns / %dms %n", FunctionTimeTaken3, FunctionTimeTaken3 / 1000000);

        if (PrintGPU) {
            LevenshteinTree.PrintInorder(LevenshteinTree, new int[] {0, 5});
        }
    }

    public static void main(String[] args) {
        GPU.Init();

        GPU.AddProgram(
                "LevenshteinSolver",
                Main.class.getClassLoader().getResourceAsStream("LevenshteinSolver.cl")
        );

        WordList = LoadWordList("RL/RandomNumberFileSize=20000.txt");

        if (Manual) {
            Input = new Scanner(System.in);

            while (QueryCPU && QueryGPU) {
                System.out.println("Enter Search Term: ");
                String SearchTerm = Input.nextLine();

                if (QueryGPU) {
                    if (HeadrGPU) System.out.println("GPU: ");
                    QueryGPULev(SearchTerm);
                    if (HeadrCPU) System.out.println();
                }

                if (QueryCPU) {
                    if (HeadrCPU) System.out.println("CPU: ");
                    QueryCPULev(SearchTerm);
                    System.out.println();
                }
            }
        }
        else {
            Tests.Test2();
        }
        GPU.Dispose();
    }

}
