package com.seoultech.onde

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val REQUEST_LOCATION_PERMISSION = 1
    private val REQUEST_BLUETOOTH_PERMISSION = 2
    private var isDiscoverable = mutableStateOf(false)

    private lateinit var deviceRecyclerView: RecyclerView
    private lateinit var deviceAdapter: DeviceAdapter
    private val deviceList = mutableListOf<DeviceInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Firebase 초기화
        FirebaseApp.initializeApp(this) // Firebase 초기화 필요

        // RecyclerView 초기화
        deviceRecyclerView = findViewById(R.id.deviceRecyclerView)
        deviceAdapter = DeviceAdapter(deviceList)
        deviceRecyclerView.layoutManager = LinearLayoutManager(this)
        deviceRecyclerView.adapter = deviceAdapter


        // 위치 권한 요청
        requestLocationPermission()
        // 블루투스 권한 요청
        requestBluetoothPermissions()

        auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            auth.signInAnonymously().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    saveUserToFirestore(userId)
                } else {
                    Toast.makeText(this, "사용자 등록 실패: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        val profileEditText = findViewById<EditText>(R.id.profileEditText)
        val saveProfileButton = findViewById<Button>(R.id.saveProfileButton)
        val startDiscoveryButton = findViewById<Button>(R.id.startDiscoveryButton)
        val setDiscoverableButton = findViewById<Button>(R.id.setDiscoverableButton)

        saveProfileButton.setOnClickListener {
            val profile = profileEditText.text.toString()
            saveUserProfile(profile)
        }

        startDiscoveryButton.setOnClickListener {
            startBluetoothDiscovery()
        }

        setDiscoverableButton.setOnClickListener {
            isDiscoverable.value = !isDiscoverable.value
            setDeviceDiscoverable(isDiscoverable.value)
        }
    }

    // 위치 권한 요청
    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }

    // 블루투스 권한 요청
    private fun requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 이상에서는 BLUETOOTH_SCAN과 BLUETOOTH_CONNECT 권한이 필요함
            val permissionsToRequest = mutableListOf<String>()

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }

            if (permissionsToRequest.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), REQUEST_BLUETOOTH_PERMISSION)
            }
        } else {
            // Android 12 미만의 경우 위치 권한 요청
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_PERMISSION)
            }
        }
    }

    private fun saveUserToFirestore(userId: String?) {
        val db = FirebaseFirestore.getInstance()
        val user = hashMapOf(
            "userId" to userId,
            "username" to "기본 사용자명",
            "profile" to "기본 프로필 내용" // 프로필 정보를 추가
        )
        if (userId != null) {
            db.collection("users").document(userId).set(user)
                .addOnSuccessListener {
                    Toast.makeText(this, "사용자 등록 성공", Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "사용자 정보 저장 실패", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun saveUserProfile(profile: String) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(userId).update("profile", profile)
                .addOnSuccessListener {
                    Toast.makeText(this, "프로필 업데이트 성공", Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "프로필 업데이트 실패: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun startBluetoothDiscovery() {
        try {
            if (bluetoothAdapter?.isEnabled == true) {
                // 이전에 진행 중인 탐색이 있다면 종료
                if (bluetoothAdapter.isDiscovering) {
                    bluetoothAdapter.cancelDiscovery()
                }

                // BroadcastReceiver 등록 및 탐색 시작
                val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
                registerReceiver(receiver, filter)
                bluetoothAdapter.startDiscovery()

                // 일정 시간 후 탐색 종료
                android.os.Handler(mainLooper).postDelayed({
                    if (bluetoothAdapter.isDiscovering) {
                        bluetoothAdapter.cancelDiscovery()
                        Toast.makeText(this, "블루투스 탐색이 종료되었습니다.", Toast.LENGTH_SHORT).show()
                    }
                }, 12000) // 12초 후 탐색 종료
            } else {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivity(enableBtIntent)
            }
        } catch (e: SecurityException) {
            Log.e("MainActivity", "권한 오류로 인해 Bluetooth 탐지를 시작할 수 없습니다: ${e.message}")
        }
    }

    private fun setDeviceDiscoverable(isDiscoverable: Boolean) {
        try {
            if (isDiscoverable) {
                if (bluetoothAdapter?.isEnabled == true) {
                    val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                        putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                    }
                    startActivity(discoverableIntent)
                } else {
                    // Bluetooth가 활성화되어 있지 않으면 Bluetooth 활성화 요청
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    startActivity(enableBtIntent)
                }
            } else {
                // 탐색 가능 모드를 종료하려면 별도로 할 수 있는 기능이 제한적이므로 안내 메시지로 대체
                Toast.makeText(this, "탐색 가능 모드를 종료합니다.", Toast.LENGTH_LONG).show()
            }
        } catch (e: SecurityException) {
            Log.e("MainActivity", "권한 오류로 인해 탐색 가능 모드를 설정할 수 없습니다: ${e.message}")
            Toast.makeText(this, "Bluetooth 권한이 필요합니다.", Toast.LENGTH_LONG).show()
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (BluetoothDevice.ACTION_FOUND == intent.action) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                val deviceId = device?.address

                if (deviceId != null) {
                    // Firestore에서 탐지된 사용자의 프로필 정보 조회
                    val db = FirebaseFirestore.getInstance()
                    db.collection("users").document(deviceId).get()
                        .addOnSuccessListener { document ->
                            if (document != null && document.exists()) {
                                val profile = document.getString("profile")
                                Toast.makeText(context, "탐지된 사용자 프로필: $profile", Toast.LENGTH_LONG).show()
                            } else {
                                Log.e("MainActivity", "탐지된 사용자 정보를 찾을 수 없습니다.")
                            }
                        }
                        .addOnFailureListener {
                            Log.e("MainActivity", "탐지된 사용자 정보 조회 실패: ${it.message}")
                        }
                } else {
                    Log.e("MainActivity", "탐지된 기기의 ID가 null입니다. 저장하지 않습니다.")
                }
            }
        }
    }

    data class DeviceInfo(val username: String, val profile: String)

    class DeviceAdapter(private val devices: List<DeviceInfo>) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

        class DeviceViewHolder(val view: android.view.View) : RecyclerView.ViewHolder(view) {
            val usernameTextView: android.widget.TextView = view.findViewById(R.id.usernameTextView)
            val profileTextView: android.widget.TextView = view.findViewById(R.id.profileTextView)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): DeviceViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.device_item, parent, false)
            return DeviceViewHolder(view)
        }

        override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
            val device = devices[position]
            holder.usernameTextView.text = device.username
            holder.profileTextView.text = device.profile
        }

        override fun getItemCount() = devices.size
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }
}
