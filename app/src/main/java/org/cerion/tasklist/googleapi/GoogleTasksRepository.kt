package org.cerion.tasklist.googleapi

import org.cerion.tasklist.database.Task
import org.cerion.tasklist.database.TaskList
import java.util.*

class GoogleTasksRepository(private val api: GoogleApi) {

    private val lists = api.taskListsApi
    private val tasks = api.tasksApi

    fun getLists() = lists.getAll()
    fun getList(id: String) = lists.get(id)
    fun createList(list: TaskList): TaskList? = lists.insert(list)
    fun updateList(list: TaskList): Boolean = lists.update(list)
    fun deleteList(list: TaskList): Boolean = lists.delete(list)

    fun getTasks(listId: String, dtUpdatedMin: Date? = null) = tasks.list(listId, dtUpdatedMin)
    fun createTask(task: Task): Task? = tasks.insert(task)
    fun updateTask(task: Task): Boolean = tasks.update(task)
    fun deleteTask(task: Task): Boolean = tasks.delete(task)
}
