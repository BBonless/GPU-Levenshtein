int C2D(int X, int XS, int Y) {
    return XS * Y + X;
}

kernel void Test2D(global const int *Search, global const int *SearchWordLen, global const int *Base, global const int *LongestWordLen, global int *Output) {
    uint GS1 = get_global_size(1);
    uint GI1 = get_global_id(1);
    uint GS0 = get_global_size(0);
    uint GI0 = get_global_id(0);

    int WordTotal = 0;

    for (int i = 0; i < LongestWordLen; i++) {
        int CurrentChar = Base[C2D(i, GS0, GI1)];

        if (CurrentChar == 0) {
            break;
        }

        WordTotal += CurrentChar;
    }

    Output[GI1] = WordTotal;

}