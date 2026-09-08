package org.kabuapp.kabuapp.core.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.Log;
import android.util.TypedValue;

public class ThemeColorResolver
{
    private ThemeColorResolver()
    {
    }

    public static int resolveColorAttribute(Context context, int themeAttributeId)
    {
        TypedValue typedValue = new TypedValue();
        if (context.getTheme().resolveAttribute(themeAttributeId, typedValue, true))
        {
            if (typedValue.type >= TypedValue.TYPE_FIRST_INT && typedValue.type <= TypedValue.TYPE_LAST_INT)
            {
                return typedValue.data;
            }
            else if (typedValue.type == TypedValue.TYPE_STRING)
            {
                try
                {
                    return context.getResources().getColorStateList(typedValue.resourceId, context.getTheme()).getDefaultColor();
                }
                catch (Resources.NotFoundException e1)
                {
                    try
                    {
                        return context.getResources().getColor(typedValue.resourceId, context.getTheme());
                    }
                    catch (Resources.NotFoundException e2)
                    {
                        Log.e("ThemeColorResolver", "Resource not for Attribute not found: " +
                                context.getResources().getResourceEntryName(themeAttributeId), e2);
                        return Color.BLACK;
                    }
                }
            }
        }
        Log.w("ThemeColorResolver", "Not able so resolve theme Attribute: " + context.getResources().getResourceEntryName(themeAttributeId));
        return Color.BLACK;
    }
}
