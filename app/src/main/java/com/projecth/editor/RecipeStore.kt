package com.projecth.editor

import android.content.Context
import org.json.JSONArray

class RecipeStore(context:Context){
 private val p=context.getSharedPreferences("project_h_recipes",Context.MODE_PRIVATE)
 fun load():List<EditRecipe>{ val a=JSONArray(p.getString("recipes","[]")!!); return buildList{ for(i in 0 until a.length()) runCatching{add(EditRecipe.fromJson(a.getString(i)))} } }
 fun save(r:EditRecipe){ val a=JSONArray(); (load().filterNot{it.name.equals(r.name,true)}+r).forEach{a.put(it.toJson())}; p.edit().putString("recipes",a.toString()).apply() }
 fun delete(name:String){ val a=JSONArray(); load().filterNot{it.name.equals(name,true)}.forEach{a.put(it.toJson())}; p.edit().putString("recipes",a.toString()).apply() }
}
