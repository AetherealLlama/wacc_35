package wacc.utils

import kotlin.reflect.full.companionObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger

interface Logging

inline fun <T : Any> getClassForLogging(javaClass: Class<T>): Class<*> {
    return javaClass.enclosingClass?.takeIf {
        it.kotlin.companionObject?.java == javaClass
    } ?: javaClass
}

inline fun getRootLogger(): Logger = getLogger(Logger.ROOT_LOGGER_NAME)

inline fun <reified T : Logging> T.logger(): Logger = getLogger(getClassForLogging(this.javaClass))
