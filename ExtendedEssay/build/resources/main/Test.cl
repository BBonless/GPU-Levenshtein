kernel void Test(global const int *Integers, global int *Out) {
    uint GI = get_global_id(0);
    uint GS = get_global_size(0);
    uint LI = get_local_id(0);
    uint LS = get_local_size(0);

    Out[GI] = LI;
}