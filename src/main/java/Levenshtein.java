import static java.lang.Math.max;
import static java.lang.Math.min;

public class Levenshtein {
    public static void Calculate(String BaseTerm, String SearchTerm, SearchTree Tree) {
        String OperatingOriginalString = "#" + BaseTerm;

        int[][] Matrix = new int[ OperatingOriginalString.length() ][ SearchTerm.length() ];

        for (int y = 0; y < Matrix.length; y++) {
            for (int x = 0; x < Matrix[0].length; x++) {

                if (min(x, y) == 0) {
                    Matrix[y][x] = max(x, y);
                }
                else {
                    int Term1 = Matrix[y - 1][x] + 1;
                    int Term2 = Matrix[y][x - 1] + 1;
                    int Term3 = Matrix[y - 1][x - 1];
                    if (OperatingOriginalString.charAt(y) != SearchTerm.charAt(x)) {
                        Term3++;
                    }

                    Matrix[y][x] = min(Term1, min(Term2, Term3));
                }

            }
        }

        int Distance = Matrix[OperatingOriginalString.length() - 1][SearchTerm.length() - 1];
        int TotalLen = (OperatingOriginalString.length() - 1) + (SearchTerm.length() - 1);

        float Ratio = (float)(TotalLen - Distance) / (float)TotalLen;
        float Score = (float)Distance + (Distance == 0 ? 0 : Ratio);

        Tree.Insert(new SearchTree(
                new LevenshteinData(BaseTerm, Score)
        ));
    }

    public static void CalculateTreeless(String Original, String Search) {
        String OperatingOriginalString = "#" + Original;

        int[][] Matrix = new int[ OperatingOriginalString.length() ][ Search.length() ];

        for (int y = 0; y < Matrix.length; y++) {
            for (int x = 0; x < Matrix[0].length; x++) {

                if (min(x, y) == 0) {
                    Matrix[y][x] = max(x, y);
                }
                else {
                    int Term1 = Matrix[y - 1][x] + 1;
                    int Term2 = Matrix[y][x - 1] + 1;
                    int Term3 = Matrix[y - 1][x - 1];
                    if (OperatingOriginalString.charAt(y) != Search.charAt(x)) {
                        Term3++;
                    }

                    Matrix[y][x] = min(Term1, min(Term2, Term3));
                }

            }
        }
        int Distance = Matrix[OperatingOriginalString.length() - 1][Search.length() - 1] - 1;
        System.out.println(Distance);
    }
}
