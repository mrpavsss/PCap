package com.example.pcap.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.pcap.LoginActivity
import com.example.pcap.Post
import com.example.pcap.R

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri

import android.provider.MediaStore

import android.os.Build
import android.os.Environment
import androidx.core.net.toFile
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.pcap.BitmapScaler
import com.parse.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


// TODO : Profile Picture, About Me
class ProfileFragment : FeedFragment() {

    // PICK_PHOTO_CODE is a constant integer
    val PICK_PHOTO_CODE = 1046

    lateinit var ivProfilePic: ImageView

    val photoFileName = "pfp.jpg"

    lateinit var user: ParseUser

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        user = ParseUser.getCurrentUser()

        view.findViewById<TextView>(R.id.tvUsername).text = user.username

        ivProfilePic = view.findViewById(R.id.ivProfilePic)
        if (user.getParseFile(KEY_PFP) != null) {
            Glide.with(requireContext())
                .load(user.getParseFile(KEY_PFP)?.url)
                .transform(CircleCrop())
                .into(ivProfilePic)
        } else {
            Glide.with(requireContext())
                .load(R.drawable.instagram_user_filled_24)
                .transform(CircleCrop())
                .into(ivProfilePic)
        }




        ivProfilePic.setOnClickListener {
            onPickPhoto()
        }

        view.findViewById<Button>(R.id.btnLogOut).setOnClickListener {
            ParseUser.logOut()
            val currentUser = ParseUser.getCurrentUser()
            if (currentUser == null) {
                Log.i(TAG, "Log out successful")
                Toast.makeText(requireContext(), "Successfully logged out!", Toast.LENGTH_SHORT).show()
                val i = Intent(requireContext(), LoginActivity::class.java)
                startActivity(i)
                requireActivity().finish()
            } else {
                Log.e(TAG, "Log out unsuccessful")
                Toast.makeText(requireContext(), "Something went wrong when logging out!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun queryPosts() {
        // Specify which class to query
        val query: ParseQuery<Post> = ParseQuery.getQuery(Post::class.java)
        // Find all Post objects
        query.include(Post.KEY_USER)
        query.whereEqualTo(Post.KEY_USER, ParseUser.getCurrentUser())
        query.addDescendingOrder("createdAt")
        query.findInBackground(object : FindCallback<Post> {
            override fun done(posts: MutableList<Post>?, e: ParseException?) {
                if (e != null) {
                    Log.e(TAG, "Error fetching posts")
                } else {
                    if (posts != null) {
                        for (post in posts) {
                            Log.i(
                                TAG, "Post: " + post.getDescription()
                                        + " , username: " + post.getUser())
                        }

                        allPosts.addAll(posts)
                        adapter.notifyDataSetChanged()
                    }
                }
            }

        })
    }

    // Trigger gallery selection for a photo
    fun onPickPhoto() {
        // Create intent for picking a photo from the gallery
        val intent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )

        // If you call startActivityForResult() using an intent that no app can handle, your app will crash.
        // So as long as the result is not null, it's safe to use the intent.
        if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
            // Bring up gallery to select a photo
            startActivityForResult(intent, PICK_PHOTO_CODE)
        }
    }

    fun loadFromUri(photoUri: Uri?): Bitmap? {
        var image: Bitmap? = null
        try {
            // check version of Android on device
            if (Build.VERSION.SDK_INT > 27 && photoUri != null) {
                // on newer versions of Android, use the new decodeBitmap method
                val source: ImageDecoder.Source =
                    ImageDecoder.createSource(requireContext().getContentResolver(), photoUri)
                image = ImageDecoder.decodeBitmap(source)
            } else {
                // support older versions of Android by using getBitmap
                image = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), photoUri)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return image
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data != null && requestCode == PICK_PHOTO_CODE) {
            val photoUri: Uri? = data.data

            // Load the image located at photoUri into selectedImage
            val selectedImage = loadFromUri(photoUri)

            // See BitmapScaler.java: https://gist.github.com/nesquena/3885707fd3773c09f1bb
            val resizedBitmap = BitmapScaler.scaleToFitWidth(selectedImage!!, ivProfilePic.measuredWidth)
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

            if (photoUri != null) {
                user.put(KEY_PFP, ParseFile(resizedFile))
                Log.i(TAG, "Photo success ${photoUri.path}")
            }

            user.saveInBackground { e ->
                if (e == null) {
                    //Save successfull
                    Toast.makeText(requireContext(), "Profile picture updated!", Toast.LENGTH_SHORT).show()
                    Glide.with(requireContext())
                        .load(user.getParseFile(KEY_PFP)?.url)
                        .transform(CircleCrop())
                        .into(ivProfilePic)
                } else {
                    // Something went wrong while saving
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Error: Something went wrong trying to save your profile image!", Toast.LENGTH_SHORT).show()
                }
            }

//            // Load the selected image into a preview
//            ivProfilePic.setImageBitmap(BitmapFactory.decodeFile(resizedFile!!.absolutePath))
        }
    }

    fun getPhotoFileUri(fileName: String): File {
        // Get safe storage directory for photos
        // Use `getExternalFilesDir` on Context to access package-specific directories.
        // This way, we don't need to request external read/write runtime permissions.
        val mediaStorageDir =
            File(requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                ComposeFragment.TAG
            )

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            Log.d(ComposeFragment.TAG, "failed to create directory")
        }

        // Return the file target for the photo based on filename
        return File(mediaStorageDir.path + File.separator + fileName)
    }

    companion object {
        const val KEY_PFP = "profilePic"
        private const val TAG = "ProfileFragment"
    }
}