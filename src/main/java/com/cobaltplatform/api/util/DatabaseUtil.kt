package com.cobaltplatform.api.util

import com.pyranid.Database
import kotlin.reflect.KClass

/**
 * @author Transmogrify LLC.
 */

fun <T : Any> Database.queryForObject(sql: String,
                                      klass: KClass<T>,
                                      vararg parameters: Any): T? {
    return queryForObject(sql, klass.java, *parameters).orElse(null)
}

// TODO when passing lists, they list needs to be manually flattened,
//  such as -- listOfIds passed in as *listOfIds.toTypedArray()
fun <T : Any> Database.queryForList(sql: String,
                                    klass: KClass<T>,
                                    vararg parameters: Any): List<T> {
    val dbResults = queryForList(sql, klass.java, *parameters)
    return dbResults
}

fun <T : Any> Database.queryForList(sql: String,
                                    klass: KClass<T>): List<T> {
    val dbResults = queryForList(sql, klass.java)
    return dbResults
}


fun <T> Collection<T>.sqlInPlaceholders(): String {
    return " (${joinToString { "?" }})"
}

fun <T> Array<T>.sqlInPlaceholders(): String {
    return " (${joinToString { "?" }})"
}
