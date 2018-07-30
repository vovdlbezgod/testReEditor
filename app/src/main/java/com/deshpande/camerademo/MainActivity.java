package com.deshpande.camerademo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;
import android.media.ResourceBusyException;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.*;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private Button takePictureButton;
    private Button setMaskBtn;
    private Button getObjBtn;
    private Button slctMaskBtn;
    private ImageView imageView;
    private Uri file;
    private Bitmap localBitmap;
    private Bitmap maskBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        takePictureButton = (Button) findViewById(R.id.button_image);
        setMaskBtn = (Button) findViewById(R.id.buttonSet);
        getObjBtn = (Button) findViewById(R.id.buttonGet);
        slctMaskBtn = (Button) findViewById(R.id.slctMsk);
        imageView = (ImageView) findViewById(R.id.imageview);
        localBitmap = BitmapFactory.decodeResource(
                getApplicationContext().getResources(),
                R.drawable.photo);
        maskBitmap = BitmapFactory.decodeResource(
                getApplicationContext().getResources(),
                R.drawable.mask);
        Log.d("myLog","Height="+maskBitmap.getHeight() + " Width=" + maskBitmap.getWidth());
        Log.d("myLog","Height="+localBitmap.getHeight() + " Width=" + localBitmap.getWidth());
        imageView.setImageBitmap(localBitmap);
        OnClickListener setMskOncl = new OnClickListener() {
            @Override
            public void onClick(View view) {
                imageView.setImageBitmap(deletingMaskFromSource(localBitmap, maskBitmap));
            }
        };
        setMaskBtn.setOnClickListener(setMskOncl);
        OnClickListener getMskOncl = new OnClickListener() {
            @Override
            public void onClick(View view) {
                imageView.setImageBitmap(getObjectByMask(maskBitmap));
            }
        };
        getObjBtn.setOnClickListener(getMskOncl);

        OnClickListener slctMskOncl = new OnClickListener() {
            @Override
            public void onClick(View view) {
                imageView.setImageBitmap(seletingObjectOnMask(maskBitmap, Color.GREEN));
            }
        };
        slctMaskBtn.setOnClickListener(slctMskOncl);

        //returnBackWithSavedImage();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            takePictureButton.setEnabled(false);
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE }, 0);
        }
    }
    //вырезает объект для заполнения фоном
    private Bitmap deletingMaskFromSource(Bitmap src, Bitmap mask){
        if (mask != null) {
            int picw = src.getWidth();
            int pich = src.getHeight();
            int[] pixSrc = new int[picw * pich];
            int[] pix = new int[picw * pich];
            mask.getPixels(pix, 0, picw, 0, 0, picw, pich);
            src.getPixels(pixSrc, 0, picw, 0, 0, picw, pich);

            for (int y = 0; y < pich; y++) {
                // from left to right
                for (int x = 0; x < picw; x++) {
                    int index = y * picw + x;
                    if (pix[index] == Color.BLACK) {
                        pix[index] = pixSrc[index];
                    } else {
                        break;
                    }
                }

                // from right to left
                for (int x = picw - 1; x >= 0; x--) {
                    int index = y * picw + x;
                    if (pix[index] == Color.BLACK) {
                        pix[index] = pixSrc[index];
                    } else {
                        break;
                    }
                }
            }

            Bitmap bm = Bitmap.createBitmap(pix, picw, pich,
                    Bitmap.Config.RGB_565);
            return bm;
        }
        return null;
    }
    //выделяет объекты
    private Bitmap seletingObjectOnMask(Bitmap mask, int color){
        //mask = createTransparentBitmapFromBitmap(mask, Color.BLACK);
        if (mask != null) {
            Bitmap result = Bitmap.createBitmap(mask.getWidth(), mask.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas tempCanvas = new Canvas(result);


            Bitmap colored = Bitmap.createBitmap(mask.getWidth(), mask.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvasColored = new Canvas(colored);
            canvasColored.drawColor(color);
            Paint paintColored = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintColored.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
            canvasColored.drawBitmap(colored, 0, 0, null);
            Bitmap tempMask = createTransparentBitmapFromBitmap(mask, Color.BLACK);
            canvasColored.drawBitmap(tempMask, 0, 0, paintColored);


            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DARKEN));
            tempCanvas.drawBitmap(localBitmap, 0, 0, null);
            tempCanvas.drawBitmap(colored, 0, 0, paint);
            paint.setXfermode(null);
            return result;
        }
        return null;
    }
    //получает объект вырезанный по маске
    private Bitmap getObjectByMask(Bitmap mask){
        //mask = createTransparentBitmapFromBitmap(mask, Color.BLACK);
        if (mask != null) {
            Bitmap result = Bitmap.createBitmap(mask.getWidth(), mask.getHeight(), Bitmap.Config.ARGB_4444);
            Canvas tempCanvas = new Canvas(result);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
            tempCanvas.drawBitmap(localBitmap, 0, 0, null);
            mask = createTransparentBitmapFromBitmap(mask, Color.BLACK);//заменяем на маске Color.BLACK на прозрачный
            tempCanvas.drawBitmap(mask, 0, 0, paint);
            paint.setXfermode(null);
            return result;
        }
        return null;
    }
    //заменяет выбранный цвет на прозрачный
    public static Bitmap createTransparentBitmapFromBitmap(Bitmap bitmap, int replaceThisColor) {
        if (bitmap != null) {
            int picw = bitmap.getWidth();
            int pich = bitmap.getHeight();
            int[] pix = new int[picw * pich];
            bitmap.getPixels(pix, 0, picw, 0, 0, picw, pich);

            for (int y = pich - 1; y >= 0; y--) {
                // from left to right
                for (int x = 0; x < picw; x++) {
                    int index = y * picw + x;
                    int r = (pix[index] >> 16) & 0xff;
                    int g = (pix[index] >> 8) & 0xff;
                    int b = pix[index] & 0xff;

                    if (pix[index] == replaceThisColor) {
                        pix[index] = Color.TRANSPARENT;
                    } else {
                        break;
                    }
                }

                // from right to left
                for (int x = picw - 1; x >= 0; x--) {
                    int index = y * picw + x;
                    int r = (pix[index] >> 16) & 0xff;
                    int g = (pix[index] >> 8) & 0xff;
                    int b = pix[index] & 0xff;

                    if (pix[index] == replaceThisColor) {
                        pix[index] = Color.TRANSPARENT;
                    } else {
                        break;
                    }
                }
            }

            Bitmap bm = Bitmap.createBitmap(pix, picw, pich,
                    Bitmap.Config.ARGB_4444);

            return bm;
        }
        return null;
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 0) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                takePictureButton.setEnabled(true);
            }
        }
    }

    public void takePicture(View view) {
        /*Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        file = Uri.fromFile(getOutputMediaFile());
        Toast.makeText(this, "Saved at: " + file.getPath().toString(), Toast.LENGTH_LONG).show();
        intent.putExtra(MediaStore.EXTRA_OUTPUT, file);

        startActivityForResult(intent, 100);*/
        returnBackWithSavedImage();
    }

    private static File getOutputMediaFile(){
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "CameraDemo");

        if (!mediaStorageDir.exists()){
            if (!mediaStorageDir.mkdirs()){
                Log.d("CameraDemo", "failed to create directory");
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
    }

        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == 100) {
                if (resultCode == RESULT_OK) {
                    imageView.setImageURI(file);
                }
            }
        }


    public void returnBackWithSavedImage() {
        new CountDownTimer(1000, 500) {
            public void onTick(long millisUntilFinished) {

            }

            public void onFinish() {
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String imageName = "IMG_" + timeStamp + ".jpg";
                Intent returnIntent = new Intent();
                returnIntent.putExtra("imagePath", saveImage("CameraDemo", imageName));
                Log.d("myLog", "setResult");
                setResult(Activity.RESULT_OK, returnIntent);
                Log.d("myLog", "after setResult");
                //finish();
            }
        }.start();
    }

    public String saveImage(String folderName, String imageName) {
        String selectedOutputPath = "";
        if (isSDCARDMounted()) {
            File mediaStorageDir = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), folderName);
            // Create a storage directory if it does not exist
            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    Log.d("PhotoEditorSDK", "Failed to create directory");
                }
            }
            // Create a media file name
            selectedOutputPath = mediaStorageDir.getPath() + File.separator + imageName;
            Log.d("PhotoEditorSDK", "selected camera path " + selectedOutputPath);
            File file = new File(selectedOutputPath);
            try {
                FileOutputStream out = new FileOutputStream(file);
                if (imageView != null) {
                    imageView.setDrawingCacheEnabled(true);
                    imageView.getDrawingCache().compress(Bitmap.CompressFormat.PNG, 80, out);
                }
                out.flush();
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return selectedOutputPath;
    }

    private boolean isSDCARDMounted() {
        String status = Environment.getExternalStorageState();
        return status.equals(Environment.MEDIA_MOUNTED);
    }

}
