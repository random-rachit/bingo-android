package com.cafedroid.bingo_android

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView

class CursiveTextView(context: Context, attrs: AttributeSet? = null) :
    AppCompatTextView(context, attrs) {
    init { this.setCustomFont(context) }
}

class CursiveEditText(context: Context, attrs: AttributeSet? = null) :
    AppCompatEditText(context, attrs) {
    init { this.setCustomFont(context) }
}