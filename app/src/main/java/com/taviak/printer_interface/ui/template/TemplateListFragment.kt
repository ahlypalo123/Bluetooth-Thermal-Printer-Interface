package com.taviak.printer_interface.ui.template

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.taviak.printer_interface.App
import com.taviak.printer_interface.R
import com.taviak.printer_interface.data.dao.ReceiptTemplateDao
import com.taviak.printer_interface.data.dao.VariableDao
import com.taviak.printer_interface.data.model.*
import com.taviak.printer_interface.util.ReceiptBuilder
import com.taviak.printer_interface.util.inflate
import kotlinx.android.synthetic.main.fragment_template.*
import kotlinx.android.synthetic.main.fragment_template.toolbar
import kotlinx.android.synthetic.main.item_image.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TemplateListFragment : Fragment() {

    private val dao: ReceiptTemplateDao = App.db.receiptTemplateDao()
    private val list: MutableList<ReceiptTemplate> = mutableListOf()
    private val adapter = Adapter()
    private var lastActive = -1
    private lateinit var updatedTemplate: ReceiptTemplate

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_template, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = activity as AppCompatActivity?
        activity?.setSupportActionBar(toolbar)
        activity?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        activity?.supportActionBar?.setDisplayShowTitleEnabled(false)
        setHasOptionsMenu(true)

        // pager_templates?.adapter =
        pager_templates?.adapter = adapter
        pager_templates?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val item = list[position]
                toggleButtonEnabled(!item.active)
                btn_edit?.setOnClickListener {
                    edit(item)
                }
                btn_apply?.setOnClickListener {
                    updateActive(item)
                }
            }
        })

        TabLayoutMediator(tab_templates, pager_templates) { tab, _ ->
            tab.icon = ContextCompat.getDrawable(requireContext(), R.drawable.shape_gray_circle)
        }.attach()

        btn_create?.setOnClickListener {
            create()
        }

        dao.getAllLiveData().observe(viewLifecycleOwner) {
            list.clear()
            list.addAll(it)
            btn_edit?.isEnabled = list.isNotEmpty()
            btn_apply?.isEnabled = list.isNotEmpty()
            adapter.notifyDataSetChanged()
        }
    }

    private fun toggleButtonEnabled(f: Boolean) {
        btn_apply?.isEnabled = f
        btn_apply?.text = if (f) "Использовать этот шаблон" else "Этот шаблон используется"
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            activity?.onBackPressed()
        }
        return true
    }

    private fun updateActive(item: ReceiptTemplate) = CoroutineScope(Dispatchers.IO).launch {
        val active = dao.getActive()
        active?.active = false
        active?.let {
            dao.save(it)
        }

        item.active = true
        dao.save(item)
        withContext(Dispatchers.Main) {
            toggleButtonEnabled(false)
        }
    }

    fun removeTemplate() = CoroutineScope(Dispatchers.IO).launch {
        dao.delete(updatedTemplate)
    }

    private fun create() {
        updatedTemplate = ReceiptTemplate()
        activity?.supportFragmentManager
            ?.beginTransaction()
            ?.setCustomAnimations(R.anim.slide_from_bottom, R.anim.fade_out, R.anim.fade_in, R.anim.slide_to_bottom)
            ?.replace(R.id.layout_activity_container, TemplateEditorFragment(), TemplateEditorFragment::class.simpleName)
            ?.addToBackStack(TemplateEditorFragment::class.simpleName)?.commit()
    }

    private fun edit(item: ReceiptTemplate) {
        updatedTemplate = item
        activity?.supportFragmentManager
            ?.beginTransaction()
            ?.setCustomAnimations(R.anim.slide_from_bottom, R.anim.fade_out, R.anim.fade_in, R.anim.slide_to_bottom)
            ?.replace(R.id.layout_activity_container, TemplateEditorFragment(item.data), TemplateEditorFragment::class.simpleName)
            ?.addToBackStack(TemplateEditorFragment::class.simpleName)?.commit()
    }

    fun saveTemplate(data: ReceiptTemplateData) = CoroutineScope(Dispatchers.IO).launch {
        updatedTemplate.data = data
        if (updatedTemplate.id == 0L) {
            dao.insert(updatedTemplate)
        } else {
            dao.save(updatedTemplate)
        }
    }

    inner class Adapter : RecyclerView.Adapter<Adapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(parent.inflate(R.layout.item_image))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            holder.bind(list[position])

        override fun getItemCount() = list.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind(item: ReceiptTemplate) {
                if (item.active) {
                    lastActive = adapterPosition
                }
                val bmp = ReceiptBuilder(context, null).build(item.data)
                itemView.image_check?.setImageBitmap(bmp)
            }

        }
    }

}