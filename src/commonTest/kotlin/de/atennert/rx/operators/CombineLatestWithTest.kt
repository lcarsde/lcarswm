package de.atennert.rx.operators

import de.atennert.rx.Observable
import de.atennert.rx.Subscriber
import de.atennert.rx.util.Tuple1
import de.atennert.rx.util.Tuple2
import kotlin.test.Test
import kotlin.test.assertContentEquals

class CombineLatestWithTest {
    @Test
    fun `combine 1 observable`() {
        val observer = object : Subscriber<Tuple1<Int>>() {
            val received = mutableListOf<Any>()
            override fun next(value: Tuple1<Int>) {
                received.add(value)
            }
            override fun complete() {
                received.add("complete")
            }
        }
        Observable.of(1)
            .apply(combineLatestWith())
            .subscribe(observer)

        assertContentEquals(listOf(Tuple1(1), "complete"), observer.received)
    }

    @Test
    fun `combine 2 observables`() {
        val observer = object : Subscriber<Tuple2<Int, Int>>() {
            val received = mutableListOf<Any>()
            override fun next(value: Tuple2<Int, Int>) {
                received.add(value)
            }
            override fun complete() {
                received.add("complete")
            }
        }
        Observable.of(1)
            .apply(combineLatestWith(Observable.of(2)))
            .subscribe(observer)

        assertContentEquals(listOf(Tuple2(1, 2), "complete"), observer.received)
    }
}