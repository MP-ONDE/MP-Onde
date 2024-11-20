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
import java.io.ByteArrayOutputStream

class AdditionalInfoActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private lateinit var editTextNickname: EditText
    private lateinit var radioGroupGender: RadioGroup
    private lateinit var editTextAge: EditText
    private lateinit var editTextInterests: EditText
    private lateinit var editTextOotd: EditText
    private lateinit var editTextSmallTalk: EditText
    private lateinit var editTextSns: EditText
    private lateinit var buttonUploadPhoto: Button
    private lateinit var buttonTakePhoto: Button
    private lateinit var imageViewPhotoPreview: ImageView
    private lateinit var buttonSaveInfo: Button

    private var selectedGender: String? = null
    private var selectedPhotoUri: Uri? = null

    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_IMAGE_PICK = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_additional_info)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // Initialize UI elements
        editTextNickname = findViewById(R.id.editTextNickname)
        radioGroupGender = findViewById(R.id.radioGroupGender)
        editTextAge = findViewById(R.id.editTextAge)
        editTextInterests = findViewById(R.id.editTextInterests)
        editTextOotd = findViewById(R.id.editTextOotd)
        editTextSmallTalk = findViewById(R.id.editTextSmallTalk)
        editTextSns = findViewById(R.id.editTextSns)
        buttonUploadPhoto = findViewById(R.id.buttonUploadPhoto)
        buttonTakePhoto = findViewById(R.id.buttonTakePhoto)
        imageViewPhotoPreview = findViewById(R.id.imageViewPhotoPreview)
        buttonSaveInfo = findViewById(R.id.buttonSaveInfo)

        // Gender selection
        radioGroupGender.setOnCheckedChangeListener { _, checkedId ->
            selectedGender = when (checkedId) {
                R.id.radioMale -> "남"
                R.id.radioFemale -> "여"
                else -> null
            }
        }

        // Photo upload
        buttonUploadPhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, REQUEST_IMAGE_PICK)
        }

        // Photo capture
        buttonTakePhoto.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
        }

        // Save information
        buttonSaveInfo.setOnClickListener {
            saveAdditionalInfo()
        }
    }

    private fun saveAdditionalInfo() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "사용자 인증 실패", Toast.LENGTH_SHORT).show()
            return
        }

        val nickname = editTextNickname.text.toString().trim()
        val age = editTextAge.text.toString().trim()
        val interests = editTextInterests.text.toString().trim()
        val ootd = editTextOotd.text.toString().trim()
        val smallTalk = editTextSmallTalk.text.toString().trim()
        val sns = editTextSns.text.toString().trim()

        if (nickname.isEmpty() || selectedGender == null || age.isEmpty() ||
            interests.isEmpty() || ootd.isEmpty() || smallTalk.isEmpty()) {
            Toast.makeText(this, "모든 필수 항목을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val userInfo = hashMapOf(
            "nickname" to nickname,
            "gender" to selectedGender!!,
            "age" to age,
            "interests" to interests,
            "ootd" to ootd,
            "smallTalk" to smallTalk,
            "sns" to sns
        )

        db.collection("users").document(userId).set(userInfo)
            .addOnSuccessListener {
                if (selectedPhotoUri != null) {
                    uploadPhotoToStorage(userId)
                } else {
                    Toast.makeText(this, "정보가 저장되었습니다.", Toast.LENGTH_SHORT).show()
                    startMainActivity()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "정보 저장 실패", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadPhotoToStorage(userId: String) {
        val storageRef = storage.reference.child("users/$userId/photo.jpg")
        val uploadTask = storageRef.putFile(selectedPhotoUri!!)

        uploadTask.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                val photoUrl = uri.toString()
                db.collection("users").document(userId).set(
                    mapOf("photoUrl" to photoUrl),
                    SetOptions.merge()
                ).addOnSuccessListener {
                    Toast.makeText(this, "사진이 저장되었습니다.", Toast.LENGTH_SHORT).show()
                    startMainActivity()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "사진 URL 가져오기 실패", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "사진 업로드 실패", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
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
}