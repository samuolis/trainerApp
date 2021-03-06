package com.trainerapp.utils

import android.graphics.*
import android.widget.ImageView
import com.trainerapp.models.User

class DrawableUtils {

    private val r = Rect()

    companion object {

        private val r = Rect()

        @Deprecated("Need to fix crash when navigating fast or remove")
        fun setupInitials(imageView: ImageView, user: User) : Bitmap{

            var bitmap = Bitmap.createBitmap(imageView.width, imageView.height, Bitmap.Config.ARGB_8888)
            var canvas = Canvas(bitmap)
            var paint = Paint()
            var firstAndLastName = user.fullName?.split(" ")
            var initials = ""
            firstAndLastName?.forEach { name ->
                initials = initials + name.get(0)
            }
            setTextSizeForWidth(paint,imageView.width.toFloat()-100, initials)
            drawCenter(canvas, paint, initials)
            imageView.setImageBitmap(bitmap)
            return bitmap
        }

        fun setupInitialsForDetails(imageView: ImageView, user: User, size: Int) : Bitmap{

            var bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            var canvas = Canvas(bitmap)
            var paint = Paint()
            var firstAndLastName = user.fullName?.split(" ")
            var initials = ""
            firstAndLastName?.forEach { name ->
                initials = initials + name.get(0)
            }
            setTextSizeForWidth(paint,(size*0.75).toFloat(), initials)
            drawCenter(canvas, paint, initials)
            imageView.setImageBitmap(bitmap)
            return bitmap
        }

        fun getBitmapForImageViewSize(imageView: ImageView, user: User) : Bitmap{

            var bitmap = Bitmap.createBitmap(imageView.width, imageView.height, Bitmap.Config.ARGB_8888)
            var canvas = Canvas(bitmap)
            var paint = Paint()
            var firstAndLastName = user.fullName?.split(" ")
            var initials = ""
            firstAndLastName?.forEach { name ->
                initials = initials + name.get(0)
            }
            setTextSizeForWidth(paint,imageView.width.toFloat()-100, initials)
            drawCenter(canvas, paint, initials)
            return bitmap
        }

        private fun drawCenter(canvas: Canvas, paint: Paint, text: String) {
            canvas.getClipBounds(r)
            val cHeight = r.height()
            val cWidth = r.width()
            paint.textAlign = Paint.Align.LEFT
            paint.color = Color.WHITE
            paint.getTextBounds(text, 0, text.length, r)
            val x = cWidth / 2f - r.width() / 2f - r.left
            val y = cHeight / 2f + r.height() / 2f - r.bottom
            canvas.drawText(text, x, y, paint)
        }

        private fun setTextSizeForWidth(paint: Paint, desiredWidth: Float,
                                        text: String) {

            // Pick a reasonably large value for the test. Larger values produce
            // more accurate results, but may cause problems with hardware
            // acceleration. But there are workarounds for that, too; refer to
            // http://stackoverflow.com/questions/6253528/font-size-too-large-to-fit-in-cache
            val testTextSize = 48f

            // Get the bounds of the text, using our testTextSize.
            paint.textSize = testTextSize
            val bounds = Rect()
            paint.getTextBounds(text, 0, text.length, bounds)

            // Calculate the desired size as a proportion of our testTextSize.
            val desiredTextSize = testTextSize * desiredWidth / bounds.width()

            // Set the paint for that size.
            paint.textSize = desiredTextSize
        }
    }

}
