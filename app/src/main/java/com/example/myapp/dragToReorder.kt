package com.example.myapp

import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import com.example.myapp.model.Block
import com.example.myapp.model.SlideState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.lang.IndexOutOfBoundsException
import kotlin.math.roundToInt
import kotlin.math.sign

//https://developersbreach.com/drag-to-reorder-compose/

fun Modifier.dragToReorder(
    block: Block,
    blocksList: MutableList<Block>,
    itemHeight: Int,
    updateSlideState: (blockList: Block, slideState: SlideState) -> Unit,
    isDraggedAfterLongPress: Boolean,
    onStartDrag: () -> Unit,
    onStopDrag: (currentIndex: Int, destinationIndex: Int) -> Unit,
): Modifier = composed {
//    val vibrator = LocalContext.current.getSystemService(Vibrator::class.java)
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    pointerInput(Unit) {
        // Wrap in a coroutine scope to use suspend functions for touch events and animation.
        coroutineScope {
            val blockIndex = blocksList.indexOf(block)
            val offsetToSlide = itemHeight / 4
            var numberOfItems = 0
            var previousNumberOfItems: Int
            var listOffset = 0

            val onDragStart = {
//                vibrator.vibrate(VibrationEffect.createOneShot(50, 10))
                // Interrupt any ongoing animation of other items.
                launch {
                    offsetX.stop()
                    offsetY.stop()
                }
                onStartDrag()
            }
            val onDrag = { change: PointerInputChange ->
                val horizontalDragOffset = offsetX.value + change.positionChange().x
                launch {
                    offsetX.snapTo(horizontalDragOffset)
                }
                val verticalDragOffset = offsetY.value + change.positionChange().y
                launch {
                    offsetY.snapTo(verticalDragOffset)
                    val offsetSign = offsetY.value.sign.toInt()
                    previousNumberOfItems = numberOfItems
                    numberOfItems = calculateNumberOfSlidItems(
                        offsetY.value * offsetSign,
                        itemHeight,
                        offsetToSlide,
                        previousNumberOfItems
                    )
                    Log.i("DragToReorder", "numberOfItems: $numberOfItems")

                    if (previousNumberOfItems > numberOfItems) {
                        Log.i(
                            "DragToReorder",
                            "previousNumberOfItems $previousNumberOfItems > numberOfItems $numberOfItems"
                        )
                        updateSlideState(
                            blocksList[blockIndex + previousNumberOfItems * offsetSign],
                            SlideState.NONE
                        )
                    } else if (numberOfItems != 0) {
                        try {
                            Log.i(
                                "DragToReorder",
                                "Item is inside, numberOfItems: $numberOfItems, offsetSign: $offsetSign, blockIndex: $blockIndex, ind: ${blockIndex + numberOfItems * offsetSign}"
                            )
                            updateSlideState(
                                blocksList[blockIndex + numberOfItems * offsetSign],
                                if (offsetSign == 1) {
                                    SlideState.UP
                                } else {
                                    SlideState.DOWN
                                }
                            )
                        } catch (e: IndexOutOfBoundsException) {
                            numberOfItems = previousNumberOfItems
                            Log.i(
                                "DragToReorder",
                                "Item is outside or at the edge, numberOfItems: $numberOfItems"
                            )
                        }
                    }
                    listOffset = numberOfItems * offsetSign
                }
                // Consume the gesture event, not passed to external
                change.consumePositionChange()
            }
            val onDragEnd = {
                launch {
                    offsetX.animateTo(0f)
                }
                launch {
                    Log.i("LOG", "DragEnd")
//                    vibrator.vibrate(VibrationEffect.createOneShot(50, 10))
                    offsetY.animateTo(itemHeight * numberOfItems * offsetY.value.sign)
                    onStopDrag(blockIndex, blockIndex + listOffset)
                    Log.i("LOG", "DragEnd end")
                }
            }
            if (isDraggedAfterLongPress)
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDrag = { change, _ -> onDrag(change) },
                    onDragEnd = { onDragEnd() }
                ) else
                while (true) {
                    val pointerId = awaitPointerEventScope { awaitFirstDown().id }
                    awaitPointerEventScope {
                        drag(pointerId) { change ->
                            onDragStart()
                            onDrag(change)
                        }
                    }
                    onDragEnd()
                }
        }
    }
        .offset {
            // Use the animating offset value here.
            IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt())
        }
}


private fun calculateNumberOfSlidItems(
    offsetY: Float,
    itemHeight: Int,
    offsetToSlide: Int,
    previousNumberOfItems: Int
): Int {
    val numberOfItemsInOffset = (offsetY / itemHeight).toInt()
    val numberOfItemsPlusOffset = ((offsetY + offsetToSlide) / itemHeight).toInt()
    val numberOfItemsMinusOffset = ((offsetY - offsetToSlide - 1) / itemHeight).toInt()
    return when {
        offsetY - offsetToSlide - 1 < 0 -> 0
        numberOfItemsPlusOffset > numberOfItemsInOffset -> numberOfItemsPlusOffset
        numberOfItemsMinusOffset < numberOfItemsInOffset -> numberOfItemsInOffset
        else -> previousNumberOfItems
    }
}