package de.atennert.rx

fun interface Operator<X, Y> {
    fun call(value: Observable<X>): Observable<Y>
}