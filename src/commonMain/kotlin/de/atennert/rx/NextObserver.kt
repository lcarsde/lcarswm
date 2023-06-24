package de.atennert.rx

class NextObserver<T>(
    private val nextHandler: Handler<T>,
    private val errorHandler: Handler<Throwable>,
    private val completeHandler: Handler<Unit>
): Observer<T> {
    constructor(nextHandler: Handler<T>, errorHandler: Handler<Throwable>) : this(nextHandler, errorHandler, {})

    constructor(nextHandler: Handler<T>) : this(nextHandler, {}, {})

    override fun next(value: T) {
        nextHandler.handle(value)
    }

    override fun error(error: Throwable) {
        errorHandler.handle(error)
    }

    override fun complete() {
        completeHandler.handle(Unit)
    }

    fun interface Handler<T> {
        fun handle(value: T)
    }
}