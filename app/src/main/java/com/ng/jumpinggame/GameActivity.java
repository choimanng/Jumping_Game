package com.ng.jumpinggame;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.io.IOException;
import java.util.Random;

public class GameActivity extends Activity {

    //Random Number
    Random randomNumber;

    //Surface view
    JumpView jumpView;

    //initialize sound variables
    private SoundPool soundPool;
    int sampleCoin = -1, sampleJump = -1, sampleDie = -1;

    //Display Setting Parameters
    int screenWidth, screenHeight,topGap;
    int blockSize, numBlocksWide, numBlocksHigh;

    //Movements
    boolean jumpGuyIsMovingLeft, jumpGuyIsMovingRight;
    boolean jumpGuyIsMovingDown, jumpGuyIsMovingUp;

    //stats
    int score, lives, numCoins;
    long lastFrameTime;

    private Handler myHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //New rand object
        randomNumber = new Random();

        try {
            //load sound & configure display
            loadSound();
            configureDisplay();

            //set content view
            jumpView = new JumpView(this);
            setContentView(jumpView);
        }
        catch (Exception e){
            soundPool.play(sampleDie, 1, 1, 0, 0, 1);
            returnMainActivity();
        }
    }

    class JumpView extends SurfaceView implements Runnable
    {
        //Class Variables
        volatile boolean playingJumpGame;
        Thread ourThread = null;
        SurfaceHolder ourHolder;
        Canvas canvas;
        Paint paint;

        //Game Objects
        GameObject jumpGuy;
        GameObject topStep;
        GameObject bottomStep;
        GameObject coin;

        //Constructor
        public JumpView(Context context)
        {
            super(context);

            try {
                //set view holder & paint
                ourHolder = getHolder();
                paint = new Paint();

                //Initialize Game Objects
                jumpGuy = new GameObject(16,20);
                topStep = new GameObject(12,8);
                bottomStep = new GameObject(12,8);
                coin = new GameObject(10,10);

                //Load and scale bitmaps
                topStep.setBitmap(R.drawable.step, blockSize);
                bottomStep.setBitmap(R.drawable.step, blockSize);
                coin.setBitmap(R.drawable.coin, blockSize);
                jumpGuy.setBitmapSpriteSheet(R.drawable.jumpguy,blockSize,5);

                //Reset Game Objects
                resetTopStep();
                resetJumpGuy();
                resetCoin();
                resetBottomStep();

                //initialize New Game
                playingJumpGame = true;
                lives = 3;
            }
            catch (Exception e){
                playingJumpGame = false;
                returnMainActivity();
            }


            //Another thread with the handler object to set the sprite sheet frame rate
            myHandler = new Handler() {
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    if (playingJumpGame) {
                        jumpGuy.animSprite();
                        myHandler.sendEmptyMessageDelayed(0, 200);
                    }
                }
            };
            myHandler.sendEmptyMessage(0);
        }

        public void resetTopStep()
        {
            topStep.setLeft (randomNumber.nextInt(numBlocksWide - topStep.getWidth()));
            topStep.setTop (numBlocksHigh / 2);                      //middle of the screen
        }

        public void resetJumpGuy()
        {
            jumpGuy.setLeft (topStep.getLeft());        //on the left end of the top step
            jumpGuy.setTop(topStep.getTop() - jumpGuy.getHeight());     //on top of the top step
            jumpGuyIsMovingLeft = false;
            jumpGuyIsMovingRight = false;
            jumpGuyIsMovingDown = false;
            jumpGuyIsMovingUp = false;
        }

        public void resetCoin()
        {
            if ( randomNumber.nextInt(2) == 0 && (bottomStep.getTop() - topStep.getBottom() > coin.getHeight() * 2) )
            {
                coin.setLeft (randomNumber.nextInt(numBlocksWide - coin.getWidth()));
                int y = topStep.getBottom() + randomNumber.nextInt(bottomStep.getTop() - topStep.getBottom() - coin.getHeight() + 1);
                coin.setTop(y);
            }
            else
            {
                coin.setLeft(0);
                coin.setTop(0);
            }
        }

        public void resetBottomStep()
        {
            //Get the random y position
            int y = topStep.getBottom() + randomNumber.nextInt(numBlocksHigh - topStep.getBottom() - bottomStep.getHeight() + 1);
            bottomStep.setTop(y);

            //Decide whether next step is on the right or on the left
            boolean isOnRight = true;
            if (topStep.getLeft() - jumpGuy.getWidth() <= 0 )
            {
                isOnRight = true;
            }
            else if (topStep.getRight() + jumpGuy.getWidth() >= numBlocksWide)
            {
                isOnRight = false;
            }
            else if ( randomNumber.nextInt(2) == 1)
            {
                isOnRight = false;
            }

            //Find the maximum x distance
            int maxBlocksHorizontal;
            if (isOnRight)
            {
                maxBlocksHorizontal = numBlocksWide - topStep.getRight() - jumpGuy.getWidth();
            }
            else
            {
                maxBlocksHorizontal = topStep.getLeft() - jumpGuy.getWidth();
            }
            if (maxBlocksHorizontal > ((bottomStep.getTop() - jumpGuy.getBottom()) / 2))
            {
                maxBlocksHorizontal = (bottomStep.getTop() - jumpGuy.getBottom()) / 2;
            }

            //Set the random x position
            if (isOnRight)
            {
                int x  = topStep.getLeft() + jumpGuy.getWidth() + randomNumber.nextInt(maxBlocksHorizontal + 1);
                bottomStep.setLeft(x);
            }
            else
            {
                int x =  topStep.getLeft() - jumpGuy.getWidth() - randomNumber.nextInt(maxBlocksHorizontal + 1);
                bottomStep.setLeft(x);
            }
        }

        @Override
        public void run() {
            try {
                while (playingJumpGame) {
                    updateGame();
                    drawGame();
                    controlFPS();
                }
            }
            catch (Exception e){
                playingJumpGame = false;
                returnMainActivity();
            }
        }

        public void updateGame()
        {
            //update jumpGuy Right Motion
            if (jumpGuyIsMovingRight) {
                jumpGuy.moveRight(1);
                if (jumpGuy.getRight() > numBlocksWide)
                    jumpGuy.setRight(numBlocksWide);
            }

            //update jumpGuy Left Motion
            if (jumpGuyIsMovingLeft)
            {
                jumpGuy.moveLeft(1);
                if (jumpGuy.getLeft() < 0) {
                    jumpGuy.setLeft(0);
                }
            }

            //check if jumpGuy is standing on the top step
            jumpGuyIsMovingDown = true;

            if ( jumpGuy.getBottom() >= topStep.getTop() && jumpGuy.getBottom() <= topStep.getBottom() )
            {
                if (jumpGuy.getRight() > topStep.getLeft() && jumpGuy.getLeft() < topStep.getRight())
                {
                    jumpGuy.setBottom (topStep.getTop());
                    jumpGuyIsMovingDown = false;
                }
            }

            //check if jumpGuy is standing on the bottom step
            if ( jumpGuy.getBottom() >= bottomStep.getTop() && jumpGuy.getBottom() <= bottomStep.getBottom() )
            {
                if (jumpGuy.getRight() > bottomStep.getLeft() && jumpGuy.getLeft() < bottomStep.getRight())
                {
                    jumpGuy.setBottom (bottomStep.getTop());
                    jumpGuyIsMovingDown = false;
                    //Use the bottom step as top step & create new bottom step
                    soundPool.play(sampleJump, 1, 1, 0, 0, 1);
                    topStep.setLeft (bottomStep.getLeft());
                    topStep.setTop (bottomStep.getTop());
                    resetBottomStep();
                    resetCoin();
                    score++;
                }
            }

            //update Overall Position
            jumpGuy.moveUp(1);
            topStep.moveUp(1);
            bottomStep.moveUp(1);
            if (coin.getLeft() != 0 && coin.getTop() != 0)
                coin.moveUp(1);

            //update jumpGuy Vertical Motion
            if (jumpGuyIsMovingDown)
                jumpGuy.moveDown(3);

            //if jumping up
            if (jumpGuyIsMovingUp)
            {
                jumpGuy.moveUp(20);
                jumpGuyIsMovingUp = false;
            }

            //check if hit the top or bottom of the screen, then die
            if (jumpGuy.getTop() <= 0 || jumpGuy.getTop() >= bottomStep.getBottom())
            {
                lives--;
                soundPool.play(sampleDie, 1, 1, 0, 0, 1);
                if (lives != 0)
                {
                    //Initialize objects
                    resetTopStep();
                    resetJumpGuy();
                    resetBottomStep();
                    resetCoin();
                }
                else
                {
                    returnMainActivity();
                }
            }

            //check if hit the coin
            if (coin.getLeft() != 0 && coin.getTop() != 0)
            {
                if (jumpGuy.isOscillated(coin))
                {
                    soundPool.play(sampleCoin, 1, 1, 0, 0, 1);
                    numCoins++;
                    coin.setLeft(0);
                    coin.setTop(0);
                }
            }
        }

        public void drawGame()
        {
            if (ourHolder.getSurface().isValid())
            {
                //set canvas & paint
                canvas = ourHolder.lockCanvas();
                canvas.drawColor(Color.WHITE);          //set the background color to white

                //draw a top border & Top gap information
                paint.setColor(Color.BLACK);            //set the paint color to black
                paint.setTextSize(topGap - 30);                             //set the paint text size
                canvas.drawText(getString(R.string.score) + "  " + score + "  " + getString(R.string.coins) + numCoins + "  " + getString(R.string.lives) + lives, 10, topGap - 15, paint);
                paint.setStrokeWidth(3);                //4 pixel border
                canvas.drawLine(0, topGap, screenWidth, topGap, paint);

                //Draw the game objects
                topStep.drawBitmap(canvas, blockSize, topGap);
                bottomStep.drawBitmap(canvas, blockSize, topGap);
                if (coin.getLeft() != 0 && coin.getTop() != 0)
                    coin.drawBitmap(canvas, blockSize, topGap);
                jumpGuy.drawBitmapSpriteSheet(canvas, blockSize, topGap);

                //unlock Canvas And Post
                ourHolder.unlockCanvasAndPost(canvas);
            }
        }

        public void controlFPS()
        {
            long timeThisFrame = (System.currentTimeMillis() - lastFrameTime);
            long timeToSleep = 10 - timeThisFrame;
            if (timeToSleep > 0) {
                try {
                    Thread.sleep(timeToSleep);
                } catch (InterruptedException e) {
                    //Catch exception
                }
            }
            lastFrameTime = System.currentTimeMillis();
        }

        @Override
        public boolean onTouchEvent(MotionEvent motionEvent) {
            switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    if (motionEvent.getX() > screenWidth - screenWidth / 5 || motionEvent.getX() >= jumpGuy.getRight() * blockSize)
                    {
                        jumpGuyIsMovingRight = true;
                        jumpGuyIsMovingLeft = false;
                    }
                    if (motionEvent.getX() < screenWidth / 5 || motionEvent.getX() < jumpGuy.getRight() * blockSize)
                    {
                        jumpGuyIsMovingLeft = true;
                        jumpGuyIsMovingRight = false;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    jumpGuyIsMovingRight = false;
                    jumpGuyIsMovingLeft = false;
                    if (motionEvent.getY() < jumpGuy.getTop()  * blockSize)
                        jumpGuyIsMovingUp = true;
                    break;
            }
            return true;
        }

        public void pause() {
            playingJumpGame = false;
            try {
                ourThread.join();
            } catch (InterruptedException e)
            {
                //Catch Exception
            }
        }

        public void resume() {
            playingJumpGame = true;
            ourThread = new Thread(this);
            ourThread.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        jumpView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        jumpView.resume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        while (true) {
            jumpView.pause();
            break;
        }
        finish();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            jumpView.pause();
            returnMainActivity();
            return true;
        }
        return false;
    }

    public void returnMainActivity()
    {
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
        finish();
    }

    public void loadSound()
    {
        //Sound code
        soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        try {
            //Create objects of the 2 required classes
            AssetManager assetManager = getAssets();
            AssetFileDescriptor descriptor;

            //create our three fx in memory ready for use
            descriptor = assetManager.openFd("coin.wav");
            sampleCoin = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("die.wav");
            sampleDie = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("jump.wav");
            sampleJump = soundPool.load(descriptor, 0);
        } catch (IOException e) {
            //catch exceptions here
        }
    }

    public void configureDisplay()
    {
        //find out the width and height of the screen
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;

        //Determine how many game blocks will fit into the height and width
        topGap = screenHeight/20;
        blockSize = screenWidth/200;
        numBlocksWide = 200;
        numBlocksHigh = ((screenHeight - topGap ))/blockSize;
    }


    public class GameObject
    {
        private Rect scaledRect;
        private int width, height;
        private Bitmap bitmap;
        //Sprite sheet frames
        int numFrames;
        int frameNumber;

        public GameObject(int width, int height)
        {
            this.width = width;
            this.height = height;
            scaledRect = new Rect (0,0,0,0);
            numFrames = 0;
            frameNumber = 0;
        }

        public int getWidth()
        {
            return width;
        }

        public int getHeight()
        {
            return height;
        }

        public void setLeft(int left)
        {
            scaledRect.left = left;
            scaledRect.right = left + width;
        }

        public void setTop(int top)
        {
            scaledRect.top = top;
            scaledRect.bottom = top + height;
        }

        public void setRight(int right)
        {
            scaledRect.right = right;
            scaledRect.left = right - width;
        }

        public void setBottom(int bottom)
        {
            scaledRect.bottom = bottom;
            scaledRect.top = bottom - height;
        }

        public int getLeft()
        {
            return scaledRect.left;
        }

        public int getRight()
        {
            return scaledRect.right;
        }

        public int getTop()
        {
            return scaledRect.top;
        }

        public int getBottom()
        {
            return scaledRect.bottom;
        }

        public void moveUp(int numBlocks)
        {
            setTop(scaledRect.top - numBlocks);
        }

        public void moveDown(int numBlocks)
        {
            setTop(scaledRect.top + numBlocks);
        }

        public void moveLeft(int numBlocks)
        {
            setLeft(scaledRect.left - numBlocks);
        }

        public void moveRight(int numBlocks)
        {
            setLeft(scaledRect.left + numBlocks);
        }

        public Rect getScaledRect()
        {
            return scaledRect;
        }

        public boolean isOscillated(GameObject objectB)
        {
            Rect rectA = this.scaledRect;
            Rect rectB = objectB.getScaledRect();

            if (rectA.left > rectB.right || rectB.left > rectA.right) {         // If one rectangle is on left side of other
                return false;
            }
            else if (rectA.top > rectB.bottom || rectB.top > rectA.bottom) {           // If one rectangle is above other
                return false;
            }
            return true;
        }

        public void setBitmap(int id, int blockSize)
        {
            bitmap = BitmapFactory.decodeResource(getResources(),id);
            bitmap = Bitmap.createScaledBitmap(bitmap, width * blockSize, height * blockSize, false);
        }

        public void setBitmapSpriteSheet(int id, int blockSize, int numFrames)
        {
            this.numFrames = numFrames;
            bitmap = BitmapFactory.decodeResource(getResources(),id);
            bitmap = Bitmap.createScaledBitmap(bitmap, width * blockSize * numFrames, height * blockSize, false);
        }

        public void drawBitmap(Canvas canvas, int blockSize, int topGap)
        {
            Paint paint = new Paint();
            canvas.drawBitmap(bitmap, scaledRect.left * blockSize, scaledRect.top * blockSize + topGap, paint);
        }

        public void drawBitmapSpriteSheet(Canvas canvas, int blockSize, int topGap)
        {
            Rect destRect = new Rect(scaledRect.left * blockSize, scaledRect.top  * blockSize  + topGap,
                    scaledRect.right * blockSize, scaledRect.bottom  * blockSize + topGap);
            Rect rectToBeDrawn = new Rect((frameNumber * width * blockSize), 0,(frameNumber + 1 ) * width * blockSize, height  * blockSize);
            Paint paint = new Paint();
            canvas.drawBitmap(bitmap, rectToBeDrawn, destRect, paint);
        }

        public void animSprite()
        {
            //the next frame
            frameNumber++;

            //don't try and draw frames that don't exist
            if(frameNumber == numFrames){
                frameNumber = 0;//back to the first frame
            }
        }
    }
}









