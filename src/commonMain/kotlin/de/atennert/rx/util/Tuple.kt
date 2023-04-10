package de.atennert.rx.util

/*
 * See https://dzone.com/articles/kotlin-the-tuple-type
 */

data class Tuple1<out A>(val v1: A)
data class Tuple2<out A, out B>(val v1: A, val v2: B)
data class Tuple3<out A, out B, out C>(val v1: A, val v2: B, val v3: C)

object Tuple {
    operator fun <A> invoke(v1: A) = Tuple1(v1)
    operator fun <A, B> invoke(v1: A, v2: B) = Tuple2(v1, v2)
    operator fun <A, B, C> invoke(v1: A, v2: B, v3: C) = Tuple3(v1, v2, v3)
}