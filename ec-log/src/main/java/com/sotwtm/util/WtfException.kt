package com.sotwtm.util

/**
 * WTF exception. This exception throw when [Log.wtf] is being called on debug build.
 * @author sheungon
 * */
class WtfException : RuntimeException {

    /** {@inheritDoc} */
    constructor() : super()

    /** {@inheritDoc} */
    constructor(message: String?) : super(message)

    /** {@inheritDoc} */
    constructor(message: String?, cause: Throwable?) : super(message, cause)

    /** {@inheritDoc} */
    constructor(cause: Throwable?) : super(cause)

    /** {@inheritDoc} */
    constructor(message: String?, cause: Throwable?, enableSuppression: Boolean, writableStackTrace: Boolean) : super(
        message,
        cause,
        enableSuppression,
        writableStackTrace
    )
}