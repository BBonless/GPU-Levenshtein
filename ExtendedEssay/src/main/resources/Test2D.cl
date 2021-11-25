int C3D(int X, int XS, int Y, int Z, int ZS) {
    return X + XS * (Y + ZS * Z);
}

kernel void Test2D(
    global const int *Search,
    global const int *SearchWordLen,
    global const int *Base,
    global const int *BaseSize,
    global const int *LongestWordLen,
    global int *Matrices,
    global float *Output
    )
{
    int GI = get_global_id(0);
    int LWL = LongestWordLen[0]+1;
    
    int CurrentBaseLen = 0;
    bool CurrentBaseLenFound = false;

    for (int i = 0; i < LWL; i++) {
        Matrices[C3D(i, LWL, 0, GI, LWL)] = i;
    }
    for (int j = 0; j < LWL; j++) {
        Matrices[C3D(0, LWL, j, GI, LWL)] = j;
    }

    for (int x = 1; x < SearchWordLen[0] + 1; x++) { /*For each character in the search term*/
        int CurrentSearchChar = Search[x - 1];
        for (int y = 1; y < LWL; y++) { /*For each character in the base assigned to this work item*/
            int CurrentBaseChar = Base[((GI * LWL-1) + y) - GI];

            if (CurrentBaseChar == 0) {
                break;
            }

            CurrentBaseLen += 1 * (!(CurrentBaseLenFound^0) && (CurrentBaseChar^0));

            if (min(x,y) == 0) {
                Matrices[C3D(x, LWL, y, GI, LWL)] = max(x,y);
            }
            else {
                int Term1 = Matrices[C3D(x - 1, LWL, y, GI, LWL)] + 1;
                int Term2 = Matrices[C3D(x, LWL, y - 1, GI, LWL)] + 1;
                int Term3 = Matrices[C3D(x - 1, LWL, y - 1, GI, LWL)];

                Term3 += 1 * (bool)(CurrentSearchChar^CurrentBaseChar);

                Matrices[C3D(x, LWL, y, GI, LWL)] = min(Term1, min(Term2, Term3));
            }
        }
        CurrentBaseLenFound = true;
    }

    int Distance = Matrices[C3D(SearchWordLen[0],LWL,CurrentBaseLen,GI,LWL)];
    int TotalLength = SearchWordLen[0] + CurrentBaseLen;
    float Ratio = ((float)(TotalLength - Distance) / TotalLength) * (bool)(Distance^0);

    Output[GI] = Distance + Ratio;
}