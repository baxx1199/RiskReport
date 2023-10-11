package com.example.riskreport

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.properties.Delegates

enum class providersType{
    EMAIL,
    GOOGLE,
    FACEBOOK,
    ANONIMO
}
class loginActivity : AppCompatActivity() {
    companion object{
        lateinit var userEmail:String;
        lateinit var providerSession:String;
    }
    private var email by Delegates.notNull<String>();
    private var password by Delegates.notNull<String>();
    private lateinit var etEmail: EditText;
    private lateinit var etPassword: EditText;
    private lateinit var tv_forgott_pass: TextView;
   // private lateinit var lvTerm: LinearLayout;

    private lateinit var mAuth:FirebaseAuth;

    private val GOOGLE_SIGN_IN = 1010
    private val callbackManager = CallbackManager.Factory.create()


    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_RiskReport)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)


      /*  lvTerm =findViewById(R.id.ly_layaout_terms)
        lvTerm.visibility= View.INVISIBLE*/

        etEmail=findViewById(R.id.et_email)
        etPassword=findViewById(R.id.et_password)
        tv_forgott_pass =findViewById(R.id.tv_forgott_password)
        mAuth = FirebaseAuth.getInstance()

        tv_forgott_pass.setOnClickListener {
            forgott_password()
        }
        verifiedSession()
    }

    fun forgott_password(){
        resetPassword()
    }
    fun login(view: View){
        loginUser()
    }
    fun loginAnonimo(view: View){
        logInAnonimo()
    }
    fun loginWithGoogle(view: View){
        loginUserGoogle()
    }
    fun loginWithFacebook(view: View){
        loginUserFacebook()
    }

    private fun loginUser(){
        email = etEmail.text.toString();
        password = etPassword.text.toString();

        if(email.isNotBlank() && email.isNotEmpty() && password.isNotBlank() && password.isNotEmpty()){

            mAuth.signInWithEmailAndPassword(email,password)
                .addOnCompleteListener(this) {task->
                    if(task.isSuccessful) {
                        goBrigadierHome(email,"EMAIL")
                    }
                    else{
                        showalert("Usuario o contraseña incorrectos.")
                        /*if(lvTerm.visibility==View.INVISIBLE){
                            lvTerm.visibility=View.VISIBLE
                        }
                        else {
                            val ckTerms = findViewById<CheckBox>(R.id.cb_terms)
                            if(ckTerms.isChecked) register()
                        }*/
                    }
                };
        }else{
            showalert("Debes ingresar un correo y contraseña valida.")
        }


    }

    private fun goBrigadierHome(email:String, provider:String){
        userEmail=email
        providerSession = provider

        val intent = Intent(this, BrigadierPanelActivity::class.java).apply {
            putExtra("email" ,email)
            putExtra("provider", providerSession)
        }
        startActivity(intent)
    }

    private fun goUserHome(email:String, provider:String){
        userEmail=email
        providerSession = provider

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("email" ,email)
            putExtra("provider", providerSession)
        }
        startActivity(intent)
    }

    private fun register(){
        email = etEmail.text.toString();
        password = etPassword.text.toString();

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email,password)
            .addOnCompleteListener(this) {task->
                println(task.isSuccessful)
                if(task.isSuccessful){
                            var dataRegister = SimpleDateFormat("dd/MM/yyyy").format(Date())
                            //var  dbAccess = FirebaseFirestore.getInstance()
                            val db = Firebase.firestore
                    println(db.toString())
                            val user = hashMapOf(
                                "user" to email,
                                "date-register" to dataRegister
                            )

                            db.collection("users")
                                .add(user)
                                .addOnSuccessListener { documentReference ->
                                    Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
                                }
                                .addOnFailureListener { e ->
                                    Log.w(TAG, "Error adding document", e)
                                }
                            goBrigadierHome(email,"EMAIL")
                    }
                     else Toast.makeText(this, "Error, Ocurrio un error durante la creación del usuario.", Toast.LENGTH_SHORT).show()
            }


    }

    private fun showalert(message: String){
        val builder =AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage(message)
        builder.setPositiveButton("Aceptar", null)
        val dialog: AlertDialog= builder.create()
        dialog.show()

    }

    private fun logInAnonimo(){
        val homeintent = Intent(this, MainActivity::class.java).apply {
            putExtra("email" ,"")
            putExtra("provider", "ANONIMO")
        }
        startActivity(homeintent)
    }

    private fun loginUserFacebook(){
        LoginManager.getInstance().logInWithReadPermissions(this, listOf("email"))
        LoginManager.getInstance().registerCallback(callbackManager,
            object: FacebookCallback<LoginResult>{
                override fun onSuccess(result: LoginResult?) {
                    result?.let {
                        val token = it.accessToken

                        val credential = FacebookAuthProvider.getCredential(token.token)
                        FirebaseAuth.getInstance().signInWithCredential(credential)
                            .addOnCompleteListener {
                                if (it.isSuccessful) {
                                    goUserHome(it.result?.user?.email ?: "", "FACEBOOK")
                                } else {
                                    showalert("Se ha producido un error en la autentificación mediante facebook.")
                                }
                            }
                    }
                }

                override fun onCancel() {

                }

                override fun onError(error: FacebookException?) {
                    showalert("Se ha producido un error en la conexion con el servicio de Facebook.")
                }
            })
    }

    private fun loginUserGoogle(){
        val googleConf = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id))
            .requestEmail()
            .build()
        val googleClient = GoogleSignIn.getClient(this, googleConf)
        googleClient.signOut()

        startActivityForResult(googleClient.signInIntent, GOOGLE_SIGN_IN)

    }

    private fun verifiedSession(){
        val prefs: SharedPreferences =
            getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
        val email = prefs.getString("email", null)
        val provider = prefs.getString("provider", null)

        if(email != null && provider != null){

            if(provider == "EMAIL") {
                goBrigadierHome(email, provider)
            }else {
                goUserHome(email, provider)
            }

        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)

        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == GOOGLE_SIGN_IN){
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)

            try {

                val account = task.getResult(ApiException::class.java)

                if(account!= null){
                    val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                    FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener {
                        if(it.isSuccessful){
                            goUserHome(account.email?:"", "GOOGLE" )
                        }else{
                            showalert("Se ha producido un error en la autentificación.")
                        }
                    }
                }
            }catch (e: ApiException){
                showalert("Error al obtener el Account")
            }

        }
    }



    private fun resetPassword(){
            startActivity(Intent(this, RestorePasswordActivity2::class.java))
    }

}