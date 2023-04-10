package de.atennert.rx.operators

import de.atennert.rx.Observable
import de.atennert.rx.Observer
import de.atennert.rx.Operator

fun <X> filterNotNull(): Operator<X?, X> {
    return Operator { source ->
        Observable { subscriber ->
            source.subscribe(object : Observer<X?> {
                override fun next(value: X?) {
                    if (value != null) {
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
