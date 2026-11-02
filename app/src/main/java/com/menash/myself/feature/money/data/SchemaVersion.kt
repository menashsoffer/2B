package com.menash.myself.feature.money.data

/**
 * Every money document the app writes carries `"v": [CURRENT]`. Documents from before the field existed
 * have no "v" and read as version 1. A document from a newer app version is skipped, never half-read:
 * [check] throws [NewerSchemaException], which the repositories already catch, report and drop.
 */
object SchemaVersion {
    const val KEY = "v"
    const val CURRENT = 1L

    fun of(data: Map<String, Any?>): Long = (data[KEY] as? Number)?.toLong() ?: 1L

    fun check(data: Map<String, Any?>) {
        val version = of(data)
        if (version > CURRENT) throw NewerSchemaException(version)
    }

    fun stamp(fields: Map<String, Any>): Map<String, Any> = fields + (KEY to CURRENT)
}

class NewerSchemaException(version: Long) :
    IllegalArgumentException("Document schema v$version is newer than this app (v${SchemaVersion.CURRENT})")
