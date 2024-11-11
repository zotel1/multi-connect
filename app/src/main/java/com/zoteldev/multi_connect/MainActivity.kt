package com.zoteldev.multi_connect

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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private val bluetoothAdapter: BluetoothAdapter? by lazy { BluetoothAdapter.getDefaultAdapter() }
    private lateinit var deviceRecyclerView: RecyclerView
    private lateinit var deviceAdapter: DeviceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_CONNECT),
                REQUEST_PERMISSION_BLUETOOTH
            )
        } else {
            listPairedDevices()
            discoverNearbyDevices()
        }


        // Inicializar RecyclerView para mostrar dispositivos
        deviceRecyclerView = findViewById(R.id.device_recycler_view)
        deviceAdapter = DeviceAdapter()
        deviceRecyclerView.layoutManager = LinearLayoutManager(this)
        deviceRecyclerView.adapter = deviceAdapter

        // Verificar permisos y disponibilidad de Bluetooth
        checkPermissions()
    }

    private fun checkPermissions() {
        if (bluetoothAdapter == null) {
            Log.e("Bluetooth", "Bluetooth no está disponible en este dispositivo")
            Toast.makeText(this, "Bluetooth no disponible", Toast.LENGTH_SHORT).show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Pedir permisos de Bluetooth en tiempo de ejecución (para Android 12+)
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestBluetoothPermissions()
                return
            }
        }

        // Habilitar Bluetooth si está deshabilitado
        if (!bluetoothAdapter!!.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        } else {
            listPairedDevices()
            discoverNearbyDevices()
        }
    }

    private fun requestBluetoothPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        ), REQUEST_PERMISSION_BLUETOOTH)
    }

    private fun listPairedDevices() {
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        val devices = mutableListOf<BluetoothDevice>()
        pairedDevices?.forEach { device ->
            devices.add(device)
            Log.d("Bluetooth Device", "Paired Device - Nombre: ${device.name}, Dirección: ${device.address}")
        }
        deviceAdapter.setDevices(devices)
    }

    private fun discoverNearbyDevices() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
            registerReceiver(bluetoothReceiver, filter)
            bluetoothAdapter?.startDiscovery()
        } else {
            requestBluetoothPermissions()
        }
    }

    // BroadcastReceiver para dispositivos descubiertos
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                device?.let {
                    Log.d("Bluetooth Device", "Discovered Device - Nombre: ${it.name}, Dirección: ${it.address}")
                    deviceAdapter.addDevice(it)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothReceiver)
    }

    companion object {
        const val REQUEST_ENABLE_BT = 1
        const val REQUEST_PERMISSION_BLUETOOTH = 2
    }
}
