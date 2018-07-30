package com.deshpande.camerademo;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

public abstract class MaskedDrawable extends Drawable {
    public abstract void setPictureBitmap(Bitmap pictureBitmap);

    public abstract void setMaskBitmap(Bitmap maskBitmap);

    public interface MaskedDrawableFactory {
        MaskedDrawable createMaskedDrawable();
    }
}
