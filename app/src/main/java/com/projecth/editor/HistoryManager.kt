package com.projecth.editor

class HistoryManager<T>(private val capacity: Int = 40) {
    private val undo = ArrayDeque<T>()
    private val redo = ArrayDeque<T>()

    fun push(state: T) {
        if (undo.size >= capacity) undo.removeFirst()
        undo.addLast(state)
        redo.clear()
    }

    fun canUndo() = undo.isNotEmpty()
    fun canRedo() = redo.isNotEmpty()

    fun undo(current: T): T? {
        if (undo.isEmpty()) return null
        redo.addLast(current)
        return undo.removeLast()
    }

    fun redo(current: T): T? {
        if (redo.isEmpty()) return null
        undo.addLast(current)
        return redo.removeLast()
    }

    fun clear() {
        undo.clear()
        redo.clear()
    }
}
