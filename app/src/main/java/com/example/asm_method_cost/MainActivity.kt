package com.example.asm_method_cost

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Test().test()
        setContentView(R.layout.activity_main)
        mainTest(100,"ms")
        objectReturn(100,"ms",Person("hoop"))
    }

    private fun objectReturn(i: Int, s: String, person: Person):Person {
        Thread.sleep(80)
        return Person("zeng")
    }

    private fun mainTest(delay:Long,unit:String) :Boolean{
        Thread.sleep(delay)
        return true
    }

}

data class Person(val name:String)