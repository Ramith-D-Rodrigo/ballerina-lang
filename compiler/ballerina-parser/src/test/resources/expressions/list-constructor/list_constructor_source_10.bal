function foo() {
    var v1 = [...n];
    var v2 = [...n, 1, 2];
    var v3 = [1, 2, ...n];
    var v4 = [1, 2, ...[3, 4]];
    var v5 = [1, 2, ...n, 5, 6];
    var v6 = [1, 2, ...[4, 5], 5, 6];
    var v7 = [1, 2, ...n1, ...n2];
    var v8 = [...[2, ...["bar", ...n1, [...n2], "baz", 6]], ...n3];
}
