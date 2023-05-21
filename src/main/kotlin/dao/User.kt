package dao

import org.jetbrains.exposed.sql.Table

object Users : Table() {
    val id = varchar("id", 18).uniqueIndex()
    val numberOfMessages = integer("message")
}