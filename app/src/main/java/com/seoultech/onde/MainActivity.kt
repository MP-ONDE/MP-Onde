package com.seoultech.onde

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID


class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private lateinit var requestMultiplePermissionsLauncher: ActivityResultLauncher<Array<String>>

    private val requiredPermissions = mutableListOf<String>().apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.BLUETOOTH_ADVERTISE)
        }
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
    }.toTypedArray()

    companion object {
        const val REQUEST_ENABLE_BT = 1
        val SERVICE_UUID: UUID = UUID.fromString("fee84403-9ba8-4797-b69b-7330b6f1a464")
    }

    // BLE 스캔 상태를 추적하기 위한 변수
    private var isScanning = false
    private var isAdvertising = false
    // BLE 스캔 중지 핸들러
    private val scanHandler = Handler(Looper.getMainLooper())

    // 스캔된 사용자 ID 해시를 저장할 집합 (중복 방지)
    private val scannedUserHashes = mutableSetOf<String>()

    // 사용자 정보를 저장할 리스트
    private val scannedUsers = mutableListOf<User>()

    // RecyclerView 및 어댑터
    private lateinit var recyclerView: RecyclerView
    private lateinit var userAdapter: UserAdapter

    //위젯
    private lateinit var topAppBar: MaterialToolbar
    private lateinit var startScanButton: Button


    // 생성
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_main)
        // UI 요소 초기화 및 이벤트 리스너 설정
        initViews()
        // 상단 앱바 설정
        setSupportActionBar(topAppBar)
        topAppBar.setNavigationOnClickListener {
            isAdvertising = !isAdvertising
            checkPermissionsAndToggleAdvertise(isAdvertising)
            updateAdvertiseButtonIcon()
        }

        // Firebase 초기화 및 사용자 등록
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        // 사용자 인증 상태 확인
        if (auth.currentUser == null) {
            // 사용자가 로그인하지 않은 경우, LoginActivity로 이동
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            return // 현재 액티비티를 종료하여 아래 코드가 실행되지 않도록 함
        }

        // 권한 요청 초기화
        requestMultiplePermissionsLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val deniedPermissions = permissions.filter { !it.value }.map { it.key }
            if (deniedPermissions.isEmpty()) {
                // 모든 권한이 허용됨
                // 필요에 따라 추가 작업 수행
            } else {
                // 거부된 권한이 있음
                val message = "다음 권한을 허용해야 합니다:\n${
                    deniedPermissions.joinToString("\n") { permission ->
                        when (permission) {
                            Manifest.permission.BLUETOOTH_SCAN -> "블루투스 스캔"
                            Manifest.permission.BLUETOOTH_CONNECT -> "블루투스 연결"
                            Manifest.permission.BLUETOOTH_ADVERTISE -> "블루투스 광고"
                            Manifest.permission.ACCESS_FINE_LOCATION -> "정밀 위치 정보"
                            Manifest.permission.ACCESS_COARSE_LOCATION -> "대략적 위치 정보"
                            else -> permission
                        }
                    }
                }"
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                showPermissionSettingsDialog()
            }
        }
    }

    private fun initViews() {
        topAppBar = findViewById(R.id.topAppBar)
        startScanButton = findViewById(R.id.startScanButton)
        recyclerView = findViewById(R.id.recyclerView) // XML 레이아웃에 RecyclerView 추가


        startScanButton.setOnClickListener {
            checkPermissionsAndStartScan()
        }

        userAdapter = UserAdapter(scannedUsers)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = userAdapter

        updateAdvertiseButtonIcon()
    }

    private fun updateAdvertiseButtonIcon() {
        val iconRes = if (isAdvertising) {
            R.drawable.ic_bluetooth_disabled
        } else {
            R.drawable.ic_bluetooth
        }
        topAppBar.navigationIcon = getDrawable(iconRes)
    }


    private fun checkPermissionsAndToggleAdvertise(shouldAdvertise: Boolean) {
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            requestMultiplePermissionsLauncher.launch(missingPermissions.toTypedArray())
        } else {
            if (bluetoothAdapter?.isEnabled == true) {
                if (shouldAdvertise) {
                    startAdvertising()
                } else {
                    stopAdvertising()
                }
            } else {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }
        }
    }

    private fun checkPermissionsAndStartScan() {
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            requestMultiplePermissionsLauncher.launch(missingPermissions.toTypedArray())
        } else {
            if (bluetoothAdapter?.isEnabled == true) {
                startScanning()
            } else {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }
        }
    }


    private fun startAdvertising() {
        val bluetoothLeAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser

        if (bluetoothLeAdvertiser == null) {
            Toast.makeText(this, "BLE Advertiser를 사용할 수 없습니다.", Toast.LENGTH_LONG).show()
            return
        }

        // BLE 광고 지원 여부 확인
        if (bluetoothAdapter?.isMultipleAdvertisementSupported == false) {
            Log.e("Advertise", "BLE 광고를 지원하지 않는 기기입니다.")
            Toast.makeText(this, "BLE 광고를 지원하지 않는 기기입니다.", Toast.LENGTH_LONG).show()
            return
        }

        // 권한 체크
        val hasAdvertisePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (!hasAdvertisePermission) {
            Toast.makeText(this, "BLUETOOTH_ADVERTISE 권한이 필요합니다.", Toast.LENGTH_LONG).show()
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .build()

        val serviceUuid = ParcelUuid(SERVICE_UUID)

        // 사용자 ID 해시 생성
        val userId = auth.currentUser?.uid ?: "unknown"
        val serviceDataString = HashUtils.generateUserIdHash(userId)
        val serviceData = Base64.decode(serviceDataString, Base64.NO_WRAP)
        Log.d("Advertise", "광고에 사용될 userIdHash: $serviceDataString") // 로그 추가


        val advertiseData = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(serviceUuid)
            .build()

        val scanResponseData = AdvertiseData.Builder()
            .addServiceData(serviceUuid, serviceData)
            .build()

        try {
            bluetoothLeAdvertiser.startAdvertising(settings, advertiseData, scanResponseData, advertiseCallback)
            isAdvertising = true
        } catch (e: Exception) {
            Log.e("Advertise", "광고 시작 중 예외 발생: ${e.message}")
            Toast.makeText(this, "광고 시작 중 예외 발생: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun stopAdvertising() {
        val bluetoothLeAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser

        // 권한 체크
        val hasAdvertisePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Android S 미만에서는 해당 권한이 필요하지 않습니다.
        }

        if (!hasAdvertisePermission) {
            Toast.makeText(this, "광고를 중지하려면 BLUETOOTH_ADVERTISE 권한이 필요합니다.", Toast.LENGTH_LONG).show()
            return
        }

        try {
            bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
            isAdvertising = false
            Toast.makeText(this, "BLE 광고 중지", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Log.e("Advertise", "권한 오류로 인해 광고를 중지할 수 없습니다: ${e.message}")
            Toast.makeText(this, "권한 오류로 인해 광고를 중지할 수 없습니다.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("Advertise", "광고 중지 중 예외 발생: ${e.message}")
            Toast.makeText(this, "광고 중지 중 예외 발생: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }


    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
            Log.d("Advertise", "BLE 광고 시작 성공")
            Toast.makeText(this@MainActivity, "BLE 광고 시작 성공", Toast.LENGTH_SHORT).show()
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            Log.e("Advertise", "BLE 광고 시작 실패: $errorCode")
            Toast.makeText(this@MainActivity, "BLE 광고 시작 실패: $errorCode", Toast.LENGTH_LONG).show()
            isAdvertising = false
        }
    }

    private fun startScanning() {
        if (isScanning) {
            Toast.makeText(this, "이미 스캔 중입니다.", Toast.LENGTH_SHORT).show()
            return
        }
        // 스캔 결과 리스트 초기화
        scannedUserHashes.clear()
        scannedUsers.clear()
        userAdapter.notifyDataSetChanged()

        val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

        if (bluetoothLeScanner == null) {
            Toast.makeText(this, "BLE Scanner를 사용할 수 없습니다.", Toast.LENGTH_LONG).show()
            return
        }

        // 권한 체크
        val hasScanPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }

        if (!hasScanPermission) {
            Toast.makeText(this, "BLUETOOTH_SCAN 또는 ACCESS_FINE_LOCATION 권한이 필요합니다.", Toast.LENGTH_LONG).show()
            return
        }

        // 앱 UUID를 이용한 앱 사용자 필터링
        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()
        val filters = listOf(scanFilter)

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            bluetoothLeScanner.startScan(filters, settings, scanCallback)
            isScanning = true
            Toast.makeText(this, "BLE 스캔을 시작합니다.", Toast.LENGTH_SHORT).show()

            // 12초 후 스캔 중지
            scanHandler.postDelayed({
                stopScanning()
            }, 12000)
        } catch (e: Exception) {
            Log.e("Scanner", "스캔 시작 중 예외 발생: ${e.message}")
            Toast.makeText(this, "스캔 시작 중 예외 발생: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun stopScanning() {
        val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

        // 권한 체크
        val hasScanPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Android S 미만에서는 해당 권한이 필요하지 않습니다.
        }

        if (!hasScanPermission) {
            Toast.makeText(this, "스캔을 중지하려면 BLUETOOTH_SCAN 권한이 필요합니다.", Toast.LENGTH_LONG).show()
            return
        }

        try {
            bluetoothLeScanner?.stopScan(scanCallback)
            isScanning = false
            Toast.makeText(this, "BLE 스캔 중지", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Log.e("Scanner", "권한 오류로 인해 스캔을 중지할 수 없습니다: ${e.message}")
            Toast.makeText(this, "권한 오류로 인해 스캔을 중지할 수 없습니다.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("Scanner", "스캔 중지 중 예외 발생: ${e.message}")
            Toast.makeText(this, "스캔 중지 중 예외 발생: ${e.message}", Toast.LENGTH_LONG).show()
        }
        // 스캔 종료 후 메시지 표시
        Toast.makeText(this, "스캔이 완료되었습니다.", Toast.LENGTH_SHORT).show()
    }


    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            Log.d("Scanner", "스캔 결과 수신: ${result.device.address}, RSSI: ${result.rssi}")

            val serviceUuid = ParcelUuid(SERVICE_UUID)
            val serviceData = result.scanRecord?.getServiceData(serviceUuid)

            if (serviceData != null) {
                val userIdHashString = Base64.encodeToString(serviceData, Base64.NO_WRAP)
                val rssi = result.rssi
                // 이미 수집된 사용자인지 확인
                if (scannedUserHashes.add(userIdHashString)) {
                    Log.d("Scanner", "새로운 사용자 발견: $userIdHashString, RSSI: $rssi")
                    // 사용자 정보를 가져와 리스트에 추가
                    fetchUserInfo(userIdHashString, rssi)
                } else {
                    // 이미 발견된 사용자라면 RSSI 업데이트
                    updateUserRssi(userIdHashString, rssi)
                }
            } else {
                Log.e("Scanner", "serviceData를 가져올 수 없습니다.")
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e("Scanner", "스캔 실패: $errorCode")
            Toast.makeText(this@MainActivity, "스캔 실패: $errorCode", Toast.LENGTH_LONG).show()
            isScanning = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            stopAdvertising()
            stopScanning()
        } catch (e: Exception) {
            Log.e("MainActivity", "BLE 중지 중 예외 발생: ${e.message}")
        }
    }

    private fun showPermissionSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("권한 필요")
            .setMessage("앱을 사용하려면 모든 권한을 허용해야 합니다. 설정에서 권한을 허용해주세요.")
            .setPositiveButton("설정으로 이동") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun fetchUserInfo(userIdHash: String, rssi: Int) {
        db.collection("users")
            .whereEqualTo("userIdHash", userIdHash)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    for (document in documents) {
                        val nickname = document.getString("nickname") ?: "Unknown"
                        val smallTalk = document.getString("smallTalk") ?: ""
                        val ootd = document.getString("ootd") ?: ""
                        val user = User(userIdHash, nickname, ootd, smallTalk, rssi)
                        scannedUsers.add(user)

                        // RSSI 값에 따라 리스트 정렬
                        scannedUsers.sortByDescending { it.rssi }
                        userAdapter.notifyDataSetChanged()
                        Log.d("Scanner", "사용자 정보 추가: $nickname, RSSI: $rssi")
                    }
                } else {
                    Log.d("Scanner", "사용자 정보를 찾을 수 없습니다: $userIdHash")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Scanner", "사용자 정보 조회 실패: ${exception.message}")
            }
    }

    private fun updateUserRssi(userIdHash: String, rssi: Int) {
        val index = scannedUsers.indexOfFirst { it.userIdHash == userIdHash }
        if (index != -1) {
            val user = scannedUsers[index]
            if (user.rssi != rssi) {
                scannedUsers[index] = user.copy(rssi = rssi)
                // 리스트를 다시 정렬하고 어댑터에 변경 사항 알림
                scannedUsers.sortByDescending { it.rssi }
                userAdapter.notifyDataSetChanged()
            }
        }
    }

    // 탑 바와 관련된 menu, layout 불러오기
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_edit_profile -> {
                val intent = Intent(this, ProfileEditActivity::class.java)
                startActivity(intent)
                return true
            }
            R.id.menu_logout -> {
                auth.signOut()
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
                return true
            }
            R.id.action_question -> {
                // AI 추천 메뉴 아이템 클릭 시 AIRecommendationBottomSheetFragment를 다이얼로그 형식으로 띄우기
                showAIRecommendationFragment()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun showAIRecommendationFragment() {
        // AIRecommendationBottomSheetFragment 인스턴스를 생성하고 다이얼로그로 띄우기
        val fragment = AIRecommendationBottomSheetFragment.newInstance()
        fragment.show(supportFragmentManager, fragment.tag)
    }

}






