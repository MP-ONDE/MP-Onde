package com.seoultech.onde

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.seoultech.onde.HashUtils.generateUserIdHash
import java.io.ByteArrayOutputStream
import com.squareup.picasso.Picasso
import java.io.InputStream

class ProfileEditActivity : AppCompatActivity() {
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

    private var userId: String? = null
    private var isLoading = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_edit)

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

        userId = auth.currentUser?.uid

        if (userId != null) {
            isLoading = true
            showLoading(true)
            db.collection("users").document(userId!!).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        editTextNickname.setText(document.getString("nickname") ?: "")
                        val gender = document.getString("gender") ?: ""
                        if (gender == "남") {
                            radioGroupGender.check(R.id.radioMale)
                        } else if (gender == "여") {
                            radioGroupGender.check(R.id.radioFemale)
                        }
                        editTextAge.setText(document.getString("age") ?: "")
                        editTextInterests.setText(document.getString("interests") ?: "")
                        editTextOotd.setText(document.getString("ootd") ?: "")
                        editTextSmallTalk.setText(document.getString("smallTalk") ?: "")
                        editTextSns.setText(document.getString("sns") ?: "")
                        val photoUrl = document.getString("photoUrl")
                        if (photoUrl != null) {
                            Picasso.get()
                                .load(photoUrl)
                                .into(imageViewPhotoPreview)
                        }
                    }
                    isLoading = false
                    showLoading(false)
                }
                .addOnFailureListener {
                    Toast.makeText(
                        this,
                        "사용자 정보 불러오기 실패: ${it.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    isLoading = false
                    showLoading(false)
                }
        }

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
        val userIdHash = generateUserIdHash(userId)

        if (nickname.isEmpty() || selectedGender == null || age.isEmpty() ||
            interests.isEmpty() || ootd.isEmpty() || smallTalk.isEmpty()
        ) {
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
            "sns" to sns,
            "userIdHash" to userIdHash,
            "lastSeenTimestamp" to null
        )

        db.collection("users").document(userId).get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val existingPhotoUrl = document.getString("photoUrl")
                if (existingPhotoUrl != null && selectedPhotoUri == null) {
                    userInfo["photoUrl"] = existingPhotoUrl
                }
            }

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
                    Log.e("SaveInfo", "정보 저장 실패: ${it.message}")
                    Toast.makeText(this, "정보 저장 실패: ${it.message}", Toast.LENGTH_SHORT).show()
                }
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

    private fun getRotationAngle(uri: Uri): Float {
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val exif = inputStream?.let { ExifInterface(it) }
        val orientation = exif?.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        inputStream?.close()
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    }

    // 비트맵을 회전시키는 함수
    private fun rotateBitmap(bitmap: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    // URI를 받아 EXIF 정보를 기반으로 이미지를 수정
    private fun getCorrectedBitmap(uri: Uri): Bitmap {
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        val rotationAngle = getRotationAngle(uri)
        return rotateBitmap(originalBitmap, rotationAngle)
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_PICK -> {
                    // 갤러리에서 선택한 이미지 처리
                    selectedPhotoUri = data?.data
                    selectedPhotoUri?.let {
                        val correctedBitmap = getCorrectedBitmap(it)
                        imageViewPhotoPreview.setImageBitmap(correctedBitmap)
                    }
                }
                REQUEST_IMAGE_CAPTURE -> {
                    // 카메라로 찍은 이미지 처리
                    val photoBitmap = data?.extras?.get("data") as Bitmap
                    selectedPhotoUri = bitmapToUri(photoBitmap)

                    // EXIF 정보를 사용하여 이미지를 회전
                    selectedPhotoUri?.let {
                        val correctedBitmap = getCorrectedBitmap(it)
                        imageViewPhotoPreview.setImageBitmap(correctedBitmap)
                    }
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

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            buttonSaveInfo.isEnabled = false
        } else {
            buttonSaveInfo.isEnabled = true
        }
    }
}
