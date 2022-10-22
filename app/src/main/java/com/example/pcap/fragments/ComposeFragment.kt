package com.example.pcap.fragments

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.Image
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.pcap.*
import com.parse.ParseFile
import com.parse.ParseUser
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class ComposeFragment : Fragment() {

    val CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 1034
    val photoFileName = "photo.jpg"
    var photoFile: File? = null

    lateinit var ivPreview: ImageView
    lateinit var pb: ProgressBar
    lateinit var etDescription: EditText
    lateinit var ivPictureBtn: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_compose, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ivPreview = view.findViewById(R.id.imageView)
        pb = view.findViewById(R.id.pbLoading)
        etDescription = view.findViewById(R.id.description)
        ivPictureBtn = view.findViewById(R.id.btnTakePicture)

        view.findViewById<Button>(R.id.btnSubmit).setOnClickListener {
            val description = etDescription.text.toString()
            val user = ParseUser.getCurrentUser()
            if (photoFile != null) {
                submitPost(description, user, photoFile!!)
            } else {
                Log.e(TAG, "photoFile is null")
                Toast.makeText(requireContext(), "You can't make a post without a photo!", Toast.LENGTH_SHORT).show()
            }
        }

        ivPictureBtn.setOnClickListener {
            onLaunchCamera()
        }
    }

    fun submitPost(description: String, user: ParseUser, file: File) {

        pb.visibility = ProgressBar.VISIBLE
        val post = Post()
        post.setDescription(description)
        post.setUser(user)
        post.setImage(ParseFile(file))
        post.saveInBackground { exception ->
            if (exception != null) {
                Log.e(TAG, "Error while saving post")
                exception.printStackTrace()
                Toast.makeText(requireContext(), "Error: Something went wrong trying to save your post!", Toast.LENGTH_SHORT).show()
            } else {
                Log.i(TAG, "Successfully saved post")
                Toast.makeText(requireContext(), "Post created!", Toast.LENGTH_SHORT).show()
                etDescription.text.clear()
                ivPreview.setImageResource(android.R.color.transparent)
                ivPictureBtn.visibility = View.VISIBLE
            }
            pb.visibility = ProgressBar.INVISIBLE
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
                FileProvider.getUriForFile(requireContext(), "com.codepath.fileprovider", photoFile!!)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider)

            // If you call startActivityForResult() using an intent that no app can handle, your app will crash.
            // So as long as the result is not null, it's safe to use the intent.
            if (intent.resolveActivity(requireContext().packageManager) != null) {
                // Start the image capture intent to take photo
                startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == AppCompatActivity.RESULT_OK) {
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
                ivPictureBtn.visibility = View.INVISIBLE
                ivPreview.setImageBitmap(BitmapFactory.decodeFile(resizedFile!!.absolutePath))
            } else { // Result was a failure
                Toast.makeText(requireContext(), "Picture wasn't taken!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun getPhotoFileUri(fileName: String): File {
        // Get safe storage directory for photos
        // Use `getExternalFilesDir` on Context to access package-specific directories.
        // This way, we don't need to request external read/write runtime permissions.
        val mediaStorageDir =
            File(requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), TAG)

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            Log.d(TAG, "failed to create directory")
        }

        // Return the file target for the photo based on filename
        return File(mediaStorageDir.path + File.separator + fileName)
    }

    companion object {
        const val TAG = "ComposeFragment"
    }

}