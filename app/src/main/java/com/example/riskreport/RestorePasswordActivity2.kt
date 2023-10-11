package com.example.riskreport

import android.content.ContentValues
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth

class RestorePasswordActivity2 : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth;
    private lateinit var et_email: EditText;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restore_password)

        et_email = findViewById<EditText>(R.id.et_email)

        mAuth = FirebaseAuth.getInstance()

        val btn_send_email = findViewById<TextView>(R.id.tv_btn_send_email)
        val btn_back_login = findViewById<TextView>(R.id.tv_btn_back_login)
        btn_send_email.setOnClickListener {
           restore_password()
        }
        btn_back_login.setOnClickListener {
            startActivity(Intent(this, loginActivity::class.java))
        }
    }
    fun restore_password(){
        sendEmailRestore()
    }
    private fun sendEmailRestore(){
        val email = et_email.text.toString()

        if(email.isNotBlank() && email.isNotEmpty()){
            mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if(task.isSuccessful){
                        Toast.makeText(baseContext, "Se ha enviado un correo de verificación a tu cuenta.", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, loginActivity::class.java))
                    }
                    else{
                        Toast.makeText(baseContext, "Verifica que el correo sea correcto. No se pudo enviar el correo.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { task ->
                    Toast.makeText(baseContext, "No se pudo verificar el correo intenta nuevamente.", Toast.LENGTH_SHORT).show()

                    Log.w(ContentValues.TAG, "Error to send message", task)
                }

        }else{
            Toast.makeText(baseContext, "Debes ingresar un correo para recuperar la contraseña", Toast.LENGTH_SHORT).show()
        }

    }
}