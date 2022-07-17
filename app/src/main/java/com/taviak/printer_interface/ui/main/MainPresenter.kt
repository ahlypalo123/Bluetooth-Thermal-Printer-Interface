package com.taviak.printer_interface.ui.main

import androidx.lifecycle.LifecycleOwner
import com.taviak.printer_interface.App
import com.taviak.printer_interface.R
import com.taviak.printer_interface.data.dao.ReceiptTemplateDao
import com.taviak.printer_interface.data.dao.VariableDao
import com.taviak.printer_interface.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class MainPresenter(
    private val view: MainView,
    private val owner: LifecycleOwner
) {

    var variables: MutableList<Variable> = mutableListOf() // fields
    var lists: MutableList<ReceiptListElement> = mutableListOf()
    var pictures: MutableList<ReceiptImageElement> = mutableListOf()
    private val variableDao: VariableDao = App.db.variableDao()
    private val dao: ReceiptTemplateDao = App.db.receiptTemplateDao()
    private val mutex = Mutex()
    var template: ReceiptTemplate? = null

    fun init() {
        dao.getActiveLiveData().observe(owner) {
            template = it
            updateTemplate(it)
        }
    }

    private fun updateTemplate(template: ReceiptTemplate?) = CoroutineScope(Dispatchers.IO).launch {
        mutex.withLock {
            variables.clear()
            lists.clear()
            pictures.clear()
            template?.data?.flatten()?.forEach { el ->
                when (el) {
                    is ReceiptListElement -> {
                        lists.add(el)
                    }
                    is ReceiptTextElement -> {
                        variables.addAll(extractVariables(el))
                    }
                    is ReceiptImageElement -> {
                        if (el.fileName == null) {
                            pictures.add(el)
                        }
                    }
                }
            }
            variables.sortBy { !it.expression.isNullOrBlank() }
            withContext(Dispatchers.Main) {
                view.onFieldsUpdated()
            }
        }
    }

    private suspend fun extractVariables(el: ReceiptElement) : Set<Variable> {
        val res = mutableSetOf<Variable>()
        if (el !is ReceiptTextElement) {
            return res
        }

        var startFrom = 0
        while (true) {
            val ind1 = el.text.indexOf('{', startIndex = startFrom)
            if (ind1 == -1) {
                return res
            }
            val ind2 = el.text.indexOf('}', startIndex = startFrom)
            startFrom = ind2 + 1
            val name = el.text.substring(ind1 + 1, ind2)
            variableDao.findByShortName(name)?.let {
                res.add(it)
            }
        }
    }

    fun onAddItem(el: ReceiptListElement) = CoroutineScope(Dispatchers.IO).launch {
        val itemData = el.data.flatten()
            .map { extractVariables(it) }.flatten()
            .filter { it.scope == VariableScope.ITEM.ordinal }
        withContext(Dispatchers.Main) {
            view.navigateToItem(itemData)
        }
    }

}