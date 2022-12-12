package com.sangyan.todo

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.sangyan.todo.databinding.FragmentHomeBinding
import com.sangyan.todo.utils.ToDoAdapter
import com.sangyan.todo.utils.ToDoData


class FragmentHome : Fragment(), AddToDoFragment.DialogNextBtnClickListener,
    ToDoAdapter.ToDoAdapterClickInterface {
    private lateinit var auth : FirebaseAuth
    private lateinit var databaseRef : DatabaseReference
    private lateinit var navController: NavController
    private lateinit var binding : FragmentHomeBinding
    private  var popupFragment  : AddToDoFragment?=null
    private lateinit var adapter : ToDoAdapter
    private lateinit var mList : MutableList<ToDoData>


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
         binding = FragmentHomeBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init(view)
        getDataFromFirebase()
        registerEvents()
    }

    private fun registerEvents() {
        binding.addButtonHome.setOnClickListener{
            if(popupFragment!=null)
                childFragmentManager.beginTransaction().remove(popupFragment!!).commit()
           popupFragment = AddToDoFragment()
            popupFragment!!.setListener(this)
            popupFragment!!.show(childFragmentManager,
            AddToDoFragment.TAG)
        }
    }

    private fun init(view: View) {
            navController = Navigation.findNavController(view)
        auth = FirebaseAuth.getInstance()
        databaseRef = FirebaseDatabase.getInstance().reference.child("Tasks")
            .child(auth.currentUser?.uid.toString())
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        mList  = mutableListOf()
        adapter = ToDoAdapter(mList)
        adapter.setListener(this)
        binding.recyclerView.adapter = adapter
    }

    override fun onSaveTask(todo: String, todoEt: TextInputEditText) {
         databaseRef.push().setValue(todo).addOnCompleteListener{
             if(it.isSuccessful){
                 Toast.makeText(context,"Todo saved Successfully",Toast.LENGTH_SHORT).show()
                 todoEt.text = null

             }
             else {
                 Toast.makeText(context,it.exception?.toString(),Toast.LENGTH_SHORT).show()
             }
             popupFragment!!.dismiss()
         }
    }

    override fun onUpdateTask(todoData: ToDoData, todoEt: TextInputEditText) {
       val map = HashMap<String,Any>()
        map[todoData.taskId] = todoData.task
        databaseRef.updateChildren(map).addOnCompleteListener{
            if (it.isSuccessful){
                Toast.makeText(context,"Updated Successfully",Toast.LENGTH_SHORT).show()

            }
            else{
                Toast.makeText(context,it.exception?.message,Toast.LENGTH_SHORT).show()
            }
            todoEt.text = null
            popupFragment!!.dismiss()
        }
    }

    private fun getDataFromFirebase(){
         databaseRef.addValueEventListener(object :ValueEventListener{
             override fun onDataChange(snapshot: DataSnapshot) {
                mList.clear()
                 for(taskSnapshot in snapshot.children){
                     val todoTask = taskSnapshot.key?.let {
                         ToDoData(it,taskSnapshot.value.toString())
                     }
                     if (todoTask!=null){
                         mList.add(todoTask)
                     }
                 }
                 adapter.notifyDataSetChanged()
             }

             override fun onCancelled(error: DatabaseError) {
                 Toast.makeText(context,error.message,Toast.LENGTH_SHORT).show()
             }

         })
    }

    override fun onDeleteTaskBtnClicked(toDoData: ToDoData) {
        databaseRef.child(toDoData.taskId).removeValue().addOnCompleteListener{
            if (it.isSuccessful) {
                Toast.makeText(context,"Deleted Successfully",Toast.LENGTH_SHORT).show()
            }
            else{
                Toast.makeText(context,"",Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onEditTaskBtnClicked(toDoData: ToDoData) {
      if(popupFragment!=null)
      childFragmentManager.beginTransaction().remove(popupFragment!!).commit()
        popupFragment = AddToDoFragment.newInstance(toDoData.taskId ,toDoData.task)
        popupFragment!!.setListener(this)
        popupFragment!!.show(childFragmentManager,AddToDoFragment.TAG)
    }


}