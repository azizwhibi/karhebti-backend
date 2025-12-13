package com.example.karhebti_android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.karhebti_android.repository.BreakdownsRepository

class BreakdownViewModelFactory(
    private val repo: BreakdownsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BreakdownViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BreakdownViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

