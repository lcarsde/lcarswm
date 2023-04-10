package de.atennert.rx.util

/*
 * See https://dzone.com/articles/kotlin-the-tuple-type
 */

data class Tuple1<out A>(val v1: A)
data class Tuple2<out A, out B>(val v1: A, val v2: B)

object Tuple {
    operator fun <A> invoke(v1: A) = Tuple1(v1)
    operator fun <A, B> invoke(v1: A, v2: B) = Tuple2(v1, v2)
}