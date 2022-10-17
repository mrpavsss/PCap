package com.example.pcap

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.*
import androidx.core.content.FileProvider
import com.parse.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import androidx.appcompat.app.ActionBar


class MainActivity : AppCompatActivity() {

    val CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 1034
    val photoFileName = "photo.jpg"
    var photoFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        supportActionBar?.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        supportActionBar?.setCustomView(R.layout.actionbar_title);

        findViewById<Button>(R.id.btnSubmit).setOnClickListener {
            val description = findViewById<EditText>(R.id.description).text.toString()
            val user = ParseUser.getCurrentUser()
            if (photoFile != null) {
                submitPost(description, user, photoFile!!)
            } else {
                Log.e(TAG, "photoFile is null")
                Toast.makeText(this, "You can't make a post without a photo!", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<ImageView>(R.id.btnTakePicture).setOnClickListener {
            onLaunchCamera()
        }

        queryPosts()

        // TODO: Move this function to ProfileActivity
        findViewById<Button>(R.id.btnLogOut).setOnClickListener {
            ParseUser.logOut()
            val currentUser = ParseUser.getCurrentUser()
            if (currentUser == null) {
                Log.i(TAG, "Log out successful")
                Toast.makeText(this, "Successfully logged out!", Toast.LENGTH_SHORT).show()
                val i = Intent(this@MainActivity, LoginActivity::class.java)
                startActivity(i)
                finish()
            } else {
                Log.e(TAG, "Log out unsuccessful")
                Toast.makeText(this, "Something went wrong when logging out!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun submitPost(description: String, user: ParseUser, file: File) {
        val pb = findViewById<ProgressBar>(R.id.pbLoading)
        pb.visibility = ProgressBar.VISIBLE
        val post = Post()
        post.setDescription(description)
        post.setUser(user)
        post.setImage(ParseFile(file))
        post.saveInBackground { exception ->
            if (exception != null) {
                Log.e(TAG, "Error while saving post")
                exception.printStackTrace()
                Toast.makeText(this, "Error: Something went wrong trying to save your post!", Toast.LENGTH_SHORT).show()
            } else {
                Log.i(TAG, "Successfully saved post")
                Toast.makeText(this, "Post created!", Toast.LENGTH_SHORT).show()
                findViewById<EditText>(R.id.description).text.clear()
                findViewById<ImageView>(R.id.imageView).setImageResource(android.R.color.transparent)
                findViewById<ImageView>(R.id.btnTakePicture).visibility = View.VISIBLE
            }
            pb.visibility = ProgressBar.INVISIBLE
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                val ivPreview: ImageView = findViewById(R.id.imageView)

                // by this point we have the camera photo on disk
                val takenImage = BitmapFactory.decodeFile(photoFile!!.absolutePath)
                // RESIZE BITMAP
                // See BitmapScaler.java: https://gist.github.com/nesquena/3885707fd3773c09f1bb
                val resizedBitmap = BitmapScaler.scaleToFitWidth(takenImage, ivPreview.measuredWidth)
                // Configure byte output stream
                val bytes = ByteArrayOutputStream()
                // Compress the image further
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 40, bytes)
                // Create a new file for the resized bitmap (`getPhotoFileUri` defined above)
                val resizedFile = getPhotoFileUri(photoFileName + "_resized")
                resizedFile.createNewFile()
                val fos = FileOutputStream(resizedFile)
                // Write the bytes of the bitmap to file
                fos.write(bytes.toByteArray())
                fos.close()

                // Load the taken image into a preview
                findViewById<ImageView>(R.id.btnTakePicture).visibility = View.INVISIBLE
                ivPreview.setImageBitmap(BitmapFactory.decodeFile(resizedFile!!.absolutePath))
            } else { // Result was a failure
                Toast.makeText(this, "Picture wasn't taken!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun onLaunchCamera() {
        // create Intent to take a picture and return control to the calling application
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Create a File reference for future access
        photoFile = getPhotoFileUri(photoFileName)

        // wrap File object into a content provider
        // required for API >= 24
        // See https://guides.codepath.com/android/Sharing-Content-with-Intents#sharing-files-with-api-24-or-higher
        if (photoFile != null) {
            val fileProvider: Uri =
                FileProvider.getUriForFile(this, "com.codepath.fileprovider", photoFile!!)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider)

            // If you call startActivityForResult() using an intent that no app can handle, your app will crash.
            // So as long as the result is not null, it's safe to use the intent.

            // If you call startActivityForResult() using an intent that no app can handle, your app will crash.
            // So as long as the result is not null, it's safe to use the intent.
            if (intent.resolveActivity(packageManager) != null) {
                // Start the image capture intent to take photo
                startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE)
            }
        }
    }

    fun getPhotoFileUri(fileName: String): File {
        // Get safe storage directory for photos
        // Use `getExternalFilesDir` on Context to access package-specific directories.
        // This way, we don't need to request external read/write runtime permissions.
        val mediaStorageDir =
            File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), TAG)

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            Log.d(TAG, "failed to create directory")
        }

        // Return the file target for the photo based on filename
        return File(mediaStorageDir.path + File.separator + fileName)
    }

    // Query for all posts in our server
    fun queryPosts() {

        // Specify which class to query
        val query: ParseQuery<Post> = ParseQuery.getQuery(Post::class.java)
        // Find all Post objects
        query.include(Post.KEY_USER)
        query.findInBackground(object : FindCallback<Post> {
            override fun done(posts: MutableList<Post>?, e: ParseException?) {
                if (e != null) {
                    Log.e(TAG, "Error fetching posts")
                } else {
                    if (posts != null) {
                        for (post in posts) {
                            Log.i(TAG, "Post: " + post.getDescription()
                                    + " , username: " + post.getUser())
                        }
                    }
                }
            }

        })
    }

    companion object {
        const val TAG = "MainActivity"
    }
}