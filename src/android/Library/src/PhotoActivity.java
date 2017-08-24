package com.synconset;

import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.io.Serializable;
import com.synconset.FakeR;


public class PhotoActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>, ViewPager.OnPageChangeListener{
    public static final String MAX_IMAGES_KEY = "MAX_IMAGES";
    public static final String WIDTH_KEY = "WIDTH";
    public static final String HEIGHT_KEY = "HEIGHT";
    public static final String QUALITY_KEY = "QUALITY";
    public static final String OUTPUT_TYPE_KEY = "OUTPUT_TYPE";

    private static final int CURSORLOADER_THUMBS = 0;
    private static final int CURSORLOADER_REAL = 1;
    private Cursor imagecursor, actualimagecursor;
    private int image_column_index, image_column_orientation, actual_image_column_index, orientation_column_index;
    private FakeR fakeR;
    private Map<String, Integer> fileNames = new HashMap<String, Integer>();
    private TextView tv_title;
    private TextView tv_sure;
    private ArrayList<String> imgs = new ArrayList<String>();
    private ProgressDialog progress;

    private int desiredWidth;
    private int desiredHeight;
    private int quality;
    private OutputType outputType;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        fileNames = (Map<String, Integer>) intent.getSerializableExtra("fileNames");
        desiredWidth = getIntent().getIntExtra(WIDTH_KEY, 0);
        desiredHeight = getIntent().getIntExtra(HEIGHT_KEY, 0);
        outputType = OutputType.fromValue(intent.getIntExtra(OUTPUT_TYPE_KEY, 0));
        quality = getIntent().getIntExtra(QUALITY_KEY, 0);

        for (Map.Entry<String, Integer> item : fileNames.entrySet()){
            imgs.add(item.getKey());
        }

        fakeR = new FakeR(this);
        setContentView(fakeR.getId("layout", "activity_photo"));
        ViewPager mVpPager = (ViewPager) findViewById(fakeR.getId("id", "vp_pager"));
        mVpPager.addOnPageChangeListener(this);
        mPagerAdapter adapter = new mPagerAdapter();
        mVpPager.setAdapter(adapter);

        ImageView iv_back = (ImageView) findViewById(fakeR.getId("id", "iv_back"));
        iv_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        tv_title = (TextView) findViewById(fakeR.getId("id", "tv_indicator"));
        tv_sure = (TextView) findViewById(fakeR.getId("id", "tv_sure"));
        tv_sure.setText("确定"+imgs.size());
        tv_sure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progress.show();

                if (fileNames.isEmpty()) {
                    setResult(RESULT_CANCELED);
                    progress.dismiss();
                    finish();
                } else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR); //prevent orientation changes during processing
                    new ResizeImagesTask().execute(fileNames.entrySet());
                }
            }
        });

        LoaderManager.enableDebugLogging(false);
        getLoaderManager().initLoader(CURSORLOADER_THUMBS, null, this);
        getLoaderManager().initLoader(CURSORLOADER_REAL, null, this);

        progress = new ProgressDialog(this);
        progress.setTitle(getString(fakeR.getId("string", "multi_image_picker_processing_images_title")));
        progress.setMessage(getString(fakeR.getId("string", "multi_image_picker_processing_images_message")));
    }

    @Override
    public Loader<Cursor> onCreateLoader(int cursorID, Bundle args) {
        ArrayList<String> img = new ArrayList<String>();
        switch (cursorID) {
            case CURSORLOADER_THUMBS:
                img.add(MediaStore.Images.Media._ID);
                img.add(MediaStore.Images.Media.ORIENTATION);
                break;

            case CURSORLOADER_REAL:
                img.add(MediaStore.Images.Thumbnails.DATA);
                img.add(MediaStore.Images.Media.ORIENTATION);
                break;
        }

        return new CursorLoader(
                this,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                img.toArray(new String[img.size()]),
                null,
                null,
                "DATE_MODIFIED DESC"
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor == null) {
            // NULL cursor. This usually means there's no image database yet....
            return;
        }

        switch (loader.getId()) {
            case CURSORLOADER_THUMBS:
                imagecursor = cursor;
                image_column_index = imagecursor.getColumnIndex(MediaStore.Images.Media._ID);
                image_column_orientation = imagecursor.getColumnIndex(MediaStore.Images.Media.ORIENTATION);
//                ia.notifyDataSetChanged();
                break;

            case CURSORLOADER_REAL:
                actualimagecursor = cursor;
                actual_image_column_index = actualimagecursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                orientation_column_index = actualimagecursor.getColumnIndexOrThrow(MediaStore.Images.Media.ORIENTATION);
                break;
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case CURSORLOADER_THUMBS:
                imagecursor = null;
                break;

            case CURSORLOADER_REAL:
                actualimagecursor = null;
                break;
        }

    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        tv_title.setText((position + 1) + "/" + imgs.size());
    }

    @Override
    public void onPageSelected(int position) {

    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    private class mPagerAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return imgs.size();
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0==arg1;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object o){

            //container.removeViewAt(position);

        }

        //设置ViewPager指定位置要显示的view
        @Override
        public Object instantiateItem(ViewGroup container,int position){
            ImageView im= new ImageView(PhotoActivity.this);

            File file = new File(imgs.get(position));
            if(file.exists()){
                Bitmap bm = BitmapFactory.decodeFile(imgs.get(position));
                im.setImageBitmap(bm);
            }
//            im.setImageResource(imgs.get(position));
            container.addView(im);
            return im;

        }

    }

    private class ResizeImagesTask extends AsyncTask<Set<Map.Entry<String, Integer>>, Void, ArrayList<String>> {
        private Exception asyncTaskError = null;

        @Override
        protected ArrayList<String> doInBackground(Set<Map.Entry<String, Integer>>... fileSets) {
            Set<Map.Entry<String, Integer>> fileNames = fileSets[0];
            ArrayList<String> al = new ArrayList<String>();
            try {
                Iterator<Map.Entry<String, Integer>> i = fileNames.iterator();
                Bitmap bmp;
                while (i.hasNext()) {
                    Map.Entry<String, Integer> imageInfo = i.next();
                    File file = new File(imageInfo.getKey());
                    int rotate = imageInfo.getValue();
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 1;
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(file.getAbsolutePath(), options);
                    int width = options.outWidth;
                    int height = options.outHeight;
                    float scale = calculateScale(width, height);

                    if (scale < 1) {
                        int finalWidth = (int)(width * scale);
                        int finalHeight = (int)(height * scale);
                        int inSampleSize = calculateInSampleSize(options, finalWidth, finalHeight);
                        options = new BitmapFactory.Options();
                        options.inSampleSize = inSampleSize;

                        try {
                            bmp = this.tryToGetBitmap(file, options, rotate, true);
                        } catch (OutOfMemoryError e) {
                            options.inSampleSize = calculateNextSampleSize(options.inSampleSize);
                            try {
                                bmp = this.tryToGetBitmap(file, options, rotate, false);
                            } catch (OutOfMemoryError e2) {
                                throw new IOException("Unable to load image into memory.");
                            }
                        }
                    } else {
                        try {
                            bmp = this.tryToGetBitmap(file, null, rotate, false);
                        } catch(OutOfMemoryError e) {
                            options = new BitmapFactory.Options();
                            options.inSampleSize = 2;

                            try {
                                bmp = this.tryToGetBitmap(file, options, rotate, false);
                            } catch(OutOfMemoryError e2) {
                                options = new BitmapFactory.Options();
                                options.inSampleSize = 4;

                                try {
                                    bmp = this.tryToGetBitmap(file, options, rotate, false);
                                } catch (OutOfMemoryError e3) {
                                    throw new IOException("Unable to load image into memory.");
                                }
                            }
                        }
                    }

                    if (outputType == OutputType.FILE_URI) {
                        file = storeImage(bmp, file.getName());
                        al.add(Uri.fromFile(file).toString());

                    } else if (outputType == OutputType.BASE64_STRING) {
                        al.add(getBase64OfImage(bmp));
                    }
                }
                return al;
            } catch (IOException e) {
                try {
                    asyncTaskError = e;
                    for (int i = 0; i < al.size(); i++) {
                        URI uri = new URI(al.get(i));
                        File file = new File(uri);
                        file.delete();
                    }
                } catch (Exception ignore) {
                }

                return new ArrayList<String>();
            }
        }

        @Override
        protected void onPostExecute(ArrayList<String> al) {
            Intent data = new Intent();

            if (asyncTaskError != null) {
                Bundle res = new Bundle();
                res.putString("ERRORMESSAGE", asyncTaskError.getMessage());
                data.putExtras(res);
                setResult(RESULT_CANCELED, data);

            } else if (al.size() > 0) {
                Bundle res = new Bundle();
                res.putStringArrayList("MULTIPLEFILENAMES", al);

                if (imagecursor != null) {
                    res.putInt("TOTALFILES", imagecursor.getCount());
                }

                data.putExtras(res);
                setResult(RESULT_OK, data);

            } else {
                setResult(RESULT_CANCELED, data);
            }

            progress.dismiss();
            finish();
        }

        private Bitmap tryToGetBitmap(File file,
                                      BitmapFactory.Options options,
                                      int rotate,
                                      boolean shouldScale) throws IOException, OutOfMemoryError {
            Bitmap bmp;
            if (options == null) {
                bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
            } else {
                bmp = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            }

            if (bmp == null) {
                throw new IOException("The image file could not be opened.");
            }

            if (options != null && shouldScale) {
                float scale = calculateScale(options.outWidth, options.outHeight);
                bmp = this.getResizedBitmap(bmp, scale);
            }

            if (rotate != 0) {
                Matrix matrix = new Matrix();
                matrix.setRotate(rotate);
                bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
            }

            return bmp;
        }

        /*
        * The following functions are originally from
        * https://github.com/raananw/PhoneGap-Image-Resizer
        *
        * They have been modified by Andrew Stephan for Sync OnSet
        *
        * The software is open source, MIT Licensed.
        * Copyright (C) 2012, webXells GmbH All Rights Reserved.
        */
        private File storeImage(Bitmap bmp, String fileName) throws IOException {
            int index = fileName.lastIndexOf('.');
            String name = fileName.substring(0, index);
            String ext = fileName.substring(index);
            File file = File.createTempFile("tmp_" + name, ext);
            OutputStream outStream = new FileOutputStream(file);

            if (ext.compareToIgnoreCase(".png") == 0) {
                bmp.compress(Bitmap.CompressFormat.PNG, quality, outStream);
            } else {
                bmp.compress(Bitmap.CompressFormat.JPEG, quality, outStream);
            }

            outStream.flush();
            outStream.close();
            return file;
        }

        private Bitmap getResizedBitmap(Bitmap bm, float factor) {
            int width = bm.getWidth();
            int height = bm.getHeight();
            // create a matrix for the manipulation
            Matrix matrix = new Matrix();
            // resize the bit map
            matrix.postScale(factor, factor);
            // recreate the new Bitmap
            return Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
        }

        private String getBase64OfImage(Bitmap bm) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bm.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            return Base64.encodeToString(byteArray, Base64.NO_WRAP);
        }
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private int calculateNextSampleSize(int sampleSize) {
        double logBaseTwo = (int)(Math.log(sampleSize) / Math.log(2));
        return (int)Math.pow(logBaseTwo + 1, 2);
    }

    private float calculateScale(int width, int height) {
        float widthScale = 1.0f;
        float heightScale = 1.0f;
        float scale = 1.0f;
        if (desiredWidth > 0 || desiredHeight > 0) {
            if (desiredHeight == 0 && desiredWidth < width) {
                scale = (float)desiredWidth/width;

            } else if (desiredWidth == 0 && desiredHeight < height) {
                scale = (float)desiredHeight/height;

            } else {
                if (desiredWidth > 0 && desiredWidth < width) {
                    widthScale = (float)desiredWidth/width;
                }

                if (desiredHeight > 0 && desiredHeight < height) {
                    heightScale = (float)desiredHeight/height;
                }

                if (widthScale < heightScale) {
                    scale = widthScale;
                } else {
                    scale = heightScale;
                }
            }
        }

        return scale;
    }

    enum OutputType {

        FILE_URI(0), BASE64_STRING(1);

        int value;

        OutputType(int value) {
            this.value = value;
        }

        public static OutputType fromValue(int value) {
            for (OutputType type : OutputType.values()) {
                if (type.value == value) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Invalid enum value specified");
        }
    }

}





