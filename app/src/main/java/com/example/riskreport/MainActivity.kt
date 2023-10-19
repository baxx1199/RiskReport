package com.example.riskreport

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TableLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.riskreport.databinding.ActivityMainBinding
import com.facebook.login.LoginManager
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    var tableReports:TableLayout? = null
    lateinit var provider_ :String
    private lateinit var btn_logout:ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bundle: Bundle? = intent.extras
        val email: String? = bundle?.getString("email")
        val provider: String? = bundle?.getString("provider")

        tableReports= findViewById(R.id.tl_historial)
        tableReports?.removeAllViews()

        btn_logout = findViewById(R.id.iv_logOut)

       btn_logout.setOnClickListener {
            logOutSession(provider_)
        }

        setup(email ?: "", provider ?: "")

        //SESSION
        val prefs: Editor =
            getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
        prefs.putString("email", email)
        prefs.putString("provider", provider)

        prefs.apply()



        /*val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)*/
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        /* val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)*/
    }

    override fun onResume() {
        super.onResume()
        tableReports?.removeAllViews()
        getReports(provider_)
    }
    private fun setup(email:String, provider:String){
        val tv_name_main = findViewById<TextView>(R.id.tv_name_main)
        title= "Home"
        tv_name_main.text = "Aprendiz"
        provider_=provider

        getReports(provider)
    }


    @SuppressLint("SuspiciousIndentation")
    fun logOutSession(provider:String){

        val prefs: Editor =
            getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
            prefs.clear()
            prefs.apply()

        if(provider== "FACEBOOK"){
            LoginManager.getInstance().logOut()
        }

        FirebaseAuth.getInstance().signOut()
        startActivity(Intent(this, loginActivity::class.java))
    }

    fun newReport(view: View){
        goToNewReport()
    }

    private fun goToNewReport(){
        val newreportintent = Intent(this, NewReporActivity::class.java)
        startActivity(newreportintent)
    }

    private fun getReports(provider:String)
    {
        val typesRisk = resources.getStringArray(R.array.types_risk)
        val db = FirebaseFirestore.getInstance()

        val allRecords = mutableListOf<Map<String, Any>>() // Lista para almacenar todos los registros
        val tasks = mutableListOf<Task<QuerySnapshot>>() // Lista de tareas de obtención

        for (riskType in typesRisk) {
            val task = db.collection(riskType)
                .get()
                .addOnSuccessListener { result ->
                    for (document in result) {
                        val data = document.data
                        allRecords.add(data) // Agregar el registro a la lista
                        Log.d(TAG, "${document.id} => $data")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.w(TAG, "Error getting documents for $riskType", exception)
                }

            tasks.add(task)
        }

        // Esperar a que todas las tareas de obtención se completen
        Tasks.whenAllSuccess<QuerySnapshot>(tasks)
            .addOnSuccessListener { _ ->
                // Llamar a la función para mostrar los datos por pantalla
                tableReports?.removeAllViews()
                renderReports(allRecords, provider)
            }
            .addOnFailureListener { exception ->
                // Ocurrió un error al obtener los registros
                Log.e(TAG, "Error getting documents", exception)
            }

    }

    private fun renderReports(reports:MutableList<Map< String, Any>>,provider: String){

        for (i in (0 until reports.size)){
            val row = LayoutInflater.from(this).inflate(R.layout.row_table_reports, null, false)
            val tv_type_risk = row.findViewById<TextView>(R.id.tv_tt_type_risk)
            val tv_area_risk = row.findViewById<TextView>(R.id.tv_tt_area_risk)
            val tv_zone_risk = row.findViewById<TextView>(R.id.tv_tt_zone_risk)
            val btn_seeReport= row.findViewById<Button>(R.id.btn_see_report)

            tv_type_risk.text = "Riesgo "+reports[i].get("risk_type").toString()
            tv_area_risk.text = reports[i].get("area_of_risk").toString()
            tv_zone_risk.text = reports[i].get("zone_of_risk").toString()


            btn_seeReport.setOnClickListener {
                val detailIntent = Intent(this, DetailsReportActivity::class.java).apply {
                    putExtra( "id" , reports[i].get("id").toString())
                    putExtra( "risk_type" , reports[i].get("risk_type").toString())
                    putExtra("area_of_risk", reports[i].get("area_of_risk").toString())
                    putExtra("zone_of_risk", reports[i].get("zone_of_risk").toString())
                    putExtra("reported_by", reports[i].get("reported_by").toString())
                    putExtra("description_risk" , reports[i].get("description_risk").toString())
                    putExtra("reported_at", reports[i].get("reported_at").toString())
                    putExtra("status", reports[i].get("status").toString())
                    putExtra("brigadier_name", reports[i].get("brigadier_name").toString())
                    putExtra("revision_date", reports[i].get("revision_date").toString())
                    putExtra("observation", reports[i].get("observation").toString())
                    putExtra("image_url", reports[i].get("image").toString())
                    putExtra("image_name", reports[i].get("image_name").toString())

                    if(provider == "EMAIL"){
                        putExtra("provider", "EMAIL")
                    }else{
                        putExtra("provider", provider_?: "ANONIMO")
                    }
                }
                startActivity(detailIntent)
            }

            tableReports?.addView(row)
        }
    }
}