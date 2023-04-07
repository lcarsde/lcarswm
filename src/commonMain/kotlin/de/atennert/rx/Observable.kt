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
        fun <T> of(vararg elems: T): Observable<T> {
            return Observable { subscriber ->
                elems.forEach { subscriber.next(it) }
                subscriber.complete()
                Subscription { subscriber.unsubscribe() }
            }
        }
    }
}