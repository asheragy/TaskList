package org.cerion.tasklist.data

import java.util.*

//Error from tasks API, json encoded with error code and message
class GoogleApiException(message: String, val errorCode: Int) : Exception("Error $errorCode: $message")

interface GoogleTasklistsApi {
    @Throws(GoogleApiException::class)
    fun get(id: String): TaskList?

    @Throws(GoogleApiException::class)
    fun getAll(): List<TaskList>?

    @Throws(GoogleApiException::class)
    fun insert(list: TaskList): TaskList?

    @Throws(GoogleApiException::class)
    fun update(list: TaskList): Boolean
}

interface GoogleTasksApi {
    fun delete(task: Task): Boolean

    @Throws(GoogleApiException::class)
    fun list(listId: String, dtUpdatedMin: Date?): List<Task>?

    @Throws(GoogleApiException::class)
    fun insert(task: Task): Task?

    @Throws(GoogleApiException::class)
    fun update(task: Task): Boolean
}
