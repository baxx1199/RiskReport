package com.example.riskreport

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NewReporActivity : AppCompatActivity() {
    private var isReportBeingSaved = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_repor)

        val btn_back = findViewById<ImageView>(R.id.iv_back_menu)

        btn_back.setOnClickListener {
            backMenu()

        }

        val btn_take_picture = findViewById<ImageView>(R.id.iv_btn_camera)

        val actv_types = findViewById<AutoCompleteTextView>(R.id.actv_type_risk)
        val actv_areas = findViewById<AutoCompleteTextView>(R.id.actv_area)
        val actv_zona = findViewById<AutoCompleteTextView>(R.id.actv_zone)


        val riskTypes = resources.getStringArray(R.array.types_risk)
        val senaAreas = resources.getStringArray(R.array.sena_areas)

        val risk_types_adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, riskTypes)
        val sena_areas_adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, senaAreas)

        actv_types.setAdapter(risk_types_adapter)
        actv_areas.setAdapter(sena_areas_adapter)

        actv_areas.setOnItemClickListener { _, _, _, _  ->


            val areaSeleccionada = actv_areas.text.toString()

            // Buscar la posición del área seleccionada en el array de áreas
            val posicionArea = senaAreas.indexOf(areaSeleccionada)

            Log.d("Seleccion", "Posición seleccionada: $posicionArea")
            // Obtener el área seleccionada

            // Obtener la lista de lugares disponibles según el área seleccionada
            val lugaresDisponibles = when (posicionArea) {
                0 -> resources.getStringArray(R.array.zonas_auditorio)
                1 -> resources.getStringArray(R.array.zonas_administracion)
                2 -> resources.getStringArray(R.array.zonas_entrada)
                3 -> resources.getStringArray(R.array.zonas_zona_deportiva)
                4 -> resources.getStringArray(R.array.zonas_lacteos)
                5 -> resources.getStringArray(R.array.zonas_biblioteca)
                6 -> resources.getStringArray(R.array.zonas_bloque_a)
                7 -> resources.getStringArray(R.array.zonas_bloque_b)
                8 -> resources.getStringArray(R.array.zonas_bloque_c)
                9 -> resources.getStringArray(R.array.zonas_internado_masculino)
                10 -> resources.getStringArray(R.array.zonas_internado_femenino)
                11 -> resources.getStringArray(R.array.zonas_centro_convivencia)
                12 -> resources.getStringArray(R.array.zonas_gastronomia)
                13 -> resources.getStringArray(R.array.zonas_zona_agrobiotecnologica)
                else -> arrayOf() // Manejar otros casos según tus necesidades
            }

            val zonaAdapter = ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line,lugaresDisponibles)
            actv_zona.setAdapter(zonaAdapter)

        }

        val btnSendReport = findViewById<TextView>(R.id.tv_btn_send_report)
        btnSendReport.setOnClickListener { sendReport() }

        btn_take_picture.setOnClickListener {
            dispacthCameraIntent()
        }
    }

    private fun generateDocumentName(area: String, zona: String): String {
        // Genera un ID único para el documento (puedes usar la hora actual, por ejemplo)
        val uniqueId = SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.getDefault()).format(Date())

        // Concatena el nombre del área, el nombre de la zona y el ID único
        return "$area-$zona-$uniqueId"
    }

    fun sendReport(){
        if (isReportBeingSaved) {
            return
        }
        Toast.makeText(baseContext, "Guardando reporte", Toast.LENGTH_SHORT).show()
        isReportBeingSaved = true
        disableButton()
        saveReport()
    }
    private fun saveReport(){

        val actv_types = findViewById<AutoCompleteTextView>(R.id.actv_type_risk)
        val actv_areas = findViewById<AutoCompleteTextView>(R.id.actv_area)
        val actv_zona = findViewById<AutoCompleteTextView>(R.id.actv_zone)

        val areaOfRisk = actv_areas.text.toString()
        val zonaOfRisk = actv_zona.text.toString()

        val typeOfRisk = actv_types.text.toString()
        val area_of_risk = actv_areas.text.toString()
        val zone_of_risk = actv_zona.text.toString()
        // Validar que las variables no sean nulas o cadenas vacías
        if (typeOfRisk.isEmpty() || area_of_risk.isEmpty() || zone_of_risk.isEmpty() || fileImage == null) {
            Toast.makeText(baseContext, "No ser guardo el reporte, Asegúrate de completar todos los campos y adjuntar una imagen.", Toast.LENGTH_SHORT).show()
            isReportBeingSaved = false
            enableButton()
            return
        }
        // Obtén los tipos de riesgo permitidos y las áreas permitidas desde los arrays de recursos
        val allowedRiskTypes = resources.getStringArray(R.array.types_risk)
        val allowedAreas = resources.getStringArray(R.array.sena_areas)

        // Verifica si el tipo de riesgo y el área seleccionados son válidos
        if (!allowedRiskTypes.contains(typeOfRisk)) {
            // Muestra un mensaje de error y no permitas la creación del reporte
            Toast.makeText(baseContext, "El tipo de riesgo seleccionado no es válido", Toast.LENGTH_SHORT).show()
            isReportBeingSaved = false
            enableButton()

            return
        }

        if (!allowedAreas.contains(areaOfRisk)) {
            // Muestra un mensaje de error y no permitas la creación del reporte
            Toast.makeText(baseContext, "El área seleccionada no es válida", Toast.LENGTH_SHORT).show()
            isReportBeingSaved = false
            enableButton()

            return
        }


        val documentName = generateDocumentName(areaOfRisk, zonaOfRisk)

        val db = FirebaseFirestore.getInstance()
        val nameCollectionDb = actv_types.text.toString()
        val report = createReport(documentName)

        // Agregar un nuevo documento con un ID automático generado por Firebase Firestore
        db.collection(nameCollectionDb)
            .document(documentName)
            .set(report)
            .addOnSuccessListener {
                // El informe se guardó exitosamente
                Toast.makeText(baseContext, "El reporte se guardo exitosamente.", Toast.LENGTH_SHORT).show()

                isReportBeingSaved = false
                enableButton()
                val imageBitmap = BitmapFactory.decodeFile(fileImage.toString())
                uploadImageToStorage(nameCollectionDb, documentName, imageBitmap)
            }
            .addOnFailureListener { exception ->
                // Ocurrió un error al agregar el documento
                Toast.makeText(baseContext, "Ocurrio un error y el reporte no se pudo guardar.", Toast.LENGTH_SHORT).show()

                isReportBeingSaved = false
                enableButton()

                Log.e(TAG, "Error al agregar el documento", exception)
                // Manejar el error según tus necesidades
            }



    }

    private fun createReport(id:String): HashMap<String, String> {

        val actv_types = findViewById<AutoCompleteTextView>(R.id.actv_type_risk)
        val actv_areas = findViewById<AutoCompleteTextView>(R.id.actv_area)
        val actv_zona = findViewById<AutoCompleteTextView>(R.id.actv_zone)
        val ed_username = findViewById<TextView>(R.id.ed_user_name_reported)
        val ed_description = findViewById<TextView>(R.id.ed_description)

        val dataRegister = SimpleDateFormat("dd/MM/yyyy").format(Date())
        val typeOfRisk = actv_types.text.toString()
        val area_of_risk = actv_areas.text.toString()
        val zone_of_risk = actv_zona.text.toString()
        var reported_by = ed_username.text.toString()

        if (reported_by.isEmpty()) {
            reported_by = ""
        }

        var description = ed_description.text.toString()
        if (description.isEmpty()) {
            description = ""
        }

        val report = hashMapOf(
            "id" to id,
            "image" to "", //debe ser una referencia a la ubicacion de la foto en firebase storage para poder recuperarla
            "risk_type" to typeOfRisk,
            "area_of_risk" to area_of_risk,
            "zone_of_risk" to zone_of_risk,
            "reported_by" to reported_by,
            "description_risk" to description,
            "reported_at" to dataRegister,
            /*"location" to "",*/
            "status" to "Pendiente",
            "brigadier_name" to "",
            "revision_date" to "",
            "observation" to "",

            )

        return report
    }


    fun dispacthCameraIntent(){
        val intetn= Intent(MediaStore.ACTION_IMAGE_CAPTURE).also {
            it.resolveActivity(packageManager).also{ component ->
                createPhotoFile()
                val photoUrl: Uri = FileProvider.getUriForFile(this, "com.example.riskreport.fileprovider",fileImage)
                it.putExtra(MediaStore.EXTRA_OUTPUT, photoUrl)
            }
        }
        startForResult.launch(intetn)
    }

    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        result ->
        if(result.resultCode == Activity.RESULT_OK){
           // val intent = result.data
           // val imageBitmap = intent?.extras?.get("data")as Bitmap
            val imageBitmap = BitmapFactory.decodeFile(fileImage.toString())
            val imagePreview = findViewById<ImageView>(R.id.iv_preview_img)
            imagePreview.setImageBitmap(imageBitmap)

        }
    }
    private lateinit var fileImage:File
    private fun createPhotoFile(){
        val dir =getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
       fileImage = File.createTempFile(
            "IMG_${System.currentTimeMillis()}_",
            ".jpg",
            dir
        )

    }

    //guardando la imagen en el firebase storage

    private fun uploadImageToStorage(collectionName: String, documentName: String, imageBitmap: Bitmap) {
        // Convierte la imagen capturada en un ByteArray
        val baos = ByteArrayOutputStream()
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val imageData = baos.toByteArray()

        // Genera un nombre único para la imagen
        val uniqueId = SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.getDefault()).format(Date())
        val imageName = "image_$uniqueId.jpg"

        // Referencia al almacenamiento de Firebase Storage
        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("$collectionName/$documentName/$imageName")

        // Sube la imagen a Firebase Storage
        val uploadTask = imageRef.putBytes(imageData)
        uploadTask.addOnSuccessListener { taskSnapshot ->
            // La imagen se ha subido correctamente, ahora puedes obtener la URL de descarga
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                val imageUrl = uri.toString()

                // Actualiza el documento del informe con la referencia de la imagen y el nombre de la imagen
                updateReportImageReference(collectionName, documentName, imageUrl, imageName)
            }
        }.addOnFailureListener { exception ->
            // Maneja el error en caso de que la subida de la imagen falle
            Log.e(TAG, "Error al subir la imagen a Firebase Storage", exception)
        }
    }

    private fun updateReportImageReference(collectionName: String, documentName: String, imageUrl: String, imageName: String) {
        val db = FirebaseFirestore.getInstance()
        val reportRef = db.collection(collectionName).document(documentName)

        val updateData = hashMapOf(
            "image" to imageUrl,
            "image_name" to imageName
        )

        // Actualiza el campo "image" y "image_name" del documento del informe
        reportRef
            .update(updateData as Map<String, Any>)
            .addOnSuccessListener {
                Log.d(TAG, "Referencia de imagen actualizada en el documento del informe")
                onBackPressed()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error al actualizar la referencia de imagen en el documento del informe", exception)

            }
    }

    fun backMenu(){

            startActivity(Intent(this, MainActivity::class.java))
    }

    // Habilitar el botón
    private fun enableButton() {
        val btnSendReport = findViewById<TextView>(R.id.tv_btn_send_report)
        btnSendReport.isEnabled = true
    }

    // Deshabilitar el botón
    private fun disableButton() {
        val btnSendReport = findViewById<TextView>(R.id.tv_btn_send_report)
        btnSendReport.isEnabled = false
    }

}