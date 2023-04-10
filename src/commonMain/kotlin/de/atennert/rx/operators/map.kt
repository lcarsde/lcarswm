package de.atennert.rx.operators

import de.atennert.rx.Observable
import de.atennert.rx.Observer
import de.atennert.rx.Operator

fun <X,Y> map(f: (X) -> Y): Operator<X, Y> {
    return Operator { source ->
        Observable { subscriber ->
            source.subscribe(object : Observer<X> {
                override fun next(value: X) {
                    subscriber.next(f(value))
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
