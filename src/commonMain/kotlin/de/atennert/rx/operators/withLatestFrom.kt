package de.atennert.rx.operators

import de.atennert.rx.Observable
import de.atennert.rx.Observer
import de.atennert.rx.Operator
import de.atennert.rx.Subscription
import de.atennert.rx.util.Tuple
import de.atennert.rx.util.Tuple1
import de.atennert.rx.util.Tuple2
import de.atennert.rx.util.Tuple3

fun <A> withLatestFrom(): Operator<A, Tuple1<A>> = Operator { source ->
    source.apply(map { Tuple(it) })
}

fun <A, B> withLatestFrom(obs1: Observable<B>): Operator<A, Tuple2<A, B>> {
    return Operator { source ->
        Observable { subscriber ->
            var valueB: B? = null
            val subscription = Subscription()
            subscription.add(obs1.subscribe(object : Observer<B> {
                override fun next(value: B) {
                    valueB = value
                }

                override fun error(error: Throwable) {
                    subscriber.error(error)
                }

                override fun complete() {
                    // Nothing to do
                }
            }))
            subscription.add(source.subscribe(object : Observer<A> {
                override fun next(value: A) {
                    valueB?.let {
                        subscriber.next(Tuple(value, it))
                    }
                }

                override fun error(error: Throwable) {
                    subscriber.error(error)
                }

                override fun complete() {
                    subscriber.complete()
                }
            }))
            subscription.add(subscriber)
            subscription
        }
    }
}

fun <A, B, C> withLatestFrom(obs1: Observable<B>, obs2: Observable<C>): Operator<A, Tuple3<A, B, C>> {
    return Operator { source ->
        Observable { subscriber ->
            var valueB: B? = null
            var valueC: C? = null
            val subscription = Subscription()
            subscription.add(obs1.subscribe(object : Observer<B> {
                override fun next(value: B) {
                    valueB = value
                }

                override fun error(error: Throwable) {
                    subscriber.error(error)
                }

                override fun complete() {
                    // Nothing to do
                }
            }))
            subscription.add(obs2.subscribe(object : Observer<C> {
                override fun next(value: C) {
                    valueC = value
                }

                override fun error(error: Throwable) {
                    subscriber.error(error)
                }

                override fun complete() {
                    // Nothing to do
                }
            }))
            subscription.add(source.subscribe(object : Observer<A> {
                override fun next(value: A) {
                    valueB?.let { valB ->
                        valueC?.let { valC ->
                            subscriber.next(Tuple(value, valB, valC))
                        }
                    }
                }

                override fun error(error: Throwable) {
                    subscriber.error(error)
                }

                override fun complete() {
                    subscriber.complete()
                }
            }))
            subscription.add(subscriber)
            subscription
        }
    }
}
