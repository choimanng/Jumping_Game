package com.ng.jumpinggame;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    //Buttons
    Button buttonPlay, buttonExit;
    ImageView imageJumpingIcon;

    //Sound Effect
    private SoundPool soundPool;
    int sample1 = -1;
    int sample2 = -1;

    //Animation
    Animation anim1, anim2;

    //The jump guy sprite sheet
    Bitmap jumpGuyBitmap;
    int numFrames = 5;
    int frameNumber = 0;
    int jumpGuyWidth = 560, jumpGuyHeight = 770;

    //Canvas, etc
    Canvas canvas;
    Paint paint;
    Bitmap ourBitmap;
    private Handler myHandler;
    boolean gameOn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Set Buttons & ImageView
        buttonPlay = (Button)findViewById(R.id.buttonPlay);
        buttonPlay.setOnClickListener(this);
        buttonExit = (Button)findViewById(R.id.buttonExit);
        buttonExit.setOnClickListener(this);
        imageJumpingIcon = (ImageView) findViewById(R.id.imageJumpingIcon);

        //Set animation
        anim1 = AnimationUtils.loadAnimation(this, R.anim.anim1);
        anim2 = AnimationUtils.loadAnimation(this, R.anim.fadeout);

        //Create Sound Effect in memory
        soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC,0);
        try{
            AssetManager assetManager = getAssets();
            AssetFileDescriptor descriptor;
            descriptor = assetManager.openFd("sample1.ogg");
            sample1 = soundPool.load(descriptor, 0);
            descriptor = assetManager.openFd("sample2.ogg");
            sample2 = soundPool.load(descriptor, 0);

        }
        catch(IOException e)
        {
            //Catch Error
        }

        //Set canvas & paint
        ourBitmap = Bitmap.createBitmap(jumpGuyWidth,jumpGuyHeight, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(ourBitmap);
        paint = new Paint();

        //the sprite sheet
        jumpGuyBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.jumpguy);
        jumpGuyBitmap = Bitmap.createScaledBitmap(jumpGuyBitmap, jumpGuyWidth * numFrames, jumpGuyHeight, false);

        myHandler = new Handler() {
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (gameOn) {
                    updateJumpGuySpriteSheet();
                    myHandler.sendEmptyMessageDelayed(0, 200);
                }
            }
        };
        gameOn = true;
        myHandler.sendEmptyMessage(0);
    }

    public void updateJumpGuySpriteSheet() {
        //don't try and draw frames that don't exist
        if(frameNumber == numFrames){
            frameNumber = 0;//back to the first frame
        }

        canvas.drawColor(Color.WHITE);          //set the background color to black
        Rect destRect = new Rect(0, 0, jumpGuyWidth, jumpGuyHeight);
        Rect rectToBeDrawn = new Rect((frameNumber * jumpGuyWidth), 0,(frameNumber + 1 ) * jumpGuyWidth, jumpGuyHeight);
        canvas.drawBitmap(jumpGuyBitmap, rectToBeDrawn, destRect, paint);
        imageJumpingIcon.setImageBitmap(ourBitmap);

        //now the next frame
        frameNumber++;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.buttonPlay)
        {
            //Sound & Animation
            view.startAnimation(anim1);
            soundPool.play(sample1, 1, 1, 0, 0, 1);

            //Go to Game Activity
            gameOn = false;
            Intent i;
            i = new Intent(this, GameActivity.class);
            startActivity(i);
            finish();
        }

        if (view.getId() == R.id.buttonExit)
        {
            //Sound & Animation
            view.startAnimation(anim1);
            soundPool.play(sample2, 1, 1, 0, 0, 1);

            //Exit
            gameOn = false;
            finish();
        }
    }
}
