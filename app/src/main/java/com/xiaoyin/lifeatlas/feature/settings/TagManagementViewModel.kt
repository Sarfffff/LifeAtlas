package com.xiaoyin.lifeatlas.feature.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoyin.lifeatlas.core.model.Tag
import com.xiaoyin.lifeatlas.data.repository.RepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TagManagementUiState(
    val tags: List<Tag> = emptyList(),
    val editingTag: Tag? = null,
    val editingName: String = "",
    val editingColor: String? = null,
    val deletingTag: Tag? = null,
    val errorMessage: String? = null
)

class TagManagementViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RepositoryProvider.memoryRepository(application)
    private val editorState = MutableStateFlow(TagEditorState())

    val uiState: StateFlow<TagManagementUiState> = combine(
        repository.observeAllTags(),
        editorState
    ) { tags, editor ->
        TagManagementUiState(
            tags = tags,
            editingTag = editor.editingTag,
            editingName = editor.editingName,
            editingColor = editor.editingColor,
            deletingTag = editor.deletingTag,
            errorMessage = editor.errorMessage
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TagManagementUiState()
        )

    fun startEdit(tag: Tag) {
        editorState.update {
            it.copy(
                editingTag = tag,
                editingName = tag.name,
                editingColor = tag.color,
                deletingTag = null,
                errorMessage = null
            )
        }
    }

    fun onEditingNameChange(value: String) {
        editorState.update { it.copy(editingName = value, errorMessage = null) }
    }

    fun onEditingColorChange(value: String?) {
        editorState.update { it.copy(editingColor = value, errorMessage = null) }
    }

    fun saveEdit() {
        val state = editorState.value
        val tag = state.editingTag ?: return

        viewModelScope.launch {
            runCatching {
                repository.renameTag(tag.id, state.editingName)
                repository.updateTagColor(tag.id, state.editingColor)
            }.onSuccess {
                editorState.update { TagEditorState() }
            }.onFailure { error ->
                editorState.update {
                    it.copy(errorMessage = error.message ?: "保存标签失败")
                }
            }
        }
    }

    fun cancelEdit() {
        editorState.update { it.copy(editingTag = null, editingName = "", editingColor = null, errorMessage = null) }
    }

    fun requestDelete(tag: Tag) {
        editorState.update { it.copy(deletingTag = tag, editingTag = null, errorMessage = null) }
    }

    fun confirmDelete() {
        val tag = editorState.value.deletingTag ?: return

        viewModelScope.launch {
            runCatching {
                repository.deleteTag(tag.id)
            }.onSuccess {
                editorState.update { TagEditorState() }
            }.onFailure { error ->
                editorState.update {
                    it.copy(deletingTag = null, errorMessage = error.message ?: "删除标签失败")
                }
            }
        }
    }

    fun cancelDelete() {
        editorState.update { it.copy(deletingTag = null, errorMessage = null) }
    }
}

private data class TagEditorState(
    val editingTag: Tag? = null,
    val editingName: String = "",
    val editingColor: String? = null,
    val deletingTag: Tag? = null,
    val errorMessage: String? = null
)
