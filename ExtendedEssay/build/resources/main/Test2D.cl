int C2D(int X, int XS, int Y) {
    return XS * Y + X;
}

int C3D(int X, int XS, int Y, int Z, int ZS) {
    return X + XS * (Y + ZS * Z);
    /*return X + (Y * XS) + (Z * XS * ZS);*/
}

kernel void Test2D(
    global const int *Search,
    global const int *SearchWordLen,
    global const int *Base,
    global const int *BaseSize,
    global const int *LongestWordLen,
    global int *Matrices,
    global int *Output
    )
{
    int GS1 = get_global_size(1);
    int GI1 = get_global_id(1);
    int GS0 = get_global_size(0);
    int GI0 = get_global_id(0);

    int LWL = LongestWordLen[0]+1;
    int BS = BaseSize[0];
    int BaseLen = 0;

    for (int i = 0; i < LWL; i++) {
        Matrices[C3D(i, LWL, 0, GI1, LWL)] = i;
    }
    for (int j = 0; j < LWL; j++) {
        Matrices[C3D(0, LWL, j, GI1, LWL)] = j;
    }

    for (int x = 1; x < LWL; x++) { /*For each character in the search term*/
        int CurrentSearchChar = x == 0 ? 1 : Search[x - 1];
        for (int y = 1; y < LWL; y++) { /*For each character in the base assigned to this work item*/
            int CurrentBaseChar = y == 0 ? 1 : Base[((GI1 * LWL-1) + y) - 1];

            printf("%u %u \n", CurrentSearchChar, CurrentBaseChar);

            /*BaseLen += 1 * ( !(x^0) & (CurrentBaseChar^0) );*/
            if (x == 0 && CurrentBaseChar != 0) {
                BaseLen++;
            }

            /*if (min(x,y) == 0) {
                Matrices[C3D(x, LWL, y, GI1, LWL)] = max(x,y);
            }
            else {
                int Term1 = Matrices[C3D(x - 1, LWL, y, GI1, LWL)] + 1;
                int Term2 = Matrices[C3D(x, LWL, y - 1, GI1, LWL)] + 1;
                int Term3 = Matrices[C3D(x - 1, LWL, y - 1, GI1, LWL)];

                if (CurrentSearchChar != CurrentBaseChar) {
                    Term3++;
                }

                int toadd = 0;
                if (x > SearchWordLen[0]) {
                    toadd = -1;
                }
                else if (y > BaseLen) {
                    toadd = -1;
                }
                else {
                    toadd = min(Term1, min(Term2, Term3));
                }
                Matrices[C3D(x, LWL, y, GI1, LWL)] = toadd;
            }*/

            int penis = CurrentSearchChar != CurrentBaseChar ? 1 : 0;
            int t1 = Matrices[C3D(x-1,LWL,y,GI1,LWL)];
            int t2 = Matrices[C3D(x,LWL,y-1,GI1,LWL)];
            int t3 = Matrices[C3D(x-1,LWL,y-1,GI1,LWL)] + penis;

            Matrices[C3D(x, LWL, y, GI1, LWL)] = min(t1,min(t2,t3));
        }
        printf("\n","");
    }

    /*if (GI1 == 0) {printf("%u %u\n", SearchWordLen[0] - 1, BaseLen - 1);}*/
    Output[GI1] = Matrices[C3D(SearchWordLen[0]-1,LWL,BaseLen-1,GI1,LWL)];
}