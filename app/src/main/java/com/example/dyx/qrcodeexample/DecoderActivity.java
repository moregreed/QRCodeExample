package com.example.dyx.qrcodeexample;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dlazaro66.qrcodereaderview.QRCodeReaderView;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Hashtable;

public class DecoderActivity extends Activity implements QRCodeReaderView.OnQRCodeReadListener {

    private TextView myTextView;
    private QRCodeReaderView mydecoderview;
    private ImageView line_image;
    private ImageView image_back;
    private ImageView image_more;
    private Bitmap scanBitmap;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decoder);

        mydecoderview = (QRCodeReaderView) findViewById(R.id.qrdecoderview);
        mydecoderview.setOnQRCodeReadListener(this);

        myTextView = (TextView) findViewById(R.id.exampleTextView);

        line_image = (ImageView) findViewById(R.id.line_image);
        ScaleAnimation animation = new ScaleAnimation(1.0f, 1.0f, 0.0f, 1.0f);
        animation.setRepeatCount(-1);
        animation.setRepeatMode(Animation.RESTART);
        animation.setInterpolator(new LinearInterpolator());
        animation.setDuration(1200);
        line_image.startAnimation(animation);
        image_back = (ImageView) findViewById(R.id.image_back);
        image_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        image_more = (ImageView) findViewById(R.id.image_more);
        image_more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getPhoto();
            }
        });
    }

    private void getPhoto() {
        Intent intent = new Intent();
        if(Build.VERSION.SDK_INT<19){
            intent.setAction(Intent.ACTION_GET_CONTENT);
        }else{
            intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
        }
        intent.setType("image/*");
        Intent wrappterIntent = Intent.createChooser(intent,null);
        startActivityForResult(intent,Constants.RQC_PICTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK){
            if(requestCode == Constants.RQC_PICTURE){
                Uri uri = data.getData();
                String path = UriUtil.getPath(this, uri);
                scannerImage(path);
            }
        }
    }
    //从图片中识别二维码
    private void scannerImage(final String path) {
        if(TextUtils.isEmpty(path)){
            return ;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                Hashtable<DecodeHintType,Object>hints = new Hashtable<DecodeHintType, Object>();
                hints.put(DecodeHintType.CHARACTER_SET,"utf-8");
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                scanBitmap = BitmapFactory.decodeFile(path,options);
                options.inJustDecodeBounds = false;
                int sampleSize = (int) (options.outHeight / (float) 400);

                if (sampleSize <= 0)
                    sampleSize = 1;
                options.inSampleSize = sampleSize;
                scanBitmap = BitmapFactory.decodeFile(path, options);

                int width = scanBitmap.getWidth();
                int height = scanBitmap.getHeight();
                int[] pixels = new int[width * height];
                scanBitmap.getPixels(pixels, 0, width, 0, 0, width, height);

                RGBLuminanceSource source = new RGBLuminanceSource(width,height,pixels);
                BinaryBitmap bitmap1 = new BinaryBitmap(new HybridBinarizer(source));
                QRCodeReader reader = new QRCodeReader();
                try {

                    Result result = reader.decode(bitmap1, hints);
                    if (result == null) {
                        Looper.prepare();
                        Toast.makeText(getApplicationContext(), "图片格式有误", Toast.LENGTH_SHORT)
                                .show();
                        Looper.loop();
                    } else {
                        String recode = recode(result.toString());
                        Intent data = new Intent();
                        data.putExtra(Constants.EX_QR, recode);
                        setResult(RESULT_OK, data);
                        finish();
                    }

                } catch (NotFoundException e) {
                    e.printStackTrace();
                } catch (ChecksumException e) {
                    e.printStackTrace();
                } catch (FormatException e) {
                    e.printStackTrace();

                }
            }
        }).start();
    }

    private String recode(String str) {
        String formart = "";

        try {
            boolean ISO = Charset.forName("ISO-8859-1").newEncoder()
                    .canEncode(str);
            if (ISO) {
                formart = new String(str.getBytes("ISO-8859-1"), "GB2312");
//                Log.i("1234      ISO8859-1", formart);
            } else {
                formart = str;
//                Log.i("1234      stringExtra", str);
            }
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return formart;
    }

    // Called when a QR is decoded
    // "text" : the text encoded in QR
    // "points" : points where QR control points are placed
    @Override
    public void onQRCodeRead(String text, PointF[] points) {
        Intent intent = new Intent();
        intent.putExtra(Constants.EX_QR, text);
        setResult(RESULT_OK, intent);
        finish();
    }


    // Called when your device have no camera
    @Override
    public void cameraNotFound() {

    }

    // Called when there's no QR codes in the camera preview image
    @Override
    public void QRCodeNotFoundOnCamImage() {

    }

    @Override
    protected void onResume() {
        super.onResume();
        mydecoderview.getCameraManager().startPreview();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mydecoderview.getCameraManager().stopPreview();
    }


}

