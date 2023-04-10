package de.atennert.rx.operators

import de.atennert.rx.Observable
import de.atennert.rx.Observer
import de.atennert.rx.Operator

fun <X> filter(f: (X) -> Boolean): Operator<X, X> {
    return Operator { source ->
        Observable { subscriber ->
            source.subscribe(object : Observer<X> {
                override fun next(value: X) {
                    if (f(value)) {
                        subscriber.next(value)
                    }
                }

                override fun error(error: Throwable) {
                    subscriber.error(error)
                }

                override fun complete() {
                    subscriber.complete()
                }
            })
        }
    }
}
