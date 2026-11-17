package com.example.divineroster.ui.dashboard

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.divineroster.R
import com.example.divineroster.databinding.ItemCalendarDayBinding

data class CalendarDay(
    val dayOfMonth: Int?,
    val hasEvent: Boolean,
    val isToday: Boolean
)

class CalendarDayAdapter : RecyclerView.Adapter<CalendarDayAdapter.ViewHolder>() {

    private var days: List<CalendarDay> = emptyList()

    fun submitDays(newDays: List<CalendarDay>) {
        days = newDays
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCalendarDayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(days[position])
    }

    override fun getItemCount(): Int = days.size

    class ViewHolder(private val binding: ItemCalendarDayBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(day: CalendarDay) {
            if (day.dayOfMonth == null) {
                binding.tvDayNumber.text = ""
                binding.dotEventIndicator.visibility = View.GONE
                return
            }

            binding.tvDayNumber.text = day.dayOfMonth.toString()
            if (day.isToday) {
                binding.tvDayNumber.setBackgroundResource(R.drawable.bg_calendar_today)
                binding.tvDayNumber.setTextColor(0xFFFFFFFF.toInt())
            } else {
                binding.tvDayNumber.background = null
                binding.tvDayNumber.setTextColor(resolveThemeColor(android.R.attr.textColorPrimary))
            }
            binding.dotEventIndicator.visibility = if (day.hasEvent) View.VISIBLE else View.GONE
        }

        private fun resolveThemeColor(attr: Int): Int {
            val typedValue = TypedValue()
            binding.root.context.theme.resolveAttribute(attr, typedValue, true)
            return typedValue.data
        }
    }
}
