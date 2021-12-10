import java.util.Random;

public class Tests {
    public static String GenRandString(int Length) {
        Random R = new Random();
        StringBuilder Result = new StringBuilder();
        for (int i = 0; i < Length; i++) {
            Result.append((char)(int)(97 + R.nextFloat() * 25));
        }
        return Result.toString();
    }

    //Testing the speed of different search term sizes
    //Range: 1, 3, 5, 7, 10, 25, 50, 75, 100, 500, 1000, 2000, 5000, 10000
    //Repeats: 30?
    public static void Test1() {
        //Warmup
        Main.QueryCPULev(GenRandString(10));

        int Repeats = 30;
        int[] SearchTermSizes = new int[] {1,3,5,7,10,25,50,75,100,500,1000,2000,5000,10000};
        System.out.println("Speed of different search term sizes");
        System.out.println();
        for (int SearchTermSize : SearchTermSizes) {
            String[] TestSearchTerms = new String[Repeats];
            for (int i = 0; i < Repeats; i++) {
                TestSearchTerms[i] = GenRandString(SearchTermSize);
            }

            long SuperTotalTime = System.nanoTime();
            long TotalTime = 0;
            for (String TestSearchTerm : TestSearchTerms) {
                long IndividualTime = System.nanoTime();
                Main.QueryCPULev(TestSearchTerm);
                TotalTime += System.nanoTime() - IndividualTime;
            }

            System.out.printf("%nResults for search term size of %d%n", SearchTermSize);
            System.out.printf("Overall Time Taken: %dns / %dms %n", System.nanoTime() - SuperTotalTime, (System.nanoTime() - SuperTotalTime) / 1000000);
            System.out.printf("Average Time Taken: %dns / %dms %n", (TotalTime / Repeats), (TotalTime / Repeats) / 1000000);

        }
    }

    //Simply run a query for a dataset 30 times
    public static void Test2() {
        int Repeats = 30;

        long SuperTotalTime = System.nanoTime();
        long TotalTime = 0;
        for (int R = 0; R < Repeats; R++) {
            String SearchTerm = GenRandString(15);

            long ITime = System.nanoTime();
            Main.QueryGPULev(SearchTerm);
            TotalTime += System.nanoTime() - ITime;
        }
        System.out.printf("%nResults for current dataset %n");
        System.out.printf("Overall Time Taken: %dns / %dms %n", System.nanoTime() - SuperTotalTime, (System.nanoTime() - SuperTotalTime) / 1000000);
        System.out.printf("Average Time Taken: %dns / %dms %n", (TotalTime / Repeats), (TotalTime / Repeats) / 1000000);
    }

}