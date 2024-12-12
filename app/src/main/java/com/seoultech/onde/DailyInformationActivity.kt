package com.seoultech.onde

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.io.ByteArrayOutputStream

class DailyInformationActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private lateinit var editTextOotd: EditText
    private lateinit var editTextSmallTalk: EditText
    private lateinit var imageViewPhotoPreview: ImageView
    private lateinit var buttonUploadPhoto: Button
    private lateinit var buttonTakePhoto: Button
    private lateinit var buttonSaveInfo: Button

    private var selectedPhotoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daily_information)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // Initialize UI elements
        editTextOotd = findViewById(R.id.editTextOotd)
        editTextSmallTalk = findViewById(R.id.editTextSmallTalk)
        imageViewPhotoPreview = findViewById(R.id.imageViewPhotoPreview)
        buttonUploadPhoto = findViewById(R.id.buttonUploadPhoto)
        buttonTakePhoto = findViewById(R.id.buttonTakePhoto)
        buttonSaveInfo = findViewById(R.id.buttonSaveInfo)

        // Load existing user information
        loadUserInformation()

        buttonSaveInfo.setOnClickListener {
            val ootd = editTextOotd.text.toString().trim()
            val smallTalk = editTextSmallTalk.text.toString().trim()

            if (ootd.isEmpty() || smallTalk.isEmpty()) {
                Toast.makeText(this, "모든 필드를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Information saving process
            updateUserInformation(ootd, smallTalk)
        }

        buttonUploadPhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, REQUEST_IMAGE_PICK)
        }

        buttonTakePhoto.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
        }
    }

    private fun loadUserInformation() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    editTextOotd.setText(document.getString("ootd"))
                    editTextSmallTalk.setText(document.getString("smallTalk"))

                    // Load photo preview if available
                    val photoUrl = document.getString("photoUrl")
                    if (!photoUrl.isNullOrEmpty()) {
                        Picasso.get()
                            .load(photoUrl)
                            .into(imageViewPhotoPreview)
                    }
                } else {
                    Toast.makeText(this, "사용자 정보를 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "사용자 정보를 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUserInformation(ootd: String, smallTalk: String) {
        val userId = auth.currentUser?.uid ?: return

        // User information update
        val userUpdates = hashMapOf(
            "ootd" to ootd,
            "smallTalk" to smallTalk
        )

        db.collection("users").document(userId).set(userUpdates, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "정보가 성공적으로 저장되었습니다.", Toast.LENGTH_SHORT).show()
                if (selectedPhotoUri != null) {
                    uploadPhotoToStorage(userId)
                } else {
                    // If no photo is selected, finish and navigate to MainActivity
                    startMainActivity()
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "정보 저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadPhotoToStorage(userId: String) {
        val storageRef = storage.reference.child("users/$userId/photo.jpg")
        val uploadTask = storageRef.putFile(selectedPhotoUri!!)

        uploadTask.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                val photoUrl = uri.toString()

                // Update photo URL in Firestore
                db.collection("users").document(userId).set(
                    mapOf("photoUrl" to photoUrl),
                    SetOptions.merge() // Merge existing fields to update the new photoUrl
                ).addOnSuccessListener {
                    Toast.makeText(this, "사진이 저장되었습니다.", Toast.LENGTH_SHORT).show()
                    // Navigate to MainActivity after successful upload
                    startMainActivity()
                    finish()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "사진 URL 가져오기 실패", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "사진 업로드 실패", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_PICK -> {
                    selectedPhotoUri = data?.data
                    imageViewPhotoPreview.setImageURI(selectedPhotoUri)
                }
                REQUEST_IMAGE_CAPTURE -> {
                    val photoBitmap = data?.extras?.get("data") as Bitmap
                    selectedPhotoUri = bitmapToUri(photoBitmap)
                    imageViewPhotoPreview.setImageBitmap(photoBitmap)
                }
            }
        }
    }

    private fun bitmapToUri(bitmap: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(contentResolver, bitmap, "Photo", null)
        return Uri.parse(path)
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    companion object {
        private const val REQUEST_IMAGE_PICK = 1
        private const val REQUEST_IMAGE_CAPTURE = 2
    }
}