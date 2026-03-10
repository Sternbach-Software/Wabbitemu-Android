package io.github.angelsl.wabbitemu.fragment

import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.angelsl.wabbitemu.CalcKeyManager
import io.github.angelsl.wabbitemu.R
import io.github.angelsl.wabbitemu.WabbitLCD
import io.github.angelsl.wabbitemu.calc.CalculatorManager
import io.github.angelsl.wabbitemu.calc.variants.TI84PSELayout

class EmulatorButtonsFragment: Fragment(R.layout.emulator_grid_of_buttons) {
    private val keyManager = CalcKeyManager.getInstance()
    private val calcManager = CalculatorManager.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val layout = TI84PSELayout()
        val rowSize = layout.keys.first().size/*assume all rows have equal elements, so we can know that if rows are 5 long, item 3 is in row 1, and item 8 is in row 2*/

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
//        recyclerView.layoutManager = GridLayoutManager(requireContext(), 8, GridLayoutManager.VERTICAL, false)
        recyclerView.adapter = object: RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): RecyclerView.ViewHolder {
                return object : RecyclerView.ViewHolder(
                    Button(requireContext())
                ){}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                (holder.itemView as Button).apply {
                    val button = layout.keys[position / rowSize][position % rowSize]
                    visibility = if(button == null) View.INVISIBLE else View.VISIBLE
                    Log.d("EmulatorButtonsFragment", "Creating button: $button at position: $position")
                    text = button?.toString()
                    setOnTouchListener { v, event ->
                        v.performClick()
                        CalcKeyManager.KEY_MAPPINGS.find { it.key == button }?.let { mapping ->
                            button?.let { id ->
                                if(event.action == MotionEvent.ACTION_DOWN) {
                                    Log.d("EmulatorButtonsFragment", "Key down: $button (${mapping?.group}, ${mapping?.bit})")
                                    keyManager.doKeyDown(id, mapping.group, mapping.bit)
                                } else {
                                    Log.d("EmulatorButtonsFragment", "Key up: $button")
                                    keyManager.doKeyUp(id)
                                }
                                true
                            } ?: false
                        }
                            .also {
                                Log.d("EmulatorButtonsFragment", "Mapping: $it")
                            } == true
                    }
                }
            }

            override fun getItemCount(): Int {
                return layout.keys.size * rowSize
            }

        }
        val screen = view.findViewById<WabbitLCD?>(R.id.textureView)
        screen?.let { calcManager.setScreenCallback(screen) }
//        for(row in layout.keys) {
//            for(button in row) {
//
//            }
//        }
    }
}