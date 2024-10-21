import android.bluetooth.*
import android.content.Context
import android.os.Environment
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.*

object BluetoothService {

    private var bluetoothGatt: BluetoothGatt? = null
    private var selectedDevice: BluetoothDevice? = null

    // UUIDs de servicios y características (debes ajustarlos según tus necesidades)
    private lateinit var UUIDServiceTX: UUID
    private lateinit var UUIDCharTX: UUID
    private lateinit var UUIDServiceRX: UUID
    private lateinit var UUIDCharRX: UUID

    // Variables para las características
    private var rxCharacteristic: BluetoothGattCharacteristic? = null
    private var txCharacteristic: BluetoothGattCharacteristic? = null

    // Variables observables
    var meters: String = "0"
        set(value) {
            field = value
            notifyDataChanged()
        }

    var deviceConnected: String = "none"
        set(value) {
            field = value
            notifyDataChanged()
        }

    var sat: String = "0"
        set(value) {
            field = value
            notifyDataChanged()
        }

    var action: String = "none"
        set(value) {
            field = value
            notifyDataChanged()
        }

    // Variables para manejar los vuelos y coordenadas
    private val coordinatesMap: MutableMap<String, MutableList<Pair<Double, Double>>> = mutableMapOf()
    private var currentFlyName: String = ""
    private var totalFlyCount = 0
    private var currentFlyIndex = 1

    private var partialDataBuffer: MutableMap<String, String> = mutableMapOf(
        "flyName" to "",
        "coordinates" to "",
        "numLines" to ""
    )

    // Observadores
    private val observers = mutableListOf<() -> Unit>()

    fun addObserver(observer: () -> Unit) {
        observers.add(observer)
    }

    fun removeObserver(observer: () -> Unit) {
        observers.remove(observer)
    }

    private fun notifyDataChanged() {
        observers.forEach { it() }
    }

    fun selectDevice(device: BluetoothDevice) {
        selectedDevice = device
    }

    fun connectToDevice(context: Context, callback: BluetoothGattCallback) {
        selectedDevice?.let { device ->
            bluetoothGatt = device.connectGatt(context, false, callback)
        } ?: run {
            Toast.makeText(context, "No se ha seleccionado ningún dispositivo", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendData(value: String) {
        val valueBytes = value.toByteArray()

        bluetoothGatt?.let { gatt ->
            val service = gatt.getService(UUIDServiceTX)
            if (service != null) {
                val txCharacteristic = service.getCharacteristic(UUIDCharTX)

                if (txCharacteristic != null) {
                    txCharacteristic.value = valueBytes
                    if (txCharacteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) {
                        val success = gatt.writeCharacteristic(txCharacteristic)
                        if (!success) {
                            Log.e("BluetoothService", "Error al iniciar la escritura")
                        } else {
                            return
                        }
                    } else {
                        Log.e("BluetoothService", "La característica TX no admite escritura")
                    }
                } else {
                    Log.e("BluetoothService", "Característica TX no encontrada")
                }
            } else {
                Log.e("BluetoothService", "Servicio TX no encontrado")
            }
        } ?: run {
            Log.e("BluetoothService", "BluetoothGatt no disponible")
        }
    }

    fun disconnect() {
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    // Método para manejar la configuración de notificaciones en la característica RX
    fun setupNotifications() {
        bluetoothGatt?.let { gatt ->
            rxCharacteristic?.let { characteristic ->
                gatt.setCharacteristicNotification(characteristic, true)
                val descriptor = characteristic.getDescriptor(
                    UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                )
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
            } ?: run {
                Log.e("BluetoothService", "Característica RX no configurada para notificaciones")
            }
        }
    }

    val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i("BluetoothService", "Conectado a ${gatt?.device?.name}")

                    gatt?.device?.let { device ->
                        if (device.name == "M5Stack_BLE") {
                            Log.i("BluetoothService", "Cambiando nombre del dispositivo a Aeroagri Device")
                            deviceConnected = "Aeroagri Device"
                        }
                    }

                    gatt?.discoverServices()
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i("BluetoothService", "Desconectado")
                    disconnect()
                }

                else -> {
                    Log.e("BluetoothService", "Error de conexión: $status")
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                gatt?.let {
                    // Variables para almacenar las características TX y RX
                    var rxCharacteristic: BluetoothGattCharacteristic? = null
                    var txCharacteristic: BluetoothGattCharacteristic? = null

                    // Iterar sobre los servicios descubiertos
                    for (service in it.services) {
                        println("Servicio descubierto: ${service.uuid}")

                        // Iterar sobre las características del servicio
                        for (characteristic in service.characteristics) {
                            println("Característica descubierta: ${characteristic.uuid}")

                            // Verificar si es la característica TX
                            if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) {
                                txCharacteristic = characteristic
                                UUIDCharRX =
                                    characteristic.uuid  // Asignar la característica TX a la variable global
                                UUIDServiceRX =
                                    service.uuid  // Asignar el servicio a la variable global
                                println("  -> Característica TX encontrada")
                            }
                            // Verificar si es la característica RX
                            if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0 ||
                                characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0
                            ) {
                                rxCharacteristic = characteristic
                                UUIDCharTX =
                                    characteristic.uuid  // Asignar la característica RX a la variable global
                                UUIDServiceTX =
                                    service.uuid  // Asignar el servicio a la variable global
                                println("  -> Característica RX encontrada")
                            }
                        }

                        // Si encontramos tanto RX como TX, configurar notificaciones y salir del bucle
                        if (rxCharacteristic != null && txCharacteristic != null) {
                            // Habilitar notificaciones en RX
                            gatt.setCharacteristicNotification(rxCharacteristic, true)

                            val descriptor = rxCharacteristic.getDescriptor(
                                UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")  // UUID del descriptor de notificación
                            )
                            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            gatt.writeDescriptor(descriptor)
                            break  // Salir del bucle una vez que hemos encontrado lo que necesitamos
                        }
                    }
                }
            }
        }

        /*
        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
    super.onCharacteristicChanged(gatt, characteristic)
    characteristic?.let {
        val value = it.value // Valor en bytes recibido
        val stringValue = String(value) // Convertir a string
        Log.i("BluetoothService", "Datos recibidos: $stringValue")

        // Acumular los datos recibidos para formar un mensaje completo
        incomingDataBuffer.append(stringValue)

        // Verificar si el mensaje completo ha sido recibido (basado en algún delimitador o longitud conocida)
        if (incomingDataBuffer.contains(":25")) { // Esto es solo un ejemplo; ajusta según tus necesidades
            val fullMessage = incomingDataBuffer.toString()
            Log.i("BluetoothService", "Mensaje completo: $fullMessage")

            // Limpiar el buffer después de procesar el mensaje completo
            incomingDataBuffer.clear()

            val values = fullMessage.split(",") // Separar por comas

            if (values.size >= 3) {
                try {
                    // Asignar los valores
                    meters = values[0]
                    sat = values[1]
                    action = values[2]

                    if (action == "cotecia") {
                        deviceConnected = action
                        action = "none"
                    }
                } catch (e: NumberFormatException) {
                    // Manejar la excepción si los valores no se pueden convertir a entero
                    Log.i("BluetoothService", "Error al convertir los valores: ${e.message}")
                }
            }
        }
    }
}
        * */

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            super.onCharacteristicChanged(gatt, characteristic)
            characteristic?.let {
                val value = it.value // Valor en bytes recibido
                val stringValue = String(value) // Convertir a string
                Log.i("BluetoothService", "Datos recibidos: $stringValue")

                // Separar los valores por comas
                val values = stringValue.split(",")

                // Manejo de las diferentes partes del mensaje
                when {
                    stringValue.startsWith("VUELO - ") -> {
                        partialDataBuffer["flyName"] = stringValue.trim()
                    }
                    values.size == 2 && isValidCoordinate(values) -> {
                        partialDataBuffer["coordinates"] = stringValue.trim()
                    }
                    values.size == 1 && stringValue.startsWith("n:") -> {
                        partialDataBuffer["numLines"] = stringValue.trim()
                        processFlyData(partialDataBuffer)
                        partialDataBuffer.clear()
                    }
                    values.size == 3 -> {
                        processOtherData(values)
                    }
                    else -> {
                        Log.w("BluetoothService", "Formato de datos desconocido: $stringValue")
                    }
                }
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("BluetoothService", "Valor escrito exitosamente")
            } else {
                val errorMessage = when (status) {
                    BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> "Escritura no permitida"
                    BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION -> "Autenticación insuficiente"
                    BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED -> "Solicitud no soportada"
                    BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION -> "Encriptación insuficiente"
                    else -> "Error desconocido: $status"
                }
                Log.e("BluetoothService", "Error al escribir característica: $errorMessage")
            }
        }
    }

    private fun isValidCoordinate(values: List<String>): Boolean {
        return values.size == 2 && values[0].toDoubleOrNull() != null && values[1].toDoubleOrNull() != null
    }

    private fun processFlyData(data: MutableMap<String, String>) {
        val flyName = data["flyName"] ?: return
        val coordinatesString = data["coordinates"] ?: return
        val numLinesString = data["numLines"] ?: return

        val coordinates = coordinatesString.split(",")
        if (coordinates.size == 2) {
            val latitude = coordinates[0].toDoubleOrNull()
            val longitude = coordinates[1].toDoubleOrNull()
            val numLines = numLinesString.split(":")[1].toIntOrNull() ?: 0

            if (latitude != null && longitude != null) {
                addCoordinate(latitude, longitude, flyName)

                // Si hemos recibido todas las líneas esperadas, guardar en KML
                if (coordinatesMap[flyName]?.size == numLines) {
                    saveCoordinatesToKML()
                }
            }
        }
    }

    private fun processOtherData(values: List<String>) {
        if (values.size == 3) {
            try {
                // Asignar los valores
                meters = values[0]
                sat = values[1]
                action = values[2]

                if (action == "cotecia") {
                    deviceConnected = action
                    action = "none"
                }

            } catch (e: NumberFormatException) {
                // Manejar la excepción si los valores no se pueden convertir a entero
                Log.i("BluetoothService", "Error al convertir los valores: ${e.message}")
            }
        }
    }

    private fun addCoordinate(latitude: Double, longitude: Double, flyName: String) {
        val coordinates = coordinatesMap.getOrPut(flyName) { mutableListOf() }
        coordinates.add(latitude to longitude)
    }

    private fun saveCoordinatesToKML() {
        val fileName = "vuelos_coordenadas.kml"
        val kmlContent = StringBuilder()

        kmlContent.append("""
        <?xml version="1.0" encoding="UTF-8"?>
        <kml xmlns="http://www.opengis.net/kml/2.2">
        <Document>
        <name>Vuelos Coordenadas</name>
    """.trimIndent())

        for ((flyName, coordinates) in coordinatesMap) {
            coordinates.forEach { (latitude, longitude) ->
                kmlContent.append("""
                <Placemark>
                    <name>$flyName</name>
                    <Point>
                        <coordinates>$longitude,$latitude,0</coordinates>
                    </Point>
                </Placemark>
            """.trimIndent())
            }
        }

        kmlContent.append("""
        </Document>
        </kml>
    """.trimIndent())

        // Guardar el archivo KML
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File("$downloadsDir/$fileName")
            val outputStream = FileOutputStream(file)
            val writer = OutputStreamWriter(outputStream)
            writer.write(kmlContent.toString())
            writer.flush()
            writer.close()
            Log.i("BluetoothService", "Archivo KML guardado en: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("BluetoothService", "Error al guardar el archivo KML: ${e.message}")
        }
    }
}