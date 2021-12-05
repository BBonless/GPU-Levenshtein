int F3D(int X, int XS, int Y, int Z, int ZS) { /*Flatten 3D Coordinates*/
    return X + XS * (Y + ZS * Z);
}

kernel void LevenshteinSolver(
    global const int *Term,
    global const int *SearchTermLen,
    global const int *Base,
    global const int *BaseSize,
    global const int *LongestBaseTermLen,
    global int *DistanceMatrices,
    global float *Output
    )
{
    int GI = get_global_id(0);
    int LTL = LongestBaseTermLen[0]+1;
    
    int CurrentBaseTermLen = 0;
    bool CurrentBaseTermLenFound = false;

    for (int i = 0; i < LTL; i++) {
        DistanceMatrices[F3D(i, LTL, 0, GI, LTL)] = i;
    }
    for (int j = 0; j < LTL; j++) {
        DistanceMatrices[F3D(0, LTL, j, GI, LTL)] = j;
    }

    for (int x = 1; x < SearchTermLen[0] + 1; x++) { /*For each character in the search term*/
        int CurrentSearchChar = Term[x - 1];
        for (int y = 1; y < LTL; y++) { /*For each character in the base assigned to this work item*/
            int CurrentBaseChar = Base[((GI * LTL-1) + y) - GI];

            if (CurrentBaseChar == 0) {
                break;
            }

            CurrentBaseTermLen += 1 * (!(CurrentBaseTermLenFound^0) && (CurrentBaseChar^0));

            if (min(x,y) == 0) {
                DistanceMatrices[F3D(x, LTL, y, GI, LTL)] = max(x,y);
            }
            else {
                int EqTerm1 = DistanceMatrices[F3D(x - 1, LTL, y, GI, LTL)] + 1;
                int EqTerm2 = DistanceMatrices[F3D(x, LTL, y - 1, GI, LTL)] + 1;
                int EqTerm3 = DistanceMatrices[F3D(x - 1, LTL, y - 1, GI, LTL)];

                EqTerm3 += 1 * (bool)(CurrentSearchChar^CurrentBaseChar);

                DistanceMatrices[F3D(x, LTL, y, GI, LTL)] = min(EqTerm1, min(EqTerm2, EqTerm3));
            }
        }
        CurrentBaseTermLenFound = true;
    }

    int Distance = DistanceMatrices[F3D(SearchTermLen[0],LTL,CurrentBaseTermLen,GI,LTL)];
    int TotalLength = SearchTermLen[0] + CurrentBaseTermLen;
    float Ratio = ((float)(TotalLength - Distance) / TotalLength) * (bool)(Distance^0);

    Output[GI] = Distance + Ratio;
}