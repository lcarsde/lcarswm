package de.atennert.rx.operators

import de.atennert.rx.*

fun <X> shareReplay(replayCount: Int = 1, refCount: Boolean = false): Operator<X, X> {
    val values = mutableListOf<X>()
    var isComplete = false
    var error: Throwable? = null
    val subscribers = mutableSetOf<Subscriber<X>>()

    var sourceSubscription: Subscription? = null

    fun addValue(value: X) {
        values.add(value)
        if (values.size > replayCount) {
            values.removeFirst()
        }
    }

    fun unsubscribeAll() {
        subscribers.forEach { it.unsubscribe() }
        subscribers.clear()

        sourceSubscription?.unsubscribe()
        sourceSubscription = null
    }

    return Operator { source ->
        Observable { subscriber ->
            subscribers.add(subscriber)

            sourceSubscription?.run {
                val err = error
                if (err != null) {
                    subscriber.error(err)
                } else {
                    values.forEach { subscriber.next(it) }
                    if (isComplete) {
                        subscriber.complete()
                    }
                }
            }

            if (sourceSubscription == null) {
                sourceSubscription = source.subscribe(object : Observer<X> {
                    override fun next(value: X) {
                        addValue(value)
                        subscribers.forEach { it.next(value) }
                    }

                    override fun error(err: Throwable) {
                        error = err
                        subscribers.forEach { it.error(err) }
                        unsubscribeAll()
                    }

                    override fun complete() {
                        isComplete = true
                        subscribers.forEach { it.complete() }
                        unsubscribeAll()
                    }
                })
            }

            Subscription {
                subscriber.unsubscribe()
                subscribers.remove(subscriber)

                if (refCount && subscribers.size < 1) {
                    sourceSubscription?.unsubscribe()
                    sourceSubscription = null
                    values.clear()
                }
            }
        }
    }
}
