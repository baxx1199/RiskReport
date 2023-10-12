package com.example.riskreport

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TableLayout
import android.widget.TextView
import com.facebook.login.LoginManager
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class BrigadierPanelActivity : AppCompatActivity() {

    var tableReports: TableLayout? = null
    lateinit var allRecords: MutableList<Map<String, Any>>  // Inicialización como lista vacía
    lateinit var btn_see_success_reports:TextView
    lateinit var btn_see_pendig_reports:TextView
    lateinit var btn_see_all_reports:TextView
    lateinit var tv_brigadier_name:TextView
    lateinit var provider_ :String
    private lateinit var btn_logout: ImageView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_brigadier_panel)


        val bundle: Bundle? = intent.extras
        val email: String? = bundle?.getString("email")
        provider_ ="EMAIL"
        allRecords=  mutableListOf<Map<String, Any>>()
        tableReports= findViewById(R.id.tl_historial_brig)
        tableReports?.removeAllViews()

        setup(email ?: "")

        //SESSION
        val prefs: SharedPreferences.Editor =
            getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
        prefs.putString("email", email)
        prefs.putString("provider", provider_)

        prefs.apply()

        btn_see_pendig_reports= findViewById(R.id.tv_btn_reports_pending)
        btn_see_success_reports= findViewById(R.id.tv_btn_reports_succesful)
        btn_see_all_reports= findViewById(R.id.tv_btn_allreports)

        btn_see_all_reports.setOnClickListener {
            allRecords.clear()
            tableReports?.removeAllViews()
            getReports(provider_)
        }
        btn_see_pendig_reports.setOnClickListener {
            tableReports?.removeAllViews()
            showReportsByStatus("Pendiente", allRecords)
        }
        btn_see_success_reports.setOnClickListener {
            tableReports?.removeAllViews()
            showReportsByStatus("Validado", allRecords)
        }

        btn_logout = findViewById(R.id.iv_logout)

        btn_logout.setOnClickListener {
            logOutSession(provider_)
        }


    }

    override fun onResume() {
        super.onResume()
        allRecords.clear()
        tableReports?.removeAllViews()
        getReports("EMAIL")
    }

    private fun setup(email:String){
        tv_brigadier_name = findViewById(R.id.tv_email_brigadier)
        title= "Home"
        tv_brigadier_name.text = email

        getReports(provider_)
    }

    @SuppressLint("SuspiciousIndentation")
    fun logOutSession(provider:String){

        val prefs: SharedPreferences.Editor =
            getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
        prefs.clear()
        prefs.apply()

        if(provider== "FACEBOOK"){
            LoginManager.getInstance().logOut()
        }

        FirebaseAuth.getInstance().signOut()
        startActivity(Intent(this, loginActivity::class.java))
    }

    private fun getReports(provider:String)
    {
        val typesRisk = resources.getStringArray(R.array.types_risk)
        val db = FirebaseFirestore.getInstance()

        allRecords.clear()
        val tasks = mutableListOf<Task<QuerySnapshot>>() // Lista de tareas de obtención

        for (riskType in typesRisk) {
            val task = db.collection(riskType)
                .get()
                .addOnSuccessListener { result ->
                    for (document in result) {
                        val data = document.data
                        allRecords.add(data)
                        Log.d(ContentValues.TAG, "${document.id} => $data")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.w(ContentValues.TAG, "Error getting documents for $riskType", exception)
                }

            tasks.add(task)
        }

        // Esperar a que todas las tareas de obtención se completen
            Tasks.whenAllSuccess<QuerySnapshot>(tasks)
                .addOnSuccessListener { _ ->
                    // Llamar a la función para mostrar los datos por pantalla
                    tableReports?.removeAllViews()
                    renderReports(allRecords)
                }
                .addOnFailureListener { exception ->
                    // Ocurrió un error al obtener los registros
                    Log.e(ContentValues.TAG, "Error getting documents", exception)
                }

    }

    private fun renderReports(reports: MutableList<Map<String, Any>>) {
        val uniqueReports = mutableSetOf<Map<String, Any>>()

        for (i in 0 until reports.size) {
            uniqueReports.add(reports[i])
        }

        for (report in uniqueReports) {
            val row = LayoutInflater.from(this).inflate(R.layout.row_table_reports, null, false)
            val tv_type_risk = row.findViewById<TextView>(R.id.tv_tt_type_risk)
            val tv_area_risk = row.findViewById<TextView>(R.id.tv_tt_area_risk)
            val tv_zone_risk = row.findViewById<TextView>(R.id.tv_tt_zone_risk)
            val btn_seeReport = row.findViewById<Button>(R.id.btn_see_report)

            tv_type_risk.text = "Riesgo " + report["risk_type"].toString()
            tv_area_risk.text = report["area_of_risk"].toString()
            tv_zone_risk.text = report["zone_of_risk"].toString()

            btn_seeReport.setOnClickListener {
                val detailIntent = Intent(this, DetailsReportActivity::class.java).apply {
                    putExtra("id", report["id"].toString())
                    putExtra("risk_type", report["risk_type"].toString())
                    putExtra("area_of_risk", report["area_of_risk"].toString())
                    putExtra("zone_of_risk", report["zone_of_risk"].toString())
                    putExtra("reported_by", report["reported_by"].toString())
                    putExtra("description_risk", report["description_risk"].toString())
                    putExtra("reported_at", report["reported_at"].toString())
                    putExtra("status", report["status"].toString())
                    putExtra("brigadier_name", report["brigadier_name"].toString())
                    putExtra("revision_date", report["revision_date"].toString())
                    putExtra("observation", report["observation"].toString())
                    putExtra("image_url", report["image"].toString())
                    putExtra("image_name", report["image_name"].toString())
                    putExtra("provider", "EMAIL")
                }
                startActivity(detailIntent)
            }

            tableReports?.addView(row)
        }
    }


    private fun showReportsByStatus(status: String, reports: MutableList<Map<String, Any>>) {

        val filteredReports = reports.filter { it["status"] == status } // Filtra los reportes según el estado

        renderReports(filteredReports.toMutableList())
    }
}