package com.example.riskreport

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StorageException
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

class DetailsReportActivity : AppCompatActivity() {
    private lateinit var ly_edit_report_by_brigadier: LinearLayout
    private lateinit var provider_ : String
    private lateinit var btnUpdateReport: TextView
    private lateinit var iv_image_preview: ImageView
    private var isReportBeingSaved = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details_report)

        ly_edit_report_by_brigadier =findViewById(R.id.ly_edit_report_by_brigadier)
        ly_edit_report_by_brigadier.visibility= View.INVISIBLE
        btnUpdateReport = findViewById(R.id.tv_btn_update_report)
        btnUpdateReport.visibility = View.INVISIBLE

        val btn_back = findViewById<ImageView>(R.id.iv_back)
        iv_image_preview = findViewById<ImageView>(R.id.iv_preview_img)
        btn_back.setOnClickListener {
            backMenu()
        }


        val bundle = intent.extras
        provider_=  bundle?.getString( "provider")!!
        val id_name=  bundle?.getString( "id")
        val type_risk=  bundle?.getString( "risk_type")
        val area_of_risk=  bundle?.getString( "area_of_risk")
        val zone_of_risk=  bundle?.getString( "zone_of_risk")
        val reported_by=  bundle?.getString( "reported_by")
        val description_risk=  bundle?.getString( "description_risk")
        val reported_at=  bundle?.getString( "reported_at")
        val status=  bundle?.getString( "status")
        val brigadier_name=  bundle?.getString( "brigadier_name")
        val revision_date=  bundle?.getString( "revision_date")
        val observation=  bundle?.getString( "observation")
        val imgUrl=  bundle?.getString( "image_url")
        val imgName = bundle?.getString("image_name")


        if (imgName != null) {
            // Carga y muestra la imagen desde Firebase Storage
            loadImageFromFirebaseStorage(type_risk!!,id_name!!,imgName)
        }

        setup(type_risk?:"",area_of_risk?:"",zone_of_risk?:"",reported_at?:"",reported_by?:"",description_risk?:"",status?:"",brigadier_name?:"",revision_date?:"",observation?:"", id_name?:"",
            provider_, imgUrl?:"", imgName?:"")
    }

    override fun onStart() {
        super.onStart()
        ly_edit_report_by_brigadier.visibility = View.VISIBLE
        btnUpdateReport.visibility = View.VISIBLE

        if (provider_ != "EMAIL") {
            ly_edit_report_by_brigadier.visibility = View.INVISIBLE
            btnUpdateReport.visibility = View.INVISIBLE
        }

    }

    fun backMenu(){
        if(provider_== "EMAIL"){
            startActivity(Intent(this, BrigadierPanelActivity::class.java))
        }else{
            startActivity(Intent(this, MainActivity::class.java))


        }
    }
    private fun setup(type_risk:String,area_of_risk:String,zone_of_risk:String,reported_at:String,reported_by:String,description_risk:String,status:String,brigadier_name:String,revision_date:String,observation:String,id:String, provider:String, imgUrl:String, imgName:String){

        btnUpdateReport.setOnClickListener {
            validation_status_button()
            updateReport(id,type_risk,area_of_risk,zone_of_risk,reported_at,reported_by,description_risk,status,brigadier_name,revision_date,observation,imgUrl)
        }
        val tv_type_risk = findViewById<TextView>(R.id.tv_type_risk)
        val tv_area_risk = findViewById<TextView>(R.id.tv_area)
        val tv_zone_risk = findViewById<TextView>(R.id.tv_zone)
        val tv_report_by = findViewById<TextView>(R.id.tv_user_reported)
        val tv_report_at = findViewById<TextView>(R.id.tv_dateAt)
        val tv_desc = findViewById<TextView>(R.id.tv_description)
        val actv_status = findViewById<AutoCompleteTextView>(R.id.actv_status_report)
        val tv_observation = findViewById<TextView>(R.id.ed_observation)


        val statusArray = resources.getStringArray(R.array.report_status)

        val status_adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, statusArray)
        actv_status.setAdapter(status_adapter)

        tv_type_risk.text = type_risk
        tv_area_risk.text =area_of_risk
        tv_zone_risk.text = zone_of_risk
        tv_report_by.text = reported_by?:"Anonimo"
        tv_report_at.text = reported_at
        tv_desc.text = description_risk?:"Sin detalles."
        actv_status.hint = status
        tv_observation.text = observation




        if(provider!= "EMAIL"){
            ly_edit_report_by_brigadier.visibility= View.INVISIBLE
            btnUpdateReport.visibility= View.INVISIBLE
        }

    }
    fun validation_status_button(){
        if (isReportBeingSaved) {
            return
        }
        Toast.makeText(baseContext, "Actualizando reporte", Toast.LENGTH_SHORT).show()
        isReportBeingSaved = true
        disableButton()
    }

    //ocultar opciones de edicion de reporte si no son brigadistas
    private fun toggleFormForBrigadier(){
        val prefs: SharedPreferences =
            getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)

        val provider = prefs.getString("provider", null)

        if (provider != null) {
            if (provider == "EMAIL") {
                ly_edit_report_by_brigadier.visibility = View.VISIBLE
                btnUpdateReport.visibility = View.VISIBLE
            }
        }
    }
    
   
    private fun updateReport(id:String,type_risk:String,area_of_risk:String,zone_of_risk:String,reported_at:String,reported_by:String,description_risk:String,status:String,brigadier_name:String,revision_date:String,observation:String, imgUrl: String){


        val type = type_risk
        val documentName = id

        val db = FirebaseFirestore.getInstance()
       val report = createReport(documentName,type_risk,area_of_risk,zone_of_risk,reported_at,reported_by,description_risk,status,brigadier_name,revision_date,observation)

        // Agregar un nuevo documento con un ID automático generado por Firebase Firestore
        db.collection(type)
            .document(documentName)
            .set(report)
            .addOnSuccessListener {
                // El informe se guardó exitosamente
                Toast.makeText(baseContext, "El reporte se actualizo exitosamente.", Toast.LENGTH_SHORT).show()

                isReportBeingSaved = false
                enableButton()
                Log.d(ContentValues.TAG, "Informe guardado con nombre de documento: $documentName")
                onBackPressed()
            }
            .addOnFailureListener { exception ->
                // Ocurrió un error al agregar el documento
                Toast.makeText(baseContext, "Ocurrio un error y el reporte no se pudo actualizar.", Toast.LENGTH_SHORT).show()

                isReportBeingSaved = false
                enableButton()

                Log.e(ContentValues.TAG, "Error al agregar el documento", exception)

            }

    }
    private fun createReport(id:String,type_risk:String,area_of_risk:String,zone_of_risk:String,reported_at:String,reported_by:String,description_risk:String,status:String,brigadier_name:String,revision_date:String,observation:String): HashMap<String, String> {
        val tv_status = findViewById<TextView>(R.id.actv_status_report)
        val tv_observation = findViewById<TextView>(R.id.ed_observation)
        val tv_name_brigadier = findViewById<TextView>(R.id.ed_name_brigadier)

        var dataRevision = SimpleDateFormat("dd/MM/yyyy").format(Date())

        val report = hashMapOf(
            "id" to id,
            "risk_type" to type_risk,
            "area_of_risk" to area_of_risk,
            "zone_of_risk" to zone_of_risk,
            "reported_by" to reported_by,
            "description_risk" to description_risk,
            "reported_at" to reported_at,
            "status" to tv_status.text.toString(),
            "brigadier_name" to tv_name_brigadier.text.toString(),
            "revision_date" to dataRevision,
            "observation" to tv_observation.text.toString(),

            )

        return report
    }

    private fun showalert(message: String){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage(message)
        builder.setPositiveButton("Aceptar", null)
        val dialog: AlertDialog = builder.create()
        dialog.show()

    }


    private fun loadImageFromFirebaseStorage(typeRisk:String,idReport:String, imageName: String) {

        val storageRef = FirebaseStorage.getInstance().reference

        val imageRef = storageRef.child("$typeRisk/$idReport/$imageName")




            val localFile = File.createTempFile("IMG_", "jpg")
            imageRef?.getFile(localFile)?.addOnSuccessListener {
                // Carga y muestra la imagen desde el archivo temporal
                val bitmap = BitmapFactory.decodeFile(localFile.toString())
                iv_image_preview.setImageBitmap(bitmap)
            }?.addOnFailureListener { exception ->

                if (exception is StorageException) {
                    iv_image_preview.setImageResource(R.mipmap.loginbged)
                }
            }

    }
    // Habilitar el botón
    private fun enableButton() {
       btnUpdateReport.isEnabled = true
    }

    // Deshabilitar el botón
    private fun disableButton() {

        btnUpdateReport.isEnabled = false
    }
}