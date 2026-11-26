package top.ltfan.m3x.backdrop.internal

/** Returns the smallest power-of-two array capacity that holds [candidateCount] elements. */
internal fun shaderUniformCapacity(candidateCount: Int, minimum: Int = 1): Int {
    require(candidateCount >= 0)
    require(minimum >= 1 && minimum.countOneBits() == 1)
    var capacity = minimum
    while (capacity < candidateCount) capacity *= 2
    return capacity
}
