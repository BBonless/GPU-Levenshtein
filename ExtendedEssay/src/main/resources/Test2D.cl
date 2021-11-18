kernel void Test2D(global const int *A2D, global int *Output) {
    uint GS1 = get_global_size(1);
    uint GI1 = get_global_id(1);
    uint GS0 = get_global_size(0);
    uint GI0 = get_global_id(0);

    uint C2D = GS0 * GI1 + GI0;

    uint Total = 0;
    for (int i = 0; i < 3; i++) {
        Total += A2D[C2D + i];
    }
    Output[GI1] = Total;

}