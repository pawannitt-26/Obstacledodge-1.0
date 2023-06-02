
package com.example.task2


import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaPlayer
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random



@OptIn(DelicateCoroutinesApi::class)
class JumpingGame(context: Context, attrs: AttributeSet?) : View(context, attrs) {


    companion object {
        private const val OBSTACLE_WIDTH_SHORT = 100
        private const val OBSTACLE_WIDTH_LONG = 150
        private const val PLAYER_SIZE = 150
        private const val OBSTACLE_SIZE = 150
        private const val GROUND_HEIGHT = 980
        private var MOVE_SPEED = 8
        private var obstacleSpeed = 8
        private const val GENERATE_DELAY = 3500L
        private const val JUMP_HEIGHT = 400
    }


    private var obstacleShortBitmap: Bitmap? = null
    private var obstacleLongBitmap: Bitmap? = null
    private var backgroundBitmap: Bitmap? = null
    private var groundBitmap: Bitmap? = null
    private var playerBitmap: Bitmap? = null
    private var chaserBitmap: Bitmap? = null
    private var playerX: Int = 400
    private var obstacleCrossed: Int = 0
    private var playerY: Int = GROUND_HEIGHT - PLAYER_SIZE
    private var chaserY: Int = GROUND_HEIGHT - PLAYER_SIZE
    private var obstacleY: Int = GROUND_HEIGHT - OBSTACLE_SIZE
    private var isJumping: Boolean = false
    private var jumpCount: Int = 0
    private var chaserX: Int = 150
    private var isChaserJumping: Boolean = false
    private var chaserJumpCount: Int = 0
    private val CHASER_JUMP_HEIGHT: Int = 400
    private var obstacleList: MutableList<Obstacle> = mutableListOf()
    private var mediaPlayer: MediaPlayer? = null
    private var mediaPlayer1: MediaPlayer? = null


    private inner class Obstacle(val bitmap: Bitmap, var x: Int) {
        val width: Int = bitmap.width
    }

    init {
        obstacleShortBitmap = BitmapFactory.decodeResource(resources, R.drawable.img_4)
        obstacleLongBitmap = BitmapFactory.decodeResource(resources, R.drawable.img_3)
        playerBitmap = BitmapFactory.decodeResource(resources, R.drawable.img_2)
        chaserBitmap = BitmapFactory.decodeResource(resources, R.drawable.img_1)
        backgroundBitmap = BitmapFactory.decodeResource(resources, R.drawable.background)
        groundBitmap = BitmapFactory.decodeResource(resources, R.drawable.ground)
        mediaPlayer = MediaPlayer.create(context, R.raw.templerun)
        mediaPlayer?.isLooping = true
        mediaPlayer1 = MediaPlayer.create(context, R.raw.collision)

        // Start generating obstacles
        GlobalScope.launch {
            while (true) {
                generateObstacle()
                delay(GENERATE_DELAY)
            }
        }

        // Start moving player and chaser
        GlobalScope.launch {
            while (true) {
                movePlayerAndChaser()
                delay(16)
            }
        }
    }




    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)


        if (!mediaPlayer?.isPlaying!!) {
            mediaPlayer?.start()
        }


        // Draw the background image as a full-screen background
        backgroundBitmap?.let {
            canvas.drawBitmap(it, null, Rect(0, 0, width, height), null)
        }

       // Draw the ground
        groundBitmap?.let {
            canvas.drawBitmap(it, null, Rect(0, GROUND_HEIGHT - 100, width, height), null)
        }


        // Draw the player rectangle
        val playerRect = Rect(playerX, playerY, playerX + PLAYER_SIZE, playerY + PLAYER_SIZE)
        val playerPaint = Paint().apply {
            color = Color.TRANSPARENT
        }
        canvas.drawRect(playerRect, playerPaint)
        // Draw the player bitmap
        playerBitmap?.let {
            val playerBitmapRect =
                Rect(playerX, playerY, playerX + PLAYER_SIZE, playerY + PLAYER_SIZE)
            canvas.drawBitmap(it, null, playerBitmapRect, null)
        }


        // Draw the chaser rectangle
        val chaserRect = Rect(chaserX, chaserY, chaserX + PLAYER_SIZE, chaserY + PLAYER_SIZE)
        val chaserPaint = Paint().apply {
            color = Color.TRANSPARENT
        }
        canvas.drawRect(chaserRect, chaserPaint)
        // Draw the chaser bitmap
        chaserBitmap?.let {
            val chaserBitmapRect =
                Rect(chaserX, chaserY, chaserX + PLAYER_SIZE, chaserY + PLAYER_SIZE)
            canvas.drawBitmap(it, null, chaserBitmapRect, null)
        }


        // Draw obstacles
        for (obstacle in obstacleList) {
            //Draw obstacle rectangle
            val obstacleRect = Rect(obstacle.x, obstacleY, obstacle.x + OBSTACLE_SIZE,obstacleY + OBSTACLE_SIZE)
            val obstaclePaint = Paint().apply {
                color = Color.TRANSPARENT
            }
            canvas.drawRect(obstacleRect, obstaclePaint)
            // Draw the obstacle bitmap
            obstacle.bitmap?.let {
                val obstacleBitmapRect = Rect(obstacle.x, obstacleY, obstacle.x + OBSTACLE_SIZE, obstacleY + OBSTACLE_SIZE)
                canvas.drawBitmap(it, null, obstacleBitmapRect, null)
            }
        }


        val obstaclesToRemove = mutableListOf<Obstacle>()

        for (obstacle in obstacleList) {
            obstacle?.let {
                it.x -= obstacleSpeed

                if (obstacleCrossed % 3 == 0 && obstacleCrossed > 0) {
                    it.x -= obstacleSpeed + 3
                }

                // Check if the obstacle has moved off the screen
                if (it.x + it.width < 0) {
                    obstaclesToRemove.add(it)
                }
            }
        }

    // Remove obstacles that have moved off the screen
        obstacleList.removeAll(obstaclesToRemove)


        // Check for collision
        checkCollision()


        // Redraw the view
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN && !isJumping) {
            jump()
        }
        return true
    }

    private fun jump() {
        isJumping = true
        jumpCount = 0

        // Perform the jump animation
        GlobalScope.launch {
            while (jumpCount < JUMP_HEIGHT) {
                playerY -= MOVE_SPEED
                jumpCount += MOVE_SPEED
                delay(16)
            }
            // Reverse the jump animation
            while (jumpCount > 0) {
                playerY += MOVE_SPEED
                jumpCount -= MOVE_SPEED
                delay(16)
            }

            // Reset the player position and jumping flag
            playerY = GROUND_HEIGHT - PLAYER_SIZE
            isJumping = false
        }

    }


    private fun generateObstacle() {
        val randomWidth =
            if (Random.nextBoolean()) OBSTACLE_WIDTH_SHORT else OBSTACLE_WIDTH_LONG
        val randomBitmap =
            if (randomWidth == OBSTACLE_WIDTH_SHORT) obstacleShortBitmap else obstacleLongBitmap

        randomBitmap?.let {
            obstacleList.add(Obstacle(it, width))
        }
    }


    private fun movePlayerAndChaser() {

//        playerX += MOVE_SPEED
//        chaserX += MOVE_SPEED


        // Check if an obstacle is close to the chaser
        val obstacleThreshold = chaserX + PLAYER_SIZE + OBSTACLE_SIZE
        for (obstacle in obstacleList) {
            if (obstacle.x < obstacleThreshold + 5 && obstacle.x > chaserX) {
                // Start chaser jump
                if (!isChaserJumping) {
                    startChaserJump()
                }
                break
            }
        }

//        if (playerX > width) {
//            playerX = 0
//        }
//
//        if (chaserX > width) {
//            chaserX = playerX - 400
//        }

    }

    private fun startChaserJump() {
        isChaserJumping = true
        chaserJumpCount = 0

        // Perform the chaser jump animation
        GlobalScope.launch {
            while (chaserJumpCount < CHASER_JUMP_HEIGHT) {
                chaserY -= MOVE_SPEED
                chaserJumpCount += MOVE_SPEED
                delay(16)
            }

            // Reverse the chaser jump animation
            while (chaserJumpCount > 0) {
                chaserY += MOVE_SPEED
                chaserJumpCount -= MOVE_SPEED
                delay(16)
            }

            // Reset the chaser jump flag
            chaserY = GROUND_HEIGHT - PLAYER_SIZE
            isChaserJumping = false
        }
    }

    private var count = 3
    private fun checkCollision() {
        val playerRect = Rect(playerX, playerY, playerX + PLAYER_SIZE, playerY + PLAYER_SIZE)

        for (obstacle in obstacleList) {
            val obstacleRect = Rect(obstacle.x, obstacleY, obstacle.x + OBSTACLE_SIZE, obstacleY + OBSTACLE_SIZE)

            if (playerRect.intersect(obstacleRect)) {
                if (!mediaPlayer1?.isPlaying!!) {
                    mediaPlayer1?.start()
                }
                count--
               for (count in 1..3){
                   resetGame()
               }
                if (count == 0){
                    mediaPlayer?.stop()
                    mediaPlayer1?.stop()
                    val intent = Intent(context,home::class.java)
                    context.startActivity(intent)
                }

            }
        }
    }

    private fun resetGame() {
        // Reset the player, chaser, and obstacle positions
        playerX = 400
        playerY = GROUND_HEIGHT - PLAYER_SIZE
        chaserX = 150
        chaserY = GROUND_HEIGHT - PLAYER_SIZE
        obstacleList.clear()
    }



}




