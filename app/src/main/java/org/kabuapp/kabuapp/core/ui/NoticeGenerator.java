package org.kabuapp.kabuapp.core.ui;

import android.content.Context;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.widget.TextView;

import org.kabuapp.kabuapp.R;

public class NoticeGenerator
{
    private NoticeGenerator()
    {
    }

    public static void setNotice(Context c, TextView myTextView)
    {
        String prefix = c.getString(R.string.notice_code);
        String linkText = c.getString(R.string.github_text);
        String url = c.getString(R.string.github_link);

        String fullText = prefix + " " + linkText + ".";

        SpannableString spannableString = new SpannableString(fullText);

        int linkStartIndex = fullText.indexOf(linkText);
        int linkEndIndex = linkStartIndex + linkText.length();

        spannableString.setSpan(new URLSpan(url), linkStartIndex, linkEndIndex, Spanned.SPAN_COMPOSING);

        myTextView.setText(spannableString);
        myTextView.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
