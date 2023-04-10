package de.atennert.rx

fun interface Subscribe<T> {
    fun subscribe(subscriber: Subscriber<T>): Subscription
}

open class Observable<T>(private val subscribeFn: Subscribe<T>) {

    fun <R> apply(operator: Operator<T, R>): Observable<R> {
        return operator.call(this)
    }

    fun subscribe(observer: Observer<T>): Subscription {
        return subscribeFn.subscribe(Subscriber(observer))
    }

    companion object {
        private val EMPTY: Observable<Any?> = Observable { subscriber ->
            subscriber.complete()
            Subscription { subscriber.unsubscribe() }
        }

        @Suppress("UNCHECKED_CAST")
        fun <T> empty(): Observable<T> = EMPTY as Observable<T>

        fun <T> error(error: Throwable = Error()) = Observable<T> { subscriber ->
            subscriber.error(error)
            Subscription { subscriber.unsubscribe() }
        }

        fun <T> of(vararg elems: T): Observable<T> {
            return Observable { subscriber ->
                elems.forEach { subscriber.next(it) }
                subscriber.complete()
                Subscription { subscriber.unsubscribe() }
            }
        }
    }
}