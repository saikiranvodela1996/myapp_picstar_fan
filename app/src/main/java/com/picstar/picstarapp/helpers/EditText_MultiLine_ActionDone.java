package com.picstar.picstarapp.helpers;

/**
 * Created by santhoshannam on 9/4/2015.
 */

import android.content.Context;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;

// An EditText that lets you use actions ("Done", "Go", etc.) on multi-line edits.
public class EditText_MultiLine_ActionDone extends androidx.appcompat.widget.AppCompatEditText {

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        InputConnection conn = super.onCreateInputConnection(outAttrs);
        outAttrs.imeOptions &= ~EditorInfo.IME_FLAG_NO_ENTER_ACTION;
        return conn;
    }

    public EditText_MultiLine_ActionDone(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public EditText_MultiLine_ActionDone(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditText_MultiLine_ActionDone(Context context) {
        super(context);
    }


}